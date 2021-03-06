package rut.dataFormat;

public class DataFormatFactory {

	public static DataFormat createDataFormat(String dataFormatName, String operation){

		switch (dataFormatName) {
		case "JSON":
			return new JSON(operation);
		case "XML":
			return new XML(operation);

		default:
			return new RutFormat(operation);
		}

	}
}
