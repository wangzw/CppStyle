package org.wangzw.plugin.cppstyle;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.cdt.codan.core.cxx.externaltool.ArgsSeparator;
import org.eclipse.cdt.codan.core.cxx.externaltool.InvocationFailure;
import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

/**
 * Invokes an external tool to perform checks on a single file.
 */
public class CpplintInvoker {
	private static final NullProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();
	private final CommandBuilder commandBuilder = new CommandBuilder();

	/**
	 * Invokes an external tool.
	 * 
	 * @param parameters
	 *            the parameters to pass to the external tool executable.
	 * @param settings
	 *            user-configurable settings.
	 * @param argsSeparator
	 *            separates the arguments to pass to the external tool
	 *            executable. These arguments are stored in a single
	 *            {@code String}.
	 * @param parsers
	 *            parse the output of the external tool.
	 * @throws InvocationFailure
	 *             if the external tool could not be invoked or if the external
	 *             tool itself reports that it cannot be executed (e.g. due to a
	 *             configuration error).
	 * @throws Throwable
	 *             if something else goes wrong.
	 */
	public void invoke(InvocationParameters parameters, CpplintCheckSettings settings, ArgsSeparator argsSeparator,
			IConsoleParser[] parsers) throws InvocationFailure, Throwable {
		Command command = commandBuilder.buildCommand(parameters, settings, argsSeparator);
		launchCommand(command, parsers, parameters, settings);
	}

	private void launchCommand(Command command, IConsoleParser[] parsers, InvocationParameters parameters,
			CpplintCheckSettings settings) throws InvocationFailure, CoreException {
		IProject project = parameters.getActualFile().getProject();
		final String toolName = settings.getExternalToolName();
		final IPath workingDirectory = parameters.getWorkingDirectory();
		CppStyleMessageConsole console = parameters.getConsole();
		final IPath commandPath = command.getPath();
		final String[] commandArgs = command.getArgs();
		final String[] commandEnv = command.getEnv();
		launchOnBuildConsole(project, console, parsers, toolName, commandPath, commandArgs, commandEnv,
				workingDirectory, NULL_PROGRESS_MONITOR);
	}

	public void launchOnBuildConsole(IProject project, CppStyleMessageConsole console, IConsoleParser[] parsers,
			final String toolName, final IPath commandPath, final String[] commandArgs, final String[] commandEnv,
			final IPath workingDirectory, final IProgressMonitor monitor) throws CoreException, InvocationFailure {
		monitor.beginTask("Launching " + toolName, 100);
		ConsoleOutputSniffer sniffer = new ConsoleOutputSniffer(console.getOutputStream(), console.getErrorStream(),
				parsers);
		final OutputStream out = sniffer.getOutputStream();
		final OutputStream err = sniffer.getErrorStream();

		console.getOutputStream().println("Launching " + toolName);

		try {
			ICommandLauncher launcher = new CommandLauncher();
			launcher.showCommand(true);
			launcher.setProject(project);
			Process p = launcher.execute(commandPath, commandArgs, commandEnv, workingDirectory,
					new SubProgressMonitor(monitor, 50));
			if (p == null) {
				String format = "Unable to launch external tool '%s': %s"; //$NON-NLS-1$
				throw new InvocationFailure(String.format(format, commandPath, launcher.getErrorMessage()));
			}
			try {
				// this is process input stream which we don't need
				p.getOutputStream().close();
			} catch (Throwable ignored) {
				// ignore
			}
			try {
				launcher.waitAndRead(out, err, new SubProgressMonitor(monitor, 50));
			} finally {
				p.destroy();
			}

		} finally {
			// closing sniffer's streams will shut down the parsers as well
			try {
				out.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				err.close();
			} catch (IOException e) {
				// ignore
			}
			monitor.done();

			console.getOutputStream().println("");
		}
	}
}
