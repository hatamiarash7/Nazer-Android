package ir.hatamiarash.nazer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ir.hatamiarash.nazer.config.NazerConfig;
import ir.hatamiarash.nazer.helpers.Helper;
import ir.hatamiarash.nazer.models.ExceptionModel;

public class Nazer {
	private static String CLASS = Nazer.class.getName();
	private static String TAG = Nazer.class.getName();
	
	private static RequestQueue mRequestQueue;
	
	private static String ApiKey = "null";
	private static Context mContext;
	private static String packageName;
	
	
	//Extras passed to the error activity
	private static final String EXTRA_CONFIG = "ir.hatamiarash.nazer.EXTRA_CONFIG";
	private static final String EXTRA_STACK_TRACE = "ir.hatamiarash.nazer.EXTRA_STACK_TRACE";
	private static final String EXTRA_ACTIVITY_LOG = "ir.hatamiarash.nazer.EXTRA_ACTIVITY_LOG";
	
	//General constants
	private static final String INTENT_ACTION_ERROR_ACTIVITY = "ir.hatamiarash.nazer.ERROR";
	private static final String INTENT_ACTION_RESTART_ACTIVITY = "ir.hatamiarash.nazer.RESTART";
	private static final String NAZER_HANDLER_PACKAGE_NAME = "ir.hatamiarash.nazer";
	private static final String DEFAULT_HANDLER_PACKAGE_NAME = "com.android.internal.os";
	private static final int MAX_STACK_TRACE_SIZE = 131071; //128 KB - 1
	private static final int MAX_ACTIVITIES_IN_LOG = 50;
	
	//Shared preferences
	private static final String SHARED_PREFERENCES_FILE = "custom_activity_on_crash";
	private static final String SHARED_PREFERENCES_FIELD_TIMESTAMP = "last_crash_timestamp";
	
	//Internal variables
	@SuppressLint("StaticFieldLeak") //This is an application-wide component
	private static Application application;
	private static NazerConfig config = new NazerConfig();
	private static final Deque<String> activityLog = new ArrayDeque<>(MAX_ACTIVITIES_IN_LOG);
	private static WeakReference<Activity> lastActivityCreated = new WeakReference<>(null);
	private static boolean isInBackground = true;
	
	
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
	
