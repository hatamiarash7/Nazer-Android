package ir.hatamiarash.nazer;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Events {
	private static String TAG = Events.class.getName();
	
	public static void report(String event) {
		try {
			Networking networking = new Networking();
			
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("key", Nazer.getApiKey());
			jsonObject.put("body", event);
			jsonObject.put("app", Nazer.getPackageName());
			
			networking.sendRequest(jsonObject.toString(), "event");
		} catch (JSONException exception) {
			Log.e(TAG, exception.getMessage());
		}
	}
}
