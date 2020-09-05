package rut.dataFormat;

import rut.Node;

/**
XML support is implemented in this class
 * @author Yaakov Freedman
 * @version dev 0.1
 */
/*TODO: Implement */
public class XML implements DataFormat{

	public XML() {
		
	}
	
	public Node convertFrom(String text) {
		return new Node();
	}
	
	public String convertTo(Node rootNode) {
		return "";
	}
}