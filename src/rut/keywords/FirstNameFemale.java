package rut.keywords;


import java.util.HashSet;

import rut.MemoryStorage;
import rut.Node;
import rut.utilities.Randomizer;

public class FirstNameFemale extends Keyword {

	public FirstNameFemale(MemoryStorage memory) {
		super(memory, "FirstNameFemale");

	}

	/**
	 * Creates a random female English first name. If the parameter 'unique' is passed,
	 * the name will be a unique value in the database.
	 * 
	 * @param memory the instance of MemoryStorage that the application is using,
	 *               access is needed to the data
	 * @param if this String is set to 'unique' the value returned will be unique 
	 * across the database 
	 * @return a string that is a random female English first name
	 */

	public String generate(String parameter) {
		
		String temporaryFirstName;
		Node nodeToSearchFor = null;

		HashSet<String> nameSet = new HashSet<String>(Randomizer.firstNamesFemaleEnglish);
		
		/* No generation will be made for improper parameters,
		 * instead the original value is returned */
		if (!parameter.isEmpty() && !parameter.equals("unique")) {
			
			return this.getKeywordName() + " " + parameter;
		
		}

		
		do {
			
			if (nameSet.size() == 0) {
			
				temporaryFirstName = Randomizer.text();
			}
			else {
			
				temporaryFirstName = Randomizer.nameEnglish(nameSet);
			
			}
			
			nameSet.remove(temporaryFirstName);
			
			if (parameter.equals("unique")) {
				
				/* Search both names and values */
				nodeToSearchFor = memory.getNodeByName(temporaryFirstName);
				if (nodeToSearchFor == null) {
				
					nodeToSearchFor = memory.getNodeByValue(temporaryFirstName);
				
				}
				
				/* If every unique name has been exhausted, a random string is used at the moment...
				 * Eventually the random names list will be large enough that this is not a real issue. */
			}

		} while (nodeToSearchFor != null);

		return temporaryFirstName;
	}

}