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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * A reusable stateless thread-safe read-only configuration, typically used for creating one-time use {@link Session}
 * objects.
 *
 * <p>
 * Contexts are created through the {@link ContextBuilder#build()} method (and subclasses of {@link ContextBuilder}).
 *
 * <p>
 * Subclasses MUST implement the following constructor:
 *
 * <p class='bcode w800'>
 * 	<jk>public</jk> T(PropertyStore);
 * </p>
 *
 * <p>
 * Besides that restriction, a context object can do anything you desire.
 * <br>However, it MUST be thread-safe and all fields should be declared final to prevent modification.
 * <br>It should NOT be used for storing temporary or state information.
 *
 * @see PropertyStore
 */
@ConfigurableContext
public abstract class Context {

	static final String PREFIX = "Context";

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.Context#CONTEXT_debug CONTEXT_debug}
	 * 	<li><b>Name:</b>  <js>"Context.debug.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Context.debug</c>
	 * 	<li><b>Environment variable:</b>  <c>CONTEXT_DEBUG</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#debug()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.ContextBuilder#debug()}
	 * 			<li class='jm'>{@link org.apache.juneau.SessionArgs#debug(Boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>
	 * 		Enables {@link Serializer#BEANTRAVERSE_detectRecursions}.
	 * </ul>
	 *
	 * <p>
	 * Enables the following additional information during parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean setters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer with debug enabled.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.debug()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_debug</jsf>, <jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Create a POJO model with a recursive loop.</jc>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> Object <jf>f</jf>;
	 * 	}
	 * 	A a = <jk>new</jk> A();
	 * 	a.<jf>f</jf> = a;
	 *
	 * 	<jc>// Throws a SerializeException and not a StackOverflowError</jc>
	 * 	String json = s.serialize(a);
	 * </p>
	 */
	public static final String CONTEXT_debug = PREFIX + ".debug.b";

	/**
	 * Configuration property:  Locale.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.Context#CONTEXT_locale CONTEXT_locale}
	 * 	<li><b>Name:</b>  <js>"Context.locale.s"</js>
	 * 	<li><b>Data type:</b>  {@link java.util.Locale}
	 * 	<li><b>System property:</b>  <c>Context.locale</c>
	 * 	<li><b>Environment variable:</b>  <c>CONTEXT_LOCALE</c>
	 * 	<li><b>Default:</b>  <jk>null</jk> (defaults to {@link java.util.Locale#getDefault()})
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#locale()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.ContextBuilder#locale(Locale)}
	 * 			<li class='jm'>{@link org.apache.juneau.SessionArgs#locale(Locale)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Specifies the default locale for serializer and parser sessions when not specified via {@link SessionArgs#locale(Locale)}.
	 * Typically used for POJO swaps that need to deal with locales such as swaps that convert <l>Date</l> and <l>Calendar</l>
	 * objects to strings by accessing it via the session passed into the {@link PojoSwap#swap(BeanSession, Object)} and
	 * {@link PojoSwap#unswap(BeanSession, Object, ClassMeta, String)} methods.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define a POJO swap that skips serializing beans if we're in the UK.</jc>
	 * 	<jk>public class</jk> MyBeanSwap <jk>extends</jk> StringSwap&lt;MyBean&gt; {
	 * 		<ja>@Override</ja>
	 * 		public String swap(BeanSession session, MyBean o) throws Exception {
	 * 			<jk>if</jk> (session.getLocale().equals(Locale.<jsf>UK</jsf>))
	 * 				<jk>return null</jk>;
	 * 			<jk>return</jk> o.toString();
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a serializer that uses the specified locale if it's not passed in through session args.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.locale(Locale.<jsf>UK</jsf>)
	 * 		.pojoSwaps(MyBeanSwap.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>CONTEXT_locale</jsf>, Locale.<jsf>UK</jsf>)
	 * 		.addTo(<jsf>BEAN_pojoSwaps</jsf>, MyBeanSwap.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Define on session-args instead.</jc>
	 * 	SerializerSessionArgs sessionArgs = <jk>new</jk> SerializerSessionArgs().locale(Locale.<jsf>UK</jsf>);
	 * 	<jk>try</jk> (WriterSerializerSession session = s.createSession(sessionArgs)) {
	 *
	 * 		<jc>// Produces "null" if in the UK.</jc>
	 * 		String json = s.serialize(<jk>new</jk> MyBean());
	 * 	}
	 * </p>
	 */
	public static final String CONTEXT_locale = PREFIX + ".locale.s";

	/**
	 * Configuration property:  Media type.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.Context#CONTEXT_mediaType CONTEXT_mediaType}
	 * 	<li><b>Name:</b>  <js>"Context.mediaType.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.http.MediaType}
	 * 	<li><b>System property:</b>  <c>Context.mediaType</c>
	 * 	<li><b>Environment variable:</b>  <c>CONTEXT_MEDIATYPE</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#mediaType()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.ContextBuilder#mediaType(MediaType)}
	 * 			<li class='jm'>{@link org.apache.juneau.SessionArgs#mediaType(MediaType)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Specifies the default media type for serializer and parser sessions when not specified via {@link SessionArgs#mediaType(MediaType)}.
	 * Typically used for POJO swaps that need to serialize the same POJO classes differently depending on
	 * the specific requested media type.   For example, a swap could handle a request for media types <js>"application/json"</js>
	 * and <js>"application/json+foo"</js> slightly differently even though they're both being handled by the same JSON
	 * serializer or parser.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define a POJO swap that skips serializing beans if the media type is application/json.</jc>
	 * 	<jk>public class</jk> MyBeanSwap <jk>extends</jk> StringSwap&lt;MyBean&gt; {
	 * 		<ja>@Override</ja>
	 * 		public String swap(BeanSession session, MyBean o) throws Exception {
	 * 			<jk>if</jk> (session.getMediaType().equals(<js>"application/json"</js>))
	 * 				<jk>return null</jk>;
	 * 			<jk>return</jk> o.toString();
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a serializer that uses the specified media type if it's not passed in through session args.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.mediaType(MediaType.<jsf>JSON</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>CONTEXT_mediaType</jsf>, MediaType.<jsf>JSON</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Define on session-args instead.</jc>
	 * 	SerializerSessionArgs sessionArgs = <jk>new</jk> SerializerSessionArgs().mediaType(MediaType.<jsf>JSON</jsf>);
	 * 	<jk>try</jk> (WriterSerializerSession session = s.createSession(sessionArgs)) {
	 *
	 * 		<jc>// Produces "null" since it's JSON.</jc>
	 * 		String json = s.serialize(<jk>new</jk> MyBean());
	 * 	}
	 * </p>
	 */
	public static final String CONTEXT_mediaType = PREFIX + ".mediaType.s";

	/**
	 * Configuration property:  Time zone.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.Context#CONTEXT_timeZone CONTEXT_timeZone}
	 * 	<li><b>Name:</b>  <js>"Context.timeZone.s"</js>
	 * 	<li><b>Data type:</b>  {@link java.util.TimeZone}
	 * 	<li><b>System property:</b>  <c>Context.timeZone</c>
	 * 	<li><b>Environment variable:</b>  <c>CONTEXT_TIMEZONE</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#timeZone()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.ContextBuilder#timeZone(TimeZone)}
	 * 			<li class='jm'>{@link org.apache.juneau.SessionArgs#timeZone(TimeZone)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Specifies the default time zone for serializer and parser sessions when not specified via {@link SessionArgs#timeZone(TimeZone)}.
	 * Typically used for POJO swaps that need to deal with timezones such as swaps that convert <l>Date</l> and <l>Calendar</l>
	 * objects to strings by accessing it via the session passed into the {@link PojoSwap#swap(BeanSession, Object)} and
	 * {@link PojoSwap#unswap(BeanSession, Object, ClassMeta, String)} methods.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define a POJO swap that skips serializing beans if the time zone is GMT.</jc>
	 * 	<jk>public class</jk> MyBeanSwap <jk>extends</jk> StringSwap&lt;MyBean&gt; {
	 * 		<ja>@Override</ja>
	 * 		public String swap(BeanSession session, MyBean o) throws Exception {
	 * 			<jk>if</jk> (session.getTimeZone().equals(TimeZone.<jsf>GMT</jsf>))
	 * 				<jk>return null</jk>;
	 * 			<jk>return</jk> o.toString();
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a serializer that uses GMT if the timezone is not specified in the session args.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.timeZone(TimeZone.<jsf>GMT</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>CONTEXT_timeZone</jsf>, TimeZone.<jsf>GMT</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Define on session-args instead.</jc>
	 * 	SerializerSessionArgs sessionArgs = <jk>new</jk> SerializerSessionArgs().timeZone(TimeZone.<jsf>GMT</jsf>);
	 * 	<jk>try</jk> (WriterSerializerSession ss = JsonSerializer.<jsf>DEFAULT</jsf>.createSession(sessionArgs)) {
	 *
	 * 		<jc>// Produces "null" since the time zone is GMT.</jc>
	 * 		String json = s.serialize(<jk>new</jk> MyBean());
	 * 	}
	 * </p>
	 */
	public static final String CONTEXT_timeZone = PREFIX + ".timeZone.s";


	private final PropertyStore propertyStore;
	private final int identityCode;

	private final boolean debug;
	private final Locale locale;
	private final TimeZone timeZone;
	private final MediaType mediaType;

	/**
	 * Constructor for this class.
	 *
	 * <p>
	 * Subclasses MUST implement the same public constructor.
	 *
	 * @param ps The read-only configuration for this context object.
	 * @param allowReuse If <jk>true</jk>, subclasses that share the same property store values can be reused.
	 */
	public Context(PropertyStore ps, boolean allowReuse) {
		this.propertyStore = ps == null ? PropertyStore.DEFAULT : ps;
		this.identityCode = allowReuse ? new HashCode().add(getClass().getName()).add(ps).get() : System.identityHashCode(this);
		debug = getBooleanProperty(CONTEXT_debug, false);
		locale = getInstanceProperty(CONTEXT_locale, Locale.class, Locale.getDefault());
		timeZone = getInstanceProperty(CONTEXT_timeZone, TimeZone.class, null);
		mediaType = getInstanceProperty(CONTEXT_mediaType, MediaType.class, null);
	}

	/**
	 * Returns the raw property value with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value, or <jk>null</jk> if it doesn't exist.
	 */
	public Object getProperty(String key) {
		return propertyStore.getProperty(key);
	}

	/**
	 * Returns the property value with the specified name.
	 *
	 * @param key The property name.
	 * @param c The class to cast or convert the value to.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final <T> T getProperty(String key, Class<T> c, T def) {
		return propertyStore.getProperty(key, c, def);
	}

	/**
	 * Shortcut for calling <code>getProperty(key, Boolean.<jk>class</jk>, def)</code>.
	 *
	 * @param key The property name.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final Boolean getBooleanProperty(String key, Boolean def) {
		return getProperty(key, Boolean.class, def);
	}

	/**
	 * Shortcut for calling <code>getProperty(key, Integer.<jk>class</jk>, def)</code>.
	 *
	 * @param key The property name.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final Integer getIntegerProperty(String key, Integer def) {
		return getProperty(key, Integer.class, def);
	}

	/**
	 * Shortcut for calling <code>getProperty(key, Long.<jk>class</jk>, def)</code>.
	 *
	 * @param key The property name.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final Long getLongProperty(String key, Long def) {
		return getProperty(key, Long.class, def);
	}

	/**
	 * Shortcut for calling <code>getProperty(key, String.<jk>class</jk>, def)</code>.
	 *
	 * @param key The property name.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final String getStringProperty(String key, String def) {
		return getProperty(key, String.class, def);
	}

	/**
	 * Returns a property as a parsed comma-delimited list of strings.
	 *
	 * @param key The property name.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final String[] getCdlProperty(String key, String def) {
		return StringUtils.split(StringUtils.emptyIfNull(getProperty(key, String.class, def)));
	}

	/**
	 * Same as {@link #getStringProperty(String, String)} but returns a blank instead of the default value if it resolves to <js>"NONE"</js>.
	 *
	 * @param key The property name.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final String getStringPropertyWithNone(String key, String def) {
		String s = getProperty(key, String.class, def);
		return "NONE".equalsIgnoreCase(s) ? "" : s;
	}

	/**
	 * Returns the class property with the specified name.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final <T> Class<? extends T> getClassProperty(String key, Class<T> type, Class<? extends T> def) {
		return propertyStore.getClassProperty(key, type, def);
	}

	/**
	 * Returns the array property value with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final <T> T[] getArrayProperty(String key, Class<T> eType) {
		return propertyStore.getArrayProperty(key, eType);
	}

	/**
	 * Returns the array property value with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @param def The default value.
	 * @return The property value, or the default value if it doesn't exist.
	 */
	public final <T> T[] getArrayProperty(String key, Class<T> eType, T[] def) {
		return propertyStore.getArrayProperty(key, eType, def);
	}

	/**
	 * Returns the class array property with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public final Class<?>[] getClassArrayProperty(String key) {
		return propertyStore.getClassArrayProperty(key);
	}

	/**
	 * Returns the class array property with the specified name.
	 *
	 * @param key The property name.
	 * @param def The default value.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public final Class<?>[] getClassArrayProperty(String key, Class<?>[] def) {
		return propertyStore.getClassArrayProperty(key, def);
	}

	/**
	 * Returns the class array property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value, or an empty array if it doesn't exist.
	 */
	public final <T> Class<T>[] getClassArrayProperty(String key, Class<T> eType) {
		return propertyStore.getClassArrayProperty(key, eType);
	}

	/**
	 * Returns the set property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>LinkedHashSet</c>, or an empty set if it doesn't exist.
	 */
	public final <T> Set<T> getSetProperty(String key, Class<T> eType) {
		return propertyStore.getSetProperty(key, eType);
	}

	/**
	 * Returns the set property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @param def The default value if the property doesn't exist or is empty.
	 * @return The property value as an unmodifiable <c>LinkedHashSet</c>, or the default value if it doesn't exist or is empty.
	 */
	public final <T> Set<T> getSetProperty(String key, Class<T> eType, Set<T> def) {
		return propertyStore.getSetProperty(key, eType, def);
	}

	/**
	 * Returns the class set property with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value as an unmodifiable <c>LinkedHashSet</c>, or an empty set if it doesn't exist.
	 */
	public final Set<Class<?>> getClassSetProperty(String key) {
		return propertyStore.getClassSetProperty(key);
	}

	/**
	 * Returns the class set property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>LinkedHashSet</c>, or an empty set if it doesn't exist.
	 */
	public final <T> Set<Class<T>> getClassSetProperty(String key, Class<T> eType) {
		return propertyStore.getClassSetProperty(key, eType);
	}

	/**
	 * Returns the list property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>ArrayList</c>, or an empty list if it doesn't exist.
	 */
	public final <T> List<T> getListProperty(String key, Class<T> eType) {
		return propertyStore.getListProperty(key, eType);
	}

	/**
	 * Returns the list property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @param def The default value if the property doesn't exist or is empty.
	 * @return The property value as an unmodifiable <c>ArrayList</c>, or the default value if it doesn't exist or is empty.
	 */
	public final <T> List<T> getListProperty(String key, Class<T> eType, List<T> def) {
		return propertyStore.getListProperty(key, eType, def);
	}

	/**
	 * Returns the class list property with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value as an unmodifiable <c>ArrayList</c>, or an empty list if it doesn't exist.
	 */
	public final List<Class<?>> getClassListProperty(String key) {
		return propertyStore.getClassListProperty(key);
	}

	/**
	 * Returns the class list property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>ArrayList</c>, or an empty list if it doesn't exist.
	 */
	public final <T> List<Class<T>> getClassListProperty(String key, Class<T> eType) {
		return propertyStore.getClassListProperty(key, eType);
	}

	/**
	 * Returns the map property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>LinkedHashMap</c>, or an empty map if it doesn't exist.
	 */
	public final <T> Map<String,T> getMapProperty(String key, Class<T> eType) {
		return propertyStore.getMapProperty(key, eType);
	}

	/**
	 * Returns the class map property with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value as an unmodifiable <c>LinkedHashMap</c>, or an empty map if it doesn't exist.
	 */
	public final Map<String,Class<?>> getClassMapProperty(String key) {
		return propertyStore.getClassMapProperty(key);
	}

	/**
	 * Returns the class map property with the specified name.
	 *
	 * @param key The property name.
	 * @param eType The class type of the elements in the property.
	 * @return The property value as an unmodifiable <c>LinkedHashMap</c>, or an empty map if it doesn't exist.
	 */
	public final <T> Map<String,Class<T>> getClassMapProperty(String key, Class<T> eType) {
		return propertyStore.getClassMapProperty(key, eType);
	}

	/**
	 * Returns an instance of the specified class, string, or object property.
	 *
	 * <p>
	 * If instantiating a class, assumes the class has a no-arg constructor.
	 * Otherwise, throws a runtime exception.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def
	 * 	The default value if the property doesn't exist.
	 * 	<br>Can either be an instance of <c>T</c>, or a <code>Class&lt;? <jk>extends</jk> T&gt;</code>, or <jk>null</jk>.
	 * @return A new property instance.
	 */
	public <T> T getInstanceProperty(String key, Class<T> type, Object def) {
		return propertyStore.getInstanceProperty(key, type, def);
	}

	/**
	 * Returns an instance of the specified class, string, or object property.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def
	 * 	The default value if the property doesn't exist.
	 * 	<br>Can either be an instance of <c>T</c>, or a <code>Class&lt;? <jk>extends</jk> T&gt;</code>.
	 * @param resolver
	 * 	The resolver to use for instantiating objects.
	 * @param args
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T getInstanceProperty(String key, Class<T> type, Object def, ResourceResolver resolver, Object...args) {
		return propertyStore.getInstanceProperty(key, type, def, resolver, args);
	}

	/**
	 * Returns an instance of the specified class, string, or object property.
	 *
	 * @param key The property name.
	 * @param outer The outer object if the class we're instantiating is an inner class.
	 * @param type The class type of the property.
	 * @param def
	 * 	The default value if the property doesn't exist.
	 * 	<br>Can either be an instance of <c>T</c>, or a <code>Class&lt;? <jk>extends</jk> T&gt;</code>.
	 * @param resolver
	 * 	The resolver to use for instantiating objects.
	 * @param args
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T getInstanceProperty(String key, Object outer, Class<T> type, Object def, ResourceResolver resolver, Object...args) {
		return propertyStore.getInstanceProperty(key, outer, type, def, resolver, args);
	}

	/**
	 * Returns the specified property as an array of instantiated objects.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def The default object to return if the property doesn't exist.
	 * @return A new property instance.
	 */
	public <T> T[] getInstanceArrayProperty(String key, Class<T> type, T[] def) {
		return propertyStore.getInstanceArrayProperty(key, type, def);
	}

	/**
	 * Returns the specified property as an array of instantiated objects.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @param def The default object to return if the property doesn't exist.
	 * @param resolver
	 * 	The resolver to use for instantiating objects.
	 * @param args
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T[] getInstanceArrayProperty(String key, Class<T> type, T[] def, ResourceResolver resolver, Object...args) {
		return propertyStore.getInstanceArrayProperty(key, type, def, resolver, args);
	}

	/**
	 * Returns the specified property as an array of instantiated objects.
	 *
	 * @param key The property name.
	 * @param outer The outer object if the class we're instantiating is an inner class.
	 * @param type The class type of the property.
	 * @param def The default object to return if the property doesn't exist.
	 * @param resolver
	 * 	The resolver to use for instantiating objects.
	 * @param args
	 * 	Arguments to pass to the constructor.
	 * 	Constructors matching the arguments are always used before no-arg constructors.
	 * @return A new property instance.
	 */
	public <T> T[] getInstanceArrayProperty(String key, Object outer, Class<T> type, T[] def, ResourceResolver resolver, Object...args) {
		return propertyStore.getInstanceArrayProperty(key, outer, type, def, resolver, args);
	}

	/**
	 * Returns the keys found in the specified property group.
	 *
	 * <p>
	 * The keys are NOT prefixed with group names.
	 *
	 * @param group The group name.
	 * @return The set of property keys, or an empty set if the group was not found.
	 */
	public Set<String> getPropertyKeys(String group) {
		return propertyStore.getPropertyKeys(group);
	}

	/**
	 * Returns the property store associated with this context.
	 *
	 * @return The property store associated with this context.
	 */
	@BeanIgnore
	public final PropertyStore getPropertyStore() {
		return propertyStore;
	}

	/**
	 * Creates a builder from this context object.
	 *
	 * <p>
	 * Builders are used to define new contexts (e.g. serializers, parsers) based on existing configurations.
	 *
	 * @return A new ContextBuilder object.
	 */
	public ContextBuilder builder() {
		return null;
	}

	/**
	 * Create a new bean session based on the properties defined on this context.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @return A new session object.
	 */
	public Session createSession() {
		return createSession(createDefaultSessionArgs());
	}

	/**
	 * Create a new session based on the properties defined on this context combined with the specified
	 * runtime args.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @param args
	 * 	The session arguments.
	 * @return A new session object.
	 */
	public abstract Session createSession(SessionArgs args);

	/**
	 * Defines default session arguments used when calling the {@link #createSession()} method.
	 *
	 * @return A SessionArgs object, possibly a read-only reusable instance.
	 */
	public abstract SessionArgs createDefaultSessionArgs();

	@Override /* Object */
	public int hashCode() {
		return identityCode;
	}

	/**
	 * Returns a uniqueness identity code for this context.
	 *
	 * @return A uniqueness identity code.
	 */
	public int identityCode() {
		return identityCode;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		// Context objects are considered equal if they're the same class and have the same set of properties.
		if (o == null)
			return false;
		if (o.getClass() != this.getClass())
			return false;
		Context c = (Context)o;
		return (c.propertyStore.equals(propertyStore));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Debug mode.
	 *
	 * @see #CONTEXT_debug
	 * @return
	 * 	<jk>true</jk> if debug mode is enabled.
	 */
	protected boolean isDebug() {
		return debug;
	}

	/**
	 * Locale.
	 *
	 * @see #CONTEXT_locale
	 * @return
	 * 	The default locale for serializer and parser sessions.
	 */
	protected final Locale getLocale() {
		return locale;
	}

	/**
	 * Media type.
	 *
	 * @see #CONTEXT_mediaType
	 * @return
	 * 	The default media type value for serializer and parser sessions.
	 */
	protected final MediaType getMediaType() {
		return mediaType;
	}

	/**
	 * Time zone.
	 *
	 * @see #CONTEXT_timeZone
	 * @return
	 * 	The default timezone for serializer and parser sessions.
	 */
	protected final TimeZone getTimeZone() {
		return timeZone;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}

	/**
	 * Returns the properties defined on this bean context as a simple map for debugging purposes.
	 *
	 * @return A new map containing the properties defined on this context.
	 */
	public OMap toMap() {
		return new DefaultFilteringOMap()
			.a("Context", new DefaultFilteringOMap()
				.a("identityCode", identityCode)
				.a("propertyStore", propertyStore)
			);
	}
}
