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

import static org.apache.juneau.common.internal.StringUtils.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.svl.*;

/**
 * JVM args variable resolver.
 *
 * <p>
 * The format for this var is <js>"$A{arg[,default]}"</js>.
 *
 * <p>
 * This variable resolver requires that the command-line arguments be made available through any of the following:
 * <ul class='spaced-list'>
 * 	<li><js>"sun.java.command"</js> system property.
 * 	<li><js>"juneau.args"</js> system property.
 * 	<li>{@link #init(Args)} has been called.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an args object from the main(String[]) method.</jc>
 * 	Args <jv>args</jv> = <jk>new</jk> Args(<jv>argv</jv>);
 *
 * 	ArgsVar.<jsm>init</jsm>(<jv>args</jv>);
 *
 * 	<jc>// Create a variable resolver that resolves JVM arguments (e.g. "$A{1}")</jc>
 * 	VarResolver <jv>varResolver</jv> = VarResolver.<jsm>create</jsm>().vars(ArgsVar.<jk>class</jk>).build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(<jv>varResolver</jv>.resolve(<js>"Arg #1 is set to $A{1}"</js>));
 * </p>
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
 */
public class ArgsVar extends DefaultingVar {

	/** The name of this variable. */
	public static final String NAME = "A";

	private static volatile Args ARGS;

	/**
	 * Initialize the args for this variable.
	 *
	 * @param args The parsed command-line arguments.
	 */
	public static void init(Args args) {
		ARGS = args;
	}

	private final Args args;

	/**
	 * Constructor.
	 */
	public ArgsVar() {
		super(NAME);
		if (ARGS != null)
			this.args = ARGS;
		else {
			String s = System.getProperty("sun.java.command");
			if (isNotEmpty(s)) {
				int i = s.indexOf(' ');
				args = new Args(i == -1 ? "" : s.substring(i+1));
			} else {
				args = new Args(System.getProperty("juneau.args", ""));
			}
		}
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return args.getArg(key);
	}
}
