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

The Rut Database Interpreter is the engine that runs Rut Database operations.

 Instructions are passed to the interpreter as Statement objects.
 For each Statement received, a String is returned. 
 Improper Statements return a string describing the error. 
 Interpreter makes good use of it's MemoryStorage object for accessing, traversing, and manipulating nodes.
 Interpreter also uses the DiskStorage object heavily for reading and writing to permanent storage.
 Once an Interpreter object is created, it can be passed to a Shell object as a constructor argument. 
 Using the Shell with the Interpreter allows for direct user/file/application interaction.
 
 The main public Interpreter methods are as follows:
 * String processStatement(Statement)
 
 * boolean getKillSignal()
 
 * boolean getWriteToDiskSignal()
 
 * long getUid()  
 
 * long generateUid()
 
 Todo: read employeeFirstName where employeeFirstName = ...
        write employeeFirstName where employeeFirstName = ...
        doesn't work because when employeefirstName is selected,
         we cannot evaluate the parent
 This needs to be corrected!
*/

package rut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import rut.keywords.Decimal;
import rut.keywords.FirstNameFemale;
import rut.keywords.FirstNameMale;
import rut.keywords.Keyword;
import rut.keywords.Newid;
import rut.keywords.Text;
import rut.keywords.Boolean;
import rut.keywords.Date;

//import rut.keywords.Integer;

import rut.keywords.Time;
import rut.operation.InvalidOperationException;
import rut.operation.Operation;
import rut.operation.OperationFactory;
import rut.keywords.LastName;
import rut.utilities.DataTypes;

public class Interpreter {

	private boolean killSignal;
	private boolean writeToDiskSignal;
	private boolean suppressOutputSignal;
	private MemoryStorage memory;
	private DiskStorage disk;

	public Interpreter(MemoryStorage memory, DiskStorage disk) {

		/*
		 * These variables are immutable, once the interpreter object is created they
		 * cannot be changed.
		 */
		this.memory = memory;
		this.disk = disk;

		/* Reset variables that can be changed */
		this.reset();

	}

	/*
	 * Resets all of the interpreter's member variables that can be changed to their
	 * default values
	 */
	public void reset() {

		this.writeToDiskSignal = false;
		this.suppressOutputSignal = false;

	}

	public String generateErrorResponse(Set<String> errorMessages) {
	
		String errorResponse = "";
		if (errorMessages.size() > 0) {

			errorResponse = String.join("\n", errorMessages);

		}		
		
		return errorResponse;
	}
	
	public String processStatement(Statement statement) {
		
		
		/*
		Set<String> errorMessages = statement.getErrorMessages();
		if (errorMessages.size() > 0) {

			return String.join("\n", errorMessages);

		}
		*/
		int iterations = statement.getIterations();
		StringBuilder response = new StringBuilder();
		String iterationResponse = "";
		String statementText = statement.getOriginalStatementString();
		LinkedHashMap<String, Integer> responses = new LinkedHashMap<String, Integer>();

		/*
		 * The number of times a specific response message is displayed, this is tallied
		 */
		int responseDisplayNumber = 1;

		for (int i = 0; i < iterations; i++) {

			iterationResponse = this.runStatementIteration(statement);

			if (responses.keySet().contains(iterationResponse)) {

				responseDisplayNumber = responses.get(iterationResponse).intValue() + 1;

			} else {

				responseDisplayNumber = 1;

			}

			responses.put(iterationResponse, responseDisplayNumber);

			if (iterations > 1) {

				statement.parseStatement(statementText);

			}
		}

		/* Save changes to disk. */
		if (this.writeToDiskSignal && !this.suppressOutputSignal) {
			this.disk.writeDataMapToDisk(this.memory.getDataMap());
		}

		/* Clear interpreter signal variables in preparation for next statement */
		this.reset();

		int responseNumber = 0;

		for (String responseMessage : responses.keySet()) {

			responseNumber = responses.get(responseMessage);
			if (iterations > 1) {

				response.append(responseMessage + " (*" + responseNumber + ")\n");

			}

			else {

				response.append(responseMessage);
			}
		}
		return response.toString();
	}

