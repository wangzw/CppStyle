package org.wangzw.plugin.cppstyle;

import org.eclipse.core.runtime.IPath;

/**
 * The command to execute to invoke an external tool.
 */
class Command {
	private final IPath path;
	private final String[] args;
	private final String[] env;

	Command(IPath path, String[] args) {
		this(path, args, new String[] {});
	}

	Command(IPath path, String[] args, String[] env) {
		this.path = path;
		this.args = args;
		this.env = env;
	}

	IPath getPath() {
		return path;
	}

	String[] getArgs() {
		return args;
	}

	String[] getEnv() {
		return env;
	}
}
