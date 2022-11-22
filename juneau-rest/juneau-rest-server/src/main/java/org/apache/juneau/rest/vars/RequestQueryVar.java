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

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Request query variable resolver.
 *
 * <p>
 * The format for this var is <js>"$RQ{key1[,key2...]}"</js>.
 *
 * <p>
 * Used to resolve values returned by {@link RestRequest#getQueryParam(String)}.
 * <br>When multiple keys are used, returns the first non-null/empty value.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// URI = "...?foo=X&amp;bar=Y"</jc>
 * 	String <jv>foo</jv> = <jv>restRequest</jv>.getVarResolver().resolve(<js>"$RQ{foo}"</js>);
 * 	String <jv>fooOrBar</jv> = <jv>restRequest</jv>.getVarResolver().resolve(<js>"$RHQ{foo,bar}"</js>);
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
public class RequestQueryVar extends MultipartResolvingVar {

	/** The name of this variable. */
	public static final String NAME = "RQ";

	/**
	 * Constructor.
	 */
	public RequestQueryVar() {
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
		return session.getBean(RestRequest.class).orElseThrow(InternalServerError::new).getQueryParam(key).orElse(null);
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.getBean(RestRequest.class).isPresent();
	}
}