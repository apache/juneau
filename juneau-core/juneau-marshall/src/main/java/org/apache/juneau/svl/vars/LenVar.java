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

import java.util.regex.*;

import org.apache.juneau.svl.*;

/**
 * A transformational variable resolver that returns character count or list count (using given delimiter).
 *
 * <p>
 * The format for this var is one of the following:
 * <ul>
 * 	<li><js>"$LN{arg}"</js>
 * 	<li><js>"$LN{arg,delimiter}"</js>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a variable resolver that resolves system properties and $SW vars.</jc>
 * 	VarResolver <jv>varResolver</jv> = VarResolver.<jsm>create</jsm>().vars(LenVar.<jk>class</jk>, SystemPropertiesVar.<jk>class</jk>).build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(<jv>varResolver</jv>.resolve(<js>"Parts = $LN{$P{os.version},.} Chars = $LN{$P{os.version}}"</js>));
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
public class LenVar extends MultipartVar {

	/** The name of this variable. */
	public static final String NAME = "LN";

	/**
	 * Constructor.
	 */
	public LenVar() {
		super(NAME);
	}

	@Override /* MultipartVar */
	public String resolve(VarResolverSession session, String[] args) {
		assertArg(args.length <= 2, "Invalid number of arguments passed to $LN var.  Must have 1 or 2 arguments.");

		int len = 0;
		String stringArg = args[0];
		if (args.length == 1)
			len = stringArg.length();
		else if (args.length == 2) {
			//delimiter is given
			String delimiter = Pattern.quote(args[1]);
			len = stringArg.trim().split(delimiter).length;
		}
		return String.valueOf(len);
	}
}
