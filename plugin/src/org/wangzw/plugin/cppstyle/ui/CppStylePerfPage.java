package org.wangzw.plugin.cppstyle.ui;

import java.io.File;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wangzw.plugin.cppstyle.CppCodeFormatter;
import org.wangzw.plugin.cppstyle.CppStyle;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class CppStylePerfPage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	private FileFieldEditor clangFormatPath = null;
	private FileFieldEditor cpplintPath = null;
	private BooleanFieldEditor enableCpplintOnSave = null;
	private BooleanFieldEditor enableClangFormatOnSave = null;

	public CppStylePerfPage() {
		super(GRID);
		setPreferenceStore(CppStyle.getDefault().getPreferenceStore());
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {
		clangFormatPath = new FileFieldEditor(
				CppStyleConstants.CLANG_FORMAT_PATH, "Chang-format path:",
				getFieldEditorParent());

		addField(clangFormatPath);

		cpplintPath = new FileFieldEditor(CppStyleConstants.CPPLINT_PATH,
				"Cpplint path:", getFieldEditorParent());

		addField(cpplintPath);

		enableCpplintOnSave = new BooleanFieldEditor(
				CppStyleConstants.ENABLE_CPPLINT_ON_SAVE,
				"Enable cpplint on save", getFieldEditorParent());

		if (!checkPathExist(CppCodeFormatter.getCpplintPath())) {
			enableCpplintOnSave.setEnabled(false, getFieldEditorParent());
		}

		addField(enableCpplintOnSave);
		
		enableClangFormatOnSave = new BooleanFieldEditor(
				CppStyleConstants.ENABLE_CLANGFORMAT_ON_SAVE,
				"Enable clang-format on save", getFieldEditorParent());

		if (!checkPathExist(CppCodeFormatter.getClangFormatPath())) {
			enableClangFormatOnSave.setEnabled(false, getFieldEditorParent());
		}

		addField(enableClangFormatOnSave);
	}

	public void init(IWorkbench workbench) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);

		if (event.getProperty().equals(FieldEditor.VALUE)) {
			if (event.getSource() == clangFormatPath) {
				clangFormatPathChange(event.getNewValue().toString());
			} else if (event.getSource() == cpplintPath) {
				cpplintPathChange(event.getNewValue().toString());
			}

			checkState();
		}
	}

	private boolean checkPathExist(String path) {
		File file = new File(path);
		return file.exists() && !file.isDirectory();
	}

	private void clangFormatPathChange(String newPath) {
		if (!checkPathExist(newPath)) {
			enableClangFormatOnSave.setEnabled(false, getFieldEditorParent());
			this.setValid(false);
			this.setMessage("Clang-format path \"" + newPath
					+ "\" does not exist", ERROR);
		} else {
			enableClangFormatOnSave.setEnabled(true, getFieldEditorParent());
			this.setValid(true);
			this.setMessage(null, ERROR);
		}
	}

	private void cpplintPathChange(String newPath) {
		if (!checkPathExist(newPath)) {
			enableCpplintOnSave.setEnabled(false, getFieldEditorParent());
			this.setValid(false);
			this.setMessage("Cpplint path \"" + newPath + "\" does not exist",
					ERROR);
		} else {
			enableCpplintOnSave.setEnabled(true, getFieldEditorParent());
			this.setValid(true);
			this.setMessage(null, ERROR);
		}
	}
}