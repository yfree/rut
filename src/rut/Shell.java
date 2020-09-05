/* 
Copyright 2019 Yaakov Freedman

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

 The Rut Shell is the interface between the Rut Database 
 and the user.
 The user interacts with the database in one of three ways:
 1) Through command line argument
 2) Through a script file
 3) Through an interactive shell
 
 */

package rut;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class Shell {

	private String shellId;

	private String version;

	private boolean killSignal;

	private Scanner statementScanner;

	private Statement statement;
	
	String input;
	
	String response;

	public Shell(String version) {

		this.version = version;
		this.killSignal = false;
		
		this.statement = new Statement();
		
		/* generate a random id to identify the current shell) */
		this.shellId = UUID.randomUUID().toString();
	
		/* For debugging */
		// System.out.println("Creating Shell Id: " + this.shellId + ".");
	}

	public String getShellId() {
		return this.shellId;
	}

	public String getVersion() {
		return this.version;
	}

	public boolean getKillSignal() {
		return this.killSignal;
	}

	public void spawnShell(Interpreter interpreter, String[] args) {

		/* Read from file */
		if (args.length == 1 && args[0].endsWith(".rut")) {

			interpreter.setSuppressOutputSignal(true);
			this.runScriptShell(interpreter, args[0]);

		}
		
		/* Read from command line argument / stdin */
		else if (args.length > 0 && !args[0].equals("debug")) {

			this.runArgumentShell(interpreter, args);

		}
		
		/* Read from interactive shell with debugging output */
		else if (args.length == 1 && args[0].equals("debug")) {
			
			this.runInteractiveShell(interpreter, "debug");
		
		}
		
		/* Read from interactive shell */
		else {

			this.runInteractiveShell(interpreter);

		}

	}

	private void runInteractiveShell(Interpreter interpreter) {
		this.runInteractiveShell(interpreter, "");
	}
	
	private void runInteractiveShell(Interpreter interpreter, String debug) {
		
		this.input = "";
		this.response = "";
		
		this.statementScanner = new Scanner(System.in);

		this.statementScanner.useDelimiter("(?<!\\\\);");
		
		System.out.println("Rut Database Server, " + this.version + ".");

		while (!this.killSignal) {

			System.out.print("rut~> ");
			this.input = statementScanner.next();

			this.statement.parseStatement(input);
			this.response = interpreter.processStatement(statement);
			this.killSignal = interpreter.getKillSignal();

			if (debug.length() != 0) {
			
				System.out.println(statement);
			}
			
			System.out.println(this.response);
		}

		this.statementScanner.close();

	}

	private void runArgumentShell(Interpreter interpreter, String[] args) {
		
		this.input = "";
		this.response = "";

		String argString = String.join(" ", args);

		if (!argString.endsWith(";")) {
			argString += ";";
		}

		if (!argString.toLowerCase().endsWith("exit;")) {
			argString += "exit;";
		}

		this.statementScanner = new Scanner(argString);

		/* This gives the delimiter ';' the ability to be escaped. 
		 * Escaped semi colons '\;' are not used to delimit.
		 * The '\' is removed and just there to indicate escape. */
		this.statementScanner.useDelimiter("(?<!\\\\);");

		while (!this.killSignal) {

			this.input = this.statementScanner.next();

			this.statement.parseStatement(this.input);
			this.response = interpreter.processStatement(this.statement);
			this.killSignal = interpreter.getKillSignal();

			/* For debugging: */
			//System.out.println(statement);
			
			System.out.println(this.response);
		}

		this.statementScanner.close();
	}

	/* Read from a .rut script file */
	private void runScriptShell(Interpreter interpreter, String filePath) {

		int lineNumber = 1;

		File file = new File(filePath);

		Set<String> errorMessages = new HashSet<String>();

		try {

			this.statementScanner = new Scanner(file);

			this.statementScanner.useDelimiter("(?<!\\\\);");

			while (this.statementScanner.hasNext()) {

				this.input = this.statementScanner.next();

				this.statement.parseStatement(this.input);

				errorMessages = statement.getErrorMessages();

				if (errorMessages.size() > 0) {
					System.out.println(" Error on line number " + lineNumber + ": ");
					for (String errorMessage : errorMessages) {

						System.out.println("    " + errorMessage);
					}
				} else {

					interpreter.processStatement(this.statement);
				}

				lineNumber++;
			}

			statementScanner.close();
			/*
			 * For script input, the file is written to and saved after the script's
			 * execution is complete.
			 */
			interpreter.getDisk().writeDataMapToDisk(interpreter.getMemory().getDataMap());
			
		} catch (FileNotFoundException e) {

			System.out.println("Could not access the script file: " + filePath + "...no changes were made.");
		}

	}
}
