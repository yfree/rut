package rut.operation;

import rut.MemoryStorage;
import rut.Statement;

public class RollBack extends Operation{
	
	public RollBack(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);

	}
	
	public String execute() {	
		/* TODO */
		/* Reach into memory and perform a rollback */
		
		return "abc";
	}
	
}
