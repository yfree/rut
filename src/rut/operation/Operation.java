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

	public String operation;
	public String opVerbPastTense;
	public String childNameToProcess;
	public ConcurrentHashMap<String, Node> childDataToProcess;
	protected ConcurrentHashMap<String, Node> fetchedNodesData;
	protected ConcurrentHashMap<String, String> childNamesValues;
	protected ConcurrentHashMap<String, Node> fetchedSelectedChildNodesData;
	protected boolean searchRules;

	public Operation(Statement opStatement, MemoryStorage memoryStorage) {

		this.statement = opStatement;
		this.memory = memoryStorage;
		this.dataFormat = opStatement.getDataFormat();
		this.operation = opStatement.getOperation();
		this.outputBufferRows = new ArrayList<String>();
		this.processedNodesCount = 0;
		this.childDataToProcess = new ConcurrentHashMap<String, Node>();
		this.fetchedNodesData = new ConcurrentHashMap<String, Node>();
		this.fetchedSelectedChildNodesData = new ConcurrentHashMap<String, Node>();
		this.childNamesValues = new ConcurrentHashMap<String, String>();
		this.searchRules = false;
	}

	/*
	 * The default execute() method performs processNode() on every node fetched. If
	 * the developer wants an operation that does not do node fetching (such as the
	 * 'exit' operation), the developer may override the execute() method.
	 * Otherwise, the develop can use this execute method for their operation and
	 * just define the method processNode(), which is going to determine what
	 * happens to the nodes that are fetched.
	 * This method returns the string response sent from the interpreter.
	 */
	public String execute() {

		this.childNameToProcess = this.statement.getSelectedNodeName();

		String selectedNodeValue = this.statement.getSelectedNodeValue();
		this.childNamesValues = this.statement.getChildNamesValues();
		ArrayList<String> parentNames = this.statement.getParentNames();
		ConcurrentHashMap<String, ArrayList<String>> whereConditionRules = this.statement.getWhereConditionRules();
		this.searchRules = parentNames.contains("rule");
		ConcurrentHashMap<String, Node> nodesData = new ConcurrentHashMap<String, Node>();
		String nodeHierarchy = Statement.cleanRootFromString(this.statement.getNodeHierarchyString());
		/*
		 * We need a parent node and a child name to do processing. We are now going to
		 * fetch the parent node
		 */

		if (nodeHierarchy.equals("Root")) {

			nodesData = this.memory.getRootData();

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

			/* Process the selected node as defined for this operation */
			this.processedNodesCount += this.processNodeData(fullPath, this.fetchedNodesData.get(fullPath));

		}
		return this.generateResponse();
	}

	/**
	 * The point of this method is actually in order to optionally NOT call it. You
	 * see, if we want to use an operation where the node name we are calling does
	 * not have to exist, we can just override this method and not call it.
	 * Otherwise, it will ensure that when I do the operation on a node, the node in
	 * question actually exists, not just the parent of that node.
	 *
	 * @param uncheckedNodesData
	 * @return
	 */
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

		DataFormat dataFormat = DataFormatFactory.createDataFormat(this.dataFormat, this.operation);

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
	 * processNode() is operation specific. This method is called once the nodes
	 * have been selected for processing. The programmer defines in this method what
	 * to do with these nodes per the operation. String fullPath - this is the full
	 * path of the PARENT of the node to operate on Node fetchedNode this is the
	 * PARENT of the node to operate on The member variable this.childNameToProcess
	 * is the actual name of the node. Modifications such as node re-indexing
	 * requires the parent node, that's why this is done, although it may sound
	 * counter-intuitive.
	 */
	protected int processNodeData(String fullPath, Node fetchedNode) {
		/* intentionally left blank */
		return 0;
	}

	/**
	 * After a node is processed - its children generally have to be processed as well.
	 * Changes can cause effects down the tree.
	 * 
	 * generateChildDataToProcess populates the member variable dataToProcess
	 * with the child path and parent node of the full child path/fetched
	 * node passed to it (in ready for processing format), along with all of the
	 * node's children. 
	 * NOTE: In order to use this method, call it within your operation
	 * specific processNodeData and pass it the parameters that were passed to
	 * processNodeData. It will return a LinkedHashSet of the child paths to
	 * process in the appropriate order. More importantly though, it will also
	 * populate the member variable Operation 'dataToProcess'.
	 * @return nodeToProcessOrder HashSet - this is the ORDER of the children
	 * You need to keep this variable and process and process the children in this order.
	 * Order matters very much for processing the children
	 */

	protected LinkedHashSet<String> generateChildDataToProcess(String fullPath, Node fetchedNode) {

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
					this.childDataToProcess.put(fullPathOfChild, parentNode);
					dataToProcessOrder.add(fullPathOfChild);
				}

			}

		} else {

			fullPathOfChild = finalPath.toString() + this.childNameToProcess;
			dataToProcessOrder.add(fullPathOfChild);
			this.childDataToProcess.put(fullPathOfChild, fetchedNode);
		}

		return dataToProcessOrder;

	}

	protected boolean validate() {

		return false;
	}
	
}
