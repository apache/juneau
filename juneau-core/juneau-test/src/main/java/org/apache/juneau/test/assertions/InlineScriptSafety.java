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
package org.apache.juneau.test.assertions;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.opentest4j.*;

/**
 * Guards shipped resources against a literal closing script tag ({@code </script}, matched case-insensitively).
 *
 * <p>
 * Host pages may inline JavaScript or CSS resources into an HTML {@code <script>} block.  The HTML parser ends that
 * block at the first closing script tag it sees, even one inside a comment or string, which silently truncates the
 * rest of the resource.  Nothing fails loudly, so this check names each offending file and line instead.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Fail if any *.js or *.css file beside juneau-views.js contains a closing script tag.</jc>
 * 	InlineScriptSafety.<jsm>assertNoScriptCloseTag</jsm>(ViewsMixin.<jk>class</jk>, <js>"/org/apache/juneau/views/juneau-views.js"</js>, <js>".js"</js>, <js>".css"</js>);
 * </p>
 */
public final class InlineScriptSafety {

	private static final Pattern CLOSING_SCRIPT = Pattern.compile("</script", Pattern.CASE_INSENSITIVE);

	private InlineScriptSafety() {}

	/**
	 * Asserts that no resource beside the specified anchor resource contains a closing script tag.
	 *
	 * <p>
	 * The directory is located through the classpath, so the resources must be on the file system (not in a jar).
	 *
	 * @param anchor Class used to resolve <c>resource</c>.
	 * @param resource Resource path, absolute or relative to <c>anchor</c>'s package, of a file in the directory to scan.
	 * @param extensions File-name suffixes to scan (e.g. <js>".js"</js>).  Must not be empty.
	 * @throws IOException If the directory cannot be located or a file cannot be read.
	 * @throws AssertionFailedError If no matching files were found, or any contains a closing script tag.
	 */
	public static void assertNoScriptCloseTag(Class<?> anchor, String resource, String... extensions) throws IOException {
		var files = listResources(anchor, resource, extensions);
		if (files.isEmpty())
			throw new AssertionFailedError(String.format("No resources matching '%s' found beside '%s'", String.join("', '", extensions), resource));
		var found = new ArrayList<String>();
		for (var p : files)
			found.addAll(findCloseTags(p.getFileName().toString(), new String(Files.readAllBytes(p), UTF_8)));
		if (! found.isEmpty())
			throw new AssertionFailedError(String.format("Closing script tag found at '%s'", String.join("', '", found)));
	}

	/**
	 * Lists the files in the directory containing the specified resource that end with one of the specified suffixes.
	 *
	 * @param anchor Class used to resolve <c>resource</c>.
	 * @param resource Resource path, absolute or relative to <c>anchor</c>'s package, of a file in the directory to scan.
	 * @param extensions File-name suffixes to include.
	 * @return The matching files, sorted by path.  Never <jk>null</jk>.
	 * @throws IOException If the resource does not exist, is not on the file system, or the directory cannot be listed.
	 */
	public static List<Path> listResources(Class<?> anchor, String resource, String... extensions) throws IOException {
		var url = anchor.getResource(resource);
		if (url == null)
			throw new FileNotFoundException(String.format("Resource not found: '%s'", resource));
		Path dir;
		try {
			dir = Path.of(url.toURI()).getParent();
		} catch (URISyntaxException | FileSystemNotFoundException e) {
			throw new IOException(String.format("Resource is not on the file system: '%s'", url), e);
		}
		try (var s = Files.list(dir)) {
			return s.filter(p -> {
				var name = p.getFileName().toString();
				return Arrays.stream(extensions).anyMatch(name::endsWith);
			}).sorted().toList();
		}
	}

	/**
	 * Finds each line of the specified text containing a closing script tag.
	 *
	 * @param name Name used to identify the text in the results (typically the file name).
	 * @param text The text to scan.
	 * @return A <c>name:line</c> entry (1-based) for each offending line.  Never <jk>null</jk>.
	 */
	public static List<String> findCloseTags(String name, String text) {
		var out = new ArrayList<String>();
		var lines = text.split("\n", -1);
		for (var i = 0; i < lines.length; i++)
			if (CLOSING_SCRIPT.matcher(lines[i]).find())
				out.add(name + ":" + (i + 1));
		return out;
	}
}
