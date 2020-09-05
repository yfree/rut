package rut.keywords;

import java.text.SimpleDateFormat;

import rut.Definitions;
import rut.MemoryStorage;
import rut.utilities.Randomizer;

public class Date extends Keyword {

	public Date(MemoryStorage memory) {
		super(memory, "Date");

	}

	/**
	 * Creates a random date. Can display the current date with the the parameter
	 * set as 'now'.
	 * 
	 * @param memory the instance of MemoryStorage that the application is using,
	 *               access is needed to the data
	 * @param if     this String is 'now', the current date will be displayed
	 * @return a date in the 'Definitions' format as a String
	 */

	public String generate(String parameter) {

		String date;

		if (parameter.isEmpty()) {
			
			date = Randomizer.date();
		}
	
		else if (parameter.equals("now")) {

			SimpleDateFormat dateFormat = new SimpleDateFormat(Definitions.dateFormat);
			dateFormat.setLenient(false);

			java.util.Date dateNow = new java.util.Date();
			date = dateFormat.format(dateNow);

		} else {

			date = this.getKeywordName() + " " + parameter;

		}

		return date;
	}

}
