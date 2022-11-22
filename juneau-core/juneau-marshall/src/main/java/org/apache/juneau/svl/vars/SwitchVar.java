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
import static org.apache.juneau.common.internal.StringUtils.*;

import java.util.regex.*;

import org.apache.juneau.svl.*;

/**
 * A basic switch/case logic variable resolver.
 *
 * <p>
 * The format for this var is one of the following:
 * <ul>
 * 	<li><js>"$SW{stringArg, pattern:thenValue[, pattern:thenValue...]}"</js>
 * 	<li>...
 * </ul>
 *
 * <p>
 * The pattern can be any string optionally containing <js>'*'</js> or <js>'?'</js> representing any or one character
 * respectively.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a variable resolver that resolves system properties and $SW vars.</jc>
 * 	VarResolver <jv>varResolver</jv> = VarResolver.<jsm>create</jsm>().vars(SwitchVar.<jk>class</jk>, SystemPropertiesVar.<jk>class</jk>).build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(<jv>varResolver</jv>.resolve(<js>"We are running on $SW{$P{os.name},*win*:Windows,*:Something else}!"</js>));
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
public class SwitchVar extends MultipartVar {

	/** The name of this variable. */
	public static final String NAME = "SW";

	/**
	 * Constructor.
	 */
	public SwitchVar() {
		super(NAME);
	}

	@Override /* MultipartVar */
	public String resolve(VarResolverSession session, String[] args) {
		assertArg(args.length >= 2, "Invalid number of arguments passed to $SW var.  Must have 2 or more arguments.");

		String stringArg = args[0];
		for (int i = 1; i < args.length; i++) {
			String pattern = args[i];

			String[] parts = split(pattern, ':', 2);
			assertArg(parts.length >= 2, "Invalid arguments passed to $SW var.  Each case statement must contains 'pattern:value'.");

			Pattern p = Pattern.compile(parts[0].replace("*", ".*").replace("?", "."));
			if (p.matcher(stringArg).matches())
				return parts[1];
		}

		// Nothing matched and no else clause.
		return "";
	}
}
