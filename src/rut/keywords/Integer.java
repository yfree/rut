package rut.keywords;

import rut.InvalidConversionException;
import rut.MemoryStorage;
import rut.utilities.DataTypes;
import rut.utilities.Randomizer;

public class Integer extends Keyword {

	public Integer(MemoryStorage memory) {
		super(memory, "Integer");

	}

	/**
	 * Creates a random integer. Its max length is a positive number and can be set
	 * with an integer parameter and its max length is 999,999,999.
	 * 
	 * @param memory the instance of MemoryStorage that the application is using,
	 *               access is needed to the data
	 * @param if     this String is an integer, it will be the size of the text
	 * @return a random integer
	 */

	public String generate(String parameter) {

		String temporaryText = "";
		int size;

		if (parameter.isEmpty()) {

			temporaryText = String.valueOf(Randomizer.integer());

		} else if (DataTypes.checkInteger(parameter)) {

			temporaryText = this.getKeywordName() + " " + parameter;
			
			try {

				if (DataTypes.intify(parameter) >= 0 && DataTypes.intify(parameter) <= 99999999) {

					size = DataTypes.intify(parameter);
					temporaryText = String.valueOf(Randomizer.integer(size));
				}
			}

			catch (InvalidConversionException e) {

				
			}

		} else {

			temporaryText = this.getKeywordName() + " " + parameter;

		}

		return temporaryText;
	}
}
