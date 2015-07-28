package org.wangzw.plugin.cppstyle;

import java.net.URI;

import org.eclipse.cdt.codan.core.CodanRuntime;
import org.eclipse.cdt.codan.core.cxx.externaltool.ArgsSeparator;
import org.eclipse.cdt.codan.core.cxx.externaltool.InvocationFailure;
import org.eclipse.cdt.codan.core.model.AbstractCheckerWithProblemPreferences;
import org.eclipse.cdt.codan.core.model.CheckerLaunchMode;
import org.eclipse.cdt.codan.core.model.IProblem;
import org.eclipse.cdt.codan.core.model.IProblemLocation;
import org.eclipse.cdt.codan.core.model.IProblemLocationFactory;
import org.eclipse.cdt.codan.core.model.IProblemWorkingCopy;
import org.eclipse.cdt.codan.core.param.FileScopeProblemPreference;
import org.eclipse.cdt.codan.core.param.MapProblemPreference;
import org.eclipse.cdt.codan.core.param.RootProblemPreference;
import org.eclipse.cdt.codan.core.param.SharedRootProblemPreference;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.ProblemMarkerInfo;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

public class CpplintChecker extends AbstractCheckerWithProblemPreferences implements IMarkerGenerator {
	private final ArgsSeparator argsSeparator;
	private final CpplintCheckSettings settings;
	private final CpplintInvoker invoker;
	private final RootProblemPreference preferences;

	/**
	 * Constructor.
	 */
	public CpplintChecker() {
		this.argsSeparator = new ArgsSeparator();
		this.settings = new CpplintCheckSettings();
		this.invoker = new CpplintInvoker();
		this.preferences = new SharedRootProblemPreference();
	}

	/**
	 * Returns {@code false} because this checker cannot run "as you type" by
	 * default.
	 * 
	 * @return {@code false}.
	 */
	@Override
	public boolean runInEditor() {
		return false;
	}

	@Override
	public boolean processResource(IResource resource) {
		if (!shouldProduceProblems(resource)) {
			return false;
		}

		if (!CpplintCheckSettings.checkCpplint(resource)) {
			return false;
		}

		process(resource);
		return false;
	}

	private void process(IResource resource) {
		try {
			CppStyleMessageConsole console = CppStyle.buildConsole();
			console.getListener().setFile((IFile) resource);
			String path = resource.getLocation().toOSString();
			InvocationParameters parameters = new InvocationParameters(resource, resource, path, null, console);
			if (parameters != null) {
				invokeExternalTool(parameters);
			}
		} catch (Throwable error) {
			logResourceProcessingFailure(error, resource);
		}
	}

	private void invokeExternalTool(InvocationParameters parameters) throws Throwable {
		updateConfigurationSettingsFromPreferences(parameters.getActualFile());
		IConsoleParser[] parsers = new IConsoleParser[] { createErrorParserManager(parameters) };
		try {
			invoker.invoke(parameters, settings, argsSeparator, parsers);
		} catch (InvocationFailure error) {
			handleInvocationFailure(error, parameters);
		}
	}

	private void updateConfigurationSettingsFromPreferences(IResource fileToProcess) {
		settings.updateValuesFrom((IFile) fileToProcess);
	}

	private ErrorParserManager createErrorParserManager(InvocationParameters parameters) {
		IProject project = parameters.getActualFile().getProject();
		URI workingDirectory = URIUtil.toURI(parameters.getWorkingDirectory());
		return new ErrorParserManager(project, workingDirectory, this, getParserIDs());
	}

	/**
	 * @return the IDs of the parsers to use to parse the output of the external
	 *         tool.
	 */
	protected String[] getParserIDs() {
		return new String[] { CppStyleConstants.CPPLINT_OUTPUT_PARSER_ID };
	}

	/**
	 * Handles a failure reported when invoking the external tool. This
	 * implementation simply logs the failure.
	 * 
	 * @param error
	 *            the reported failure.
	 * @param parameters
	 *            the parameters passed to the external tool executable.
	 */
	protected void handleInvocationFailure(InvocationFailure error, InvocationParameters parameters) {
		logResourceProcessingFailure(error, parameters.getActualFile());
	}

	private void logResourceProcessingFailure(Throwable error, IResource resource) {
		String location = resource.getLocation().toOSString();
		String msg = String.format("Unable to process resource %s", location); //$NON-NLS-1$
		CppStyle.log(msg, error);
	}

	/**
	 * Returns the id of the problem used as reference to obtain this checker's
	 * preferences. All preferences in a external-tool-based checker are shared
	 * among its defined problems.
	 * 
	 * @return the id of the problem used as reference to obtain this checker's
	 *         preferences.
	 */
	protected String getReferenceProblemId() {
		return CppStyleConstants.CPPLINT_ERROR_PROBLEM_ID;
	}

	@Override
	public void initPreferences(IProblemWorkingCopy problem) {
		getTopLevelPreference(problem); // initialize

//		FileScopeProblemPreference scope = getScopePreference(problem);
//		Path[] value = new Path[5];
//		value[0] = new Path("*.cc");
//		value[1] = new Path("*.h");
//		value[2] = new Path("*.cpp");
//		value[3] = new Path("*.cu");
//		value[4] = new Path("*.cuh");
//		scope.setAttribute(FileScopeProblemPreference.INCLUSION, value);

		getLaunchModePreference(problem).enableInLaunchModes(CheckerLaunchMode.RUN_ON_FILE_SAVE,
				CheckerLaunchMode.RUN_ON_DEMAND);
	}

	@Override
	protected void setDefaultPreferenceValue(IProblemWorkingCopy problem, String key, Object defaultValue) {
		MapProblemPreference map = getTopLevelPreference(problem);
		map.setChildValue(key, defaultValue);
	}

	@Override
	public RootProblemPreference getTopLevelPreference(IProblem problem) {
		RootProblemPreference map = (RootProblemPreference) problem.getPreference();
		if (map == null) {
			map = preferences;
			if (problem instanceof IProblemWorkingCopy) {
				((IProblemWorkingCopy) problem).setPreference(map);
			}
		}
		return map;
	}

	@Deprecated
	@Override
	public void addMarker(IResource file, int lineNumber, String description, int severity, String variableName) {
		addMarker(new ProblemMarkerInfo(file, lineNumber, description, severity, variableName));
	}

	@Override
	public void addMarker(ProblemMarkerInfo info) {
		String problemId = info.getAttribute(CppStyleConstants.CPPLINT_PROBLEM_ID_KEY);
		reportProblem(problemId, createProblemLocation(info), info.description);
	}

	protected IProblemLocation createProblemLocation(ProblemMarkerInfo info) {
		IProblemLocationFactory factory = CodanRuntime.getInstance().getProblemLocationFactory();
		return factory.createProblemLocation((IFile) info.file, info.startChar, info.endChar, info.lineNumber);
	}
}
