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
package org.apache.juneau.svl.vars;

import org.apache.juneau.svl.*;

/**
 * Environment variable variable resolver.
 * <p>
 * The format for this var is <js>"$E{envVar}"</js> or <js>"$E{envVar,defaultValue}"</js>
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	<jc>// Create a variable resolver that resolves environment variables (e.g. "$E{PATH}")</jc>
 * 	VarResolver r = <jk>new</jk> VarResolver().addVars(EnvVariablesVar.<js>class</js>);
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"Environment variable PATH is set to $E{PATH}"</js>));
 * </p>
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * @see org.apache.juneau.svl
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class EnvVariablesVar extends DefaultingVar {

	/**
	 * Constructor.
	 */
	public EnvVariablesVar() {
		super("E");
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String varVal) {
		// Note that lookup is case-insensitive on windows.
		return System.getenv(varVal);
	}
}