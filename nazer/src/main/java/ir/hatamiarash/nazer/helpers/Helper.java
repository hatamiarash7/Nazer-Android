package ir.hatamiarash.nazer.helpers;

import java.io.File;

public class Helper {
	public static int isRooted() {
		int found = 0;
		String[] places = {"/sbin/", "/system/bin/", "/system/xbin/",
				"/data/local/xbin/", "/data/local/bin/",
				"/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
		for (String where : places) {
			if (new File(where + "su").exists()) {
				found = 1;
				break;
			}
		}
		return found;
	}
}
