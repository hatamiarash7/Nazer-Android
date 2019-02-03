package ir.hatamiarash.nazer;

import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import static ir.hatamiarash.nazer.helpers.FormatHelper.fixResponse;

class Networking {
	private static String TAG = Networking.class.getName();
	
	Networking() {
	}
	
	void sendRequest(final String body, String type) {
		StringRequest request = new StringRequest(Request.Method.POST, "http://n.arash-hatami.ir/api/android/" + type, new Response.Listener<String>() {
			@Override
			public void onResponse(String response) {
				try {
					response = fixResponse(response);
					JSONObject jsonObject = new JSONObject(response);
					Boolean error = jsonObject.getBoolean("error");
					if (error)
						Log.w(TAG, jsonObject.getString("error_msg"));
					else
						Log.w(TAG, "Event Reported");
				} catch (JSONException e) {
					Log.w(TAG, e.getMessage());
				}
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError volleyError) {
				Log.w(TAG, "Event Not Reported");
				if (volleyError != null && volleyError.getMessage() != null)
					if (!volleyError.getMessage().isEmpty())
						Log.w(TAG, volleyError.getMessage());
			}
		}) {
			@NotNull
			@Contract(pure = true)
			@Override
			public String getBodyContentType() {
				return "application/json; charset=utf-8";
			}
			
			@Contract(pure = true)
			@Nullable
			@Override
			public byte[] getBody() {
				try {
					return body.getBytes("utf-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return null;
				}
			}
		};
		request.setRetryPolicy(new DefaultRetryPolicy(
				0,
				DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
				DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
		));
		Nazer.addToRequestQueue(request);
	}
}
