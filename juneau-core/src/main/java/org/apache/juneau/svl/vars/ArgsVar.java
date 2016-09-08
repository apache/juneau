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
import org.apache.juneau.utils.*;

/**
 * JVM args variable resolver.
 * <p>
 * The format for this var is <js>"$ARG{argNameOrNum}"</js> or <js>"$ARG{argNameOrNum,defaultValue}"</js>
 * <p>
 * This variable resolver requires that an {@link Args} object be set as a context object on the resolver or a
 * 	session object on the resolver session.
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	<jc>// Create an args object from the main(String[]) method.</jc>
 * 	Args args = new Args(argv);
 *
 * 	<jc>// Create a variable resolver that resolves JVM arguments (e.g. "$ARG{1}")</jc>
 * 	VarResolver r = <jk>new</jk> VarResolver().addVars(ArgsVar.<js>class</js>).addContextObject(<jsf>SESSION_args</jsf>, args);
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"Arg #1 is set to $ARG{1}"</js>));
 * </p>
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * @see org.apache.juneau.utils.Args
 * @see org.apache.juneau.svl
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class ArgsVar extends DefaultingVar {

	/**
	 * The name of the session or context object that identifies the {@link Args} object.
	 */
	public static final String SESSION_args = "args";

	/**
	 * Constructor.
	 */
	public ArgsVar() {
		super("ARG");
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return session.getSessionObject(Args.class, SESSION_args).getArg(key);
	}
}
