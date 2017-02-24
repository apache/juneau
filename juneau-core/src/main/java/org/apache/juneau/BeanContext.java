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

import static org.apache.juneau.Visibility.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Core class of the Juneau architecture.
 * <p>
 * 	This class servers multiple purposes:
 * 	<ul class='spaced-list'>
 * 		<li>Provides the ability to wrap beans inside {@link Map} interfaces.
 * 		<li>Serves as a repository for metadata on POJOs, such as associated {@link BeanFilter beanFilters}, {@link PropertyNamer property namers}, etc...
 * 			which are used to tailor how POJOs are serialized and parsed.
 * 		<li>Serves as a common utility class for all {@link Serializer Serializers} and {@link Parser Parsers}
 * 				for serializing and parsing Java beans.
 * 	</ul>
 * <p>
 * 	All serializer and parser contexts extend from this context.
 *
 * <h5 class='topic'>Bean Contexts</h5>
 * 	Bean contexts are created through the {@link ContextFactory#getContext(Class)} method.
 * 	These context objects are read-only, reusable, and thread-safe.
 * 	The {@link ContextFactory} class will typically cache copies of <code>Context</code> objects based on
 * 		the current settings on the factory.
 * <p>
 * 	Each bean context maintains a cache of {@link ClassMeta} objects that describe information about classes encountered.
 * 	These <code>ClassMeta</code> objects are time-consuming to construct.
 * 	Therefore, instances of {@link BeanContext} that share the same <js>"BeanContext.*"</js> property values share
 * 		the same cache.  This allows for efficient reuse of <code>ClassMeta</code> objects so that the information about
 * 		classes only needs to be calculated once.
 *  Because of this, many of the properties defined on the {@link BeanContext} class cannot be overridden on the session.
 *
 * <h5 class='topic'>Bean Sessions</h5>
 * <p>
 * 	Whereas <code>BeanContext</code> objects are permanent, unchangeable, cached, and thread-safe,
 * 		{@link BeanSession} objects are ephemeral and not thread-safe.
 * 	They are meant to be used as quickly-constructed scratchpads for creating bean maps.
 * 	{@link BeanMap} objects can only be created through the session.
 *
 * <h5 class='topic'>BeanContext configuration properties</h5>
 * 	<code>BeanContexts</code> have several configuration properties that can be used to tweak behavior on how beans are handled.
 * 	These are denoted as the static <jsf>BEAN_*</jsf> fields on this class.
 * <p>
 * 	Some settings (e.g. {@link BeanContext#BEAN_beansRequireDefaultConstructor}) are used to differentiate between bean and non-bean classes.
 * 	Attempting to create a bean map around one of these objects will throw a {@link BeanRuntimeException}.
 * 	The purpose for this behavior is so that the serializers can identify these non-bean classes and convert them to plain strings using the {@link Object#toString()} method.
 * <p>
 * 	Some settings (e.g. {@link BeanContext#BEAN_beanFieldVisibility}) are used to determine what kinds of properties are detected on beans.
 * <p>
 * 	Some settings (e.g. {@link BeanContext#BEAN_beanMapPutReturnsOldValue}) change the runtime behavior of bean maps.
 * <p>
 * 	Settings are specified using the {@link ContextFactory#setProperty(String, Object)} method and related convenience methods.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct a context from scratch.</jc>
 * 	BeanContext beanContext = ContextFactory.<jsm>create</jsm>()
 * 		.setProperty(BeanContext.<jsf>BEAN_beansRequireDefaultConstructor</jsf>, <jk>true</jk>)
 * 		.addNotBeanClasses(Foo.<jk>class</jk>)
 * 		.getBeanContext();
 *
 * 	<jc>// Clone an existing context factory.</jc>
 * 	BeanContext beanContext = ContextFactory.<jsm>create</jsm>(otherConfig)
 * 		.setProperty(BeanContext.<jsf>BEAN_beansRequireDefaultConstructor</jsf>, <jk>true</jk>)
 * 		.addNotBeanClasses(Foo.<jk>class</jk>)
 * 		.getBeanContext();
 * </p>
 *
 * <h5 class='topic'>Bean Maps</h5>
 * <p>
 * 	{@link BeanMap BeanMaps} are wrappers around Java beans that allow properties to be retrieved and
 * 	set using the common {@link Map#put(Object,Object)} and {@link Map#get(Object)} methods.<br>
 * 	<br>
 * 	Bean maps are created in two ways...
 * 	<ol>
 * 		<li> {@link BeanSession#toBeanMap(Object) BeanSession.toBeanMap()} - Wraps an existing bean inside a {@code Map} wrapper.
 * 		<li> {@link BeanSession#newBeanMap(Class) BeanSession.newBeanMap()} - Create a new bean instance wrapped in a {@code Map} wrapper.
 * 	</ol>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// A sample bean class</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> String getName();
 * 		<jk>public void</jk> setName(String name);
 * 		<jk>public int</jk> getAge();
 * 		<jk>public void</jk> setAge(<jk>int</jk> age);
 * 	}
 *
 * 	<jc>// Create a new bean session</jc>
 *  BeanSession session = BeanContext.<jsf>DEFAULT</jsf>.createSession();
 *
 * 	<jc>// Wrap an existing bean in a new bean map</jc>
 * 	BeanMap&lt;Person&gt; m1 = session.toBeanMap(<jk>new</jk> Person());
 * 	m1.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	m1.put(<js>"age"</js>, 45);
 *
 * 	<jc>// Create a new bean instance wrapped in a new bean map</jc>
 * 	BeanMap&lt;Person&gt; m2 = session.newBeanMap(Person.<jk>class</jk>);
 * 	m2.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	m2.put(<js>"age"</js>, 45);
 * 	Person p = m2.getBean();  <jc>// Get the bean instance that was created.</jc>
 * </p>
 *
 * <h5 class='topic'>Bean Annotations</h5>
 * <p>
 * 	This package contains annotations that can be applied to
 * 	class definitions to override what properties are detected on a bean.
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Bean class definition where only property 'name' is detected.</jc>
 * 	<ja>&#64;Bean</ja>(properties=<js>"name"</js>)
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> String getName();
 * 		<jk>public void</jk> setName(String name);
 * 		<jk>public int</jk> getAge();
 * 		<jk>public void</jk> setAge(<jk>int</jk> age);
 * 	}
 * <p>
 * 	See {@link Bean @Bean} and {@link BeanProperty @BeanProperty} for more information.
 *
 * <h5 class='topic'>Beans with read-only properties</h5>
 * <p>
 * 	Bean maps can also be defined on top of beans with read-only properties by adding a
 * 	{@link BeanConstructor @BeanConstructor} annotation to one of the constructors on the
 * 	bean class.  This will allow read-only properties to be set through constructor arguments.
 * <p>
 * 	When the <code>@BeanConstructor</code> annotation is present, bean instantiation is delayed until the call to {@link BeanMap#getBean()}.
 * 	Until then, bean property values are stored in a local cache until <code>getBean()</code> is called.
 * 	Because of this additional caching step, parsing into read-only beans tends to be slower and use
 * 	more memory than parsing into beans with writable properties.
 * <p>
 * 	Attempting to call {@link BeanMap#put(String,Object)} on a read-only property after calling {@link BeanMap#getBean()}
 * 	will result in a {@link BeanRuntimeException} being thrown.
 * 	Multiple calls to {@link BeanMap#getBean()} will return the same bean instance.
 * <p>
 * 	Beans can be defined with a combination of read-only and read-write properties.
 * <p>
 * 	See {@link BeanConstructor @BeanConstructor} for more information.
 *
 * <h5 class='topic'>BeanFilters and PojoSwaps</h5>
 * <p>
 * 	{@link BeanFilter BeanFilters} and {@link PojoSwap PojoSwaps} are used to tailor how beans and POJOs are handled.<br>
 * 	<ol class='spaced-list'>
 * 		<li>{@link BeanFilter} - Allows you to tailor handling of bean classes.
 * 			This class can be considered a programmatic equivalent to the {@link Bean} annotation when
 * 			annotating classes are not possible (e.g. you don't have access to the source).
 * 			This includes specifying which properties are visible and the ability to programmatically override the execution of properties.
 * 		<li>{@link PojoSwap} - Allows you to swap out non-serializable objects with serializable replacements.
 * 	</ol>
 * <p>
 * 	See <a class='doclink' href='transform/package-summary.html#TOC'>org.apache.juneau.transform</a> for more information.
 *
 * <h5 class='topic'>ClassMetas</h5>
 * <p>
 * 	The {@link ClassMeta} class is a wrapper around {@link Class} object that provides cached information
 * 	about that class (e.g. whether it's a {@link Map} or {@link Collection} or bean).
 * <p>
 * 	As a general rule, it's best to reuse bean contexts (and therefore serializers and parsers too)
 * 	whenever possible since it takes some time to populate the internal {@code ClassMeta} object cache.
 * 	By reusing bean contexts, the class type metadata only needs to be calculated once which significantly
 * 	improves performance.
 * <p>
 * 	See {@link ClassMeta} for more information.
 */
@SuppressWarnings({"unchecked","rawtypes","hiding"})
public class BeanContext extends Context {

	/**
	 * <b>Configuration property:</b>  Beans require no-arg constructors.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beansRequireDefaultConstructor"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, a Java class must implement a default no-arg constructor to be considered a bean.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 */
	public static final String BEAN_beansRequireDefaultConstructor = "BeanContext.beansRequireDefaultConstructor";

	/**
	 * <b>Configuration property:</b>  Beans require {@link Serializable} interface.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beansRequireSerializable"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 */
	public static final String BEAN_beansRequireSerializable = "BeanContext.beansRequireSerializable";

	/**
	 * <b>Configuration property:</b>  Beans require setters for getters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beansRequireSettersForGetters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, only getters that have equivalent setters will be considered as properties on a bean.
	 * Otherwise, they will be ignored.
	 */
	public static final String BEAN_beansRequireSettersForGetters = "BeanContext.beansRequireSettersForGetters";

	/**
	 * <b>Configuration property:</b>  Beans require at least one property.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beansRequireSomeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then a Java class must contain at least 1 property to be considered a bean.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 */
	public static final String BEAN_beansRequireSomeProperties = "BeanContext.beansRequireSomeProperties";

	/**
	 * <b>Configuration property:</b>  {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property value.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanMapPutReturnsOldValue"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property values.
	 * <p>
	 * Disabled by default because it introduces a slight performance penalty.
	 */
	public static final String BEAN_beanMapPutReturnsOldValue = "BeanContext.beanMapPutReturnsOldValue";

	/**
	 * <b>Configuration property:</b>  Look for bean constructors with the specified minimum visibility.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanConstructorVisibility"</js>
	 * 	<li><b>Data type:</b> {@link Visibility}
	 * 	<li><b>Default:</b> {@link Visibility#PUBLIC}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 */
	public static final String BEAN_beanConstructorVisibility = "BeanContext.beanConstructorVisibility";

	/**
	 * <b>Configuration property:</b>  Look for bean classes with the specified minimum visibility.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanClassVisibility"</js>
	 * 	<li><b>Data type:</b> {@link Visibility}
	 * 	<li><b>Default:</b> {@link Visibility#PUBLIC}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean class is <jk>protected</jk>, then
	 * 	the class will not be interpreted as a bean class.
	 */
	public static final String BEAN_beanClassVisibility = "BeanContext.beanClassVisibility";

	/**
	 * <b>Configuration property:</b>  Look for bean fields with the specified minimum visibility.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanFieldVisibility"</js>
	 * 	<li><b>Data type:</b> {@link Visibility}
	 * 	<li><b>Default:</b> {@link Visibility#PUBLIC}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Fields are not considered bean properties unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean field is <jk>protected</jk>, then
	 * 	the field will not be interpreted as a bean property.
	 * <p>
	 * Use {@link Visibility#NONE} to prevent bean fields from being interpreted as bean properties altogether.
	 */
	public static final String BEAN_beanFieldVisibility = "BeanContext.beanFieldVisibility";

	/**
	 * <b>Configuration property:</b>  Look for bean methods with the specified minimum visibility.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.methodVisibility"</js>
	 * 	<li><b>Data type:</b> {@link Visibility}
	 * 	<li><b>Default:</b> {@link Visibility#PUBLIC}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Methods are not considered bean getters/setters unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean method is <jk>protected</jk>, then
	 * 	the method will not be interpreted as a bean getter or setter.
	 */
	public static final String BEAN_methodVisibility = "BeanContext.methodVisibility";

	/**
	 * <b>Configuration property:</b>  Use Java {@link Introspector} for determining bean properties.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.useJavaBeanIntrospector"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 * Most {@link Bean @Bean} annotations will be ignored.
	 */
	public static final String BEAN_useJavaBeanIntrospector = "BeanContext.useJavaBeanIntrospector";

	/**
	 * <b>Configuration property:</b>  Use interface proxies.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.useInterfaceProxies"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then interfaces will be instantiated as proxy classes through the use of an {@link InvocationHandler}
	 * if there is no other way of instantiating them.
	 */
	public static final String BEAN_useInterfaceProxies = "BeanContext.useInterfaceProxies";

	/**
	 * <b>Configuration property:</b>  Ignore unknown properties.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignoreUnknownBeanProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code RuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreUnknownBeanProperties = "BeanContext.ignoreUnknownBeanProperties";

	/**
	 * <b>Configuration property:</b>  Ignore unknown properties with null values.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignoreUnknownNullBeanProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, trying to set a <jk>null</jk> value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code RuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreUnknownNullBeanProperties = "BeanContext.ignoreUnknownNullBeanProperties";

	/**
	 * <b>Configuration property:</b>  Ignore properties without setters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignorePropertiesWithoutSetters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a bean property without a setter will silently be ignored.
	 * Otherwise, a {@code RuntimeException} is thrown.
	 */
	public static final String BEAN_ignorePropertiesWithoutSetters = "BeanContext.ignorePropertiesWithoutSetters";

	/**
	 * <b>Configuration property:</b>  Ignore invocation errors on getters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignoreInvocationExceptionsOnGetters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean getter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnGetters = "BeanContext.ignoreInvocationExceptionsOnGetters";

	/**
	 * <b>Configuration property:</b>  Ignore invocation errors on setters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignoreInvocationExceptionsOnSetters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean setter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnSetters = "BeanContext.ignoreInvocationExceptionsOnSetters";

	/**
	 * <b>Configuration property:</b>  Sort bean properties in alphabetical order.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.sortProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * Otherwise, the natural order of the bean properties is used which is dependent on the
	 * 	JVM vendor.
	 * On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
	 * On Oracle JVMs, the bean properties are not ordered (which follows the offical JVM specs).
	 * <p>
	 * This property is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
	 * to force bean properties to be in a particular order and can just alter the order of the fields/methods
	 * in the Java file.
	 */
	public static final String BEAN_sortProperties = "BeanContext.sortProperties";

	/**
	 * <b>Configuration property:</b>  Packages whose classes should not be considered beans.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.notBeanPackages.set"</js>
	 * 	<li><b>Data type:</b> <code>Set&lt;String&gt;</code>
	 * 	<li><b>Default:</b>
	 * 	<ul>
	 * 		<li><code>java.lang</code>
	 * 		<li><code>java.lang.annotation</code>
	 * 		<li><code>java.lang.ref</code>
	 * 		<li><code>java.lang.reflect</code>
	 * 		<li><code>java.io</code>
	 * 		<li><code>java.net</code>
	 * 		<li><code>java.nio.*</code>
	 * 		<li><code>java.util.*</code>
	 * 	</ul>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * When specified, the current list of ignore packages are appended to.
	 * <p>
	 * Any classes within these packages will be serialized to strings using {@link Object#toString()}.
	 * <p>
	 * Note that you can specify prefix patterns to include all subpackages.
	 */
	public static final String BEAN_notBeanPackages = "BeanContext.notBeanPackages.set";

	/**
	 * <b>Configuration property:</b>  Add to packages whose classes should not be considered beans.
	 */
	public static final String BEAN_notBeanPackages_add = "BeanContext.notBeanPackages.set.add";

	/**
	 * <b>Configuration property:</b>  Remove from packages whose classes should not be considered beans.
	 */
	public static final String BEAN_notBeanPackages_remove = "BeanContext.notBeanPackages.set.remove";

	/**
	 * <b>Configuration property:</b>  Classes to be excluded from consideration as being beans.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.notBeanClasses.set"</js>
	 * 	<li><b>Data type:</b> <code>Set&lt;Class&gt;</code>
	 * 	<li><b>Default:</b> empty set
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Not-bean classes are typically converted to <code>Strings</code> during serialization even if they
	 * appear to be bean-like.
	 */
	public static final String BEAN_notBeanClasses = "BeanContext.notBeanClasses.set";

	/**
	 * <b>Configuration property:</b>  Add to classes that should not be considered beans.
	 */
	public static final String BEAN_notBeanClasses_add = "BeanContext.notBeanClasses.set.add";

	/**
	 * <b>Configuration property:</b>  Remove from classes that should not be considered beans.
	 */
	public static final String BEAN_notBeanClasses_remove = "BeanContext.notBeanClasses.set.remove";

	/**
	 * <b>Configuration property:</b>  Bean filters to apply to beans.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanFilters.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;Class&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * This is a programmatic equivalent to the {@link Bean @Bean} annotation.
	 * It's useful when you want to use the Bean annotation functionality, but you don't have the ability
	 * 	to alter the bean classes.
	 * <p>
	 * There are two category of classes that can be passed in through this method:
	 * <ul class='spaced-list'>
	 * 	<li>Subclasses of {@link BeanFilterBuilder}.
	 * 		These must have a public no-arg constructor.
	 * 	<li>Bean interface classes.
	 * 		A shortcut for defining a {@link InterfaceBeanFilterBuilder}.
	 * 		Any subclasses of an interface class will only have properties defined on the interface.
	 * 		All other bean properties will be ignored.
	 * </ul>
	 */
	public static final String BEAN_beanFilters = "BeanContext.beanFilters.list";

	/**
	 * <b>Configuration property:</b>  Add to bean filters.
	 */
	public static final String BEAN_beanFilters_add = "BeanContext.beanFilters.list.add";

	/**
	 * <b>Configuration property:</b>  Remove from bean filters.
	 */
	public static final String BEAN_beanFilters_remove = "BeanContext.beanFilters.list.remove";

	/**
	 * <b>Configuration property:</b>  POJO swaps to apply to Java objects.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.pojoSwaps.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;Class&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * There are two category of classes that can be passed in through this method:
	 * <ul>
	 * 	<li>Subclasses of {@link PojoSwap}.
	 * 	<li>Surrogate classes.  A shortcut for defining a {@link SurrogateSwap}.
	 * </ul>
	 */
	public static final String BEAN_pojoSwaps = "BeanContext.pojoSwaps.list";

	/**
	 * <b>Configuration property:</b>  Add to POJO swap classes.
	 */
	public static final String BEAN_pojoSwaps_add = "BeanContext.pojoSwaps.list.add";

	/**
	 * <b>Configuration property:</b>  Remove from POJO swap classes.
	 */
	public static final String BEAN_pojoSwaps_remove = "BeanContext.pojoSwaps.list.remove";

	/**
	 * <b>Configuration property:</b>  Implementation classes for interfaces and abstract classes.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.implClasses.map"</js>
	 * 	<li><b>Data type:</b> <code>Map&lt;Class,Class&gt;</code>
	 * 	<li><b>Default:</b> empty map
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation
	 * 	class for the interface/abstract class so that instances of the implementation
	 * 	class are used when instantiated (e.g. during a parse).
	 */
	public static final String BEAN_implClasses = "BeanContext.implClasses.map";

	/**
	 * <b>Configuration property:</b>  Add an implementation class.
	 */
	public static final String BEAN_implClasses_put = "BeanContext.implClasses.map.put";

	/**
	 * <b>Configuration property:</b>  Bean lookup dictionary.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanDictionary.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;Class&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * This list can consist of the following class types:
	 * <ul>
	 * 	<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean.typeName()}.
	 * 	<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
	 * 	<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
	 * </ul>
	 */
	public static final String BEAN_beanDictionary = "BeanContext.beanDictionary.list";

	/**
	 * <b>Configuration property:</b>  Add to bean dictionary.
	 */
	public static final String BEAN_beanDictionary_add = "BeanContext.beanDictionary.list.add";

	/**
	 * <b>Configuration property:</b>  Remove from bean dictionary.
	 */
	public static final String BEAN_beanDictionary_remove = "BeanContext.beanDictionary.list.remove";

	/**
	 * <b>Configuration property:</b>  Name to use for the bean type properties used to represent a bean type.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanTypePropertyName"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"_type"</js>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 */
	public static final String BEAN_beanTypePropertyName = "BeanContext.beanTypePropertyName";

	/**
	 * <b>Configuration property:</b>  Default parser to use when converting <code>Strings</code> to POJOs.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.defaultParser"</js>
	 * 	<li><b>Data type:</b> <code>Class</code>
	 * 	<li><b>Default:</b> {@link JsonSerializer}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Used in the in the {@link BeanSession#convertToType(Object, Class)} method.
	 */
	public static final String BEAN_defaultParser = "BeanContext.defaultParser";

	/**
	 * <b>Configuration property:</b>  Locale.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.locale"</js>
	 * 	<li><b>Data type:</b> <code>Locale</code>
	 * 	<li><b>Default:</b> <code>Locale.getDefault()</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Used in the in the {@link BeanSession#convertToType(Object, Class)} method.ß
	 */
	public static final String BEAN_locale = "BeanContext.locale";

	/**
	 * <b>Configuration property:</b>  TimeZone.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.timeZone"</js>
	 * 	<li><b>Data type:</b> <code>TimeZone</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Used in the in the {@link BeanSession#convertToType(Object, Class)} method.ß
	 */
	public static final String BEAN_timeZone = "BeanContext.timeZone";

	/**
	 * <b>Configuration property:</b>  Media type.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.mediaType"</js>
	 * 	<li><b>Data type:</b> <code>MediaType</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Specifies a default media type value for serializer and parser sessions.
	 */
	public static final String BEAN_mediaType = "BeanContext.mediaType";

	/**
	 * <b>Configuration property:</b>  Debug mode.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.debug"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>Enables {@link SerializerContext#SERIALIZER_detectRecursions}.
	 * </ul>
	 * <p>
	 * Enables the following additional information during parsing:
	 * <ul class='spaced-list'>
	 * 	<li>When bean setters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * </ul>
	 */
	public static final String BEAN_debug = "BeanContext.debug";

	/*
	 * The default package pattern exclusion list.
	 * Any beans in packages in this list will not be considered beans.
	 */
	private static final String[] DEFAULT_NOTBEAN_PACKAGES = {
		"java.lang",
		"java.lang.annotation",
		"java.lang.ref",
		"java.lang.reflect",
		"java.io",
		"java.net",
		"java.nio.*",
		"java.util.*"
	};

	/*
	 * The default bean class exclusion list.
	 * Anything in this list will not be considered beans.
	 */
	private static final Class<?>[] DEFAULT_NOTBEAN_CLASSES = {
		Map.class,
		Collection.class,
		Reader.class,
		Writer.class,
		InputStream.class,
		OutputStream.class,
		Throwable.class
	};


	static final void loadDefaults(ContextFactory config) {
		config.setProperty(BEAN_notBeanPackages, DEFAULT_NOTBEAN_PACKAGES);
		config.setProperty(BEAN_notBeanClasses, DEFAULT_NOTBEAN_CLASSES);
	}


	// This map is important!
	// We may have many ConfigFactory objects that have identical BeanContext properties.
	// This map ensures that if the BeanContext properties in the ConfigFactory are the same,
	// then we reuse the same Class->ClassMeta cache map.
	// This significantly reduces the number of times we need to construct ClassMeta objects which can be expensive.
	private static final ConcurrentHashMap<Integer,Map<Class,ClassMeta>> cmCacheCache = new ConcurrentHashMap<Integer,Map<Class,ClassMeta>>();

	/** Default config.  All default settings. */
	public static final BeanContext DEFAULT = ContextFactory.create().getContext(BeanContext.class);

	/** Default config.  All default settings except sort bean properties. */
	public static final BeanContext DEFAULT_SORTED = ContextFactory.create().setProperty(BEAN_sortProperties, true).getContext(BeanContext.class);

	final boolean
		beansRequireDefaultConstructor,
		beansRequireSerializable,
		beansRequireSettersForGetters,
		beansRequireSomeProperties,
		beanMapPutReturnsOldValue,
		useInterfaceProxies,
		ignoreUnknownBeanProperties,
		ignoreUnknownNullBeanProperties,
		ignorePropertiesWithoutSetters,
		ignoreInvocationExceptionsOnGetters,
		ignoreInvocationExceptionsOnSetters,
		useJavaBeanIntrospector,
		sortProperties,
		debug;

	final Visibility
		beanConstructorVisibility,
		beanClassVisibility,
		beanMethodVisibility,
		beanFieldVisibility;

	final Class<?>[] notBeanClasses, beanDictionaryClasses;
	final String[] notBeanPackageNames, notBeanPackagePrefixes;
	final BeanFilter[] beanFilters;
	final PojoSwap<?,?>[] pojoSwaps;
	final BeanRegistry beanRegistry;
	final Map<Class<?>,Class<?>> implClasses;
	final Class<?>[] implKeyClasses, implValueClasses;
	final ClassLoader classLoader;
	final Locale locale;
	final TimeZone timeZone;
	final MediaType mediaType;

	final Map<Class,ClassMeta> cmCache;
	final ClassMeta<Object> cmObject;  // Reusable ClassMeta that represents general Objects.
	final ClassMeta<String> cmString;  // Reusable ClassMeta that represents general Strings.
	final ClassMeta<Class> cmClass;  // Reusable ClassMeta that represents general Classes.

	// Optional default parser set by setDefaultParser().
	final ReaderParser defaultParser;

	final String beanTypePropertyName;

	final int hashCode;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)} or {@link ContextFactory#getBeanContext()}.
	 *
	 * @param cf The factory that created this context.
	 */
	public BeanContext(ContextFactory cf) {
		super(cf);

		ContextFactory.PropertyMap pm = cf.getPropertyMap("BeanContext");
		hashCode = pm.hashCode();
		classLoader = cf.classLoader;
		defaultParser = cf.defaultParser;

		beansRequireDefaultConstructor = pm.get(BEAN_beansRequireDefaultConstructor, boolean.class, false);
		beansRequireSerializable = pm.get(BEAN_beansRequireSerializable, boolean.class, false);
		beansRequireSettersForGetters = pm.get(BEAN_beansRequireSettersForGetters, boolean.class, false);
		beansRequireSomeProperties = pm.get(BEAN_beansRequireSomeProperties, boolean.class, true);
		beanMapPutReturnsOldValue = pm.get(BEAN_beanMapPutReturnsOldValue, boolean.class, false);
		useInterfaceProxies = pm.get(BEAN_useInterfaceProxies, boolean.class, true);
		ignoreUnknownBeanProperties = pm.get(BEAN_ignoreUnknownBeanProperties, boolean.class, false);
		ignoreUnknownNullBeanProperties = pm.get(BEAN_ignoreUnknownNullBeanProperties, boolean.class, true);
		ignorePropertiesWithoutSetters = pm.get(BEAN_ignorePropertiesWithoutSetters, boolean.class, true);
		ignoreInvocationExceptionsOnGetters = pm.get(BEAN_ignoreInvocationExceptionsOnGetters, boolean.class, false);
		ignoreInvocationExceptionsOnSetters = pm.get(BEAN_ignoreInvocationExceptionsOnSetters, boolean.class, false);
		useJavaBeanIntrospector = pm.get(BEAN_useJavaBeanIntrospector, boolean.class, false);
		sortProperties = pm.get(BEAN_sortProperties, boolean.class, false);
		beanTypePropertyName = pm.get(BEAN_beanTypePropertyName, String.class, "_type");
		debug = cf.getProperty(BEAN_debug, boolean.class, false);

		beanConstructorVisibility = pm.get(BEAN_beanConstructorVisibility, Visibility.class, PUBLIC);
		beanClassVisibility = pm.get(BEAN_beanClassVisibility, Visibility.class, PUBLIC);
		beanMethodVisibility = pm.get(BEAN_methodVisibility, Visibility.class, PUBLIC);
		beanFieldVisibility = pm.get(BEAN_beanFieldVisibility, Visibility.class, PUBLIC);

		notBeanClasses = pm.get(BEAN_notBeanClasses, Class[].class, new Class[0]);

		List<String> l1 = new LinkedList<String>();
		List<String> l2 = new LinkedList<String>();
		for (String s : pm.get(BEAN_notBeanPackages, String[].class, new String[0])) {
			if (s.endsWith(".*"))
				l2.add(s.substring(0, s.length()-2));
			else
				l1.add(s);
		}
		notBeanPackageNames = l1.toArray(new String[l1.size()]);
		notBeanPackagePrefixes = l2.toArray(new String[l2.size()]);

		LinkedList<BeanFilter> lbf = new LinkedList<BeanFilter>();
 		try {
			for (Class<?> c : pm.get(BEAN_beanFilters, Class[].class, new Class[0])) {
				if (isParentClass(BeanFilter.class, c))
					lbf.add((BeanFilter)c.newInstance());
				else if (isParentClass(BeanFilterBuilder.class, c))
					lbf.add(((BeanFilterBuilder)c.newInstance()).build());
				else
					lbf.add(new InterfaceBeanFilterBuilder(c).build());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
 		beanFilters = lbf.toArray(new BeanFilter[0]);

		LinkedList<PojoSwap<?,?>> lpf = new LinkedList<PojoSwap<?,?>>();
 		try {
			for (Class<?> c : pm.get(BEAN_pojoSwaps, Class[].class, new Class[0])) {
				if (isParentClass(PojoSwap.class, c))
					lpf.add((PojoSwap<?,?>)c.newInstance());
				else
					lpf.addAll(SurrogateSwap.findPojoSwaps(c));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
 		pojoSwaps = lpf.toArray(new PojoSwap[0]);

 		implClasses = new TreeMap<Class<?>,Class<?>>(new ClassComparator());
 		Map<Class,Class> m = pm.getMap(BEAN_implClasses, Class.class, Class.class, null);
 		if (m != null)
	 		for (Map.Entry<Class,Class> e : m.entrySet())
	 			implClasses.put(e.getKey(), e.getValue());
		implKeyClasses = implClasses.keySet().toArray(new Class[0]);
		implValueClasses = implClasses.values().toArray(new Class[0]);

		locale = pm.get(BEAN_locale, Locale.class, Locale.getDefault());
		timeZone = pm.get(BEAN_timeZone, TimeZone.class, null);
		mediaType = pm.get(BEAN_mediaType, MediaType.class, null);

		if (! cmCacheCache.containsKey(hashCode)) {
			ConcurrentHashMap<Class,ClassMeta> cm = new ConcurrentHashMap<Class,ClassMeta>();
			cm.putIfAbsent(String.class, new ClassMeta(String.class, this, null, null, findPojoSwap(String.class), findChildPojoSwaps(String.class)));
			cm.putIfAbsent(Object.class, new ClassMeta(Object.class, this, null, null, findPojoSwap(Object.class), findChildPojoSwaps(Object.class)));
			cmCacheCache.putIfAbsent(hashCode, cm);
		}
		this.cmCache = cmCacheCache.get(hashCode);
		this.cmString = cmCache.get(String.class);
		this.cmObject = cmCache.get(Object.class);
		this.cmClass = cmCache.get(Class.class);

		this.beanDictionaryClasses = pm.get(BEAN_beanDictionary, Class[].class, new Class[0]);
		this.beanRegistry = new BeanRegistry(this, null);
	}

	/**
	 * Create a new bean session based on the properties defined on this context.
	 *
	 * @param op The override properties.
	 * 	This map can contain values to override properties defined on this context.
	 * 	Note that only session-overridable settings can be overridden.
	 * @param locale The bean session locale.
	 * 	Typically used by {@link PojoSwap PojoSwaps} to provide locale-specific output.
	 * 	If <jk>null</jk>, the system default locale is assumed.
	 * @param timeZone The bean session timezone.
	 * 	Typically used by time-sensitive {@link PojoSwap PojoSwaps} to provide timezone-specific output.
	 * 	If <jk>null</jk> the system default timezone is assumed on {@link Date} objects, or the
	 * 		locale specified on {@link Calendar} objects are used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 * @return A new session object.
	 */
	public BeanSession createSession(ObjectMap op, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new BeanSession(this, op, locale, timeZone, mediaType);
	}

	/**
	 * Create a new bean session based on the properties defined on this context.
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @return A new session object.
	 */
	public BeanSession createSession() {
		return new BeanSession(this, null, this.locale, this.timeZone, this.mediaType);
	}

	/**
	 * Returns <jk>true</jk> if the specified bean context shares the same cache as this bean context.
	 * Useful for testing purposes.
	 *
	 * @param bc The bean context to compare to.
	 * @return <jk>true</jk> if the bean contexts have equivalent settings and thus share caches.
	 */
	public final boolean hasSameCache(BeanContext bc) {
		return bc.cmCache == this.cmCache;
	}

	/**
	 * Determines whether the specified class is ignored as a bean class based on the various
	 * 	exclusion parameters specified on this context class.
	 *
	 * @param c The class type being tested.
	 * @return <jk>true</jk> if the specified class matches any of the exclusion parameters.
	 */
	protected final boolean isNotABean(Class<?> c) {
		if (c.isArray() || c.isPrimitive() || c.isEnum() || c.isAnnotation())
			return true;
		Package p = c.getPackage();
		if (p != null) {
			for (String p2 : notBeanPackageNames)
				if (p.getName().equals(p2))
					return true;
			for (String p2 : notBeanPackagePrefixes)
				if (p.getName().startsWith(p2))
					return true;
		}
		for (Class exclude : notBeanClasses)
			if (isParentClass(exclude, c))
				return true;
		return false;
	}

	/**
	 * Prints meta cache statistics to <code>System.out</code>.
	 */
	protected static void dumpCacheStats() {
		try {
			int ctCount = 0;
			for (Map<Class,ClassMeta> cm : cmCacheCache.values())
				ctCount += cm.size();
			System.out.println(MessageFormat.format("ClassMeta cache: {0} instances in {1} caches", ctCount, cmCacheCache.size())); // NOT DEBUG
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the {@link BeanMeta} class for the specified class.
	 *
	 * @param <T> The class type to get the meta-data on.
	 * @param c The class to get the meta-data on.
	 * @return The {@link BeanMeta} for the specified class, or <jk>null</jk> if the class
	 * 	is not a bean per the settings on this context.
	 */
	public final <T> BeanMeta<T> getBeanMeta(Class<T> c) {
		if (c == null)
			return null;
		return getClassMeta(c).getBeanMeta();
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Class} object.
	 *
	 * @param <T> The class type being wrapped.
	 * @param type The class to resolve.
	 * @return If the class is not an array, returns a cached {@link ClassMeta} object.
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.<br>
	 */
	public final <T> ClassMeta<T> getClassMeta(Class<T> type) {

		// If this is an array, then we want it wrapped in an uncached ClassMeta object.
		// Note that if it has a pojo swap, we still want to cache it so that
		// we can cache something like byte[] with ByteArrayBase64Swap.
		if (type.isArray() && findPojoSwap(type) == null)
			return new ClassMeta(type, this, findImplClass(type), findBeanFilter(type), findPojoSwap(type), findChildPojoSwaps(type));

		// This can happen if we have transforms defined against String or Object.
		if (cmCache == null)
			return null;

		ClassMeta<T> cm = cmCache.get(type);
		if (cm == null) {

			synchronized (this) {
				// Make sure someone didn't already set it while this thread was blocked.
				cm = cmCache.get(type);
				if (cm == null)
					cm = new ClassMeta<T>(type, this, findImplClass(type), findBeanFilter(type), findPojoSwap(type), findChildPojoSwaps(type));
			}
		}
		return cm;
	}

	/**
	 * Used to resolve <code>ClassMetas</code> of type <code>Collection</code> and <code>Map</code> that have
	 * <code>ClassMeta</code> values that themselves could be collections or maps.
	 * <p>
	 * <code>Collection</code> meta objects are assumed to be followed by zero or one meta objects indicating the element type.
	 * <p>
	 * <code>Map</code> meta objects are assumed to be followed by zero or two meta objects indicating the key and value types.
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><code>getClassMeta(String.<jk>class</jk>);</code> - A normal type.
	 * 	<li><code>getClassMeta(List.<jk>class</jk>);</code> - A list containing objects.
	 * 	<li><code>getClassMeta(List.<jk>class</jk>, String.<jk>class</jk>);</code> - A list containing strings.
	 * 	<li><code>getClassMeta(LinkedList.<jk>class</jk>, String.<jk>class</jk>);</code> - A linked-list containing strings.
	 * 	<li><code>getClassMeta(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);</code> - A linked-list containing linked-lists of strings.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>);</code> - A map containing object keys/values.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);</code> - A map containing string keys/values.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);</code> - A map containing string keys and values of lists containing beans.
	 * </ul>
	 *
	 * @param type The class to resolve.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The resolved class meta.
	 */
	public final <T> ClassMeta<T> getClassMeta(Type type, Type...args) {
		if (type == null)
			return null;
		ClassMeta<T> cm = type instanceof Class ? getClassMeta((Class)type) : resolveClassMeta(type, null);
		if (args.length == 0)
			return cm;
		ClassMeta<?>[] cma = new ClassMeta[args.length+1];
		cma[0] = cm;
		for (int i = 0; i < Array.getLength(args); i++) {
			Type arg = (Type)Array.get(args, i);
			cma[i+1] = arg instanceof Class ? getClassMeta((Class)arg) : resolveClassMeta(arg, null);
		}
		return (ClassMeta<T>) getTypedClassMeta(cma, 0);
	}

	/*
	 * Resolves the 'genericized' class meta at the specified position in the ClassMeta array.
	 */
	private ClassMeta<?> getTypedClassMeta(ClassMeta<?>[] c, int pos) {
		ClassMeta<?> cm = c[pos++];
		if (cm.isCollection()) {
			ClassMeta<?> ce = c.length == pos ? object() : getTypedClassMeta(c, pos);
			return (ce.isObject() ? cm : new ClassMeta(cm, null, null, ce));
		} else if (cm.isMap()) {
			 ClassMeta<?> ck = c.length == pos ? object() : c[pos++];
			 ClassMeta<?> cv = c.length == pos ? object() : getTypedClassMeta(c, pos);
			 return (ck.isObject() && cv.isObject() ? cm : new ClassMeta(cm, ck, cv, null));
		}
		return cm;
	}

	final ClassMeta resolveClassMeta(Type o, Map<Class<?>,Class<?>[]> typeVarImpls) {
		if (o == null)
			return null;

		if (o instanceof ClassMeta)
			return (ClassMeta)o;

		Class c = null;
		if (o instanceof Class) {
			c = (Class)o;
		} else if (o instanceof ParameterizedType) {
			// A parameter (e.g. <String>.
			c = (Class<?>)((ParameterizedType)o).getRawType();
		} else if (o instanceof GenericArrayType) {
			// An array parameter (e.g. <byte[]>.
			GenericArrayType gat = (GenericArrayType)o;
			Type gatct = gat.getGenericComponentType();
			if (gatct instanceof Class) {
				Class gatctc = (Class)gatct;
				c = Array.newInstance(gatctc, 0).getClass();
			} else if (gatct instanceof ParameterizedType) {
				Class gatctc = (Class<?>)((ParameterizedType)gatct).getRawType();
				c = Array.newInstance(gatctc, 0).getClass();
			} else {
				return null;
			}
		} else if (o instanceof TypeVariable) {
			if (typeVarImpls != null) {
				TypeVariable t = (TypeVariable) o;
				String varName = t.getName();
				int varIndex = -1;
				Class gc = (Class)t.getGenericDeclaration();
				TypeVariable[] tv = gc.getTypeParameters();
				for (int i = 0; i < tv.length; i++) {
					if (tv[i].getName().equals(varName)) {
						varIndex = i;
					}
				}
				if (varIndex != -1) {

					// If we couldn't find a type variable implementation, that means
					// the type was defined at runtime (e.g. Bean b = new Bean<Foo>();)
					// in which case the type is lost through erasure.
					// Assume java.lang.Object as the type.
					if (! typeVarImpls.containsKey(gc))
						return object();

					return getClassMeta(typeVarImpls.get(gc)[varIndex]);
				}
			}
			// We don't know the bounded type, so just resolve to Object.
			return object();
		} else {
			// This can happen when trying to resolve the "E getFirst()" method on LinkedList, whose type is a TypeVariable
			// These should just resolve to Object.
			return object();
		}

		ClassMeta rawType = getClassMeta(c);

		// If this is a Map or Collection, and the parameter types aren't part
		// of the class definition itself (e.g. class AddressBook extends List<Person>),
		// then we need to figure out the parameters.
		if (rawType.isMap() || rawType.isCollection()) {
			ClassMeta[] params = findParameters(o, c);
			if (params == null)
				return rawType;
			if (rawType.isMap()) {
				if (params.length != 2)
					return rawType;
				if (params[0].isObject() && params[1].isObject())
					return rawType;
				return new ClassMeta(rawType, params[0], params[1], null);
			}
			if (rawType.isCollection()) {
				if (params.length != 1)
					return rawType;
				if (params[0].isObject())
					return rawType;
				return new ClassMeta(rawType, null, null, params[0]);
			}
		}

		return rawType;
	}

	final ClassMeta[] findParameters(Type o, Class c) {
		if (o == null)
			o = c;

		// Loop until we find a ParameterizedType
		if (! (o instanceof ParameterizedType)) {
			loop: do {
				o = c.getGenericSuperclass();
				if (o instanceof ParameterizedType)
					break loop;
				for (Type t : c.getGenericInterfaces()) {
					o = t;
					if (o instanceof ParameterizedType)
						break loop;
				}
				c = c.getSuperclass();
			} while (c != null);
		}

		if (o instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)o;
			if (! pt.getRawType().equals(Enum.class)) {
				List<ClassMeta<?>> l = new LinkedList<ClassMeta<?>>();
				for (Type pt2 : pt.getActualTypeArguments()) {
					if (pt2 instanceof WildcardType || pt2 instanceof TypeVariable)
						return null;
					l.add(resolveClassMeta(pt2, null));
				}
				if (l.isEmpty())
					return null;
				return l.toArray(new ClassMeta[l.size()]);
			}
		}

		return null;
	}

	/**
	 * Shortcut for calling {@code getClassMeta(o.getClass())}.
	 *
	 * @param <T> The class of the object being passed in.
	 * @param o The class to find the class type for.
	 * @return The ClassMeta object, or <jk>null</jk> if {@code o} is <jk>null</jk>.
	 */
	public final <T> ClassMeta<T> getClassMetaForObject(T o) {
		if (o == null)
			return null;
		return (ClassMeta<T>)getClassMeta(o.getClass());
	}


	/**
	 * Used for determining the class type on a method or field where a {@code @BeanProperty} annotation
	 * 	may be present.
	 *
	 * @param <T> The class type we're wrapping.
	 * @param p The property annotation on the type if there is one.
	 * @param t The type.
	 * @param typeVarImpls Contains known resolved type parameters on the specified class so
	 * 	that we can result {@code ParameterizedTypes} and {@code TypeVariables}.<br>
	 * 	Can be <jk>null</jk> if the information is not known.
	 * @return The new {@code ClassMeta} object wrapped around the {@code Type} object.
	 */
	protected final <T> ClassMeta<T> resolveClassMeta(BeanProperty p, Type t, Map<Class<?>,Class<?>[]> typeVarImpls) {
		ClassMeta<T> cm = resolveClassMeta(t, typeVarImpls);
		ClassMeta<T> cm2 = cm;
		if (p != null) {

			if (p.type() != Object.class)
				cm2 = resolveClassMeta(p.type(), typeVarImpls);

			if (cm2.isMap()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class, Object.class} : p.params());
				if (pParams.length != 2)
					throw new RuntimeException("Invalid number of parameters specified for Map (must be 2): " + pParams.length);
				ClassMeta<?> keyType = resolveType(pParams[0], cm2.getKeyType(), cm.getKeyType());
				ClassMeta<?> valueType = resolveType(pParams[1], cm2.getValueType(), cm.getValueType());
				if (keyType.isObject() && valueType.isObject())
					return cm2;
				return new ClassMeta<T>(cm2, keyType, valueType, null);
			}

			if (cm2.isCollection()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class} : p.params());
				if (pParams.length != 1)
					throw new RuntimeException("Invalid number of parameters specified for Collection (must be 1): " + pParams.length);
				ClassMeta<?> elementType = resolveType(pParams[0], cm2.getElementType(), cm.getElementType());
				if (elementType.isObject())
					return cm2;
				return new ClassMeta<T>(cm2, null, null, elementType);
			}

			return cm2;
		}

		return cm;
	}

	private ClassMeta<?> resolveType(Type...t) {
		for (Type tt : t) {
			if (tt != null) {
				ClassMeta<?> cm = getClassMeta(tt);
				if (tt != cmObject)
					return cm;
			}
		}
		return cmObject;
	}

	/**
	 * Returns the {@link PojoSwap} associated with the specified class, or <jk>null</jk> if there is no
	 * pojo swap associated with the class.
	 *
	 * @param <T> The class associated with the swap.
	 * @param c The class associated with the swap.
	 * @return The swap associated with the class, or null if there is no association.
	 */
	private final <T> PojoSwap findPojoSwap(Class<T> c) {
		// Note:  On first
		if (c != null)
			for (PojoSwap f : pojoSwaps)
				if (isParentClass(f.getNormalClass(), c))
					return f;
		return null;
	}

	/**
	 * Checks whether a class has a {@link PojoSwap} associated with it in this bean context.
	 * @param c The class to check.
	 * @return <jk>true</jk> if the specified class or one of its subclasses has a {@link PojoSwap} associated with it.
	 */
	private final PojoSwap[] findChildPojoSwaps(Class<?> c) {
		if (c == null || pojoSwaps.length == 0)
			return null;
		List<PojoSwap> l = null;
		for (PojoSwap f : pojoSwaps) {
			if (isParentClass(c, f.getNormalClass())) {
				if (l == null)
					l = new ArrayList<PojoSwap>();
				l.add(f);
			}
		}
		return l == null ? null : l.toArray(new PojoSwap[l.size()]);
	}

	/**
	 * Returns the {@link BeanFilter} associated with the specified class, or <jk>null</jk> if there is no
	 * bean filter associated with the class.
	 *
	 * @param <T> The class associated with the bean filter.
	 * @param c The class associated with the bean filter.
	 * @return The bean filter associated with the class, or null if there is no association.
	 */
	private final <T> BeanFilter findBeanFilter(Class<T> c) {
		if (c != null)
			for (BeanFilter f : beanFilters)
				if (isParentClass(f.getBeanClass(), c))
					return f;
		return null;
	}

	/**
	 * Returns the type property name as defined by {@link BeanContext#BEAN_beanTypePropertyName}.
	 *
	 * @return The type property name.  Never <jk>null</jk>.
	 */
	protected final String getBeanTypePropertyName() {
		return beanTypePropertyName;
	}

	/**
	 * Returns the bean registry defined in this bean context defined by {@link BeanContext#BEAN_beanDictionary}.
	 *
	 * @return The bean registry defined in this bean context.  Never <jk>null</jk>.
	 */
	protected final BeanRegistry getBeanRegistry() {
		return beanRegistry;
	}

	/**
	 * Gets the no-arg constructor for the specified class.
	 *
	 * @param <T> The class to check.
	 * @param c The class to check.
	 * @param v The minimum visibility for the constructor.
	 * @return The no arg constructor, or <jk>null</jk> if the class has no no-arg constructor.
	 */
	protected final <T> Constructor<? extends T> getImplClassConstructor(Class<T> c, Visibility v) {
		if (implClasses.isEmpty())
			return null;
		Class cc = c;
		while (cc != null) {
			Class implClass = implClasses.get(cc);
			if (implClass != null)
				return findNoArgConstructor(implClass, v);
			for (Class ic : cc.getInterfaces()) {
				implClass = implClasses.get(ic);
				if (implClass != null)
					return findNoArgConstructor(implClass, v);
			}
			cc = cc.getSuperclass();
		}
		return null;
	}

	private final <T> Class<? extends T> findImplClass(Class<T> c) {
		if (implClasses.isEmpty())
			return null;
		Class cc = c;
		while (cc != null) {
			Class implClass = implClasses.get(cc);
			if (implClass != null)
				return implClass;
			for (Class ic : cc.getInterfaces()) {
				implClass = implClasses.get(ic);
				if (implClass != null)
					return implClass;
			}
			cc = cc.getSuperclass();
		}
		return null;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>Object</code>.
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent "any object type" when an object type
	 * 	is not known.
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Object.<jk>class</jk>)</code> but uses
	 * 	a cached copy to avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>Object</code> class.
	 */
	protected final ClassMeta<Object> object() {
		return cmObject;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>String</code>.
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent key types in maps.
	 * <p>
	 * This method is identical to calling <code>getClassMeta(String.<jk>class</jk>)</code> but uses
	 * 	a cached copy to avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>String</code> class.
	 */
	protected final ClassMeta<String> string() {
		return cmString;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>Class</code>.
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent key types in maps.
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Class.<jk>class</jk>)</code> but uses
	 * 	a cached copy to avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>String</code> class.
	 */
	protected final ClassMeta<Class> _class() {
		return cmClass;
	}

	@Override /* Object */
	public int hashCode() {
		return hashCode;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o instanceof BeanContext)
			return ((BeanContext)o).hashCode == hashCode;
		return false;
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("BeanContext", new ObjectMap()
				.append("id", System.identityHashCode(this))
				.append("beansRequireDefaultConstructor", beansRequireDefaultConstructor)
				.append("beansRequireSerializable", beansRequireSerializable)
				.append("beansRequireSettersForGetters", beansRequireSettersForGetters)
				.append("beansRequireSomeProperties", beansRequireSomeProperties)
				.append("beanMapPutReturnsOldValue", beanMapPutReturnsOldValue)
				.append("beanConstructorVisibility", beanConstructorVisibility)
				.append("beanClassVisibility", beanClassVisibility)
				.append("beanMethodVisibility", beanMethodVisibility)
				.append("beanFieldVisibility", beanFieldVisibility)
				.append("useInterfaceProxies", useInterfaceProxies)
				.append("ignoreUnknownBeanProperties", ignoreUnknownBeanProperties)
				.append("ignoreUnknownNullBeanProperties", ignoreUnknownNullBeanProperties)
				.append("ignorePropertiesWithoutSetters", ignorePropertiesWithoutSetters)
				.append("ignoreInvocationExceptionsOnGetters", ignoreInvocationExceptionsOnGetters)
				.append("ignoreInvocationExceptionsOnSetters", ignoreInvocationExceptionsOnSetters)
				.append("useJavaBeanIntrospector", useJavaBeanIntrospector)
				.append("beanFilters", beanFilters)
				.append("pojoSwaps", pojoSwaps)
				.append("notBeanClasses", notBeanClasses)
				.append("implClasses", implClasses)
				.append("sortProperties", sortProperties)
				.append("locale", locale)
				.append("timeZone", timeZone)
				.append("mediaType", mediaType)
			);
	}
}
