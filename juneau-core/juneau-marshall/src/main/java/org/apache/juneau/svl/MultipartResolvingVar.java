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
package org.apache.juneau.svl;

import static org.apache.juneau.common.internal.StringUtils.*;

/**
 * Interface for the resolution of vars that can have one or more keys where the first non-null resolution is returned.
 *
 * <p>
 * For example, to resolve the system property <js>"myProperty"</js> but then resolve <js>"myProperty2"</js> if the
 * property doesn't exist: <js>"$S{myProperty1,myProperty2}"</js>
 *
 * <p>
 * Subclasses must implement the following method:
 * <ul class='javatree'>
 * 	<li class='jm'>{@link #resolve(VarResolverSession, String)}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
 */
public abstract class MultipartResolvingVar extends SimpleVar {

	/**
	 * Constructor.
	 *
	 * @param name The name of this variable.
	 */
	public MultipartResolvingVar(String name) {
		super(name);
	}

	@Override /* Var*/
	public String doResolve(VarResolverSession session, String s) throws Exception {
		int i = s.indexOf(',');
		if (i == -1)
			return resolve(session, s.trim());
		for (String s2 : split(s)) {
			String v = resolve(session, s2);
			if (v != null)
				return v;
		}
		return null;
	}
}
