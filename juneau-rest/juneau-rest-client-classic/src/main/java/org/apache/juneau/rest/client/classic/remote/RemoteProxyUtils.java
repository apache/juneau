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
package org.apache.juneau.rest.client.classic.remote;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.juneau.commons.svl.*;

/**
 * Shared helpers for the classic REST-proxy engine.
 *
 * <p>
 * The parse/apply logic here is ported from the next-generation engine
 * (<c>org.apache.juneau.rest.client.remote.RemoteClient</c> and <c>RrpcInterfaceMeta</c>) so that the classic
 * engine can honor the same {@link org.apache.juneau.http.remote.Remote @Remote}/{@link org.apache.juneau.http.remote.RemoteOp @RemoteOp}
 * members without introducing a cross-module dependency on freshly-installed common code.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxies">REST Proxy Basics</a>
 * </ul>
 */
public class RemoteProxyUtils {

	private RemoteProxyUtils() {}

	private static final Logger LOG = Logger.getLogger("org.apache.juneau.rest.client.classic.remote");

	/** Interfaces already warned about an engine-specific member, to keep the build-time warning one-time. */
	private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

	/**
	 * Parses an array of constant {@code "name<delim>value"} strings into resolved name/value entries.
	 *
	 * <p>
	 * Ported from the next-generation engine's {@code RrpcInterfaceMeta.parseConstantParts(...)}.  Each entry is
	 * resolved through {@link VarResolver#DEFAULT} (so values such as {@code "$S{sysprop}"} expand), then split on the
	 * first occurrence of {@code delim} ({@code ':'} for the {@code "Name: value"} header form, {@code '='} for the
	 * {@code "name=value"} query/form-data form).  Entries with no delimiter are skipped.  Both the name and the value
	 * are trimmed.
	 *
	 * @param entries The raw annotation strings (may be empty).
	 * @param delim The name/value delimiter character.
	 * @return An unmodifiable list of resolved name/value entries. Never <jk>null</jk>, but may be empty.
	 */
	public static List<Map.Entry<String,String>> parseConstantParts(String[] entries, char delim) {
		if (entries == null || entries.length == 0)
			return List.of();
		var l = new ArrayList<Map.Entry<String,String>>();
		for (var e : entries) {
			var s = VarResolver.DEFAULT.resolve(e);
			var i = s.indexOf(delim);
			if (i != -1)
				l.add(Map.entry(s.substring(0, i).trim(), s.substring(i + 1).trim()));
		}
		return Collections.unmodifiableList(l);
	}

	/**
	 * Merges interface-level and method-level constant parts into a single name-keyed map, with method-level entries
	 * overriding interface-level entries of the same name (declaration order preserved).
	 *
	 * <p>
	 * Ported from the next-generation engine's {@code RemoteInvocationHandler.applyConstants(...)}.
	 *
	 * @param interfaceLevel Interface-level constants (applied first; lower precedence).
	 * @param methodLevel Method-level constants (override interface-level entries of the same name).
	 * @return An ordered, name-keyed map of merged constants. Never <jk>null</jk>, but may be empty.
	 */
	public static Map<String,String> mergeConstants(List<Map.Entry<String,String>> interfaceLevel, List<Map.Entry<String,String>> methodLevel) {
		var merged = new LinkedHashMap<String,String>();
		for (var e : interfaceLevel)
			merged.put(e.getKey(), e.getValue());
		for (var e : methodLevel)
			merged.put(e.getKey(), e.getValue());
		return merged;
	}

	/**
	 * Combines a base URL with a method path, avoiding double slashes.
	 *
	 * <p>
	 * Ported verbatim from the next-generation engine's {@code RemoteInvocationHandler.combinePaths(...)}.
	 *
	 * @param base The base URL/path.
	 * @param method The method path.
	 * @return The combined path.
	 */
	public static String combinePaths(String base, String method) {
		if (base.isEmpty())
			return method.isEmpty() ? "" : method;
		if (method.isEmpty())
			return base;
		if (base.endsWith("/") && method.startsWith("/"))
			return base + method.substring(1);
		if (! base.endsWith("/") && ! method.startsWith("/"))
			return base + "/" + method;
		return base + method;
	}

	/**
	 * Enforces the SSRF guardrail: when {@code url} carries a URI scheme it must be {@code http} or {@code https};
	 * otherwise an {@link IllegalArgumentException} is thrown.  Scheme-less (relative) values pass through unchanged.
	 *
	 * <p>
	 * Ported from the next-generation engine's {@code RemoteInvocationHandler.requireHttpScheme(...)}.
	 *
	 * @param url The URL to validate.
	 * @return The unchanged URL.
	 */
	public static String requireHttpScheme(String url) {
		var scheme = schemeOf(url);
		if (! (scheme == null || scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
			throw new IllegalArgumentException("Unsupported URL scheme '" + scheme + "' in @Remote URL override; only http/https are allowed: " + url);
		return url;
	}

	/**
	 * Returns the URI scheme of a URL (the token before the first {@code :} when it precedes any {@code /}, {@code ?}
	 * or {@code #}), or <jk>null</jk> if the value has no scheme.
	 *
	 * <p>
	 * Ported verbatim from the next-generation engine's {@code RemoteInvocationHandler.schemeOf(...)}.
	 *
	 * @param url The URL to inspect.
	 * @return The scheme, or <jk>null</jk> if none.
	 */
	public static String schemeOf(String url) {
		if (url.isEmpty() || ! Character.isLetter(url.charAt(0)))
			return null;
		for (var i = 0; i < url.length(); i++) {
			var c = url.charAt(i);
			if (c == ':')
				return url.substring(0, i);
			if (c == '/' || c == '?' || c == '#')
				return null;
			if (! (Character.isLetterOrDigit(c) || c == '+' || c == '-' || c == '.'))
				return null;
		}
		return null;
	}

	/**
	 * Emits a one-time build-time warning that an {@link org.apache.juneau.http.remote.Remote @Remote}/{@link org.apache.juneau.http.remote.RemoteOp @RemoteOp}
	 * member is set on the classic engine that the classic engine cannot honor.
	 *
	 * <p>
	 * De-duplicated per interface-class + member name so it fires at most once for a given proxy interface.
	 *
	 * @param interfaceClass The proxy interface the member was declared on.
	 * @param member The unsupported member name (e.g. <js>"interceptors"</js>).
	 * @param detail A short explanation of why the member is not honored.
	 */
	public static void warnUnsupportedMember(Class<?> interfaceClass, String member, String detail) {
		var key = interfaceClass.getName() + "#" + member;
		if (WARNED.add(key))
			LOG.warning(() -> "@Remote/@RemoteOp member '" + member + "' set on " + interfaceClass.getName()
				+ " is not honored by the classic REST-proxy engine: " + detail
				+ "  Use the next-generation engine (RestClient.remote(...)) for this feature.");
	}
}
