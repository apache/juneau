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
package org.apache.juneau;

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Class used to add properties to a {@link PropertyStore} from an annotation (e.g. {@link BeanConfig}).
 *
 * @param <T> The annotation that this <c>ConfigApply</c> reads from.
 */
public abstract class ConfigApply<T extends Annotation> {

	private final VarResolverSession r;
	private final Class<T> c;

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The string resolver to use for resolving strings.
	 */
	protected ConfigApply(Class<T> c, VarResolverSession r) {
		this.r = r == null ? VarResolver.DEFAULT.createSession() : r;
		this.c = c;
	}

	/**
	 * Apply the specified annotation to the specified property store builder.
	 *
	 * @param a The annotation.
	 * @param ps The property store builder.
	 */
	public abstract void apply(AnnotationInfo<T> a, PropertyStoreBuilder ps);


	/**
	 * Resolves the specified string.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved string.
	 */
	protected String string(String in) {
		return r.resolve(in);
	}

	/**
	 * Resolves the specified strings in the string array.
	 *
	 * @param in The string array containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected String[] strings(String[] in) {
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++)
			out[i] = r.resolve(in[i]);
		return out;
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected String[] strings(String in) {
		in = r.resolve(in);
		return StringUtils.split(in);
	}

	/**
	 * Resolves the specified strings as a maps of strings-to-strings.
	 *
	 * @param in The string array containing variables to resolve.
	 * @param loc The annotation field name.
	 * @return A map of strings-to-strings.
	 */
	protected Map<String,String> stringsMap(String[] in, String loc) {
		Map<String,String> m = new LinkedHashMap<>();
		for (String s : strings(in)) {
			for (String s2 : split(s, ';')) {
				int i = s2.indexOf(':');
				if (i == -1)
					throw new ConfigException("Invalid syntax for key/value pair on annotation @{0}({1}): {2}", c.getSimpleName(), loc, s2);
				m.put(s2.substring(0, i).trim(), s2.substring(i+1).trim());
			}
		}
		return m;
	}

	/**
	 * Resolves the specified string and converts it to a boolean.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved boolean.
	 */
	public boolean bool(String in) {
		return Boolean.parseBoolean(r.resolve(in));
	}

	/**
	 * Resolves the specified string and converts it to an int.
	 *
	 * @param in The string containing variables to resolve.
	 * @param loc The annotation field name.
	 * @return The resolved int.
	 */
	protected int integer(String in, String loc) {
		try {
			return Integer.parseInt(r.resolve(in));
		} catch (NumberFormatException e) {
			throw new ConfigException("Invalid syntax for integer on annotation @{0}({1}): {2}", c.getSimpleName(), loc, in);
		}
	}

	/**
	 * Resolves the specified string and converts it to a Visibility.
	 *
	 * @param in The string containing variables to resolve.
	 * @param loc The annotation field name.
	 * @return The resolved Visibility.
	 */
	protected Visibility visibility(String in, String loc) {
		try {
			return Visibility.valueOf(r.resolve(in));
		} catch (IllegalArgumentException e) {
			throw new ConfigException("Invalid syntax for visibility on annotation @{0}({1}): {2}", c.getSimpleName(), loc, in);
		}
	}

	/**
	 * Resolves the specified strings and converts it to an ObjectMap.
	 *
	 * @param in The strings to be concatenated and parsed into an ObjectMapl.
	 * @param loc The annotation field name.
	 * @return The resolved ObjectMap.
	 */
	protected ObjectMap objectMap(String[] in, String loc) {
		return objectMap(joinnl(strings(in)), loc);
	}

	/**
	 * Resolves the specified string and converts it to an ObjectMap.
	 *
	 * @param in The string to be parsed into an ObjectMapl.
	 * @param loc The annotation field name.
	 * @return The resolved ObjectMap.
	 */
	protected ObjectMap objectMap(String in, String loc) {
		try {
			if (! isObjectMap(in, true))
				in = "{" + in + "}";
			return new ObjectMap(in);
		} catch (Exception e) {
			throw new ConfigException("Invalid syntax for Simple-JSON on annotation @{0}({1}): {2}", c.getSimpleName(), loc, in);
		}
	}
}
