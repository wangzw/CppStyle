package org.wangzw.plugin.cppstyle.ui;

import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.TextConsoleViewer;
import org.wangzw.plugin.cppstyle.CppStyle;

public class CppStyleConsoleViewer extends TextConsoleViewer {
	private CppStyleMessageConsole console = null;

	private IDocumentListener documentListener = new IDocumentListener() {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			clearFileLink();
		}
	};

	public CppStyleConsoleViewer(Composite parent, CppStyleMessageConsole console) {
		super(parent, console);
		this.console = console;
		this.getDocument().addDocumentListener(documentListener);
	}

	@Override
	protected void createControl(Composite parent, int styles) {
		super.createControl(parent, styles);
		setReadOnly();
		Control control = getTextWidget();
		control.addMouseListener(this);
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		StyledText widget = getTextWidget();
		if (widget != null) {
			int offset = -1;
			try {
				Point p = new Point(e.x, e.y);
				offset = widget.getOffsetAtLocation(p);
				FileLink link = getFileLink(offset);

				if (link != null) {
					link.linkActivated();
				}

			} catch (IllegalArgumentException ex) {
			}
		}
	}

	@Override
	protected void handleDispose() {
		Control control = getTextWidget();

		if (control != null) {
			control.removeMouseListener(this);
		}

		this.getDocument().removeDocumentListener(documentListener);
		super.handleDispose();
	}

	/**
	 * makes the associated text widget uneditable.
	 */
	public void setReadOnly() {
		ConsolePlugin.getStandardDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				StyledText text = getTextWidget();
				if (text != null && !text.isDisposed()) {
					text.setEditable(false);
				}
			}
		});
	}

	public FileLink getFileLink(int offset) {
		if (offset >= 0 && console != null) {
			return console.getFileLink(offset);
		}
		return null;
	}

	public void clearFileLink() {
		try {
			Position[] positions = getDocument().getPositions(CppStyleMessageConsole.ERROR_MARKER_CATEGORY);

			for (Position position : positions) {
				getDocument().removePosition(CppStyleMessageConsole.ERROR_MARKER_CATEGORY, position);
			}
		} catch (BadPositionCategoryException e) {
			e.printStackTrace();
		}
	}
}
