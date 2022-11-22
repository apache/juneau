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

import static org.apache.juneau.doc.internal.Console.*;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.*;

/**
 * Javadoc link checker.
 *
 * <p>
 * Runs against the generated javadocs folder looking for any broken internal links (missing files, invalid anchor tags, etc...).
 */
public class DocLinkTester {

	private static Map<String,Set<String>> ANCHORS = new LinkedHashMap<>();
	private static Pattern p = Pattern.compile("(href|src)\\=['\\\"]([^'\\\"]+)['\\\"]");
	private static Pattern p2 = Pattern.compile("(name|id)\\=['\\\"]([^'\\\"]+)['\\\"]");
	private static int errors, files, directories, links;
	private static Map<String,File> docFiles = new HashMap<>();


	/**
	 * Entry point.
	 *
	 * @param args Not used
	 */
	public static void main(String[] args) {
		try {
			long startTime = System.currentTimeMillis();
			File root = new File("../target/site/apidocs").getCanonicalFile();
			for (File fc : new File(root, "doc-files").listFiles())
				docFiles.put(fc.getAbsolutePath(), fc);
			info("Checking links in {0}", root);
			process(root);
			for (String df : new TreeMap<>(docFiles).keySet())
				error(docFiles.get(df), "unused");
			info("Checked {0} links in {1} files in {2} directories in {3}ms", links, files, directories, System.currentTimeMillis()-startTime);
			if (errors == 0)
				info("No link errors");
			else {
				Console.error(errors + " errors");  // NOT DEBUG
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void process(File dir) throws Exception {
		if (dir.isDirectory()) {
			for (File fc : dir.listFiles()) {
				if (fc.isFile() && fc.getName().endsWith(".html")) {
					files++;
					resolveLinks(fc);
				}
			}
			for (File fc : dir.listFiles()) {
				if (fc.isDirectory() && ! fc.getName().equals("src-html")) {
					directories++;
					process(fc);
				}
			}
		}
	}

	private static boolean hasAnchor(File f, String anchor) throws Exception {
		String key = f.getCanonicalPath();
		if (! ANCHORS.containsKey(key)) {
			Set<String> s = new HashSet<>();
			String c2 = IOUtils.read(f);
			Matcher m2 = p2.matcher(c2);
			while (m2.find()) {
				s.add(m2.group(2));
			}
			ANCHORS.put(key, s);
		}
		return ANCHORS.get(key).contains(anchor);
	}

	private static void resolveLinks(File f) throws Exception {
		String contents = IOUtils.read(f);
		Matcher m = p.matcher(contents);
		while (m.find()) {
			String link = m.group(2);
			String anchor = null;
			if (link.startsWith("https://") || link.startsWith("http://") || link.startsWith("mailto:") || link.startsWith("javascript:") || link.startsWith("$") || link.startsWith("{OVERVIEW_URL}"))
				continue;
			links++;
			if (link.indexOf('?') != -1)
				link = link.substring(0, link.indexOf('?'));

			if (link.indexOf('#') != -1) {
				anchor = link.substring(link.lastIndexOf('#')+1);
				link = link.substring(0, link.lastIndexOf('#'));
			}
			File f2 = link.isEmpty() ? f : new File(f.getParentFile().getAbsolutePath() + "/" + link);
			if (! f2.exists()) {
				error(f, "missingLink=["+link+"]");
			} else if (anchor != null) {
				anchor = URLDecoder.decode(anchor, "UTF-8").replace("<","&lt;").replace(">","&gt;");
				if (f2.isFile()) {
					boolean foundAnchor = hasAnchor(f2, anchor);
					if (! foundAnchor)
						error(f, "missingAnchor=["+link+"#"+anchor+"]");
				} else {
					error(f, "invalidAnchor=["+link+"#"+anchor+"]");
				}
			} else {
				docFiles.remove(f2.getAbsolutePath());
			}
		}
	}

	private static void error(File f, String msg) {
		errors++;
		Console.error("{0}: {1}", f.getAbsolutePath(), msg);
	}
}
