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
package org.apache.juneau.utils;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.collections.*;

/**
 * Wraps a {@link ResourceBundle} to provide some useful additional functionality.
 *
 * <ul class='spaced-list'>
 * 	<li>
 * 		Instead of throwing {@link MissingResourceException}, the {@link #getString(String)} method
 * 		will return <js>"{!!key}"</js> if the bundle was not found, and <js>"{!key}"</js> if bundle
 * 		was found but the key is not in the bundle.
 * 	<li>
 * 		A client locale can be set as a {@link ThreadLocal} object using the static {@link #setClientLocale(Locale)}
 * 		so that client localized messages can be retrieved using the {@link #getClientString(String, Object...)}
 * 		method on all instances of this class.
 * 	<li>
 * 		Resource bundles on parent classes can be added to the search path for this class by using the
 * 		{@link #addSearchPath(Class, String)} method.
 * 		This allows messages to be retrieved from the resource bundles of parent classes.
 * 	<li>
 * 		Locale-specific bundles can be retrieved by using the {@link #getBundle(Locale)} method.
 * 	<li>
 * 		The {@link #getString(Locale, String, Object...)} method can be used to retrieve locale-specific messages.
 * 	<li>
 * 		Messages in the resource bundle can optionally be prefixed with the simple class name.
 * 		For example, if the class is <c>MyClass</c> and the properties file contains <js>"MyClass.myMessage"</js>,
 * 		the message can be retrieved using <code>getString(<js>"myMessage"</js>)</code>.
 * </ul>
 *
 * <ul class='notes'>
 * 	<li>
 * 		This class is thread-safe.
 * </ul>
 */
public class MessageBundle extends ResourceBundle {

	private static final ThreadLocal<Locale> clientLocale = new ThreadLocal<>();

	private final ResourceBundle rb;
	private final String bundlePath, className;
	private final Class<?> forClass;
	private final long creationThreadId;

	// A map that contains all keys [shortKeyName->keyName] and [keyName->keyName], where shortKeyName
	// refers to keys prefixed and stripped of the class name (e.g. "foobar"->"MyClass.foobar")
	private final Map<String,String> keyMap = new ConcurrentHashMap<>();

	// Contains all keys present in all bundles in searchBundles.
	private final ConcurrentSkipListSet<String> allKeys = new ConcurrentSkipListSet<>();

	// Bundles to search through to find properties.
	// Typically this will be a list of resource bundles for each class up the class hierarchy chain.
	private final CopyOnWriteArrayList<MessageBundle> searchBundles = new CopyOnWriteArrayList<>();

	// Cache of message bundles per locale.
	private final ConcurrentHashMap<Locale,MessageBundle> localizedBundles = new ConcurrentHashMap<>();

