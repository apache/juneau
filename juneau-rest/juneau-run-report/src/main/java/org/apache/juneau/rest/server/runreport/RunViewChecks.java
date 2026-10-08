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

import java.net.*;
import java.util.regex.*;

/**
 * Shared validation grammar; not intended for application use.
 *
 * <p>
 * The run-view grammars live in one place so the server types, the report readers and the client module
 * (which mirrors them, pinned by a shared vectors file) cannot drift.
 */
public final class RunViewChecks {

	private static final Pattern RUN_ID = Pattern.compile("^[A-Za-z0-9_-]{1,128}$");
	private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9._~-]{1,128}$");
	private static final Pattern STEP_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");
	private static final Pattern FW = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,31}$");
	private static final Pattern TAB_CR_LF = Pattern.compile("[\t\r\n]");
	private static final Pattern FRAGMENT_HREF = Pattern.compile("^#[A-Za-z0-9._:~-]{0,128}$");

	/** The placeholder a raw-href template must contain exactly once. */
	public static final String LINE_PLACEHOLDER = "{line}";

	private RunViewChecks() {}

	/**
	 * Whether the string is a valid run id.
	 *
	 * @param s The candidate.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if valid.
	 */
	public static boolean isRunId(String s) {
		return s != null && RUN_ID.matcher(s).matches();
	}

	/**
	 * Whether the string is a valid opaque page token.
	 *
	 * @param s The candidate.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if valid.
	 */
	public static boolean isToken(String s) {
		return s != null && TOKEN.matcher(s).matches();
	}

	/**
	 * Whether the string is a valid step id.
	 *
	 * @param s The candidate.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if valid.
	 */
	public static boolean isStepId(String s) {
		return s != null && STEP_ID.matcher(s).matches();
	}

	/**
	 * Whether the string is a valid framework key.
	 *
	 * @param s The candidate.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if valid.
	 */
	public static boolean isFw(String s) {
		return s != null && FW.matcher(s).matches();
	}

	/**
	 * Whether the string is a safe note href.
	 *
	 * <p>
	 * Either an absolute URL whose scheme is exactly <c>http</c> or <c>https</c> and which has a non-empty host, or
	 * anything that {@link #isSafeLineHref(String)} accepts.  TAB, CR and LF are stripped first.
	 *
	 * @param s The candidate.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if safe.
	 */
	public static boolean isNoteHref(String s) {
		if (s == null)
			return false;
		var t = TAB_CR_LF.matcher(s).replaceAll("");
		if (t.isEmpty())
			return false;
		if (t.startsWith("http://") || t.startsWith("https://")) {
			if (t.contains("\\"))
				return false;
			try {
				var host = new URI(t).getHost();
				return host != null && ! host.isEmpty();
			} catch (URISyntaxException e) {
				return false;
			}
		}
		return isSafeLineHref(t);
	}

	/**
	 * Whether the string is a valid raw-href template.
	 *
	 * <p>
	 * It must contain exactly one <c>{line}</c> and, with <c>1</c> substituted, be a safe line href.
	 *
	 * @param s The candidate.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if valid.
	 */
	public static boolean isRawHrefTemplate(String s) {
		if (s == null)
			return false;
		var i = s.indexOf(LINE_PLACEHOLDER);
		if (i < 0 || s.indexOf(LINE_PLACEHOLDER, i + 1) >= 0)
			return false;
		return isSafeLineHref(s.replace(LINE_PLACEHOLDER, "1"));
	}

	/**
	 * Whether a line href is safe: a bare fragment, or a same-origin path accepted by
	 * {@link #isSameOriginPath(String)}.
	 *
	 * <p>
	 * TAB, CR and LF are stripped first and backslashes are treated as slashes.
	 *
	 * @param s The candidate href.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if the href is safe.
	 */
	public static boolean isSafeLineHref(String s) {
		if (s == null)
			return false;
		var t = TAB_CR_LF.matcher(s).replaceAll("");
		if (t.startsWith("#"))
			return FRAGMENT_HREF.matcher(t).matches();
		return isSameOriginPath(t.replace('\\', '/'));
	}

	/**
	 * Whether the string is a same-origin path template: no <c>://</c>, no <c>//</c> prefix, no scheme
	 * colon-before-slash, and no <c>..</c> path segments.
	 *
	 * @param path The candidate template.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a same-origin path template.
	 */
	public static boolean isSameOriginPath(String path) {
		if (path == null || path.isBlank())
			return false;
		if (path.contains("://"))
			return false;
		if (path.startsWith("//"))
			return false;
		var colon = path.indexOf(':');
		var slash = path.indexOf('/');
		if (colon >= 0 && (slash < 0 || colon < slash))
			return false;
		for (var seg : path.split("/", -1))
			if ("..".equals(seg))
				return false;
		return true;
	}

	/**
	 * Truncates a value for an error message: 64 characters plus an ellipsis.
	 *
	 * @param s The value.  May be <jk>null</jk>.
	 * @return The clipped text, or <js>"null"</js> for <jk>null</jk>.
	 */
	public static String clip(String s) {
		if (s == null)
			return "null";
		return s.length() > 64 ? s.substring(0, 64) + "\u2026" : s;
	}

	/**
	 * Returns the stripped first line of a string.
	 *
	 * @param s The string.  May be <jk>null</jk>.
	 * @return The first line, or an empty string for <jk>null</jk>.
	 */
	public static String firstLine(String s) {
		if (s == null)
			return "";
		var i = s.indexOf('\n');
		return (i < 0 ? s : s.substring(0, i)).strip();
	}
}
