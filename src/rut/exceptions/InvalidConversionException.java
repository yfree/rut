/* 
Copyright 2019 Yaakov Freedman

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
*/

package rut.exceptions;

/**
 * InvalidConversionExeception is thrown when one of Statement's conversion
 * methods encounters a value that cannot be converted. Current methods that
 * throw this Exception are: Statement.intify(String stringValue)
 * 
 * @author Yaakov Freedman
 * @version dev 0.1
 */

@SuppressWarnings("serial")
public class InvalidConversionException extends Exception {

	public InvalidConversionException(String dataType) {

		super("Value is invalid for the data type " + dataType + ".");

	}
}