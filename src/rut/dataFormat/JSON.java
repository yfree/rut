package rut.dataFormat;

import rut.Node;

/**
JSON support is implemented in this class
 * @author Yaakov Freedman
 * @version dev 0.1
 */
/*TODO: Implement */
public class JSON implements DataFormat{

	public JSON() {
		
	}
	
	public Node convertFrom(String text) {
		return new Node();
	}
	
	public String convertTo(Node rootNode) {
		return "";
	}
}