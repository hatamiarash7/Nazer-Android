package ir.hatamiarash.nazer_sample;


import android.app.Application;

import ir.hatamiarash.nazer.Nazer;
import ir.hatamiarash.nazer.config.NazerConfig;

public class App extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		Nazer.initialize(this);
		NazerConfig.Builder.create().apply();
	}
}
