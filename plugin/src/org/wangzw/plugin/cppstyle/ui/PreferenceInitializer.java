package org.wangzw.plugin.cppstyle.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.wangzw.plugin.cppstyle.CppStyle;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public void initializeDefaultPreferences() {
		IPreferenceStore store = CppStyle.getDefault().getPreferenceStore();
		store.setDefault(CppStyleConstants.CLANG_FORMAT_PATH,
				findBinaryPath("clang-format"));
		store.setDefault(CppStyleConstants.CPPLINT_PATH,
				findBinaryPath("cpplint.py"));
		store.setDefault(CppStyleConstants.ENABLE_CPPLINT_ON_SAVE, false);
		store.setDefault(CppStyleConstants.ENABLE_CLANGFORMAT_ON_SAVE, false);
	}

	private String findBinaryPath(String bin) {
		try {
			Process process = Runtime.getRuntime().exec("which " + bin);

			if (process.waitFor() == 0) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
				return reader.readLine();
			}

			return "";
		} catch (IOException e) {
		} catch (InterruptedException e) {
		}

		return "";
	}
}
