/* 
Copyright 2019 Yaakov Freedman

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
*/

package rut;

import java.util.LinkedHashMap;
import java.util.Date;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rut.utilities.DataTypes;
import rut.utilities.Randomizer;

/**
 * A Statement object is an instruction that is to be structured and passed to
 * the interpreter in Rut Database.
 * 
 * This class contains a lexer and parser (the parser is composed of several
 * methods). All fields are extracted from the user's statement and populated
 * into structured member variables. Errors are captured here and the
 * interpreter is subsequently signaled to expect an error.
 * 
 * The only public methods that are of great importance for a Statement are:
 * 
 * String parseStatement(String) - which takes the user input and loads up the
 * Statement variables with it.
 * 
 * You can also manually reset() the Statement variables, however the
 * parseStatement() method does that for you.
 * 
 * @author Yaakov Freedman
 * @version dev 0.2
 */

public class Statement {

	/* The original statement */
	private String originalStatementString;

	/* The statement statement as substrings are manipulated. */
	private String statementString;

	/* The name of the operation to be executed */
	private String operation;

	/* The portion of the string after the operation */
	private String argument;

	/*
	 * This is where the node name that is selected within an argument is parsed to
	 */
	private String selectedNodeName;

	/*
	 * This is where the selected node value is parsed to. Used in a write operation
	 * when the argument contains an '=' sign, (as opposed to the where condition
	 * containing an '=' sign)
	 */
	private String selectedNodeValue;

	/*
	 * This is where the specified parent node names are parsed to. Order is
	 * descending
	 */
	private ArrayList<String> parentNames;

	/**
	 * This is where the selected children's names (and values if they exist) that
	 * are selected within an argument are parsed to. When a value isn't specified,
	 * the item's value will be empty
	 */
	private LinkedHashMap<String, String> childrenNamesValues;

	/**
	 * The format that the statement is requested to be returned in.
	 * Valid options are: RutFormat, XML, JSON
	 * TODO: implement, at the moment this will default to standard RutFormat
	 */
	private String dataFormat = "RutFormat";

	/**
	 * The portion of the string following a 'where' Todo: ensure this is enforced
	 * to come specifically after the argument
	 */
	private String whereCondition;

	/**
	 * This is where the where condition rules are parsed to, the key is the field
	 * name and the value is the required value or expression of the field
	 */
	private LinkedHashMap<String, ArrayList<String>> whereConditionRules;

	/**
	 * The number of times a statement should be executed. Default value is 1. This
	 * can be set with the Times keyword.
	 */

	private int iterations;

	/**
	 * Error messages that are returned for errors caught by the statement parser
	 */
	private Set<String> errorMessages;

	/**
	 * Tokens from the statement that are hidden in order to exclude them from
	 * standard validation.
	 */
	private LinkedHashMap<String, String> hiddenTokens;

	/**
	 * Tokens which were originally a keyword (such as newid) but were replaced from
	 * the original statement because children names cannot be duplicate, (even
	 * though newid multiple times will resolve to different values). The
	 * Interpreter accesses the keyword tokens and makes the appropriate
	 * conversions. Key is the token name and value is the keyword the token
	 * represents. NOTE: Only used when a keyword appears more than once in a
	 * statement.
	 */

	private LinkedHashMap<String, String> keywordTokens;

	public Statement() {

		/* Reset / Initialize variables that can be changed */
		this.reset();
	}

	public String toString() {

		String newline = "\n";

		StringBuilder statementString = new StringBuilder();
		statementString.append("Original Statement: \"" + this.originalStatementString + "\"" + newline);
		statementString.append("Statement: \"" + this.statementString + "\"" + newline);
		statementString.append("Operation: \"" + this.operation + "\"" + newline);
		statementString.append("Argument String: \"" + this.argument + "\"" + newline);
		statementString.append("Selected Node Name: \"" + this.selectedNodeName + "\"" + newline);
		statementString.append("Selected Node Value: \"" + this.selectedNodeValue + "\"" + newline);
		statementString.append("Parent Names: " + this.parentNames + newline);
		statementString.append("Selected Children Names: " + this.childrenNamesValues + newline);
		statementString.append("Where Condition: \"" + this.whereCondition + "\"" + newline);
		statementString.append("Where Condition Rules: " + this.whereConditionRules + newline);
		statementString.append("Iterations: " + this.iterations + newline);
		statementString.append("Parse Error Messages: " + this.errorMessages + newline);
		statementString.append("Keyword Tokens: " + this.keywordTokens + newline);
		return statementString.toString();
	}

	/* Resets all of the statement's member variables to their default values */
	public void reset() {
		this.setOriginalStatementString("");
		this.setStatementString("");
		this.setOperation("");
		this.setArgument("");
		this.setSelectedNodeName("");
		this.setSelectedNodeValue("");
		this.setParentNames(new ArrayList<String>());
		this.setChildrenNamesValues(new LinkedHashMap<String, String>());
		this.setWhereCondition("");
		this.setWhereConditionRules(new LinkedHashMap<String, ArrayList<String>>());
		this.setIterations(1);
		this.errorMessages = new HashSet<String>();
		this.hiddenTokens = new LinkedHashMap<String, String>();
		this.keywordTokens = new LinkedHashMap<String, String>();
	}

	/**
	 * This is main public method for the Statement class. Parses the user input
	 * into a variable structure for the interpreter to understand and process.
	 * Several types of Parser errors are caught in this method. 1) Non Unicode -
	 * Basic Latin data 2) Illegal characters (Caught by the Lexer) 3) Empty node
	 * name when an argument is required 4) Illegal operation name 5) Using the node
	 * Name 'Child' more than once. Other non-operation specific errors are caught
	 * by the helper parse methods when encountered.
	 * 
	 * Operation specific errors are caught by the checkForOpErrors method called at
	 * the end of parseStatement. Further error checking is performed by the
	 * Interpreter in its own checkForOpErrors, this error checking is done for
	 * errors that require access to the data such as checking that the values
	 * adhere to the rules set for the nodes being written to.
	 * 
	 * @param userInput the user input as a String
	 */

