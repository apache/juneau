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
package org.apache.juneau.terminal.report;

import java.net.*;
import java.util.regex.*;

import org.apache.juneau.commons.http.*;

/**
 * Shared validation grammar; not intended for application use.
 *
 * <p>
 * The terminal-view grammars live in one place so the server types, the report readers and the client module
 * (which mirrors them, pinned by a shared vectors file) cannot drift.
 */
public final class TerminalViewChecks {

	private static final Pattern RUN_ID = Pattern.compile("^[A-Za-z0-9_-]{1,128}$");
	private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9._~-]{1,128}$");
	private static final Pattern STEP_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");
	private static final Pattern FW = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,31}$");
	private static final Pattern TAB_CR_LF = Pattern.compile("[\t\r\n]");
	private static final Pattern FRAGMENT_HREF = Pattern.compile("^#[A-Za-z0-9._:~-]{0,128}$");

	/** The byte-offset placeholder; a raw-href template contains it exactly once. */
	public static final String OFFSET_PLACEHOLDER = "{offset}";

	/** The refresh floor, in milliseconds, for terminal-view and terminal poll intervals. */
	public static final long MIN_REFRESH_MS = 1000;

	private TerminalViewChecks() {}

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
	 * It must contain exactly one <c>{offset}</c> placeholder and, with <c>1</c>
	 * substituted, be a safe line href.
	 *
	 * @param s The candidate.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if valid.
	 */
	public static boolean isRawHrefTemplate(String s) {
		if (s == null)
			return false;
		if (count(s, OFFSET_PLACEHOLDER) != 1)
			return false;
		return isSafeLineHref(s.replace(OFFSET_PLACEHOLDER, "1"));
	}

	private static int count(String s, String token) {
		var n = 0;
		for (var i = s.indexOf(token); i >= 0; i = s.indexOf(token, i + token.length()))
			n++;
		return n;
	}

	/**
	 * Whether a line href is safe: a bare fragment, or a same-origin path accepted by
	 * {@link #isSameOriginPath(String)}.
	 *
	 * <p>
	 * TAB, CR and LF are stripped first.  A backslash anywhere refuses the href.
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
		return isSameOriginPath(t);
	}

	/**
	 * Whether the string is a same-origin path template, by the one shared rule in
	 * {@link SafePathTemplate#isSameOriginPath(String)}: no <c>://</c>, no <c>//</c> prefix, no scheme
	 * colon-before-slash, no <c>..</c> path segment, and no backslash, control character, or leading or trailing space.
	 *
	 * @param path The candidate template.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a same-origin path template.
	 */
	public static boolean isSameOriginPath(String path) {
		return SafePathTemplate.isSameOriginPath(path);
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