	public String runStatementIteration(Statement statement) {
		
		String response = "";
		
		/*
		 * Keywords that are specific to node traversal (root, child) are processed
		 * later when the node trees are traversed
		 */
		this.processKeywords(statement);
		/*
		 * The second and final level of error checking is performed. This type of error
		 * checking is called interpreter error checking, error checking that requires
		 * access to the data and cannot be done by the statement parser based on the
		 * statement syntax alone. This error checking is only done if the operation is
		 * currently one of the operations that checkForOpErrors tests for.
		 */
		String[] operationsToCheck = { "write", "enforce" };
		ArrayList<String> operationsToCheckFor = new ArrayList<String>(Arrays.asList(operationsToCheck));

		/* After checkForOpErrors runs, if operation errors are found they will 
		 * be added to the error messages container. */
		if (operationsToCheckFor.contains(statement.getOperation())) {
			
			this.checkForOpErrors(statement);
			
		}
		
		Set<String> errorMessages = statement.getErrorMessages();
		
		if (errorMessages.size() > 0) {
			return generateErrorResponse(errorMessages);
		}	
		
		try {
			Operation operation = OperationFactory.createOperation(statement, this.memory);
			response = operation.execute();
			this.writeToDiskSignal = this.memory.getWriteToDiskSignal();
			this.killSignal = this.memory.getKillSignal();
		} catch (InvalidOperationException e) {
			return "Could not execute operation due to a system error.";
		}
		
		return response;
	}
	
	public boolean getWriteToDiskSignal() {
		return this.writeToDiskSignal;
	}

	public boolean getSuppressOutputSignal() {
		return this.suppressOutputSignal;
	}

	public void setSuppressOutputSignal(boolean suppressOutputSignal) {
		this.suppressOutputSignal = suppressOutputSignal;
	}

	public MemoryStorage getMemory() {
		return this.memory;
	}

	public void setMemory(MemoryStorage memory) {
		this.memory = memory;
	}

	public DiskStorage getDisk() {
		return this.disk;
	}

	public void setDisk(DiskStorage disk) {
		this.disk = disk;
	}
	
	public boolean getKillSignal() {
		return this.killSignal;
	}
	/**
	 * The error checking performed by this method is only done after the
	 * instruction was accepted by the parser to be valid. Certain errors are data
	 * related rather than syntax related, this is where the interpreter's error
	 * checker comes into play. Interpreter.checkForOpErrors() has several helper
	 * methods that check for a variety of errors. Error operation is triggered and
	 * individual error messages are added within these helper methods.
	 * 
	 * @param selectedNodeName
	 * @param childrenNamesValues
	 * @return
	 */
	private boolean checkForOpErrors(Statement statement) {

		boolean result = true;

		ArrayList<String> parentNames = statement.getParentNames();
		String selectedNodeName = statement.getSelectedNodeName();
		String selectedNodeValue = statement.getSelectedNodeValue();
		LinkedHashMap<String, String> childrenNamesValues = statement.getChildrenNamesValues();

		switch (statement.getOperation()) {

		case "enforce":

			String ruleSetName = this.resolveRuleSetName(selectedNodeName, parentNames);
			String type = this.resolveRuleValue("type", ruleSetName, statement);

			/* Set the default value for type */
			if (type.isEmpty()) {
				type = "text";
			}

			String max = this.resolveRuleValue("max", ruleSetName, statement);
			String min = this.resolveRuleValue("min", ruleSetName, statement);

			String ruleValue = "";

			LinkedHashMap<String, String> rulesToSet = new LinkedHashMap<String, String>();

			/*
			 * Make sure that the ruleSetName is already set if selectedNodeName is not the
			 * ruleSetName
			 */
			if (!ruleSetName.equals(selectedNodeName)) {

				Node testNode = this.memory.getNodeByHierarchy(statement.getNodeHierarchyString(), true);
				if (testNode == null) {

					statement.addError("Rule set " + ruleSetName
							+ " does not exist. Either create the rule set first, or set the rule using the parent/children syntax ' write rule.ruleset: rule1 = ..., rule2 = ..., etc.'.");

				}
			}

			/* If the selected node name is a rule, it goes in the list of rules to set */
			if (Definitions.nodeRuleNames.contains(selectedNodeName)) {

				rulesToSet.put(selectedNodeName, selectedNodeValue);

			}
			/* Otherwise the rules to set are the children names values */
			else {
				rulesToSet = childrenNamesValues;
			}

			/* Check that the rule values are valid */
			for (String ruleName : rulesToSet.keySet()) {

				ruleValue = rulesToSet.get(ruleName);

				/* Rules must have valid values */
				if (!this.checkEnforcedRuleValue(ruleName, ruleValue, type, max, min, statement)) {

					result = false;
				}
			}

			/* Rules must not conflict with the current values their respective nodes */
			if (!this.checkEnforceForRules(statement, type)) {

				result = false;
			}

			break;

		case "write":

			/*
			 * Make sure that the value being set does not violate the enforced rules set
			 * for that node name.
			 */
			/*
			 * if (!this.checkWriteForRules(statement)) {
			 * 
			 * result = false;
			 * 
			 * }
			 */
			break;
		default:
			/* do nothing */

		}

		return result;
	}

