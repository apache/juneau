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

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

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
 * <p class='bcode w800'>
 * 	<jc>// Create an args object from the main(String[]) method.</jc>
 * 	Args args = new Args(argv);
 *
 * 	ArgsVar.<jsm>init</jsm>(args);
 *
 * 	<jc>// Create a variable resolver that resolves JVM arguments (e.g. "$A{1}")</jc>
 * 	VarResolver r = <jk>new</jk> VarResolver().addVars(ArgsVar.<js>class</js>);
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"Arg #1 is set to $ARGS{1}"</js>));
 * </p>
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * @see org.apache.juneau.utils.Args
 * @see org.apache.juneau.svl
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
