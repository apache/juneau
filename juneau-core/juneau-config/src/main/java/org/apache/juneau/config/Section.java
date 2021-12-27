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
package org.apache.juneau.config;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static java.util.Optional.*;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.internal.*;
import org.apache.juneau.parser.*;

/**
 * A single section in a config file.
 */
public class Section {

	final Config config;
	private final ConfigMap configMap;
	final String name;

	/**
	 * Constructor.
	 *
	 * @param config The config that this entry belongs to.
	 * @param configMap The map that this belongs to.
	 * @param name The section name of this entry.
	 */
	protected Section(Config config, ConfigMap configMap, String name) {
		this.config = config;
		this.configMap = configMap;
		this.name = name;
	}

	/**
	 * Returns <jk>true</jk> if this section exists.
	 *
	 * @return <jk>true</jk> if this section exists.
	 */
	public boolean isPresent() {
		return configMap.hasSection(name);
	}

	/**
	 * Shortcut for calling <code>toBean(sectionName, c, <jk>false</jk>)</code>.
	 *
	 * @param c The bean class to create.
	 * @return A new bean instance, or <jk>null</jk> if this section does not exist.
	 * @throws ParseException Malformed input encountered.
	 */
	public <T> T toBean(Class<T> c) throws ParseException {
		return toBean(c, false);
	}

	/**
	 * Shortcut for calling <code>asBean(sectionName, c, <jk>false</jk>)</code>.
	 *
	 * @param c The bean class to create.
	 * @return A new bean instance, or {@link Optional#empty()} if this section does not exist.
	 * @throws ParseException Malformed input encountered.
	 */
	public <T> Optional<T> asBean(Class<T> c) throws ParseException {
		return ofNullable(toBean(c));
	}

	/**
	 * Converts this config file section to the specified bean instance.
	 *
	 * <p>
	 * Key/value pairs in the config file section get copied as bean property values to the specified bean class.
	 *
	 * <h5 class='figure'>Example config file</h5>
	 * <p class='bcode w800'>
	 * 	<cs>[MyAddress]</cs>
	 * 	<ck>name</ck> = <cv>John Smith</cv>
	 * 	<ck>street</ck> = <cv>123 Main Street</cv>
	 * 	<ck>city</ck> = <cv>Anywhere</cv>
	 * 	<ck>state</ck> = <cv>NY</cv>
	 * 	<ck>zip</ck> = <cv>12345</cv>
	 * </p>
	 *
	 * <h5 class='figure'>Example bean</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public class</jk> Address {
	 * 		public String name, street, city;
	 * 		public StateEnum state;
	 * 		public int zip;
	 * 	}
	 * </p>
	 *
	 * <h5 class='figure'>Example usage</h5>
	 * <p class='bcode w800'>
	 * 	Config cf = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 * 	Address myAddress = cf.getSection(<js>"MySection"</js>).toBean(Address.<jk>class</jk>);
	 * </p>
	 *
	 * @param c The bean class to create.
	 * @param ignoreUnknownProperties
	 * 	If <jk>false</jk>, throws a {@link ParseException} if the section contains an entry that isn't a bean property
	 * 	name.
	 * @return A new bean instance, or <jk>null</jk> if this section doesn't exist.
	 * @throws ParseException Unknown property was encountered in section.
	 */
	public <T> T toBean(Class<T> c, boolean ignoreUnknownProperties) throws ParseException {
		assertArgNotNull("c", c);
		if (! isPresent()) return null;

		Set<String> keys = configMap.getKeys(name);

		BeanMap<T> bm = config.beanSession.newBeanMap(c);
		for (String k : keys) {
			BeanPropertyMeta bpm = bm.getPropertyMeta(k);
			if (bpm == null) {
				if (! ignoreUnknownProperties)
					throw new ParseException("Unknown property ''{0}'' encountered in configuration section ''{1}''.", k, name);
			} else {
				bm.put(k, config.get(name + '/' + k).as(bpm.getClassMeta().getInnerClass()).orElse(null));
			}
		}

		return bm.getBean();
	}

