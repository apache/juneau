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
package org.apache.juneau.xml;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * Factory class for getting unique instances of {@link Namespace} objects.
 *
 * <p>
 * For performance reasons, {@link Namespace} objects are stored in {@link IdentityList IdentityLists}.
 * For this to work property, namespaces with the same name and URI must only be represented by a single
 * {@link Namespace} instance.
 * This factory class ensures this identity uniqueness.
 */
public final class NamespaceFactory {

	private static ConcurrentHashMap<String,Namespace> cache = new ConcurrentHashMap<String,Namespace>();

	/**
	 * Get the {@link Namespace} with the specified name and URI, and create a new one if this is the first time it's
	 * been encountered.
	 *
	 * @param name The namespace name.  See {@link Namespace#getName()}.
	 * @param uri The namespace URI.  See {@link Namespace#getUri()}.
	 * @return The namespace object.
	 */
	public static Namespace get(String name, String uri) {
		String key = name + "+" + uri;
		Namespace n = cache.get(key);
		if (n == null) {
			n = new Namespace(name, uri);
			Namespace n2 = cache.putIfAbsent(key, n);
			return (n2 == null ? n : n2);
		}
		return n;
	}

	/**
	 * Converts the specified object into a {@link Namespace} object.
	 *
	 * <p>
	 * Can be any of following types:
	 * <ul>
	 * 	<li>A {@link Namespace} object
	 * 	<li>A JSON string containing a single key/value pair indicating the name/URI mapping.
	 * 	<li>A <code>Map</code> containing a single key/value pair indicating the name/URI mapping.
	 * </ul>
	 *
	 * @param o The input.
	 * @return The namespace object, or <jk>null</jk> if the input was <jk>null</jk> or an empty JSON object.
	 */
	@SuppressWarnings("rawtypes")
	public static Namespace parseNamespace(Object o) {
		if (o == null)
			return null;
		if (o instanceof Namespace)
			return (Namespace)o;
		try {
			Map<?,?> m = (o instanceof Map ? (Map)o : new ObjectMap(o.toString()));
			if (m.size() == 0)
				return null;
			if (m.size() > 1)
				throw new RuntimeException("Too many namespaces specified.  Only one allowed. '"+o+"'");
			Map.Entry<?,?> e = m.entrySet().iterator().next();
			return get(e.getKey().toString(), e.getValue().toString());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts the specified object into an array of {@link Namespace} object.
	 *
	 * <p>
	 * Can be any of following types:
	 * <ul>
	 * 	<li>A {@link Namespace} array
	 * 	<li>A JSON string with key/value pairs indicating name/URI pairs.
	 * 	<li>A <code>Map</code> with key/value pairs indicating name/URI pairs.
	 * 	<li>A <code>Collection</code> containing any of object that can be passed to {@link #parseNamespace(Object)}.
	 * </ul>
	 *
	 * @param o The input.
	 * @return The namespace objects, or <jk>null</jk> if the input was <jk>null</jk> or an empty JSON object.
	 */
	@SuppressWarnings("rawtypes")
	public static Namespace[] parseNamespaces(Object o) {
		try {
			if (o instanceof Namespace[])
				return (Namespace[])o;

			if (o instanceof CharSequence)
				o = new ObjectMap(o.toString());

			Namespace[] n;
			int i = 0;
			if (o instanceof Collection) {
				Collection c = (Collection)o;
				n = new Namespace[c.size()];
				for (Object o2 : c)
					n[i++] = parseNamespace(o2);
			} else if (o instanceof Map) {
				Map<?,?> m = (Map<?,?>)o;
				n = new Namespace[m.size()];
				for (Map.Entry e : m.entrySet())
					n[i++] = get(e.getKey().toString(), e.getValue().toString());
			} else {
				throw new RuntimeException("Invalid type passed to NamespaceFactory.listFromObject: '"+o+"'");
			}
			return n;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
