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
 * Upper-case variable resolver.
 *
 * <p>
 * The format for this var is <js>"$NE{stringValue}"</js>.
 *
 * <p>
 * This variable returns <js>"true"</js> if the contents are not empty.
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
 */
public class NotEmptyVar extends SimpleVar {

	/** The name of this variable. */
	public static final String NAME = "NE";

	/**
	 * Constructor.
	 */
	public NotEmptyVar() {
		super(NAME);
	}

	@Override /* Parameter */
	public String resolve(VarResolverSession session, String key) {
		return String.valueOf(! key.trim().isEmpty());
	}
}