	/**
	 * Converts this config file section to the specified bean instance.
	 *
	 * <p>
	 * Key/value pairs in the config file section get copied as bean property values to the specified bean class.
	 *
	 * <h5 class='figure'>Example config file</h5>
	 * <p class='bcode w800'>
	 * 	<cs>[MyAddress]</cs>
	 * 	<ck>name</ck> = <cv>John Smith</cv>
	 * 	<ck>street</ck> = <cv>123 Main Street</cv>
	 * 	<ck>city</ck> = <cv>Anywhere</cv>
	 * 	<ck>state</ck> = <cv>NY</cv>
	 * 	<ck>zip</ck> = <cv>12345</cv>
	 * </p>
	 *
	 * <h5 class='figure'>Example bean</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public class</jk> Address {
	 * 		public String name, street, city;
	 * 		public StateEnum state;
	 * 		public int zip;
	 * 	}
	 * </p>
	 *
	 * <h5 class='figure'>Example usage</h5>
	 * <p class='bcode w800'>
	 * 	Config cf = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 * 	Address myAddress = cf.getSection(<js>"MySection"</js>).asBean(Address.<jk>class</jk>).orElse(<jk>null</jk>);
	 * </p>
	 *
	 * @param c The bean class to create.
	 * @param ignoreUnknownProperties
	 * 	If <jk>false</jk>, throws a {@link ParseException} if the section contains an entry that isn't a bean property
	 * 	name.
	 * @return A new bean instance, or <jk>null</jk> if this section doesn't exist.
	 * @throws ParseException Unknown property was encountered in section.
	 */
	public <T> Optional<T> asBean(Class<T> c, boolean ignoreUnknownProperties) throws ParseException {
		return ofNullable(toBean(c, ignoreUnknownProperties));
	}

	/**
	 * Returns this section as a map.
	 *
	 * @return A new {@link OMap}, or <jk>null</jk> if this section doesn't exist.
	 */
	public OMap toMap() {
		if (! isPresent()) return null;

		Set<String> keys = configMap.getKeys(name);

		OMap om = new OMap();
		for (String k : keys)
			om.put(k, config.get(name + '/' + k).as(Object.class).orElse(null));
		return om;
	}

	/**
	 * Returns this section as a map.
	 *
	 * @return A new {@link OMap}, or {@link Optional#empty()} if this section doesn't exist.
	 */
	public Optional<OMap> asMap() {
		return ofNullable(toMap());
	}

