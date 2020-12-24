package rut.operation;

import rut.MemoryStorage;
import rut.Statement;

public class OperationFactory {

	public static Operation createOperation(Statement opStatement, MemoryStorage memory) throws InvalidOperationException  {
		
		String opName = opStatement.getOperation();
		
		switch (opName) {

		case "begin":
			return new Begin(opStatement, memory);
			
		case "comment":
			return new Comment(opStatement, memory);
			
		case "commit":
			return new Commit(opStatement, memory);	
			
		case "delete":
			return new Delete(opStatement, memory);
			
		case "enforce":
			return new Enforce(opStatement, memory);	

		case "exit":
			return new Exit(opStatement, memory);

		case "read":
			return new Read(opStatement, memory);
			
		case "redo":
			return new Redo(opStatement, memory);

		case "rename":
			return new Rename(opStatement, memory);

		case "rollback":
			return new RollBack(opStatement, memory);

		case "undo":
			return new Undo(opStatement, memory);
			
		case "write":
			return new Write(opStatement, memory);

		default:
			throw new InvalidOperationException();
		}
	}
}