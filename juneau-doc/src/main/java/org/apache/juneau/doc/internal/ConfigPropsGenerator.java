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
 * Utility for generating the configuration property glossary page.
 */
public class ConfigPropsGenerator {

	private static final Map<String,PropertyInfo> ENTRIES = new TreeMap<>();

	/**
	 * Entry point.
	 *
	 * @param args Not used
	 */
	public static void main(String[] args) {
		run(System.out);
	}

	/**
	 * Entry point.
	 *
	 * @param out Output.
	 */
	public static void run(PrintStream out) {
		try {
			out.println("<table class='styled w1000'>");
			out.println("\t<tr>");
			out.println("\t\t<th>Context</th><th>ID</th><th style='min-width:250px'>Description</th><th>Data type</th>");
			out.println("\t</tr>");
			for (String s : "juneau-core/juneau-config,juneau-core/juneau-dto,juneau-core/juneau-marshall,juneau-core/juneau-marshall-rdf,juneau-rest/juneau-rest-client,juneau-rest/juneau-rest-server".split(","))
				process(new File("../" + s + "/src/main/java"));
			String processing = "xxx";
			for (PropertyInfo pi : ENTRIES.values()) {
				if (pi.file.equals(processing))
					pi.context = "";
				processing = pi.file;
			}
			ENTRIES.values().stream().forEach(x->x.out(out));
			out.println("</table>");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Convert contents to a string.
	 *
	 * @return Output.
	 */
	public static String run() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		run(ps);
		ps.flush();
		try {
			return os.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return e.getLocalizedMessage();
		}
	}

	private static final String
		PROPERTY = "Configuration property:",
		ID = "<li><b>ID:</b>",
		NAME = "<li><b>Name:</b>",
		DATA_TYPE = "<li><b>Data type:</b>",
		SYSTEM_PROPERTY = "<li><b>System property:</b>",
		ENVIRONMENT_VARIABLE = "<li><b>Environment variable:</b>",
		DEFAULT = "<li><b>Default:</b>",
		METHODS = "<li><b>Methods:</b>",
		ANNOTATIONS = "<li><b>Annotations:</b>"

	;

	private static void process(File f) throws Exception {
		for (File f2 : f.listFiles()) {
			if (f2.isDirectory()) {
				process(f2);
			} else if (f2.isFile() && f2.getName().endsWith(".java")) {
				String contents = IOUtils.read(f2);
				if (contents.contains("<h5 class='section'>Property:</h5>")) {
					List<String> javadocs = IOUtils.findJavadocs(contents);
					for (String jd : javadocs) {
						if (jd.contains("<h5 class='section'>Property:</h5>")) {
							PropertyInfo pi = new PropertyInfo(f2);
							int x = 0;
							for (String line : jd.split("\n")) {
								if (line.contains(PROPERTY)) {
									pi.description(line.substring(line.indexOf(PROPERTY) + PROPERTY.length()).trim());
								} else {
									if (line.contains(ID)) {
										pi.id(line.substring(line.indexOf(ID) + ID.length()).trim());
										x = 1;
									} else if (line.contains(NAME)) {
										pi.name(line.substring(line.indexOf(NAME) + NAME.length()).trim());
										x = 2;
									} else if (line.contains(DATA_TYPE)) {
										pi.dataType(line.substring(line.indexOf(DATA_TYPE) + DATA_TYPE.length()).trim());
										x = 3;
									} else if (line.contains(SYSTEM_PROPERTY)) {
										pi.systemProperty(line.substring(line.indexOf(SYSTEM_PROPERTY) + SYSTEM_PROPERTY.length()).trim());
										x = 4;
									} else if (line.contains(ENVIRONMENT_VARIABLE)) {
										pi.envVar(line.substring(line.indexOf(ENVIRONMENT_VARIABLE) + ENVIRONMENT_VARIABLE.length()).trim());
										x = 5;
									} else if (line.contains(DEFAULT)) {
										pi.def(line.substring(line.indexOf(DEFAULT) + DEFAULT.length()).trim());
										x = 6;
									} else if (line.contains(METHODS)) {
										pi.def(line.substring(line.indexOf(METHODS) + METHODS.length()).trim());
										x = 7;
									} else if (line.contains(ANNOTATIONS)) {
										pi.def(line.substring(line.indexOf(ANNOTATIONS) + ANNOTATIONS.length()).trim());
										x = 8;
									} else if (line.contains("* </ul>") || line.contains("<li><b>")) {
										x = 0;
									} else if (x != 0) {
										if (x == 1)
											pi.id(line.substring(line.indexOf('*')+1).trim());
										else if (x == 2)
											pi.name(line.substring(line.indexOf('*')+1).trim());
										else if (x == 3)
											pi.dataType(line.substring(line.indexOf('*')+1).trim());
										else if (x == 4)
											pi.systemProperty(line.substring(line.indexOf('*')+1).trim());
										else if (x == 5)
											pi.envVar(line.substring(line.indexOf('*')+1).trim());
										else if (x == 6)
											pi.def(line.substring(line.indexOf('*')+1).trim());
										else if (x == 7)
											pi.methods(line.substring(line.indexOf('*')+1).trim());
										else if (x == 8)
											pi.annotations(line.substring(line.indexOf('*')+1).trim());
									}
								}
							}
							ENTRIES.put(f2.getName() + "/" + pi.name, pi);
/*
 * 	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li><c>List&lt;{@link org.apache.juneau.html.HtmlWidget}&gt;</c>
	 * 			<li><c>Class&lt;? <jk>extends</jk> {@link org.apache.juneau.html.HtmlWidget}&gt;&gt;</c>
	 * 		</ul>
	 * 	<li><b>Default:</b>  empty list

 */

						}
					}
				}
			}
		}
	}

	static class PropertyInfo {
		String file="", context="", id="", name="", description="", dataType="", systemProperty="", envVar="", def="", methods="", annotations="";

		public PropertyInfo(File f) {
			String path = f.getAbsolutePath();
			path = path.substring(path.indexOf("org/apache")).replace(".java","").replace('/','.');
			context = "{@link " + path + "}";
			file = f.getName();
		}

		public void id(String s) {
			this.id += s;
		}
		public void name(String s) {
			this.name += s;
		}
		public void description(String s) {
			this.description += s;
		}
		public void dataType(String s) {
			this.dataType += s;
		}
		public void systemProperty(String s) {
			this.systemProperty += s;
		}
		public void envVar(String s) {
			this.envVar += s;
		}
		public void def(String s) {
			this.def += s;
		}
		public void methods(String s) {
			this.methods += s;
		}
		public void annotations(String s) {
			this.annotations += s;
		}
		public void out(PrintStream out) {
			out.println("\t<tr>");
			out.println("\t\t<td>" + context + "</td>");
			out.println("\t\t<td>" + id + "</td>");
			out.println("\t\t<td>" + description + "</td>");
			out.println("\t\t<td style='max-width:250px;overflow:hidden'>" + dataType + "</td>");
			out.println("\t</tr>");
		}
	}
}
