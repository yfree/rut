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

A Node object is a field of data. */

package rut;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class Node {

	/* This is a lock that determines whether or not a node can be written to. */
	private boolean locked;

	/* This is the id of the shell that is locking up the node. */

	private String shellId;

	/*
	 * This is the value assigned to a node, it be may cast to any atomic data type,
	 * such as a numeric value.
	 */
	private String value;

	/* This is a map of the names and references to this node's children nodes */
	private LinkedHashMap<String, Node> children;

	public Node() {
		this.setChildren(new LinkedHashMap<String, Node>());
		this.setValue("");
	}

	public Node(LinkedHashMap<String, Node> children) {
		this.setChildren(children);
		this.setValue("");
	}

	public Node(String value) {
		this.setChildren(new LinkedHashMap<String, Node>());
		this.setValue(value);
	}

	public Node(String value, LinkedHashMap<String, Node> children) {
		this.setValue(value);
		this.setChildren(children);
	}

	public String toString() {
		return this.getValue();
	}

	public String get() {
		return this.getValue();
	}

	public void set(String value) {
		this.setValue(value);
	}

	public Node getChild(String key) {
		return this.getChildren().get(key);
	}

	public void setChild(String name, Node node) {
		this.children.put(name, node);
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getChildCount() {

		return this.children.size();
	}

	public LinkedHashMap<String, Node> getChildren() {
		return this.children;
	}

	public void setChildren(LinkedHashMap<String, Node> children) {
		this.children = children;
	}

	
	/*
	 * removes a node child by name
	 */
	public void removeNodeChild(String nodeName) {
	
	//TODO: Implement me!
	}
	
	/*
	 * Adds a node child with a name and a value
	 */
	public void addNodeChild(String nodeName, String nodeValue) {

		Node newNode = new Node();

		if (nodeValue != null) {
			newNode.setValue(nodeValue);
		}

		this.setChild(nodeName, newNode);
	}
	
	/*
	 * Adds a node child with a name and and NO value
	 */
	public void addNodeChild(String nodeName) {

		this.addNodeChild(nodeName, "");
	}
	
	public boolean getLocked() {
		return this.locked;
	}

	public void setLocked(boolean locked) {
		locked = this.locked;
	}

	public String getShellId() {
		return this.shellId;
	}

	public void setShellId(String shellId) {
		this.shellId = shellId;
	}

	/**
	 * This is a recursive method which traverses the node's descendants, while
	 * maintaining the output for each node it passes through. This method is used
	 * exclusively for reading the database to the user. All of the other methods
	 * that are used to search through the nodes do not keep track of the nodes they
	 * pass through, and do not belong to Node but rather to Memory Storage.
	 * 
	 * @param childrenNamesValues
	 * @param whereConditionRules
	 * @param result
	 * @param traverseDepth
	 * @return
	 */
	public ArrayList<String> traverse(LinkedHashMap<String, String> childrenNamesValues,
			LinkedHashMap<String, ArrayList<String>> whereConditionRules, ArrayList<String> result, int traverseDepth,
			boolean searchRules) {

		Node currentNode = this;
		Node nextNode;

		/*
		 * Display the node's value (if one is present) and go to a newline. We want the
		 * key for this node to be on the same line as the value in the display
		 */
		result.add(this.get().toString() + "\n");

		/* Stop traversing if the current node has no children */
		if (currentNode.getChildren() == null) {
			return result;
		}

		/* For Where Condition */
		if (!whereConditionRules.isEmpty()) {

			if (!this.whereConditionSatisfied(whereConditionRules, currentNode)) {

				/*
				 * Skipped records that don't satisfy the Where Condition are empty, but the
				 * empty stub must be removed, so we look for a specific pattern that identifies
				 * stub records and remove them from the result list.
				 */
				if (result.get(result.size() - 1).length() < 2) {

					this.removeLastElement(result);
					this.removeLastElement(result);

				}

				return result;
			}

		}

		for (String key : currentNode.getChildren().keySet()) {
//TODO: at this point the rule is removed WHY?
			/* Skip rules from being traversed if specified */
			if (!searchRules && key.equals("rule")) {

				continue;
			}

			/* For selected fields (children names) */
			/*
			 * If any children names from the provided list are found, the siblings that do
			 * not contain those names are not displayed
			 */

			if (!childrenNamesValues.isEmpty()) {
				if (this.setsIntersect(currentNode.getChildren().keySet(), childrenNamesValues.keySet())
						&& !childrenNamesValues.keySet().contains(key)) {

					continue;
				}

			}
			nextNode = currentNode.getChild(key);

			result.add(this.indent(traverseDepth) + key + "-> ");

			result = nextNode.traverse(childrenNamesValues, whereConditionRules, result, traverseDepth + 1,
					searchRules);

		}
		return result;
	}

	/* Generates a string composed of n number of tabs */
	public String indent(int tabCount) {

		/* We're going to use 4 spaces per tab as the default */
		String tabValue = "    ";

		StringBuilder spaceString = new StringBuilder();
		for (int i = 0; i < tabCount; i++) {
			spaceString.append(tabValue);
		}
		return spaceString.toString();
	}

	/*
	 * As input this method accepts the Where Condition Rules and the node to check
	 * from. If the fields to test for the Where Condition rules are not present,
	 * the where condition will NOT be satisfied. Returns true or false
	 */

	public boolean whereConditionSatisfied(LinkedHashMap<String, ArrayList<String>> whereConditionRules,
			Node currentNode) {

		/* Iterate through the Where Condition rules */
		for (String whereConditionKey : whereConditionRules.keySet()) {

			ArrayList<String> whereConditionValues = whereConditionRules.get(whereConditionKey);

			boolean testField = false;

			/* Test children */
			for (String key : currentNode.getChildren().keySet()) {

				testField = currentNode.checkNodeValue(whereConditionKey, whereConditionValues, key);
				if (testField) {
					return true;
				}

				Node nextNodeToPeek = currentNode.getChild(key);
				/* Test grand children */
				for (String nextKey : nextNodeToPeek.getChildren().keySet()) {
					testField = nextNodeToPeek.checkNodeValue(whereConditionKey, whereConditionValues, nextKey);
					if (testField) {
						return true;
					}
				}

			}
		}
		return false;

	}

	/**
	 * Removes the last element from an ArrayList<String>
	 * 
	 * @param arrayToCut
	 */
	private void removeLastElement(ArrayList<String> arrayToCut) {
		if (arrayToCut.size() > 0) {
			arrayToCut.remove(arrayToCut.size() - 1);
		}
	}

	/*
	 * Accepts two HashSet<String> containers. Returns true if any of the values
	 * intersect. Otherwise returns false
	 */

	private boolean setsIntersect(Set<String> keySet1, Set<String> keySet2) {

		Set<String> keySetCopy = new HashSet<String>();
		keySetCopy.addAll(keySet1);
		keySetCopy.retainAll(keySet2);

		/*
		 * An intersection occurs if any values remain in the keySet after the retainAll
		 * operation
		 */
		return !keySetCopy.isEmpty();
	}

	/*
	 * As input this method accepts the Where Condition Rules and the node name to
	 * check. The node's children are checked to see if the where condition is
	 * satisfied. If the fields to test for the Where Condition rules are not
	 * present, the where condition will NOT be satisfied. Returns true or false
	 * NOTE: The whereConditionRule linked hash map should contain only ONE column
	 * name. Subsequent rules will not be evaluated. This is a helper method
	 * intended to process results within a loop, one column/child name at a time.
	 */
	private boolean checkNodeValue(String whereConditionKey, ArrayList<String> whereConditionValues, String key) {
		boolean result = false;
		String nodeValue;
		Node nextNodeToPeek;

		if (whereConditionKey.equals(key)) {

			nextNodeToPeek = this.getChild(key);
			nodeValue = nextNodeToPeek.get();

			if (whereConditionValues.contains(nodeValue)) {
				result = true;
			}
			/*
			 * Also evaluate to true if there is no value to test, and we are just testing
			 * the presence of the child node name.
			 */

			else if (whereConditionValues.size() == 0) {
				result = true;
			}
		}

		return result;
	}

	public ArrayList<String> generateTree(String childName) {

		Node nodeToTraverse;
		
		/* If the plain String 'Root' is read, we have to run this method a tiny bit differently... */
		if (childName.equals("Root")) {
			
			nodeToTraverse = this;
		}
		else {
		
			nodeToTraverse = this.getChild(childName);
		}
		ArrayList<String> outputRows = new ArrayList<String>();
		
		return this.treeMapRecurse(nodeToTraverse, childName, outputRows);
	}

	private ArrayList<String> treeMapRecurse(Node currentNode, String fullPath, ArrayList<String> outputRows) {

		Node nextNode;
		String nodeName;

		if (fullPath.length() > 0) {

			outputRows.add(fullPath + ":" + currentNode.getValue());

		}

		if (currentNode != null) {

			for (String key : currentNode.getChildren().keySet()) {

				nextNode = currentNode.getChild(key);

				if (fullPath.length() == 0) {
					nodeName = key;
				} else {
					nodeName = fullPath + "." + key;
				}

				outputRows = nextNode.treeMapRecurse(nextNode, nodeName, outputRows);

			}
		}

		return outputRows;
	}
}