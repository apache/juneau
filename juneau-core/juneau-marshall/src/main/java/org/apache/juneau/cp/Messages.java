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
package org.apache.juneau.cp;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ResourceBundleUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.common.utils.Utils.isEmpty;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.function.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.parser.ParseException;

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
 * <p class='bini'>
 * 	<cc># Contents of MyClass.properties</cc>
 * 	<ck>foo</ck> = <cv>foo {0}</cv>
 * 	<ck>MyClass.bar</ck> = <cv>bar {0}</cv>
 * </p>
 * <p class='bjava'>
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
 * <p class='bjava'>
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
 * <p class='bjava'>
 * 	<jc>// Return value from Japan locale bundle.</jc>
 * 	String <jv>foo</jv> = <jsf>MESSAGES</jsf>.forLocale(Locale.<jsf>JAPAN</jsf>).getString(<js>"foo"</js>);
 * </p>
 *
 */
public class Messages extends ResourceBundle {
	/**
	 * Builder class.
	 */
	public static class Builder extends BeanBuilder<Messages> {

		private static class MessagesString {
			public String name;
			public String[] baseNames;
			public String locale;
		}

		Class<?> forClass;
		Locale locale;
		String name;
		Messages parent;

		List<Tuple2<Class<?>,String>> locations;

		private String[] baseNames = { "{package}.{name}", "{package}.i18n.{name}", "{package}.nls.{name}", "{package}.messages.{name}" };

		/**
		 * Constructor.
		 *
		 * @param forClass The base class.
		 */
		protected Builder(Class<?> forClass) {
			super(Messages.class, BeanStore.INSTANCE);
			this.forClass = forClass;
			this.name = cns(forClass);
			locations = list();
			locale = Locale.getDefault();
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
			this.baseNames = baseNames == null ? a() : baseNames;
			return this;
		}

		@Override /* Overridden from BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		/**
		 * Specifies the locale.
		 *
		 * @param value
		 * 	The locale.
		 * 	If <jk>null</jk>, the default locale is used.
		 * @return This object.
		 */
		public Builder locale(Locale value) {
			this.locale = value == null ? Locale.getDefault() : value;
			return this;
		}

		/**
		 * Specifies the locale.
		 *
		 * @param value
		 * 	The locale.
		 * 	If <jk>null</jk>, the default locale is used.
		 * @return This object.
		 */
		public Builder locale(String value) {
			return locale(value == null ? null : Locale.forLanguageTag(value));
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

		/**
		 * Specifies a location of where to look for messages.
		 *
		 * @param bundlePath The bundle path.
		 * @return This object.
		 */
		public Builder location(String bundlePath) {
			this.locations.add(0, Tuple2.of(forClass, bundlePath));
			return this;
		}

		/**
		 * Specifies the bundle name (e.g. <js>"Messages"</js>).
		 *
		 * @param value
		 * 	The bundle name.
		 * 	<br>If <jk>null</jk>, the forClass class name is used.
		 * @return This object.
		 */
		public Builder name(String value) {
			this.name = isEmpty(value) ? cns(forClass) : value;
			return this;
		}

		/**
		 * Adds a parent bundle.
		 *
		 * @param value The parent bundle.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder parent(Messages value) {
			this.parent = value;
			return this;
		}

		@Override /* Overridden from BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override /* Overridden from BeanBuilder */
		protected Messages buildDefault() {

			if (! locations.isEmpty()) {
				Tuple2<Class<?>,String>[] mbl = locations.toArray(new Tuple2[0]);

				var x = (Builder)null;

				for (var i = mbl.length - 1; i >= 0; i--) {
					var c = firstNonNull(mbl[i].getA(), forClass);
					var value = mbl[i].getB();
					if (isJsonObject(value, true)) {
						MessagesString ms;
						try {
							ms = Json5.DEFAULT.read(value, MessagesString.class);
						} catch (ParseException e) {
							throw toRex(e);
						}
						x = Messages.create(c).name(ms.name).baseNames(StringUtils.splita(ms.baseNames, ',')).locale(ms.locale).parent(x == null ? null : x.build());
					} else {
						x = Messages.create(c).name(value).parent(x == null ? null : x.build());
					}
				}

				return x == null ? null : x.build();  // Shouldn't be null.
			}

			return new Messages(this);
		}

		ResourceBundle getBundle() {
			var cl = forClass.getClassLoader();
			var m = JsonMap.of("name", name, "package", forClass.getPackage().getName());
			for (var bn : baseNames) {
				bn = StringUtils.formatNamed(bn, m);
				var rb = findBundle(bn, locale, cl);
				if (nn(rb))
					return rb;
			}
			return null;
		}
	}

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
		if (nn(parent))
			setParent(parent);
		this.locale = locale == null ? Locale.getDefault() : locale;

		var keyMap = new TreeMap<String,String>();

		var cn = cns(c) + '.';
		if (nn(rb)) {
			rb.keySet().forEach(x -> {
				keyMap.put(x, x);
				if (x.startsWith(cn)) {
					var shortKey = x.substring(cn.length());
					keyMap.put(shortKey, x);
				}
			});
		}
		if (nn(parent)) {
			parent.keySet().forEach(x -> {
				keyMap.put(x, x);
				if (x.startsWith(cn)) {
					var shortKey = x.substring(cn.length());
					keyMap.put(shortKey, x);
				}
			});
		}

		this.keyMap = u(copyOf(keyMap));
		this.rbKeys = rb == null ? Collections.emptySet() : rb.keySet();
	}

	@Override /* Overridden from ResourceBundle */
	public boolean containsKey(String key) {
		return keyMap.containsKey(key);
	}

	/**
	 * Looks for all the specified keys in the resource bundle and returns the first value that exists.
	 *
	 * @param keys The list of possible keys.
	 * @return The resolved value, or <jk>null</jk> if no value is found or the resource bundle is missing.
	 */
	public String findFirstString(String...keys) {
		for (var k : keys) {
			if (containsKey(k))
				return getString(k);
		}
		return null;
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
		var mb = localizedMessages.get(locale);
		if (mb == null) {
			var parent = this.parent == null ? null : this.parent.forLocale(locale);
			var rb = this.rb == null ? null : findBundle(this.rb.getBaseBundleName(), locale, c.getClassLoader());
			mb = new Messages(c, rb, locale, parent);
			localizedMessages.put(locale, mb);
		}
		return mb;
	}

	@Override /* Overridden from ResourceBundle */
	public Enumeration<String> getKeys() { return Collections.enumeration(keySet()); }

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
		var s = getString(key);
		if (s.startsWith("{!"))
			return s;
		return mformat(s, args);
	}

	@Override /* Overridden from ResourceBundle */
	public Set<String> keySet() {
		return keyMap.keySet();
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
		Set<String> set = set();
		keySet().forEach(x -> {
			if (x.equals(prefix) || (x.startsWith(prefix) && x.charAt(prefix.length()) == '.'))
				set.add(x);
		});
		return set;
	}

	@Override /* Overridden from Object */
	public String toString() {
		var m = new JsonMap();
		for (var k : new TreeSet<>(keySet()))
			m.put(k, getString(k));
		return Json5.of(m);
	}

	@Override /* Overridden from ResourceBundle */
	protected Object handleGetObject(String key) {
		var k = keyMap.get(key);
		if (k == null)
			return "{!" + key + "}";
		try {
			if (rbKeys.contains(k))
				return rb.getObject(k);
		} catch (@SuppressWarnings("unused") MissingResourceException e) { /* Shouldn't happen */ }
		return parent.handleGetObject(key);
	}
}