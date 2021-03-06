package rut.dataFormat;

/**
 * RutFormat is the default node output
 * 
 * @author Yaakov Freedman
 * @version dev 0.1
 */
/*
 * TODO: Implement, move from Node.traverse and other areas where 'read' is
 * implemented to use this instead...
 */
public class RutFormat extends DataFormat {

	private int tabLength = 4;

	public RutFormat(String operation) {

		super(operation);
	}

	protected String convertLine(String line) {

		String convertedLine = "";
		String nodePath = "";
		String nodeValue = "";
		
	
		switch (this.operation) {
		
		case "read":
			
			int separatorToken = line.indexOf(':');
			nodePath = line.substring(0, separatorToken);
			
			if (separatorToken < line.length() - 1) {
				
				nodeValue = line.substring(separatorToken + 1);
		
			}
		
			convertedLine = this.convertNodePath(nodePath) + "-> " + nodeValue;
			return convertedLine;	
			
		default:
		
			return line;
		}

	
	}

	protected String convertNodePath(String fullNodePath) {
		
		String lastNode = fullNodePath;
		String[] nodeNames = fullNodePath.split("\\.");
		int nodeCount = nodeNames.length;

		if (nodeNames.length > 1) {

			lastNode = nodeNames[nodeCount - 1];

		}

		return this.createTab(nodeCount - 1) + lastNode;
	}

	private String createTab(int tabCount) {

		int MAX_NUMBER_OF_TABS_PER_LINE = 8;
		StringBuilder finalTab = new StringBuilder();

		for (int i = 0; i < tabCount && i < MAX_NUMBER_OF_TABS_PER_LINE; i++) {
			for (int i2 = 0; i2 < tabLength; i2++) {
				finalTab.append(" ");
			}
			
		}

		return finalTab.toString();
	}

}