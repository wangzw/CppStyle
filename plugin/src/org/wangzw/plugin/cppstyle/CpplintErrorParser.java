package org.wangzw.plugin.cppstyle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IErrorParser;
import org.eclipse.cdt.core.ProblemMarkerInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;

public class CpplintErrorParser implements IErrorParser {
	private static String PROBLEM_PREFIX = "org.wangzw.plugin.cppstyle.cpplint.";

	private static Pattern pattern = Pattern.compile(CppStyleConstants.CPPLINT_OUTPUT_PATTERN);

	private int findSeverityCode(String text) {
		return IMarker.SEVERITY_ERROR;
	}

	private String findProblemId(String category, String subcategory) {
		String id = PROBLEM_PREFIX + category + "." + subcategory;
		return id;
	}

	@Override
	public boolean processLine(String line, ErrorParserManager parserManager) {
		Matcher matcher = pattern.matcher(line);

		if (!matcher.matches()) {
			return false;
		}

		IFile fileName = parserManager.findFileName(matcher.group(CppStyleConstants.CPPLINT_OUTPUT_PATTERN_PATH_GROUP));

		if (fileName != null) {
			int lineNumber = Integer.parseInt(matcher.group(CppStyleConstants.CPPLINT_OUTPUT_PATTERN_LINE_NO_GROUP));
			lineNumber = lineNumber > 0 ? lineNumber : 1;
			String description = matcher.group(CppStyleConstants.CPPLINT_OUTPUT_PATTERN_MSG_GROUP);
			int severity = findSeverityCode(matcher.group(CppStyleConstants.CPPLINT_OUTPUT_PATTERN_SEVERITY_GROUP));
			ProblemMarkerInfo info = new ProblemMarkerInfo(fileName, lineNumber, description, severity, null);
			String category = matcher.group(CppStyleConstants.CPPLINT_OUTPUT_PATTERN_CATEGORY_GROUP);
			String subcate = matcher.group(CppStyleConstants.CPPLINT_OUTPUT_PATTERN_CATEGORY_SUBGROUP);
			String problem = findProblemId(category, subcate);
			int retry = 1;

			do {
				try {
					if (problem != null) {
						info.setAttribute(CppStyleConstants.CPPLINT_PROBLEM_ID_KEY, problem);
						parserManager.addProblemMarker(info);
						return true;
					}

					return false;
				} catch (IllegalArgumentException e) {
					CppStyle.log("Unexpected cpplint problem: " + category + "/" + subcate, e);
					problem = CppStyleConstants.CPPLINT_ERROR_PROBLEM_ID;
				}
			} while (retry-- > 0);
		}

		return false;
	}

}
