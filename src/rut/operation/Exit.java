package rut.operation;

import rut.MemoryStorage;
import rut.Statement;

public class Exit extends Operation {

	public Exit(Statement opStatement, MemoryStorage memory) {
		super(opStatement, memory);

	}
	
	public String execute() {
	
		this.memory.setKillSignal(true);
		return "Bye!";
	}
	
}
