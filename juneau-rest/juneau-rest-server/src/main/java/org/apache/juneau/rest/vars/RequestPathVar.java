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

import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Request path variable resolver.
 *
 * <p>
 * The format for this var is <js>"$RP{key1[,key2...]}"</js>.
 *
 * <p>
 * Used to resolve values returned by {@link RestRequest#getPath(String)}.
 * <br>When multiple keys are used, returns the first non-null/empty value.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// URI path pattern = "/foo/{foo}/bar/{bar}"</jc>
 * 	String foo = restRequest.resolveVars(<js>"$RP{foo}"</js>);
 * 	String fooOrBar = restRequest.resolveVars(<js>"$RP{foo,bar}"</js>);
 * </p>
 *
 * <ul class='notes'>
 * 	<li>
 * 		This variable resolver requires that a {@link RestRequest} object be set as a context object on the resolver
 * 		or a session object on the resolver session.
 * 	<li>
 * 		For security reasons, nested and recursive variables are not resolved.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestSvlVariables}
 * </ul>
 */
public class RequestPathVar extends MultipartResolvingVar {

	private static final String SESSION_req = "req";

	/** The name of this variable. */
	public static final String NAME = "RP";

	/**
	 * Constructor.
	 */
	public RequestPathVar() {
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
		RestRequest req = session.getSessionObject(RestRequest.class, SESSION_req, true);
		if ("REMAINDER".equals(key))
			return req.getPathRemainder();
		return req.getPath(key);
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.hasSessionObject(SESSION_req);
	}
}