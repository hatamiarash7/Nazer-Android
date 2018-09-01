package ir.hatamiarash.nazer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Nazer {
	private static String CLASS = Nazer.class.getName();
	
	private static RequestQueue mRequestQueue;
	
	private static String ApiKey = "null";
	private static Context mContext;
	private static String packageName;
	
	public static void initialize(@NotNull Context context) {
		mContext = context;
		packageName = context.getPackageName();
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			ApiKey = bundle.getString("ir.hatamiarash.nazer.ApiKey");
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(CLASS, "Failed to load meta-data, NameNotFound: " + e.getMessage());
		} catch (NullPointerException e) {
			Log.e(CLASS, "Failed to load meta-data, NullPointer: " + e.getMessage());
		}
	}
	
	@Contract(pure = true)
	public static String getApiKey() {
		return ApiKey;
	}
	
	@Contract(pure = true)
	public static String getPackageName() {
		return packageName;
	}
	
	private static RequestQueue getRequestQueue() {
		if (mRequestQueue == null) {
			mRequestQueue = Volley.newRequestQueue(mContext);
		}
		return mRequestQueue;
	}
	
	public static <T> void addToRequestQueue(@NotNull Request<T> req) {
		req.setTag(CLASS);
		getRequestQueue().add(req);
	}
}
