package rut.operation;


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;

public class Delete extends Operation {

	public Delete(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);
		this.opVerbPastTense = "deleted";
	}

	public int processNodeData(String fullPath, Node fetchedNode) {
		Node deletedNode = new Node();
		Node parentNode = new Node();
		int nodesDeleted = 0;
		String childName = "";
		LinkedHashMap<String, Node> theChildren = new LinkedHashMap<String, Node>();
		LinkedHashSet<String> dataToProcessOrder = this.generateDescendantDataToProcess(fullPath, fetchedNode);
		
		/* A parent Node matches each child path in the DataToProcess container */
		for (String fullChildPath : dataToProcessOrder) {

			parentNode = this.dataToProcess.get(fullChildPath);
			theChildren = parentNode.getChildren();
			childName = this.memory.parseNodeName(fullChildPath);
			deletedNode = theChildren.remove(childName);

			if (deletedNode != null) {

				nodesDeleted++;
				this.memory.setWriteToDiskSignal(true);
				parentNode.setChildren(theChildren);
				this.memory.deleteDataMap(fullChildPath);

			}

		}
		return nodesDeleted;

	}
}