	/**
	 * Sets the locale for this thread so that calls to {@link #getClientString(String, Object...)} return messages in
	 * that locale.
	 *
	 * @param locale The new client locale.
	 */
	public static void setClientLocale(Locale locale) {
		MessageBundle.clientLocale.set(locale);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * When this method is used, the bundle path is determined by searching for the resource bundle
	 * in the following locations:
	 * <ul>
	 * 	<li><c>[package].ForClass.properties</c>
	 * 	<li><c>[package].nls.ForClass.properties</c>
	 * 	<li><c>[package].i18n.ForClass.properties</c>
	 * </ul>
	 *
	 * @param forClass The class
	 * @return A new message bundle belonging to the class.
	 */
	public static final MessageBundle create(Class<?> forClass) {
		return create(forClass, findBundlePath(forClass));
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * A shortcut for calling <c>new MessageBundle(forClass, bundlePath)</c>.
	 *
	 * @param forClass The class
	 * @param bundlePath The location of the resource bundle.
	 * @return A new message bundle belonging to the class.
	 */
	public static final MessageBundle create(Class<?> forClass, String bundlePath) {
		return new MessageBundle(forClass, bundlePath);
	}

	private static final String findBundlePath(Class<?> forClass) {
		String path = forClass.getName();
		if (tryBundlePath(forClass, path))
			return path;
		path = forClass.getPackage().getName() + ".nls." + forClass.getSimpleName();
		if (tryBundlePath(forClass, path))
			return path;
		path = forClass.getPackage().getName() + ".i18n." + forClass.getSimpleName();
		if (tryBundlePath(forClass, path))
			return path;
		return null;
	}

	private static final boolean tryBundlePath(Class<?> c, String path) {
		try {
			path = c.getName();
			ResourceBundle.getBundle(path, Locale.getDefault(), c.getClassLoader());
			return true;
		} catch (MissingResourceException e) {
			return false;
		}
	}

	/**
	 * Constructor.
	 *
	 * @param forClass The class using this resource bundle.
	 * @param bundlePath
	 * 	The path of the resource bundle to wrap.
	 * 	This can be an absolute path (e.g. <js>"com.foo.MyMessages"</js>) or a path relative to the package of the
	 * 	<l>forClass</l> (e.g. <js>"MyMessages"</js> if <l>forClass</l> is <js>"com.foo.MyClass"</js>).
	 */
	public MessageBundle(Class<?> forClass, String bundlePath) {
		this(forClass, bundlePath, Locale.getDefault());
	}

	private MessageBundle(Class<?> forClass, String bundlePath, Locale locale) {
		this.forClass = forClass;
		this.className = forClass.getSimpleName();
		if (bundlePath == null)
			throw new RuntimeException("Bundle path was null.");
		if (bundlePath.endsWith(".properties"))
			throw new RuntimeException("Bundle path should not end with '.properties'");
		this.bundlePath = bundlePath;
		this.creationThreadId = Thread.currentThread().getId();
		ClassLoader cl = forClass.getClassLoader();
		ResourceBundle trb = null;
		try {
			trb = ResourceBundle.getBundle(bundlePath, locale, cl);
		} catch (MissingResourceException e) {
			try {
				trb = ResourceBundle.getBundle(forClass.getPackage().getName() + '.' + bundlePath, locale, cl);
			} catch (MissingResourceException e2) {
			}
		}
		this.rb = trb;
		if (rb != null) {

			// Populate keyMap with original mappings.
			for (Enumeration<String> e = getKeys(); e.hasMoreElements();) {
				String key = e.nextElement();
				keyMap.put(key, key);
			}

			// Override/augment with shortname mappings (e.g. "foobar"->"MyClass.foobar")
			String c = className + '.';
			for (Enumeration<String> e = getKeys(); e.hasMoreElements();) {
				String key = e.nextElement();
				if (key.startsWith(c)) {
					String shortKey = key.substring(className.length() + 1);
					keyMap.put(shortKey, key);
				}
			}

			allKeys.addAll(keyMap.keySet());
		}
		searchBundles.add(this);
	}


	/**
	 * Add another bundle path to this resource bundle.
	 *
	 * <p>
	 * Order of property lookup is first-to-last.
	 *
	 * <p>
	 * This method must be called from the same thread as the call to the constructor.
	 * This eliminates the need for synchronization.
	 *
	 * @param forClass The class using this resource bundle.
	 * @param bundlePath The bundle path.
	 * @return This object (for method chaining).
	 */
	public MessageBundle addSearchPath(Class<?> forClass, String bundlePath) {
		assertSameThread(creationThreadId, "This method can only be called from the same thread that created the object.");
		MessageBundle srb = new MessageBundle(forClass, bundlePath);
		if (srb.rb != null) {
			allKeys.addAll(srb.keySet());
			searchBundles.add(srb);
		}
		return this;
	}

	@Override /* ResourceBundle */
	public boolean containsKey(String key) {
		return allKeys.contains(key);
	}

	/**
	 * Similar to {@link ResourceBundle#getString(String)} except allows you to pass in {@link MessageFormat} objects.
	 *
	 * @param key The resource bundle key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @return
	 * 	The resolved value.  Never <jk>null</jk>.
	 * 	<js>"{!!key}"</js> if the bundle is missing.
	 * 	<js>"{!key}"</js> if the key is missing.
	 */
	public String getString(String key, Object...args) {
		String s = getString(key);
		if (s.length() > 0 && s.charAt(0) == '{')
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
		return getBundle(locale).getString(key, args);
	}

	/**
	 * Same as {@link #getString(String, Object...)} but uses the locale specified on the call to {@link #setClientLocale(Locale)}.
	 *
	 * @param key The resource bundle key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @return
	 * 	The resolved value.  Never <jk>null</jk>.
	 * 	<js>"{!!key}"</js> if the bundle is missing.
	 * 	<js>"{!key}"</js> if the key is missing.
	 */
	public String getClientString(String key, Object...args) {
		return getString(clientLocale.get(), key, args);
	}

	/**
	 * Looks for all the specified keys in the resource bundle and returns the first value that exists.
	 *
	 * @param keys The list of possible keys.
	 * @return The resolved value, or <jk>null</jk> if no value is found or the resource bundle is missing.
	 */
	public String findFirstString(String...keys) {
		if (rb == null)
			return null;
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
		MessageBundle srb = getBundle(locale);
		return srb.findFirstString(keys);
	}

	@Override /* ResourceBundle */
	public Set<String> keySet() {
		return Collections.unmodifiableSet(allKeys);
	}

	/**
	 * Returns all keys in this resource bundle with the specified prefix.
	 *
	 * @param prefix The prefix.
	 * @return The set of all keys in the resource bundle with the prefix.
	 */
	public Set<String> keySet(String prefix) {
		Set<String> set = new HashSet<>();
		for (String s : keySet()) {
			if (s.equals(prefix) || (s.startsWith(prefix) && s.charAt(prefix.length()) == '.'))
				set.add(s);
		}
		return set;
	}

	@Override /* ResourceBundle */
	public Enumeration<String> getKeys() {
		if (rb == null)
			return new Vector<String>(0).elements();
		return rb.getKeys();
	}

	@Override /* ResourceBundle */
	protected Object handleGetObject(String key) {
		for (MessageBundle srb : searchBundles) {
			if (srb.rb != null) {
				String key2 = srb.keyMap.get(key);
				if (key2 != null) {
					try {
						return srb.rb.getObject(key2);
					} catch (Exception e) {
						return "{!"+key+"}";
					}
				}
			}
		}
		if (rb == null)
			return "{!!"+key+"}";
		return "{!"+key+"}";
	}

	/**
	 * Returns this resource bundle as an {@link OMap}.
	 *
	 * <p>
	 * Useful for debugging purposes.
	 * Note that any class that implements a <c>swap()</c> method will automatically be serialized by
	 * calling this method and serializing the result.
	 *
	 * <p>
	 * This method always constructs a new {@link OMap} on each call.
	 *
	 * @return A new map containing all the keys and values in this bundle.
	 */
	public OMap swap() {
		OMap om = new OMap();
		for (String k : allKeys)
			om.put(k, getString(k));
		return om;
	}

	/**
	 * Returns the resource bundle for the specified locale.
	 *
	 * @param locale The client locale.
	 * @return The resource bundle for the specified locale.  Never <jk>null</jk>.
	 */
	public MessageBundle getBundle(Locale locale) {
		MessageBundle mb = localizedBundles.get(locale);
		if (mb != null)
			return mb;
		mb = new MessageBundle(forClass, bundlePath, locale);
		List<MessageBundle> l = new ArrayList<>(searchBundles.size()-1);
		for (int i = 1; i < searchBundles.size(); i++) {
			MessageBundle srb = searchBundles.get(i);
			srb = new MessageBundle(srb.forClass, srb.bundlePath, locale);
			l.add(srb);
			mb.allKeys.addAll(srb.keySet());
		}
		mb.searchBundles.addAll(l);
		localizedBundles.putIfAbsent(locale, mb);
		return localizedBundles.get(locale);
	}
}