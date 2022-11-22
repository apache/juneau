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
 * Request path variable resolver.
 *
 * <p>
 * The format for this var is <js>"$RP{key1[,key2...]}"</js>.
 *
 * <p>
 * Used to resolve values returned by {@link RestRequest#getPathParam(String)}.
 * <br>When multiple keys are used, returns the first non-null/empty value.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// URI path pattern = "/foo/{foo}/bar/{bar}"</jc>
 * 	String <jv>foo</jv> = <jv>restRequest</jv>.getVarResolver().resolve(<js>"$RP{foo}"</js>);
 * 	String <jv>fooOrBar</jv> = <jv>restRequest</jv>.getVarResolver().resolve(<js>"$RP{foo,bar}"</js>);
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
public class RequestPathVar extends MultipartResolvingVar {

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
		RestRequest req = session.getBean(RestRequest.class).orElseThrow(InternalServerError::new);
		if ("REMAINDER".equals(key))
			return req.getPathParams().getRemainder().orElse(null);
		return req.getPathParam(key).orElse(null);
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.getBean(RestRequest.class).isPresent();
	}
}