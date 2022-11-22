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

import org.apache.juneau.collections.*;

/**
 * Subclass of an {@link JsonMap} that automatically resolves any SVL variables in values.
 *
 * <p>
 * Resolves variables in the following values:
 * <ul>
 * 	<li>Values of type {@link CharSequence}.
 * 	<li>Arrays containing values of type {@link CharSequence}.
 * 	<li>Collections containing values of type {@link CharSequence}.
 * 	<li>Maps containing values of type {@link CharSequence}.
 * </ul>
 *
 * <p>
 * All other data types are left as-is.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
 */
@SuppressWarnings({"serial"})
public class ResolvingJsonMap extends JsonMap {

	private final VarResolverSession varResolver;

	/**
	 * Constructor.
	 *
	 * @param varResolver The var resolver session to use for resolving SVL variables.
	 */
	public ResolvingJsonMap(VarResolverSession varResolver) {
		super();
		this.varResolver = varResolver;
	}

	@Override /* Map */
	public Object get(Object key) {
		return varResolver.resolve(super.get(key));
	}
}
