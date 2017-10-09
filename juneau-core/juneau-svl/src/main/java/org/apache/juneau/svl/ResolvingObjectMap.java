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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Subclass of an {@link ObjectMap} that automatically resolves any SVL variables in values.
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
 */
@SuppressWarnings({"serial","unchecked","rawtypes"})
public class ResolvingObjectMap extends ObjectMap {

	private final VarResolverSession varResolver;

	/**
	 * Constructor.
	 *
	 * @param varResolver The var resolver session to use for resolving SVL variables.
	 */
	public ResolvingObjectMap(VarResolverSession varResolver) {
		super();
		this.varResolver = varResolver;
	}

	@Override /* Map */
	public Object get(Object key) {
		return resolve(super.get(key));
	}

	private Object resolve(Object o) {
		if (o == null)
			return null;
		if (o instanceof CharSequence)
			return varResolver.resolve(o.toString());
		if (o.getClass().isArray()) {
			if (! containsVars(o))
				return o;
			Object o2 = Array.newInstance(o.getClass().getComponentType(), Array.getLength(o));
			for (int i = 0; i < Array.getLength(o); i++)
				Array.set(o2, i, resolve(Array.get(o, i)));
			return o2;
		}
		if (o instanceof Collection) {
			try {
				Collection c = (Collection)o;
				if (! containsVars(c))
					return o;
				Collection c2 = c.getClass().newInstance();
				for (Object o2 : c)
					c2.add(resolve(o2));
				return c2;
			} catch (Exception e) {
				return o;
			}
		}
		if (o instanceof Map) {
			try {
				Map m = (Map)o;
				if (! containsVars(m))
					return o;
				Map m2 = m.getClass().newInstance();
				for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
					m2.put(e.getKey(), resolve(e.getValue()));
				return m2;
			} catch (Exception e) {
				return o;
			}
		}
		return o;
	}

	private static boolean containsVars(Object array) {
		for (int i = 0; i < Array.getLength(array); i++) {
			Object o = Array.get(array, i);
			if (o instanceof CharSequence && o.toString().contains("$"))
				return true;
		}
		return false;
	}

	private static boolean containsVars(Collection c) {
		for (Object o : c)
			if (o instanceof CharSequence && o.toString().contains("$"))
				return true;
		return false;
	}

	private static boolean containsVars(Map m) {
		for (Object o : m.values())
			if (o instanceof CharSequence && o.toString().contains("$"))
				return true;
		return false;
	}
}
