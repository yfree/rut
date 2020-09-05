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
 
 * boolean getRunningScriptSignal()
 
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

import rut.exceptions.InvalidConversionException;
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

	public String processStatement(Statement statement) {

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

	/*
	 * This is the main method of the interpreter. A statement is passed as input
	 * and the correct operation is performed. The result of the operation is
	 * returned as a String.
	 */
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
		 * currently on the the operations that checkForOpErrors tests for.
		 */
		String[] operationsToCheck = { "write", "enforce" };
		ArrayList<String> operationsToCheckFor = new ArrayList<String>(Arrays.asList(operationsToCheck));

		if (operationsToCheckFor.contains(statement.getOperation())) {
			/*
			 * If errors are discovered by checkForOpErrors(), the operation is changed to
			 * 'error'. Therefore it is not necessary to evaluate the checkForOpError()
			 * return value, although it becomes false after the first error is discovered.
			 */

			this.checkForOpErrors(statement);

		}

		String selectedNodeName = statement.getSelectedNodeName();
		String selectedNodeValue = statement.getSelectedNodeValue();
		LinkedHashMap<String, String> childrenNamesValues = statement.getChildrenNamesValues();
		ArrayList<String> parentNames = statement.getParentNames();
		LinkedHashMap<String, ArrayList<String>> whereConditionRules = statement.getWhereConditionRules();
		String operation = statement.getOperation();
		Set<String> statementErrors = statement.getErrorMessages();

		switch (operation) {

		case "read":
			response = this.executeOpRead(selectedNodeName, childrenNamesValues, parentNames, whereConditionRules);
			break;

		case "write":
			response = this.executeOpWrite(selectedNodeName, selectedNodeValue, childrenNamesValues, parentNames,
					whereConditionRules);
			break;

		case "enforce":
			response = this.executeOpEnforce(selectedNodeName, selectedNodeValue, childrenNamesValues, parentNames,
					whereConditionRules);

			break;

		case "delete":

			response = this.executeOpDelete(selectedNodeName, childrenNamesValues, parentNames, whereConditionRules);
			break;

		case "rename":
			response = this.executeOpRename(selectedNodeName, selectedNodeValue, childrenNamesValues, parentNames,
					whereConditionRules);
			break;

		case "exit":
			response = this.executeOpExit();
			break;

		case "begin":
			response = this.executeOpBegin();
			break;

		case "commit":
			response = this.executeOpCommit();
			break;

		case "rollback":
			response = this.executeOpRollback();
			break;

		case "error":
			response = this.executeOpError(statementErrors);
			break;

		case "comment":
			/* do nothing */
			break;

		default:
			/* do nothing */
		}

		return response;
	}

	public boolean getKillSignal() {

		return this.killSignal;
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

				Node testNode = this.memory.getNodeByHierarchy(statement.getNodeHierarchy(), true);
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

	/*
	 * Can read by node name, can read multiple nodes with the same name. Can read
	 * by node name and hierarchical parent names. Can read selected children names
	 * - will read those fields only Can read WHERE CONDITIONS: multiple conditions
	 * are treated as 'OR' will add support for 'AND' Todo: read joins
	 */
	/*
	 * TODO: Thorough fixing of WHERE. I want this to filter and be able to do basic
	 * things such as read employeeTitle where employeeTitle = ..., I know its
	 * working right now like for 25% of cases, it needs revamping and to be made
	 * production ready.
	 */
	private String executeOpRead(String selectedNodeName, LinkedHashMap<String, String> childrenNamesValues,
			ArrayList<String> parentNames, LinkedHashMap<String, ArrayList<String>> whereConditionRules) {

		ArrayList<String> parentNamesCopy = new ArrayList<String>(parentNames);
		ArrayList<String> actualValues = new ArrayList<String>();
		ArrayList<String> keyOnlyEntries = new ArrayList<String>();
		boolean noResults = false;
		ArrayList<String> resultMasterList;

		boolean searchRules = parentNamesCopy.contains("rule") || selectedNodeName.equals("rule") ? true : false;

		/* TODO: FIX ME - Do not display rules for 'read Root.Child' */
		if (parentNamesCopy.contains("Root") && selectedNodeName.equals("Child")) {
			searchRules = false;

		}

		/*
		 * The traversing of the node tree takes place during the construction of the
		 * result master lists. Each instance of a name being read is a separate list.
		 * For instance, 'read employeeFirstName' will create multiple result lists to
		 * be created, one result list for each 'employeeFirstName' and that node's
		 * children. The lists are added together to form the superset variable
		 * resultMasterLists. This is the final result that will be formatted (a bit
		 * more) and then presented to the user.
		 */
		ArrayList<ArrayList<String>> resultMasterLists = this.generateResultLists(parentNamesCopy, selectedNodeName,
				whereConditionRules, childrenNamesValues, searchRules);

		if (resultMasterLists.size() < 1) {

			/* There are obviously no results if nothing was returned... */
			noResults = true;
		} else {
			for (String result : resultMasterLists.get(0)) {
				result = result.trim();
				if (result.endsWith("->")) {
					keyOnlyEntries.add(result);
				} else {
					actualValues.add(result);
				}
			}

			if (actualValues.size() < 1) {

				/*
				 * If only keys are presented but no values, this is a case of 'no results' and
				 * we do not show the empty keys.
				 */
				noResults = true;
			}
		}

		if (noResults) {

			resultMasterList = new ArrayList<String>();
			resultMasterList.add("No results found.\n");
			if (resultMasterLists.size() == 0) {
				resultMasterLists.add(new ArrayList<String>());
			}

			resultMasterLists.set(0, resultMasterList);
		}

		/*
		 * Return the final result presentation. Basic, XML, JSON, or RutDisplay are
		 * valid data formats for presentation
		 */

		return this.createResultDisplay(resultMasterLists, "Basic");
	}

	/*
	 * Write to a node, create a new node, or create a new node and write to it.
	 */
	private String executeOpWrite(String selectedNodeName, String selectedNodeValue,
			LinkedHashMap<String, String> childrenNamesValues, ArrayList<String> parentNames,
			LinkedHashMap<String, ArrayList<String>> whereConditionRules) {

		boolean searchRules = parentNames.contains("rule");
		int nodesToWriteCount = 0;

		ArrayList<String> childNodeNames;

		Node nodeParentToWriteTo;

		ConcurrentHashMap<String, Node> parentsToWriteToData = this.getParentDataToWriteTo(parentNames,
				selectedNodeName, searchRules);
		/*
		 * debugCounter++; if (debugCounter % 1000 == 0) {
		 * System.out.println("1000 getParentNodesToWriteTo Completed"); }
		 */

		/*
		 * Process new nodes by appending a child to the parents and writing the value
		 * to them
		 */
		for (String fullPath : parentsToWriteToData.keySet()) {

			nodeParentToWriteTo = parentsToWriteToData.get(fullPath);

			childNodeNames = new ArrayList<String>();

			/* Where Conditions evaluated */
			if (!whereConditionRules.isEmpty()) {
				if (!nodeParentToWriteTo.whereConditionSatisfied(whereConditionRules, nodeParentToWriteTo)) {
					continue;
				}
			}

			if (selectedNodeName.equals("Child")) {
				ArrayList<String> parentNamesCopy = new ArrayList<String>(parentNames);
				parentNamesCopy.add("Child");
				childNodeNames = memory.resolveChildKeyWord(parentNamesCopy, searchRules);
			} else {
				childNodeNames.add(selectedNodeName);
			}

			for (String childNodeName : childNodeNames) {
				Node writtenNode;

				/* Check if the node exists */
				writtenNode = nodeParentToWriteTo.getChild(childNodeName);
				writtenNode = (writtenNode == null) ? new Node(): writtenNode;
				String nodeValue = selectedNodeValue.isEmpty() ? "" : selectedNodeValue;

				writtenNode.setValue(nodeValue);
				this.memory.addDataMap(writtenNode, fullPath + "." + childNodeName);

				nodesToWriteCount++;
				nodesToWriteCount += this.generateChildrenNodes(writtenNode, childrenNamesValues);
			}
		}

		/* Signal to save to disk because changes have been made. */
		if ((nodesToWriteCount) > 0) {

			this.writeToDiskSignal = true;

		}

		return this.generateResultMessage("written to", nodesToWriteCount);
	}

	/* Deletes one or more nodes from the database. */
	private String executeOpDelete(String selectedNodeName, LinkedHashMap<String, String> childrenNamesValues,
			ArrayList<String> parentNames, LinkedHashMap<String, ArrayList<String>> whereConditionRules) {

		boolean searchRules = parentNames.contains("rule");

		int deletedNodesCount = 0;

		ArrayList<Node> fetchedNodes = new ArrayList<Node>();
		if (parentNames.isEmpty()) {

			fetchedNodes = this.memory.getNodesByChildName(selectedNodeName, searchRules);

		} else {

			fetchedNodes = this.memory.getNodesByHierarchy(parentNames, searchRules);
		}

		ArrayList<String> childNodeNames = new ArrayList<String>();
		for (Node fetchedNode : fetchedNodes) {

			if (selectedNodeName.equals("Child")) {
				ArrayList<String> parentNamesCopy = new ArrayList<String>(parentNames);
				parentNamesCopy.add("Child");
				childNodeNames = memory.resolveChildKeyWord(parentNamesCopy, searchRules);
			} else {
				childNodeNames.add(selectedNodeName);
			}

			for (String childNodeName : childNodeNames) {

				deletedNodesCount += this.memory.deleteNode(fetchedNode, childNodeName);

			}

		}

		/* Signal to save to disk because changes have been made. */
		if (deletedNodesCount > 0) {

			this.writeToDiskSignal = true;

		}

		return this.generateResultMessage("deleted", deletedNodesCount);
	}

	/* Renames one or more nodes in the database */
	private String executeOpRename(String selectedNodeName, String selectedNodeValue,
			LinkedHashMap<String, String> childrenNamesValues, ArrayList<String> parentNames,
			LinkedHashMap<String, ArrayList<String>> whereConditionRules) {

		boolean searchRules = parentNames.contains("rule");

		int renamedNodesCount = 0;

		ArrayList<Node> fetchedNodes = new ArrayList<Node>();

		if (parentNames.isEmpty()) {

			fetchedNodes = this.memory.getNodesByChildName(selectedNodeName, searchRules);

		} else {

			fetchedNodes = this.memory.getNodesByHierarchy(parentNames, searchRules);

		}

		for (Node fetchedNode : fetchedNodes) {

			/*
			 * Check to see if there is a node under the parent with the name that will be
			 * used as the new name. You cannot have two nodes with the same name as
			 * children of the same parent...
			 */
			Node testNode = this.memory.getNodeByName(selectedNodeValue, searchRules);

			if (testNode == null) {

				renamedNodesCount += this.memory.renameNode(fetchedNode, selectedNodeName, selectedNodeValue);

			}
		}

		/* Signal to save to disk because changes have been made. */
		if (renamedNodesCount > 0) {

			this.writeToDiskSignal = true;

		}

		return this.generateResultMessage("renamed", renamedNodesCount);
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

		this.executeOpWrite(selectedNodeName, selectedNodeValue, childrenNamesValues, parentNames, whereConditionRules);

		/* This skips rules if they already exist */
		this.memory.createDefaultRuleSet(ruleSetName);

		return "Rules set for " + ruleSetName + ".";
	}

	private String executeOpExit() {
		this.killSignal = true;
		return "Goodbye.";
	}

	private String executeOpBegin() {
		return "This operation has not yet been implemented.";
	}

	private String executeOpCommit() {
		return "This operation has not yet been implemented.";
	}

	private String executeOpRollback() {
		return "This operation has not yet been implemented.";
	}

	/*
	 * This is a special operation for when an input error is detected.
	 */

	private String executeOpError(Set<String> parseErrors) {

		String errorMessagesString = "";
		errorMessagesString = String.join("\n", parseErrors);

		return errorMessagesString;
	}

	/**
	 * This is a helper method for executeOpRead's generateResultLists. This method
	 * constructs an individual list of node names / values based on the name
	 * provided as input. This method creates a single list, so for instance if the
	 * name was 'employeeFirstName' and there were ten employee first name's to be
	 * displayed, ten lists will need to be created. The responsibility for creating
	 * multiple lists and appending them together is the for the method
	 * generateResultLists.
	 * 
	 * @param parentNames
	 * @param selectedNodeName
	 * @param childSubstituteNames
	 * @param selectedNodeNameSwitch
	 * @param resultNodes
	 * @param resultNode
	 * @param whereConditionRules
	 * @param childrenNamesValues
	 * @param searchRules
	 * @return
	 */
	private ArrayList<String> generateResultList(ArrayList<String> parentNames, String selectedNodeName,
			ArrayList<String> childSubstituteNames, boolean selectedNodeNameSwitch, ArrayList<Node> resultNodes,
			Node resultNode, LinkedHashMap<String, ArrayList<String>> whereConditionRules,
			LinkedHashMap<String, String> childrenNamesValues, boolean searchRules, int childCounter) {
		ArrayList<String> resultList = new ArrayList<String>();

		ArrayList<String> resultStrings;
		int depth = 0;
		int lastElementIndex = parentNames.size();
		int currentIndex = 1;

		for (String parentName : parentNames) {

			if (parentName.equals("Child")) {

				parentName = childSubstituteNames.get(childCounter);

			}

			if (currentIndex == lastElementIndex) {

				break;
			}

			if (resultNodes.size() > 0) {

				resultList.add(resultNodes.get(0).indent(depth) + parentName + "-> \n");
			}
			depth++;

			// Skip the last element, that will be processes separately.
			currentIndex++;

		}

		resultStrings = new ArrayList<String>();

		if (selectedNodeName.equals("Child")) {

			/*
			 * Once this switch is set, it will not be unset. selectedNodeName will traverse
			 * the list of child substitute names and use each one of them instead of
			 * 'child'.
			 */
			selectedNodeNameSwitch = true;

		}
		if (selectedNodeNameSwitch) {

			selectedNodeName = childSubstituteNames.get(childCounter);

		}

		resultList.add(resultNodes.get(0).indent(depth) + selectedNodeName + "-> ");

		resultStrings = resultNode.traverse(childrenNamesValues, whereConditionRules, resultStrings, 1, searchRules);

		/* Special formatting to conform for items with parent nodes specified. */
		if (resultStrings.size() > 0) {

			for (String resultItem : resultStrings) {

				resultList.add(resultItem.replaceAll("\n", "\n" + resultNodes.get(0).indent(depth)));

			}

		}
		String lastResultForEntry = "";
		int lastResultForEntryIndex = 0;
		if (resultList.size() > 0) {

			lastResultForEntryIndex = resultList.size() - 1;
			lastResultForEntry = resultList.get(lastResultForEntryIndex);

			/*
			 * Formatting fix for multiple results with parent names and children...
			 * basically on the last line, there is indentation after the line break that
			 * must be removed...
			 */
			if (lastResultForEntry.contains("\n")) {
				lastResultForEntry = lastResultForEntry.split("\\\\r?\\n")[0];

				resultList.set(lastResultForEntryIndex, lastResultForEntry + "\n");

			}
		}

		return resultList;

	}

	/*
	 * This is a helper method for executeOpRead that produces unformatted lists of
	 * node names/values (i.e. results) read from the database. These lists will
	 * later be combined for the final output.
	 */
	private ArrayList<ArrayList<String>> generateResultLists(ArrayList<String> parentNames, String selectedNodeName,
			LinkedHashMap<String, ArrayList<String>> whereConditionRules,
			LinkedHashMap<String, String> childrenNamesValues, boolean searchRules) {
		ArrayList<ArrayList<String>> resultMasterLists = new ArrayList<ArrayList<String>>();

		/* When child keyword is used, this list contains the real names for display. */
		ArrayList<String> childSubstituteNames = new ArrayList<String>();
		ArrayList<Node> resultNodes = new ArrayList<Node>();

		/* If no parent nodes are specified, the Result Nodes List will be simple */
		if (parentNames.isEmpty()) {
			/* WORKS */
			resultNodes = this.memory.getNodesByName(selectedNodeName, searchRules);

		} else {

			/*
			 * If parent nodes ARE specified, some modifications need to be done before our
			 * Result Nodes List is ready for processing.
			 */

			String fullPath = this.memory.parseFullPath(parentNames, selectedNodeName);
			resultNodes = this.memory.justNodes(this.memory.getDataByPath(fullPath, searchRules));

			System.out.println(resultNodes);

			// resultNodes = memory.getNodesByNamePathContains(selectedNodeName, parents,
			// searchRules);

			/* Put the selected node name back in the list for sequential processing */
			parentNames.add(selectedNodeName);

		}

		if (resultNodes.size() > 0 && parentNames.contains("Child")) {

			childSubstituteNames = memory.resolveChildKeyWord(parentNames, searchRules);

		}

		/*
		 * The Result Nodes List is ready for processing.
		 * 
		 * This list will be traversed and the results will be compiled for display.
		 */

		/*
		 * This is used to tell the selectedNodeName to use the real name when a name
		 * has been set to the 'Child' keyword.
		 */
		boolean selectedNodeNameSwitch = false;

		int childCounter = 0;

		for (Node resultNode : resultNodes) {

			ArrayList<String> resultList = this.generateResultList(parentNames, selectedNodeName, childSubstituteNames,
					selectedNodeNameSwitch, resultNodes, resultNode, whereConditionRules, childrenNamesValues,
					searchRules, childCounter++);

			resultMasterLists.add(resultList);
		}

		return resultMasterLists;
	}

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
	 * Gets node data for a parent nodes to be written to
	 * 
	 * @param parentNames      - the parent names of the selected node name
	 * @param selectedNodeName - the selected node name, whose parents are being
	 *                         searched for
	 * @param searchRules      - specifies whether or not to search through rules
	 * @return - ConcurrentHashMap<String of the full path, Node>
	 */
	private ConcurrentHashMap<String, Node> getParentDataToWriteTo(ArrayList<String> parentNames,
			String selectedNodeName, boolean searchRules) {

		String searchString = selectedNodeName;
		String parentString = String.join(".", parentNames);

		if (parentNames.size() > 0) {
			searchString = parentString + "." + searchString;
		}

		System.out.println("Search String " + searchString);
		ConcurrentHashMap<String, Node> parentsToWriteToData = memory.getDataByPath(searchString, searchRules);

		return parentsToWriteToData;
	}

	/*
	 * Generates the result message for all the non-read operations. The parameters
	 * are the verb to be used in the message and the number of nodes affected by
	 * the operation.
	 */
	private String generateResultMessage(String verb, int nodeCount) {
		String resultMessage = "";
		if (nodeCount == 0) {
			resultMessage = "No nodes " + verb + ".";
		} else if (nodeCount == 1) {
			resultMessage = "1 node " + verb + ".";
		} else if (nodeCount > 1) {
			resultMessage = nodeCount + " nodes " + verb + ".";
		}
		return resultMessage;
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
						testNode = memory.getNodeByHierarchy(nodeHierarchy);
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

	private String createResultDisplay(ArrayList<ArrayList<String>> resultMasterLists, String dataFormat) {

		StringBuilder resultString = new StringBuilder();

		if (!Definitions.dataFormats.contains(dataFormat)) {

			/* Nothing to return */
			return resultString.toString();

		}

		for (ArrayList<String> resultList : resultMasterLists) {

			resultString.append(String.join("", resultList));

		}

		/*
		 * The size of the separator is the size of the longest row of result text
		 * (limited by 64 characters) ...
		 */

		int separatorSize = this.getSeparatorSize(resultString.toString());
		String separator = this.createSeparator(separatorSize);

		return separator + "\n" + resultString.toString() + separator;

	}

	private int getSeparatorSize(String resultMasterListString) {

		int resultSize = 0;
		int largestSize = 0;

		for (String result : resultMasterListString.split("\\n")) {

			resultSize = result.length();

			if (resultSize > largestSize) {

				largestSize = resultSize;

			}
		}

		return largestSize;
	}

	private String createSeparator(int separatorSize) {

		StringBuilder separator = new StringBuilder();

		if (separatorSize > 64) {

			separatorSize = 64;
		}

		for (int i = 0; i < separatorSize; i++) {
			separator.append("=");
		}

		return separator.toString();
	}
}