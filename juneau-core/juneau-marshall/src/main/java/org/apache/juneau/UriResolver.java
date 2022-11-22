// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.UriRelativity.*;
import static org.apache.juneau.UriResolution.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

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
 * 	<js>"context:/"</js>, <js>"servlet:/"</js>, and <js>"request:/"</js>.
 *
 * <p>
 * The following list shows the protocols of URLs that can be resolved with this class:
 * <ul>
 * 	<li><js>"foo://foo"</js> - Absolute URI.
 * 	<li><js>"/foo"</js> - Root-relative URI.
 * 	<li><js>"/"</js> - Root URI.
 * 	<li><js>"context:/foo"</js> - Context-root-relative URI.
 * 	<li><js>"context:/"</js> - Context-root URI.
 * 	<li><js>"servlet:/foo"</js> - Servlet-path-relative URI.
 * 	<li><js>"servlet:/"</js> - Servlet-path URI.
 * 	<li><js>"request:/foo"</js> - Request-path-relative URI.
 * 	<li><js>"request:/"</js> - Request-path URI.
 * 	<li><js>"foo"</js> - Path-info-relative URI.
 * 	<li><js>""</js> - Path-info URI.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class UriResolver {

	private final UriResolution resolution;
	private final UriRelativity relativity;
	private final String authority, contextRoot, servletPath, pathInfo, parentPath;

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
	 * 		<li><js>"context:/foo"</js> - Context-root-relative URI.
	 * 		<li><js>"context:/"</js> - Context-root URI.
	 * 		<li><js>"servlet:/foo"</js> - Servlet-path-relative URI.
	 * 		<li><js>"servlet:/"</js> - Servlet-path URI.
	 * 		<li><js>"request:/foo"</js> - Request-path-relative URI.
	 * 		<li><js>"request:/"</js> - Request-path URI.
	 * 		<li><js>"foo"</js> - Path-info-relative URI.
	 * 		<li><js>""</js> - Path-info URI.
	 * 	</ul>
	 * @return The converted URI.
	 */
	public String resolve(Object uri) {
		return resolve(uri, resolution);
	}

	private String resolve(Object uri, UriResolution res) {
		String s = stringify(uri);
		if (isAbsoluteUri(s))
			return hasDotSegments(s) && res != NONE ? normalize(s) : s;
		if (res == ROOT_RELATIVE && startsWith(s, '/'))
			return hasDotSegments(s) ? normalize(s) : s;
		if (res == NONE && ! isSpecialUri(s))
			return s;
		return append(new StringBuilder(), s).toString();
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
		String r = resolve(relativeTo, ABSOLUTE);
		String s = resolve(uri, ABSOLUTE);
		return URI.create(r).relativize(URI.create(s)).toString();
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
			String uri = stringify(o);
			uri = nullIfEmpty(uri);
			boolean needsNormalize = hasDotSegments(uri) && resolution != null;

			// Absolute paths are not changed.
			if (isAbsoluteUri(uri))
				return a.append(needsNormalize ? normalize(uri) : uri);
			if (resolution == NONE && ! isSpecialUri(uri))
				return a.append(emptyIfNull(uri));
			if (resolution == ROOT_RELATIVE && startsWith(uri, '/'))
				return a.append(needsNormalize ? normalize(uri) : uri);

			Appendable a2 = needsNormalize ? new StringBuilder() : a;

			// Root-relative path
			if (startsWith(uri, '/')) {
				if (authority != null)
					a2.append(authority);
				if (uri.length() != 1)
					a2.append(uri);
				else if (authority == null)
					a2.append('/');
			}

			// Context-relative path
			else if (uri != null && uri.startsWith("context:/")) {
				if (resolution == ABSOLUTE && authority != null)
					a2.append(authority);
				if (contextRoot != null)
					a2.append('/').append(contextRoot);
				if (uri.length() > 9)
					a2.append('/').append(uri.substring(9));
				else if (contextRoot == null && (authority == null || resolution != ABSOLUTE))
					a2.append('/');
			}

			// Resource-relative path
			else if (uri != null && uri.startsWith("servlet:/")) {
				if (resolution == ABSOLUTE && authority != null)
					a2.append(authority);
				if (contextRoot != null)
					a2.append('/').append(contextRoot);
				if (servletPath != null)
					a2.append('/').append(servletPath);
				if (uri.length() > 9)
					a2.append('/').append(uri.substring(9));
				else if (servletPath == null && contextRoot == null && (authority == null || resolution != ABSOLUTE))
					a2.append('/');
			}

			// Request-relative path
			else if (uri != null && uri.startsWith("request:/")) {
				if (resolution == ABSOLUTE && authority != null)
					a2.append(authority);
				if (contextRoot != null)
					a2.append('/').append(contextRoot);
				if (servletPath != null)
					a2.append('/').append(servletPath);
				if (pathInfo != null)
					a2.append('/').append(pathInfo);
				if (uri.length() > 9)
					a2.append('/').append(uri.substring(9));
				else if (servletPath == null && contextRoot == null && pathInfo == null && (authority == null || resolution != ABSOLUTE))
					a2.append('/');
			}

			// Relative path
			else {
				if (resolution == ABSOLUTE && authority != null)
					a2.append(authority);
				if (contextRoot != null)
					a2.append('/').append(contextRoot);
				if (servletPath != null)
					a2.append('/').append(servletPath);
				if (relativity == RESOURCE && uri != null)
					a2.append('/').append(uri);
				else if (relativity == PATH_INFO) {
					if (uri == null) {
						if (pathInfo != null)
							a2.append('/').append(pathInfo);
					} else {
						if (parentPath != null)
							a2.append('/').append(parentPath);
						a2.append('/').append(uri);
					}
				}
				else if (uri == null && contextRoot == null && servletPath == null && (authority == null || resolution != ABSOLUTE))
					a2.append('/');
			}

			if (needsNormalize)
				a.append(normalize(a2.toString()));

			return a;
		} catch (IOException e) {
			throw asRuntimeException(e);
		}
	}

	private static boolean isSpecialUri(String s) {
		if (s == null || s.length() == 0)
			return false;
		char c = s.charAt(0);
		if (c != 's' && c != 'c' && c != 'r')
			return false;
		return s.startsWith("servlet:/") || s.startsWith("context:/") || s.startsWith("request:/");
	}

	private static String normalize(String s) {
		s = URI.create(s).normalize().toString();
		if (s.length() > 1 && s.charAt(s.length()-1) == '/')
			s = s.substring(0, s.length()-1);
		return s;
	}

	private static boolean hasDotSegments(String s) {
		if (s == null)
			return false;
		for (int i = 0; i < s.length()-1; i++) {
			char c = s.charAt(i);
			if ((i == 0 && c == '/') || (c == '/' && s.charAt(i+1) == '.'))
				return true;
		}
		return false;
	}
}
