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
package org.apache.juneau.rest.vars;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Request attribute variable resolver.
 *
 * <p>
 * The format for this var is <js>"$R{key}"</js>.
 * The possible values are:
 * <ul>
 * 	<li><code>$R{contextPath}</code> - Value returned by {@link RestRequest#getContextPath()}.
 * 	<li><code>$R{method}</code> - Value returned by {@link RestRequest#getMethod()}.
 * 	<li><code>$R{methodDescription}</code> - Value returned by {@link RestRequest#getMethodDescription()}.
 * 	<li><code>$R{pathInfo}</code> - Value returned by {@link RestRequest#getPathInfo()}.
 * 	<li><code>$R{requestParentURI}</code> - Value returned by {@link UriContext#getRootRelativePathInfoParent()}.
 * 	<li><code>$R{requestURI}</code> - Value returned by {@link RestRequest#getRequestURI()}.
 * 	<li><code>$R{servletDescription}</code> - Value returned by {@link RestRequest#getServletDescription()}.
 * 	<li><code>$R{servletTitle}</code> - Value returned by {@link RestRequest#getServletTitle()}.
 * 	<li><code>$R{servletParentURI}</code> - Value returned by {@link UriContext#getRootRelativeServletPathParent()}.
 * 	<li><code>$R{servletPath}</code> - Value returned by {@link RestRequest#getServletPath()}.
 * 	<li><code>$R{servletURI}</code> - Value returned by {@link UriContext#getRootRelativeServletPath()}.
 * </ul>
 *
 * <p>
 * This variable resolver requires that a {@link RestRequest} object be set as a context object on the resolver or a
 * session object on the resolver session.
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * @see org.apache.juneau.svl
 */
public class RequestVar extends SimpleVar {

	/**
	 * The name of the session or context object that identifies the {@link RestRequest} object.
	 */
	public static final String SESSION_req = "req";

	/** The name of this variable. */
	public static final String NAME = "R";

	/**
	 * Constructor.
	 */
	public RequestVar() {
		super(NAME);
	}

	@Override /* Parameter */
	public String resolve(VarResolverSession session, String key) {
		RestRequest req = session.getSessionObject(RestRequest.class, SESSION_req);
		if (key.length() > 0) {
				String k = key.toString();
				int i = k.indexOf('.');
				if (i != -1) {
					String prefix = k.substring(0, i);
					String remainder = k.substring(i+1);
					Object o = req.resolveProperty(null, prefix, remainder);
					if (o != null)
						return o.toString();
				} else {
					Object o = req.resolveProperty(null, "Request", key);
					if (o != null)
						return o.toString();
				}
				Object o = req.getProperties().get(key);
				if (o != null)
					return o.toString();
				return req.getPathMatch().get(key);
			}
		return null;
	}
}