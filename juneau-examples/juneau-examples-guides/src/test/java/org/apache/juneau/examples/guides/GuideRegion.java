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
package org.apache.juneau.examples.guides;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Reads a named region out of a guide resource file, normalized the same way the docs checker normalizes fences.
 *
 * <p>
 * A guide page shows a resource file (expected output, a sample POM) through a fence such as
 * <c>```text guide=SpringBootRestApi/run-output.txt#get-pet</c>.  The docs build checks that the fence equals the
 * region; the guide's test calls {@link #read(String, String)} to check that the region equals what the code really
 * produces.  Together they keep the page, the resource and the code in step.
 *
 * <p>
 * A region is the block between <c>region NAME</c> and <c>endregion NAME</c> marker lines, written as a
 * <c>//</c>, <c>#</c> or <c>&lt;!-- --&gt;</c> comment.  Normalization: CRLF to LF, tabs expanded to four-column stops, common
 * leading indentation removed, trailing whitespace stripped from every line, leading and trailing blank lines
 * dropped.
 */
public final class GuideRegion {

	private static final String MARKER = "^\\s*(?://|#|<!--)\\s*%s\\s+(%s)\\s*(?:-->)?\\s*$";

	private GuideRegion() {}

	/**
	 * Returns the normalized body of a region in a classpath resource.
	 *
	 * @param resourcePath The resource path relative to the classpath root (for example
	 * 	<js>"SpringBootRestApi/run-output.txt"</js>).
	 * @param region The region name.
	 * @return The normalized region body, lines joined with <c>\n</c>.
	 * @throws IllegalArgumentException If the resource or the region is missing, or the region is unterminated.
	 */
	public static String read(String resourcePath, String region) {
		var open = Pattern.compile(String.format(MARKER, "region", Pattern.quote(region)));
		var close = Pattern.compile(String.format(MARKER, "endregion", Pattern.quote(region)));
		var lines = readLines(resourcePath);
		var body = new ArrayList<String>();
		var inside = false;
		for (var line : lines) {
			if (! inside) {
				inside = open.matcher(line).matches();
			} else if (close.matcher(line).matches()) {
				return normalize(body);
			} else {
				body.add(line);
			}
		}
		throw new IllegalArgumentException((inside ? "Region unterminated: " : "Region not found: ") + resourcePath + "#" + region);
	}

	/**
	 * Normalizes text the same way {@link #read(String, String)} normalizes a region body.
	 *
	 * <p>
	 * Tests use this on captured program output before comparing it with a region.
	 *
	 * @param text The text to normalize.
	 * @return The normalized text, lines joined with <c>\n</c>.
	 */
	public static String normalize(String text) {
		return normalize(Arrays.asList(text.replace("\r\n", "\n").split("\n", -1)));
	}

	private static List<String> readLines(String resourcePath) {
		try (var in = GuideRegion.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (in == null)
				throw new IllegalArgumentException("Resource not found: " + resourcePath);
			return Arrays.asList(new String(in.readAllBytes(), UTF_8).replace("\r\n", "\n").split("\n", -1));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// Same as Python's str.expandtabs(4): a tab advances to the next multiple of four columns.
	private static String expandTabs(String line) {
		var sb = new StringBuilder();
		for (var c : line.toCharArray()) {
			if (c == '\t') {
				do {
					sb.append(' ');
				} while (sb.length() % 4 != 0);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static String normalize(List<String> body) {
		var lines = new ArrayList<String>();
		for (var line : body)
			lines.add(expandTabs(line).stripTrailing());
		while (! lines.isEmpty() && lines.get(0).isEmpty())
			lines.remove(0);
		while (! lines.isEmpty() && lines.get(lines.size() - 1).isEmpty())
			lines.remove(lines.size() - 1);
		var indent = lines.stream().filter(x -> ! x.isEmpty()).mapToInt(x -> x.length() - x.stripLeading().length()).min().orElse(0);
		var sb = new StringBuilder();
		for (var line : lines) {
			if (! sb.isEmpty())
				sb.append('\n');
			sb.append(line.isEmpty() ? line : line.substring(indent));
		}
		return sb.toString();
	}
}
