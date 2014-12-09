package org.wangzw.plugin.cppstyle.ui;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;

public class CppStyleConsolePage extends TextConsolePage {
	private CppStyleConsoleViewer view = null;
	private CppStyleMessageConsole console = null;

	public CppStyleConsolePage(CppStyleMessageConsole console, IConsoleView view) {
		super(console, view);
		this.console = console;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	protected TextConsoleViewer createViewer(Composite parent) {
		view = new CppStyleConsoleViewer(parent, console);
		return view;
	}

	@Override
	protected void contextMenuAboutToShow(IMenuManager menuManager) {
		super.contextMenuAboutToShow(menuManager);
		menuManager.remove(ActionFactory.CUT.getId());
		menuManager.remove(ActionFactory.PASTE.getId());
	}

	/**
	 * Highlight next/previous error or error by console offset
	 * 
	 * @param position
	 */
	void moveToError(int position) {
		if (console == null)
			return;

		System.err.println("position: " + position);
	}

}
