package rut.operation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;

public class Read extends Operation {

	public Read(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);
		this.opVerbPastTense = "read";

	}

	protected int processNodeData(String fullPath, Node fetchedNode) {

		int nodeCount = 0;
		ArrayList<String> rawResultLines = new ArrayList<String>();

		if (this.descendantNamesValues.isEmpty()) {
			rawResultLines = fetchedNode.generateTree(this.childNameToProcess);
		} else {
			HashSet<String> selectedChildNodePaths = this.fetchSelectedChildNodePaths(fullPath);

			/*
			 * If none of the selected node children exist, white list only the selected
			 * node, no children
			 */
			if (selectedChildNodePaths.isEmpty()) {

				selectedChildNodePaths.add(this.childNameToProcess);
			}

			rawResultLines = fetchedNode.generateTree(this.childNameToProcess, selectedChildNodePaths);
		}

		nodeCount = rawResultLines.size();
		this.outputBufferRows.addAll(rawResultLines);

		return nodeCount;

	}

	/**
	 * Retrieves the child nodes selected based on the fullPath String that is passed 
	 * to the method. Whatever the fullPath is, only its descendants will be searched for the children nodes that are
	 * from childNameValues. The node data is returned
	 * 
	 * 
	 * @return ConcurrentHashMap<String, Node> fetchedChildNodes
	 */

	private HashSet<String> fetchSelectedChildNodePaths(String fullPath) {
		ConcurrentHashMap<String, Node> candidateChildNodesData = new ConcurrentHashMap<String, Node>();
		HashSet<String> checkedChildNodePaths = new HashSet<String>();

		for (String descendantName : this.descendantNamesValues.keySet()) {

			candidateChildNodesData = this.memory.getDataByPath(descendantName, this.searchRules);
			checkedChildNodePaths.addAll(this.buildChildNodePaths(candidateChildNodesData, fullPath));
		}

		return checkedChildNodePaths;
	}

	/**
	 * A helper method for fetchSelectedChildNodePaths 
	 * Accepts a ConcurrentHashMap<String, Node> of node data taken from the data
	 * map that contains the name matching a name in the selected childNameValues and the fullPath 
	 * (as passed by the previous method that calls this)
	 * This method will check each entry in the data map to make sure the node is a
	 * descendant of the selected node. i.e. just because it has the name we are
	 * looking for doesn't mean it is one of the selected node's children. Once this
	 * is verified, it will also add all of node's descendants...This method doesn't
	 * just check, it also builds, hence the name. Upon completion a HashSet will be
	 * returned containing only the paths of nodes that have the correct node name
	 * AND are descendants of the selected node. The returned paths START with the
	 * selected node name...
	 * 
	 * @param candidateChildNodesData data of paths to check their descendants
	 * @return HashSet<String> of checked paths that start with the selected node
	 *         name
	 */
	private HashSet<String> buildChildNodePaths(ConcurrentHashMap<String, Node> candidateChildNodesData,
			String fullPath) {

		HashSet<String> checkedPaths = new HashSet<String>();
		Node currentNode;
		String currentPath = "";
		String shortCandidatePath = "";

		for (String candidateChildPath : candidateChildNodesData.keySet()) {

			currentNode = candidateChildNodesData.get(candidateChildPath);

			if (fullPath.equals("")) {
				currentPath = this.childNameToProcess;
				shortCandidatePath = candidateChildPath;
			} else {
				currentPath = fullPath + "." + this.childNameToProcess;
				shortCandidatePath = candidateChildPath.replace(fullPath + ".", "");
			}

			/*
			 * If currentPath passes this check, the node is a descendant of our selected
			 * node and a valid result
			 */
			
			
			if (this.memory.checkSubPathInPath(currentPath, candidateChildPath)) {

				checkedPaths.addAll(this.explodeNodePaths(shortCandidatePath));

				/* Check this child node for its own descendants and add them to the list */
				checkedPaths.addAll(currentNode.buildNodeDescendantPaths(shortCandidatePath));

			}
		}
		return checkedPaths;
	}

	/**
	 * Takes a full path and returns a HashSet the nodes in the path like so:
	 * Example: Input: a.b.c Output: a.b.c, a.b, a
	 * 
	 * This is necessary to filter out acceptable nodes paths to traverse when
	 * fetching selected children.
	 * 
	 * @param fullPath
	 * @return
	 */
	private HashSet<String> explodeNodePaths(String fullPath) {
		HashSet<String> resultPaths = new HashSet<String>();
		String[] nodes = fullPath.split("\\.");
		StringBuilder currentPath = new StringBuilder();

		for (String node : nodes) {

			currentPath.append(node);
			resultPaths.add(currentPath.toString());

			currentPath.append(".");
		}

		return resultPaths;
	}
}