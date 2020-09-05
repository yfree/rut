package rut;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * Definitions is a static class that contains all of the globally defined data.
 * When adding new operations, types, rules, etc. it is important to add these
 * entries in this class so that it can be made available to the rest of the Rut
 * Database application.
 * 
 * @author Yaakov Freedman
 * @version dev 0.2
 */
public class Definitions {

	/* The date format used through out the application. */
	public static String dateFormat;
	
	/* The time format used through out the application. */
	public static String timeFormat;
	
	/* Defines the operations by name and commands used to invoke them. */
	public static LinkedHashMap<String, String[]> operations;
	
	/* Defines the operations that require an argument */
	public static HashSet<String> requiredArgument;
	
	/* The names and order of the node rules are defined here. */
	public static HashSet<String> nodeRuleNames;
	
	 /* The list of enforced data types available for a node value. */
	public static HashSet<String> nodeRuleTypes;
 
	/* A list of node rules and their default values for rules that have default values.  */
	public static LinkedHashMap<String, String> nodeRuleDefaultValues;
	
	/* List of valid Data Formats that are understood Rut Database */
	public static HashSet<String> dataFormats;
	
	/* A comprehensive list of all the reserved words that cannot be used as identifiers in a 
	 * Rut Database. */
	public static HashSet<String> reservedWords;
	
	/*
	 * A list of keywords that when used for values or names are processed to 
	 * their corresponding value.
	 */
	public static HashSet<String> keywords;
	
	static {

		/* Set static variables */
		
		dateFormat = "MM/dd/yyyy";
		
		timeFormat = "HH:mm:ss";
		
		operations = new LinkedHashMap<String, String[]>();
		operations.put("read", new String[] { "read" });
		operations.put("write", new String[] { "write" });
		operations.put("delete", new String[] { "delete" });
		operations.put("rename", new String[] { "rename" });
		operations.put("exit", new String[] { "exit" });
		operations.put("begin", new String[] { "begin" });
		operations.put("commit", new String[] { "commit" });
		operations.put("rollback", new String[] { "rollback" });
		operations.put("comment", new String[] { "//" });

		requiredArgument = new HashSet<String>();
		requiredArgument.add("read");
		requiredArgument.add("write");
		requiredArgument.add("delete");
		requiredArgument.add("rename");

		String[] ruleNames = new String[] { "type", "max", "min", "required", "key", "unique" };

		nodeRuleNames = new HashSet<String>(Arrays.asList(ruleNames));

		String[] nodeTypes = new String[] { "text", "integer", "boolean", "decimal", "date", "time" };

		nodeRuleTypes = new HashSet<String>(Arrays.asList(nodeTypes));
	
		nodeRuleDefaultValues = new LinkedHashMap<String, String>();
		nodeRuleDefaultValues.put("required", "false");
		nodeRuleDefaultValues.put("key", "false");
		nodeRuleDefaultValues.put("type", "text");
		nodeRuleDefaultValues.put("unique", "false");
		
		reservedWords = new HashSet<String>();
		reservedWords.addAll(operations.keySet());
		reservedWords.add("rule");
		reservedWords.add("config");
		
		reservedWords.addAll(nodeRuleNames);
		reservedWords.addAll(nodeRuleTypes);
		
		dataFormats = new HashSet<String>();
		
		dataFormats.add("Basic");
		dataFormats.add("RutFormat");
		dataFormats.add("XML");
		dataFormats.add("JSON");
		
		String[] keywordsList = new String[] { "Root", "Child", "Times", "Newid", "FirstNameMale", "FirstNameFemale", "LastName", "Time", "Date", "Integer", "Decimal", "Text", "Boolean"};

		keywords = new HashSet<String>(Arrays.asList(keywordsList));	
	}
}
