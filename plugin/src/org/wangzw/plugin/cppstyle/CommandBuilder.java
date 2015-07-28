package org.wangzw.plugin.cppstyle;

import java.io.File;

import org.eclipse.cdt.codan.core.cxx.externaltool.ArgsSeparator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Creates the command to use to invoke an external tool.
 */
public class CommandBuilder {
	Command buildCommand(InvocationParameters parameters, CpplintCheckSettings settings, ArgsSeparator argsSeparator) {
		IPath executablePath = executablePath(settings);
		String[] args = argsToPass(parameters, settings, argsSeparator);
		return new Command(executablePath, args);
	}

	private IPath executablePath(CpplintCheckSettings configurationSettings) {
		File executablePath = configurationSettings.getPath();
		return new Path(executablePath.toString());
	}

	private String[] argsToPass(InvocationParameters parameters, CpplintCheckSettings configurationSettings,
			ArgsSeparator argsSeparator) {
		String actualFilePath = parameters.getActualFilePath();
		String[] args = configuredArgs(configurationSettings, argsSeparator);
		return addFilePathToArgs(actualFilePath, args);
	}

	private String[] configuredArgs(CpplintCheckSettings settings, ArgsSeparator argsSeparator) {
		String args = settings.getArgs();
		return argsSeparator.splitArguments(args);
	}

	private String[] addFilePathToArgs(String actualFilePath, String[] configuredArgs) {
		int argCount = configuredArgs.length;
		String[] allArgs = new String[argCount + 1];
		allArgs[0] = actualFilePath;
		// Copy arguments
		System.arraycopy(configuredArgs, 0, allArgs, 1, argCount);
		return allArgs;
	}
}