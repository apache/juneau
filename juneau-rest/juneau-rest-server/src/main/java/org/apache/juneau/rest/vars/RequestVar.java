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
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.response.*;
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
 * 	<li><js>"authorityPath"</js> - Value returned by {@link RestRequest#getAuthorityPath()}
 * 	<li><js>"contextPath"</js> - Value returned by {@link RestRequest#getContextPath()}
 * 	<li><js>"method"</js> - Value returned by {@link RestRequest#getMethod()}
 * 	<li><js>"pathInfo"</js> - Value returned by {@link RestRequest#getPathInfo()}
 * 	<li><js>"requestParentURI"</js> - Value returned by {@link UriContext#getRootRelativePathInfoParent()}
 * 	<li><js>"requestURI"</js> - Value returned by {@link RestRequest#getRequestURI()}
 * 	<li><js>"servletParentURI"</js> - Value returned by {@link UriContext#getRootRelativeServletPathParent()}
 * 	<li><js>"servletPath"</js> - See {@link RestRequest#getServletPath()}
 * 	<li><js>"servletURI"</js> - See {@link UriContext#getRootRelativeServletPath()}
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	String <jv>servletClass</jv> = <jv>restRequest</jv>.getVarResolver().resolve(<js>"$R{servletClass}"</js>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This variable resolver requires that a {@link RestRequest} bean be available in the session bean store.
 * 	<li class='note'>
 * 		For security reasons, nested and recursive variables are not resolved.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
 * </ul>
 */
public class RequestVar extends MultipartResolvingVar {

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

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		RestRequest req = session.getBean(RestRequest.class).orElseThrow(InternalServerError::new);
		char c = StringUtils.charAt(key, 0);
		if (c == 'a') {
			if ("authorityPath".equals(key))
				return req.getAuthorityPath();
		} else if (c == 'c') {
			if ("contextPath".equals(key))
				return req.getContextPath();
		} else if (c == 'm') {
			if ("method".equals(key))
				return req.getMethod();
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
				return req.getContext().getResourceClass().getName();
			if ("servletClassSimple".equals(key))
				return req.getContext().getResourceClass().getSimpleName();
			if ("servletParentURI".equals(key))
				return req.getUriContext().getRootRelativeServletPathParent();
			if ("servletPath".equals(key))
				return req.getServletPath();
			if ("servletURI".equals(key))
				return req.getUriContext().getRootRelativeServletPath();
		}
		return req.getAttributes().get(key).asString().orElse(null);
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.getBean(RestRequest.class).isPresent();
	}
}