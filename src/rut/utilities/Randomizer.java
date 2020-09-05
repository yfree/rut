package rut.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

/**
 * This static class is the randomization library, which can be used internally
 * by Rut but more importantly, as a powerful testing resource. This library
 * will be made available to the user via keywords that trigger random values.
 * TODO: Implement the library...and also change 'adds' to inline arrays
 * 
 * @author Yaakov Freedman
 * @version dev 0.2
 */

public class Randomizer {

	public static HashSet<String> firstNamesMaleEnglish;

	public static HashSet<String> firstNamesFemaleEnglish;

	public static HashSet<String> lastNamesEnglish;

	public static Random randomGenerator;

	static {

		String[] firstMaleEnglish = new String[] { "Arnold", "Kyle", "Eric", "Julian", "Nicholas", "Robert", "William",
				"Thomas", "Kyle", "Wesley", "Jared", "Jacob", "Jordan", "Phillip", "Anthony", "Gary", "Spencer",
				"Charles", "Melvin", "Marvin", "Joseph", "Isaac", "Levi", "Daniel", "Amon", "Edward", "David",
				"Michael" };

		firstNamesMaleEnglish = new HashSet<String>(Arrays.asList(firstMaleEnglish));

		String[] firstFemaleEnglish = new String[] { "Jessica", "Mary", "Sheila", "Rachel", "Tracy", "Phoebe", "Hellen",
				"Rebecca", "Sophia", "Nina", "Numia", "Elizabeth", "Alicia", "Britney", "Delila", "Abigail", "Alana",
				"Amber", "Anna", "April", "Anita", "Barbara", "Brook", "Carla", "Candace", "Colleen", "Darlene",
				"Dorothy", "Emily", "Gloria", "Harriet", "Jennifer", "Jill", "Kimberly", "Linda", "Maria", "Michelle",
				"Patricia", "Rose", "Stacy", "Wendy" };

		firstNamesFemaleEnglish = new HashSet<String>(Arrays.asList(firstFemaleEnglish));

		String[] lastEnglish = new String[] { "Peterson", "Phillip", "Smith", "Redford", "Jacobson", "Jones", "Alden",
				"Timbers", "Covington", "Smith", "Dawson", "Worthington", "Chase", "Smathers", "Chesterfield", "Phelps",
				"Weatherfield", "Melington", "McDonald", "Roy", "Jackson", "Macintire", "Green", "Foster", "Turner",
				"Griffin", "Collins", "Walker", "Campbell", "Bailey", "Perry", "Murphy", "Young", "Johnson" };

		lastNamesEnglish = new HashSet<String>(Arrays.asList(lastEnglish));

		randomGenerator = new Random();
	}

	/**
	 * Create random integer with 2^32 possibilities
	 * 
	 * @return
	 */
	public static int integer() {

		/* Only positive numbers... */

		return Math.abs(randomGenerator.nextInt());
	}

	/**
	 * Create random integer with a fixed max size
	 * 
	 * @param size
	 * @return
	 */
	public static int integer(int size) {

		return randomGenerator.nextInt(size + 1);

	}

	public static double decimal() {

		return Randomizer.decimal(Randomizer.integer(9999999), Randomizer.integer(99));
	}

	public static double decimal(int first, int second) {

		String randomFirst = String.valueOf(Randomizer.integer(first));
		String randomSecond = String.valueOf(Randomizer.integer(second));
		String finalString = randomFirst + "." + randomSecond;

		return Double.parseDouble(finalString);
	}

	/**
	 * Create random string with a random max size
	 * 
	 * @return string of random letters of a random size
	 */
	public static String text() {

		int randomSize = Randomizer.integer(10);

		return Randomizer.text(randomSize);
	}

	/**
	 * Create random string with a fixed size
	 * 
	 * @param size
	 * @return string of random letters with a specified size
	 */
	public static String text(int size) {

		StringBuilder randomString = new StringBuilder();
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		int randomSize;

		for (int i = 0; i < size; i++) {

			randomSize = Randomizer.integer(alphabet.length() - 1);
			Character randomChar = alphabet.charAt(randomSize);

			randomString.append(randomChar.toString());

		}
		return randomString.toString();
	}

	/**
	 * Returns a random String that is a valid time in the Definitions timeformat.
	 * 
	 * @return A String of a random time
	 */
	public static String time() {

		String hour, minute, second, randomTime;

		boolean validTime;

		do {

			hour = String.valueOf(Randomizer.integer(23));
			hour = Randomizer.adjustTimeNumberFormat(hour);
			
			minute = String.valueOf(Randomizer.integer(59));
			minute = Randomizer.adjustTimeNumberFormat(minute);
			
			second = String.valueOf(Randomizer.integer(59));
			second = Randomizer.adjustTimeNumberFormat(second);
			
			randomTime = hour + ":" + minute + ":" + second;
			validTime = DataTypes.checkTime(randomTime);

		} while (!validTime);

		return randomTime;
	}

	public static String date() {

		String month, day, year, randomDate;

		boolean validDate;

		do {
			
			month = String.valueOf(Randomizer.integer(12));
			month = Randomizer.adjustTimeNumberFormat(month);
			
			day = String.valueOf(Randomizer.integer(31));
			day = Randomizer.adjustTimeNumberFormat(day);
			
			year = String.valueOf(Randomizer.integer(100) + 1920);
			randomDate = month + "/" + day + "/" + year;
			validDate = DataTypes.checkDate(randomDate);

		} while (!validDate);

		return randomDate;

	}

	public static boolean bool() {

		return Randomizer.integer(1) == 1 ? true : false;
	}

	public static String firstName() {

		return "";
	}

	/**
	 * Returns a random first name for English male first names.
	 * 
	 * @return
	 */

	public static String firstNameMaleEnglish() {

		return nameEnglish(Randomizer.firstNamesMaleEnglish);
	}

	/**
	 * Returns a random first name for English female first names.
	 * 
	 * @return
	 */

	public static String firstNameFemaleEnglish() {

		return nameEnglish(Randomizer.firstNamesFemaleEnglish);
	}

	/**
	 * Returns a random first name for English last names.
	 * 
	 * @return
	 */

	public static String lastNameEnglish() {

		return nameEnglish(Randomizer.lastNamesEnglish);
	}

	/**
	 * Returns a random first name from a specified list.
	 * 
	 * @param nameSet the name set of random first names
	 * @return
	 */
	public static String nameEnglish(HashSet<String> nameSet) {

		int setSize = nameSet.size();

		int randomIndex = Randomizer.integer(setSize - 1);

		ArrayList<String> names = new ArrayList<String>(nameSet);

		return names.get(randomIndex);
	}

	/* Prepends a zero to numbers that are less than 10 to adhere to the format that 
	 * looks like this '06:01' */
	public static String adjustTimeNumberFormat(String numericString) {
		
		return numericString.length() == 1 ? "0" + numericString : numericString;
	}
}
