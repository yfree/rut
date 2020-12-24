package rut.operation;

import java.util.concurrent.ConcurrentHashMap;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;

public class Write extends Operation {

	public Write(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);
		this.opVerbPastTense = "written";
		
	}
	
	protected int processNodeData(String fullPath, Node fetchedNode) {

		/* Handle Root Node */
		StringBuilder finalPath = new StringBuilder();
		finalPath.append(fullPath);
		if (finalPath.length() > 0) {
			finalPath.append(".");
		}
		
		Node currentNode = fetchedNode.getChild(this.childNameToProcess);
		if (currentNode == null) {
		
			fetchedNode.addNodeChild(this.childNameToProcess, this.statement.getSelectedNodeValue());
			currentNode = fetchedNode.getChild(this.childNameToProcess);
			
			this.memory.addDataMap(currentNode, finalPath.toString() + this.childNameToProcess);
		
		}
		else {
			
			currentNode.setValue(this.statement.getSelectedNodeValue());
		}
		
		this.memory.setWriteToDiskSignal(true);

		return 1;
	}
	
	/* Write operation overrides the filterDataNodesWithoutChild method. 
	 * This is the only node-processing operation for which we do not care if the node in question exists or not.
	 * If it doesn't exist, we would like to create a new one. Therefore, this method is prevented from filtering result nodes. */
	
	protected ConcurrentHashMap<String, Node> filterNodesDataWithoutChild(ConcurrentHashMap<String, Node>  uncheckedNodesData) {
		
		return uncheckedNodesData;
	}
	
}
