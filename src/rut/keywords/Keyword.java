package rut.keywords;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import rut.Definitions;
import rut.MemoryStorage;
import rut.Statement;
/**
 * For all Keyword subclasses, all that is necessary is to implement the generate() method.
 * Everything else is taken care of. Then go ahead and call the Keyword from the Interpreter with all the other 
 * keyword calls.
 * @author Yaakov Freedman
 *
 */
//TODO: Make keywords part of the value not necessarily the whole value
public abstract class Keyword {

	protected MemoryStorage memory;
	private String keywordName;

	public Keyword(MemoryStorage memory, String keywordName) {

		if (!Definitions.keywords.contains(keywordName)) {

			System.out.println("Invalid keyword " + keywordName);
			System.exit(1);
		}

		this.memory = memory;
		this.setKeywordName(keywordName);
		
	}
	
	/**
	 * Execute the logic to replace the keywords with specified values.
	 * The generate method called by execute is implementation dependent and 
	 * is what actually creates the keyword result value(s).
	 * @param statement
	 * @param memory
	 */
	public void execute(Statement statement) {
		ArrayList<String> tokenList = statement.getKeywordTokenList(this.keywordName);

		String selectedNodeName = statement.getSelectedNodeName();
		String selectedNodeValue = statement.getSelectedNodeValue();
		
		String processedValue = "";
		
		/* selectedNodeName */
		processedValue = this.processValue(selectedNodeName, tokenList);
		
		statement.setSelectedNodeName(processedValue);

		/* selectedNodeValue */
		processedValue = this.processValue(selectedNodeValue, tokenList);
		
		statement.setSelectedNodeValue(processedValue);
		
		/* childrenNamesValues */

		String newChildName, newChildValue, childValue;
		LinkedHashMap<String, String> newChildrenNamesValues = new LinkedHashMap<String, String>();

		for (String childName : statement.getChildrenNamesValues().keySet()) {

			childValue = statement.getChildrenNamesValues().get(childName);
			
			/* child name */
			newChildName = this.processValue(childName, tokenList);
			
			/* child value */
			newChildValue = this.processValue(childValue, tokenList);
			
			newChildrenNamesValues.put(newChildName, newChildValue);

		}

		statement.setChildrenNamesValues(newChildrenNamesValues);

		statement.clearKeywordTokenList(this.keywordName);

	}

	/**
	 * Replaces a specified value containing the appropriate keyword (and possible parameters) with 
	 * the corresponding values. 
	 * @param valueToProcess The String to process (e.g. nodeName, nodeValue)
	 * @param tokenList The tokenList from statement, so the appropriate switches can be made if necessary
	 * @return a String containing the new processed value
	 */
	public String processValue (String valueToProcess, ArrayList<String> tokenList) {
		
		String parameter = "";
		
		for (String token : tokenList) {
			
			valueToProcess = valueToProcess.replace(token, this.keywordName).trim();
		
		}
		
		parameter = Statement.extractKeywordParameter(valueToProcess, this.keywordName);
		if (!parameter.isEmpty()) {
			
			valueToProcess = this.generate(parameter);
		
		}
		else if (valueToProcess.equals(this.keywordName)) {
		
			valueToProcess = this.generate();
		
		}
		
		return valueToProcess;
	}
	
	public String getKeywordName() {
		return keywordName;
	}

	public void setKeywordName(String keywordName) {
		this.keywordName = keywordName;
	}

	protected abstract String generate(String parameter);

	protected String generate() {
		return generate("");
	}
}
