package com.wcities.eventseeker.util;

import java.util.regex.Pattern;

public class FieldValidationUtil {
	
	private static final String TAG = FieldValidationUtil.class.getName();

	public static boolean isValidPhone(String phoneNo) {
		Pattern pattern = Pattern.compile("^\\+?(?:[0-9] ?){6,14}[0-9]$");
	    return pattern.matcher(phoneNo).matches();
	}

	public static boolean isValidName(String name) {
		return name.length() > 0;
	}
	
	public static boolean isValidEmail(String email) {
		/*String emailRegex = "(?:[a-z0-9!#$%\\&'*+/=?\\^_`{|}~-]+(?:\\.[a-z0-9!#$%\\&'*+/=?\\^_`{|}"
							+ "~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\"
							+ "x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-"
							+ "z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5"
							+ "]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-"
	    					+ "9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21"
	    					+ "-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";*/
		
		String emailRegex = "(?:[a-z0-9!#$%\\&'*+/=?\\^_`{|}~-]+(?:\\.[a-z0-9!#$%\\&'*+/=?\\^_`{|}~-]+)*|"
							+ "\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\"
							+ "[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])"
							+ "?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?"
							+ "[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]"
							+ ":(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09"
							+ "\\x0b\\x0c\\x0e-\\x7f])+)\\])";
		
		Pattern pattern = Pattern.compile(emailRegex);
	    return pattern.matcher(email.toLowerCase()).matches();
	}

	public static boolean isPasswordMatching(String password, String confirmPassword) {
		return password.equals(confirmPassword);
	}
	
	public static boolean isNumber(String number) {
		return number.matches("[0-9]+");
	}
	
}
