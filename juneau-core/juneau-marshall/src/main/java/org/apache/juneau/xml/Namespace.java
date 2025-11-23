/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.xml;

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;

/**
 * Represents a simple namespace mapping between a simple name and URI.
 *
 * <p>
 * In general, the simple name will be used as the XML prefix mapping unless there are conflicts or prefix re-mappings
 * in the serializer.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlBasics">XML Basics</a>
 * </ul>
 */
@Bean(sort = true)
public class Namespace {

	private static final ConcurrentHashMap<String,Namespace> CACHE = new ConcurrentHashMap<>();

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
		if (o instanceof Namespace o2)
			return o2;
		if (o instanceof CharSequence o3)
			return of(o3.toString());
		throw rex("Invalid object type passed to Namespace.create(Object):  ''{0}''", cn(o));
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
	public static Namespace[] createArray(Object o) {

		if (o instanceof Namespace[])
			return (Namespace[])o;

		if (o instanceof String[]) {
			var ss = (String[])o;
			var n = new Namespace[ss.length];
			for (var i = 0; i < ss.length; i++)
				n[i] = create(ss[i]);
			return n;
		}

		if (o instanceof CharSequence o2) {
			var ss = splita(o2.toString());
			var n = new Namespace[ss.length];
			for (var i = 0; i < ss.length; i++)
				n[i] = create(ss[i]);
			return n;
		}

		if (o instanceof Collection o2) {
			var n = new Namespace[o2.size()];
			var i = 0;
			for (var o3 : o2) {
				if (o3 instanceof Namespace o4)
					n[i++] = o4;
				else if (o3 instanceof CharSequence o4)
					n[i++] = create(o4.toString());
				else
					throw rex("Invalid type passed to NamespaceFactory.createArray: ''{0}''", cn(o));
			}
			return n;
		}

		throw rex("Invalid type passed to NamespaceFactory.createArray: ''{0}''", cn(o));
	}

	/**
	 * Create a {@link Namespace} from a <js>"name:uri"</js> string pair.
	 *
	 * @param key The key/pair string.
	 * @return The namespace object.
	 */
	public static Namespace of(String key) {
		var n = CACHE.get(key);
		if (nn(n))
			return n;
		var i = key.indexOf(':');
		if (i == -1)
			return of(key, null);
		if (key.startsWith("http://") || key.startsWith("https://"))
			return of(null, key);
		return of(key.substring(0, i).trim(), key.substring(i + 1).trim());
	}

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
		var key = name + ":" + uri;
		var n = CACHE.get(key);
		if (n == null) {
			n = new Namespace(key, name, uri);
			var n2 = CACHE.putIfAbsent(key, n);
			return (n2 == null ? n : n2);
		}
		return n;
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
	public String getName() { return name; }

	/**
	 * Returns the namespace URI.
	 *
	 * @return The namespace URI.
	 */
	public String getUri() { return uri; }

	@Override /* Overridden from Object */
	public String toString() {
		return key;
	}
}