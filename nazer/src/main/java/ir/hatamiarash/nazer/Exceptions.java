package ir.hatamiarash.nazer;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import ir.hatamiarash.nazer.models.ExceptionModel;

import static ir.hatamiarash.nazer.Nazer.getPackageName;

class Exceptions {
	private static String TAG = Exceptions.class.getName();
	
	static void report(@NotNull ExceptionModel exception) {
		try {
			Networking networking = new Networking();
			
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("key", Nazer.getApiKey());
			jsonObject.put("app", getPackageName());
			jsonObject.put("body", exception.getStackTrace());
			jsonObject.put("message", exception.getMessage());
			jsonObject.put("version_name", exception.getVersionName());
			jsonObject.put("version_code", exception.getVersionCode());
			jsonObject.put("is_root", exception.getIsRoot());
			jsonObject.put("is_background", exception.getIsBackground());
			jsonObject.put("activity", exception.getActivity());
			jsonObject.put("method", exception.getMethod());
			jsonObject.put("line", exception.getLine());
			
			networking.sendRequest(jsonObject.toString(), "exception");
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		}
	}
}
