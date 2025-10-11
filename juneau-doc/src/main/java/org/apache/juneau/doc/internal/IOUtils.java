/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.doc.internal;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Various I/O related utility methods.
 */
@SuppressWarnings("javadoc")
public class IOUtils {

	public static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, "UTF-8");
	}

	private static final Pattern JDOC_PATTERN = Pattern.compile("(?s)\\/\\*\\*.*?\\*\\/");

	public static List<String> findJavadocs(String contents) {
		List<String> l = new ArrayList<>();
		Matcher m = JDOC_PATTERN.matcher(contents);
		while (m.find()) {
			l.add(m.group());
		}
		return l;
	}

	public static String read(File f) throws IOException {
		return readFile(f.getAbsolutePath());
	}

	public static void writeFile(String path, String contents) throws IOException {
		Files.write(Paths.get(path), contents.getBytes("UTF-8"));
	}
}
