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
package org.apache.juneau.rest.server.views;

import java.util.regex.*;

/**
 * Shared grammars for console-output data: colours, line hrefs, image sources, icon names, tokens.
 *
 * <p>
 * The JavaScript module {@code juneau-console-output.js} implements the same rules, and both are pinned by the
 * shared test vector file {@code console-output-vectors.json}.
 */
final class ConsoleOutputChecks {

	static final Pattern HEX_COLOR = Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
	static final Pattern RGB_COLOR = Pattern.compile("^rgb\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)$");
	static final Pattern FRAGMENT_HREF = Pattern.compile("^#[A-Za-z0-9._:~-]{0,128}$");
	static final Pattern ICON_NAME = Pattern.compile("^[a-z][A-Za-z0-9.-]{0,63}$");
	static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9._~-]{1,128}$");
	static final Pattern LOG_ID = Pattern.compile("^[A-Za-z0-9_-]{1,128}$");
	static final Pattern ANCHOR_PREFIX = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,31}$");

	private ConsoleOutputChecks() {}

	static boolean isSafeColor(String s) {
		if (s == null)
			return false;
		if (HEX_COLOR.matcher(s).matches())
			return true;
		var m = RGB_COLOR.matcher(s);
		if (! m.matches())
			return false;
		for (var i = 1; i <= 3; i++)
			if (Integer.parseInt(m.group(i)) > 255)
				return false;
		return true;
	}

	/**
	 * Whether a line href is safe: a bare fragment, or a path accepted by {@link RegionDef#isSafeDetailEndpoint(String)}.
	 *
	 * @param s The candidate href.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if the href is safe.
	 */
	static boolean isSafeLineHref(String s) {
		if (s == null)
			return false;
		var t = stripTabCrLf(s);
		if (t.startsWith("#"))
			return FRAGMENT_HREF.matcher(t).matches();
		return isSafePath(t);
	}

	static boolean isSafeLineImageSrc(String s) {
		if (s == null)
			return false;
		var t = stripTabCrLf(s);
		return ! t.startsWith("#") && isSafePath(t);
	}

	static boolean isIconName(String s) {
		return s != null && ICON_NAME.matcher(s).matches();
	}

	static boolean isToken(String s) {
		return s != null && TOKEN.matcher(s).matches();
	}

	static boolean isLogId(String s) {
		return s != null && LOG_ID.matcher(s).matches();
	}

	static boolean isAnchorPrefix(String s) {
		return s != null && ANCHOR_PREFIX.matcher(s).matches();
	}

	/** Truncates a value for an error message: 64 chars plus an ellipsis. */
	static String clip(String s) {
		if (s == null)
			return "null";
		return s.length() > 64 ? s.substring(0, 64) + "…" : s;
	}

	private static String stripTabCrLf(String s) {
		return s.replace("\t", "").replace("\r", "").replace("\n", "");
	}

	private static boolean isSafePath(String t) {
		return RegionDef.isSafeDetailEndpoint(t.replace('\\', '/'));
	}
}
