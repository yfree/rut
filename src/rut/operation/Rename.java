package rut.operation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;

public class Rename extends Operation {

	private String newNodeName;

	public Rename(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);

		this.newNodeName = this.statement.getSelectedNodeValue();

		this.opVerbPastTense = "renamed";
	}

	public int processNodeData(String fullPath, Node fetchedNode) {
		Node renamedNode = new Node();
		Node parentNode = new Node();
		int nodesRenamed = 0;
		String childName = "";
		LinkedHashMap<String, Node> theChildren = new LinkedHashMap<String, Node>();

		LinkedHashSet<String> dataToProcessOrder = this.generateDescendantDataToProcess(fullPath, fetchedNode);

		/* A parent Node matches each child path in the DataToProcess container */
		for (String fullChildPath : dataToProcessOrder) {

			parentNode = this.dataToProcess.get(fullChildPath);
			theChildren = parentNode.getChildren();
			childName = this.memory.parseNodeName(fullChildPath);

			if (this.childNameToProcess.equals(childName)) {

				renamedNode = theChildren.remove(childName);
				theChildren.put(this.newNodeName, renamedNode);
				this.memory.setWriteToDiskSignal(true);
				parentNode.setChildren(theChildren);
				nodesRenamed++;
			}

			this.memory.renameParentDataMap(fullChildPath, this.childNameToProcess, this.newNodeName);

		}
		return nodesRenamed;
	}

}