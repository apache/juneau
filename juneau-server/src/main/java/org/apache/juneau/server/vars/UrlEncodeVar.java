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
package org.apache.juneau.server.vars;

import org.apache.juneau.server.*;
import org.apache.juneau.svl.*;

/**
 * URL-encoding variable resolver.
 * <p>
 * The format for this var is <js>"$UE{innerValue}"</js>.
 * <p>
 * This variable takes the contents inside the variable and replaces it with a value returned by calling {@link RestUtils#encode(String)}).
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * @see org.apache.juneau.svl
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class UrlEncodeVar extends SimpleVar {

	/**
	 * Constructor.
	 */
	public UrlEncodeVar() {
		super("UE");
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return RestUtils.encode(key);
	}
}