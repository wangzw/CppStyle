package org.wangzw.plugin.cppstyle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.ui.ICEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
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
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsoleStream;
import org.wangzw.plugin.cppstyle.ui.CppStyleConsolePatternMatchListener;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

public class CppCodeFormatter extends CodeFormatter {
	private CppStyleMessageConsole console = null;
	private CppStyleConsolePatternMatchListener listener = null;
	private MessageConsoleStream out = null;
	private MessageConsoleStream err = null;

	public CppCodeFormatter() {
		super();
		setupConsoleStream();
	}

	@Override
	public String createIndentationString(int indentationLevel) {
		return super.createIndentationString(indentationLevel);
	}

	@Override
	public void setOptions(Map<String, ?> arg0) {
	}

	@Override
	public TextEdit format(int kind, String source, int offset, int length,
			int arg4, String lineSeparator) {
		TextEdit[] edits = format(kind, source, new IRegion[] { new Region(
				offset, length) }, lineSeparator);

		if (edits != null) {
			return edits[0];
		}

		return null;
	}

	@Override
	public TextEdit[] format(int kind, String source, IRegion[] regions,
			String lineSeparator) {

		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = page.getActiveEditor();

		return format(source, editor, regions);
	}

	public void formatAndApply(ICEditor editor) {
		TextEdit rootEdit = new MultiTextEdit();
		IDocument doc = editor.getDocumentProvider().getDocument(
				editor.getEditorInput());

		TextEdit[] editors = format(doc.get(), editor, null);

		if (editors == null) {
			return;
		}

		for (TextEdit e : editors) {
			rootEdit.addChild(e);
		}

		IDocumentUndoManager manager = DocumentUndoManagerRegistry
				.getDocumentUndoManager(doc);
		manager.beginCompoundChange();

		try {
			rootEdit.apply(doc);
		} catch (MalformedTreeException e) {
			err.print(e.getMessage());
		} catch (BadLocationException e) {
			err.print(e.getMessage());
		}

		manager.endCompoundChange();
	}

	private TextEdit[] format(String source, IEditorPart editor,
			IRegion[] regions) {
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
			IFile file = ((IFileEditorInput) ceditor.getEditorInput())
					.getFile();
			path = file.getLocation().toOSString();
			root = file.getProject().getLocation().toOSString();
		} else {
			root = ResourcesPlugin.getWorkspace().getRoot().getLocation()
					.toOSString();
			path = new File(root, "a.cc").getAbsolutePath();
		}

		conf = getClangForamtConfigureFile(path);

