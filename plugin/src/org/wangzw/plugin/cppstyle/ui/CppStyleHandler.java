package org.wangzw.plugin.cppstyle.ui;

import org.eclipse.cdt.ui.ICEditor;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.util.Util;
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

	protected ICEditor getSaveableEditor(ExecutionEvent event) {

		IWorkbenchPart activePart = HandlerUtil.getActivePart(event);

		if (activePart instanceof ICEditor) {
			return (ICEditor) activePart;
		}

		return null;
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ICEditor editor = getSaveableEditor(event);

		if (editor == null) {
			return null;
		}

		if (!editor.isDirty()) {
			return null;
		}

		IFile file = ((IFileEditorInput) editor.getEditorInput()).getFile();

		CppCodeFormatter formater = new CppCodeFormatter();

		if (formater.runClangFormatOnSave(file)) {
			formater.formatAndApply(editor);
		}

		IWorkbenchPage page = editor.getSite().getPage();
		page.saveEditor(editor, false);

		formater.deleteAllMarkers(file);

		if (formater.runCpplintOnSave(file)) {
			formater.checkFileFormat(file);
		}

		return null;
	}
}
