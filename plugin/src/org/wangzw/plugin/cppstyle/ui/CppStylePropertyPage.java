package org.wangzw.plugin.cppstyle.ui;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.internal.Workbench;
import org.wangzw.plugin.cppstyle.CpplintCheckSettings;

public class CppStylePropertyPage extends PropertyPage implements
		SelectionListener, IPropertyChangeListener, ModifyListener {

	private static final String PROJECTS_PECIFIC_TEXT = "Enable project specific settings";

	private Button projectSpecificButton;
	private Button enableCpplintOnSaveButton;
	private Button enableClangFormatOnSaveButton;
	private DirectoryFieldEditor projectRoot;
	private Text projectRootText = null;
	String projectPath = null;

	/**
	 * Constructor for SamplePropertyPage.
	 */
	public CppStylePropertyPage() {
		super();
	}

	private void constructPage(Composite parent) {
		Composite composite = createComposite(parent, 2);

		projectSpecificButton = new Button(composite, SWT.CHECK);
		projectSpecificButton.setText(PROJECTS_PECIFIC_TEXT);
		projectSpecificButton.addSelectionListener(this);

		Button perfSetting = new Button(composite, SWT.PUSH);
		perfSetting.setText("Configure Workspace Settings...");
		perfSetting.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				configureWorkspaceSettings();
			}
		});

		createSepeerater(parent);

		composite = createComposite(parent, 1);

		enableCpplintOnSaveButton = new Button(composite, SWT.CHECK);
		enableCpplintOnSaveButton
				.setText(CppStyleConstants.ENABLE_CPPLINT_TEXT);
		enableCpplintOnSaveButton.addSelectionListener(this);

		enableClangFormatOnSaveButton = new Button(composite, SWT.CHECK);
		enableClangFormatOnSaveButton
				.setText(CppStyleConstants.ENABLE_CLANGFORMAT_TEXT);
		enableClangFormatOnSaveButton.addSelectionListener(this);

		createSepeerater(parent);

		composite = createComposite(parent, 1);

		Label laber = new Label(composite, SWT.NONE);
		laber.setText(CppStyleConstants.PROJECT_ROOT_TEXT);

		composite = createComposite(composite, 1);

		projectPath = getCurrentProject();

		projectRoot = new DirectoryFieldEditor(CppStyleConstants.CPPLINT_PATH,
				"Root:", composite) {
			String errorMsg = super.getErrorMessage();

			@Override
			protected boolean doCheckState() {
				this.setErrorMessage(errorMsg);

				String fileName = getTextControl().getText();
				fileName = fileName.trim();
				if (fileName.length() == 0 && isEmptyStringAllowed()) {
					return true;
				}

				File file = new File(fileName);
				if (false == file.isDirectory()) {
					return false;
				}

				this.setErrorMessage("Directory or its up level directories should contain .git, .hg, or .svn.");

				String path = CpplintCheckSettings.getVersionControlRoot(file);

				if (path == null) {
					return false;
				}

				if (!path.startsWith(projectPath)) {
					this.setErrorMessage("Should be a subdirectory of project's root.");
					return false;
				}

				return true;
			}

		};

		projectRoot.setPage(this);
		projectRoot.setFilterPath(new File(projectPath));
		projectRoot.setPropertyChangeListener(this);
		projectRootText = projectRoot.getTextControl(composite);
		projectRootText.addModifyListener(this);
		projectRoot.setEnabled(true, composite);

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

		String root = getPropertyValueString(CppStyleConstants.CPPLINT_PROJECT_ROOT);
		projectRoot.setStringValue(root);
	}

	public static String getCurrentProject() {
		ISelectionService selectionService = Workbench.getInstance()
				.getActiveWorkbenchWindow().getSelectionService();

		ISelection selection = selectionService.getSelection();

		IProject project = null;
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection)
					.getFirstElement();

			if (element instanceof IResource) {
				project = ((IResource) element).getProject();
			}
		}

		if (project != null) {
			return project.getLocation().toOSString();
		}

		return null;
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(SWT.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);
		constructPage(composite);
		return composite;
	}

	private Composite createComposite(Composite parent, int ncols) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = ncols;
		composite.setLayout(layout);

		GridData data = new GridData(SWT.FILL);
		data.verticalAlignment = SWT.FILL;
		data.horizontalAlignment = SWT.FILL;
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		return composite;
	}

	private void createSepeerater(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	protected void performDefaults() {
		super.performDefaults();
		projectSpecificButton.setSelection(false);
		enableCpplintOnSaveButton.setSelection(false);
		enableCpplintOnSaveButton.setEnabled(false);
		enableClangFormatOnSaveButton.setSelection(false);
		enableClangFormatOnSaveButton.setEnabled(false);
		projectRoot.setStringValue("");
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

			((IResource) getElement()).setPersistentProperty(new QualifiedName(
					"", CppStyleConstants.CPPLINT_PROJECT_ROOT), projectRoot
					.getStringValue());
		} catch (CoreException e) {
			return false;
		}
		return true;
	}

	public String getPropertyValueString(String key) {
		String value;
		try {
			value = ((IResource) getElement())
					.getPersistentProperty(new QualifiedName("", key));
			if (value == null) {
				return "";
			}

		} catch (CoreException e) {
			return "";
		}

		return value;
	}

	public boolean getPropertyValue(String key) {
		return Boolean.parseBoolean(getPropertyValueString(key));
	}

	/**
	 * Creates a new preferences page and opens it
	 */
	public void configureWorkspaceSettings() {
		// create a new instance of the current class
		IPreferencePage page = new CppStylePerfPage();
		page.setTitle(getTitle());
		// and show it
		showPreferencePage(CppStyleConstants.PerfPageId, page);
	}

	/**
	 * Show a single preference pages
	 * 
	 * @param id
	 *            - the preference page identification
	 * @param page
	 *            - the preference page
	 */
	protected void showPreferencePage(String id, IPreferencePage page) {
		final IPreferenceNode targetNode = new PreferenceNode(id, page);
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog = new PreferenceDialog(getControl()
				.getShell(), manager);
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				dialog.open();
			}
		});
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

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE)) {
			if (event.getSource() == projectRoot) {
				setValid(projectRoot.isValid());
			}
		}
	}

	@Override
	public void modifyText(ModifyEvent e) {
		if (e.getSource() == projectRootText) {
			if (projectRootText.getText().isEmpty()) {
				setValid(true);
			}
		}
	}
}