	public void parseStatement(String userInput) {

		/* Reset all fields before parsing a statement. */
		this.reset();

		this.originalStatementString = userInput;

		/* remove newlines */
		this.originalStatementString = this.originalStatementString.replace("\r", "");
		this.originalStatementString = this.originalStatementString.replace("\n", "");

		this.statementString = this.originalStatementString;

		/* Immediately check that the statement contains only Unicode - Basic Latin. */
		if (!DataTypes.checkText(this.statementString)) {

			this.addError("Invalid characters encountered, only Unicode - Basic Latin is allowed.");

			return;
		}

		/* Correct delimiters that were escaped (i.e. replace '\;' with ';' */
		this.statementString = this.statementString.replace("\\;", ";");

		/* If this is a comment, do not process further */
		if (this.statementString.startsWith("//")) {
			this.setOperation("comment");
			return;
		}

		/*
		 * Special pre-processing required for keywords, (if they appear more than once
		 * they are substituted with a value stored in the keywordTokens container)
		 */

		this.makeKeywordsTokens();

		/*
		 * This is a special keyword unlike any other. It is processed separately.
		 */
		if (!this.processTimesKeyword()) {
			return;
		}

		/* Remove all repeating spaces */
		this.statementString = this.statementString.replaceAll("\\s+", " ");

		/* The following token expressions are temporarily hidden from the parser */

		/* Time values */
		this.hideTokens("\\d{2}:\\d{2}:\\d{2}");

		/* Double quoted values */
		this.hideTokens("\"[^\"]*\"");

		/* Single quoted values */
		this.hideTokens("\'[^\']*\'");

		/* Perform a lexical scan for illegal chars */
		if (!this.checkCharacters(this.statementString)) {

			this.addError("Illegal characters found in query: \"" + this.originalStatementString + "\".");

			return;
		}

		/* Chunk the statement into a structure */
		this.chunkStatement();

		/* child keyword cannot be used repeatedly, causes hard stop error */
		if (this.getNodeHierarchyNameCount("Child") > 1) {

			this.addError("Child keyword cannot be used repeatedly in a statement.");
		}

		/* rule keyword cannot be used repeatedly, causes hard stop error */
		if (this.getNodeHierarchyNameCount("rule") > 1) {

			this.addError("Rule node name cannot be used repeatedly in a statement.");
		}

		/*
		 * Detect if the write operation is writing rules. If so, the operation (behind
		 * the scenes) is changes to 'enforce'. This is a special operation that works
		 * similar to 'write' in every way except for the fact it writes nodes which are
		 * RULES.
		 */

		this.detectEnforce();

		/* Detect empty node name when argument is required */
		if (this.selectedNodeName.trim().isEmpty() && (Definitions.requiredArgument.contains(this.operation))) {

			this.addError(Statement.capFirstLetter(this.operation) + " requires an argument.");

		}

		/* Detect bad operation */
		if (this.operation.length() == 0 && this.statementString.length() > 0) {

			this.addError("Cannot interpret operation in query: \"" + this.originalStatementString + "\".");

		}

		/*
		 * Hidden tokens are returned to the statement before the check for operation
		 * errors is performed
		 */
		this.putTokensBack();

		/*
		 * Now that names are parsed, individually check them (nothing except names) for
		 * illegal characters
		 */
		String childrenNamesText = String.join(" ", this.childrenNamesValues.keySet());

		String regex = "[a-zA-Z0-9\\-_\\s]*";
		if (!Statement.checkStringRegex(regex, this.selectedNodeName)
				|| !Statement.checkStringRegex(regex, childrenNamesText)) {

			this.addError("Node names can only contain letters, numbers, spaces, dashes, and underscores.");

			return;
		}

		/* And lastly, operation specific syntax errors */
		this.checkForOpErrors();
	}

	public String getOriginalStatementString() {
		return this.originalStatementString;
	}

	public void setOriginalStatementString(String originalStatementString) {
		this.originalStatementString = originalStatementString;
	}

	public String getStatementString() {
		return statementString;
	}

	public void setStatementString(String statementString) {
		this.statementString = statementString;
	}

