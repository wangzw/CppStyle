package org.wangzw.plugin.cppstyle.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

public class CppStyleConsolePatternMatchListener implements IPatternMatchListener {

	private IFile file = null;
	private Pattern pattern = null;
	private int lineNumGroup = CppStyleConstants.CPPLINT_OUTPUT_PATTERN_LINE_NO_GROUP;
	private String patternMsg = CppStyleConstants.CPPLINT_OUTPUT_PATTERN;

	public IFile getFile() {
		return file;
	}

	public void setFile(IFile file) {
		this.file = file;
	}

	@Override
	public void connect(TextConsole console) {
		pattern = Pattern.compile(patternMsg);
	}

	@Override
	public void disconnect() {
	}

	@Override
	public String getPattern() {
		return patternMsg;
	}

	@Override
	public int getCompilerFlags() {
		return 0;
	}

	@Override
	public String getLineQualifier() {
		return "\\n|\\r";
	}

	@Override
	public void matchFound(PatternMatchEvent event) {
		try {
			CppStyleMessageConsole console = (CppStyleMessageConsole) event.getSource();

			String line = console.getDocument().get(event.getOffset(), event.getLength());

			Matcher m = pattern.matcher(line);
			if (m.matches()) {
				String ln = m.group(lineNumGroup);

				int lineno = Integer.parseInt(ln);

				FileLink link = new FileLink(file, null, -1, -1, lineno == 0 ? 1 : lineno);
				console.addFileLink(link, event.getOffset(), event.getLength());
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
}
