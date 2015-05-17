package org.wangzw.plugin.cppstyle.ui;

import org.eclipse.debug.internal.ui.views.console.ShowStandardErrorAction;
import org.eclipse.debug.internal.ui.views.console.ShowStandardOutAction;
import org.eclipse.debug.internal.ui.views.console.ShowWhenContentChangesAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;

public class CppStyleConsolePage extends TextConsolePage {
	private CppStyleConsoleViewer viewer = null;
	private CppStyleMessageConsole console = null;
	private ShowWhenContentChangesAction fStdOut;
	private ShowWhenContentChangesAction fStdErr;

	public CppStyleConsolePage(CppStyleMessageConsole console, IConsoleView view) {
		super(console, view);
		this.console = console;
	}

	@Override
	public void dispose() {
		super.dispose();

		if (fStdOut != null) {
			fStdOut.dispose();
			fStdOut = null;
		}
		if (fStdErr != null) {
			fStdErr.dispose();
			fStdErr = null;
		}
	}

	@Override
	protected TextConsoleViewer createViewer(Composite parent) {
		viewer = new CppStyleConsoleViewer(parent, console);
		return viewer;
	}

	@Override
	protected void contextMenuAboutToShow(IMenuManager menuManager) {
		super.contextMenuAboutToShow(menuManager);
		menuManager.remove(ActionFactory.CUT.getId());
		menuManager.remove(ActionFactory.PASTE.getId());
	}

	@Override
	protected void configureToolBar(IToolBarManager mgr) {
		super.configureToolBar(mgr);
		fStdOut = new ShowStandardOutAction();
		fStdErr = new ShowStandardErrorAction();
		mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, fStdOut);
		mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, fStdErr);
	}

	public boolean activeOnStdout() {
		if (fStdOut == null || !fStdOut.isChecked()) {
			return false;
		}

		return true;
	}

	public boolean activeOnStderr() {
		if (fStdErr == null || !fStdErr.isChecked()) {
			return false;
		}

		return true;
	}

}
