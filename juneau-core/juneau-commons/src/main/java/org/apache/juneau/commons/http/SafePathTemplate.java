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
package org.apache.juneau.commons.http;

/**
 * Shared, dependency-free predicates for validating <b>same-origin</b> request paths declared by higher layers, plus
 * the single {@link #MIN_POLL_INTERVAL_MS polling floor} those layers clamp declared poll intervals to.
 *
 * <p>
 * This is the toolkit's one home for "is this app-declared endpoint safe to fetch from the browser" so the widgets
 * and views modules can both <b>call</b> it without either depending on the other.  The same-origin core
 * ({@link #isSameOriginPath(String)}) rejects an absolute URL, a protocol-relative {@code //host} prefix, a
 * scheme (colon-before-slash), and any {@code ..} path segment &mdash; the base rule every declarative endpoint
 * shares.  {@link #isNonTemplatedPath(String)} adds the stricter "no {@code {…}} placeholder" rule an endpoint that
 * is <b>not</b> row/parameter-scoped requires (a card field-list, for example, is fixed &mdash; it is not fetched
 * per-row, so a {@code {id}} template is meaningless and rejected).  A path-templated caller (a calendar that
 * legitimately needs {@code {year}}/{@code {month}}) layers {@link #isSafeTemplate(String, String...)} on top of
 * {@link #isSameOriginPath(String)} rather than duplicating the same-origin core.  Document links (an event
 * {@code href}) use {@link #isSafeDocumentUrl(String)}, which is the same-origin core plus query/fragment.
 *
 * @since 10.0.0
 */
public final class SafePathTemplate {

	/**
	 * The minimum honored polling interval, in milliseconds.
	 *
	 * <p>
	 * A declared poll interval below this floor is clamped up to it rather than honored as configured &mdash; a
	 * declarable interval with no floor lets a consumer configure a self-inflicted load problem on a server-side
	 * endpoint.  This is the single, easily-tested source of truth for the floor shared by every polling widget
	 * (view tables, card field-lists, calendars), so a stale/cached client script cannot be tricked into honoring a
	 * sub-floor value the server never actually declared.
	 */
	public static final long MIN_POLL_INTERVAL_MS = 5_000L;

	private SafePathTemplate() {}

	/**
	 * Whether {@code path} is a same-origin path: not blank, no {@code ://}, no {@code //} prefix, no scheme
	 * (colon before the first slash), and no {@code ..} path segments.
	 *
	 * <p>
	 * This is the shared same-origin core.  It deliberately says nothing about {@code {…}} template placeholders:
	 * a per-row/per-parameter caller (which needs {@code {id}}) uses this as-is, and a caller that forbids
	 * templating layers {@link #isNonTemplatedPath(String)} on top.
	 *
	 * @param path The candidate path.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a same-origin path.
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
		for (var seg : path.split("/", -1)) {
			if ("..".equals(seg))
				return false;
		}
		return true;
	}

	/**
	 * Whether {@code path} is a same-origin path (per {@link #isSameOriginPath(String)}) that also carries <b>no</b>
	 * {@code {…}} template placeholder.
	 *
	 * <p>
	 * A fixed, non-parameterized endpoint (a card field-list refresh, for example) must be exactly the path it
	 * declares; a {@code {id}}-style placeholder has no substitution source and is rejected.
	 *
	 * @param path The candidate path.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a same-origin, non-templated path.
	 */
	public static boolean isNonTemplatedPath(String path) {
		return isSameOriginPath(path) && path.indexOf('{') < 0;
	}

	/**
	 * Clamps a declared poll interval up to {@link #MIN_POLL_INTERVAL_MS}.
	 *
	 * @param intervalMs The declared interval, in milliseconds.
	 * @return {@code max(intervalMs, }{@link #MIN_POLL_INTERVAL_MS}{@code )}.
	 */
	public static long clampPollInterval(long intervalMs) {
		return Math.max(intervalMs, MIN_POLL_INTERVAL_MS);
	}

	/**
	 * Whether {@code template} is a same-origin path template (per {@link #isSameOriginPath(String)}) that also
	 * contains every required substitution token.
	 *
	 * <p>
	 * A per-month calendar GET like {@code /events/{year}/{month}} is same-origin <em>and</em> must actually
	 * declare the tokens the emitter will substitute.  Built on {@link #isSameOriginPath(String)} rather than a
	 * second same-origin implementation.
	 *
	 * @param template The candidate template.  Can be <jk>null</jk>.
	 * @param requiredTokens The literal tokens (for example {@code "{year}"}, {@code "{month}"}) that must all be
	 * 	present.  May be empty (same-origin only).
	 * @return <jk>true</jk> if {@code template} is same-origin and contains every token.
	 */
	public static boolean isSafeTemplate(String template, String...requiredTokens) {
		if (!isSameOriginPath(template))
			return false;
		if (requiredTokens != null)
			for (var t : requiredTokens)
				if (t != null && !template.contains(t))
					return false;
		return true;
	}

	/**
	 * Whether {@code url} is a same-origin <b>document</b> URL (query and fragment allowed).
	 *
	 * <p>
	 * Mirrors the client-side {@code isSafeDetailUrl} check.  Query/hash never trip the absolute/scheme/{@code ..}
	 * rejections in {@link #isSameOriginPath(String)}; a {@code ?} value containing {@code ://} is still rejected.
	 *
	 * @param url The candidate document URL.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the URL is a safe same-origin document link.
	 */
	public static boolean isSafeDocumentUrl(String url) {
		return isSameOriginPath(url);
	}
}
