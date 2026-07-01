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
package org.apache.juneau.test.assertions;

import static org.apache.juneau.commons.utils.ResourceBundleUtils.*;

import java.util.*;

/**
 * A lightweight message-bundle loader for the assertions classes.
 *
 * <p>
 * Self-contained replacement for the marshall {@code Messages} loader, built on commons
 * {@link org.apache.juneau.commons.utils.ResourceBundleUtils}.  Supports only the single pattern used by the
 * assertion classes: {@code Messages.of(SomeAssertion.class, "Messages").getString(shortKey)}.
 */
public class Messages {

	private final Class<?> forClass;
	private final ResourceBundle bundle;

	private Messages(Class<?> forClass, ResourceBundle bundle) {
		this.forClass = forClass;
		this.bundle = bundle;
	}

	/**
	 * Creates a message bundle for the specified class.
	 *
	 * @param forClass The class the bundle belongs to.  Must not be <jk>null</jk>.
	 * @param name The bundle name (e.g. <js>"Messages"</js>).  Must not be <jk>null</jk>.
	 * @return A new message bundle.  Never <jk>null</jk>.
	 */
	public static Messages of(Class<?> forClass, String name) {
		var baseName = forClass.getPackage().getName() + "." + name;
		var bundle = findBundle(baseName, Locale.getDefault(), forClass.getClassLoader());
		return new Messages(forClass, bundle);
	}

	/**
	 * Returns the value for the specified key.
	 *
	 * <p>
	 * Resolves <c>&lt;SimpleClassName&gt;.&lt;key&gt;</c> first, then the bare <c>key</c>.
	 *
	 * @param key The short message key.
	 * @return The resolved value, or <js>"{!key}"</js> if not found.
	 */
	public String getString(String key) {
		if (bundle != null) {
			var fullKey = forClass.getSimpleName() + "." + key;
			if (bundle.containsKey(fullKey))
				return bundle.getString(fullKey);
			if (bundle.containsKey(key))
				return bundle.getString(key);
		}
		return "{!" + key + "}";
	}
}
