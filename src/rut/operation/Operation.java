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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;
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

	public Operation(Statement opStatement, MemoryStorage memoryStorage) {

		this.statement = opStatement;
		this.memory = memoryStorage;
		this.dataFormat = opStatement.getDataFormat();
		this.outputBufferRows = new ArrayList<String>();
		this.processedNodesCount = 0;

	}

	/*
	 * Redo this method in its entirety! The default execute() method performs
	 * processNode() on every node fetched. Nodes are identified for fetching using
	 * the following named criteria: parent Name , node name, Child keyword, Root
	 * keyword, child nodes when a colon proceeds them. And all associated values
	 * set with any of the above node identifiers, i.e. x = y
	 */
	public String execute() {

		String selectedNodeName = statement.getSelectedNodeName();
		String selectedNodeValue = statement.getSelectedNodeValue();
		LinkedHashMap<String, String> childrenNamesValues = statement.getChildrenNamesValues();
		ArrayList<String> parentNames = statement.getParentNames();
		String nodeHierarchy = statement.getNodeHierarchyString();

		LinkedHashMap<String, ArrayList<String>> whereConditionRules = statement.getWhereConditionRules();
		String operation = statement.getOperation();
		Set<String> statementErrors = statement.getErrorMessages();

		boolean searchRules = parentNames.contains("rule");

		ConcurrentHashMap<String, Node> fetchedNodesData = new ConcurrentHashMap<String, Node>();

		this.childNameToProcess = this.statement.getSelectedNodeName();

		/*We need a parent node and a child name to do processing. 
		 *We are now going to fetch the parent node */
		
		if (this.statement.getNodeHierarchyString().equals("Root")) {

			fetchedNodesData.put("", this.memory.getRootNode());
		} else if (parentNames.isEmpty()) {

			fetchedNodesData = this.memory.getParentNodesDataByChildName(this.childNameToProcess, searchRules);

		} else {

			fetchedNodesData = this.memory.getParentNodesDataByHierarchy(nodeHierarchy, searchRules);
			/*
			 * We have retrieved the parent node but still have to check if this node has a
			 * child with the required child node name we are searching for
			 */
			fetchedNodesData = this.filterNodesDataWithoutChild(fetchedNodesData);
		}
		
		for (String fullPath : fetchedNodesData.keySet()) {
/*
			if (fullPath.equals("Root")) {
				fullPath = "";
			}
*/
			System.out.println(fullPath);
			
			this.processedNodesCount += this.processNodeData(fullPath, fetchedNodesData.get(fullPath));

		}

		return this.generateResponse();
	}

	protected ConcurrentHashMap<String, Node> filterNodesDataWithoutChild(ConcurrentHashMap<String, Node> uncheckedNodesData) {
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
		
			for (String line : dataFormat.getLines(this.outputBufferRows)){
			
				resultMessage.append(line + "\n");
		
			}
		}
		
		if (this.processedNodesCount == 0) {
			resultMessage.append("No nodes " + this.opVerbPastTense + ".");
		} else if (this.processedNodesCount == 1) {
			resultMessage.append("1 node " + this.opVerbPastTense + ".");
		} else if (this.processedNodesCount > 1) {
			resultMessage.append(this.processedNodesCount.toString() + " nodes " + 
					this.opVerbPastTense + ".");
		}

		/* Clear member variables as result message is returned */
		this.dataFormat = "";
		this.outputBufferRows = new ArrayList<String>();
		this.processedNodesCount = 0;
		
		return resultMessage.toString();
	}

	/*
	 * processNode() is operation specific.
	 * 
	 * No two node-processing operations will process a node in the same way.
	 * However not all operations are node-processing operations, therefore
	 * defining a procesNode() is optional. For non-node-processing operations, 
	 * you can redefine the execute method in its entirety.
	 */
	protected int processNodeData(String fullPath, Node fetchedNode) {
		/* intentionally left blank */
		return 0;
	}

	protected boolean validate() {

		return false;
	}
}