		if (conf == null) {
			err.println("Cannot find clang-format configure file under any level parent directories of path ("
					+ path + ").");
			err.println("Run clang-format with Google style by default.");
			formatArg = "-style=Google";
		} else {
			out.println("Use clang-format configure file (" + conf + ")");
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

		String command = clangFormat + " -assume-filename=" + path + " "
				+ sb.toString();

		ProcessBuilder builder = new ProcessBuilder(commands);
		builder.directory(new File(root));
		builder.redirectErrorStream(true);

		try {
			Process process = builder.start();
			out.println("Run clang-format command: " + command);

			OutputStreamWriter output = new OutputStreamWriter(
					process.getOutputStream());

			output.write(source);
			output.flush();
			output.close();

			InputStreamReader reader = new InputStreamReader(
					process.getInputStream());

			final char[] buffer = new char[1024];
			final StringBuilder out = new StringBuilder();

			for (;;) {
				int rsz = reader.read(buffer, 0, buffer.length);

				if (rsz < 0) {
					break;
				}

				out.append(buffer, 0, rsz);
			}

			String newSource = out.toString();

			int code = process.waitFor();
			if (code != 0) {
				err.println("clang-format return error (" + code + ").");
				err.println(newSource);
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
				if (source.charAt(lenSource - suffix - 1) != newSource
						.charAt(lenNewSource - suffix - 1)) {
					break;
				}
			}

			retval[0] = new ReplaceEdit(start,
					source.length() - start - suffix, newSource.substring(
							start, lenNewSource - suffix));

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

	public void checkFileFormat(IFile file) {
		try {
			String path = file.getLocation().toOSString();
			String cpplint = getCpplintPath();
			String projectRoot = file.getProject().getLocation().toOSString();
			String root = getCpplintRoot(file);

			List<String> commands = new ArrayList<String>();
			commands.add(cpplint);

			if (root != null && !root.isEmpty()) {
				commands.add("--root=" + root);
			}

			commands.add(path);

			StringBuffer sb = new StringBuffer();

			for (String arg : commands) {
				sb.append(arg);
				sb.append(' ');
			}

			ProcessBuilder builder = new ProcessBuilder(commands);
			builder.directory(new File(projectRoot));
			builder.redirectErrorStream(true);
			Process process = builder.start();

			out.println("Run cpplint.py command: " + sb.toString());

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			parserCpplintOutput(file, reader);

			process.waitFor();
		} catch (IOException e) {
			err.println(e.getMessage());
		} catch (InterruptedException e) {
			err.println(e.getMessage());
		}
	}

	private void parserCpplintOutput(IFile file, BufferedReader reader) {
		String line = null;

		String pattern = CppStyleConstants.CPPLINT_OUTPUT_PATTERN;
		Pattern p = Pattern.compile(pattern);
		int lineNumGroup = CppStyleConstants.CPPLINT_OUTPUT_PATTERN_LINE_NO_GROUP;
		int msgGroup = CppStyleConstants.CPPLINT_OUTPUT_PATTERN_MSG_GROUP;

		listener.setFile(file);

		try {
			while ((line = reader.readLine()) != null) {
				Matcher m = p.matcher(line);

				if (m.matches()) {
					String ln = m.group(lineNumGroup);
					String msg = m.group(msgGroup);

					if (ln != null && msg != null) {
						int lineno = Integer.parseInt(ln);
						createIssueMarker(file, lineno == 0 ? 1 : lineno, msg);
					}

					err.println(CppStyleConstants.CPPLINT_CONSOLE_ERROR_PREFIX
							+ line);
				} else {
					if (line.startsWith("Done") || line.startsWith("Total")) {
						out.println(CppStyleConstants.CPPLINT_CONSOLE_PREFIX
								+ line);
					} else {
						err.println(CppStyleConstants.CPPLINT_CONSOLE_PREFIX
								+ line);
					}
				}
			}
		} catch (IOException e) {
			err.println(e.getMessage());
		}
	}

	public void createIssueMarker(IResource resource, int line, String msg) {
		try {
			IMarker marker = resource
					.createMarker(CppStyleConstants.CPPLINT_MARKER);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.MESSAGE, msg);
			marker.setAttribute(IMarker.LINE_NUMBER, line);
			marker.setAttribute(IMarker.PROBLEM, true);
		} catch (CoreException e) {
			err.println(e.getMessage());
		}
	}

	public void deleteAllMarkers(IResource target) {
		String type = CppStyleConstants.CPPLINT_MARKER;

		try {
			IMarker[] markers = target.findMarkers(type, true,
					IResource.DEPTH_INFINITE);

			for (IMarker marker : markers) {
				marker.delete();
			}
		} catch (CoreException e) {
			err.println(e.getMessage());
		}
	}

	private boolean enableCpplintOnSave(IResource resource) {
		boolean enable = CppStyle.getDefault().getPreferenceStore()
				.getBoolean(CppStyleConstants.ENABLE_CPPLINT_ON_SAVE);

		try {
			IProject project = resource.getProject();
			String enableProjectSpecific = project
					.getPersistentProperty(new QualifiedName("",
							CppStyleConstants.PROJECTS_PECIFIC_PROPERTY));

			if (enableProjectSpecific != null
					&& Boolean.parseBoolean(enableProjectSpecific)) {
				String value = project.getPersistentProperty(new QualifiedName(
						"", CppStyleConstants.ENABLE_CPPLINT_PROPERTY));
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

	public boolean runCpplintOnSave(IResource resource) {
		if (!enableCpplintOnSave(resource)) {
			return false;
		}

		String cpplint = getCpplintPath();

		if (cpplint == null) {
			err.println("cpplint.py is not specified.");
			return false;
		}

		File file = new File(cpplint);

		if (!file.exists()) {
			err.println("cpplint.py (" + cpplint + ") does not exist.");
			return false;
		}

		if (!file.canExecute()) {
			err.println("cpplint.py (" + cpplint + ") is not executable.");
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
					.getPersistentProperty(new QualifiedName("",
							CppStyleConstants.PROJECTS_PECIFIC_PROPERTY));

			if (enableProjectSpecific != null
					&& Boolean.parseBoolean(enableProjectSpecific)) {
				String value = project.getPersistentProperty(new QualifiedName(
						"", CppStyleConstants.ENABLE_CLANGFORMAT_PROPERTY));
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
		return CppStyle.getDefault().getPreferenceStore()
				.getString(CppStyleConstants.CLANG_FORMAT_PATH);
	}

	public static String getCpplintPath() {
		return CppStyle.getDefault().getPreferenceStore()
				.getString(CppStyleConstants.CPPLINT_PATH);
	}

	static String getVersionControlRoot(IFile file) {
		File current = file.getLocation().toFile();

		File dir = current.getParentFile();

		return getVersionControlRoot(dir);
	}

	public static String getVersionControlRoot(File dir) {

		while (dir != null) {
			for (final File entry : dir.listFiles()) {
				String name = entry.getName();
				if (name.equals(".git") || name.equals(".hg")
						|| name.equals(".svn")) {
					return dir.getPath();
				}
			}

			dir = dir.getParentFile();
		}

		return null;
	}

	public static String getCpplintRoot(IFile file) {
		IProject project = file.getProject();
		String rootSpec;
		try {
			rootSpec = project.getPersistentProperty(new QualifiedName("",
					CppStyleConstants.CPPLINT_PROJECT_ROOT));

			if (rootSpec == null || rootSpec.isEmpty()) {
				return null;
			}
		} catch (CoreException e) {
			return null;
		}

		String rootVc = getVersionControlRoot(file);

		if (rootVc == null) {
			return null;
		}

		String relative = new File(rootVc).toURI()
				.relativize(new File(rootSpec).toURI()).getPath();

		if (relative.endsWith("" + Path.SEPARATOR)) {
			return relative.substring(0, relative.length() - 1);
		}

		return relative;
	}

	private void setupConsoleStream() {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();

		for (int i = 0; i < existing.length; i++) {
			if (CppStyleConstants.CONSOLE_NAME.equals(existing[i].getName())) {
				console = (CppStyleMessageConsole) existing[i];
				console.clearConsole();
				listener = console.getListener();
			}
		}

		if (console == null) {
			// no console found, so create a new one
			listener = new CppStyleConsolePatternMatchListener();
			console = new CppStyleMessageConsole(listener);
			conMan.addConsoles(new IConsole[] { console });
		}

		out = console.newStdoutMessageStream();
		err = console.newStderrMessageStream();

		err.setColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
	}
}
