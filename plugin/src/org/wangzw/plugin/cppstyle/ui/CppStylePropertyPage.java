package org.wangzw.plugin.cppstyle.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

public class CppStylePropertyPage extends PropertyPage implements
		SelectionListener {

	private static final String PROJECTS_PECIFIC_TEXT = "Enable project specific settings";

	private Button projectSpecificButton;
	private Button enableCpplintOnSaveButton;
	private Button enableClangFormatOnSaveButton;

	/**
	 * Constructor for SamplePropertyPage.
	 */
	public CppStylePropertyPage() {
		super();
	}

	private void constructPage(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		projectSpecificButton = new Button(composite, SWT.CHECK);
		projectSpecificButton.setText(PROJECTS_PECIFIC_TEXT);
		projectSpecificButton.addSelectionListener(this);

		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);

		composite = createDefaultComposite(parent);

		enableCpplintOnSaveButton = new Button(composite, SWT.CHECK);
		enableCpplintOnSaveButton.setText(CppStyleConstants.ENABLE_CPPLINT_TEXT);
		enableCpplintOnSaveButton.addSelectionListener(this);

		enableClangFormatOnSaveButton = new Button(composite, SWT.CHECK);
		enableClangFormatOnSaveButton.setText(CppStyleConstants.ENABLE_CLANGFORMAT_TEXT);
		enableClangFormatOnSaveButton.addSelectionListener(this);

		if (!getPropertyValue(CppStyleConstants.PROJECTS_PECIFIC_PROPERTY)) {
			projectSpecificButton.setSelection(false);
			enableCpplintOnSaveButton.setEnabled(false);
			enableClangFormatOnSaveButton.setEnabled(false);
		} else {
			projectSpecificButton.setSelection(true);
			enableCpplintOnSaveButton.setEnabled(true);
			enableCpplintOnSaveButton
					.setSelection(getPropertyValue(CppStyleConstants.ENABLE_CPPLINT_PROPERTY));
			enableClangFormatOnSaveButton.setEnabled(true);
			enableClangFormatOnSaveButton
					.setSelection(getPropertyValue(CppStyleConstants.ENABLE_CLANGFORMAT_PROPERTY));
		}
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);
		constructPage(composite);
		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	protected void performDefaults() {
		super.performDefaults();
		projectSpecificButton.setSelection(false);
		enableCpplintOnSaveButton.setSelection(false);
		enableCpplintOnSaveButton.setEnabled(false);
		enableClangFormatOnSaveButton.setSelection(false);
		enableClangFormatOnSaveButton.setEnabled(false);
	}

	public boolean performOk() {
		try {
			((IResource) getElement()).setPersistentProperty(new QualifiedName(
					"", CppStyleConstants.PROJECTS_PECIFIC_PROPERTY),
					new Boolean(projectSpecificButton.getSelection())
							.toString());

			((IResource) getElement()).setPersistentProperty(new QualifiedName(
					"", CppStyleConstants.ENABLE_CPPLINT_PROPERTY),
					new Boolean(enableCpplintOnSaveButton.getSelection())
							.toString());

			((IResource) getElement()).setPersistentProperty(new QualifiedName(
					"", CppStyleConstants.ENABLE_CLANGFORMAT_PROPERTY),
					new Boolean(enableClangFormatOnSaveButton.getSelection())
							.toString());
		} catch (CoreException e) {
			return false;
		}
		return true;
	}

	public boolean getPropertyValue(String key) {
		String enable;
		try {
			enable = ((IResource) getElement())
					.getPersistentProperty(new QualifiedName("", key));
			if (enable == null) {
				return false;
			}

		} catch (CoreException e) {
			return false;
		}

		return Boolean.parseBoolean(enable);
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == projectSpecificButton) {
			enableCpplintOnSaveButton.setEnabled(projectSpecificButton
					.getSelection());
			enableClangFormatOnSaveButton.setEnabled(projectSpecificButton
					.getSelection());
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

}