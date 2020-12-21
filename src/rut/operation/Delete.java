package rut.operation;

import java.util.LinkedHashMap;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;

public class Delete extends Operation {

	public Delete(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);
		this.opVerbPastTense = "deleted";
	}
	
	public int processNodeData(String fullPath, Node fetchedNode) {
		
		int nodesDeleted = 0;

		LinkedHashMap<String, Node> theChildren = fetchedNode.getChildren();
		Node deletedNode = theChildren.remove(this.childNameToProcess);

		if (deletedNode != null) {

			nodesDeleted = 1;
			fetchedNode.setChildren(theChildren);

			/* ...and its gone. Java's garbage collection is a good thing... */
		}

		return nodesDeleted;
	}
	
}
