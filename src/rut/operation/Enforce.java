package rut.operation;

import rut.MemoryStorage;
import rut.Node;
import rut.Statement;

public class Enforce extends Operation {

	public Enforce(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);

	}
	
	protected int processNode(Node fetchedNode) {
		
		return 0;
	}
	
}
