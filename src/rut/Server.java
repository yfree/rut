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

The Server is the main class of Rut Database.
The Server defines how all of the other objects will interact with each other in a 
magnificent symphony of digital harmony.

Furthermore, the server controls the networking and access to the database.

Todo: Incorporate the networking, the threads, and the SSL communication.

*/

package rut;

import java.util.concurrent.ConcurrentHashMap;

public class Server {

	public static void main(String[] args) {

		//int PORT = 1922;
		
		String VERSION = "Development Version 0.2";
		
		/* Get the database structure from the master database file */
		DiskStorage disk = new DiskStorage("resources/master.database");
		
		/* Load database tree into memory with all of its methods and variables */
		ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> dataMap = disk.readDataMapFromDisk();
		MemoryStorage memory = new MemoryStorage(dataMap);
		
		/* Load the Rut Querying Language interpreter */
		Interpreter interpreter = new Interpreter(memory, disk);
		
		/* Attach a shell to the interpreter for statement processing */
		Shell shell = new Shell(VERSION);
		shell.spawnShell(interpreter, args); 
		
	}
}
