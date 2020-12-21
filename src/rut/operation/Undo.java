package rut.operation;

import rut.Statement;
import rut.MemoryStorage;

public class Undo extends Operation {

	public Undo(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);

	}
	
	public String execute() {	
		
		/* Intentionally left blank */
		
		return "";
	}
}