	public String getOperation() {

		return this.operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getArgument() {
		return argument;
	}

	public void setArgument(String argument) {
		this.argument = argument;
	}

	public String getSelectedNodeName() {
		return this.selectedNodeName;
	}

	public void setSelectedNodeName(String selectedNodeName) {
		this.selectedNodeName = selectedNodeName;
	}

	public String getSelectedNodeValue() {
		return this.selectedNodeValue;
	}

	public void setSelectedNodeValue(String selectedNodeValue) {
		this.selectedNodeValue = selectedNodeValue;
	}

	public ArrayList<String> getParentNames() {
		return this.parentNames;
	}

	public void setParentNames(ArrayList<String> parentNames) {
		this.parentNames = parentNames;
	}

	public LinkedHashMap<String, String> getChildrenNamesValues() {
		return this.childrenNamesValues;
	}

	public void setChildrenNamesValues(LinkedHashMap<String, String> childrenNamesValues) {
		this.childrenNamesValues = childrenNamesValues;
	}

	public String getWhereCondition() {
		return this.whereCondition;
	}

	public void setWhereCondition(String whereCondition) {
		this.whereCondition = whereCondition;
	}

	public LinkedHashMap<String, ArrayList<String>> getWhereConditionRules() {
		return this.whereConditionRules;
	}

	public void setWhereConditionRules(LinkedHashMap<String, ArrayList<String>> whereConditionRules) {
		this.whereConditionRules = whereConditionRules;
	}

	
	public String getDataFormat() {
		return this.dataFormat;
	}

	public void setDataFormat(String dataFormat) {
		this.dataFormat = dataFormat;
	}
	
	public int getIterations() {
		return this.iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public Set<String> getErrorMessages() {
		return this.errorMessages;
	}

	public void setErrorMessages(Set<String> errorMessages) {
		this.errorMessages = errorMessages;
	}

	/**
	 * * Record an error that has been detected.
	 * 
	 * @param errorMessage
	 */

	public void addError(String errorMessage) {
		this.operation = "error";
		this.errorMessages.add(errorMessage);
	}

	/**
	 * Returns the full node hierarchy parsed in the user statement, in other words,
	 * the parent names followed by the selected node name like so: parent1,
	 * parent2, selectedNodeName.
	 * 
	 * @return A new ArrayList
	 */
	public ArrayList<String> getNodeHierarchy() {
		ArrayList<String> nodeHierarchy = new ArrayList<String>(this.parentNames);
		nodeHierarchy.add(this.selectedNodeName);
		return nodeHierarchy;
	}
	
	/**
	 * Returns the full node hierarchy parsed in the user statement, in other words,
	 * the parent names followed by the selected node name like so: 
	 * parent1.parent2
	 * 
	 * @return A new String
	 */
	public String getNodeParentString() {
		ArrayList<String> nodeHierarchy = new ArrayList<String>(this.parentNames);
		return String.join(".", nodeHierarchy); 
	}
	
	/**
	 * Returns the full node hierarchy parsed in the user statement, in other words,
	 * the parent names followed by the selected node name like so: 
	 * parent1.parent2.selectedNodeName
	 * 
	 * @return A new String
	 */
	public String getNodeHierarchyString() {
		
		return String.join(".", this.getNodeHierarchy()); 
	}
	/**
	 * Gets a list of tokens corresponding to a specific keyword. Keyword tokens are
	 * grouped with the token as the key and specific keyword as the value, so token
	 * keys with the value matching the tokenValue parameter are returned as an
	 * ArrayList of type String
	 * 
	 * @param tokenValue the keyword type to search for, e.g. newid
	 * @return an Array List of type string containing the keyword tokens
	 */
	public ArrayList<String> getKeywordTokenList(String tokenValue) {
		ArrayList<String> tokenList = new ArrayList<String>();

		for (String token : this.keywordTokens.keySet()) {
			if (this.keywordTokens.get(token).equals(tokenValue)) {

				tokenList.add(token);

			}
		}

		return tokenList;

	}

	/**
	 * Removes the keyword tokens from the statement's list that have a value
	 * matching the parameter passed. The value is the keyword id that the token
	 * represents. This method is useful for removing all of the keyword tokens for
	 * a particular keyword after they have been processed.
	 * 
	 * @param tokenValue the keyword type to search for, e.g. newid
	 */
	public void clearKeywordTokenList(String tokenValue) {
		ArrayList<String> tokensToDelete = new ArrayList<String>();

		for (String token : this.keywordTokens.keySet()) {
			if (this.keywordTokens.get(token).equals(tokenValue)) {

				tokensToDelete.add(token);

			}
		}

		for (String tokenToDelete : tokensToDelete) {

			this.keywordTokens.remove(tokenToDelete);

		}

	}

	public static String extractKeywordParameter(String text, String keyword) {
		String parameter = "";

		String regex = "^(" + keyword + ")\\s(\\w+)$";
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(text);

		if (matcher.find() && matcher.groupCount() == 2) {

			parameter = matcher.group(2);

		}
		return parameter;
	}

	public static String capFirstLetter(String theString) {
		String firstLetterCapped = theString.substring(0, 1).toUpperCase();

		String stringWithoutFirstLetter = theString.substring(1);

		return firstLetterCapped + stringWithoutFirstLetter;
	}

	/**
	 * Evaluates whether or not a regex expression is matched by a string. Returns
	 * true or false
	 */
	public static boolean checkStringRegex(String regex, String theString) {

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(theString);
		return matcher.matches();
	}

	/**
	 * 
	 * This method makes sure the first string is less than or equal to the second
	 * string, depending on the data type. For instance, the date 10/10/1999 is less
	 * than or equal to 10/10/1999 as dates and 10.2 is less than or equal to 11.11
	 * as decimals.
	 * 
	 * @param value1 the first value to compare as a string value
	 * @param value2 the second value to compare as a string value
	 * @param type   the data type of the values provided (integer, decimal, date,
	 *               time are valid)
	 * @return
	 */
	public static boolean isLessThanOrEqual(String value1, String value2, String type) {

		boolean result = false;

		switch (type) {

		case "text":
			try {

				/* The first value is used as the textual item to compare to the number */

				int value1Int = value1.length();
				int value2Int = DataTypes.intify(value2);

				if (value1Int <= value2Int) {

					result = true;
				}

			} catch (Exception e) {

			}

			break;

		case "integer":

			try {
				int value1Int = DataTypes.intify(value1);
				int value2Int = DataTypes.intify(value2);
				if (value1Int <= value2Int) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;

		case "decimal":

			try {
				double value1Decimal = DataTypes.decify(value1);
				double value2Decimal = DataTypes.decify(value2);
				if (value1Decimal <= value2Decimal) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;

		case "date":

			try {

				Date value1Date = DataTypes.datify(value1);
				Date value2Date = DataTypes.datify(value2);

				if (value1Date.compareTo(value2Date) <= 0) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;
		case "time":

			try {
				LocalTime value1Time = DataTypes.timify(value1);
				LocalTime value2Time = DataTypes.timify(value2);
				if (value1Time.compareTo(value2Time) <= 0) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;
		default:
			/* boolean is not evaluated */
		}

		return result;

	}

	/**
	 * 
	 * This method makes sure the first string is greater than the second string,
	 * depending on the data type. For instance, the date 10/10/1999 is greater than
	 * 07/01/1992 as dates and 12.24 is greater than 11.11 as decimals.
	 * 
	 * @param value1 the first value to compare as a string value
	 * @param value2 the second value to compare as a string value
	 * @param type   the data type of the values provided (integer, decimal, date,
	 *               time are valid)
	 * @return
	 */
	public static boolean isGreaterThan(String value1, String value2, String type) {

		boolean result = false;

		switch (type) {

		case "text":

			try {

				/* The first value is used as the textual item to compare to the number */

				int value1Int = value1.length();
				int value2Int = DataTypes.intify(value2);

				if (value1Int > value2Int) {

					result = true;
				}

			} catch (Exception e) {

			}

			break;

		case "integer":

			try {
				int value1Int = DataTypes.intify(value1);
				int value2Int = DataTypes.intify(value2);
				if (value1Int > value2Int) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;

		case "decimal":

			try {
				double value1Decimal = DataTypes.decify(value1);
				double value2Decimal = DataTypes.decify(value2);
				if (value1Decimal > value2Decimal) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;

		case "date":

			try {

				Date value1Date = DataTypes.datify(value1);
				Date value2Date = DataTypes.datify(value2);

				if (value1Date.compareTo(value2Date) > 0) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;
		case "time":

			try {
				LocalTime value1Time = DataTypes.timify(value1);
				LocalTime value2Time = DataTypes.timify(value2);
				if (value1Time.compareTo(value2Time) > 0) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;
		default:
			/* boolean is not evaluated */
		}

		return result;

	}

	/**
	 * 
	 * This method makes sure the first string is less than the second string,
	 * depending on the data type. For instance, the date 10/10/1999 is less than
	 * 11/30/1999 as dates and 10.2 is less than 11.11 as decimals.
	 * 
	 * @param value1 the first value to compare as a string value
	 * @param value2 the second value to compare as a string value
	 * @param type   the data type of the values provided (integer, decimal, date,
	 *               time are valid)
	 * @return
	 */

	public static boolean isLessThan(String value1, String value2, String type) {

		boolean result = false;

		switch (type) {

		case "text":
			try {

				/* The first value is used as the textual item to compare to the number */

				int value1Int = value1.length();
				int value2Int = DataTypes.intify(value2);

				if (value1Int < value2Int) {

					result = true;
				}

			} catch (Exception e) {

			}

			break;

		case "integer":

			try {
				int value1Int = DataTypes.intify(value1);
				int value2Int = DataTypes.intify(value2);
				if (value1Int < value2Int) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;

		case "decimal":

			try {
				double value1Decimal = DataTypes.decify(value1);
				double value2Decimal = DataTypes.decify(value2);
				if (value1Decimal < value2Decimal) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;

		case "date":

			try {

				Date value1Date = DataTypes.datify(value1);
				Date value2Date = DataTypes.datify(value2);

				if (value1Date.compareTo(value2Date) < 0) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;
		case "time":

			try {
				LocalTime value1Time = DataTypes.timify(value1);
				LocalTime value2Time = DataTypes.timify(value2);
				if (value1Time.compareTo(value2Time) < 0) {

					result = true;
				}

			} catch (InvalidConversionException e) {

			}

			break;
		default:
			/* boolean is not evaluated */
		}

		return result;

	}

	/*
	 * Populates the structured Statement member variables with the content of the
	 * statement as defined in statementString
	 */
	private void chunkStatement() {
		String segments[];

		String statementAfterOperation;

		for (String operationName : Definitions.operations.keySet()) {
			for (String opPhrase : Definitions.operations.get(operationName)) {

				/*
				 * The statement must start with a valid operation prefix followed by either a
				 * space or semicolon. For instance 'readjkd' is not considered the read
				 * operation, 'read' or 'read;' is though (semi colon is not part of the
				 * statement, it is the end delimiter).
				 */
				if (this.statementString.toLowerCase().startsWith(opPhrase + " ")
						|| this.statementString.toLowerCase().equals(opPhrase)) {

					this.operation = operationName;

					/* We don't want the operation to be case sensitive */
					segments = this.statementString.split("(?i)\\b" + opPhrase + "\\b", 2);
					if (segments.length > 1) {

						statementAfterOperation = segments[1].trim();

						/* we want the where clause to not be case sensitive */
						segments = statementAfterOperation.split("(?i)\\bwhere\\b", 2);
						this.argument = segments[0].trim();
						this.parseArgument();
						if (segments.length > 1) {
							this.whereCondition = segments[1].trim();
							this.parseWhereCondition();
						}
						return;
					}
				}
			}
		}
		/* At this point, the statement as been chunked as follows:
		 * <operation> <argument> <whereCondition> 
		 * Subsequently, argument and whereCondition will be parsed further (if they are present). */
	}

	/**
	 * This method parses the parameters within the argument and populates the
	 * following member variable: selectedNodeName, parentNames,
	 * childrenNamesValues, selectedNodeValue Does not touch anything beyond the
	 * main argument (e.g. a where condition).
	 */
	/* Todo: do not replace 'and' if it is in quotes */
	private void parseArgument() {

		/* Right off the bat this is the most simple case */
		this.selectedNodeName = this.argument;

		/*
		 * temporaryArgument will be sliced and diced into tiny little digital pieces
		 * depending on which tokens are encountered
		 */
		String temporaryArgument = this.argument;

		/* : Symbol: parse selected children names and values if they exist */
		if (temporaryArgument.contains(":")) {
			temporaryArgument = this.parseArgumentChildren(temporaryArgument);
		}

		/* . Symbol: parse parent names if they are present */
		if (temporaryArgument.contains(".")) {

			temporaryArgument = this.parseArgumentParents(temporaryArgument);

		}

		/* = Symbol: parse selected node value if it is present */
		if (temporaryArgument.contains("=")) {

			temporaryArgument = this.parseArgumentValue(temporaryArgument);

		}
	}

	/*
	 * Parses the selected node name's parents returns the modified temporary
	 * argument for further processing
	 */
	private String parseArgumentParents(String temporaryArgument) {

		/*
		 * We have to make sure the '.' is part of a parent.child structure and not part
		 * of a double value. Since parent.child structures cannot occur after the first
		 * instance of a '=' symbol, we only evaluate the statement for the '.' symbol
		 * up until the first occurrence of a '='. The rest of the string (the part
		 * after the first '=') is added back on after. Of course, if no '=' is present,
		 * this is irrelevant as no double values can occur without a '=' symbol.
		 */

		String restOfStatement = "";

		if (temporaryArgument.contains("=")) {

			String[] statementParts = temporaryArgument.split("=", 2);
			temporaryArgument = statementParts[0];
			restOfStatement = "=" + statementParts[1];
		}

		String[] parentNodeNames = temporaryArgument.split("\\.");

		for (String parentNodeName : parentNodeNames) {
			this.parentNames.add(parentNodeName.trim());
		}

		/*
		 * The LAST element is not one of the parents, it is the node name and must be
		 * separated.
		 */

		int lastParentNameIndex = parentNames.size() - 1;
		this.selectedNodeName = parentNames.get(lastParentNameIndex) + restOfStatement;
		parentNames.remove(lastParentNameIndex);
		temporaryArgument = this.selectedNodeName;

		return temporaryArgument;
	}

	/*
	 * Parses the value set to a selected node name in the argument returns the
	 * modified temporary argument for further processing
	 */
	private String parseArgumentValue(String temporaryArgument) {

		this.selectedNodeName = temporaryArgument.split("=")[0].trim();

		if (temporaryArgument.split("=").length == 2) {

			this.selectedNodeValue = temporaryArgument.split("=")[1].trim().replace("'", "").replace("\"", "");
			temporaryArgument = this.selectedNodeName;
		}

		return temporaryArgument;
	}

	/*
	 * Parses the selected node name's children and returns the modified temporary
	 * argument for further processing
	 */

	private String parseArgumentChildren(String temporaryArgument) {

		String childName = "";
		String childValue = "";

		this.selectedNodeName = temporaryArgument.split(":")[0].trim();

		if (temporaryArgument.split(":").length == 2) {

			String childrenString = temporaryArgument.split(":")[1].trim();
			String[] childStrings = childrenString.split(",");

			for (String childString : childStrings) {

				/* Parse out the child's value if it is set */
				if (childString.contains("=")) {

					childName = childString.split("=")[0].trim();

					if (childString.split("=").length == 2) {

						childValue = childString.split("=")[1].trim().replace("'", "").replace("\"", "");

					}

				}

				/* Otherwise, just capture the child's name */
				else {

					childName = childString;
					childValue = "";

				}

				if (childrenNamesValues.keySet().contains(childName.trim())) {
					this.addError("Children names cannot contain duplicates.");
				}

				this.childrenNamesValues.put(childName.trim(), childValue.trim());

			}

			temporaryArgument = this.selectedNodeName;
		}

		return temporaryArgument;
	}

	/* Parses the where condition to populate the whereConditionRules container. */

	private void parseWhereCondition() {
		String whereConditionString = this.whereCondition;
		String[] whereItems;

		// whereConditionString = whereConditionString.replace("and", ",");
		whereItems = whereConditionString.split(",");

		for (String whereItem : whereItems) {
			whereItem = whereItem.trim();

			/* try to parse for a match WITH a value... */
			if (!this.parseWhereConditionRuleWithValue(whereItem)) {

				/* otherwise try to parse for match WITHOUT a value... */
				this.parseWhereConditionRuleWithoutValue(whereItem);
			}

		}

	}

	/*
	 * Accepts a where condition rule as input and if the rule matches the regex for
	 * a condition rule with a value (e.g. a = 'b'), it will be added to the member
	 * variable whereConditionRules. If it does not match, no action is taken.
	 * Returns true if a match is found, otherwise it returns false.
	 */

	private boolean parseWhereConditionRuleWithValue(String whereItem) {
		String whereFieldName;
		String whereValue;

		/*
		 * This regex matches when a where condition has a value set, e.g. where a =
		 * 'b', c = 'd'
		 */
		String whereItemWithValueRegex = "^[\\w\\s]+\\s?=\\s?[\\w\\s]+$";

		if (!Statement.checkStringRegex(whereItemWithValueRegex, whereItem)) {
			return false;
		}
		whereFieldName = whereItem.split("=")[0].trim();
		whereValue = whereItem.split("=")[1].trim();

		/* If the key doesn't exist, add a new array */
		if (!this.whereConditionRules.containsKey(whereFieldName)) {
			ArrayList<String> whereValues = new ArrayList<String>();
			whereValues.add(whereValue);
			this.whereConditionRules.put(whereFieldName, whereValues);
		}
		/* If the key already exists, add the item to the array */
		else {
			this.whereConditionRules.get(whereFieldName).add(whereValue);
		}
		return true;
	}

	/*
	 * Accepts a where condition rule as input and if the rule matches the regex for
	 * a condition rule WITHOUT a value (e.g. a ), it will be added to the member
	 * variable whereConditionRules. A where condition rule without a value simply
	 * tests the presence of the field. If it does not match, no action is taken.
	 * Returns true if a match is found, otherwise it returns false.
	 */

	private boolean parseWhereConditionRuleWithoutValue(String whereItem) {
		/*
		 * This regex matches when a where condition does not have a value set, e.g.
		 * where a, b, c
		 */
		String whereItemWithoutValueRegex = "^[\\w\\s]+$";

		if (!Statement.checkStringRegex(whereItemWithoutValueRegex, whereItem)) {
			return false;
		} else {
			/*
			 * This array is intentionally empty. It indicates that there are no rules for
			 * this where condition other than the presence of the field (the where
			 * condition rule key).
			 */
			ArrayList<String> whereValues = new ArrayList<String>();
			this.whereConditionRules.put(whereItem, whereValues);
			return true;
		}

	}

	/*
	 * Operation specific syntax related errors are checked for in this method.
	 */
	private void checkForOpErrors() {

		switch (this.operation) {

		case "read":

			if (!this.selectedNodeValue.isEmpty()) {
				this.addError("Cannot set a value for the read operation.");
			}

			for (String childName : this.childrenNamesValues.keySet()) {
				if (!this.childrenNamesValues.get(childName).isEmpty()) {
					this.addError("Children names cannot have values for the read operation.");
					break;
				}
			}

			break;

		case "write":

			if ((this.selectedNodeName.equals("Root") && !this.selectedNodeValue.isEmpty())
					|| (this.childrenNamesValues.keySet().contains("Root"))) {
				this.addError("Root is not a valid node to be written to.");

			}

			/* Cannot write to reserved words */
			if (Definitions.reservedWords.contains(this.selectedNodeName)) {

				this.addError("Cannot write to reserved word '" + this.selectedNodeName + "'.");

			}

			break;

		case "delete":

			/* Cannot delete reserved words, e.g. 'rule' */
			if (Definitions.reservedWords.contains(this.selectedNodeName)) {

				this.addError("Cannot delete reserved word '" + this.selectedNodeName + "'.");

			}

			/* Cannot delete node rule names, e.g. max, min, type... */
			if (Definitions.nodeRuleNames.contains(this.selectedNodeName)) {

				this.addError("Cannot delete node rule '" + this.selectedNodeName + "'.");

			}

			/* Cannot delete root node. */
			if (this.selectedNodeName.equals("Root") || this.childrenNamesValues.keySet().contains("Root")) {

				this.addError("Cannot delete root node.");

			}

			/* Cannot assign a value to a node you are deleting, e.g. delete a = b */
			if (!this.selectedNodeValue.isEmpty()) {

				this.addError("Cannot set a value for the delete operation.");

			}

			/* Cannot use children nodes in a delete operation, e.g. delete a: b,c,d */
			if (!this.childrenNamesValues.isEmpty()) {

				this.addError("Child nodes cannot be used in the delete operation.");

			}

			break;

		case "rename":

			/* Cannot rename reserved words, e.g. 'rule' */
			if (Definitions.reservedWords.contains(this.selectedNodeName)) {

				this.addError("Cannot rename reserved word '" + this.selectedNodeName + "'.");

			}

			/* Cannot rename node rule names, e.g. max, min, type... */
			if (Definitions.nodeRuleNames.contains(this.selectedNodeName)) {

				this.addError("Cannot rename node rule '" + this.selectedNodeName + "'.");

			}

			if (this.selectedNodeName.equals("Root") || this.childrenNamesValues.keySet().contains("Root")) {

				this.addError("Root cannot be renamed.");

			}

			if (this.selectedNodeValue.isEmpty()) {

				this.addError("A value for renaming is required.");

			}

			if (!this.childrenNamesValues.isEmpty()) {

				this.addError("Child nodes cannot be used in the rename operation.");
			}

			break;

		case "enforce":

			/*
			 * Check that if 'rule' is NOT the last parent name (i.e. the rule is set like
			 * so rule.employeeName.max) then the selected node name must be a valid rule
			 * name
			 */
			int lastElementIndex = this.parentNames.size() - 1;

			if (!this.parentNames.get(lastElementIndex).equals("rule")) {

				if (!Definitions.nodeRuleNames.contains(this.selectedNodeName)) {

					this.addError("Invalid rule name.");

				}

			}

			/* Check is selected node name is a rule set name, it is valid */
			if (this.parentNames.get(lastElementIndex).equals("rule")) {

				if ((Definitions.reservedWords.contains(this.selectedNodeName)
						|| this.selectedNodeName.equals("Root"))) {

					this.addError("Invalid rule set name.");

				}
			}
			/* Check if the last parent name is a rule set name, it is valid */
			else if (this.parentNames.size() > 1 && this.parentNames.get(lastElementIndex - 1).equals("rule")) {

				if ((Definitions.reservedWords.contains(this.parentNames.get(lastElementIndex))
						|| this.parentNames.get(lastElementIndex).equals("Root"))) {

					this.addError("Invalid rule set name.");
				}

			}
			/*
			 * Check that if the selected node name is a rule name (e.g. max, min,
			 * childrenNamesValues must be empty
			 */
			if (Definitions.nodeRuleNames.contains(this.selectedNodeName)) {

				if (this.childrenNamesValues.size() > 0) {

					this.addError("Cannot set grandchildren for a rule definition node.");

				}
			}

			/*
			 * Check that the rule set name cannot have a value set for it, only for its
			 * children. For instance rule.ruleSet = abc or rule.ruleSet = abc: max = 10,
			 * min = 5 is invalid
			 */
			if (this.parentNames.get(lastElementIndex).equals("rule") && !this.selectedNodeValue.isEmpty()) {
				this.addError("rule." + selectedNodeName + " cannot have a value written to it.");
			}

			/*
			 * Check that the rule names are valid if the rules are defined by the children
			 * nodes
			 */
			for (String ruleName : this.childrenNamesValues.keySet()) {

				if (!Definitions.nodeRuleNames.contains(ruleName)) {

					this.addError(ruleName + " is an invalid rule name.");

				}
			}

			/*
			 * 'enforce' is then further checked by the interpreter because access to the
			 * data is required.
			 */

			break;

		case "exit":
			if (!this.checkForExtraCharacters("exit")) {
				this.addError("Exit should not have any arguments.");
			}
			break;

		case "begin":
			if (!this.checkForExtraCharacters("begin")) {
				this.addError("Begin should not have any arguments.");
			}
			break;

		case "commit":
			if (!this.checkForExtraCharacters("commit")) {
				this.addError("Commit should not have any arguments.");
			}
			break;

		case "rollback":
			if (!this.checkForExtraCharacters("rollback")) {
				this.addError("Rollback should not have any arguments.");
			}
			break;

		default:
			// do nothing
		}

	}

	/*
	 * This method is used to check if an operation that requires no arguments has
	 * any extra characters (except for the ';' ending and whitespace) following the
	 * operation call. Returns true if it does not, returns false if extra
	 * characters are encountered.
	 */
	private boolean checkForExtraCharacters(String operationName) {
		String[] operationPhrases = Definitions.operations.get(operationName);
		String temporaryStatementString = this.statementString.replace(" ", "");
		if (Arrays.asList(operationPhrases).contains(temporaryStatementString)) {
			return true;
		} else
			return false;
	}

	/*
	 * Characters must be 0-9, A-Z, a-z, ',', and a maximum of ONE semicolon and one
	 * colon. Space, underscores are allowed. IF single quotes, double quotes are
	 * present there must be an even amount of them. Opening parenthesis requires a
	 * closing parenthesis
	 */
	/*
	 * This does lexical analysis, but nothing further. For instance it will check
	 * that the same number of open and close parenthesis exist but it will assume
	 * something like this is valid: ')('
	 */
	private boolean checkCharacters(String stringToCheck) {

		/* An empty string passes the check */
		if (stringToCheck.length() == 0) {
			return true;
		}
		/* Check valid characters */
		String regex = "^[a-zA-Z0-9-+;:\"\'_\\s(),=./\\\\]+$";

		if (!Statement.checkStringRegex(regex, stringToCheck)) {

			return false;
		}

		/* Check the cardinality of certain characters */
		if (this.countCharacterRegex(stringToCheck, "[']+?") % 2 == 1
				|| this.countCharacterRegex(stringToCheck, "[\"]+?") % 2 == 1
				|| this.countCharacterRegex(stringToCheck, "[)]+?") != this.countCharacterRegex(stringToCheck, "[(]+?")
				|| this.countCharacterRegex(stringToCheck, "[:]+?") > 1) {
			return false;
		}

		return true;
	}

	/**
	 * Evaluates the number of times a certain character sequence appears in a
	 * String.
	 * 
	 * @param stringToCheck     The String
	 * @param theCharacterRegex regex sequence to search for number of occurrences
	 * @return integer with the number of matches
	 */
	private int countCharacterRegex(String stringToCheck, String theCharacterRegex) {
		Pattern pattern = Pattern.compile(theCharacterRegex);
		Matcher matcher = pattern.matcher(stringToCheck);

		int count = 0;
		while (matcher.find()) {
			count++;
		}

		return count;
	}

	/**
	 * This method checks to see if a statement is an enforce statement, meaning it
	 * is writing rules. Only evaluated if the operation starts off as a 'write'.
	 * Most certainly NOT as an 'error'.
	 */
	private void detectEnforce() {

		if (this.operation.equals("write")) {

			if ((this.parentNames.size() > 0 && this.parentNames.get(0).equals("rule")) || (this.parentNames.size() > 1
					&& this.parentNames.get(0).equals("Root") && this.parentNames.get(1).equals("rule"))) {

				this.operation = "enforce";
			}

		}
	}

	/**
	 * This method hides tokens (substrings) extracted from the statement so that
	 * these tokens will not be evaluated by the lexer or parser.
	 *
	 * Here are the instances so far where the token hiding is needed: 1) Quotes -
	 * When quotes are used for the value of nodes by the user, the content of the
	 * quotes should not be evaluated for restricted characters, words, etc. Some
	 * further restrictions may still apply and will be added later. 2) Times -
	 * Times use a colon in the format, which is problematic because a colon is used
	 * to indicate that children of a node are about to be listed. Therefore, time
	 * values are hidden from the lexer and parser and placed back in to the
	 * Statement after the validation is complete.
	 * 
	 * NOTE: Although hidden tokens do not have the usual checks performed, the text
	 * of the hidden tokens must still be valid unicode, so the token hiding does
	 * not take place until after this valid textual check is performed.
	 * 
	 * The member variable hiddenTokens is populated with the tokens found in this
	 * method, and the Statement is modified to switch these tokens with unique
	 * random strings. The method putTokensBack() will put the tokens back into the
	 * Statement and empty the hiddenTokens container. The hiddenTokens container is
	 * not emptied until putTokensBack() is called, therefore this method can be
	 * called repeatedly with different regex expressions for different token types.
	 * A regex String is passed to this method to describe the tokens that should be
	 * made hidden.
	 * 
	 * @param regex This expression identifies tokens. Tokens are created for each
	 *              result matching the regex.
	 * 
	 */

	private void hideTokens(String regex) {
		String uniqueId = "";
		String tokenValue = "";

		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

		Matcher matcher = pattern.matcher(this.statementString);

		while (matcher.find()) {

			tokenValue = matcher.group();

			/*
			 * Generate a unique id that doesn't conflict with any strings currently inside
			 * the statement
			 */
			do {

				uniqueId = Randomizer.text(tokenValue.length());

			} while (this.countCharacterRegex(this.statementString, uniqueId) > 0);

			/* Token is replaced in the original full statement. */
			this.statementString = this.statementString.replace(tokenValue, uniqueId);

			/* If the token starts and ends with a double quote, strip them off */
			if ((tokenValue.startsWith("\"") && tokenValue.endsWith("\""))
					|| (tokenValue.startsWith("\'") && tokenValue.endsWith("\'"))) {
				tokenValue = tokenValue.substring(1, tokenValue.length() - 1);
			}

			this.hiddenTokens.put(uniqueId, tokenValue);
		}

	}

	/**
	 * Puts all of the hidden tokens back inside the statement string and other
	 * statement variables. Ideally, this is executed after the validation has
	 * completed. Additionally, the hiddenTokens container is emptied.
	 * Postcondition: Hidden tokens are placed back into statement variables and
	 * hiddenTokens is emptied.
	 */

	private void putTokensBack() {

		LinkedHashMap<String, String> newChildrenNamesValues = new LinkedHashMap<String, String>();
		String newChildName = "";
		String newChildValue = "";
		String tokenValue = "";
		String childValue = "";

		for (String uniqueId : this.hiddenTokens.keySet()) {

			/*
			 * After the parsing has been complete, all fields which contain the replaced
			 * uniquId are populated instead with the original hidden token value
			 */
			tokenValue = this.hiddenTokens.get(uniqueId);
			this.statementString = this.statementString.replace(uniqueId, tokenValue);
			this.selectedNodeName = this.selectedNodeName.replace(uniqueId, tokenValue);
			this.selectedNodeValue = this.selectedNodeValue.replace(uniqueId, tokenValue);

			for (String childName : this.childrenNamesValues.keySet()) {

				childValue = this.childrenNamesValues.get(childName);

				newChildName = childName.replace(uniqueId, tokenValue);
				newChildValue = childValue.replace(uniqueId, tokenValue);

				newChildrenNamesValues.put(newChildName, newChildValue);
			}

		      this.childrenNamesValues = new LinkedHashMap<String, String>();
              this.childrenNamesValues.putAll(newChildrenNamesValues);
              
			/* Where Condition Rules */
			LinkedHashMap<String, ArrayList<String>> newWhereConditionRules = new LinkedHashMap<String, ArrayList<String>>();

			String newRuleElement;

			String newWhereConditionRuleName;

			ArrayList<String> newWhereConditionRule;

			this.whereCondition = this.whereCondition.replace(uniqueId, tokenValue);

			for (String whereConditionRuleName : this.whereConditionRules.keySet()) {

				newWhereConditionRuleName = whereConditionRuleName.replace(uniqueId, tokenValue);

				newWhereConditionRule = new ArrayList<String>();

				for (String ruleElement : this.whereConditionRules.get(whereConditionRuleName)) {
					newRuleElement = ruleElement.replace(uniqueId, tokenValue);

					newWhereConditionRule.add(newRuleElement);
				}

				newWhereConditionRules.put(newWhereConditionRuleName, newWhereConditionRule);

			}

			this.whereConditionRules = new LinkedHashMap<String, ArrayList<String>>();
            this.whereConditionRules.putAll(newWhereConditionRules);
            
		}
		/* The hidden tokens container is then emptied */
		this.hiddenTokens = new LinkedHashMap<String, String>();
	}

	/**
	 * Makes all keywords unique so they can be processed inside childrenNamesValues
	 * when multiple occurrences appear such as two children named 'newid'. The
	 * replaced unique id appends a digit to the unique id, which will be ignored by
	 * the interpreter. Postcondition: statementString is modified to have all
	 * keywords occurring repeatedly replaced
	 */

	private void makeKeywordsTokens() {
		int keywordCount, keywordIndex, keywordLength, lastCharIndex;
		String uniqueToken, newStatementString, firstChar, lastChar;
		String regex;
		
		for (String keyword : Definitions.keywords) {
			String chars = "[\\.\"\'=:,;\\s]{1,1}";
			regex = chars + keyword + chars + "|" + chars + keyword + "$";
			
			keywordCount = this.countCharacterRegex(this.statementString, regex);

			if (keywordCount < 2) {
				continue;
			}

			//newStatementString = this.statementString + " ";
			newStatementString = this.statementString; 
					
			for (int i = 0; i < keywordCount; i++) {

				do {

					uniqueToken = Randomizer.text(6);

				} while (this.countCharacterRegex(this.statementString, uniqueToken) > 0);

				/* replaceFirst is used in order to replace in a loop 
				 * and have control over what goes on during each replacement. */
			
				firstChar = "";
				lastChar = "";
				
				keywordLength = keyword.length();
				keywordIndex = newStatementString.indexOf(keyword);

				if (keywordIndex > 0) {

					firstChar = String.valueOf(newStatementString.charAt(keywordIndex - 1));
				
				}
			
				lastCharIndex = keywordIndex + keywordLength;
				if (newStatementString.length() != (lastCharIndex)) {

					lastChar = String.valueOf(newStatementString.charAt(lastCharIndex));
			 	}

				newStatementString = newStatementString.replaceFirst(regex, firstChar + uniqueToken + lastChar);

				/*
				 * Keep track of our keyword tokens so the Interpreter can process them properly
				 */
				this.keywordTokens.put(uniqueToken, keyword);
			}

			this.statementString = newStatementString;

		}
	}

	/**
	 * Counts the number of times a given value appears in a node hierarchy. For
	 * instance in a.b.a.c, a appears twice.
	 * 
	 * @param value The value to count.
	 * @return an integer that is the number of occurrences
	 */
	private int getNodeHierarchyNameCount(String value) {

		int numberOfOccurences = 0;

		for (String nodeName : this.getNodeHierarchy()) {

			if (nodeName.equals(value)) {

				numberOfOccurences++;

			}
		}

		return numberOfOccurences;
	}

	private boolean processTimesKeyword() {

		int countTimes = this.countCharacterRegex(this.statementString, " Times");
		String parameter;
		int numberOfIterations;

		if (countTimes == 0) {

			return true;

		} else if (countTimes > 1) {

			this.addError("The Times keyword can only be used once in a statement.");
			return false;

		}

		if (this.statementString.matches(".*?Times [\\d]{1,7}$")) {

			if (this.statementString.split("Times ").length > 1) {

				parameter = this.statementString.split("Times ")[1];

				this.statementString = this.statementString.split("Times ")[0]; 
				try {
					numberOfIterations = DataTypes.intify(parameter);
					
					if (numberOfIterations > 9999999) {
						this.addError("Only 9,999,999 iterations are allowed.");
					
						return false;
						
					}
					
					this.setIterations(numberOfIterations);
					
				} catch (InvalidConversionException e) {
					/* This should not occur */
					this.addError("Invalid number of number of iterations.");
					return false;
				}
			}
		}

		return true;
	}
}