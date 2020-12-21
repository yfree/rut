package rut.operation;

import rut.MemoryStorage;
import rut.Statement;

public class DisplayError extends Operation{
	
	public DisplayError(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);

	}
	
	public String execute() {
	
		return this.statement.getErrorMessages().toString();
	}
	
}
