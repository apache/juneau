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

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Configurable properties on the {@link VarResolver} class.
 * <p>
 * Used to associate {@link Var Vars} and context objects with {@link VarResolver VarResolvers}.
 *
 * @see org.apache.juneau.svl
 */
public class VarResolverContext {

	private final Class<?>[] vars;
	private final Map<String,Var> varMap;
	private final Map<String,Object> contextObjects;

	/**
	 * Constructor.
	 *
	 * @param vars The Var classes used for resolving string variables.
	 * @param contextObjects Read-only context objects.
	 */
	public VarResolverContext(Class<? extends Var>[] vars, Map<String,Object> contextObjects) {

		this.vars = Arrays.copyOf(vars, vars.length);

		Map<String,Var> m = new ConcurrentSkipListMap<String,Var>();
		for (Class<?> c : vars) {
			if (! isParentClass(Var.class, c))
				throw new RuntimeException("Invalid variable class.  Must extend from Var");
			Var v = newInstance(Var.class, c);
			m.put(v.getName(), v);
		}

		this.varMap = Collections.unmodifiableMap(m);
		this.contextObjects = contextObjects == null ? null : Collections.unmodifiableMap(new ConcurrentHashMap<String,Object>(contextObjects));
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
	protected Class<?>[] getVars() {
		return Arrays.copyOf(vars, vars.length);
	}

	/**
	 * Returns the context object with the specified name.
	 *
	 * @param name The name of the context object.
	 * @return The context object, or <jk>null</jk> if no context object is specified with that name.
	 */
	protected Object getContextObject(String name) {
		return contextObjects == null ? null : contextObjects.get(name);
	}

	/**
	 * Returns the context map of this variable resolver context.
	 *
	 * @return An unmodifiable map of the context objects of this variable resolver context.
	 */
	protected Map<String,Object> getContextObjects() {
		return contextObjects;
	}
}
