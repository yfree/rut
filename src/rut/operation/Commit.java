package rut.operation;

import rut.MemoryStorage;
import rut.Statement;

public class Commit extends Operation{
	
	public Commit(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);

	}
	
	public String execute() {	
		/* TODO */
		/* Reach into memory and perform a commit */
		
		return "abc";
	}
	
}
