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

import static org.apache.juneau.internal.ClassUtils.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;

/**
 * Represents a simple namespace mapping between a simple name and URI.
 *
 * <p>
 * In general, the simple name will be used as the XML prefix mapping unless there are conflicts or prefix re-mappings
 * in the serializer.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.XmlDetails">XML Details</a> * </ul>
 */
@Bean(sort=true)
public final class Namespace {

	private static final ConcurrentHashMap<String,Namespace> CACHE = new ConcurrentHashMap<>();


	/**
	 * Create a {@link Namespace} with the specified name and URI.
	 *
	 * <p>
	 * Previously-encountered name/uri pairs return a cached copy.
	 *
	 * @param name The namespace name.  See {@link Namespace#getName()}.
	 * @param uri The namespace URI.  See {@link Namespace#getUri()}.
	 * @return The namespace object.
	 */
	public static Namespace of(String name, String uri) {
		String key = name + ":" + uri;
		Namespace n = CACHE.get(key);
		if (n == null) {
			n = new Namespace(key, name, uri);
			Namespace n2 = CACHE.putIfAbsent(key, n);
			return (n2 == null ? n : n2);
		}
		return n;
	}

	/**
	 * Create a {@link Namespace} from a <js>"name:uri"</js> string pair.
	 *
	 * @param key The key/pair string.
	 * @return The namespace object.
	 */
	public static Namespace of(String key) {
		Namespace n = CACHE.get(key);
		if (n != null)
			return n;
		int i = key.indexOf(':');
		if (i == -1)
			return of(key, null);
		if (key.startsWith("http://") || key.startsWith("https://"))
			return of(null, key);
		return of(key.substring(0, i).trim(), key.substring(i+1).trim());
	}

	/**
	 * Converts the specified object into a {@link Namespace} object.
	 *
	 * <p>
	 * Can be any of following types:
	 * <ul>
	 * 	<li>A {@link Namespace} object
	 * 	<li>A string containing a name/value pair of the form <js>"name:uri"</js>.
	 * </ul>
	 *
	 * @param o The input.
	 * @return The namespace object, or <jk>null</jk> if the input was <jk>null</jk> or an empty JSON object.
	 */
	public static Namespace create(Object o) {
		if (o == null)
			return null;
		if (o instanceof Namespace)
			return (Namespace)o;
		if (o instanceof CharSequence)
			return of(o.toString());
		throw new BasicRuntimeException("Invalid object type passed to Namespace.create(Object):  ''{0}''", className(o));
	}

	/**
	 * Converts the specified object into an array of {@link Namespace} object.
	 *
	 * <p>
	 * Can be any of following types:
	 * <ul>
	 * 	<li>A {@link Namespace} array
	 * 	<li>A comma-delimited string with key/value pairs of the form <js>"name:uri"</js>.
	 * 	<li>A <c>Collection</c> containing any of object that can be passed to {@link #createArray(Object)}.
	 * </ul>
	 *
	 * @param o The input.
	 * @return The namespace objects, or <jk>null</jk> if the input was <jk>null</jk> or an empty JSON object.
	 */
	@SuppressWarnings("rawtypes")
	public static Namespace[] createArray(Object o) {

		if (o instanceof Namespace[])
			return (Namespace[])o;

		if (o instanceof String[]) {
			String[] ss = (String[])o;
			Namespace[] n = new Namespace[ss.length];
			for (int i = 0; i < ss.length; i++)
				n[i] = create(ss[i]);
			return n;
		}

		if (o instanceof CharSequence) {
			String[] ss = StringUtils.split(o.toString());
			Namespace[] n = new Namespace[ss.length];
			for (int i = 0; i < ss.length; i++)
				n[i] = create(ss[i]);
			return n;
		}

		if (o instanceof Collection) {
			Collection c = (Collection)o;
			Namespace[] n = new Namespace[c.size()];
			int i = 0;
			for (Object o2 : c){
				if (o2 instanceof Namespace)
					n[i++] = (Namespace)o2;
				else if (o2 instanceof CharSequence)
					n[i++] = create(o2.toString());
				else
					throw new BasicRuntimeException("Invalid type passed to NamespaceFactory.createArray: ''{0}''", o);
			}
			return n;
		}

		throw new BasicRuntimeException("Invalid type passed to NamespaceFactory.createArray: ''{0}''", o);
	}


	final String key, name, uri;

	/**
	 * Constructor.
	 *
	 * @param name The short name of this schema.
	 * @param uri The URI of this schema.
	 */
	private Namespace(String key, String name, String uri) {
		this.key = key;
		this.name = name;
		this.uri = uri;
	}

	/**
	 * Returns the namespace name.
	 *
	 * @return The namespace name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the namespace URI.
	 *
	 * @return The namespace URI.
	 */
	public String getUri() {
		return uri;
	}

	@Override /* Object */
	public String toString() {
		return key;
	}
}
