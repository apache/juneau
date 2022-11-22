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
package org.apache.juneau.rest.widget;

import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.response.*;

/**
 * Defines an interface for resolvers of <js>"$W{...}"</js> string variables.
 *
 * <p>
 * Widgets must provide one of the following public constructors:
 * <ul>
 * 	<li><code><jk>public</jk> Widget();</code>
 * 	<li><code><jk>public</jk> Widget(ContextProperties);</code>
 * </ul>
 *
 * <p>
 * Widgets can be defined as inner classes of REST resource classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlPredefinedWidgets">Predefined Widgets</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlWidgets">Widgets</a>
 * </ul>
 */
public abstract class Widget implements HtmlWidget {

	/**
	 * The widget key.
	 *
	 * <p>
	 * (i.e. The variable name inside the <js>"$W{...}"</js> variable).
	 *
	 * <p>
	 * The returned value must not be <jk>null</jk>.
	 *
	 * <p>
	 * If not overridden, the default value is the class simple name.
	 *
	 * @return The widget key.
	 */
	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	private RestRequest req(VarResolverSession session) {
		return session.getBean(RestRequest.class).orElseThrow(InternalServerError::new);
	}

	private RestResponse res(VarResolverSession session) {
		return session.getBean(RestResponse.class).orElseThrow(InternalServerError::new);
	}

	@Override /* HtmlWidget */
	public String getHtml(VarResolverSession session) {
		return getHtml(req(session), res(session));
	}

	@Override /* HtmlWidget */
	public String getScript(VarResolverSession session) {
		return getScript(req(session), res(session));
	}

	@Override /* HtmlWidget */
	public String getStyle(VarResolverSession session) {
		return getStyle(req(session), res(session));
	}

	/**
	 * Resolves the HTML content for this widget.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param req The HTTP request object.
	 * @param res The current HTTP response.
	 * @return The HTML content of this widget.
	 */
	public String getHtml(RestRequest req, RestResponse res) {
		return null;
	}

	/**
	 * Resolves any Javascript that should be added to the <xt>&lt;head&gt;/&lt;script&gt;</xt> element.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param req The HTTP request object.
	 * @param res The current HTTP response.
	 * @return The Javascript needed by this widget.
	 */
	public String getScript(RestRequest req, RestResponse res) {
		return null;
	}

	/**
	 * Resolves any CSS styles that should be added to the <xt>&lt;head&gt;/&lt;style&gt;</xt> element.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param req The HTTP request object.
	 * @param res The current HTTP response.
	 * @return The CSS styles needed by this widget.
	 */
	public String getStyle(RestRequest req, RestResponse res) {
		return null;
	}

	/**
	 * Returns the file finder to use for finding files on the file system.
	 *
	 * @param req The HTTP request object.
	 * @return The file finder to used for finding files on the file system.
	 */
	protected FileFinder getFileFinder(RestRequest req) {
		return req.getStaticFiles();
	}

	/**
	 * Loads the specified javascript file and strips any Javascript comments from the file.
	 *
	 * <p>
	 * Comments are assumed to be Java-style block comments: <js>"/*"</js>.
	 *
	 * @param req The HTTP request object.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 */
	protected String loadScript(RestRequest req, String name) {
		try {
			String s = getFileFinder(req).getString(name, null).orElse(null);
			if (s != null)
				s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
			return s;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Same as {@link #loadScript(RestRequest,String)} but replaces request-time SVL variables.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getVarResolver()}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
	 * </ul>
	 *
	 * @param req The current HTTP request.
	 * @param res The current HTTP response.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected String loadScriptWithVars(RestRequest req, RestResponse res, String name) throws IOException {
		return req.getVarResolverSession().resolve(loadScript(req, name));
	}

	/**
	 * Loads the specified CSS file and strips CSS comments from the file.
	 *
	 * <p>
	 * Comments are assumed to be Java-style block comments: <js>"/*"</js>.
	 *
	 * @param req The HTTP request object.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 */
	protected String loadStyle(RestRequest req, String name) {
		try {
			String s = getFileFinder(req).getString(name, null).orElse(null);
			if (s != null)
				s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
			return s;
		} catch (IOException e) {
			throw asRuntimeException(e);
		}
	}

	/**
	 * Same as {@link #loadStyle(RestRequest,String)} but replaces request-time SVL variables.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getVarResolver()}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
	 * </ul>
	 *
	 * @param req The current HTTP request.
	 * @param res The current HTTP response.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected String loadStyleWithVars(RestRequest req, RestResponse res, String name) throws IOException {
		return req.getVarResolverSession().resolve(loadStyle(req, name));
	}

	/**
	 * Loads the specified HTML file and strips HTML comments from the file.
	 *
	 * <p>
	 * Comment are assumed to be <js>"&lt;!-- --&gt;"</js> code blocks.
	 *
	 * @param req The HTTP request object.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 */
	protected String loadHtml(RestRequest req, String name) {
		try {
			String s = getFileFinder(req).getString(name, null).orElse(null);
			if (s != null)
				s = s.replaceAll("(?s)<!--(.*?)-->\\s*", "");
			return s;
		} catch (IOException e) {
			throw asRuntimeException(e);
		}
	}

	/**
	 * Same as {@link #loadHtml(RestRequest,String)} but replaces request-time SVL variables.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getVarResolver()}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
	 * </ul>
	 *
	 * @param req The current HTTP request.
	 * @param res The current HTTP response.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected String loadHtmlWithVars(RestRequest req, RestResponse res, String name) throws IOException {
		return req.getVarResolverSession().resolve(loadHtml(req, name));
	}
}
