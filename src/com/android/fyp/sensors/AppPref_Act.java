package com.android.fyp.sensors;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class AppPref_Act extends PreferenceActivity{
	public AppPref_Act() { }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.sensor_pref);
	}
	
	 
}
