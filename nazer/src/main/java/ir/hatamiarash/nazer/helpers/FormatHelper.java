package ir.hatamiarash.nazer.helpers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class FormatHelper {
	@Contract(pure = true)
	@NotNull
	public static String fixResponse(@NotNull String response) {
		return "{\"" + response.substring(response.indexOf("error"));
	}
}
