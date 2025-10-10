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

import static org.apache.juneau.common.utils.Utils.*;

/**
 * Interface for the resolution of vars that consist of a comma-delimited list.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<js>"$X{foo, bar, baz}"</js>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SimpleVariableLanguageBasics">Simple Variable Language Basics</a>
 * </ul>
 */
public abstract class MultipartVar extends SimpleVar {

	/**
	 * Constructor.
	 *
	 * @param name The name of this variable.
	 */
	public MultipartVar(String name) {
		super(name);
	}

	/**
	 * The interface that needs to be implemented for this interface.
	 *
	 * @param session The session object used for a single instance of a string resolution.
	 * @param args The arguments inside the variable.
	 * @return The resolved variable.
	 */
	public abstract String resolve(VarResolverSession session, String[] args);

	@Override /* Var */
	public String resolve(VarResolverSession session, String s) {
		String[] s2 = s.indexOf(',') == -1 ? new String[]{s.trim()} : splita(s);
		return resolve(session, s2);
	}
}
