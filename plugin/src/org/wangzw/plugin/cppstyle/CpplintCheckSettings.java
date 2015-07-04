package org.wangzw.plugin.cppstyle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;

/**
 * User-configurable external tool settings.
 */
public final class CpplintCheckSettings {
	private File path;
	private String args;
	private final String externalToolName;

	/**
	 * Constructor.
	 * 
	 * @param externalToolName
	 */
	public CpplintCheckSettings() {
		this.externalToolName = "cpplint";
		this.path = null;
		this.args = null;
	}

	/**
	 * Returns the name of the external tool, to be displayed to the user.
	 * 
	 * @return the name of the external tool, to be displayed to the user.
	 */
	public String getExternalToolName() {
		return externalToolName;
	}

	/**
	 * Returns the setting that specifies the path and name of the external tool
	 * to invoke.
	 * 
	 * @return the setting that specifies the path and name of the external tool
	 *         to invoke.
	 */
	public File getPath() {
		return path;
	}

	/**
	 * Returns the setting that specifies the arguments to pass when invoking
	 * the external tool.
	 * 
	 * @return the setting that specifies the arguments to pass when invoking
	 *         the external tool.
	 */
	public String getArgs() {
		return args;
	}

	/**
	 * Updates the values of the configuration settings value with the ones
	 * stored in the given preference map.
	 * 
	 * @param preferences
	 *            the given preference map that may contain the values to set.
	 * @throws ClassCastException
	 *             if any of the values to set is not of the same type as the
	 *             one supported by a setting.
	 */
	public void updateValuesFrom(IFile file) {
		path = new File(CppStyle.getCpplintPath());
		args = prepareParameters(file);
	}

	private String prepareParameters(IFile file) {
		String root = getCpplintRoot(file);

		List<String> commands = new ArrayList<String>();

		if (root != null && !root.isEmpty()) {
			commands.add("--root=" + root);
		}

		StringBuffer sb = new StringBuffer();

		for (String arg : commands) {
			sb.append(arg);
			sb.append(' ');
		}

		return sb.toString();
	}

	static String getVersionControlRoot(IFile file) {
		File current = file.getLocation().toFile();

		File dir = current.getParentFile();

		return getVersionControlRoot(dir);
	}

	public static String getVersionControlRoot(File dir) {

		while (dir != null) {
			for (final File entry : dir.listFiles()) {
				String name = entry.getName();
				if (name.equals(".git") || name.equals(".hg") || name.equals(".svn")) {
					return dir.getPath();
				}
			}

			dir = dir.getParentFile();
		}

		return null;
	}

	public static String getCpplintRoot(IFile file) {
		IProject project = file.getProject();
		String rootSpec;
		try {
			rootSpec = project.getPersistentProperty(new QualifiedName("", CppStyleConstants.CPPLINT_PROJECT_ROOT));

			if (rootSpec == null || rootSpec.isEmpty()) {
				return null;
			}
		} catch (CoreException e) {
			return null;
		}

		String rootVc = getVersionControlRoot(file);

		if (rootVc == null) {
			return null;
		}

		String relative = new File(rootVc).toURI().relativize(new File(rootSpec).toURI()).getPath();

		if (relative.endsWith("" + Path.SEPARATOR)) {
			return relative.substring(0, relative.length() - 1);
		}

		return relative;
	}

	private static boolean enableCpplint(IResource resource) {
		boolean enable = CppStyle.getDefault().getPreferenceStore()
				.getBoolean(CppStyleConstants.ENABLE_CPPLINT_ON_SAVE);

		try {
			IProject project = resource.getProject();
			String enableProjectSpecific = project
					.getPersistentProperty(new QualifiedName("", CppStyleConstants.PROJECTS_PECIFIC_PROPERTY));

			if (enableProjectSpecific != null && Boolean.parseBoolean(enableProjectSpecific)) {
				String value = project
						.getPersistentProperty(new QualifiedName("", CppStyleConstants.ENABLE_CPPLINT_PROPERTY));
				if (value != null) {
					return Boolean.parseBoolean(value);
				}

				return false;
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return enable;
	}

	public static boolean checkCpplint(IResource resource) {
		if (!enableCpplint(resource)) {
			return false;
		}

		String cpplint = CppStyle.getCpplintPath();

		if (cpplint == null) {
			CppStyle.log("cpplint.py is not specified.");
			return false;
		}

		File file = new File(cpplint);

		if (!file.exists()) {
			CppStyle.log("cpplint.py (" + cpplint + ") does not exist.");
			return false;
		}

		if (!file.canExecute()) {
			CppStyle.log("cpplint.py (" + cpplint + ") is not executable.");
			return false;
		}

		return true;
	}

}
