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

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Request attribute variable resolver.
 * 
 * <p>
 * The format for this var is <js>"$RA{key1[,key2...]}"</js>.
 * 
 * <p>
 * Used to resolve values returned by {@link HttpServletRequest#getAttribute(String)}.
 * <br>When multiple keys are used, returns the first non-null/empty value.
 * 
 * <h6 class='section'>Example:</h6>
 * <p class='bcode'>
 * 	String foo = restRequest.resolveVars(<js>"$RA{foo}"</js>); 
 * 	String fooOrBar = restRequest.resolveVars(<js>"$RA{foo,bar}"</js>); 
 * </p>
 * 
 * <h5 class='topic'>Notes</h5>
 * <ul class='spaced-list'>
 * 	<li>
 * 		This variable resolver requires that a {@link RestRequest} object be set as a context object on the resolver 
 * 		or a session object on the resolver session.
 * 	<li>
 * 		For security reasons, nested and recursive variables are not resolved.
 * </ul>
 * 
 * 
 * <h5 class='section'>Documentation:</h5>
 * <ul>
 * 	<li><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.SvlVariables">Overview &gt; SVL Variables</a>
 * </ul>
 */
public class RequestAttributeVar extends MultipartResolvingVar {

	/**
	 * The name of the session or context object that identifies the {@link RestRequest} object.
	 */
	public static final String SESSION_req = "req";


	/** The name of this variable. */
	public static final String NAME = "RA";

	/**
	 * Constructor.
	 */
	public RequestAttributeVar() {
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
		return StringUtils.toString(req.getAttribute(key));
	}
}