package org.wangzw.plugin.cppstyle.ui;

import org.eclipse.cdt.ui.ICEditor;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wangzw.plugin.cppstyle.CppCodeFormatter;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CppStyleHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public CppStyleHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow activeWorkbenchWindow = HandlerUtil
				.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = activeWorkbenchWindow.getActivePage();

		if (page == null) {
			return null;
		}

		IEditorPart editor = page.getActiveEditor();

		if (editor == null) {
			return null;
		}

		if (editor instanceof ICEditor) {
			ICEditor ceditor = (ICEditor) editor;

			if (!ceditor.isDirty()) {
				return null;
			}

			ceditor.doSave(null);
			IFile file = ((IFileEditorInput) ceditor.getEditorInput())
					.getFile();

			CppCodeFormatter.deleteAllMarkers(file);
			
			if (CppCodeFormatter.runCpplintOnSave(file)) {
				CppCodeFormatter.checkFileFormat(file);
			}
		}

		return null;
	}
}
