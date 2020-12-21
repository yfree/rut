package rut.operation;

import java.util.LinkedHashMap;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;

public class Rename extends Operation {

	String newNodeName = this.statement.getSelectedNodeValue();
	
	public Rename(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);
		this.opVerbPastTense = "renamed";

	}
	
	protected int processNodeData(String fullPath, Node fetchedNode) {
		
		int renamedNodes = 0;

		LinkedHashMap<String, Node> theChildren = fetchedNode.getChildren();
		Node childNode = theChildren.get(this.childNameToProcess);
		Node deletedNode = theChildren.remove(this.childNameToProcess);

		if (deletedNode != null) {

			renamedNodes = 1;
			theChildren.put(newNodeName, childNode);
			fetchedNode.setChildren(theChildren);
		}

		return renamedNodes;
		
	}
	
}