	@RestrictTo(RestrictTo.Scope.LIBRARY)
	public static void install(@Nullable final Context context) {
		try {
			if (context == null) {
				Log.e(TAG, "Install failed: context is null!");
			} else {
				//INSTALL!
				final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
				
				if (oldHandler != null && oldHandler.getClass().getName().startsWith(NAZER_HANDLER_PACKAGE_NAME)) {
					Log.e(TAG, "Nazer was already installed, doing nothing!");
				} else {
					if (oldHandler != null && !oldHandler.getClass().getName().startsWith(DEFAULT_HANDLER_PACKAGE_NAME)) {
						Log.e(TAG, "IMPORTANT WARNING! You already have an UncaughtExceptionHandler, are you sure this is correct? If you use a custom UncaughtExceptionHandler, you must initialize it AFTER Nazer! Installing anyway, but your original handler will not be called.");
					}
					
					application = (Application) context.getApplicationContext();
					
					//We define a default exception handler that does what we want so it can be called from Crashlytics/ACRA
					Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
						@Override
						public void uncaughtException(Thread thread, final Throwable throwable) {
							if (config.isEnabled()) {
								StringWriter sw = new StringWriter();
								PrintWriter pw = new PrintWriter(sw);
								throwable.printStackTrace(pw);
								String stackTraceString = sw.toString();
								if (stackTraceString.length() > MAX_STACK_TRACE_SIZE) {
									String disclaimer = " [stack trace too large]";
									stackTraceString = stackTraceString.substring(0, MAX_STACK_TRACE_SIZE - disclaimer.length()) + disclaimer;
								}
								
								StringBuilder activityLogStringBuilder = new StringBuilder();
								while (!activityLog.isEmpty()) {
									activityLogStringBuilder.append(activityLog.poll());
								}
								
								String message = throwable.getMessage();
								
								String vCode = "null";
								String vName = "null";
								try {
									PackageInfo pInfo = application.getPackageManager().getPackageInfo(getPackageName(), 0);
									vName = pInfo.versionName;
									vCode = String.valueOf(pInfo.versionCode);
								} catch (Exception ignore) {
								}
								
								int isRoot = Helper.isRooted();
								
								int isBackground = (isInBackground) ? 1 : 0;
								
								String activity = throwable.getStackTrace()[0].getClassName();
								
								int line = throwable.getStackTrace()[0].getLineNumber();
								
								String method = throwable.getStackTrace()[0].getMethodName();
								
								Exceptions.report(new ExceptionModel(stackTraceString, activity, method, message, vCode, vName, isRoot, isBackground, line));
								
								if (oldHandler != null)
									oldHandler.uncaughtException(thread, throwable);
							} else if (oldHandler != null) {
								oldHandler.uncaughtException(thread, throwable);
							}
						}
					});
					application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
						int currentlyStartedActivities = 0;
						final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
						
						@Override
						public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
							if (activity.getClass() != config.getErrorActivityClass()) {
								// Ignore activityClass because we want the last
								// application Activity that was started so that we can
								// explicitly kill it off.
								lastActivityCreated = new WeakReference<>(activity);
							}
							if (config.isTrackActivities()) {
								activityLog.add(dateFormat.format(new Date()) + ": " + activity.getClass().getSimpleName() + " created\n");
							}
						}
						
						@Override
						public void onActivityStarted(Activity activity) {
							currentlyStartedActivities++;
							isInBackground = (currentlyStartedActivities == 0);
							//Do nothing
						}
						
						@Override
						public void onActivityResumed(Activity activity) {
							if (config.isTrackActivities()) {
								activityLog.add(dateFormat.format(new Date()) + ": " + activity.getClass().getSimpleName() + " resumed\n");
							}
						}
						
						@Override
						public void onActivityPaused(Activity activity) {
							if (config.isTrackActivities()) {
								activityLog.add(dateFormat.format(new Date()) + ": " + activity.getClass().getSimpleName() + " paused\n");
							}
						}
						
						@Override
						public void onActivityStopped(Activity activity) {
							//Do nothing
							currentlyStartedActivities--;
							isInBackground = (currentlyStartedActivities == 0);
						}
						
						@Override
						public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
							//Do nothing
						}
						
