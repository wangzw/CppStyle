package org.wangzw.plugin.cppstyle.ui;

import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.part.IPageBookViewPage;
import org.wangzw.plugin.cppstyle.CppStyle;

public class CppStyleMessageConsole extends MessageConsole {
	private CppStyleConsolePage page = null;
	private MessageConsoleStream out = new MessageConsoleStream(this);
	private MessageConsoleStream err = new MessageConsoleStream(this);

	private static class MarkerPosition extends Position {
		private FileLink link = null;

		public MarkerPosition(FileLink link, int offset, int length) {
			super(offset, length);
			this.link = link;
		}

		public FileLink getFileLink() {
			return link;
		}
	}

	public static final String ERROR_MARKER_CATEGORY = "org.wangzw.cppstyle.ERROR_MARKER_POSITION";;
	private CppStyleConsolePatternMatchListener listener = null;

	public CppStyleMessageConsole(CppStyleConsolePatternMatchListener listener) {
		super(CppStyleConstants.CONSOLE_NAME, null);
		this.getDocument().addPositionCategory(ERROR_MARKER_CATEGORY);
		this.listener = listener;
		addPatternMatchListener(listener);

		runUI(new Runnable() {

			@Override
			public void run() {
				err.setColor(new Color(getStandardDisplay(), new RGB(255, 0, 0)));
			};
		});
	}

	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		page = new CppStyleConsolePage(this, view);
		return page;
	}

	public CppStyleConsolePatternMatchListener getListener() {
		return listener;
	}

	public void setListener(CppStyleConsolePatternMatchListener listener) {
		removePatternMatchListener(this.listener);
		addPatternMatchListener(listener);
		this.listener = listener;
	}

	public CppStyleConsolePage getActivePage() {
		return page;
	}

	public FileLink getFileLink(int offset) {
		try {
			IDocument document = getDocument();
			if (document != null) {
				Position[] positions = document.getPositions(ERROR_MARKER_CATEGORY);
				Position position = findPosition(offset, positions);
				if (position instanceof MarkerPosition) {
					return ((MarkerPosition) position).getFileLink();
				}
			}
		} catch (BadPositionCategoryException e) {
		}
		return null;
	}

	public void addFileLink(FileLink link, int offset, int length) {
		IDocument document = getDocument();
		MarkerPosition linkPosition = new MarkerPosition(link, offset, length);
		try {
			document.addPosition(ERROR_MARKER_CATEGORY, linkPosition);
			IConsoleManager fConsoleManager = ConsolePlugin.getDefault().getConsoleManager();
			fConsoleManager.refresh(this);
		} catch (BadPositionCategoryException e) {
			CppStyle.log(e);
		} catch (BadLocationException e) {
			CppStyle.log(e);
		}
	}

	/**
	 * Binary search for the position at a given offset.
	 *
	 * @param offset
	 *            the offset whose position should be found
	 * @return the position containing the offset, or <code>null</code>
	 */
	private Position findPosition(int offset, Position[] positions) {

		if (positions.length == 0) {
			return null;
		}

		int left = 0;
		int right = positions.length - 1;
		int mid = 0;
		Position position = null;

		while (left < right) {

			mid = (left + right) / 2;

			position = positions[mid];
			if (offset < position.getOffset()) {
				if (left == mid) {
					right = left;
				} else {
					right = mid - 1;
				}
			} else if (offset > (position.getOffset() + position.getLength() - 1)) {
				if (right == mid) {
					left = right;
				} else {
					left = mid + 1;
				}
			} else {
				left = right = mid;
			}
		}

		position = positions[left];
		if (offset >= position.getOffset() && (offset < (position.getOffset() + position.getLength()))) {
			return position;
		}
		return null;
	}

	public MessageConsoleStream getOutputStream() {
		out.setActivateOnWrite(page != null ? page.activeOnStdout() : true);
		return out;
	}

	public MessageConsoleStream getErrorStream() {
		err.setActivateOnWrite(page != null ? page.activeOnStderr() : true);
		return err;
	}

	public static Display getStandardDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	private void runUI(Runnable run) {
		Display display;
		display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
			display.syncExec(run);
		} else {
			run.run();
		}
	}

	public void clear() {
		runUI(new Runnable() {

			@Override
			public void run() {
				IDocument document = getDocument();
				document.set("");
			};
		});

	}
}