	/**
	 * Processes keywords as defined by their keyword method. The Statement's values
	 * are changed after this method is complete.
	 * 
	 * @param statement the Statement to perform keyword processing on
	 */
	private void processKeywords(Statement statement) {

		ArrayList<Keyword> keywords = new ArrayList<Keyword>();

		keywords.add(new Newid(this.memory));
		keywords.add(new FirstNameMale(this.memory));
		keywords.add(new FirstNameFemale(this.memory));
		keywords.add(new LastName(this.memory));
		keywords.add(new Time(this.memory));
		keywords.add(new Date(this.memory));
		keywords.add(new Text(this.memory));
		keywords.add(new rut.keywords.Integer(this.memory));
		keywords.add(new Decimal(this.memory));
		keywords.add(new Boolean(this.memory));

		for (Keyword keyword : keywords) {

			keyword.execute(statement);

		}

	}

	/**
	 * If a write operation is performed where a rule is being written, it will be
	 * handled by this method. Prerequisite: Syntax level checking has been
	 * performed. For instance a rule definitions such as: rule.employeeName.lol
	 * cannot be passed.
	 * 
	 * @param selectedNodeName
	 * @param childrenNamesValues
	 * @return
	 */

	private String executeOpEnforce(String selectedNodeName, String selectedNodeValue,
			LinkedHashMap<String, String> childrenNamesValues, ArrayList<String> parentNames,
			LinkedHashMap<String, ArrayList<String>> whereConditionRules) {

		/* This is the node name that the rules will be children of */
		String ruleSetName = this.resolveRuleSetName(selectedNodeName, parentNames);

		//this.executeOpWrite(selectedNodeName, selectedNodeValue, childrenNamesValues, parentNames, whereConditionRules);

		/* This skips rules if they already exist */
		this.memory.createDefaultRuleSet(ruleSetName);

		return "Rules set for " + ruleSetName + ".";
	}


	/*
	 * This is a helper method for executeOpRead that produces unformatted lists of
	 * node names/values (i.e. results) read from the database. These lists will
	 * later be combined for the final output.
	 */
	
	/**
	 * This operation writes child nodes of a selected node and optionally populates
	 * them with values.
	 * 
	 * @param parentNode          the parent node to write to
	 * @param childrenNamesValues the children and values to write for the parent
	 *                            node
	 * @return the number of children nodes
	 */
	private int generateChildrenNodes(Node parentNode, LinkedHashMap<String, String> childrenNamesValues) {
		String childValue = "";
		Node childNode;

		for (String childName : childrenNamesValues.keySet()) {
			childValue = childrenNamesValues.get(childName);

			childNode = new Node();
			if (childValue.isEmpty()) {

				childNode.setValue("");

			} else {

				childNode.setValue(childValue);

			}

			LinkedHashMap<String, Node> nodeChildren = parentNode.getChildren();

			nodeChildren.put(childName, childNode);
			parentNode.setChildren(nodeChildren);
		}

		return childrenNamesValues.keySet().size();
	}

	/**
	 * Checks to make sure that a write statement adheres to enforced rules. Checks
	 * both the selected node name and selected node name's children if they were
	 * selected for write.
	 * 
	 * @param statement
	 * @return boolean result
	 */
	private boolean checkWriteForRules(Statement statement) {
		boolean result = true;

		String selectedNodeName = statement.getSelectedNodeName();
		String selectedNodeValue = statement.getSelectedNodeValue();
		LinkedHashMap<String, String> childrenNamesValues = statement.getChildrenNamesValues();

		LinkedHashMap<String, String> rules = this.memory.getRulesByRuleSetName(selectedNodeName);

		if (!rules.isEmpty()) {

			for (String rule : rules.keySet()) {

				result = this.checkEnforcedNodeValue(selectedNodeName, selectedNodeValue, rule, rules.get(rule),
						rules.get("type"), statement);

				if (!result) {

					return result;

				}
			}
		}

		if (!childrenNamesValues.isEmpty()) {

			for (String childName : childrenNamesValues.keySet()) {

				String childValue = childrenNamesValues.get(childName);
				rules = this.memory.getRulesByRuleSetName(childName);

				if (!rules.isEmpty()) {

					for (String rule : rules.keySet()) {

						result = this.checkEnforcedNodeValue(childName, childValue, rule, rules.get(rule),
								rules.get("type"), statement);

						if (!result) {

							return result;

						}
					}

				}
			}

		}

		return result;
	}

