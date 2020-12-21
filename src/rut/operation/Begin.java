package rut.operation;

import rut.MemoryStorage;
import rut.Statement;

public class Begin extends Operation{
	
	public Begin(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);

	}
	
	public String execute() {	
		/* TODO */
		/* Reach into memory and perform a begin */
		
		return "abc";
	}
	
}