						@Override
						public void onActivityDestroyed(Activity activity) {
							if (config.isTrackActivities()) {
								activityLog.add(dateFormat.format(new Date()) + ": " + activity.getClass().getSimpleName() + " destroyed\n");
							}
						}
					});
				}
				
				Log.i(TAG, "Nazer has been installed.");
			}
		} catch (Throwable t) {
			Log.e(TAG, "An unknown error occurred while installing Nazer, it may not have been properly initialized. Please report this as a bug if needed.", t);
		}
	}
	
	/**
	 * Given an Intent, returns the stack trace extra from it.
	 *
	 * @param intent The Intent. Must not be null.
	 * @return The stacktrace, or null if not provided.
	 */
	@Nullable
	public static String getStackTraceFromIntent(@NonNull Intent intent) {
		return intent.getStringExtra(Nazer.EXTRA_STACK_TRACE);
	}
	
	/**
	 * Given an Intent, returns the config extra from it.
	 *
	 * @param intent The Intent. Must not be null.
	 * @return The config, or null if not provided.
	 */
	@Nullable
	public static NazerConfig getConfigFromIntent(@NonNull Intent intent) {
		NazerConfig config = (NazerConfig) intent.getSerializableExtra(Nazer.EXTRA_CONFIG);
		if (config.isLogErrorOnRestart()) {
			String stackTrace = getStackTraceFromIntent(intent);
			if (stackTrace != null) {
				Log.e(TAG, "The previous app process crashed. This is the stack trace of the crash:\n" + getStackTraceFromIntent(intent));
			}
		}
		
		return config;
	}
	
	/**
	 * Given an Intent, returns the activity log extra from it.
	 *
	 * @param intent The Intent. Must not be null.
	 * @return The activity log, or null if not provided.
	 */
	@Nullable
	public static String getActivityLogFromIntent(@NonNull Intent intent) {
		return intent.getStringExtra(Nazer.EXTRA_ACTIVITY_LOG);
	}
	
	/**
	 * Given an Intent, returns several error details including the stack trace extra from the intent.
	 *
	 * @param context A valid context. Must not be null.
	 * @param intent  The Intent. Must not be null.
	 * @return The full error details.
	 */
	@NonNull
	public static String getAllErrorDetailsFromIntent(@NonNull Context context, @NonNull Intent intent) {
		//I don't think that this needs localization because it's a development string...
		
		Date currentDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		
		//Get build date
		String buildDateAsString = getBuildDateAsString(context, dateFormat);
		
		//Get app version
		String versionName = getVersionName(context);
		
		String errorDetails = "";
		
		errorDetails += "Build version: " + versionName + " \n";
		if (buildDateAsString != null) {
			errorDetails += "Build date: " + buildDateAsString + " \n";
		}
		errorDetails += "Current date: " + dateFormat.format(currentDate) + " \n";
		//Added a space between line feeds to fix #18.
		//Ideally, we should not use this method at all... It is only formatted this way because of coupling with the default error activity.
		//We should move it to a method that returns a bean, and let anyone format it as they wish.
		errorDetails += "Device: " + getDeviceModelName() + " \n \n";
		errorDetails += "Stack trace:  \n";
		errorDetails += getStackTraceFromIntent(intent);
		
		String activityLog = getActivityLogFromIntent(intent);
		
		if (activityLog != null) {
			errorDetails += "\nUser actions: \n";
			errorDetails += activityLog;
		}
		return errorDetails;
	}
	
	/**
	 * Given an Intent, restarts the app and launches a startActivity to that intent.
	 * The flags NEW_TASK and CLEAR_TASK are set if the Intent does not have them, to ensure
	 * the app stack is fully cleared.
	 * If an event listener is provided, the restart app event is invoked.
	 * Must only be used from your error activity.
	 *
	 * @param activity The current error activity. Must not be null.
	 * @param intent   The Intent. Must not be null.
	 * @param config   The config object as obtained by calling getConfigFromIntent.
	 */
	public static void restartApplicationWithIntent(@NonNull Activity activity, @NonNull Intent intent, @NonNull NazerConfig config) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		if (intent.getComponent() != null) {
			//If the class name has been set, we force it to simulate a Launcher launch.
			//If we don't do this, if you restart from the error activity, then press home,
			//and then launch the activity from the launcher, the main activity appears twice on the backstack.
			//This will most likely not have any detrimental effect because if you set the Intent component,
			//if will always be launched regardless of the actions specified here.
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
		}
		if (config.getEventListener() != null) {
			config.getEventListener().onRestartAppFromErrorActivity();
		}
		activity.finish();
		activity.startActivity(intent);
		killCurrentProcess();
	}
	
	public static void restartApplication(@NonNull Activity activity, @NonNull NazerConfig config) {
		Intent intent = new Intent(activity, config.getRestartActivityClass());
		restartApplicationWithIntent(activity, intent, config);
	}
	
	/**
	 * Closes the app.
	 * If an event listener is provided, the close app event is invoked.
	 * Must only be used from your error activity.
	 *
	 * @param activity The current error activity. Must not be null.
	 * @param config   The config object as obtained by calling getConfigFromIntent.
	 */
	public static void closeApplication(@NonNull Activity activity, @NonNull NazerConfig config) {
		if (config.getEventListener() != null) {
			config.getEventListener().onCloseAppFromErrorActivity();
		}
		activity.finish();
		killCurrentProcess();
	}
	
	/// INTERNAL METHODS NOT TO BE USED BY THIRD PARTIES
	
	/**
	 * INTERNAL method that returns the current configuration of the library.
	 * If you want to check the config, use NazerConfig.Builder.get();
	 *
	 * @return the current configuration
	 */
	@Contract(pure = true)
	@RestrictTo(RestrictTo.Scope.LIBRARY)
	@NonNull
	public static NazerConfig getConfig() {
		return config;
	}
	
	/**
	 * INTERNAL method that sets the configuration of the library.
	 * You must not use this, use NazerConfig.Builder.apply()
	 *
	 * @param config the configuration to use
	 */
	@RestrictTo(RestrictTo.Scope.LIBRARY)
	public static void setConfig(@NonNull NazerConfig config) {
		Nazer.config = config;
	}
	
	/**
	 * INTERNAL method that checks if the stack trace that just crashed is conflictive. This is true in the following scenarios:
	 * - The application has crashed while initializing (handleBindApplication is in the stack)
	 * - The error activity has crashed (activityClass is in the stack)
	 *
	 * @param throwable     The throwable from which the stack trace will be checked
	 * @param activityClass The activity class to launch when the app crashes
	 * @return true if this stack trace is conflictive and the activity must not be launched, false otherwise
	 */
	private static boolean isStackTraceLikelyConflictive(@NonNull Throwable throwable, @NonNull Class<? extends Activity> activityClass) {
		do {
			StackTraceElement[] stackTrace = throwable.getStackTrace();
			for (StackTraceElement element : stackTrace) {
				if ((element.getClassName().equals("android.app.ActivityThread") && element.getMethodName().equals("handleBindApplication")) || element.getClassName().equals(activityClass.getName())) {
					return true;
				}
			}
		} while ((throwable = throwable.getCause()) != null);
		return false;
	}
	
	/**
	 * INTERNAL method that returns the build date of the current APK as a string, or null if unable to determine it.
	 *
	 * @param context    A valid context. Must not be null.
	 * @param dateFormat DateFormat to use to convert from Date to String
	 * @return The formatted date, or "Unknown" if unable to determine it.
	 */
	@Nullable
	private static String getBuildDateAsString(@NonNull Context context, @NonNull DateFormat dateFormat) {
		long buildDate;
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
			ZipFile zf = new ZipFile(ai.sourceDir);
			
			//If this failed, try with the old zip method
			ZipEntry ze = zf.getEntry("classes.dex");
			buildDate = ze.getTime();
			
			
			zf.close();
		} catch (Exception e) {
			buildDate = 0;
		}
		
		if (buildDate > 312764400000L) {
			return dateFormat.format(new Date(buildDate));
		} else {
			return null;
		}
	}
	
	/**
	 * INTERNAL method that returns the version name of the current app, or null if unable to determine it.
	 *
	 * @param context A valid context. Must not be null.
	 * @return The version name, or "Unknown if unable to determine it.
	 */
	@NonNull
	private static String getVersionName(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionName;
		} catch (Exception e) {
			return "Unknown";
		}
	}
	
	/**
	 * INTERNAL method that returns the device model name with correct capitalization.
	 * Taken from: http://stackoverflow.com/a/12707479/1254846
	 *
	 * @return The device model name (i.e., "LGE Nexus 5")
	 */
	@NonNull
	private static String getDeviceModelName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return capitalize(model);
		} else {
			return capitalize(manufacturer) + " " + model;
		}
	}
	
	/**
	 * INTERNAL method that capitalizes the first character of a string
	 *
	 * @param s The string to capitalize
	 * @return The capitalized string
	 */
	@NonNull
	private static String capitalize(@Nullable String s) {
		if (s == null || s.length() == 0) {
			return "";
		}
		char first = s.charAt(0);
		if (Character.isUpperCase(first)) {
			return s;
		} else {
			return Character.toUpperCase(first) + s.substring(1);
		}
	}
	
	/**
	 * INTERNAL method used to guess which activity must be called from the error activity to restart the app.
	 * It will first get activities from the AndroidManifest with intent filter <action android:name="ir.hatamiarash.nazer.RESTART" />,
	 * if it cannot find them, then it will get the default launcher.
	 * If there is no default launcher, this returns null.
	 *
	 * @param context A valid context. Must not be null.
	 * @return The guessed restart activity class, or null if no suitable one is found
	 */
	@Nullable
	private static Class<? extends Activity> guessRestartActivityClass(@NonNull Context context) {
		Class<? extends Activity> resolvedActivityClass;
		
		//If action is defined, use that
		resolvedActivityClass = getRestartActivityClassWithIntentFilter(context);
		
		//Else, get the default launcher activity
		if (resolvedActivityClass == null) {
			resolvedActivityClass = getLauncherActivity(context);
		}
		
		return resolvedActivityClass;
	}
	
	/**
	 * INTERNAL method used to get the first activity with an intent-filter <action android:name="ir.hatamiarash.nazer.RESTART" />,
	 * If there is no activity with that intent filter, this returns null.
	 *
	 * @param context A valid context. Must not be null.
	 * @return A valid activity class, or null if no suitable one is found
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private static Class<? extends Activity> getRestartActivityClassWithIntentFilter(@NonNull Context context) {
		Intent searchedIntent = new Intent().setAction(INTENT_ACTION_RESTART_ACTIVITY).setPackage(context.getPackageName());
		List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(searchedIntent,
				PackageManager.GET_RESOLVED_FILTER);
		
		if (resolveInfos != null && resolveInfos.size() > 0) {
			ResolveInfo resolveInfo = resolveInfos.get(0);
			try {
				return (Class<? extends Activity>) Class.forName(resolveInfo.activityInfo.name);
			} catch (ClassNotFoundException e) {
				//Should not happen, print it to the log!
				Log.e(TAG, "Failed when resolving the restart activity class via intent filter, stack trace follows!", e);
			}
		}
		
		return null;
	}
	
	/**
	 * INTERNAL method used to get the default launcher activity for the app.
	 * If there is no launchable activity, this returns null.
	 *
	 * @param context A valid context. Must not be null.
	 * @return A valid activity class, or null if no suitable one is found
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private static Class<? extends Activity> getLauncherActivity(@NonNull Context context) {
		Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
		if (intent != null && intent.getComponent() != null) {
			try {
				return (Class<? extends Activity>) Class.forName(intent.getComponent().getClassName());
			} catch (ClassNotFoundException e) {
				//Should not happen, print it to the log!
				Log.e(TAG, "Failed when resolving the restart activity class via getLaunchIntentForPackage, stack trace follows!", e);
			}
		}
		
		return null;
	}
	
	/**
	 * INTERNAL method used to get the first activity with an intent-filter <action android:name="ir.hatamiarash.nazer.ERROR" />,
	 * If there is no activity with that intent filter, this returns null.
	 *
	 * @param context A valid context. Must not be null.
	 * @return A valid activity class, or null if no suitable one is found
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private static Class<? extends Activity> getErrorActivityClassWithIntentFilter(@NonNull Context context) {
		Intent searchedIntent = new Intent().setAction(INTENT_ACTION_ERROR_ACTIVITY).setPackage(context.getPackageName());
		List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(searchedIntent,
				PackageManager.GET_RESOLVED_FILTER);
		
		if (resolveInfos != null && resolveInfos.size() > 0) {
			ResolveInfo resolveInfo = resolveInfos.get(0);
			try {
				return (Class<? extends Activity>) Class.forName(resolveInfo.activityInfo.name);
			} catch (ClassNotFoundException e) {
				//Should not happen, print it to the log!
				Log.e(TAG, "Failed when resolving the error activity class via intent filter, stack trace follows!", e);
			}
		}
		
		return null;
	}
	
	/**
	 * INTERNAL method that kills the current process.
	 * It is used after restarting or killing the app.
	 */
	private static void killCurrentProcess() {
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(10);
	}
	
	/**
	 * INTERNAL method that stores the last crash timestamp
	 *
	 * @param timestamp The current timestamp.
	 */
	@SuppressLint("ApplySharedPref") //This must be done immediately since we are killing the app
	private static void setLastCrashTimestamp(@NonNull Context context, long timestamp) {
		context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).edit().putLong(SHARED_PREFERENCES_FIELD_TIMESTAMP, timestamp).commit();
	}
	
	/**
	 * INTERNAL method that gets the last crash timestamp
	 *
	 * @return The last crash timestamp, or -1 if not set.
	 */
	private static long getLastCrashTimestamp(@NonNull Context context) {
		return context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).getLong(SHARED_PREFERENCES_FIELD_TIMESTAMP, -1);
	}
	
	/**
	 * INTERNAL method that tells if the app has crashed in the last seconds.
	 * This is used to avoid restart loops.
	 *
	 * @return true if the app has crashed in the last seconds, false otherwise.
	 */
	private static boolean hasCrashedInTheLastSeconds(@NonNull Context context) {
		long lastTimestamp = getLastCrashTimestamp(context);
		long currentTimestamp = new Date().getTime();
		
		return (lastTimestamp <= currentTimestamp && currentTimestamp - lastTimestamp < config.getMinTimeBetweenCrashesMs());
	}
	
	/**
	 * Interface to be called when events occur, so they can be reported
	 * by the app as, for example, Google Analytics events.
	 */
	public interface EventListener extends Serializable {
		void onLaunchErrorActivity();
		
		void onRestartAppFromErrorActivity();
		
		void onCloseAppFromErrorActivity();
	}
}