	/**
	 * Checks to make sure that an enforce statement enforces rules that are
	 * consistent with the node's current value.
	 * 
	 * @param statement
	 * @return boolean result
	 */
	private boolean checkEnforceForRules(Statement statement, String resolvedType) {

		boolean result = true;
		ArrayList<String> parentNames = statement.getParentNames();
		String selectedNodeName = statement.getSelectedNodeName();
		String ruleSetName = this.resolveRuleSetName(selectedNodeName, parentNames);
		String ruleValue;
		ArrayList<Node> foundNodes = this.memory.getNodesByName(ruleSetName);

		if (foundNodes.size() == 0) {
			return true;
		}

		for (Node foundNode : foundNodes) {

			for (String ruleName : Definitions.nodeRuleNames) {
				ruleValue = this.resolveRuleValue(ruleName, ruleSetName, statement);

				result = this.checkEnforcedNodeValue(ruleSetName, foundNode.getValue(), ruleName, ruleValue,
						resolvedType, statement);

				if (!result) {

					return result;

				}
			}

		}

		return result;
	}

	/**
	 * Checks a constraint to make sure it's value adheres to the node's type. Used
	 * for checking min and max.
	 * 
	 * @param constraintValue the value of the min or max being set (the rule name
	 *                        doesn't matter)
	 * @param typeValue       the type of the node
	 * @return
	 */
	private boolean checkConstraintForType(String constraintValue, String typeValue) {

		boolean result = true;

		/* Empty values are always allowed for constraints */
		if (constraintValue.isEmpty()) {

			return result;

		}

		switch (typeValue) {

		case "text":

			/*
			 * For text, min or max must be POSITIVE integers, the integer must be converted
			 * so it can be evaluated and verified to be greater than 0
			 */

			result = DataTypes.checkInteger(constraintValue);

			int constraintValueInt = 0;
			if (result) {

				try {

					constraintValueInt = DataTypes.intify(constraintValue);

				} catch (InvalidConversionException e) {

					/*
					 * This should not occur, the type is checked before the conversion is made...
					 */
					result = false;
					break;

				}

				if (constraintValueInt < 0) {
					result = false;
				}

			}

			break;

		case "integer":

			/* For integer, min or max must be integers */
			result = DataTypes.checkInteger(constraintValue);

			break;

		case "boolean":

			/* For boolean nodes, min or max cannot be set */
			result = constraintValue.isEmpty();

			break;

		case "decimal":

			/* For decimal, min or max must be decimals */
			result = DataTypes.checkDecimal(constraintValue);

			break;

		case "date":

			/* For dates min or max must be dates */
			result = DataTypes.checkDate(constraintValue);

			break;

		case "time":

			/* For time, min or max must be times */
			result = DataTypes.checkTime(constraintValue);

			break;

		default:
			/* do nothing */

		}

		return result;
	}

	/**
	 * Checks to make sure min is not greater than max. Prerequisite: The statement
	 * has passed syntax error checking. Min and Max being '0' or '-1' are excluded
	 * from being evaluated.
	 * 
	 * @param selectedNodeName    - the name of the node being enforced
	 * @param childrenNamesValues - the enforced rules and their values
	 * @param statementErrors
	 * @return boolean result - true or false
	 */
	private boolean checkMinNotGreaterMax(String resolvedMax, String resolvedMin, String resolvedType) {

		boolean result = true;

		/* Nothing is evaluated if min or max are empty */
		if (resolvedMin.isEmpty() || resolvedMax.isEmpty()) {

			return true;

		}

		if (!Statement.isLessThanOrEqual(resolvedMin, resolvedMax, resolvedType)) {

			result = false;

		}

		return result;
	}