	/**
	 * Wraps this section inside a Java interface so that values in the section can be read and
	 * write using getters and setters.
	 *
	 * <h5 class='figure'>Example config file</h5>
	 * <p class='bcode w800'>
	 * 	<cs>[MySection]</cs>
	 * 	<ck>string</ck> = <cv>foo</cv>
	 * 	<ck>int</ck> = <cv>123</cv>
	 * 	<ck>enum</ck> = <cv>ONE</cv>
	 * 	<ck>bean</ck> = <cv>{foo:'bar',baz:123}</cv>
	 * 	<ck>int3dArray</ck> = <cv>[[[123,null],null],null]</cv>
	 * 	<ck>bean1d3dListMap</ck> = <cv>{key:[[[[{foo:'bar',baz:123}]]]]}</cv>
	 * </p>
	 *
	 * <h5 class='figure'>Example interface</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public interface</jk> MyConfigInterface {
	 *
	 * 		String getString();
	 * 		<jk>void</jk> setString(String x);
	 *
	 * 		<jk>int</jk> getInt();
	 * 		<jk>void</jk> setInt(<jk>int</jk> x);
	 *
	 * 		MyEnum getEnum();
	 * 		<jk>void</jk> setEnum(MyEnum x);
	 *
	 * 		MyBean getBean();
	 * 		<jk>void</jk> setBean(MyBean x);
	 *
	 * 		<jk>int</jk>[][][] getInt3dArray();
	 * 		<jk>void</jk> setInt3dArray(<jk>int</jk>[][][] x);
	 *
	 * 		Map&lt;String,List&lt;MyBean[][][]&gt;&gt; getBean1d3dListMap();
	 * 		<jk>void</jk> setBean1d3dListMap(Map&lt;String,List&lt;MyBean[][][]&gt;&gt; x);
	 * 	}
	 * </p>
	 *
	 * <h5 class='figure'>Example usage</h5>
	 * <p class='bcode w800'>
	 * 	Config cf = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 *
	 * 	MyConfigInterface ci = cf.get(<js>"MySection"</js>).toInterface(MyConfigInterface.<jk>class</jk>);
	 *
	 * 	<jk>int</jk> myInt = ci.getInt();
	 *
	 * 	ci.setBean(<jk>new</jk> MyBean());
	 *
	 * 	cf.save();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Calls to setters when the configuration is read-only will cause {@link UnsupportedOperationException} to be thrown.
	 * </ul>
	 *
	 * @param c The proxy interface class.
	 * @return The proxy interface.
	 */
	@SuppressWarnings("unchecked")
	public <T> T toInterface(final Class<T> c) {
		assertArgNotNull("c", c);

		if (! c.isInterface())
			throw illegalArgumentException("Class ''{0}'' passed to toInterface() is not an interface.", c.getName());

		InvocationHandler h = new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				BeanInfo bi = Introspector.getBeanInfo(c, null);
				for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
					Method rm = pd.getReadMethod(), wm = pd.getWriteMethod();
					if (method.equals(rm))
						return config.get(name + '/' + pd.getName()).to(rm.getGenericReturnType());
					if (method.equals(wm))
						return config.set(name + '/' + pd.getName(), args[0]);
				}
				throw unsupportedOperationException("Unsupported interface method.  method=''{0}''", method);
			}
		};

		return (T)Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, h);
	}

	/**
	 * Wraps this section inside a Java interface so that values in the section can be read and
	 * write using getters and setters.
	 *
	 * <h5 class='figure'>Example config file</h5>
	 * <p class='bcode w800'>
	 * 	<cs>[MySection]</cs>
	 * 	<ck>string</ck> = <cv>foo</cv>
	 * 	<ck>int</ck> = <cv>123</cv>
	 * 	<ck>enum</ck> = <cv>ONE</cv>
	 * 	<ck>bean</ck> = <cv>{foo:'bar',baz:123}</cv>
	 * 	<ck>int3dArray</ck> = <cv>[[[123,null],null],null]</cv>
	 * 	<ck>bean1d3dListMap</ck> = <cv>{key:[[[[{foo:'bar',baz:123}]]]]}</cv>
	 * </p>
	 *
	 * <h5 class='figure'>Example interface</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public interface</jk> MyConfigInterface {
	 *
	 * 		String getString();
	 * 		<jk>void</jk> setString(String x);
	 *
	 * 		<jk>int</jk> getInt();
	 * 		<jk>void</jk> setInt(<jk>int</jk> x);
	 *
	 * 		MyEnum getEnum();
	 * 		<jk>void</jk> setEnum(MyEnum x);
	 *
	 * 		MyBean getBean();
	 * 		<jk>void</jk> setBean(MyBean x);
	 *
	 * 		<jk>int</jk>[][][] getInt3dArray();
	 * 		<jk>void</jk> setInt3dArray(<jk>int</jk>[][][] x);
	 *
	 * 		Map&lt;String,List&lt;MyBean[][][]&gt;&gt; getBean1d3dListMap();
	 * 		<jk>void</jk> setBean1d3dListMap(Map&lt;String,List&lt;MyBean[][][]&gt;&gt; x);
	 * 	}
	 * </p>
	 *
	 * <h5 class='figure'>Example usage</h5>
	 * <p class='bcode w800'>
	 * 	Config cf = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 *
	 * 	MyConfigInterface ci = cf.get(<js>"MySection"</js>).asInterface(MyConfigInterface.<jk>class</jk>).orElse(<jk>null</jk>);
	 *
	 * 	<jk>int</jk> myInt = ci.getInt();
	 *
	 * 	ci.setBean(<jk>new</jk> MyBean());
	 *
	 * 	cf.save();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Calls to setters when the configuration is read-only will cause {@link UnsupportedOperationException} to be thrown.
	 * </ul>
	 *
	 * @param c The proxy interface class.
	 * @return The proxy interface.
	 */
	public <T> Optional<T> asInterface(final Class<T> c) {
		return ofNullable(toInterface(c));
	}

	/**
	 * Copies the entries in this section to the specified bean by calling the public setters on that bean.
	 *
	 * @param bean The bean to set the properties on.
	 * @param ignoreUnknownProperties
	 * 	If <jk>true</jk>, don't throw an {@link IllegalArgumentException} if this section contains a key that doesn't
	 * 	correspond to a setter method.
	 * @return An object map of the changes made to the bean.
	 * @throws ParseException If parser was not set on this config file or invalid properties were found in the section.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Section writeToBean(Object bean, boolean ignoreUnknownProperties) throws ParseException {
		assertArgNotNull("bean", bean);
		if (! isPresent()) throw illegalArgumentException("Section ''{0}'' not found in configuration.", name);

		Set<String> keys = configMap.getKeys(name);

		BeanMap<?> bm = config.beanSession.toBeanMap(bean);
		for (String k : keys) {
			BeanPropertyMeta bpm = bm.getPropertyMeta(k);
			if (bpm == null) {
				if (! ignoreUnknownProperties)
					throw new ParseException("Unknown property ''{0}'' encountered in configuration section ''{1}''.", k, name);
			} else {
				bm.put(k, config.get(name + '/' + k).as(bpm.getClassMeta().getInnerClass()).orElse(null));
			}
		}

		return this;
	}
}
