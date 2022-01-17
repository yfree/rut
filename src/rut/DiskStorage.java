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

The Disk Storage object stores and retrieves the database to and from permanent storage.
The public methods offered by a DiskStorage object are:

* ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> readDataMapFromDisk() 
* void writeDataMapToDisk(ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> dataMap)

The database is stored in a file called master.database.

The master.database format is as follows:
Each line in the master.database file represents a SINGLE node.

<nodeParent>.<nodeName>:<nodeValue>
Multiple parents can be defined like so: a.b.c.d
The node value can be blank
The ':' mark should not be missing on a line, even if no value is defined.
 
*/
package rut;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author Yaakov Freedman
 * @version dev 0.2
 *
 */
public class DiskStorage {

	private String storageFileName;

	/**
	 * Construct a DiskStorage object.
	 * 
	 * @param storageFileName full path of the database storage file
	 */
	public DiskStorage(String storageFileName) {
		this.storageFileName = storageFileName;

	}

	/**
	 * Reads the database into memory from disk storage.
	 * 
	 * The dataMap is where the database lives in memory. All CRUD operations and
	 * searches are performed on the dataMap. Each node links to its children and
	 * with Node.getChildren(), they can be accessed.
	 * 
	 * @return
	 */
	public ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> readDataMapFromDisk() {
		String masterFileRow;
		String nodeValue;
		String fullNodeName;
		Node dataMapNode;

		ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> dataMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, Node>>();
		ConcurrentHashMap<String, Node> rootRecord = new ConcurrentHashMap<String, Node>();

		rootRecord.put("", new Node());
		dataMap.put("", rootRecord);
		MemoryStorage memory = new MemoryStorage(dataMap);

		try {
			File file = new File(this.storageFileName);

			Scanner masterFile = new Scanner(file);

			while (masterFile.hasNext()) {

				masterFileRow = masterFile.nextLine();

				/* Data Map logic added below */
				dataMapNode = new Node();

				fullNodeName = masterFileRow.split(":", 2)[0];

				if (masterFileRow.split(":", 2).length == 2) {
					nodeValue = masterFileRow.split(":", 2)[1];
				} else {
					nodeValue = "";
				}

				dataMapNode.setValue(nodeValue);

				memory.addDataMap(dataMapNode, fullNodeName);

			}

			masterFile.close();

		} catch (FileNotFoundException e) {
			System.out.println("Could not find the master database file \"" + this.storageFileName + "\".\n"
					+ "This file is required for Rut Database Server to run. Exiting...");
			System.exit(1);
		}

		// memory.initDataMapChildLinks();

		return memory.getDataMap();
	}

	/**
	 * * Replaces the master.database file entirely. All nodes and node rules are
	 * presently written to this file.
	 * 
	 * @param dataMap
	 */
	public void writeDataMapToDisk(ConcurrentHashMap<String, ConcurrentHashMap<String, Node>> dataMap) {

		MemoryStorage memory = new MemoryStorage(dataMap);
		String fileText = memory.dumpDataMap();

		try {

			BufferedWriter fileWriter = new BufferedWriter(new FileWriter(this.storageFileName));
			fileWriter.write(fileText);
			fileWriter.close();

		} catch (IOException e) {

			System.out.println("Could not write to master database file \"" + this.storageFileName + "\".\n"
					+ "Access to this file is required for Rut Database Server to run. Exiting...");
			System.exit(1);

		}
	}

}
