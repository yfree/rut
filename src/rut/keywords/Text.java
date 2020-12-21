package rut.keywords;

import rut.InvalidConversionException;
import rut.MemoryStorage;
import rut.utilities.DataTypes;
import rut.utilities.Randomizer;

public class Text extends Keyword {

	public Text(MemoryStorage memory) {
		super(memory, "Text");

	}

	/**
	 * Creates a random string of text. Its length can be set with an integer
	 * parameter and its max length is 100.
	 * 
	 * @param memory the instance of MemoryStorage that the application is using,
	 *               access is needed to the data
	 * @param if     this String is an integer, it will be the size of the text
	 * @return a string of text
	 */

	public String generate(String parameter) {

		String temporaryText = "";
		int size;

		if (parameter.isEmpty()) {

			temporaryText = Randomizer.text();
		}

		else if (DataTypes.checkInteger(parameter)) {

			temporaryText = this.getKeywordName() + " " + parameter;

			try {

				if (DataTypes.intify(parameter) >= 0 && DataTypes.intify(parameter) <= 100) {

					size = DataTypes.intify(parameter);
					temporaryText = Randomizer.text(size);
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
