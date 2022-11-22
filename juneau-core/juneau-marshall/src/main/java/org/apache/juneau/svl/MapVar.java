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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;

import java.util.*;

/**
 * A subclass of {@link DefaultingVar} that simply pulls values from a {@link Map}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
 */
@SuppressWarnings("rawtypes")
public abstract class MapVar extends DefaultingVar {

	private final Map m;

	/**
	 * Constructor.
	 *
	 * @param name The name of this variable.
	 * @param m The map to pull values from.
	 */
	public MapVar(String name, Map m) {
		super(name);
		assertArgNotNull("m", m);
		this.m = m;
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String varVal) {
		return stringify(m.get(varVal));
	}
}
