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
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Request attribute variable resolver.
 *
 * <p>
 * The format for this var is <js>"$R{key1[,key2...]}"</js>.
 * <br>When multiple keys are used, returns the first non-null/empty value.
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
 * </ul>
 * 
 * <p>
 * <h6 class='figure'>Example:</h6>
 * <p class='bcode'>
 * 	String servletTitle = restRequest.resolveVars(<js>"$R{servletTitle}"</js>); 
 * 	String servletTitleOrDescription = restRequest.resolveVars(<js>"$R{servletTitle,servletDescription}"</js>); 
 * </p>
 * 
 * <h6 class='topic'>Notes:</h6>
 * <ul class='spaced-list'>
 * 	<li>
 * 		This variable resolver requires that a {@link RestRequest} object be set as a context object on the resolver 
 * 		or a session object on the resolver session.
 * 	<li>
 * 		For security reasons, nested and recursive variables are not resolved.
 * </ul>
 *
 * @see org.apache.juneau.svl
 */
public class RequestVar extends MultipartResolvingVar {

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
		char c = StringUtils.charAt(key, 0);
		if (c == 'c') {
			if ("contextPath".equals(key))
				return req.getContextPath();
		} else if (c == 'm') {
			if ("method".equals(key))
				return req.getMethod();
			if ("methodDescription".equals(key))
				return req.getMethodDescription();
			if ("methodSummary".equals(key))
				return req.getMethodSummary();
		} else if (c == 'p') {
			if ("pathInfo".equals(key))
				return req.getPathInfo();
		} else if (c == 'r') {
			if ("requestParentURI".equals(key))
				return req.getUriContext().getRootRelativePathInfoParent();
			if ("requestURI".equals(key))
				return req.getRequestURI();
		} else if (c == 's') {
			if ("servletClass".equals(key))
				return req.getContext().getResource().getClass().getName();
			if ("servletClassSimple".equals(key))
				return req.getContext().getResource().getClass().getSimpleName();
			if ("servletDescription".equals(key))
				return req.getServletDescription();
			if ("servletParentURI".equals(key))
				return req.getUriContext().getRootRelativeServletPathParent();
			if ("servletPath".equals(key))
				return req.getServletPath();
			if ("servletTitle".equals(key))
				return req.getServletTitle();
			if ("servletURI".equals(key))
				return req.getUriContext().getRootRelativeServletPath();
			if ("siteName".equals(key))
				return req.getSiteName();
		}
		return req.getProperties().getString(key);
	}
}