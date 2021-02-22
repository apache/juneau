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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;

/**
 * Configurable properties on the {@link VarResolver} class.
 *
 * <p>
 * Used to associate {@link Var Vars} and context objects with {@link VarResolver VarResolvers}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc VarResolvers}
 * </ul>
 */
public class VarResolverContext {

	private final Var[] vars;
	private final Map<String,Var> varMap;
	final BeanStore beanStore;

	/**
	 * Constructor.
	 *
	 * @param vars The Var classes used for resolving string variables.
	 * @param beanStore Used to resolve beans needed by individual vars.
	 */
	public VarResolverContext(Var[] vars, BeanStore beanStore) {

		this.vars = vars;

		Map<String,Var> m = new ConcurrentSkipListMap<>();
		for (Var v : vars)
			m.put(v.getName(), v);

		this.varMap = AMap.unmodifiable(m);
		this.beanStore = BeanStore.of(beanStore);
	}

	/**
	 * Returns an unmodifiable map of {@link Var Vars} associated with this context.
	 *
	 * @return A map whose keys are var names (e.g. <js>"S"</js>) and values are {@link Var} instances.
	 */
	protected Map<String,Var> getVarMap() {
		return varMap;
	}

	/**
	 * Returns an array of variables define in this variable resolver context.
	 *
	 * @return A new array containing the variables in this context.
	 */
	protected Var[] getVars() {
		return Arrays.copyOf(vars, vars.length);
	}

	/**
	 * Adds a bean to this session.
	 *
	 * @param <T> The bean type.
	 * @param c The bean type.
	 * @param value The bean.
	 * @return This object (for method chaining).
	 */
	public <T> VarResolverContext addBean(Class<T> c, T value) {
		beanStore.addBean(c, value);
		return this;
	}
}
