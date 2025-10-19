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

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;

/**
 * Represents a URL broken into authority/context-root/servlet-path/path-info parts.
 *
 * <p>
 * A typical request against a URL takes the following form:
 * <p class='bcode'>
 * 	http://host:port/context-root/servlet-path/path-info
 * 	|   authority   |  context   |  resource  |  path  |
 * 	+--------------------------------------------------+
 * </p>
 *
 * <p>
 * This class allows you to convert URL strings to absolute (e.g. <js>"http://host:port/foo/bar"</js>) or root-relative
 * (e.g. <js>"/foo/bar"</js>) URLs.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Bean
public class UriContext {

	/**
	 * Default URI context.
	 *
	 * <p>
	 * No information about authority, servlet-root, context-root, or path-info is known.
	 */
	public static final UriContext DEFAULT = new UriContext();

	/**
	 * Static creator.
	 *
	 * @param s
	 * 	The input string.
	 * 	<br>Example: <js>{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}</js>
	 * @return A new {@link UriContext} object.
	 */
	public static UriContext of(String s) {
		try {
			return new UriContext(s);
		} catch (ParseException e) {
			throw asRuntimeException(e);
		}
	}

	/**
	 * Static creator.
	 *
	 * @param authority
	 * 	The authority portion of URL (e.g. <js>"http://hostname:port"</js>)
	 * @param contextRoot
	 * 	The context root of the application (e.g. <js>"/context-root"</js>, or <js>"context-root"</js>)
	 * @param servletPath
	 * 	The servlet path (e.g. <js>"/servlet-path"</js>, or <js>"servlet-path"</js>)
	 * @param pathInfo
	 * 	The path info (e.g. <js>"/path-info"</js>, or <js>"path-info"</js>)
	 * @return A new {@link UriContext} object.
	 */
	public static UriContext of(String authority, String contextRoot, String servletPath, String pathInfo) {
		return new UriContext(authority, contextRoot, servletPath, pathInfo);
	}

	private static String getParent(String uri) {
		int i = uri.lastIndexOf('/');
		if (i <= 1)
			return "/";
		return uri.substring(0, i);
	}

	@SuppressWarnings("javadoc")
	public final String authority, contextRoot, servletPath, pathInfo, parentPath;

	// Lazy-initialized fields.
	private String aContextRoot, rContextRoot, aServletPath, rResource, aPathInfo, rPath;

	/**
	 * Default constructor.
	 *
	 * <p>
	 * All <jk>null</jk> values.
	 */
	public UriContext() {
		this(null, null, null, null);
	}

	/**
	 * String constructor.
	 *
	 * <p>
	 * Input string is a JSON object with the following format:
	 * <js>{authority:'xxx',contextRoot:'xxx',servletPath:'xxx',pathInfo:'xxx'}</js>
	 *
	 * @param s
	 * 	The input string.
	 * 	<br>Example: <js>{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}</js>
	 * @throws ParseException
	 * 	If input string is not a valid JSON object.
	 */
	public UriContext(String s) throws ParseException {
		JsonMap m = JsonMap.ofJson(s);
		this.authority = StringUtils.nullIfEmpty(trimSlashes(m.getString("authority")));
		this.contextRoot = StringUtils.nullIfEmpty(trimSlashes(m.getString("contextRoot")));
		this.servletPath = StringUtils.nullIfEmpty(trimSlashes(m.getString("servletPath")));
		this.pathInfo = StringUtils.nullIfEmpty(trimSlashes(m.getString("pathInfo")));
		this.parentPath = this.pathInfo == null || this.pathInfo.indexOf('/') == -1 ? null : this.pathInfo.substring(0, this.pathInfo.lastIndexOf('/'));
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Leading and trailing slashes are trimmed of all parameters.
	 *
	 * <p>
	 * Any parameter can be <jk>null</jk>.  Blanks and nulls are equivalent.
	 *
	 * @param authority
	 * 	The authority portion of URL (e.g. <js>"http://hostname:port"</js>)
	 * @param contextRoot
	 * 	The context root of the application (e.g. <js>"/context-root"</js>, or <js>"context-root"</js>)
	 * @param servletPath
	 * 	The servlet path (e.g. <js>"/servlet-path"</js>, or <js>"servlet-path"</js>)
	 * @param pathInfo
	 * 	The path info (e.g. <js>"/path-info"</js>, or <js>"path-info"</js>)
	 */
	@Beanc
	public UriContext(@Name("authority") String authority, @Name("contextRoot") String contextRoot, @Name("servletPath") String servletPath, @Name("pathInfo") String pathInfo) {
		this.authority = StringUtils.nullIfEmpty(trimSlashes(authority));
		this.contextRoot = StringUtils.nullIfEmpty(trimSlashes(contextRoot));
		this.servletPath = StringUtils.nullIfEmpty(trimSlashes(servletPath));
		this.pathInfo = StringUtils.nullIfEmpty(trimSlashes(pathInfo));
		this.parentPath = this.pathInfo == null || this.pathInfo.indexOf('/') == -1 ? null : this.pathInfo.substring(0, this.pathInfo.lastIndexOf('/'));
	}

	/**
	 * Returns the absolute URI of just the authority portion of this URI context.
	 *
	 * <p>
	 * Example:  <js>"http://hostname:port"</js>
	 *
	 * <p>
	 * If the authority is null/empty, returns <js>"/"</js>.
	 *
	 * @return
	 * 	The absolute URI of just the authority portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getAbsoluteAuthority() { return authority == null ? "/" : authority; }

	/**
	 * Returns the absolute URI of the context-root portion of this URI context.
	 *
	 * <p>
	 * Example:  <js>"http://hostname:port/context-root"</js>
	 *
	 * @return
	 * 	The absolute URI of the context-root portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getAbsoluteContextRoot() {
		if (aContextRoot == null) {
			if (authority == null)
				aContextRoot = getRootRelativeContextRoot();
			else
				// @formatter:off
				aContextRoot = (
					contextRoot == null
					? authority
					: (authority + '/' + contextRoot)
				);
			// @formatter:on
		}
		return aContextRoot;
	}

	/**
	 * Returns the absolute URI of the path portion of this URI context.
	 *
	 * <p>
	 * Example:  <js>"http://hostname:port/context-root/servlet-path/path-info"</js>
	 *
	 * @return
	 * 	The absolute URI of the path portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getAbsolutePathInfo() {
		// @formatter:off
		if (aPathInfo == null) {
			if (authority == null)
				aPathInfo = getRootRelativePathInfo();
			else {
				if (contextRoot == null) {
					if (servletPath == null)
						aPathInfo = (
							pathInfo == null
							? authority : (authority + '/' + pathInfo)
						);
					else
						aPathInfo = (
							pathInfo == null
							? (authority + '/' + servletPath)
							: (authority + '/' + servletPath + '/' + pathInfo)
						);
				} else {
					if (servletPath == null)
						aPathInfo = (
							pathInfo == null
							? authority + '/' + contextRoot
							: (authority + '/' + contextRoot + '/' + pathInfo)
						);
					else
						aPathInfo = (
							pathInfo == null
							? (authority + '/' + contextRoot + '/' + servletPath)
							: (authority + '/' + contextRoot + '/' + servletPath + '/' + pathInfo)
						);
				}
			}
		}
		return aPathInfo;
		// @formatter:on
	}

	/**
	 * Returns the parent of the URL returned by {@link #getAbsolutePathInfo()}.
	 *
	 * @return The parent of the URL returned by {@link #getAbsolutePathInfo()}.
	 */
	public String getAbsolutePathInfoParent() { return getParent(getAbsolutePathInfo()); }

	/**
	 * Returns the absolute URI of the resource portion of this URI context.
	 *
	 * <p>
	 * Example:  <js>"http://hostname:port/context-root/servlet-path"</js>
	 *
	 * @return
	 * 	The absolute URI of the resource portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getAbsoluteServletPath() {
		// @formatter:off
		if (aServletPath == null) {
			if (authority == null)
				aServletPath = getRootRelativeServletPath();
			else {
				if (contextRoot == null)
					aServletPath = (
						servletPath == null
						? authority
						: authority + '/' + servletPath
					);
				else
					aServletPath = (
						servletPath == null
						? (authority + '/' + contextRoot)
						: (authority + '/' + contextRoot + '/' + servletPath)
					);
			}
		}
		return aServletPath;
		// @formatter:on
	}

	/**
	 * Returns the parent of the URL returned by {@link #getAbsoluteServletPath()}.
	 *
	 * @return The parent of the URL returned by {@link #getAbsoluteServletPath()}.
	 */
	public String getAbsoluteServletPathParent() { return getParent(getAbsoluteServletPath()); }

	/**
	 * Returns the root-relative URI of the context portion of this URI context.
	 *
	 * <p>
	 * Example:  <js>"/context-root"</js>
	 *
	 * @return
	 * 	The root-relative URI of the context portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getRootRelativeContextRoot() {
		if (rContextRoot == null)
			rContextRoot = contextRoot == null ? "/" : ('/' + contextRoot);
		return rContextRoot;
	}

	/**
	 * Returns the root-relative URI of the path portion of this URI context.
	 *
	 * <p>
	 * Example:  <js>"/context-root/servlet-path/path-info"</js>
	 *
	 * @return
	 * 	The root-relative URI of the path portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getRootRelativePathInfo() {
		// @formatter:off
		if (rPath == null) {
			if (contextRoot == null) {
				if (servletPath == null)
					rPath = (
						pathInfo == null
						? "/"
						: ('/' + pathInfo)
					);
				else
					rPath = (
						pathInfo == null
						? ('/' + servletPath)
						: ('/' + servletPath + '/' + pathInfo)
					);
			} else {
				if (servletPath == null)
					rPath = (
						pathInfo == null
						? ('/' + contextRoot)
						: ('/' + contextRoot + '/' + pathInfo)
					);
				else
					rPath = (
						pathInfo == null
						? ('/' + contextRoot + '/' + servletPath)
						: ('/' + contextRoot + '/' + servletPath + '/' + pathInfo)
					);
			}
		}
		return rPath;
		// @formatter:on
	}

	/**
	 * Returns the parent of the URL returned by {@link #getRootRelativePathInfo()}.
	 *
	 * @return The parent of the URL returned by {@link #getRootRelativePathInfo()}.
	 */
	public String getRootRelativePathInfoParent() { return getParent(getRootRelativePathInfo()); }

	/**
	 * Returns the root-relative URI of the resource portion of this URI context.
	 *
	 * <p>
	 * Example:  <js>"/context-root/servlet-path"</js>
	 *
	 * @return
	 * 	The root-relative URI of the resource portion of this URI context.
	 * 	Never <jk>null</jk>.
	 */
	public String getRootRelativeServletPath() {
		// @formatter:off
		if (rResource == null) {
			if (contextRoot == null)
				rResource = (
					servletPath == null
					? "/"
					: ('/' + servletPath)
				);
			else
				rResource = (
					servletPath == null
					? ('/' + contextRoot)
					: ('/' + contextRoot + '/' + servletPath)
				);
		}
		return rResource;
		// @formatter:on
	}

	/**
	 * Returns the parent of the URL returned by {@link #getRootRelativeServletPath()}.
	 *
	 * @return The parent of the URL returned by {@link #getRootRelativeServletPath()}.
	 */
	public String getRootRelativeServletPathParent() { return getParent(getRootRelativeServletPath()); }

	@Override /* Overridden from Object */
	public String toString() {
		return Json5Serializer.DEFAULT.toString(this);
	}
}