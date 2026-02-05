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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.function.Supplier;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.collections.*;
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
 */
@Bean
@SuppressWarnings("java:S115") // Constants use UPPER_snakeCase convention (e.g., PROP_aContextRoot)
public class UriContext {

	// Property name constants
	private static final String PROP_aContextRoot = "aContextRoot";
	private static final String PROP_aPathInfo = "aPathInfo";
	private static final String PROP_aServletPath = "aServletPath";
	private static final String PROP_authority = "authority";
	private static final String PROP_contextRoot = "contextRoot";
	private static final String PROP_parentPath = "parentPath";
	private static final String PROP_pathInfo = "pathInfo";
	private static final String PROP_rContextRoot = "rContextRoot";
	private static final String PROP_rResource = "rResource";
	private static final String PROP_rPath = "rPath";
	private static final String PROP_servletPath = "servletPath";

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
			throw toRex(e);
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
		var i = uri.lastIndexOf('/');
		if (i <= 1)
			return "/";
		return uri.substring(0, i);
	}

	@SuppressWarnings("javadoc")
	public final String authority;
	@SuppressWarnings("javadoc")
	public final String contextRoot;
	@SuppressWarnings("javadoc")
	public final String servletPath;
	@SuppressWarnings("javadoc")
	public final String pathInfo;
	@SuppressWarnings("javadoc")
	public final String parentPath;

	// Memoized suppliers.
	private final Supplier<String> aContextRoot;
	private final Supplier<String> rContextRoot;
	private final Supplier<String> aServletPath;
	private final Supplier<String> rResource;
	private final Supplier<String> aPathInfo;
	private final Supplier<String> rPath;

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
		var m = JsonMap.ofJson(s);
		this.authority = nullIfEmpty(trimSlashes(m.getString("authority")));
		this.contextRoot = nullIfEmpty(trimSlashes(m.getString("contextRoot")));
		this.servletPath = nullIfEmpty(trimSlashes(m.getString("servletPath")));
		this.pathInfo = nullIfEmpty(trimSlashes(m.getString("pathInfo")));
		this.parentPath = this.pathInfo == null || this.pathInfo.indexOf('/') == -1 ? null : this.pathInfo.substring(0, this.pathInfo.lastIndexOf('/'));
		this.rContextRoot = mem(this::findRContextRoot);
		this.rResource = mem(this::findRResource);
		this.rPath = mem(this::findRPath);
		this.aContextRoot = mem(this::findAContextRoot);
		this.aServletPath = mem(this::findAServletPath);
		this.aPathInfo = mem(this::findAPathInfo);
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
		this.authority = nullIfEmpty(trimSlashes(authority));
		this.contextRoot = nullIfEmpty(trimSlashes(contextRoot));
		this.servletPath = nullIfEmpty(trimSlashes(servletPath));
		this.pathInfo = nullIfEmpty(trimSlashes(pathInfo));
		this.parentPath = this.pathInfo == null || this.pathInfo.indexOf('/') == -1 ? null : this.pathInfo.substring(0, this.pathInfo.lastIndexOf('/'));
		this.rContextRoot = mem(this::findRContextRoot);
		this.rResource = mem(this::findRResource);
		this.rPath = mem(this::findRPath);
		this.aContextRoot = mem(this::findAContextRoot);
		this.aServletPath = mem(this::findAServletPath);
		this.aPathInfo = mem(this::findAPathInfo);
	}

	private String findRContextRoot() {
		return contextRoot == null ? "/" : ('/' + contextRoot);
	}

	private String findRResource() {
		// @formatter:off
		if (contextRoot == null)
			return (
				servletPath == null
				? "/"
				: ('/' + servletPath)
			);
		return (
			servletPath == null
			? ('/' + contextRoot)
			: ('/' + contextRoot + '/' + servletPath)
		);
		// @formatter:on
	}

	private String findRPath() {
		// @formatter:off
		if (contextRoot == null) {
			if (servletPath == null)
				return (
					pathInfo == null
					? "/"
					: ('/' + pathInfo)
				);
			return (
				pathInfo == null
				? ('/' + servletPath)
				: ('/' + servletPath + '/' + pathInfo)
			);
		}
		if (servletPath == null)
			return (
				pathInfo == null
				? ('/' + contextRoot)
				: ('/' + contextRoot + '/' + pathInfo)
			);
		return (
			pathInfo == null
			? ('/' + contextRoot + '/' + servletPath)
			: ('/' + contextRoot + '/' + servletPath + '/' + pathInfo)
		);
		// @formatter:on
	}

	private String findAContextRoot() {
		// @formatter:off
		if (authority == null)
			return rContextRoot.get();
		return (
			contextRoot == null
			? authority
			: (authority + '/' + contextRoot)
		);
		// @formatter:on
	}

	private String findAServletPath() {
		// @formatter:off
		if (authority == null)
			return rResource.get();
		if (contextRoot == null)
			return (
				servletPath == null
				? authority
				: authority + '/' + servletPath
			);
		return (
			servletPath == null
			? (authority + '/' + contextRoot)
			: (authority + '/' + contextRoot + '/' + servletPath)
		);
		// @formatter:on
	}

	private String findAPathInfo() {
		// @formatter:off
		if (authority == null)
			return rPath.get();
		if (contextRoot == null) {
			if (servletPath == null)
				return (
					pathInfo == null
					? authority : (authority + '/' + pathInfo)
				);
			return (
				pathInfo == null
				? (authority + '/' + servletPath)
				: (authority + '/' + servletPath + '/' + pathInfo)
			);
		}
		if (servletPath == null)
			return (
				pathInfo == null
				? authority + '/' + contextRoot
				: (authority + '/' + contextRoot + '/' + pathInfo)
			);
		return (
			pathInfo == null
			? (authority + '/' + contextRoot + '/' + servletPath)
			: (authority + '/' + contextRoot + '/' + servletPath + '/' + pathInfo)
		);
		// @formatter:on
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
		return aContextRoot.get();
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
		return aPathInfo.get();
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
		return aServletPath.get();
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
		return rContextRoot.get();
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
		return rPath.get();
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
		return rResource.get();
	}

	/**
	 * Returns the parent of the URL returned by {@link #getRootRelativeServletPath()}.
	 *
	 * @return The parent of the URL returned by {@link #getRootRelativeServletPath()}.
	 */
	public String getRootRelativeServletPathParent() { return getParent(getRootRelativeServletPath()); }

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		return filteredBeanPropertyMap()
			.a(PROP_aContextRoot, aContextRoot.get())
			.a(PROP_aPathInfo, aPathInfo.get())
			.a(PROP_aServletPath, aServletPath.get())
			.a(PROP_authority, authority)
			.a(PROP_contextRoot, contextRoot)
			.a(PROP_parentPath, parentPath)
			.a(PROP_pathInfo, pathInfo)
			.a(PROP_rContextRoot, rContextRoot.get())
			.a(PROP_rResource, rResource.get())
			.a(PROP_servletPath, servletPath)
			.a(PROP_rPath, rPath.get());
		// @formatter:on
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}
}