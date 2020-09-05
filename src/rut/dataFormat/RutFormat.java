package rut.dataFormat;

import rut.Node;

/**
RutFormat is the default node output
 * @author Yaakov Freedman
 * @version dev 0.1
 */
/*TODO: Implement, move from Node.traverse and other areas where 'read' is imlemented
 * to use this instead... */
public class RutFormat implements DataFormat{

	public RutFormat() {
		
	}
	
	public Node convertFrom(String text) {
		return new Node();
	}
	
	public String convertTo(Node rootNode) {
		return "";
	}
}
