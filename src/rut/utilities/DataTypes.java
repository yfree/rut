package rut.utilities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import rut.Definitions;
import rut.exceptions.InvalidConversionException;

/**
 * This static class is used to convert text to different data types and 
 * to check that text is a valid for a particular data type.
 * Data Types in Rut: Text, Integer, Decimal, Boolean, Time, Date.
 * @author Yaakov Freedman
 * @version dev 0.2
 */
public class DataTypes {

	/**
	 * Checks that a String is valid as a text value for Rut Database. At the moment
	 * Rut supports all Strings that are Basic Latin Unicode.
	 * 
	 * @param text
	 * @return boolean
	 */
	public static boolean checkText(String text) {

		boolean result = true;
		char currentChar = 'a';

		for (int i = 0; i < text.length(); i++) {

			currentChar = text.charAt(i);

			if (Character.UnicodeBlock.of(currentChar) != Character.UnicodeBlock.BASIC_LATIN) {
				result = false;
				break;
			}

		}

		return result;
	}

	/**
	 * Checks that a String is valid as an integer value for Rut Database. At the
	 * moment Rut supports all positive and negative numbers with the same
	 * constraints as a basic Java int.
	 * 
	 * @param integer
	 * @return
	 */
	public static boolean checkInteger(String integer) {
		boolean result = true;

		try {
			Integer.parseInt(integer);
		} catch (NullPointerException e) {
			/* this means it is empty, this is ok */
		}

		catch (NumberFormatException e) {
			/* this means that it is not a valid integer, this is not ok */
			result = false;
		}

		return result;
	}

	/**
	 * Checks that a String is valid as a boolean value for Rut Database. At the
	 * moment Rut supports the texts "true" or "false" only at the moment for
	 * boolean values.
	 * 
	 * @param bool
	 * @return
	 */
	public static boolean checkBoolean(String bool) {
		boolean result = true;

		if (!bool.equals("true") && !bool.equals("false") && !bool.isEmpty()) {
			result = false;
		}

		return result;
	}

	/**
	 * Checks that a String is valid as a decimal value for Rut Database. At the
	 * moment Rut supports all positive and negative numbers with the same
	 * constraints as a basic Java double.
	 * 
	 * @param decimal
	 * @return
	 */
	public static boolean checkDecimal(String decimal) {
		boolean result = true;

		try {

			Double.parseDouble(decimal);

		} catch (NullPointerException e) {
			/* this means it is empty, this is ok */
		}

		catch (NumberFormatException e) {
			/* this means that it is not a valid integer, this is not ok */
			result = false;
		}

		return result;
	}

	/**
	 * Checks that a String is valid as a date value for Rut Database. At the moment
	 * Rut supports all valid dates in the format mm/dd/yyyy.
	 * 
	 * @param date
	 * @return
	 */
	public static boolean checkDate(String date) {
		boolean result = true;

		if (date.isEmpty()) {
			return true;
		}

		DateFormat dateFormat = new SimpleDateFormat(Definitions.dateFormat);

		dateFormat.setLenient(false);

		try {

			dateFormat.parse(date);

		} catch (ParseException e) {
			result = false;
		}

		return result;

	}

	/**
	 * Checks that a String is valid as a time value for Rut Database. At the moment
	 * Rut supports all valid times in the format HH:mm:ss.
	 * 
	 * @param time
	 * @return
	 */
	public static boolean checkTime(String time) throws DateTimeParseException {
		boolean result = true;

		if (time.isEmpty()) {
			return true;
		}

		try {

			LocalTime.parse(time);

		}

		catch (DateTimeParseException e) {

			result = false;

		}

		return result;

	}

	/**
	 * Turn a string into an int so it can be evaluated as an integer. Improper
	 * integer values throw the exception invalidConversionException
	 *
	 */

	public static int intify(String stringValue) throws InvalidConversionException {
		int intValue;

		try {
			intValue = Integer.parseInt(stringValue.trim());
		} catch (NullPointerException | NumberFormatException e) {
			throw new InvalidConversionException("integer");
		}

		return intValue;
	}

	/**
	 * Turn a string into a double so it can be evaluated as a decimal. Improper
	 * decimal values throw the exception invalidConversionException
	 *
	 */

	public static double decify(String stringValue) throws InvalidConversionException {
		double doubleValue;

		try {
			doubleValue = Double.parseDouble(stringValue.trim());
		} catch (NullPointerException | NumberFormatException e) {
			throw new InvalidConversionException("decimal");
		}

		return doubleValue;
	}

	/**
	 * Turn a string into a boolean so it can be evaluated as a boolean. Improper
	 * boolean values throw the exception invalidConversionException
	 *
	 */

	public static boolean boolify(String stringValue) throws InvalidConversionException {
		boolean booleanValue;

		if (!stringValue.equals("true") && !stringValue.equals("false")) {
			throw new InvalidConversionException("boolean");
		}

		booleanValue = Boolean.parseBoolean(stringValue.trim());

		return booleanValue;
	}

	/**
	 * Turn a string into a date so it can be evaluated as a date. Improper date
	 * values in mm/dd/yyyy format throw the exception invalidConversionException
	 *
	 */

	public static Date datify(String stringValue) throws InvalidConversionException {
		Date dateValue;

		SimpleDateFormat dateFormat = new SimpleDateFormat(Definitions.dateFormat);
		dateFormat.setLenient(false);

		try {
			dateValue = dateFormat.parse(stringValue);
		} catch (ParseException e) {
			throw new InvalidConversionException("date");
		}

		return dateValue;
	}

	/**
	 * Turn a string into a time so it can be evaluated as a time. Improper time
	 * values in HH:mm:ss format throw the exception invalidConversionException
	 *
	 */

	public static LocalTime timify(String stringValue) throws InvalidConversionException {

		LocalTime timeValue;

		try {

			timeValue = LocalTime.parse(stringValue);

		}

		catch (DateTimeParseException e) {

			throw new InvalidConversionException("time");

		}

		return timeValue;
	}

}
