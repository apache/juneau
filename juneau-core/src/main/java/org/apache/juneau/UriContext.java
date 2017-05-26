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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.UriResolution.*;
import static org.apache.juneau.UriRelativity.*;

import java.io.*;

import org.apache.juneau.annotation.*;

/**
 * Represents a URL broken into authority/context-root/servlet-path/path-info parts.
 * <p>
 * A typical request against a URL takes the following form:
 * <p class='bcode'>
 * 	http://host:port/context-root/servlet-path/path-info
 * 	|   authority   |  context   |  resource  |  path  |
 *  +--------------------------------------------------+
 * </p>
 * <p>
 * This class allows you to convert URL strings to absolute (e.g. <js>"http://host:port/foo/bar"</js>) or root-relative
 * 	(e.g. <js>"/foo/bar"</js>) URLs.
 * <p>
 * Two special protocols are used to represent context-root-relative and servlet-relative URIs:
 * 	<js>"context:/"</js> and <js>"servlet:/"</js>.
 *
 * The following list shows the types of URLs that can be resolved with this class:
 * <ul>
 * 	<li><js>"foo://foo"</js> - Absolute URI.
 * 	<li><js>"/foo"</js> - Root-relative URI.
 * 	<li><js>"/"</js> - Root URI.
 * 	<li><js>"context:/foo"</js> - Context-root-relative URI.
 * 	<li><js>"context:/"</js> - Context-root URI.
 * 	<li><js>"servlet:/foo"</js> - Servlet-path-relative URI.
 * 	<li><js>"servlet:/"</js> - Servlet-path URI.
 * 	<li><js>"foo"</js> - Path-info-relative URI.
 * 	<li><js>""</js> - Path-info URI.
 * </ul>
 *
 * The following class shows how
 */
@Bean
public class UriContext {

	/**
	 * Default URI context.
	 * No URI resolution occurs.
	 * No information about authority, servlet-root, context-root, or path-info is known.
	 */
	public static final UriContext DEFAULT = new UriContext(UriResolution.NONE, UriRelativity.PATH_INFO, null, null, null, null);

	private final String authority, contextRoot, servletPath, pathInfo, parentPath;

	private final UriResolution resolution;
	private final UriRelativity relativity;

	// Lazy-initialized fields.
	private String aContextRoot, rContextRoot, aServletPath, rResource, aPathInfo, rPath;


	/**
	 * Constructor.
	 * <p>
	 * Leading and trailing slashes are trimmed of all parameters.
	 * <p>
	 * Any parameter can be <jk>null</jk>.  Blanks and nulls are equivalent.
	 *
	 * @param resolution
	 * @param relativity
	 * @param authority - The authority portion of URL (e.g. <js>"http://hostname:port"</js>)
	 * @param contextRoot - The context root of the application (e.g. <js>"/context-root"</js>, or <js>"context-root"</js>)
	 * @param servletPath - The servlet path (e.g. <js>"/servlet-path"</js>, or <js>"servlet-path"</js>)
	 * @param pathInfo - The path info (e.g. <js>"/path-info"</js>, or <js>"path-info"</js>)
	 */
	@BeanConstructor(properties="resolution,relativity,authority,contextRoot,servletPath,pathInfo")
	public UriContext(UriResolution resolution, UriRelativity relativity, String authority, String contextRoot, String servletPath, String pathInfo) {
		this.resolution = resolution;
		this.relativity = relativity;
		this.authority = nullIfEmpty(trimSlashes(authority));
		this.contextRoot = nullIfEmpty(trimSlashes(contextRoot));
		this.servletPath = nullIfEmpty(trimSlashes(servletPath));
		this.pathInfo = nullIfEmpty(trimSlashes(pathInfo));
		this.parentPath = this.pathInfo == null || this.pathInfo.indexOf('/') == -1 ? null : this.pathInfo.substring(0, this.pathInfo.lastIndexOf('/'));
	}

	/**
	 * Default constructor.
	 */
	public UriContext() {
		this(ROOT_RELATIVE, RESOURCE, null, null, null, null);
	}

