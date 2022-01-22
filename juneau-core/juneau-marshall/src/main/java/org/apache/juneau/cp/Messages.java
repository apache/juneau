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

import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.ResourceBundleUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.utils.*;

/**
 * An enhanced {@link ResourceBundle}.
 *
 * <p>
 * Wraps a ResourceBundle to provide some useful additional functionality.
 *
 * <ul>
 * 	<li>
 * 		Instead of throwing {@link MissingResourceException}, the {@link ResourceBundle#getString(String)} method
 * 		will return <js>"{!key}"</js> if the message could not be found.
 * 	<li>
 * 		Supported hierarchical lookup of resources from parent parent classes.
 * 	<li>
 * 		Support for easy retrieval of localized bundles.
 * 	<li>
 * 		Support for generalized resource bundles (e.g. properties files containing keys for several classes).
 * </ul>
 *
 * <p>
 * The following example shows the basic usage of this class for retrieving localized messages:
 *
 * <p class='bcode w800'>
 * 	<cc># Contents of MyClass.properties</cc>
 * 	<ck>foo</ck> = <cv>foo {0}</cv>
 * 	<ck>MyClass.bar</ck> = <cv>bar {0}</cv>
 * </p>
 * <p class='bcode w800'>
 * 	<jk>public class</jk> MyClass {
 * 		<jk>private static final</jk> Messages <jsf>MESSAGES</jsf> = Messages.<jsm>of</jsm>(MyClass.<jk>class</jk>);
 *
 * 		<jk>public void</jk> doFoo() {
 *
 *			<jc>// A normal property.</jc>
 * 			String <jv>foo</jv> = <jsf>MESSAGES</jsf>.getString(<js>"foo"</js>,<js>"x"</js>);  <jc>// == "foo x"</jc>
 *
 * 			<jc>// A property prefixed by class name.</jc>
 * 			String <jv>bar</jv> = <jsf>MESSAGES</jsf>.getString(<js>"bar"</js>,<js>"x"</js>);  <jc>// == "bar x"</jc>
 *
 * 			<jc>// A non-existent property.</jc>
 * 			String <jv>baz</jv> = <jsf>MESSAGES</jsf>.getString(<js>"baz"</js>,<js>"x"</js>);  <jc>// == "{!baz}"</jc>
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * 	The ability to resolve keys prefixed by class name allows you to place all your messages in a single file such
 * 	as a common <js>"Messages.properties"</js> file along with those for other classes.
 * <p>
 * 	The following shows how to retrieve messages from a common bundle:
 *
 * <p class='bcode w800'>
 * 	<jk>public class</jk> MyClass {
 * 		<jk>private static final</jk> Messages <jsf>MESSAGES</jsf> = Messages.<jsm>of</jsm>(MyClass.<jk>class</jk>, <js>"Messages"</js>);
 * 	}
 * </p>
 *
 * <p>
 * 	Resource bundles are searched using the following base name patterns:
 * 	<ul>
 * 		<li><js>"{package}.{name}"</js>
 * 		<li><js>"{package}.i18n.{name}"</js>
 * 		<li><js>"{package}.nls.{name}"</js>
 * 		<li><js>"{package}.messages.{name}"</js>
 * 	</ul>
 *
 * <p>
 * 	These patterns can be customized using the {@link Builder#baseNames(String...)} method.
 *
 * <p>
 * 	Localized messages can be retrieved in the following way:
 *
 * <p class='bcode w800'>
 * 	<jc>// Return value from Japan locale bundle.</jc>
 * 	String <jv>foo</jv> = <jsf>MESSAGES</jsf>.forLocale(Locale.<jsf>JAPAN</jsf>).getString(<js>"foo"</js>);
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class Messages extends ResourceBundle {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param forClass
	 * 	The class we're creating this object for.
	 * @return A new builder.
	 */
	public static final Builder create(Class<?> forClass) {
		return new Builder(forClass);
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

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<Messages> {

		Class<?> forClass;
		Locale locale;
		String name;
		Messages parent;
		List<Tuple2<Class<?>,String>> locations;

		private String[] baseNames = {"{package}.{name}","{package}.i18n.{name}","{package}.nls.{name}","{package}.messages.{name}"};

		/**
		 * Constructor.
		 *
		 * @param forClass The base class.
		 */
		protected Builder(Class<?> forClass) {
			super(Messages.class, BeanStore.INSTANCE);
			this.forClass = forClass;
			this.name = forClass.getSimpleName();
			locations = new ArrayList<>();
			locale = Locale.getDefault();
		}

		@Override /* BeanBuilder */
		protected Messages buildDefault() {

			if (! locations.isEmpty()) {
				Tuple2<Class<?>,String>[] mbl = locations.toArray(new Tuple2[0]);

				Builder x = null;

				for (int i = mbl.length-1; i >= 0; i--) {
					Class<?> c = firstNonNull(mbl[i].getA(), forClass);
					String value = mbl[i].getB();
					if (isJsonObject(value, true)) {
						MessagesString ms;
						try {
							ms = SimpleJson.DEFAULT.read(value, MessagesString.class);
						} catch (ParseException e) {
							throw runtimeException(e);
						}
						x = Messages.create(c).name(ms.name).baseNames(split(ms.baseNames, ',')).locale(ms.locale).parent(x == null ? null : x.build());
					} else {
						x = Messages.create(c).name(value).parent(x == null ? null : x.build());
					}
				}

				return x == null ? null : x.build();  // Shouldn't be null.
			}

			return new Messages(this);
		}

		private static class MessagesString {
			public String name;
			public String[] baseNames;
			public String locale;
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Adds a parent bundle.
		 *
		 * @param parent The parent bundle.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder parent(Messages parent) {
			this.parent = parent;
			return this;
		}

		/**
		 * Specifies the bundle name (e.g. <js>"Messages"</js>).
		 *
		 * @param name
		 * 	The bundle name.
		 * 	<br>If <jk>null</jk>, the forClass class name is used.
		 * @return This object.
		 */
		public Builder name(String name) {
			this.name = isEmpty(name) ? forClass.getSimpleName() : name;
			return this;
		}

		/**
		 * Specifies the base name patterns to use for finding the resource bundle.
		 *
		 * @param baseNames
		 * 	The bundle base names.
		 * 	<br>The default is the following:
		 * 	<ul>
		 * 		<li><js>"{package}.{name}"</js>
		 * 		<li><js>"{package}.i18n.{name}"</js>
		 * 		<li><js>"{package}.nls.{name}"</js>
		 * 		<li><js>"{package}.messages.{name}"</js>
		 * 	</ul>
		 * @return This object.
		 */
		public Builder baseNames(String...baseNames) {
			this.baseNames = baseNames == null ? new String[]{} : baseNames;
			return this;
		}

		/**
		 * Specifies the locale.
		 *
		 * @param locale
		 * 	The locale.
		 * 	If <jk>null</jk>, the default locale is used.
		 * @return This object.
		 */
		public Builder locale(Locale locale) {
			this.locale = locale == null ? Locale.getDefault() : locale;
			return this;
		}

		/**
		 * Specifies the locale.
		 *
		 * @param locale
		 * 	The locale.
		 * 	If <jk>null</jk>, the default locale is used.
		 * @return This object.
		 */
		public Builder locale(String locale) {
			return locale(locale == null ? null : Locale.forLanguageTag(locale));
		}

		/**
		 * Specifies a location of where to look for messages.
		 *
		 * @param baseClass The base class.
		 * @param bundlePath The bundle path.
		 * @return This object.
		 */
		public Builder location(Class<?> baseClass, String bundlePath) {
			this.locations.add(0, Tuple2.of(baseClass, bundlePath));
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>

		//-------------------------------------------------------------------------------------------------------------
		// Other methods
		//-------------------------------------------------------------------------------------------------------------

		ResourceBundle getBundle() {
			ClassLoader cl = forClass.getClassLoader();
			OMap m = OMap.of("name", name, "package", forClass.getPackage().getName());
			for (String bn : baseNames) {
				bn = StringUtils.replaceVars(bn, m);
				ResourceBundle rb = findBundle(bn, locale, cl);
				if (rb != null)
					return rb;
			}
			return null;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private ResourceBundle rb;
	private Class<?> c;
	private Messages parent;
	private Locale locale;

	// Cache of message bundles per locale.
	private final ConcurrentHashMap<Locale,Messages> localizedMessages = new ConcurrentHashMap<>();

	// Cache of virtual keys to actual keys.
	private final Map<String,String> keyMap;

	private final Set<String> rbKeys;


	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected Messages(Builder builder) {
		this(builder.forClass, builder.getBundle(), builder.locale, builder.parent);
	}

	Messages(Class<?> forClass, ResourceBundle rb, Locale locale, Messages parent) {
		this.c = forClass;
		this.rb = rb;
		this.parent = parent;
		if (parent != null)
			setParent(parent);
		this.locale = locale == null ? Locale.getDefault() : locale;

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
		if (this.locale.equals(locale))
			return this;
		Messages mb = localizedMessages.get(locale);
		if (mb == null) {
			Messages parent = this.parent == null ? null : this.parent.forLocale(locale);
			ResourceBundle rb = this.rb == null ? null : findBundle(this.rb.getBaseBundleName(), locale, c.getClassLoader());
			mb = new Messages(c, rb, locale, parent);
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

	@Override /* Object */
	public String toString() {
		OMap om = new OMap();
		for (String k : new TreeSet<>(keySet()))
			om.put(k, getString(k));
		return json(om);
	}
}
