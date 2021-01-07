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

import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Utility class for getting system property values.
 */
public class SystemProperties {

	/**
	 * Returns the specified system property or environment variable value as the specified type.
	 *
	 * <p>
	 * First does a lookup on the system property with the specified property name (e.g. <js>"juneau.stackTraceCacheTimeout"</js>)
	 * <br>
	 * If not found, then searches it as an env var (e.g. <js>"JUNEAU_STACKTRACETIMEOUT"</js>).
	 * <br>
	 * If found, converts the string value using any number of string mutaters (e.g. public string constructors, static
	 * creators, etc...)
	 *
	 * @param <T> The type to convert the value to.
	 * @param c The type to convert the value to.
	 * @param propertyName The system property name.
	 * @param def The default value if not found.
	 * @return The converted property, or <jk>null</jk> if property not set.
	 */
	public static <T> T getProperty(Class<T> c, String propertyName, T def) {
		String p = getProperty(propertyName, null);
		if (p != null) {
			Mutater<String,T> t = Mutaters.get(String.class, c);
			return t == def ? null : t.mutate(p);
		}
		return def;
	}

	/**
	 * Returns the specified system property or environment variable value as the specified type.
	 *
	 * <p>
	 * First does a lookup on the system property with the specified property name (e.g. <js>"juneau.stackTraceCacheTimeout"</js>)
	 * <br>
	 * If not found, then searches it as an env var (e.g. <js>"JUNEAU_STACKTRACETIMEOUT"</js>).
	 *
	 * @param propertyName The system property name.
	 * @param def The default value if not found.
	 * @return The converted property, or <jk>null</jk> if property not set.
	 */
	public static String getProperty(String propertyName, String def) {
		propertyName = StringUtils.emptyIfNull(propertyName);
		String p = System.getProperty(propertyName);
		try {
			if (p == null)
				 p = System.getenv(propertyName.replace('.', '_').replace('-', '_').toUpperCase());
		} catch (SecurityException e) {}
		return p == null ? def : p;
	}

	/**
	 * Returns the specified system property or environment variable value as the specified type.
	 *
	 * <p>
	 * First does a lookup on the system property with the specified property name (e.g. <js>"juneau.stackTraceCacheTimeout"</js>)
	 * <br>
	 * If not found, then searches it as an env var (e.g. <js>"JUNEAU_STACKTRACETIMEOUT"</js>).
	 *
	 * @param propertyName The system property name.
	 * @return The converted property, or <jk>null</jk> if property not set.
	 */
	public static String getProperty(String propertyName) {
		return getProperty(propertyName, null);
	}
}
