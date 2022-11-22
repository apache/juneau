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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.StateMachineState.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.util.*;

/**
 * Used to resolve incoming URLS to the various URL artifacts of <l>HttpServletRequest</l>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-mock">juneau-rest-mock</a>
 * </ul>
 */
class MockPathResolver {

	private String uri, target, contextPath, servletPath, remainder;
	private String error;

	/**
	 * Constructor.
	 *
	 * @param target
	 * 	The target portion of the URL (e.g. <js>"http://localhost"</js>).
	 * 	<br>If <jk>null</jk>, <js>"http://localhost"</js> is assumed.
	 * @param contextPath
	 * 	The context path of the servlet, or <jk>null</jk> if unknown or doesn't have one.
	 * @param servletPath
	 * 	The servlet path of the servlet, or <jk>null</jk> if unknown or doesn't have one.
	 * @param pathToResolve
	 * 	The path to resolve.
	 * 	<br>Can be relative to servlet or an absolute path.
	 * @param pathVars
	 * 	Optional path variables to resolve in the context path or servlet path.
	 */
	public MockPathResolver(String target, String contextPath, String servletPath, String pathToResolve, Map<String,Object> pathVars) {
		try {
			init(target, contextPath, servletPath, pathToResolve, pathVars);
		} catch (Exception e) {
			error = e.getLocalizedMessage();
		}
	}

	private void init(String target, String contextPath, String servletPath, String pathToResolve, Map<String,Object> pathVars) {

		target = trimTrailingSlashes(emptyIfNull(target));
		if (target.isEmpty())
			target = "http://localhost";

		contextPath = fixSegment(contextPath, pathVars);
		servletPath = fixSegment(servletPath, pathVars);
		pathToResolve = emptyIfNull(pathToResolve);

		if (! (pathToResolve.startsWith("http://") || pathToResolve.startsWith("https://"))) {
			pathToResolve = fixSegment(pathToResolve, Collections.emptyMap());
			this.uri = target + contextPath + servletPath + pathToResolve;
			this.target = target;
			this.contextPath = contextPath;
			this.servletPath = servletPath;
			this.remainder = pathToResolve;
			return;
		}

		// Path starts with http[s]: so we have to parse it to resolve variables.
		this.uri = pathToResolve;

		// S03 - Found "http://", looking for any character other than '/' (end of target).
		// S04 - Found  any character, looking for 3rd '/' (end of target).
		// S05 - Found 3rd '/', resolving contextPath.
		// S06 - Resolved contextPath, resolving servletPath.
		// S07 - Resolved servletPath.
		StateMachineState state = S03;

		int cpSegments = StringUtils.countChars(contextPath, '/');
		int spSegments = StringUtils.countChars(servletPath, '/');

		this.contextPath = "";
		this.servletPath = "";
		this.remainder = "";

		int mark = 0;
		for (int i = uri.indexOf("://")+3; i < uri.length(); i++) {
			char c = uri.charAt(i);
			if (state == S03) {
				if (c != '/')
					state = S04;
				else
					break;
			} else if (state == S04) {
				if (c == '/') {
					this.target = uri.substring(0, i);
					state = S05;
					if (contextPath.isEmpty()) {
						state = S06;
						if (servletPath.isEmpty()) {
							state = S07;
						}
					}
					mark = i;
				}
			} else if (state == S05) {
				if (c == '/') {
					cpSegments--;
					if (cpSegments == 0) {
						this.contextPath = uri.substring(mark, i);
						mark = i;
						state = S06;
						if (servletPath.isEmpty()) {
							state = S07;
						}
					}
				}
			} else if (state == S06) {
				if (c == '/') {
					spSegments--;
					if (spSegments == 0) {
						this.servletPath = uri.substring(mark, i);
						mark = i;
						state = S07;
					}
				}
			}
		}

		if (state == S04) {
			this.target = uri;
		} else if (state == S05) {
			this.contextPath = uri.substring(mark);
		} else if (state == S06) {
			this.servletPath = uri.substring(mark);
		} else if (state == S07) {
			this.remainder = uri.substring(mark);
		} else {
			throw new BasicRuntimeException("Invalid URI pattern encountered:  {0}", uri);
		}

		if (! contextPath.isEmpty()) {
			UrlPathMatcher p = UrlPathMatcher.of(contextPath);
			if (p.match(UrlPath.of(this.contextPath)) == null)
				throw new BasicRuntimeException("Context path [{0}] not found in URI:  {1}", contextPath, uri);
		}

		if (! servletPath.isEmpty()) {
			UrlPathMatcher p = UrlPathMatcher.of(servletPath);
			if (p.match(UrlPath.of(this.servletPath)) == null)
				throw new BasicRuntimeException("Servlet path [{0}] not found in URI:  {1}", servletPath, uri);
		}
	}

	private static String fixSegment(String s, Map<String,Object> pathVars) {
		s = replaceVars(emptyIfNull(s), pathVars);
		if (s.isEmpty() || s.equals("/"))
			return "";
		s = trimTrailingSlashes(s);
		if (s.charAt(0) != '/')
			s = '/' + s;
		return s;

	}

	/**
	 * Returns the fully-qualified URL.
	 *
	 * @return The fully-qualified URL.
	 */
	public String getURI() {
		return uri;
	}

	/**
	 * Returns the context path of the URL.
	 *
	 * @return The context path of the URL always starting with <js>'/'</js>, or an empty string if it doesn't exist.
	 */
	public String getContextPath() {
		return contextPath;
	}

	/**
	 * Returns the servlet path of the URL.
	 *
	 * @return The servlet path of the URL always starting with <js>'/'</js>, or an empty string if it doesn't exist.
	 */
	public String getServletPath() {
		return servletPath;
	}

	/**
	 * Returns the remainder of the URL following the context and servlet paths.
	 *
	 * @return The remainder of the URL.
	 */
	public String getRemainder() {
		return remainder;
	}

	/**
	 * Returns just the hostname portion of the URL.
	 *
	 * @return The hostname portion of the URL.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Returns any parsing errors.
	 *
	 * @return Any parsing errors.
	 */
	public String getError() {
		return error;
	}

	@Override
	public String toString() {
		return JsonMap.create()
			.append("uri", uri)
			.append("contextPath", contextPath)
			.append("servletPath", servletPath)
			.append("remainder", remainder)
			.append("target", target)
			.append("error", error)
			.toString();
	}
}
