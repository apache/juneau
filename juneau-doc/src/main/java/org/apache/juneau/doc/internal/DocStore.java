// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.doc.internal;

import java.io.*;
import java.util.*;

/**
 * Implements a lookup table for document keys and links stored in docs.txt
 */
@SuppressWarnings("javadoc")
public class DocStore {

	private Map<String,Link> mappings = new TreeMap<>();

	public DocStore(File f) {
		try (Scanner s = new Scanner(f)) {
			while (s.hasNextLine()) {
				String line = s.nextLine();
				int i = line.indexOf('=');
				if (i != -1)
					mappings.put(line.substring(0, i).trim(), new Link(line.substring(i+1)));
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void addLink(String key, String href, String text) {
		if (mappings.containsKey(key)) {
			mappings.remove(key);
			Console.warning(null, "Duplicate @doc tag: {0}", key);
			return;
		}
		mappings.put(key, new Link(href, text));
	}

	public Link getLink(String key) {
		return mappings.get(key);
	}

	public void save(File f) {
		try (FileWriter fw = new FileWriter(f)) {
			for (Map.Entry<String,Link> e : mappings.entrySet()) {
				Link l = e.getValue();
				fw.append(e.getKey()).append(" = ").append(l.href).append(", ").append(l.label).append('\n');
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A single line in the docs.txt file.
	 */
	public static class Link {
		public String href;
		public String label;

		public Link(String s) {
			int i = s.indexOf(',');
			if (i != -1) {
				href = s.substring(0, i).trim();
				label = s.substring(i+1).trim();
			} else {
				href = s;
				i = s.lastIndexOf('/');
				label = s.substring(i+1).trim();
			}
		}

		public Link(String href, String text) {
			this.href = href;
			this.label = text;
		}
	}
}
