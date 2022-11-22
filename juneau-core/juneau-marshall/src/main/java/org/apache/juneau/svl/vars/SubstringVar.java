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

import static org.apache.juneau.common.internal.ArgUtils.*;

import org.apache.juneau.svl.*;

/**
 * A transformational variable resolver that returns substring for given start and end (optional)
 *
 * <p>
 * The format for this var is one of the following:
 * <ul>
 * 	<li><js>"$ST{arg,start}"</js>
 * 	<li><js>"$ST{arg,start,end}"</js>
 * </ul>
 *
 * <p>
 *
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a variable resolver that resolves system properties and $SW vars.</jc>
 * 	VarResolver <jv>varResolver</jv> = VarResolver.<jsm>create</jsm>().vars(SubstringVar.<jk>class</jk>, SystemPropertiesVar.<jk>class</jk>).build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(<jv>varResolver</jv>.resolve(<js>"Version = $ST{$P{java.version}, 0, 3}"</js>));
 * </p>
 *
 * <p>
 * Since this is a {@link MultipartVar}, any variables contained in the result will be recursively resolved.
 * <br>Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
 */
public class SubstringVar extends MultipartVar {

	/** The name of this variable. */
	public static final String NAME = "ST";

	/**
	 * Constructor.
	 */
	public SubstringVar() {
		super(NAME);
	}

	@Override /* MultipartVar */
	public String resolve(VarResolverSession session, String[] args) {
		assertArg(args.length >= 2 && args.length <= 3, "Invalid number of arguments passed to $ST var.  Must have 2 or 3 arguments.");

		String stringArg = args[0];
		String result = "";
		if (args.length == 2) {
			int start = Integer.parseInt(args[1]);
			if (start >= 0 && start <= stringArg.length())
				result = stringArg.substring(start);
			if (start < 0 && -start <= stringArg.length())
				result = stringArg.substring(stringArg.length() + start);
		}
		else if (args.length == 3) {
			int start = Integer.parseInt(args[1]);
			int end = Integer.parseInt(args[2]);
			if (start >= 0 && start < stringArg.length() && end >= 0 && end <= stringArg.length() && start < end)
				result = stringArg.substring(start, end);
		}
		return result;
	}
}
