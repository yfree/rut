package rut.keywords;

import rut.MemoryStorage;
import rut.exceptions.InvalidConversionException;
import rut.utilities.DataTypes;
import rut.utilities.Randomizer;

public class Decimal extends Keyword {

	public Decimal(MemoryStorage memory) {
		super(memory, "Decimal");

	}

	/**
	 * Creates a random decimal number. Its length can be set with an integer
	 * parameter between 0 and 9,999,999. The two decimal places are fixed at the moment.
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
			
			temporaryText = String.valueOf(Randomizer.decimal());
	
		}
		else if (DataTypes.checkInteger(parameter)) {
			
			temporaryText = this.getKeywordName() + " " + parameter;
			
			try {
				
				if (DataTypes.intify(parameter) >= 0 &&
						DataTypes.intify(parameter) <= 999999) {
					
					size = DataTypes.intify(parameter);
					
					/* At the moment, two decimal places is being used... */
					temporaryText = String.valueOf(Randomizer.decimal(size, 99));
				}
			}

			catch (InvalidConversionException e) {

			}

		} else

		{

			temporaryText = this.getKeywordName() + " " + parameter;

		}

		return temporaryText;
	}
}
