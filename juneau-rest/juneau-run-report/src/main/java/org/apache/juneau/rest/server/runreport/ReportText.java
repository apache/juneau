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
package org.apache.juneau.rest.server.runreport;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Text helpers shared by the report readers.
 */
public final class ReportText {

	private static final Pattern ANSI = Pattern.compile("\u001b\\[[0-9;]*[A-Za-z]");

	private ReportText() {}

	/**
	 * Removes ANSI escape sequences (test runners colour their messages).
	 *
	 * @param s The text.  May be <jk>null</jk>.
	 * @return The text without escape sequences, or an empty string for <jk>null</jk>.
	 */
	public static String stripAnsi(String s) {
		return s == null ? "" : ANSI.matcher(s).replaceAll("");
	}

	/**
	 * Truncates a string to at most {@code max} chars, ending in an ellipsis when it was cut.
	 *
	 * @param s The text.  May be <jk>null</jk>.
	 * @param max The maximum length, at least 1.
	 * @return The clipped text, or an empty string for <jk>null</jk>.
	 */
	public static String clip(String s, int max) {
		if (s == null)
			return "";
		return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
	}

	/**
	 * Returns the file name of a path, never the directories (warnings leave the machine).
	 *
	 * @param path The path.
	 * @return The last segment, or an empty string.
	 */
	public static String fileName(Path path) {
		var n = path == null ? null : path.getFileName();
		return n == null ? "" : n.toString();
	}

	/**
	 * Shortens a list of test file paths for display.
	 *
	 * <p>
	 * Backslashes become slashes, then the longest common <i>directory</i> prefix of all paths is removed, so
	 * {@code /ci/work/repo/src/a.test.js} and {@code /ci/work/repo/src/b/c.test.js} become {@code a.test.js} and
	 * {@code b/c.test.js}.  A list with a single path keeps its last two segments ({@code src/a.test.js}).
	 *
	 * @param paths The paths.
	 * @return The display paths, in the same order.
	 */
	public static List<String> stripCommonDir(List<String> paths) {
		var segs = new ArrayList<String[]>(paths.size());
		for (var p : paths)
			segs.add(p.replace('\\', '/').split("/", -1));
		var out = new ArrayList<String>(paths.size());
		if (segs.isEmpty())
			return out;
		if (segs.size() == 1) {
			var s = segs.get(0);
			out.add(String.join("/", Arrays.copyOfRange(s, Math.max(0, s.length - 2), s.length)));
			return out;
		}
		var common = Integer.MAX_VALUE;
		for (var s : segs)
			common = Math.min(common, s.length - 1);
		var first = segs.get(0);
		var keep = 0;
		while (keep < common && allEqual(segs, first[keep], keep))
			keep++;
		for (var s : segs)
			out.add(String.join("/", Arrays.copyOfRange(s, keep, s.length)));
		return out;
	}

	private static boolean allEqual(List<String[]> segs, String value, int index) {
		for (var s : segs)
			if (! s[index].equals(value))
				return false;
		return true;
	}
}