	/**
	 * Returns the absolute URI of just the authority portion of this URI context.
	 * <p>
	 * Example:  <js>"http://hostname:port"</js>
	 * <p>
	 * If the authority is null/empty, returns <js>"/"</js>.
	 *
	 * @return The absolute URI of just the authority portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getAbsoluteAuthority() {
		return authority == null ? "/" : authority;
	}

	/**
	 * Returns the absolute URI of the context-root portion of this URI context.
	 * <p>
	 * Example:  <js>"http://hostname:port/context-root"</js>
	 *
	 * @return The absolute URI of the context-root portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getAbsoluteContextRoot() {
		if (aContextRoot == null) {
			if (authority == null)
				aContextRoot = getRootRelativeContextRoot();
			else
				aContextRoot = (contextRoot == null ? authority : (authority + '/' + contextRoot));
		}
		return aContextRoot;
	}

	/**
	 * Returns the root-relative URI of the context portion of this URI context.
	 * <p>
	 * Example:  <js>"/context-root"</js>
	 *
	 * @return The root-relative URI of the context portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getRootRelativeContextRoot() {
		if (rContextRoot == null)
			rContextRoot = contextRoot == null ? "/" : ('/' + contextRoot);
		return rContextRoot;
	}

	/**
	 * Returns the absolute URI of the resource portion of this URI context.
	 * <p>
	 * Example:  <js>"http://hostname:port/context-root/servlet-path"</js>
	 *
	 * @return The absolute URI of the resource portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getAbsoluteServletPath() {
		if (aServletPath == null) {
			if (authority == null)
				aServletPath = getRootRelativeServletPath();
			else {
				if (contextRoot == null)
					aServletPath = (servletPath == null ? authority : authority + '/' + servletPath);
				else
					aServletPath = (servletPath == null ? (authority + '/' + contextRoot) : (authority + '/' + contextRoot + '/' + servletPath));
			}
		}
		return aServletPath;
	}

	/**
	 * Returns the root-relative URI of the resource portion of this URI context.
	 * <p>
	 * Example:  <js>"/context-root/servlet-path"</js>
	 *
	 * @return The root-relative URI of the resource portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getRootRelativeServletPath() {
		if (rResource == null) {
			if (contextRoot == null)
				rResource = (servletPath == null ? "/" : ('/' + servletPath));
			else
				rResource = (servletPath == null ? ('/' + contextRoot) : ('/' + contextRoot + '/' + servletPath));
		}
		return rResource;
	}

	/**
	 * Returns the parent of the URL returned by {@link #getAbsoluteServletPath()}.
	 *
	 * @return The parent of the URL returned by {@link #getAbsoluteServletPath()}.
	 */
	public String getAbsoluteServletPathParent() {
		return getParent(getAbsoluteServletPath());
	}

	/**
	 * Returns the parent of the URL returned by {@link #getRootRelativeServletPath()}.
	 *
	 * @return The parent of the URL returned by {@link #getRootRelativeServletPath()}.
	 */
	public String getRootRelativeServletPathParent() {
		return getParent(getRootRelativeServletPath());
	}

	/**
	 * Returns the absolute URI of the path portion of this URI context.
	 * <p>
	 * Example:  <js>"http://hostname:port/context-root/servlet-path/path-info"</js>
	 *
	 * @return The absolute URI of the path portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getAbsolutePathInfo() {
		if (aPathInfo == null) {
			if (authority == null)
				aPathInfo = getRootRelativePathInfo();
			else {
				if (contextRoot == null) {
					if (servletPath == null)
						aPathInfo = (pathInfo == null ? authority : (authority + '/' + pathInfo));
					else
						aPathInfo = (pathInfo == null ? (authority + '/' + servletPath) : (authority + '/' + servletPath + '/' + pathInfo));
				} else {
					if (servletPath == null)
						aPathInfo = (pathInfo == null ? authority + '/' + contextRoot : (authority + '/' + contextRoot + '/' + pathInfo));
					else
						aPathInfo = (pathInfo == null ? (authority + '/' + contextRoot + '/' + servletPath) : (authority + '/' + contextRoot + '/' + servletPath + '/' + pathInfo));
				}
			}
		}
		return aPathInfo;
	}

	/**
	 * Returns the root-relative URI of the path portion of this URI context.
	 * <p>
	 * Example:  <js>"/context-root/servlet-path/path-info"</js>
	 *
	 * @return The root-relative URI of the path portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getRootRelativePathInfo() {
		if (rPath == null) {
			if (contextRoot == null) {
				if (servletPath == null)
					rPath = (pathInfo == null ? "/" : ('/' + pathInfo));
				else
					rPath = (pathInfo == null ? ('/' + servletPath) : ('/' + servletPath + '/' + pathInfo));
			} else {
				if (servletPath == null)
					rPath = (pathInfo == null ? ('/' + contextRoot) : ('/' + contextRoot + '/' + pathInfo));
				else
					rPath = (pathInfo == null ? ('/' + contextRoot + '/' + servletPath) : ('/' + contextRoot + '/' + servletPath + '/' + pathInfo));
			}
		}
		return rPath;
	}

	/**
	 * Returns the parent of the URL returned by {@link #getAbsolutePathInfo()}.
	 *
	 * @return The parent of the URL returned by {@link #getAbsolutePathInfo()}.
	 */
	public String getAbsolutePathInfoParent() {
		return getParent(getAbsolutePathInfo());
	}

