package rut.dataFormat;

public class DataFormatFactory {

	public static DataFormat createDataFormat(String dataFormatName){

		switch (dataFormatName) {
		case "JSON":
			return new JSON();
		case "XML":
			return new XML();

		default:
			return new RutFormat();
		}

	}
}
