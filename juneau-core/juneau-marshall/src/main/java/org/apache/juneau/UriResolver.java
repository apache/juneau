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
package org.apache.juneau;

import static org.apache.juneau.UriRelativity.*;
import static org.apache.juneau.UriResolution.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.net.*;

/**
 * Class used to create absolute and root-relative URIs based on your current URI 'location' and rules about how to
 * make such resolutions.
 *
 * <p>
 * Combines a {@link UriContext} instance with rules for resolution ({@link UriResolution} and relativity
 * ({@link UriRelativity}) to define simple {@link #resolve(Object)} and {@link #append(Appendable, Object)} methods.
 *
 * <p>
 * Three special protocols are used to represent context-root-relative, servlet-relative, and request-path-relative
 * URIs:
 * 	<js>"context:"</js>, <js>"servlet:"</js>, and <js>"request:"</js>.
 *
 * <p>
 * The following list shows the protocols of URLs that can be resolved with this class:
 * <ul>
 * 	<li><js>"foo://foo"</js> - Absolute URI.
 * 	<li><js>"/foo"</js> - Root-relative URI.
 * 	<li><js>"/"</js> - Root URI.
 * 	<li><js>"context:/foo"</js> - Context-root-relative URI with path.
 * 	<li><js>"context:/"</js> - Context-root URI.
 * 	<li><js>"context:?foo=bar"</js> - Context-root URI with query string.
 * 	<li><js>"servlet:/foo"</js> - Servlet-path-relative URI with path.
 * 	<li><js>"servlet:/"</js> - Servlet-path URI.
 * 	<li><js>"servlet:?foo=bar"</js> - Servlet-path URI with query string.
 * 	<li><js>"request:/foo"</js> - Request-path-relative URI with path.
 * 	<li><js>"request:/"</js> - Request-path URI.
 * 	<li><js>"request:?foo=bar"</js> - Request-path URI with query string.
 * 	<li><js>"foo"</js> - Path-info-relative URI.
 * 	<li><js>""</js> - Path-info URI.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class UriResolver {

	/**
	 * Static creator.
	 *
	 * @param resolution Rule on how URIs should be resolved.
	 * @param relativity Rule on what relative URIs are relative to.
	 * @param uriContext Current URI context (i.e. the current URI 'location').
	 * @return A new {@link UriResolver} object.
	 */
	public static UriResolver of(UriResolution resolution, UriRelativity relativity, UriContext uriContext) {
		return new UriResolver(resolution, relativity, uriContext);
	}

	private static boolean hasDotSegments(String s) {
		if (s == null)
			return false;
		for (var i = 0; i < s.length() - 1; i++) {
			var c = s.charAt(i);
			if ((i == 0 && c == '/') || (c == '/' && s.charAt(i + 1) == '.'))
				return true;
		}
		return false;
	}

	private static boolean isSpecialUri(String s) {
		if (s == null || s.isEmpty())
			return false;
		var c = s.charAt(0);
		if (c != 's' && c != 'c' && c != 'r')
			return false;
		return s.startsWith("servlet:") || s.startsWith("context:") || s.startsWith("request:");
	}

	private static String normalize(String s) {
		s = URI.create(s).normalize().toString();
		if (s.length() > 1 && s.charAt(s.length() - 1) == '/')
			s = s.substring(0, s.length() - 1);
		return s;
	}

	private final UriResolution resolution;

	private final UriRelativity relativity;

	private final String authority, contextRoot, servletPath, pathInfo, parentPath;

	/**
	 * Constructor.
	 *
	 * @param resolution Rule on how URIs should be resolved.
	 * @param relativity Rule on what relative URIs are relative to.
	 * @param uriContext Current URI context (i.e. the current URI 'location').
	 */
	public UriResolver(UriResolution resolution, UriRelativity relativity, UriContext uriContext) {
		this.resolution = resolution;
		this.relativity = relativity;
		this.authority = uriContext.authority;
		this.contextRoot = uriContext.contextRoot;
		this.servletPath = uriContext.servletPath;
		this.pathInfo = uriContext.pathInfo;
		this.parentPath = uriContext.parentPath;
	}

	/**
	 * Same as {@link #resolve(Object)} except appends result to the specified appendable.
	 *
	 * @param a The appendable to append the URL to.
	 * @param o The URI to convert to absolute form.
	 * @return The same appendable passed in.
	 */
	public Appendable append(Appendable a, Object o) {

	try {
		var uri = s(o);
		uri = nullIfEmpty(uri);
		var needsNormalize = hasDotSegments(uri) && nn(resolution);

			// Absolute paths are not changed.
			if (isAbsoluteUri(uri))
				return a.append(needsNormalize ? normalize(uri) : uri);
			if (resolution == NONE && ! isSpecialUri(uri))
				return a.append(emptyIfNull(uri));
			if (resolution == ROOT_RELATIVE && startsWith(uri, '/'))
				return a.append(needsNormalize ? normalize(uri) : uri);

			var a2 = needsNormalize ? new StringBuilder() : a;

			// Root-relative path
			if (startsWith(uri, '/')) {
				if (nn(authority))
					a2.append(authority);
				if (uri.length() != 1)
					a2.append(uri);
				else if (authority == null)
					a2.append('/');
			}

			// Context-relative path
			else if (nn(uri) && uri.startsWith("context:")) {
				if (resolution == ABSOLUTE && nn(authority))
					a2.append(authority);
				var hasContext = nn(contextRoot) && ! contextRoot.isEmpty();
				if (hasContext)
					a2.append('/').append(contextRoot);
				if (uri.length() > 8) {
					var remainder = uri.substring(8);
					// Skip if remainder is just "/" and something was appended OR we're at authority level with nothing else
					if (remainder.equals("/") && (hasContext || (resolution == ABSOLUTE && nn(authority)))) {
						// Do nothing
					} else if (! remainder.isEmpty() && remainder.charAt(0) != '/' && remainder.charAt(0) != '?' && remainder.charAt(0) != '#') {
						a2.append('/').append(remainder);
					} else {
						a2.append(remainder);
					}
				} else if (! hasContext && (authority == null || resolution != ABSOLUTE))
					a2.append('/');
			}

			// Resource-relative path
			else if (nn(uri) && uri.startsWith("servlet:")) {
				if (resolution == ABSOLUTE && nn(authority))
					a2.append(authority);
				var hasContext = nn(contextRoot) && ! contextRoot.isEmpty();
				var hasServlet = nn(servletPath) && ! servletPath.isEmpty();
				if (hasContext)
					a2.append('/').append(contextRoot);
				if (hasServlet)
					a2.append('/').append(servletPath);
				if (uri.length() > 8) {
					var remainder = uri.substring(8);
					// Skip if remainder is just "/" and something was appended OR we're at authority level with nothing else
					if (remainder.equals("/") && (hasContext || hasServlet || (resolution == ABSOLUTE && nn(authority)))) {
						// Do nothing
					} else if (! remainder.isEmpty() && remainder.charAt(0) != '/' && remainder.charAt(0) != '?' && remainder.charAt(0) != '#') {
						a2.append('/').append(remainder);
					} else {
						a2.append(remainder);
					}
				} else if (! hasServlet && ! hasContext && (authority == null || resolution != ABSOLUTE))
					a2.append('/');
			}

			// Request-relative path
			else if (nn(uri) && uri.startsWith("request:")) {
				if (resolution == ABSOLUTE && nn(authority))
					a2.append(authority);
				var hasContext = nn(contextRoot) && ! contextRoot.isEmpty();
				var hasServlet = nn(servletPath) && ! servletPath.isEmpty();
				var hasPath = nn(pathInfo) && ! pathInfo.isEmpty();
				if (hasContext)
					a2.append('/').append(contextRoot);
				if (hasServlet)
					a2.append('/').append(servletPath);
				if (hasPath)
					a2.append('/').append(pathInfo);
				if (uri.length() > 8) {
					var remainder = uri.substring(8);
					// Skip if remainder is just "/" and something was appended OR we're at authority level with nothing else
					if (remainder.equals("/") && (hasContext || hasServlet || hasPath || (resolution == ABSOLUTE && nn(authority)))) {
						// Do nothing
					} else if (! remainder.isEmpty() && remainder.charAt(0) != '/' && remainder.charAt(0) != '?' && remainder.charAt(0) != '#') {
						a2.append('/').append(remainder);
					} else {
						a2.append(remainder);
					}
				} else if (! hasServlet && ! hasContext && ! hasPath && (authority == null || resolution != ABSOLUTE))
					a2.append('/');
			}

			// Relative path
			else {
				if (resolution == ABSOLUTE && nn(authority))
					a2.append(authority);
				if (nn(contextRoot))
					a2.append('/').append(contextRoot);
				if (nn(servletPath))
					a2.append('/').append(servletPath);
				if (relativity == RESOURCE && nn(uri))
					a2.append('/').append(uri);
				else if (relativity == PATH_INFO) {
					if (uri == null) {
						if (nn(pathInfo))
							a2.append('/').append(pathInfo);
					} else {
						if (nn(parentPath))
							a2.append('/').append(parentPath);
						a2.append('/').append(uri);
					}
				} else if (uri == null && contextRoot == null && servletPath == null && (authority == null || resolution != ABSOLUTE))
					a2.append('/');
			}

			if (needsNormalize)
				a.append(normalize(a2.toString()));

			return a;
		} catch (IOException e) {
			throw toRex(e);
		}
	}

	/**
	 * Relativizes a URI.
	 *
	 * <p>
	 * Similar to {@link URI#relativize(URI)}, except supports special protocols (e.g. <js>"servlet:/"</js>) for both
	 * the <c>relativeTo</c> and <c>uri</c> parameters.
	 *
	 * <p>
	 * For example, to relativize a URI to its servlet-relative form:
	 * <p class='bjava'>
	 * 	<jc>// relativeUri == "path/foo"</jc>
	 * 	String <jv>relativeUri</jv> = <jv>resolver</jv>.relativize(<js>"servlet:/"</js>, <js>"/context/servlet/path/foo"</js>);
	 * </p>
	 *
	 * @param relativeTo The URI to relativize against.
	 * @param uri The URI to relativize.
	 * @return The relativized URI.
	 */
	public String relativize(Object relativeTo, Object uri) {
		var r = resolve(relativeTo, ABSOLUTE);
		var s = resolve(uri, ABSOLUTE);
		return URI.create(r).relativize(URI.create(s)).toString();
	}

	/**
	 * Converts the specified URI to absolute form based on values in this context.
	 *
	 * @param uri
	 * 	The URI to convert to absolute form.
	 * 	Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link java.net.URI}
	 * 		<li>{@link java.net.URL}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	URI can be any of the following forms:
	 * 	<ul>
	 * 		<li><js>"foo://foo"</js> - Absolute URI.
	 * 		<li><js>"/foo"</js> - Root-relative URI.
	 * 		<li><js>"/"</js> - Root URI.
	 * 		<li><js>"context:/foo"</js> - Context-root-relative URI with path.
	 * 		<li><js>"context:/"</js> - Context-root URI.
	 * 		<li><js>"context:?foo=bar"</js> - Context-root URI with query string.
	 * 		<li><js>"servlet:/foo"</js> - Servlet-path-relative URI with path.
	 * 		<li><js>"servlet:/"</js> - Servlet-path URI.
	 * 		<li><js>"servlet:?foo=bar"</js> - Servlet-path URI with query string.
	 * 		<li><js>"request:/foo"</js> - Request-path-relative URI with path.
	 * 		<li><js>"request:/"</js> - Request-path URI.
	 * 		<li><js>"request:?foo=bar"</js> - Request-path URI with query string.
	 * 		<li><js>"foo"</js> - Path-info-relative URI.
	 * 		<li><js>""</js> - Path-info URI.
	 * 	</ul>
	 * @return The converted URI.
	 */
	public String resolve(Object uri) {
		return resolve(uri, resolution);
	}

	private String resolve(Object uri, UriResolution res) {
		var s = s(uri);
		if (isAbsoluteUri(s))
			return hasDotSegments(s) && res != NONE ? normalize(s) : s;
		if (res == ROOT_RELATIVE && startsWith(s, '/'))
			return hasDotSegments(s) ? normalize(s) : s;
		if (res == NONE && ! isSpecialUri(s))
			return s;
		return append(new StringBuilder(), s).toString();
	}
}