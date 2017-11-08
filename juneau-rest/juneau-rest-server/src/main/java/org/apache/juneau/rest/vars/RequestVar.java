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

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Request attribute variable resolver.
 *
 * <p>
 * The format for this var is <js>"$R{key[,defaultValue]}"</js>.
 *
 * <p>
 * The possible values are:
 * <ul>
 * 	<li><js>"contextPath"</js> - Value returned by {@link RestRequest#getContextPath()}
 * 	<li><js>"method"</js> - Value returned by {@link RestRequest#getMethod()}
 * 	<li><js>"methodDescription"</js> - Value returned by {@link RestRequest#getMethodDescription()}
 * 	<li><js>"methodSummary"</js> - Value returned by {@link RestRequest#getMethodSummary()}
 * 	<li><js>"pathInfo"</js> - Value returned by {@link RestRequest#getPathInfo()}
 * 	<li><js>"requestParentURI"</js> - Value returned by {@link UriContext#getRootRelativePathInfoParent()}
 * 	<li><js>"requestURI"</js> - Value returned by {@link RestRequest#getRequestURI()}
 * 	<li><js>"servletDescription"</js> - Value returned by {@link RestRequest#getServletDescription()}
 * 	<li><js>"servletParentURI"</js> - Value returned by {@link UriContext#getRootRelativeServletPathParent()}
 * 	<li><js>"servletPath"</js> - See {@link RestRequest#getServletPath()}
 * 	<li><js>"servletTitle"</js> - See {@link RestRequest#getServletTitle()}
 * 	<li><js>"servletURI"</js> - See {@link UriContext#getRootRelativeServletPath()}
 * 	<li><js>"siteName"</js> - See {@link RestRequest#getSiteName()}
 * 	<li><js>"Attribute.x"</js> - Value returned by {@link HttpServletRequest#getAttribute(String)}.
 * 	<li><js>"FormData.x"</js> - Value returned by {@link RestRequest#getFormData(String)}.
 * 	<li><js>"Header.x"</js> - Value returned by {@link RestRequest#getHeader(String)}.
 * 	<li><js>"Path.x"</js> - Value returned by {@link RestRequest#getPath(String)}.
 * 	<li><js>"Query.x"</js> = Value returned by {@link RestRequest#getQuery(String)}.
 * </ul>
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
public class RequestVar extends DefaultingVar {

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
	
	@Override /* Var */
	protected boolean allowNested() {
		return false;
	}

	@Override /* Var */
	protected boolean allowRecurse() {
		return false;
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