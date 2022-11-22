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
package org.apache.juneau.internal;

import static org.apache.juneau.internal.CollectionUtils.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;

/**
 * Utility methods for accessing system properties and environment variables.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class SystemEnv {

	/**
	 * Looks up a system property or environment variable.
	 *
	 * <p>
	 * First looks in system properties.  Then converts the name to env-safe and looks in the system environment.
	 * Then returns the default if it can't be found.
	 *
	 * @param <T> The type to convert the value to.
	 * @param name The property name.
	 * @param def The default value if not found.
	 * @return The default value.
	 */
	public static <T> T env(String name, T def) {
		return env(name).map(x -> toType(x, def)).orElse(def);
	}

	/**
	 * Looks up a system property or environment variable.
	 *
	 * <p>
	 * First looks in system properties.  Then converts the name to env-safe and looks in the system environment.
	 * Then returns the default if it can't be found.
	 *
	 * @param name The property name.
	 * @return The value if found.
	 */
	public static Optional<String> env(String name) {
		String s = System.getProperty(name);
		if (s == null)
			s = System.getenv(envName(name));
		return optional(s);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> T toType(String s, T def) {
		if (s == null || def == null)
			return null;
		Class<T> c = (Class<T>)def.getClass();
		if (c == String.class)
			return (T)s;
		if (c.isEnum())
			return (T)Enum.valueOf((Class<? extends Enum>) c, s);
		Function<String,T> f = (Function<String,T>)ENV_FUNCTIONS.get(c);
		if (f == null)
			throw new BasicRuntimeException("Invalid env type: {0}", c);
		return f.apply(s);
	};

	private static final Map<Class<?>,Function<String,?>> ENV_FUNCTIONS = new IdentityHashMap<>();
	static {
		ENV_FUNCTIONS.put(Boolean.class, x -> Boolean.valueOf(x));
		ENV_FUNCTIONS.put(Charset.class, x -> Charset.forName(x));
	}

	private static final ConcurrentHashMap<String,String> PROPERTY_TO_ENV = new ConcurrentHashMap<>();
	private static String envName(String name) {
		String name2 = PROPERTY_TO_ENV.get(name);
		if (name2 == null) {
			name2 = name.toUpperCase().replace(".", "_");
			PROPERTY_TO_ENV.put(name, name2);
		}
		return name2;
	}
}
