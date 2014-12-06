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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;

public class CppCodeFormatter extends CodeFormatter {
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
		String root = null;

		if (checkClangFormat() == false) {
			return null;
		}

		String clangFormat = getClangFormatPath();

		IEditorPart editor = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (editor == null) {
			err.println("cannot get active editor.");
			return null;
		}

		String path = null;

		if (editor instanceof ICEditor) {
			ICEditor ceditor = (ICEditor) editor;
			IFile file = ((IFileEditorInput) ceditor.getEditorInput())
					.getFile();
			path = file.getLocation().toOSString();
			root = file.getProject().getLocation().toOSString();
		} else {
			err.println("can only format c/c++ source file.");
			return null;
		}

		StringBuffer sb = new StringBuffer();

		List<String> commands = new ArrayList<String>();
		commands.add(clangFormat);
		commands.add("-assume-filename=" + path);

		for (IRegion region : regions) {
			commands.add("-offset=" + region.getOffset());
			commands.add("-length=" + region.getLength());

			sb.append("-offset=");
			sb.append(region.getOffset());
			sb.append(" -length=");
			sb.append(region.getLength());
			sb.append(' ');
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
			retval[0] = new ReplaceEdit(0, source.length(), newSource);

			return retval;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
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
			String root = file.getProject().getLocation().toOSString();

			List<String> commands = new ArrayList<String>();
			commands.add(cpplint);
			commands.add("--root=" + root);
			commands.add(path);

			StringBuffer sb = new StringBuffer();

			for (String arg : commands) {
				sb.append(arg);
				sb.append(' ');
			}

			ProcessBuilder builder = new ProcessBuilder(commands);
			builder.directory(new File(root));
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

		String pattern = "(.+)\\:(\\d+)\\:(.+)";
		Pattern p = Pattern.compile(pattern);

		try {
			while ((line = reader.readLine()) != null) {
				Matcher m = p.matcher(line);

				if (m.matches()) {
					String ln = m.group(2);
					String msg = m.group(3);

					if (ln != null && msg != null) {
						createIssueMarker(file, Integer.parseInt(ln), msg);
					}

					err.println(line);
				} else {
					if (line.startsWith("Done") || line.startsWith("Total")) {
						out.println("cpplint.py: " + line);
					} else {
						err.println("cpplint.py: " + line);
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

	public static String getClangFormatPath() {
		return CppStyle.getDefault().getPreferenceStore()
				.getString(CppStyleConstants.CLANG_FORMAT_PATH);
	}

	public static String getCpplintPath() {
		return CppStyle.getDefault().getPreferenceStore()
				.getString(CppStyleConstants.CPPLINT_PATH);
	}

	private void setupConsoleStream() {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		MessageConsole console = null;

		for (int i = 0; i < existing.length; i++) {
			if (CppStyleConstants.CONSOLE_NAME.equals(existing[i].getName())) {
				console = (MessageConsole) existing[i];
			}
		}

		if (console == null) {
			// no console found, so create a new one
			console = new MessageConsole(CppStyleConstants.CONSOLE_NAME, null);
			conMan.addConsoles(new IConsole[] { console });
		}

		out = console.newMessageStream();
		err = console.newMessageStream();

		out.setActivateOnWrite(true);
		err.setActivateOnWrite(true);

		err.setColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
	}
}
