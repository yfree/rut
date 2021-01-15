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

package rut.operation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;
import rut.dataFormat.DataFormat;
import rut.dataFormat.DataFormatFactory;

public abstract class Operation {

	protected Statement statement;
	protected MemoryStorage memory;

	protected String dataFormat;
	protected Integer processedNodesCount;
	protected ArrayList<String> outputBufferRows;
	public String opVerbPastTense;
	public String childNameToProcess;
	public ConcurrentHashMap<String, Node> dataToProcess;
	ConcurrentHashMap<String, Node> fetchedNodesData;

	public Operation(Statement opStatement, MemoryStorage memoryStorage) {

		this.statement = opStatement;
		this.memory = memoryStorage;
		this.dataFormat = opStatement.getDataFormat();
		this.outputBufferRows = new ArrayList<String>();
		this.processedNodesCount = 0;
		this.dataToProcess = new ConcurrentHashMap<String, Node>();
		this.fetchedNodesData = new ConcurrentHashMap<String, Node>();

	}

	/*
	 * Redo this method in its entirety! The default execute() method performs
	 * processNode() on every node fetched. Nodes are identified for fetching using
	 * the following named criteria: parent Name , node name, Child keyword, Root
	 * keyword, child nodes when a colon proceeds them. And all associated values
	 * set with any of the above node identifiers, i.e. x = y
	 */
	public String execute() {

		this.childNameToProcess = this.statement.getSelectedNodeName();

		String selectedNodeValue = statement.getSelectedNodeValue();
		LinkedHashMap<String, String> childrenNamesValues = statement.getChildrenNamesValues();
		ArrayList<String> parentNames = statement.getParentNames();
		String nodeHierarchy = statement.getNodeHierarchyString();
		LinkedHashMap<String, ArrayList<String>> whereConditionRules = statement.getWhereConditionRules();
		boolean searchRules = parentNames.contains("rule");
		ConcurrentHashMap<String, Node> nodesData = new ConcurrentHashMap<String, Node>();

		/*
		 * We need a parent node and a child name to do processing. We are now going to
		 * fetch the parent node
		 */

		if (this.statement.getNodeHierarchyString().equals("Root")) {

			nodesData = this.memory.getDataByPath("Root", true);

		} else if (parentNames.isEmpty()) {

			nodesData = this.memory.getParentNodesDataByChildName(this.childNameToProcess, searchRules);

		} else {

			nodesData = this.memory.getParentNodesDataByHierarchy(nodeHierarchy, searchRules);
			/*
			 * We have retrieved the parent node but still have to check if this node has a
			 * child with the required child node name we are searching for
			 */
			nodesData = this.filterNodesDataWithoutChild(nodesData);
		}

		/*
		 * If nodesData has fetched values, we want to keep it. Otherwise, we want
		 * fetchedNodesData to contain a default value.
		 */

		if (!nodesData.isEmpty()) {

			this.fetchedNodesData = nodesData;

		}

		
		for (String fullPath : this.fetchedNodesData.keySet()) {

			this.processedNodesCount += this.processNodeData(fullPath, this.fetchedNodesData.get(fullPath));
		}
		return this.generateResponse();
	}

	protected ConcurrentHashMap<String, Node> filterNodesDataWithoutChild(
			ConcurrentHashMap<String, Node> uncheckedNodesData) {
		/*
		 * Parent Nodes checked to contain the child, if so, parent nodes are added to
		 * result set
		 */
		ConcurrentHashMap<String, Node> resultNodesData = new ConcurrentHashMap<String, Node>();
		Node childNode, parentNode;

		for (String fullPath : uncheckedNodesData.keySet()) {

			parentNode = uncheckedNodesData.get(fullPath);

			childNode = parentNode.getChild(this.childNameToProcess);

			if (childNode != null) {

				resultNodesData.put(fullPath, parentNode);

			}
		}

		return resultNodesData;
	}

	protected String generateResponse() {

		StringBuilder resultMessage = new StringBuilder();

		DataFormat dataFormat = DataFormatFactory.createDataFormat(this.dataFormat);

		if (this.processedNodesCount > 0) {

			for (String line : dataFormat.getLines(this.outputBufferRows)) {

				resultMessage.append(line + "\n");

			}
		}

		if (this.processedNodesCount == 0) {
			resultMessage.append("No nodes " + this.opVerbPastTense + ".");
		} else if (this.processedNodesCount == 1) {
			resultMessage.append("1 node " + this.opVerbPastTense + ".");
		} else if (this.processedNodesCount > 1) {
			resultMessage.append(this.processedNodesCount.toString() + " nodes " + this.opVerbPastTense + ".");
		}

		return resultMessage.toString();
	}

	/*
	 * processNode() is operation specific.
	 * 
	 * No two node-processing operations will process a node in the same way.
	 * However not all operations are node-processing operations, therefore defining
	 * a procesNode() is optional. For non-node-processing operations, you can
	 * redefine the execute method in its entirety.
	 */
	protected int processNodeData(String fullPath, Node fetchedNode) {
		/* intentionally left blank */
		return 0;
	}

	/* generateDescendantDataToProcess populates the member variable dataToProcess with not only the 
	 * child path and parent node of the full child path/fetched node passed to it (in ready for processing format),
	 * but also all of the node's descendants. This is useful for operations where the node to process's descendants must 
	 * also be operated on the same way (delete, rename).
	 *  Due to the fact that the order may be of importance (for instance with the delete operation)
	 *  
	 *  NOTE: In order to use this method, simply call it within your operation specific processNodeData and pass 
	 *  it the parameters that were passed to processNodeData.
	 *  It will return a LinkedHashSet of the descendant paths to process in the appropriate order.
	 *  More importantly though, it will also populate the member variable Operation 'dataToProcess'.
	 *  */
	
	protected LinkedHashSet<String> generateDescendantDataToProcess(String fullPath, Node fetchedNode) {
		
		StringBuilder finalPath = new StringBuilder();
		finalPath.append(fullPath);

		if (finalPath.length() > 0) {

			finalPath.append(".");

		}
		
		LinkedHashSet<String> dataToProcessOrder = new LinkedHashSet<String>();		
		String rawLine = "";
		String pathFromChild = "";
		String fullPathOfChild = "";
		String parentName = "";
		int separatorToken = 0;
		Node parentNode = new Node();

		ArrayList<String> recursiveReadLines = fetchedNode.generateTree(this.childNameToProcess);
		if (recursiveReadLines.size() > 1) {

			ConcurrentHashMap<String, Node> parentNodeData = new ConcurrentHashMap<String, Node>();

			for (int i = recursiveReadLines.size() - 1; i >= 0; i--) {
				
				rawLine = recursiveReadLines.get(i);
				separatorToken = rawLine.indexOf(':');
				pathFromChild = rawLine.substring(0, separatorToken);
				fullPathOfChild = finalPath.toString() + pathFromChild;
				parentName = this.memory.parseParentName(fullPathOfChild);
				parentNodeData = this.memory.getDataByPath(parentName, false);
				
				if (parentNodeData.size() > 0) {
					
					parentNode = memory.justNodes((parentNodeData)).get(0);
					this.dataToProcess.put(fullPathOfChild, parentNode);
					dataToProcessOrder.add(fullPathOfChild);
				}

			}

		} else {

			fullPathOfChild = finalPath.toString() + this.childNameToProcess;
			dataToProcessOrder.add(fullPathOfChild);
			this.dataToProcess.put(fullPathOfChild, fetchedNode);
		}
		
		return dataToProcessOrder;

	}
	
	protected boolean validate() {

		return false;
	}
}
