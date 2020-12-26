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

Memory Storage controls the database in main memory.
It's constructor requires a root node to point to.
Once MemoryStorage is attached to the node tree,
it offers a variety of methods to access and manipulate the nodes in memory.
There are a number of very useful operations that can be performed.
Here is a list of the essential Memory Storage public methods:

* ArrayList<Node> getNodesByName(String)

* Node getNodeByName(String)

* ArrayList<Node> getNodesByName(String, boolean)

* Node getNodeByName(String, boolean)

* ArrayList<Node> getNodesByHierarchy(ArrayList<String>) or
  ArrayList<Node> getNodesByHierarchy(ArrayList<String>, ArrayList<String>)

* Node getNodeByHierarchy(ArrayList<String>)

* void addNode(Node, String, String) or void addNode(Node, String)

* ArrayList<Node> getNodesByChildName(String)

* deleteNode(Node, String)

*/
package rut;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStorage {

	private Node rootNode;

	private ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> dataMap;

	private boolean killSignal;
	
	private boolean writeToDiskSignal;

	/* Keeps track of a universal auto incrementing long. */
	private long uid;

	public MemoryStorage(ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> theDataMap) {		

		this.dataMap = theDataMap;
		this.rootNode = this.dataMap.get("Root").get("Root");
		
		this.killSignal = false;
		this.writeToDiskSignal = false;
		
		this.uid = 0;
	}

	public Node getRootNode() {
		return this.rootNode;
	}

	public void setRootNode(Node rootNode) {
		this.rootNode = rootNode;
	}

	public ConcurrentHashMap<String, Node> getFlatDataMap() {

		ConcurrentHashMap<String, Node> flatDataMap = new ConcurrentHashMap<String, Node>();
		Node currentNode = new Node();

		for (String nodeName : this.dataMap.keySet()) {

			ConcurrentHashMap<String, Node> nodeRecords = this.dataMap.get(nodeName);
			for (String fullPath : nodeRecords.keySet()) {
				currentNode = nodeRecords.get(fullPath);
				flatDataMap.put(fullPath, currentNode);
			}

		}

		return flatDataMap;
	}
	
	/* Adds a node to the dataMap */
	public boolean addDataMap(Node node, String fullPath) {

		/* Normalize Data Map entry by removing the Root keyword */
		fullPath = fullPath.replace("Root.", "");
		
		String nodeName = this.parseNodeName(fullPath);
		String parentName = this.parseParentName(fullPath);
		
		ConcurrentHashMap<String, Node> nodesByName = this.dataMap.get(nodeName);

		/* If record for nodes with that name doesn't exist, create the HashMap */
		if (nodesByName == null) {

			nodesByName = new ConcurrentHashMap<String, Node>();

		}

		/* Link to Parent */

		try {

			Node parentNode = this.justNodes(this.getDataByPath(parentName, true)).get(0);

			parentNode.setChild(nodeName, node);

			// System.out.println("Linking " + nodeName + " to its parent " + parentName +
			// ".");

		} catch (Exception e) {

			System.out.println(e);
			System.out.println("Parent Node for an existing node could not be located, exiting..");
			System.exit(1);

		}

		nodesByName.put(fullPath, node);

		this.dataMap.put(nodeName, nodesByName);
		return true;
	}

	
	/* Deletes a node from the dataMap */
	public boolean deleteDataMap(String fullPath) {
		
		/* Normalize Data Map entry by removing the Root keyword */
		fullPath = fullPath.replace("Root.", "");
		
		String nodeName = this.parseNodeName(fullPath);
		
		ConcurrentHashMap<String, Node> nodesByName = this.dataMap.get(nodeName);

		if (nodesByName == null) {
			
			return false;
		}
		try {

			nodesByName.remove(fullPath);

		} catch (Exception e) {
			
		}
		
		if (nodesByName.isEmpty()) {
		
			this.dataMap.remove(nodeName);
		
		}
		
		return true;
	
	}
	
	/**
	 * Accepts a string of the full path for a node and parses the node's name (the
	 * last item in the full path). For instance, if a.b.c.d is provided as input, d
	 * will be returned. If a is provided as input, a will be returned.
	 * 
	 * @param fullPath - the full path of the node, separated by periods
	 * @return the parsed name of the node
	 */
	public String parseNodeName(String fullPath) {

		String nodeName = "";
		int lastDot = 0;

		if (fullPath.contains(".")) {
			lastDot = fullPath.lastIndexOf('.');
			nodeName = fullPath.substring(lastDot + 1);
		} else {
			nodeName = fullPath;
		}

		return nodeName;

	}

	/**
	 * Accepts a string of the full path for a node and parses the node's name (the
	 * last item in the full path). For instance, if a.b.c.d is provided as input,
	 * a.b.c will be returned. If a is provided as input, Root will be returned.
	 * 
	 * @param fullPath - the full path of the node, separated by periods
	 * @return the parsed name of the node
	 */
	public String parseParentName(String fullPath) {

		/* Default entry is 'Root'. This will be normalized to the proper path 
		 * without that word through the pipe line, but for now it's easiest this way.
		 */
		
		String parentName = "Root";

		int lastDot = 0;

		if (fullPath.contains(".")) {
			lastDot = fullPath.lastIndexOf('.');
			parentName = fullPath.substring(0, lastDot);
		}

		return parentName;

	}

	public String parseFullPath(ArrayList<String> parentNames, String selectedNodeName) {

		String fullPath = "";

		if (parentNames.size() > 0) {

			fullPath = String.join(".", parentNames) + "." + selectedNodeName;

		} else {

			fullPath = selectedNodeName;

		}

		return fullPath;
	}

	/**
	 * Accepts the data results as input (which is a concurrent hashmap with the key
	 * containing the full path of a node and the value is the node) and returns an
	 * arraylist of the nodes.
	 * 
	 * @param dataResults Full data containing the full paths as the keys and the
	 *                    nodes
	 * @return an array list of the nodes contained in the dataResults
	 */
	public ArrayList<Node> justNodes(ConcurrentHashMap<String, Node> dataResults) {

		Collection<Node> values = dataResults.values();
		return new ArrayList<Node>(values);

	}

	public String dumpDataMap() {

		ConcurrentHashMap<String, Node> flatDataMap = this.getFlatDataMap();
		ArrayList<String> lines = new ArrayList<String>();
		Node currentNode;

		for (String fullPath : flatDataMap.keySet()) {
			/* Root is not included in the dump */
			if (fullPath.equals("Root")) {
				continue;
			}
			
			currentNode = flatDataMap.get(fullPath);
			lines.add(fullPath + ":" + currentNode.getValue());
			
		}

		Collections.sort(lines);
		Collections.reverse(lines);
		return String.join("\n", lines);
		
	}

	public String printDataMap() {

		StringBuilder result = new StringBuilder();
		String line = "";
		Node currentNode;

		ArrayList<String> sortedNames = new ArrayList<String>();
		sortedNames.addAll(this.dataMap.keySet());
		Collections.sort(sortedNames);

		for (String nodeName : sortedNames) {

			if (nodeName.equals("Root")) {
				continue;
			}

			ConcurrentHashMap<String, Node> nodeRecords = this.dataMap.get(nodeName);

			ArrayList<String> sortedRecords = new ArrayList<String>();
			sortedRecords.addAll(nodeRecords.keySet());
			Collections.sort(sortedRecords);

			for (String fullPath : sortedRecords) {
				currentNode = nodeRecords.get(fullPath);
				line = fullPath + ":" + currentNode.getValue();
				result.append(line + "\n");
			}
		}

		return result.toString();
	}
	/*
	 * 
	 * ArrayList<Node> getNodesByName(String nodeName){
	 * 
	 * return resultNodes;
	 * 
	 * }
	 * 
	 * ArrayList<Node> getNodesByFullPath(String nodeName){
	 * 
	 * return resultNodes;
	 * 
	 * }
	 * 
	 */

	public long getUid() {
		return this.uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}
	
	public boolean getKillSignal() {
		return this.killSignal;
	}

	public void setKillSignal(boolean killSignal) {
		this.killSignal = killSignal;
	}

	public boolean getWriteToDiskSignal() {
		return this.writeToDiskSignal;
	}

	public void setWriteToDiskSignal(boolean writeToDiskSignal) {
		this.writeToDiskSignal = writeToDiskSignal;
	}
	
	public ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> getDataMap() {
		return this.dataMap;
	}

	public void setDataMap(ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> dataMap) {
		this.dataMap = dataMap;
	}

	/**
	 * Accepts the full path of nodes and returns data of those nodes where the path
	 * contains the string defined in path. If searchRules set to true, data
	 * containing rules nodes will be returned in the results
	 * 
	 * @param path        string to search for
	 * @param searchRules boolean whether or not to include rules in the results
	 * @return a ConcurrentHashMap where the keys are the full paths and the values
	 *         are the nodes themselves
	 */
	public ConcurrentHashMap<String, Node> getDataByPath(String path, boolean searchRules) {
		path = path.replace("Root.", "");
		
		ConcurrentHashMap<String, Node> dataResults = new ConcurrentHashMap<String, Node>();
		String nodeName = this.parseNodeName(path);
		ConcurrentHashMap<String, Node> nodeRecords = this.dataMap.get(nodeName);
		
		if (nodeRecords == null) {
			return dataResults;
			
		}
		
		for (String fullPath : nodeRecords.keySet()) {
	
			if (!fullPath.contains("." + path) && !(fullPath.startsWith(path))) {
				continue;
			} else {

				if (fullPath.matches("\\.rules\\.") || fullPath.endsWith("\\.rules")) {
					if (!searchRules) {
						continue;
					}
				}

				dataResults.put(fullPath, nodeRecords.get(fullPath));
			}

		}

		return dataResults;
	}

	/**
	 * Retrieves the node rules from memory and constructs a container out of them.
	 * 
	 * @param selectedNodeName the rule name set to build
	 * @return LinkedHashMap<rule name, rule value>
	 */
	public LinkedHashMap<String, String> getRulesByRuleSetName(String selectedNodeName) {
		LinkedHashMap<String, String> theRules = new LinkedHashMap<String, String>();

		Node ruleNode = this.getNodeByHierarchy("rule." + selectedNodeName, true);

		if (ruleNode != null) {
			Set<String> ruleNames = ruleNode.getChildren().keySet();

			for (String ruleName : ruleNames) {

				theRules.put(ruleName, ruleNode.getChild(ruleName).getValue());

			}

		}

		return theRules;
	}

	/**
	 * Retrieves an arrayList of nodes that are parents of a node with the passed
	 * nodeName
	 * 
	 * @param nodeName
	 * @return an arrayList of the result nodes
	 */
	public ConcurrentHashMap<String, Node> getParentNodesDataByChildName(String nodeName) {

		return this.getParentNodesDataByChildName(nodeName, false);
	}

	/**
	 * Retrieves an arrayList of nodes that are parents of a node with the passed
	 * nodeName. Also determine whether or not you want to search the rule nodes
	 * 
	 * @param nodeName
	 * @param searchRules search the rule nodes, true or false
	 * @return
	 */
	public ConcurrentHashMap<String, Node> getParentNodesDataByChildName(String nodeName, boolean searchRules) {
		ConcurrentHashMap<String, Node> resultNodesData = new ConcurrentHashMap<String, Node>();
		
		ConcurrentHashMap<String, Node> childNodesData = getDataByPath(nodeName, searchRules);
		String parentName = "";
		ConcurrentHashMap<String, Node> parentNodesData;
		
		for (String fullPath : childNodesData.keySet()) {
			
			parentName = this.parseParentName(fullPath);	
			parentNodesData = this.getDataByPath(parentName, searchRules);	

			resultNodesData.putAll(parentNodesData);
			
		}
		
		return resultNodesData;
	}

	public ArrayList<Node> getNodesByName(String nodeName) {

		ArrayList<Node> resultNodes = new ArrayList<Node>();

		if (nodeName.equals("Root")) {
			resultNodes.add(this.rootNode);
		} else {
			resultNodes = getNodesByName(nodeName, false);
		}
		return resultNodes;
	}

	/*
	 * Gets the nodes by name but filters out the nodes with a full path not
	 * containing 'path'.
	 */
	public ArrayList<Node> getNodesByNamePathContains(String nodeName, String path, boolean searchRules) {
		Node currentNode;
		ArrayList<Node> resultNodes = new ArrayList<Node>();
		/*
		 * if (nodeName.equals("Root")) { resultNodes.add(this.rootNode);
		 * 
		 * return resultNodes; }
		 */

		for (String fullPath : this.dataMap.get(nodeName).keySet()) {

			if (fullPath.matches("\\.rules\\.") || fullPath.endsWith("\\.rules")) {
				if (searchRules == false) {
					continue;
				}
			}

			if (!path.equals("")) {
				if (fullPath.indexOf(path) == -1) {

					continue;

				}
			}

			currentNode = this.dataMap.get(nodeName).get(fullPath);
			resultNodes.add(currentNode);

		}

		return resultNodes;
	}

	public ArrayList<Node> getNodesByName(String nodeName, boolean searchRules) {

		Node currentNode;
		ArrayList<Node> resultNodes = new ArrayList<Node>();
				

		if (!this.dataMap.containsKey(nodeName)) {

			return resultNodes;

		}

		for (String fullPath : this.dataMap.get(nodeName).keySet()) {

			if (fullPath.matches("\\.rules\\.") || fullPath.endsWith("\\.rules")) {
				if (searchRules == false) {
					continue;
				}
			}

			currentNode = this.dataMap.get(nodeName).get(fullPath);
			resultNodes.add(currentNode);

		}

		return resultNodes;
	}

	/**
	 * Get the nodes by name but also determine whether or not you want to search
	 * the rule nodes.
	 * 
	 * @param nodeName    the node name to search for
	 * @param searchRules search the rule nodes, true or false
	 * @return the result set - an arrayList of Nodes
	 */
	/*
	 * public ArrayList<Node> getNodesByName(String nodeName, boolean searchRules) {
	 * Node currentNode = this.rootNode; ArrayList<Node> resultNodes = new
	 * ArrayList<Node>();
	 * 
	 * if (nodeName.equals("Root")) { resultNodes.add(this.rootNode); } else {
	 * resultNodes = getNodesByName(nodeName, currentNode, resultNodes,
	 * searchRules); } return resultNodes; }
	 */
	/**
	 * Get nodes by their name but limit the results to start with a specific node
	 * as the tree root
	 * 
	 * @param nodeName
	 * @param currentNode
	 * @return
	 */
	/*
	 * public ArrayList<Node> getNodesByName(String nodeName, Node currentNode) {
	 * ArrayList<Node> resultNodes = new ArrayList<Node>();
	 * 
	 * return getNodesByName(nodeName, currentNode, resultNodes, false); }
	 */
	/**
	 * Same thing as getNodesByName except it returns the type 'Node' and will only
	 * return the first result if multiple nodes are found with that name. This
	 * version of the method will limit the results to start with a specific node as
	 * the tree root. Does not search rules.
	 * 
	 * @param nodeName
	 * @param currentNode
	 * @return
	 */
	/*
	 * public Node getNodeByName(String nodeName, Node currentNode) { return
	 * getNodeByName(nodeName, currentNode, false); }
	 */
	/**
	 * Same thing as getNodesByName except it returns the type 'Node' and will only
	 * return the first result if multiple nodes are found with that name. This
	 * version of the method will limit the results to start with a specific node as
	 * the tree root. Can specify whether or not to search for rules by passing that
	 * option as a boolean parameter.
	 * 
	 * @param nodeName
	 * @param currentNode
	 * @param searchRules
	 * @return
	 */

	/*
	 * public Node getNodeByName(String nodeName, Node currentNode, boolean
	 * searchRules) { ArrayList<Node> resultNodes = new ArrayList<Node>();
	 * resultNodes = this.getNodesByName(nodeName, searchRules);
	 * 
	 * if (resultNodes.size() > 0) { return resultNodes.get(0); } else { return
	 * null; } }
	 */
	/**
	 * Same thing as getNodesByName except it returns the type 'Node' and will only
	 * return the first result if multiple nodes are found with that name
	 * 
	 * @param nodeName
	 * @return
	 */
	public Node getNodeByName(String nodeName) {
		ArrayList<Node> resultNodes = new ArrayList<Node>();
		resultNodes = this.getNodesByName(nodeName, false);

		if (resultNodes.size() > 0) {
			return resultNodes.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Get the nodes by name but also determine whether or not you want to search
	 * the rule nodes.
	 * 
	 * @param nodeName    the node name to search for
	 * @param searchRules search the rule nodes, true or false
	 * @return a single node, or if multiple results found, the first result
	 */
	public Node getNodeByName(String nodeName, boolean searchRules) {
		ArrayList<Node> resultNodes = new ArrayList<Node>();
		resultNodes = this.getNodesByName(nodeName, searchRules);

		if (resultNodes.size() > 0) {
			return resultNodes.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Accepts a list of nodes in sequential order. Traverses through then and
	 * returns the first node in the list. If any of the nodes within the parent -
	 * child structure do not exist, the method returns null.
	 * 
	 * @param nodeNames
	 * @return
	 */
	public Node getNodeByHierarchy(String nodePath) {

		return getNodeByHierarchy(nodePath, false);
	}

	/**
	 * Gets the first node result of getParentNodesByHierarchy but allows you to specify
	 * whether or not to include rule nodes in the search results.
	 * 
	 * @param String fullPath - the full path of the node
	 * @param searchRules search the rule nodes, true or false
	 * @return
	 */
	public Node getNodeByHierarchy(String nodePath, boolean searchRules) {

		ArrayList<Node> temporaryNodes = this.justNodes(this.getParentNodesDataByHierarchy(nodePath, searchRules));
		if (temporaryNodes.size() > 0) {
			return temporaryNodes.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Same thing as getNodesByValue except it returns the type 'Node' and will only
	 * return the first result if multiple nodes are found with that value
	 * 
	 * @param nodeName
	 * @return
	 */
	public Node getNodeByValue(String nodeValue) {
		ArrayList<Node> resultNodes = new ArrayList<Node>();
		resultNodes = this.getNodesByValue(nodeValue);

		if (resultNodes.size() > 0) {
			return resultNodes.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Gets ANY node that has a specified value, regardless of the node's name. Does
	 * not search node rules
	 * 
	 * @param nodeValue the value to search for
	 * @return
	 */

	public ArrayList<Node> getNodesByValue(String nodeValue) {

		Node currentNode = this.rootNode;
		ArrayList<Node> resultNodes = new ArrayList<Node>();

		return this.getNodesByValue(nodeValue, currentNode, resultNodes);

	}

	/**
	 * Returns an ArrayList of Nodes for all the Nodes that contain a certain value.
	 * Doesn't include rules in the search.
	 *
	 * @param the       name of the node
	 * @param nodeValue The value the nodes must have
	 * @return
	 */
	public ArrayList<Node> getNodesByValue(String nodeName, String nodeValue) {

		return this.getNodesByValue(nodeName, nodeValue, false);
	}

	/**
	 * Returns an ArrayList of Nodes for all the Nodes that contain a certain value.
	 * 
	 * @param nodeName    the name of the node
	 * @param nodeValue   The value the nodes must have
	 * @param searchRules to include rules in the search
	 * @return
	 */
	public ArrayList<Node> getNodesByValue(String nodeName, String nodeValue, boolean searchRules) {

		ArrayList<Node> finalNodes = new ArrayList<Node>();
		ArrayList<Node> resultNodes = this.getNodesByName(nodeName, searchRules);

		for (Node resultNode : resultNodes) {

			if (resultNode.getValue().equals(nodeValue)) {

				finalNodes.add(resultNode);

			}
		}

		return finalNodes;
	}

	/**
	 * Checks if a node with a particular name and particular value appear more than
	 * once
	 * 
	 * @param nodeName  the name of the node
	 * @param nodeValue the value of the node to check
	 * @return true if only one result is present, false if more are present
	 */
	public boolean isUnique(String nodeName, String nodeValue) {

		ArrayList<Node> resultNodes = this.getNodesByValue(nodeName, nodeValue);

		return (resultNodes.size() > 1) ? false : true;
	}

	/**
	 * This method retrieves a list of nodes that match a parent - child list. This
	 * method works with both the root and child key words used as parent names.
	 * Does not search through rules.
	 * 
	 * @param nodeNames
	 * @return
	 */

	public ConcurrentHashMap<String, Node> getParentNodesDataByHierarchy(String nodePath) {

		return getParentNodesDataByHierarchy(nodePath, false);
	}

	/**
	 * 
	 * @param nodePath   the hierarchy of nodes to search
	 * @param searchRules true or false, include rules in search results
	 * @return the PARENT of the second to last node in the hierarchy if it contains the child (the last node)
	 * for instance, if I search for x.y.z, if 'z' is a child of 'y', it will return y.
	 * This may sound strange, why aren't we just getting node z? 
	 * Remember, when node operations are performed, it is performed on the PARENT
	 * node, because the parent node contains a HashMap linking its children. 
	 * We ALWAYS need a parent node and a child name to do a node lookup
	 */
	public ConcurrentHashMap<String, Node> getParentNodesDataByHierarchy(String nodePath, boolean searchRules) {
		
		String parentPath = this.parseParentName(nodePath);

		return this.getDataByPath(parentPath, searchRules);	
	}

	/**
	 * This method is intended to resolve the child keyword name with the actual
	 * names of the children in an ArrayList of Strings. You have to pass as a
	 * parameter the entire node names parent - child list (with the child keyword
	 * substituted in). The list will be traversed until child is found and
	 * resolved. Does not care about rule nodes and makes no effort to distinguish
	 * them from regular nodes.
	 * 
	 * @param nodeNames
	 * @return
	 */
	public ArrayList<String> resolveChildKeyWord(ArrayList<String> nodeNames, boolean searchRules) {

		/* Empty arrays are immediately rejected. */
		if (nodeNames.size() < 1) {
			return new ArrayList<String>();
		}

		String nextNodeName;
		Node currentNode;
		if (nodeNames.get(0).equals("Root")) {
			currentNode = this.rootNode;
		} else {
			currentNode = this.getNodeByName(nodeNames.get(0), searchRules);
		}

		for (int i = 1; i < nodeNames.size(); i++) {
			nextNodeName = nodeNames.get(i);

			if (nextNodeName.equals("Child")) {

				return new ArrayList<String>(currentNode.getChildren().keySet());
			}

			currentNode = currentNode.getChild(nextNodeName);

		}

		/* Child Id not being resolved causes the method to return an empty list. */
		return new ArrayList<String>();
	}


	/*
	 * Deletes a node (via the parent). Returns the number of nodes deleted by the
	 * operation (i.e. 1 or 0)
	 */
	public int deleteNode(Node parentNode, String nodeName) {

		int nodesDeleted = 0;

		LinkedHashMap<String, Node> theChildren = parentNode.getChildren();
		Node deletedNode = theChildren.remove(nodeName);

		if (deletedNode != null) {

			nodesDeleted = 1;
			parentNode.setChildren(theChildren);

			/* ...and its gone. Java's garbage collection is a good thing... */
		}

		return nodesDeleted;
	}

	/**
	 * This method creates an empty set of rule nodes for the selectedNodeName
	 * passed as the name of the ruleset. Does not modify/overwrite rules that
	 * already exist. Prerequisite: the node structure Root.rule.(selectedNodeName)
	 * already exists
	 * 
	 * @param selectedNodeName
	 */
	public void createDefaultRuleSet(String selectedNodeName) {

		Node ruleNode = this.getNodeByHierarchy("rule." + selectedNodeName, true);

		if (ruleNode != null) {

			for (String ruleName : Definitions.nodeRuleNames) {

				if (ruleNode.getChildren().containsKey(ruleName)) {

					continue;

				}

				if (Definitions.nodeRuleDefaultValues.containsKey(ruleName)) {

					ruleNode.addNodeChild(ruleName, Definitions.nodeRuleDefaultValues.get(ruleName));

				} else {

					ruleNode.addNodeChild(ruleName);

				}

			}

		}

	}

	/**
	 * Renames a node (via the parent). Returns the number of nodes renamed by the
	 * operation (i.e. 1 or 0)
	 */
	public int renameNode(Node parentNode, String nodeName, String newNodeName) {

		int renamedNodes = 0;

		LinkedHashMap<String, Node> theChildren = parentNode.getChildren();
		Node childNode = theChildren.get(nodeName);
		Node deletedNode = theChildren.remove(nodeName);

		if (deletedNode != null) {

			renamedNodes = 1;
			theChildren.put(newNodeName, childNode);
			parentNode.setChildren(theChildren);
		}

		return renamedNodes;
	}

	/**
	 * This the private implementation of getNodesByName with all of the extra
	 * parameters that are automatically populated in the public method.
	 * 
	 * @param nodeName    - the current node being searched for
	 * @param currentNode - the current Node object being traversed
	 * @param resultNodes - the container of node found
	 * @param searchRules - boolean true or false, whether root.rules.* should be
	 *                    included in the result set or not. Default is false
	 * @return
	 */
	/*
	 * private ArrayList<Node> getNodesByName(String nodeName, Node currentNode,
	 * ArrayList<Node> resultNodes, boolean searchRules) {
	 * 
	 * Node nextNode;
	 */
	/* If its a LeafNode, stop traversing */
	/*
	 * if (currentNode.getChildren().isEmpty()) { return resultNodes; }
	 * 
	 * for (String key : currentNode.getChildren().keySet()) {
	 */
	/* Skip rules from being traversed if specified */
	/*
	 * if (!searchRules && key.equals("rule")) {
	 * 
	 * continue; }
	 * 
	 * nextNode = currentNode.get(key);
	 */
	/* If the Next Node's name is a match, add it to the list of Result Nodes */
	/*
	 * if (nodeName.equals(key)) { resultNodes.add(nextNode); }
	 * 
	 * resultNodes = this.getNodesByName(nodeName, nextNode, resultNodes,
	 * searchRules); } return resultNodes; }
	 */
	/**
	 * This the private implementation of getNodesByValue with all of the extra
	 * parameters that are automatically populated in the public method.
	 * 
	 * @param nodeValue   - the current node value being searched for
	 * @param currentNode - the current Node object being traversed
	 * @param resultNodes - the container of node found
	 * @return an array list of the current result nodes
	 */
	private ArrayList<Node> getNodesByValue(String nodeValue, Node currentNode, ArrayList<Node> resultNodes) {

		Node nextNode;

		/* If its a LeafNode, stop traversing */
		if (currentNode.getChildren().isEmpty()) {
			return resultNodes;
		}

		for (String key : currentNode.getChildren().keySet()) {

			nextNode = currentNode.getChild(key);

			/* If the Next Node's name is a match, add it to the list of Result Nodes */
			if (nextNode.getValue().equals(nodeValue)) {
				resultNodes.add(nextNode);
			}

			resultNodes = this.getNodesByValue(nodeValue, nextNode, resultNodes);
		}
		return resultNodes;
	}

	/**
	 * This is a helper method for getParentNodesByHierarchy. The method retrieves the
	 * nodes that come from a parent - child structure encountering the 'child'
	 * keyword. Once child keyword is resolved to the node children names, the nodes
	 * are traversed.
	 * 
	 * @param nodeNames
	 * @param currentNamesIndex
	 * @param currentNode
	 * @param searchRules       boolean whether or not to include rule nodes in the
	 *                          result set, true or false
	 * @return an ArrayList of the matching nodes
	 */
	private ArrayList<Node> getChildKeyWordResults(ArrayList<String> nodeNames, int currentNamesIndex, Node currentNode,
			boolean searchRules) {

		ArrayList<Node> childResultNodes = new ArrayList<Node>();
		int nodesWithChildStartIndex = currentNamesIndex;

		ArrayList<String> childNames = this.resolveChildKeyWord(nodeNames, searchRules);
		String currentNodeName = "";
		Node childCurrentNode;

		for (String childName : childNames) {
			childCurrentNode = currentNode;
			currentNamesIndex = nodesWithChildStartIndex;

			childCurrentNode = childCurrentNode.getChild(childName);
			currentNamesIndex = nodeNames.indexOf("Child");

			currentNamesIndex++;

			while (currentNamesIndex != nodeNames.size()) {

				currentNodeName = nodeNames.get(currentNamesIndex++);
				childCurrentNode = childCurrentNode.getChild(currentNodeName);

				if (childCurrentNode == null) {

					return childResultNodes;

				}

			}

			childResultNodes.add(childCurrentNode);
		}

		return childResultNodes;
	}

}