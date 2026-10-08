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
	 * anything that {@link ConsoleOutputChecks#isSafeLineHref(String)} accepts.  TAB, CR and LF are stripped first.
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
		return ConsoleOutputChecks.isSafeLineHref(t);
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
		return ConsoleOutputChecks.isSafeLineHref(s.replace(LINE_PLACEHOLDER, "1"));
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
