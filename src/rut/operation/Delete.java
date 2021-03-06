package rut.operation;


import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;

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
		ConcurrentHashMap<String, Node> theChildren = new ConcurrentHashMap<String, Node>();
		LinkedHashSet<String> dataToProcessOrder = this.generateDescendantDataToProcess(fullPath, fetchedNode);
		String resultLine = "";
		
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

				resultLine = fullChildPath + " deleted.";
				this.outputBufferRows.add(resultLine);
			}

		}
		return nodesDeleted;

	}
}