	/**
	 * Returns the parent of the URL returned by {@link #getRootRelativePathInfo()}.
	 *
	 * @return The parent of the URL returned by {@link #getRootRelativePathInfo()}.
	 */
	public String getRootRelativePathInfoParent() {
		return getParent(getRootRelativePathInfo());
	}

	/**
	 * Converts the specified URI to absolute form based on values in this context.
	 *
	 * @param uri The URI to convert to absolute form.
	 * @return The converted URI.
	 */
	public String resolve(String uri) {
		if (isAbsoluteUri(uri))
			return uri;
		if (resolution == ROOT_RELATIVE && startsWith(uri, '/'))
			return uri;
		if (resolution == NONE && ! isSpecialUri(uri))
			return uri;
		return append(new StringBuilder(), uri).toString();
	}

	/**
	 * Same as {@link #resolve(String)} except appends result to the specified appendable.
	 *
	 * @param a The appendable to append the URL to.
	 * @param uri The URI to convert to absolute form.
	 * @return The same appendable passed in.
	 */
	public Appendable append(Appendable a, String uri) {

		try {
			uri = nullIfEmpty(uri);

			// Absolute paths are not changed.
			if (isAbsoluteUri(uri))
				return a.append(uri);
			if (resolution == NONE && ! isSpecialUri(uri))
				return a.append(emptyIfNull(uri));
			if (resolution == ROOT_RELATIVE && startsWith(uri, '/'))
				return a.append(uri);

			// Root-relative path
			if (startsWith(uri, '/')) {
				if (authority != null){
					a.append(authority);
					if (uri.length() == 1)
						return a;
				}
				return a.append(uri);
			}

			// Context-relative path
			if (uri != null && uri.startsWith("context:/")) {
				if (resolution == ABSOLUTE && authority != null)
					a.append(authority);
				if (contextRoot != null)
					a.append('/').append(contextRoot);
				if (uri.length() > 9)
					a.append('/').append(uri.substring(9));
				else if (contextRoot == null && (authority == null || resolution != ABSOLUTE))
					a.append('/');
				return a;
			}

			// Resource-relative path
			if (uri != null && uri.startsWith("servlet:/")) {
				if (resolution == ABSOLUTE && authority != null)
					a.append(authority);
				if (contextRoot != null)
					a.append('/').append(contextRoot);
				if (servletPath != null)
					a.append('/').append(servletPath);
				if (uri.length() > 9)
					a.append('/').append(uri.substring(9));
				else if (servletPath == null && contextRoot == null && (authority == null || resolution != ABSOLUTE))
					a.append('/');
				return a;
			}

			// Relative path
			if (resolution == ABSOLUTE && authority != null)
				a.append(authority);
			if (contextRoot != null)
				a.append('/').append(contextRoot);
			if (servletPath != null)
				a.append('/').append(servletPath);
			if (relativity == RESOURCE && uri != null)
				a.append('/').append(uri);
			else if (relativity == PATH_INFO) {
				if (uri == null) {
					if (pathInfo != null)
						a.append('/').append(pathInfo);
				} else {
					if (parentPath != null)
						a.append('/').append(parentPath);
					a.append('/').append(uri);
				}
			}
			else if (uri == null && contextRoot == null && servletPath == null && (authority == null || resolution != ABSOLUTE))
				a.append('/');

			return a;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getParent(String uri) {
		int i = uri.lastIndexOf('/');
		if (i <= 1)
			return "/";
		return uri.substring(0, i);
	}

	private static boolean isSpecialUri(String s) {
		if (s == null || s.length() == 0)
			return false;
		char c = s.charAt(0);
		if (c != 's' && c != 'c')
			return false;
		return s.startsWith("servlet:/") || s.startsWith("context:/");
	}
}
