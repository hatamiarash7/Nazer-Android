package ir.hatamiarash.nazer.models;

public class ExceptionModel {
	private String stackTrace;
	private String activity;
	private String method;
	private String message;
	private String versionCode;
	private String versionName;
	private int isRoot;
	private int isBackground;
	private int line;
	
	public ExceptionModel(String stackTrace, String activity, String method, String message, String versionCode, String versionName, int isRoot, int isBackground, int line) {
		this.stackTrace = stackTrace;
		this.activity = activity;
		this.method = method;
		this.message = message;
		this.versionCode = versionCode;
		this.versionName = versionName;
		this.isRoot = isRoot;
		this.isBackground = isBackground;
		this.line = line;
	}
	
	public String getStackTrace() {
		return stackTrace;
	}
	
	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}
	
	public String getActivity() {
		return activity;
	}
	
	public void setActivity(String activity) {
		this.activity = activity;
	}
	
	public String getMethod() {
		return method;
	}
	
	public void setMethod(String method) {
		this.method = method;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getVersionCode() {
		return versionCode;
	}
	
	public void setVersionCode(String versionCode) {
		this.versionCode = versionCode;
	}
	
	public String getVersionName() {
		return versionName;
	}
	
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}
	
	public int getIsRoot() {
		return isRoot;
	}
	
	public void setIsRoot(int isRoot) {
		this.isRoot = isRoot;
	}
	
	public int getIsBackground() {
		return isBackground;
	}
	
	public void setIsBackground(int isBackground) {
		this.isBackground = isBackground;
	}
	
	public int getLine() {
		return line;
	}
	
	public void setLine(int line) {
		this.line = line;
	}
}
