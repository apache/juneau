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
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Class used to add properties to a {@link ContextProperties} from an annotation (e.g. {@link BeanConfig}).
 *
 * @param <T> The annotation that this <c>ConfigApply</c> reads from.
 */
public abstract class ConfigApply<T extends Annotation> {

	private final VarResolverSession vr;
	private final Class<T> c;

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param vr The string resolver to use for resolving strings.
	 */
	protected ConfigApply(Class<T> c, VarResolverSession vr) {
		this.vr = vr == null ? VarResolver.DEFAULT.createSession() : vr;
		this.c = c;
	}

	/**
	 * Apply the specified annotation to the specified property store builder.
	 *
	 * @param a The annotation.
	 * @param cpb The property store builder.
	 */
	public abstract void apply(AnnotationInfo<T> a, ContextPropertiesBuilder cpb);

	/**
	 * Returns the var resolver session for this apply.
	 *
	 * @return The var resolver session for this apply.
	 */
	protected VarResolverSession vr() {
		return vr;
	}

	/**
	 * Resolves the specified string.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved string.
	 */
	protected String string(String in) {
		in = vr.resolve(in);
		return isEmpty(in) ? null : in;
	}

	/**
	 * Resolves the specified strings in the string array.
	 *
	 * @param in The string array containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected List<String> stringList(String[] in) {
		return stringStream(in).collect(Collectors.toList());
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected Stream<String> stringStream(String[] in) {
		return Arrays.asList(in).stream().map(x -> vr.resolve(x)).filter(x -> !StringUtils.isEmpty(x));
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected List<String> cdList(String in) {
		return cdStream(in).collect(Collectors.toList());
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected Stream<String> cdStream(String in) {
		return Arrays.asList(StringUtils.split(vr.resolve(in))).stream().filter(x -> !StringUtils.isEmpty(x));
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
		for (String s : stringList(in)) {
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
	public Boolean bool(String in) {
		in = string(in);
		return in == null ? null : Boolean.parseBoolean(in);
	}

	/**
	 * Resolves the specified string and converts it to an int.
	 *
	 * @param in The string containing variables to resolve.
	 * @param loc The annotation field name.
	 * @return The resolved int.
	 */
	protected Integer integer(String in, String loc) {
		try {
			in = string(in);
			return in == null ? null : Integer.parseInt(in);
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
			in = string(in);
			return in == null ? null : Visibility.valueOf(in);
		} catch (IllegalArgumentException e) {
			throw new ConfigException("Invalid syntax for visibility on annotation @{0}({1}): {2}", c.getSimpleName(), loc, in);
		}
	}

	/**
	 * Resolves the specified strings and converts it to an OMap.
	 *
	 * @param in The strings to be concatenated and parsed into an OMap.
	 * @param loc The annotation field name.
	 * @return The resolved OMap.
	 */
	protected OMap omap(String[] in, String loc) {
		return omap(joinnl(stringList(in)), loc);
	}

	/**
	 * Resolves the specified string and converts it to an OMap.
	 *
	 * @param in The string to be parsed into an OMap.
	 * @param loc The annotation field name.
	 * @return The resolved OMap.
	 */
	protected OMap omap(String in, String loc) {
		try {
			if (! isJsonObject(in, true))
				in = "{" + in + "}";
			return OMap.ofJson(in);
		} catch (Exception e) {
			throw new ConfigException("Invalid syntax for Simple-JSON on annotation @{0}({1}): {2}", c.getSimpleName(), loc, in);
		}
	}

	/**
	 * Convenience method for detecting if an array is empty.
	 *
	 * @param value The array to check.
	 * @return <jk>true</jk> if the specified array is empty.
	 */
	protected boolean isEmpty(Object value) {
		return ObjectUtils.isEmpty(value);
	}

	/**
	 * Represents a no-op configuration apply.
	 */
	public static class NoOp extends ConfigApply<Annotation> {

		/**
		 * Constructor.
		 *
		 * @param r The string resolver to use for resolving strings.
		 */
		public NoOp(VarResolverSession r) {
			super(Annotation.class, r);
		}

		@Override /* ConfigApply */
		public void apply(AnnotationInfo<Annotation> ai, ContextPropertiesBuilder b) {}
	}
}
