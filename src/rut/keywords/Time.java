package rut.keywords;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import rut.Definitions;
import rut.MemoryStorage;
import rut.utilities.Randomizer;

public class Time extends Keyword {

	public Time(MemoryStorage memory) {
		super(memory, "Time");

	}

	/**
	 * Creates a random time. Can display the current time with the the parameter set as 'now'.
	 * 
	 * @param memory the instance of MemoryStorage that the application is using,
	 *               access is needed to the data
	 * @param if this String is 'now', the current time will be displayed
	 * @return a time in the 'Definitions' format as a String
	 */

		public String generate(String parameter) {
			
			String time;

			if (parameter.isEmpty()) {
				
				time = Randomizer.time();
		
			}
			else if (parameter.equals("now")) {
				
				   DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern(Definitions.timeFormat);  
				   LocalDateTime timeNow = LocalDateTime.now(); 

				   time = timeFormat.format(timeNow);
										
			} else {
				
				time = this.getKeywordName() + " " + parameter;

			}
					
	return time;
}}
