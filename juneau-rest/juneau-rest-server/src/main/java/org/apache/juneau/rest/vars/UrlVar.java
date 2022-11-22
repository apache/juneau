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
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * URL variable resolver.
 *
 * <p>
 * The format for this var is <js>"$U{uri}"</js>.
 *
 * <p>
 * The advantage of using this variable is that you can resolve URLs with special protocols such as
 * <js>"servlet:/xxx"</js>.
 *
 * <p>
 * See {@link UriResolver} for the kinds of URIs that can be resolved.
 *
 * <p>
 * This variable resolver requires that a {@link RestRequest} bean be available in the session bean store.
 *
 * <p>
 * Uses the URI resolver returned by {@link RestRequest#getUriResolver()}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
 * </ul>
 */
public class UrlVar extends SimpleVar {

	/** The name of this variable. */
	public static final String NAME = "U";

	/**
	 * Constructor.
	 */
	public UrlVar() {
		super(NAME);
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return session.getBean(RestRequest.class).orElseThrow(InternalServerError::new).getUriResolver().resolve(key);
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.getBean(RestRequest.class).isPresent();
	}
}