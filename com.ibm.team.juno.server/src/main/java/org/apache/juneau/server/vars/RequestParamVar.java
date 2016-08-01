/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/

package org.apache.juneau.server.vars;

import org.apache.juneau.server.*;
import org.apache.juneau.svl.*;

/**
 * Request parameter variable resolver.
 * <p>
 * The format for this var is <js>"$P{key}"</js> or <js>"$P{key,defaultValue}"</js>.
 * <p>
 * This variable resolver requires that a {@link RestRequest} object be set as a context object on the resolver or a
 * 	session object on the resolver session.
 * <p>
 * Values are pulled from the {@link RestRequest#getParameter(String)} method.
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * @see org.apache.juneau.svl
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class RequestParamVar extends DefaultingVar {

	/**
	 * Constructor.
	 */
	public RequestParamVar() {
		super("P");
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return session.getSessionObject(RestRequest.class, RequestVar.SESSION_req).getParameter(key);
	}
}
