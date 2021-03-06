package rut.dataFormat;

import java.util.ArrayList;

/**
 * DataFormat is an interface for the formats that rut data can be 
 * translated from and converted to. Currently supports: XML, JSon, RutFormat
 * @author Yaakov Freedman
 * @version dev 0.1
 */

public class DataFormat {

	/**
	 * The name of the operation causing the response that the data format is for
	 */
	protected String operation;
	
	public DataFormat(String operation) {
		
		this.operation = operation;
	}
	
	public ArrayList<String> getLines(ArrayList<String> rawLines){
		
		return this.convertLines(rawLines);
	}
	
	public String getText(ArrayList<String> rawLines){
		return String.join("", this.convertLines(rawLines));
	}	
	
	/* This is the only method that needs to be redefined by each child class */
	protected String convertLine(String line) {
	
		return line;
	}
	
	protected ArrayList<String> convertLines(ArrayList<String> lines) {
		
		int longestLine = 0;
		String separator = "";
		String convertedLine = "";
		ArrayList<String> convertedLines = new ArrayList<String>();
		ArrayList<String> finalLines = new ArrayList<String>();
		
		for (String line : lines) {
			
			convertedLine = this.convertLine(line);
			
			if (convertedLine.length() > longestLine) {
			
				longestLine = convertedLine.length();
			}
			
			convertedLines.add(convertedLine);
		}

		if (convertedLines.size() > 0) {
	
			separator = this.createSeparator(longestLine);
	
			finalLines.add(separator);
			finalLines.addAll(convertedLines);
			finalLines.add(separator);
		}
		
		return finalLines;		
	}
	
	protected String createSeparator(int separatorSize) {

		StringBuilder separator = new StringBuilder();

		if (separatorSize > 64) {

			separatorSize = 64;
		}

		for (int i = 0; i < separatorSize; i++) {
			separator.append("=");
		}

		return separator.toString();
	}
}
