package rut.dataFormat;

import rut.Node;

/**
 * DataFormat is an interface for the formats that rut data can be 
 * translated from and converted to. Currently supports: XML, JSon, RutFormat
 * @author Yaakov Freedman
 * @version dev 0.1
 */
/*TODO: Implement */
public interface DataFormat {

	public Node convertFrom(String text);
	
	public String convertTo(Node rootNode);
}
