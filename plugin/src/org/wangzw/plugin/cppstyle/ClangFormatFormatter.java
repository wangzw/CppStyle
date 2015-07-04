package org.wangzw.plugin.cppstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.ui.ICEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

public class ClangFormatFormatter extends CodeFormatter {
	private MessageConsoleStream out = null;
	private MessageConsoleStream err = null;

	public ClangFormatFormatter() {
		super();
		CppStyleMessageConsole console = CppStyle.buildConsole();
		out = console.getOutputStream();
		err = console.getErrorStream();
	}

	@Override
	public String createIndentationString(int indentationLevel) {
		return super.createIndentationString(indentationLevel);
	}

	@Override
	public void setOptions(Map<String, ?> arg0) {
	}

	@Override
	public TextEdit format(int kind, String source, int offset, int length, int arg4, String lineSeparator) {
		TextEdit[] edits = format(kind, source, new IRegion[] { new Region(offset, length) }, lineSeparator);

		if (edits != null) {
			return edits[0];
		}

		return null;
	}

	@Override
	public TextEdit[] format(int kind, String source, IRegion[] regions, String lineSeparator) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = page.getActiveEditor();

		return format(source, editor, regions);
	}

	public void formatAndApply(ICEditor editor) {
		TextEdit rootEdit = new MultiTextEdit();
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

		TextEdit[] editors = format(doc.get(), editor, null);

		if (editors == null) {
			return;
		}

		for (TextEdit e : editors) {
			rootEdit.addChild(e);
		}

		IDocumentUndoManager manager = DocumentUndoManagerRegistry.getDocumentUndoManager(doc);
		manager.beginCompoundChange();

		try {
			rootEdit.apply(doc);
		} catch (MalformedTreeException e) {
			CppStyle.log("Failed to apply change", e);
		} catch (BadLocationException e) {
			CppStyle.log("Failed to apply change", e);
		}

		manager.endCompoundChange();

	}

	private TextEdit[] format(String source, IEditorPart editor, IRegion[] regions) {
		String root = null;
		String path = null;
		String conf = null;
		String formatArg = "";

		if (checkClangFormat() == false) {
			return null;
		}

		String clangFormat = getClangFormatPath();

		if (editor != null && editor instanceof ICEditor) {
			ICEditor ceditor = (ICEditor) editor;
			IFile file = ((IFileEditorInput) ceditor.getEditorInput()).getFile();
			path = file.getLocation().toOSString();
			root = file.getProject().getLocation().toOSString();
		} else {
			root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
			path = new File(root, "a.cc").getAbsolutePath();
		}

		conf = getClangForamtConfigureFile(path);

		if (conf == null) {
			err.println("Cannot find clang-format configure file under any level parent directories of path (" + path
					+ ").");
			err.println("Run clang-format with Google style by default.");
			formatArg = "-style=Google";
		}

		StringBuffer sb = new StringBuffer();

		List<String> commands = new ArrayList<String>();
		commands.add(clangFormat);
		commands.add("-assume-filename=" + path);

		if (regions != null) {
			for (IRegion region : regions) {
				commands.add("-offset=" + region.getOffset());
				commands.add("-length=" + region.getLength());

				sb.append("-offset=");
				sb.append(region.getOffset());
				sb.append(" -length=");
				sb.append(region.getLength());
				sb.append(' ');
			}
		}

		if (!formatArg.isEmpty()) {
			sb.append(formatArg);
			commands.add(formatArg);
		}

		ProcessBuilder builder = new ProcessBuilder(commands);
		builder.directory(new File(root));

		try {
			Process process = builder.start();
			OutputStreamWriter output = new OutputStreamWriter(process.getOutputStream());

			output.write(source);
			output.flush();
			output.close();

			InputStreamReader reader = new InputStreamReader(process.getInputStream());
			InputStreamReader error = new InputStreamReader(process.getErrorStream());

			final char[] buffer = new char[1024];
			final StringBuilder stdout = new StringBuilder();
			final StringBuilder errout = new StringBuilder();

			for (;;) {
				int rsz = reader.read(buffer, 0, buffer.length);

				if (rsz < 0) {
					break;
				}

				stdout.append(buffer, 0, rsz);
			}

			for (;;) {
				int rsz = error.read(buffer, 0, buffer.length);

				if (rsz < 0) {
					break;
				}

				errout.append(buffer, 0, rsz);
			}

			String newSource = stdout.toString();

			int code = process.waitFor();
			if (code != 0) {
				err.println("clang-format return error (" + code + ").");
				err.println(errout.toString());
				return null;
			}

			if (errout.length() > 0) {
				err.println(errout.toString());
				return null;
			}

			if (0 == source.compareTo(newSource)) {
				return null;
			}

			TextEdit[] retval = new TextEdit[1];

			// skip the common prefix
			int start = 0, suffix = 0;
			int lenSource = source.length();
			int lenNewSource = newSource.length();
			int minLength = Math.min(lenSource, lenNewSource);

			for (; start < minLength; ++start) {
				if (source.charAt(start) != newSource.charAt(start)) {
					break;
				}
			}

			// skip the common suffix
			for (; suffix < minLength - start; ++suffix) {
				if (source.charAt(lenSource - suffix - 1) != newSource.charAt(lenNewSource - suffix - 1)) {
					break;
				}
			}

			retval[0] = new ReplaceEdit(start, source.length() - start - suffix,
					newSource.substring(start, lenNewSource - suffix));

			return retval;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	private String getClangForamtConfigureFile(String path) {
		File file = new File(path);

		while (file != null) {
			File dir = file.getParentFile();

			File conf = new File(dir, ".clang-format");
			if (dir != null && conf.exists()) {
				return conf.getAbsolutePath();
			} else {
				file = dir;
			}
		}

		return null;
	}

	public boolean checkClangFormat() {
		String clangformat = getClangFormatPath();

		if (clangformat == null) {
			err.println("clang-format is not specified.");
			return false;
		}

		File file = new File(clangformat);

		if (!file.exists()) {
			err.println("clang-format (" + clangformat + ") does not exist.");
			return false;
		}

		if (!file.canExecute()) {
			err.println("clang-format (" + clangformat + ") is not executable.");
			return false;
		}

		return true;
	}

	private boolean enableClangFormatOnSave(IResource resource) {
		boolean enable = CppStyle.getDefault().getPreferenceStore()
				.getBoolean(CppStyleConstants.ENABLE_CLANGFORMAT_ON_SAVE);

		try {
			IProject project = resource.getProject();
			String enableProjectSpecific = project
					.getPersistentProperty(new QualifiedName("", CppStyleConstants.PROJECTS_PECIFIC_PROPERTY));

			if (enableProjectSpecific != null && Boolean.parseBoolean(enableProjectSpecific)) {
				String value = project
						.getPersistentProperty(new QualifiedName("", CppStyleConstants.ENABLE_CLANGFORMAT_PROPERTY));
				if (value != null) {
					return Boolean.parseBoolean(value);
				}

				return false;
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return enable;
	}

	public boolean runClangFormatOnSave(IResource resource) {
		if (!enableClangFormatOnSave(resource)) {
			return false;
		}

		String clangFormat = getClangFormatPath();

		if (clangFormat == null) {
			err.println("clang-format is not specified.");
			return false;
		}

		File file = new File(clangFormat);

		if (!file.exists()) {
			err.println("clang-format (" + clangFormat + ") does not exist.");
			return false;
		}

		if (!file.canExecute()) {
			err.println("clang-format (" + clangFormat + ") is not executable.");
			return false;
		}

		return true;
	}

	public static String getClangFormatPath() {
		return CppStyle.getDefault().getPreferenceStore().getString(CppStyleConstants.CLANG_FORMAT_PATH);
	}

}
