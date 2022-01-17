package rut.keywords;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;

public class Newid extends Keyword {

	public Newid(MemoryStorage memory) {
		super(memory, "Newid");

	}

	/** Overrides the parent class Keyword's definition of execute.
	 * This version of execute runs only for node names not node values. 
	 * Method description: Execute the logic to replace the keywords with specified values.
	 * The generate method called by execute is implementation dependent and 
	 * is what actually creates the keyword result value(s).
	 * @param statement
	 * @param memory
	 */
	public void execute(Statement statement) {
		ArrayList<String> tokenList = statement.getKeywordTokenList(this.getKeywordName());

		String selectedNodeName = statement.getSelectedNodeName();
		String selectedNodeValue = statement.getSelectedNodeValue();
		
		String processedValue = "";
		
		/* selectedNodeName */
		processedValue = this.processValue(selectedNodeName, tokenList);
		
		statement.setSelectedNodeName(processedValue);

		/* selectedNodeValue */
		processedValue = this.processNonTranslatedValue(selectedNodeValue, tokenList);
		
		statement.setSelectedNodeValue(processedValue);
		
		/* descendantNamesValues */

		String newChildName, newChildValue, childValue;
		ConcurrentHashMap<String, String> newChildNamesValues = new ConcurrentHashMap<String, String>();

		for (String childName : statement.getChildNamesValues().keySet()) {

			childValue = statement.getChildNamesValues().get(childName);
			
			/* child name */
			newChildName = this.processValue(childName, tokenList);
			
			/* child value */
			newChildValue = this.processNonTranslatedValue(childValue, tokenList);
			
			newChildNamesValues.put(newChildName, newChildValue);

		}

		statement.setChildNamesValues(newChildNamesValues);

		statement.clearKeywordTokenList(this.getKeywordName());

	}
	
	/**
	 * Removes tokens from a string value string  
	 * @param valueToProcess The String to process (e.g. nodeName, nodeValue)
	 * @param tokenList The tokenList from statement, so the appropriate switches can be made if necessary
	 * @return a String containing the new processed value
	 */
	public String processNonTranslatedValue (String valueToProcess, ArrayList<String> tokenList) {
		
		for (String token : tokenList) {
			
			valueToProcess = valueToProcess.replace(token, this.getKeywordName()).trim();
		
		}
		
		
		return valueToProcess;
	}
	
	
	
	/**
	 * Creates a unique ID that doesn't currently exist in the database. It will try
	 * to generate one until a unique id is available
	 * 
	 * @param memory the instance of MemoryStorage that the application is using,
	 *               access is needed to the data
	 * 
	 * @return a long converted into a unique String
	 */

	public String generate(String parameter) {

		if (!parameter.isEmpty()) {

			return this.getKeywordName() + " " + parameter;

		}

		long uid = this.memory.getUid();

		String temporaryNewId;
		Node nodeToSearchFor = null;
		do {
			temporaryNewId = Long.toString(uid++);
			nodeToSearchFor = this.memory.getNodeByName(temporaryNewId);
			/*
			 * New ID must be unique, if we find it in the database, try the next number...
			 */
		} while (nodeToSearchFor != null);

		this.memory.setUid(uid);
		return temporaryNewId;
	}

}
