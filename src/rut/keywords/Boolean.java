package rut.keywords;

import rut.MemoryStorage;
import rut.utilities.Randomizer;

public class Boolean extends Keyword {

	public Boolean(MemoryStorage memory) {
		super(memory, "Boolean");

	}

	/**
	 * Creates a random boolean.
	 * @return a string that is randomly true or false
	 */

	public String generate(String parameter) {

		if (!parameter.isEmpty()) {

			return this.getKeywordName() + " " + parameter;

		}
		
		return String.valueOf(Randomizer.bool());
	}
}