	/**
	 * * Checks that the value of ruleValue has a valid syntax and value (
	 * consistent with the other rules defined for the node) for the ruleName that
	 * is passed to this method. Several extra parameters are included for min and
	 * max evaluation.
	 * 
	 * @param ruleName
	 * @param ruleValue
	 * @param resolvedType
	 * @param resolvedMax
	 * @param resolvedMin
	 * @param statement    - statement's errorMessages has items added to it when an
	 *                     error is discovered
	 * @return
	 */
	private boolean checkEnforcedRuleValue(String ruleName, String ruleValue, String resolvedType, String resolvedMax,
			String resolvedMin, Statement statement) {

		boolean result = true;

		switch (ruleName) {
		case "type":

			if (!Definitions.nodeRuleTypes.contains(ruleValue)) {
				statement.addError("Invalid value for type.");
				result = false;
			}
			break;

		case "max":
		case "min":

			/* Max and min's values must adhere to the type they are applied to... */
			if (!this.checkConstraintForType(ruleValue, resolvedType)) {

				statement.addError(ruleValue + " is an invalid value for the rule " + ruleName
						+ " when the node's type is " + resolvedType + ".");

				result = false;
			}

			if (!this.checkMinNotGreaterMax(resolvedMax, resolvedMin, resolvedType)) {

				statement.addError("Min cannot be greater than max.");

				result = false;
			}

			break;

		case "unique":
		case "required":
		case "key":
			if (!ruleValue.equals("false") && !ruleValue.equals("true")) {
				result = false;
			}
			break;

		default:
			/* do nothing */
		}

		return result;

	}

	/**
	 * Checks that the value of nodeValue is valid for the ruleName/ruleValue
	 * passed.
	 * 
	 * @param nodeName
	 * @param nodeValue
	 * @param ruleName
	 * @param ruleValue
	 * @param type
	 * @param statement - passed for quick access to its variables and members,
	 *                  specifically related to error messages
	 * @return
	 */
	private boolean checkEnforcedNodeValue(String nodeName, String nodeValue, String ruleName, String ruleValue,
			String type, Statement statement) {
		boolean result = true;

		String operation = statement.getOperation();

		switch (ruleName) {
		case "type":

			/* Empty values do not have their type evaluated. */
			if (nodeValue.isEmpty()) {
				break;
			}

			switch (ruleValue) {
			case "text":

				/*
				 * No special checking is done for text, 'checkText' is performed on all written
				 * values to ensure it is 100% Unicode - BASIC_LATIN.
				 */

				break;

			case "integer":

				if (!DataTypes.checkInteger(nodeValue)) {

					statement.addError("Enforced Rule Violation: " + nodeName + " must have a valid integer value.");

					result = false;
				}

				break;

			case "boolean":

				if (!DataTypes.checkBoolean(nodeValue)) {

					statement.addError("Enforced Rule Violation: " + nodeName + " must have a valid boolean value.");

					result = false;
				}

				break;

			case "decimal":

				if (!DataTypes.checkDecimal(nodeValue)) {

					statement.addError("Enforced Rule Violation: " + nodeName + " must have a valid decimal value.");

					result = false;
				}

				break;

			case "date":

				if (!DataTypes.checkDate(nodeValue)) {

					statement.addError("Enforced Rule Violation: " + nodeName
							+ " must have a valid date value in mm/dd/yyyy format.");

					result = false;
				}

				break;

			case "time":

				if (!DataTypes.checkTime(nodeValue)) {

					statement.addError("Enforced Rule Violation: " + nodeName
							+ " must have a valid time value in HH:mm:ss format.");

					result = false;
				}

				break;
			default:
				/* do nothing */
			}

			break;

		case "max":

			if (!ruleValue.isEmpty() && Statement.isGreaterThan(nodeValue, ruleValue, type)) {

				statement.addError("Enforced Rule Violation: the max value cannot be less than " + nodeName + ".");

				result = false;
			}

			break;

		case "min":

			if (!ruleValue.isEmpty() && Statement.isLessThan(nodeValue, ruleValue, type)) {

				statement.addError("Enforced Rule Violation: the min value cannot be greater than " + nodeName + ".");

				result = false;
			}

			break;

		case "required":

			if (ruleValue.equals("true") && nodeValue.isEmpty()) {

				statement.addError(
						"Enforced Rule Violation: " + nodeName + " is a required field and cannot be left blank.");

				result = false;
			}

			break;

		case "key":
			if (ruleValue.equals("true") && nodeValue.isEmpty()) {

				statement
						.addError("Enforced Rule Violation: " + nodeName + " is a key field and cannot be left blank.");

				result = false;
			}

			break;

		case "unique":
			if (ruleValue.equals("true")) {

				/*
				 * We have to handle this separately for trying to enforce 'unique' on a node
				 * name that has non-unique values.
				 */
				if (operation.equals("enforce")) {
					if (!this.memory.isUnique(nodeName, nodeValue)) {

						statement.addError("Enforced Rule Violation: Nodes named " + nodeName + " are not unique.");

						result = false;
						break;
					}
				}

				/*
				 * We also have to handle when trying to write non-unique values to a node name
				 * that is unique
				 */

				if (operation.equals("write")) {

					/*
					 * Check to make sure the node being written to's value isn't the same as is
					 * being written to it. Example: write a = b, when a already is b. This would
					 * trigger a rule violation because the value already exists. To prevent that,
					 * we don't create a rule violation when the new and old value are the same.
					 */

					ArrayList<String> nodeHierarchy = new ArrayList<String>();
					Node testNode;

					if (statement.getSelectedNodeName().equals(nodeName)) {

						nodeHierarchy = statement.getNodeHierarchy();
					} else if (statement.getChildrenNamesValues().keySet().contains(nodeName)) {

						nodeHierarchy = statement.getNodeHierarchy();
						nodeHierarchy.add(nodeName);

					}

					if (nodeHierarchy.size() > 0) {
						testNode = memory.getNodeByHierarchy(statement.getNodeHierarchyString());
						if (testNode != null) {
							if (testNode.getValue().equals(nodeValue)) {

								break;
							}
						}
					}

					ArrayList<Node> resultNodes = this.memory.getNodesByValue(nodeName, nodeValue);

					if (resultNodes.size() > 0) {

						statement.addError(
								"Enforced Rule Violation: All " + nodeName + " nodes must have unique values.");

						result = false;
						break;
					}
				}
			}

			break;

		default:
			/* do nothing */
		}

		return result;

	}

