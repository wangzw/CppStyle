package org.wangzw.plugin.cppstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ITranslationUnit;
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
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.console.MessageConsoleStream;
import org.wangzw.plugin.cppstyle.diff_match_patch.Diff;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

public class ClangFormatFormatter extends CodeFormatter {
	private MessageConsoleStream err = null;
	private Map<String, ?> options;

	public ClangFormatFormatter() {
		super();
		CppStyleMessageConsole console = CppStyle.buildConsole();
		err = console.getErrorStream();
	}

	@Override
	public String createIndentationString(int indentationLevel) {
		return super.createIndentationString(indentationLevel);
	}

	@Override
	public void setOptions(Map<String, ?> options) {
		if (options != null) {
			this.options = options;
		} else {
			this.options = CCorePlugin.getOptions();
		}
	}

	private String getSourceFilePath() {
		ITranslationUnit tu = (ITranslationUnit) options.get(DefaultCodeFormatterConstants.FORMATTER_TRANSLATION_UNIT);

		if (tu == null) {
			IFile file = (IFile) options.get(DefaultCodeFormatterConstants.FORMATTER_CURRENT_FILE);
			if (file != null) {
				tu = (ITranslationUnit) CoreModel.getDefault().create(file);
			}
		}

		if (tu != null) {
			return tu.getResource().getRawLocation().toOSString();
		} else {
			String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
			return new File(root, "a.cc").getAbsolutePath();
		}
	}

	@Override
	public TextEdit format(int kind, String source, int offset, int length, int arg4, String lineSeparator) {
		TextEdit retval = format(source, getSourceFilePath(), new Region(offset, length));
		return retval != null ? retval : new MultiTextEdit();
	}

	public void formatAndApply(ICEditor editor) {
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

		String path = ((IFileEditorInput) editor.getEditorInput()).getFile().getLocation().toOSString();
		TextEdit res = format(doc.get(), path, null);

		if (res == null) {
			return;
		}

		IDocumentUndoManager manager = DocumentUndoManagerRegistry.getDocumentUndoManager(doc);
		manager.beginCompoundChange();

		try {
			res.apply(doc);
		} catch (MalformedTreeException e) {
			CppStyle.log("Failed to apply change", e);
		} catch (BadLocationException e) {
			CppStyle.log("Failed to apply change", e);
		}

		manager.endCompoundChange();

	}

	private TextEdit format(String source, String path, IRegion region) {
		String conf = null;
		String formatArg = "";
		String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();

		if (checkClangFormat() == false) {
			return null;
		}

		String clangFormat = getClangFormatPath();

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

		if (region != null) {
			commands.add("-offset=" + region.getOffset());
			commands.add("-length=" + region.getLength());

			sb.append("-offset=");
			sb.append(region.getOffset());
			sb.append(" -length=");
			sb.append(region.getLength());
			sb.append(' ');
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

			diff_match_patch diff = new diff_match_patch();

			LinkedList<Diff> diffs = diff.diff_main(source, newSource);
			diff.diff_cleanupEfficiency(diffs);

			int offset = 0;
			MultiTextEdit edit = new MultiTextEdit();

			for (Diff d : diffs) {
				switch (d.operation) {
				case INSERT:
					InsertEdit e = new InsertEdit(offset, d.text);
					edit.addChild(e);
					break;
				case DELETE:
					DeleteEdit e1 = new DeleteEdit(offset, d.text.length());
					offset += d.text.length();
					edit.addChild(e1);
					break;
				case EQUAL:
					offset += d.text.length();
					break;
				}
			}

			return edit;

		} catch (IOException e) {
			CppStyle.log("Failed to format code", e);
		} catch (InterruptedException e) {
			CppStyle.log("Failed to format code", e);
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
			CppStyle.log(e);
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
