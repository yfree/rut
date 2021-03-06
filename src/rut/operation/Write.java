package rut.operation;

import java.util.concurrent.ConcurrentHashMap;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;

public class Write extends Operation {

	public Write(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);
		this.opVerbPastTense = "written";
		
		this.fetchedNodesData = this.memory.getRootData();
		
	}
	
	protected int processNodeData(String fullPath, Node fetchedNode) {

		boolean newNode = false;
		boolean setValue = this.statement.getSelectedNodeValue().length() > 0 ? true : false;
		
		StringBuilder finalPath = new StringBuilder();
		finalPath.append(fullPath);
		
		if (finalPath.length() > 0) {
			
			finalPath.append(".");
			
		}

		String resultLine = "";
		String fullNodeName = finalPath.toString() + this.childNameToProcess;
		
		/*
		 * value
		 * child name to process
		 * fetched node
		 * */
		Node currentNode = fetchedNode.getChild(this.childNameToProcess);
		if (currentNode == null) {
		
			fetchedNode.addNodeChild(this.childNameToProcess, this.statement.getSelectedNodeValue());
			currentNode = fetchedNode.getChild(this.childNameToProcess);
			
			
			this.memory.addDataMap(currentNode, fullNodeName);
			newNode = true;
		
		}
		else {
			
			/* We only overwrite the existing value if a new value is set. Otherwise the node is not touched. */
			if (setValue) {
			
				currentNode.setValue(this.statement.getSelectedNodeValue());
			}
		}
		/**/
		
		this.memory.setWriteToDiskSignal(true);

		if (newNode && setValue) {
			resultLine = "New node " + fullNodeName + " created, value set to '" + this.statement.getSelectedNodeValue() + "'.";
		}
		else if (newNode && !setValue) {
			resultLine = "New node " + fullNodeName + " created.";			
		}
		else if (!newNode && setValue) {
			resultLine = fullNodeName + " value set to '" + this.statement.getSelectedNodeValue() + "'.";
		}
		else {
			resultLine = fullNodeName + " was not modified.";
		}
		this.outputBufferRows.add(resultLine);
		return 1;
	}
	
	/* Write operation overrides the filterDataNodesWithoutChild method. 
	 * This is the only node-processing operation for which we do not care if the node in question exists or not.
	 * If it doesn't exist, we would like to create a new one. Therefore, this method is prevented from filtering result nodes. */
	
	protected ConcurrentHashMap<String, Node> filterNodesDataWithoutChild(ConcurrentHashMap<String, Node>  uncheckedNodesData) {
		return uncheckedNodesData;
	}
	
}
