package rut.operation;

import java.util.ArrayList;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;


public class Read extends Operation {
	
	
	public Read(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);
		this.opVerbPastTense = "read";
		
	}
	
	protected int processNodeData(String fullPath, Node fetchedNode) {
			
		ArrayList<String> rawResultLines = fetchedNode.generateTree(this.childNameToProcess);
		int nodeCount = rawResultLines.size();		
		
		this.outputBufferRows.addAll(rawResultLines);
	  
		return nodeCount;

	}
	
}