package org.wangzw.plugin.cppstyle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wangzw.plugin.cppstyle.ui.CppStyleConsolePatternMatchListener;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

/**
 * The activator class controls the plug-in life cycle
 */
public class CppStyle extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "cppstyle"; //$NON-NLS-1$

	// The shared instance
	private static CppStyle plugin;

	/**
	 * The constructor
	 */
	public CppStyle() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static String getCpplintPath() {
		return plugin.getPreferenceStore().getString(CppStyleConstants.CPPLINT_PATH);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CppStyle getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * Logs the specified status with this plug-in's log.
	 *
	 * @param status
	 *            status to log
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	/**
	 * Logs an internal error with the specified {@code Throwable}.
	 *
	 * @param t
	 *            the {@code Throwable} to be logged
	 */
	public static void log(Throwable t) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 1, "Internal Error", t)); //$NON-NLS-1$
	}

	/**
	 * Logs an internal error with the specified message.
	 *
	 * @param message
	 *            the error message to log
	 */
	public static void log(String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 1, message, null));
	}

	/**
	 * Logs an internal error with the specified message and {@code Throwable}.
	 *
	 * @param message
	 *            the error message to log
	 * @param t
	 *            the {@code Throwable} to be logged
	 *
	 */
	public static void log(String message, Throwable t) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 1, message, t));
	}

	public static CppStyleMessageConsole buildConsole() {
		CppStyleMessageConsole console = null;
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();

		for (int i = 0; i < existing.length; i++) {
			if (CppStyleConstants.CONSOLE_NAME.equals(existing[i].getName())) {
				console = (CppStyleMessageConsole) existing[i];
			}
		}

		if (console == null) {
			// no console found, so create a new one
			CppStyleConsolePatternMatchListener listener = new CppStyleConsolePatternMatchListener();
			console = new CppStyleMessageConsole(listener);
			conMan.addConsoles(new IConsole[] { console });
		}

		return console;
	}
}
