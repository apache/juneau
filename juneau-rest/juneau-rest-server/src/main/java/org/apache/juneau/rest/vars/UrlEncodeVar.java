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
package org.apache.juneau.rest.vars;

import static org.apache.juneau.common.internal.StringUtils.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.svl.*;

/**
 * URL-encoding variable resolver.
 *
 * <p>
 * The format for this var is <js>"$UE{uriPart}"</js>.
 *
 * <p>
 * This variable takes the contents inside the variable and replaces it with a value returned by calling
 * {@link StringUtils#urlEncode(String)}).
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
 * </ul>
 */
public class UrlEncodeVar extends SimpleVar {

	/** The name of this variable. */
	public static final String NAME = "UE";

	/**
	 * Constructor.
	 */
	public UrlEncodeVar() {
		super(NAME);
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return urlEncode(key);
	}
}