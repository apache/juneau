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
package org.apache.juneau.cp;

import static org.apache.juneau.internal.ResourceBundleUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.marshall.*;

/**
 * A wrapper around a {@link ResourceBundle}.
 *
 * <p>
 * Adds support for non-existent resource bundles and associating class loaders.
 */
public class Messages extends ResourceBundle {

	private ResourceBundle rb;
	private Class<?> c;
	private Messages parent;

	// Cache of message bundles per locale.
	private final ConcurrentHashMap<Locale,Messages> localizedMessages = new ConcurrentHashMap<>();

	// Cache of virtual keys to actual keys.
	private final Map<String,String> keyMap;

	private final Set<String> rbKeys;

	/**
	 * Creator.
	 *
	 * @param forClass
	 * 	The class we're creating this object for.
	 * @return A new builder.
	 */
	public static final MessagesBuilder create(Class<?> forClass) {
		return new MessagesBuilder(forClass);
	}

	/**
	 * Constructor.
	 *
	 * @param forClass
	 * 	The class we're creating this object for.
	 * @return A new message bundle belonging to the class.
	 */
	public static final Messages of(Class<?> forClass) {
		return create(forClass).build();
	}

	/**
	 * Constructor.
	 *
	 * @param forClass
	 * 	The class we're creating this object for.
	 * @param name
	 * 	The bundle name (e.g. <js>"Messages"</js>).
	 * 	<br>If <jk>null</jk>, uses the class name.
	 * @return A new message bundle belonging to the class.
	 */
	public static final Messages of(Class<?> forClass, String name) {
		return create(forClass).name(name).build();
	}


	/**
	 * Constructor.
	 *
	 * @param forClass
	 * 	The class we're creating this object for.
	 * @param rb
	 * 	The resource bundle we're encapsulating.  Can be <jk>null</jk>.
	 * @param parent
	 * 	The parent resource.  Can be <jk>null</jk>.
	 */
	public Messages(Class<?> forClass, ResourceBundle rb, Messages parent) {
		this.c = forClass;
		this.rb = rb;
		this.parent = parent;
		if (parent != null)
			setParent(parent);

		Map<String,String> keyMap = new TreeMap<>();

		String cn = c.getSimpleName() + '.';
		if (rb != null) {
			for (String key : rb.keySet()) {
				keyMap.put(key, key);
				if (key.startsWith(cn)) {
					String shortKey = key.substring(cn.length());
					keyMap.put(shortKey, key);
				}
			}
		}
		if (parent != null) {
			for (String key : parent.keySet()) {
				keyMap.put(key, key);
				if (key.startsWith(cn)) {
					String shortKey = key.substring(cn.length());
					keyMap.put(shortKey, key);
				}
			}
		}

		this.keyMap = Collections.unmodifiableMap(new LinkedHashMap<>(keyMap));
		this.rbKeys = rb == null ? Collections.emptySet() : rb.keySet();
	}

	/**
	 * Returns this message bundle for the specified locale.
	 *
	 * @param locale The locale to get the messages for.
	 * @return A new {@link Messages} object.  Never <jk>null</jk>.
	 */
	public Messages forLocale(Locale locale) {
		if (locale == null)
			locale = Locale.getDefault();
		Messages mb = localizedMessages.get(locale);
		if (mb == null) {
			Messages parent = this.parent == null ? null : this.parent.forLocale(locale);
			ResourceBundle rb = this.rb == null ? null : findBundle(this.rb.getBaseBundleName(), locale, c.getClassLoader());
			mb = new Messages(c, rb, parent);
			localizedMessages.put(locale, mb);
		}
		return mb;
	}

	/**
	 * Returns all keys in this resource bundle with the specified prefix.
	 *
	 * <p>
	 * Keys are returned in alphabetical order.
	 *
	 * @param prefix The prefix.
	 * @return The set of all keys in the resource bundle with the prefix.
	 */
	public Set<String> keySet(String prefix) {
		Set<String> set = new LinkedHashSet<>();
		for (String s : keySet()) {
			if (s.equals(prefix) || (s.startsWith(prefix) && s.charAt(prefix.length()) == '.'))
				set.add(s);
		}
		return set;
	}

	/**
	 * Similar to {@link ResourceBundle#getString(String)} except allows you to pass in {@link MessageFormat} objects.
	 *
	 * @param key The resource bundle key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @return
	 * 	The resolved value.  Never <jk>null</jk>.
	 * 	<js>"{!key}"</js> if the key is missing.
	 */
	public String getString(String key, Object...args) {
		String s = getString(key);
		if (s.startsWith("{!"))
			return s;
		return format(s, args);
	}

	/**
	 * Same as {@link #getString(String, Object...)} but allows you to specify the locale.
	 *
	 * @param locale The locale of the resource bundle to retrieve message from.
	 * @param key The resource bundle key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @return
	 * 	The resolved value.  Never <jk>null</jk>.
	 * 	<js>"{!!key}"</js> if the bundle is missing.
	 * 	<js>"{!key}"</js> if the key is missing.
	 */
	public String getString(Locale locale, String key, Object...args) {
		if (locale == null)
			return getString(key, args);
		return forLocale(locale).getString(key, args);
	}

	/**
	 * Looks for all the specified keys in the resource bundle and returns the first value that exists.
	 *
	 * @param keys The list of possible keys.
	 * @return The resolved value, or <jk>null</jk> if no value is found or the resource bundle is missing.
	 */
	public String findFirstString(String...keys) {
		for (String k : keys) {
			if (containsKey(k))
				return getString(k);
		}
		return null;
	}

	/**
	 * Same as {@link #findFirstString(String...)}, but uses the specified locale.
	 *
	 * @param locale The locale of the resource bundle to retrieve message from.
	 * @param keys The list of possible keys.
	 * @return The resolved value, or <jk>null</jk> if no value is found or the resource bundle is missing.
	 */
	public String findFirstString(Locale locale, String...keys) {
		Messages srb = forLocale(locale);
		return srb.findFirstString(keys);
	}

	@Override /* ResourceBundle */
	protected Object handleGetObject(String key) {
		String k = keyMap.get(key);
		if (k == null)
			return "{!" + key + "}";
		try {
			if (rbKeys.contains(k))
				return rb.getObject(k);
		} catch (MissingResourceException e) { /* Shouldn't happen */ }
		return parent.handleGetObject(key);
	}

	@Override /* ResourceBundle */
	public boolean containsKey(String key) {
		return keyMap.containsKey(key);
	}

	@Override /* ResourceBundle */
	public Set<String> keySet() {
		return keyMap.keySet();
	}

	@Override /* ResourceBundle */
	public Enumeration<String> getKeys() {
		return Collections.enumeration(keySet());
	}

	@Override
	public String toString() {
		OMap om = new OMap();
		for (String k : new TreeSet<>(keySet()))
			om.put(k, getString(k));
		return SimpleJson.DEFAULT.toString(om);
	}
}
