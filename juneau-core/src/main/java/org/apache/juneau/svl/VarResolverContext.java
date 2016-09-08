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

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Configurable properties on the {@link VarResolver} class.
 *	<p>
 *	Used to associate {@link Var Vars} and context objects with {@link VarResolver VarResolvers}.
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * @see org.apache.juneau.svl
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class VarResolverContext extends Context {

	/**
	 * An explicit list of Java classes to be excluded from consideration as being beans (<code>Set&lt;Class&gt;</code>).
	 * <p>
	 * Not-bean classes are typically converted to <code>Strings</code> during serialization even if they
	 * appear to be bean-like.
	 */
	public static final String SVL_vars = "Svl.vars.set";

	/**
	 * Add to the list of packages whose classes should not be considered beans.
	 */
	public static final String SVL_vars_add = "Svl.vars.set.add";

	/**
	 * Remove from the list of packages whose classes should not be considered beans.
	 */
	public static final String SVL_vars_remove = "Svl.vars.set.remove";

	/**
	 * Context objects associated with the resolver (<code>Map$lt;String,Object&gt;</code>).
	 */
	public static final String SVL_context = "Svl.context.map";

	/**
	 * Adds a new map entry to the {@link #SVL_context} property.
	 */
	public static final String SVL_context_put = "Svl.context.map.put";


	// Map of Vars added through addVar() method.
	private final Map<String,Var> stringVars;

	private final Map<String,Object> contextObjects;


	/**
	 * Constructor.
	 *
	 * @param cf The context factory to copy from.
	 */
	public VarResolverContext(ContextFactory cf) {
		super(cf);
		ContextFactory.PropertyMap pm = cf.getPropertyMap("Svl");

		Class<?>[] varClasses = pm.get(SVL_vars, Class[].class, new Class[0]);
		Map<String,Var> m = new ConcurrentSkipListMap<String,Var>();
		for (Class<?> c : varClasses) {
			if (! ClassUtils.isParentClass(Var.class, c))
				throw new RuntimeException("Invalid variable class.  Must extend from Var");
			try {
				Var v = (Var)c.newInstance();
				m.put(v.getName(), v);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		this.stringVars = Collections.unmodifiableMap(m);

		this.contextObjects = Collections.unmodifiableMap(pm.getMap(SVL_context, String.class, Object.class, Collections.<String,Object>emptyMap()));
	}

	/**
	 * Returns an unmodifiable map of {@link Var Vars} associated with this context.
	 *
	 * @return A map whose keys are var names (e.g. <js>"S"</js>) and values are {@link Var} instances.
	 */
	protected Map<String,Var> getVars() {
		return stringVars;
	}

	/**
	 * Returns the context object with the specified name.
	 *
	 * @param name The name of the context object.
	 * @return The context object, or <jk>null</jk> if no context object is specified with that name.
	 */
	protected Object getContextObject(String name) {
		return contextObjects.get(name);
	}
}
