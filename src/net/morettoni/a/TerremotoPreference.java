package net.morettoni.a;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class TerremotoPreference extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
	}
}