	/**
	 * This method resolves a node rule's value when it is supplied with the rule
	 * name, the node name that the rule is set for, and the user statement. The
	 * order of precedence is that first it checks the value that is to be set for
	 * the rule in the statement's childrenNamesValues, and in the case where the
	 * selectedNodeName is the rule name, it checks that too. If the rule name is
	 * not found, it will search the database for the value of this rule. If found,
	 * the rule value will be returned, otherwise an empty value will be returned.
	 * However, if the rule set exists (i.e. rules have been defined for the node
	 * name) there is no reason why any of the rules wouldn't exist with their
	 * default values.
	 * 
	 * @param ruleName    the name of the rule that is to be resolved
	 * @param ruleSetName the name of the node that the rule is defined for
	 * @param statement   the statement object, needed to check if a new value for
	 *                    the node rule is being set, which takes precedence over an
	 *                    existing value for that rule
	 * @return a String that is the value of the rule to be resolved
	 */
	private String resolveRuleValue(String ruleName, String ruleSetName, Statement statement) {

		String result = "";

		if (!Definitions.nodeRuleNames.contains(ruleName)) {
			/* Do not process any further if the provided rule name is invalid. */
			return result;

		}

		LinkedHashMap<String, String> rules = this.memory.getRulesByRuleSetName(ruleSetName);

		/*
		 * Check if selected node name is the rule name, e.g. rule.employeeFirstName.max
		 */
		if (ruleName.equals(statement.getSelectedNodeName())) {

			result = statement.getSelectedNodeValue();

		}
		/* Check if the children name values contains the rule name */
		else if (statement.getChildrenNamesValues().keySet().contains(ruleName)) {

			result = statement.getChildrenNamesValues().get(ruleName);
		}

		/* Check if the rule exists in the database */
		else if (rules.keySet().contains(ruleName)) {

			result = rules.get(ruleName);

		}

		return result;
	}

	/**
	 * Resolves the name of the rule set from the selected node name and parent
	 * names used in a statement. For instance in write rule.employee.max, employee
	 * is the rule set name. In write rule.employee: max = 10, employee is also the
	 * name of the rule set. Prerequisite: The fact that the statement is an enforce
	 * statement has already been established, i.e. one of the parent names is
	 * 'rule'.
	 * 
	 * @param selectedNodeName
	 * @param parentNames
	 * @return String that is the resolved rule set name
	 */
	private String resolveRuleSetName(String selectedNodeName, ArrayList<String> parentNames) {
		String lastParentName, ruleSetName = "";
		int lastParentNameIndex = parentNames.size() - 1;

		/* Retrieve the correct selected node name for the rule set */
		if (lastParentNameIndex >= 0) {

			lastParentName = parentNames.get(lastParentNameIndex);

			if (lastParentName.equals("rule")) {

				ruleSetName = selectedNodeName;

			}

			else if (Definitions.nodeRuleNames.contains(selectedNodeName)) {

				ruleSetName = lastParentName;
			}
		}

		return ruleSetName;
	}

}