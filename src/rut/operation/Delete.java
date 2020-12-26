package rut.operation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
		
		StringBuilder finalPath = new StringBuilder();
		finalPath.append(fullPath);

		if (finalPath.length() > 0) {

			finalPath.append(".");

		}
		
		int nodesDeleted = 0;

		LinkedHashMap<String, Node> theChildren = new LinkedHashMap<String, Node>();

		/*
		 * The node AND its descendants must be processed to prevent orphaned nodes from
		 * remaining. Each record in this data map contains a child PATH key: matching
		 * parent node to process
		 */

		ConcurrentHashMap<String, Node> dataToProcess = new ConcurrentHashMap<String, Node>();
		ArrayList<String> recursiveReadLines = fetchedNode.generateTree(this.childNameToProcess);
		
		String rawLine = "";
		String pathFromChild = "";
		String fullPathOfChild = "";
		String parentName = "";
		String childName = "";
		int separatorToken = 0;
		Node deletedNode = new Node();
		Node parentNode = new Node();

		if (recursiveReadLines.size() > 1) {

			ConcurrentHashMap<String, Node> parentNodeData = new ConcurrentHashMap<String, Node>();

			for (int i = recursiveReadLines.size() - 1; i >= 0; i--) {

				rawLine = recursiveReadLines.get(i);

				separatorToken = rawLine.indexOf(':');

				pathFromChild = rawLine.substring(0, separatorToken);
				
				fullPathOfChild = finalPath.toString() + pathFromChild;
				
				parentName = this.memory.parseParentName(fullPathOfChild);
				
				parentNodeData = this.memory.getDataByPath(parentName, false);

				parentNode = parentNodeData.get(parentName);
		
				dataToProcess.put(fullPathOfChild, parentNode);

			}

		} else {

			dataToProcess.put(finalPath.toString() + this.childNameToProcess, fetchedNode);
		}

		/*
		 * Process node marked for deletion and its descendants
		 */

		for (String fullChildPath : dataToProcess.keySet()) {

			parentNode = dataToProcess.get(fullChildPath);
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
