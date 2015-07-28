package org.wangzw.plugin.cppstyle.ui;

import org.eclipse.cdt.ui.ICEditor;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.InternalHandlerUtil;
import org.eclipse.ui.internal.SaveableHelper;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.handlers.AbstractSaveHandler;
import org.wangzw.plugin.cppstyle.ClangFormatFormatter;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CppStyleHandler extends AbstractSaveHandler {
	/**
	 * The constructor.
	 */
	public CppStyleHandler() {
		registerEnablement();
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
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ICEditor editor = getSaveableEditor(event);

		if (editor == null) {
			return null;
		}

		if (!editor.isDirty()) {
			return null;
		}

		IFile file = ((IFileEditorInput) editor.getEditorInput()).getFile();

		ClangFormatFormatter formater = new ClangFormatFormatter();

		if (formater.runClangFormatOnSave(file)) {
			formater.formatAndApply(editor);
		}

		IWorkbenchPage page = editor.getSite().getPage();
		page.saveEditor(editor, false);

		return null;
	}

	@Override
	protected EvaluationResult evaluate(IEvaluationContext context) {

		IWorkbenchWindow window = InternalHandlerUtil.getActiveWorkbenchWindow(context);
		// no window? not active
		if (window == null)
			return EvaluationResult.FALSE;
		WorkbenchPage page = (WorkbenchPage) window.getActivePage();

		// no page? not active
		if (page == null)
			return EvaluationResult.FALSE;

		// get saveable part
		ISaveablePart saveablePart = getSaveablePart(context);
		if (saveablePart == null)
			return EvaluationResult.FALSE;

		if (saveablePart instanceof ISaveablesSource) {
			ISaveablesSource modelSource = (ISaveablesSource) saveablePart;
			if (SaveableHelper.needsSave(modelSource))
				return EvaluationResult.TRUE;
			return EvaluationResult.FALSE;
		}

		if (saveablePart != null && saveablePart.isDirty())
			return EvaluationResult.TRUE;

		return EvaluationResult.FALSE;
	}
}
