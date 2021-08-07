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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ExceptionUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.utils.ReflectionMapBuilder;

/**
 * Core class of the Juneau architecture.
 * {@review}
 *
 * <p class='w800'>
 * This class servers multiple purposes:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Provides the ability to wrap beans inside {@link Map} interfaces.
 * 	<li>
 * 		Serves as a repository for metadata on POJOs, such as associated {@link Bean @Bean} annotations,
 * 		{@link PropertyNamer PropertyNamers}, etc...  which are used to tailor how POJOs are serialized and parsed.
 * </ul>
 *
 * <p class='w800'>
 * All serializers and parsers extend from this context so that they can handle POJOs using a common framework.
 *
 * <h5 class='topic'>Bean Contexts</h5>
 *
 * <p class='w800'>
 * Bean contexts are created through the {@link BeanContext#create() BeanContext.create()} and {@link BeanContextBuilder#build()} methods.
 * <br>These context objects are read-only, reusable, and thread-safe.
 *
 * <p class='w800'>
 * Each bean context maintains a cache of {@link ClassMeta} objects that describe information about classes encountered.
 * These <c>ClassMeta</c> objects are time-consuming to construct.
 * Therefore, instances of {@link BeanContext} that share the same <js>"BeanContext.*"</js> property values share
 * the same cache.  This allows for efficient reuse of <c>ClassMeta</c> objects so that the information about
 * classes only needs to be calculated once.
 * Because of this, many of the properties defined on the {@link BeanContext} class cannot be overridden on the session.
 *
 * <h5 class='topic'>Bean Sessions</h5>
 *
 * <p class='w800'>
 * Whereas <c>BeanContext</c> objects are permanent, unchangeable, cached, and thread-safe,
 * {@link BeanSession} objects are ephemeral and not thread-safe.
 * They are meant to be used as quickly-constructed scratchpads for creating bean maps.
 * {@link BeanMap} objects can only be created through the session.
 *
 * <h5 class='topic'>BeanContext configuration properties</h5>
 *
 * <p class='w800'>
 * <c>BeanContexts</c> have several configuration properties that can be used to tweak behavior on how beans are
 * handled.  These are denoted as the static <jsf>BEAN_*</jsf> fields on this class.
 *
 * <p class='w800'>
 * Some settings (e.g. {@link #BEAN_beansRequireDefaultConstructor}) are used to differentiate between bean
 * and non-bean classes.
 * Attempting to create a bean map around one of these objects will throw a {@link BeanRuntimeException}.
 * The purpose for this behavior is so that the serializers can identify these non-bean classes and convert them to
 * plain strings using the {@link Object#toString()} method.
 *
 * <p class='w800'>
 * Some settings (e.g. {@link #BEAN_beanFieldVisibility}) are used to determine what kinds of properties are
 * detected on beans.
 *
 * <p class='w800'>
 * Some settings (e.g. {@link #BEAN_beanMapPutReturnsOldValue}) change the runtime behavior of bean maps.
 *
 * <p class='w800'>
 * Settings are specified using the {@link BeanContextBuilder#set(String, Object)} method and related convenience
 * methods.
 *
 * <h5 class='section'>Example:</h5>
 *
 * <p class='bcode w800'>
 * 	<jc>// Construct a context from scratch.</jc>
 * 	BeanContext <jv>beanContext</jv> = BeanContext
 * 		.<jsm>create</jsm>()
 * 		.beansRequireDefaultConstructor()
 * 		.notBeanClasses(Foo.<jk>class</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='topic'>Bean Maps</h5>
 *
 * <p class='w800'>
 * {@link BeanMap BeanMaps} are wrappers around Java beans that allow properties to be retrieved and
 * set using the common {@link Map#put(Object,Object)} and {@link Map#get(Object)} methods.
 *
 * <p class='w800'>
 * Bean maps are created in two ways...
 * <ol>
 * 	<li>{@link BeanSession#toBeanMap(Object) BeanSession.toBeanMap()} - Wraps an existing bean inside a {@code Map}
 * 		wrapper.
 * 	<li>{@link BeanSession#newBeanMap(Class) BeanSession.newBeanMap()} - Create a new bean instance wrapped in a
 * 		{@code Map} wrapper.
 * </ol>
 *
 * <h5 class='section'>Example:</h5>
 *
 * <p class='bcode w800'>
 * 	<jc>// A sample bean class</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> String getName();
 * 		<jk>public void</jk> setName(String <jv>name</jv>);
 * 		<jk>public int</jk> getAge();
 * 		<jk>public void</jk> setAge(<jk>int</jk> <jv>age</jv>);
 * 	}
 *
 * 	<jc>// Create a new bean session</jc>
 * 	BeanSession <jv>session</jv> = BeanContext.<jsf>DEFAULT</jsf>.createSession();
 *
 * 	<jc>// Wrap an existing bean in a new bean map</jc>
 * 	BeanMap&lt;Person&gt; <jv>m1</jv> = <jv>session</jv>.toBeanMap(<jk>new</jk> Person());
 * 	<jv>m1</jv>.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	<jv>m1</jv>.put(<js>"age"</js>, 45);
 *
 * 	<jc>// Create a new bean instance wrapped in a new bean map</jc>
 * 	BeanMap&lt;Person&gt; <jv>m2</jv> = <jv>session</jv>.newBeanMap(Person.<jk>class</jk>);
 * 	<jv>m2</jv>.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	<jv>m2</jv>.put(<js>"age"</js>, 45);
 * 	Person <jv>p</jv> = <jv>m2</jv>.getBean();  <jc>// Get the bean instance that was created.</jc>
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc BeanContext}
 * </ul>
 */
@SuppressWarnings({"unchecked","rawtypes"})
@ConfigurableContext
public class BeanContext extends Context implements MetaProvider {

	static final String PREFIX = "BeanContext";

	/**
	 * Configuration property:  Annotations.
	 *
	 * <p>
	 * Defines annotations to apply to specific classes and methods.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_annotations BEAN_annotations}
	 * 	<li><b>Name:</b>  <js>"BeanContext.annotations.lo"</js>
	 * 	<li><b>Description:</b>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link java.lang.annotation.Annotation}&gt;</c>
	 * 	<li><b>Default:</b>  Empty list.
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#annotations(Annotation...)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_annotations = PREFIX + ".annotations.lo";

	/**
	 * Configuration property:  Minimum bean class visibility.
	 *
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanClassVisibility BEAN_beanClassVisibility}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanClassVisibility.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Visibility}
	 * 	<li><b>System property:</b>  <c>BeanContext.beanClassVisibility</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANCLASSVISIBILITY</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.Visibility#PUBLIC}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanClassVisibility()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanClassVisibility(Visibility)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beanClassVisibility = PREFIX + ".beanClassVisibility.s";

	/**
	 * Configuration property:  Minimum bean constructor visibility.
	 *
	 * <p>
	 * Only look for constructors with the specified minimum visibility.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanConstructorVisibility BEAN_beanConstructorVisibility}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanConstructorVisibility.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Visibility}
	 * 	<li><b>System property:</b>  <c>BeanContext.beanConstructorVisibility</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANCONSTRUCTORVISIBILITY</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.Visibility#PUBLIC}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanConstructorVisibility()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanConstructorVisibility(Visibility)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beanConstructorVisibility = PREFIX + ".beanConstructorVisibility.s";

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary in this bean context.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanDictionary BEAN_beanDictionary}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanDictionary.lc"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;Class&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#dictionary()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Beanp#dictionary()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#dictionary()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#dictionary_replace()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dictionary(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dictionary_replace(Object...)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beanDictionary = PREFIX + ".beanDictionary.lc";

	/**
	 * Configuration property:  Beans don't require at least one property.
	 *
	 * <p>
	 * When enabled, then a Java class doesn't need to contain at least 1 property to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_disableBeansRequireSomeProperties BEAN_disableBeansRequireSomeProperties}
	 * 	<li><b>Name:</b>  <js>"BeanContext.disableBeansRequireSomeProperties.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.disableBeansRequireSomeProperties</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_DIABLEBEANSREQUIRESOMEPROPERTIES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableBeansRequireSomeProperties()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#disableBeansRequireSomeProperties()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_disableBeansRequireSomeProperties = PREFIX + ".disableBeansRequireSomeProperties.b";

	/**
	 * Configuration property:  Minimum bean field visibility.
	 *
	 * <p>
	 * Only look for bean fields with the specified minimum visibility.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanFieldVisibility BEAN_beanFieldVisibility}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanFieldVisibility.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Visibility}
	 * 	<li><b>System property:</b>  <c>BeanContext.beanFieldVisibility</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANFIELDVISIBILITY</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.Visibility#PUBLIC}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanFieldVisibility()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanFieldVisibility(Visibility)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beanFieldVisibility = PREFIX + ".beanFieldVisibility.s";

	/**
	 * Configuration property:  BeanMap.put() returns old property value.
	 *
	 * <p>
	 * When enabled, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
	 * values.  Otherwise, it returns <jk>null</jk>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanMapPutReturnsOldValue BEAN_beanMapPutReturnsOldValue}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanMapPutReturnsOldValue.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.beanMapPutReturnsOldValue</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANMAPPUTRETURNSOLDVALUE</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanMapPutReturnsOldValue()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanMapPutReturnsOldValue()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beanMapPutReturnsOldValue = PREFIX + ".beanMapPutReturnsOldValue.b";

	/**
	 * Configuration property:  Minimum bean method visibility.
	 *
	 * <p>
	 * Only look for bean methods with the specified minimum visibility.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanMethodVisibility BEAN_beanMethodVisibility}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanMethodVisibility.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Visibility}
	 * 	<li><b>System property:</b>  <c>BeanContext.beanMethodVisibility</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANMETHODVISIBILITY</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.Visibility#PUBLIC}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanMethodVisibility()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanMethodVisibility(Visibility)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beanMethodVisibility = PREFIX + ".beanMethodVisibility.s";

	/**
	 * Configuration property:  Beans require no-arg constructors.
	 *
	 * <p>
	 * When enabled, a Java class must implement a default no-arg constructor to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beansRequireDefaultConstructor BEAN_beansRequireDefaultConstructor}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beansRequireDefaultConstructor.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.beansRequireDefaultConstructor</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANSREQUIREDEFAULTCONSTRUCTOR</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beansRequireDefaultConstructor()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beansRequireDefaultConstructor()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beansRequireDefaultConstructor = PREFIX + ".beansRequireDefaultConstructor.b";

	/**
	 * Configuration property:  Beans require Serializable interface.
	 *
	 * <p>
	 * When enabled, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beansRequireSerializable BEAN_beansRequireSerializable}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beansRequireSerializable.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.beansRequireSerializable</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANSREQUIRESERIALIZABLE</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beansRequireSerializable()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beansRequireSerializable()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beansRequireSerializable = PREFIX + ".beansRequireSerializable.b";

	/**
	 * Configuration property:  Beans require setters for getters.
	 *
	 * <p>
	 * When enabled, ignore read-only properties (properties with getters but not setters).
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beansRequireSettersForGetters BEAN_beansRequireSettersForGetters}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beansRequireSettersForGetters.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.beansRequireSettersForGetters</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANSREQUIRESETTERSFORGETTERS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beansRequireSettersForGetters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beansRequireSettersForGetters()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beansRequireSettersForGetters = PREFIX + ".beansRequireSettersForGetters.b";

	/**
	 * Configuration property:  Bean type property name.
	 *
	 * <p>
	 * This specifies the name of the bean property used to store the dictionary name of a bean type so that the
	 * parser knows the data type to reconstruct.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_typePropertyName BEAN_typePropertyName}
	 * 	<li><b>Name:</b>  <js>"BeanContext.typePropertyName.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>BeanContext.typePropertyName</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_TYPEPROPERTYNAME</c>
	 * 	<li><b>Default:</b>  <js>"_type"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#typePropertyName()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#typePropertyName()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#typePropertyName(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_typePropertyName = PREFIX + ".typePropertyName.s";

	/**
	 * Configuration property:  Don't ignore transient fields.
	 *
	 * <p>
	 * When enabled, methods and fields marked as <jk>transient</jk> will not be ignored as bean properties.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_disableIgnoreTransientFields BEAN_disableIgnoreTransientFields}
	 * 	<li><b>Name:</b>  <js>"BeanContext.disableIgnoreTransientFields.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.disableIgnoreTransientFields</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_DISABLEIGNORETRANSIENTFIELDS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableIgnoreTransientFields()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#disableIgnoreTransientFields()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_disableIgnoreTransientFields = PREFIX + ".disableIgnoreTransientFields.b";

	/**
	 * Configuration property:  Don't ignore unknown properties with null values.
	 *
	 * <p>
	 * When enabled, trying to set a <jk>null</jk> value on a non-existent bean property will throw a {@link BeanRuntimeException}.
	 * Otherwise it will be silently ignored.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_disableIgnoreUnknownNullBeanProperties BEAN_disableIgnoreUnknownNullBeanProperties}
	 * 	<li><b>Name:</b>  <js>"BeanContext.disableIgnoreUnknownNullBeanProperties.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.disableIgnoreUnknownNullBeanProperties</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_DISABLEIGNOREUNKNOWNNULLBEANPROPERTIES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableIgnoreUnknownNullBeanProperties()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#disableIgnoreUnknownNullBeanProperties()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_disableIgnoreUnknownNullBeanProperties = PREFIX + ".disableIgnoreUnknownNullBeanProperties.b";

	/**
	 * Configuration property:  Don't silently ignore missing setters.
	 *
	 * <p>
	 * When enabled, trying to set a value on a bean property without a setter will throw a {@link BeanRuntimeException}.
	 * Otherwise, it will be silently ignored.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_disableIgnoreMissingSetters BEAN_disableIgnoreMissingSetters}
	 * 	<li><b>Name:</b>  <js>"BeanContext.disableIgnoreMissingSetters.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.disableIgnoreMissingSetters</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_DISABLEIGNOREMISSINGSETTERS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableIgnoreMissingSetters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#disableIgnoreMissingSetters()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_disableIgnoreMissingSetters = PREFIX + ".disableIgnoreMissingSetters.b";

	/**
	 * Configuration property:  Don't use interface proxies.
	 *
	 * <p>
	 * When <jk>false</jk>, interfaces will be instantiated as proxy classes through the use of an
	 * {@link InvocationHandler} if there is no other way of instantiating them.
	 * Otherwise, throws a {@link BeanRuntimeException}.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_disableInterfaceProxies BEAN_disableInterfaceProxies}
	 * 	<li><b>Name:</b>  <js>"BeanContext.disableInterfaceProxies.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.disableInterfaceProxies</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_DISABLEINTERFACEPROXIES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableInterfaceProxies()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#disableInterfaceProxies()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_disableInterfaceProxies = PREFIX + ".disableInterfaceProxies.b";

	/**
	 * Configuration property:  Find fluent setters.
	 *
	 * <p>
	 * When enabled, fluent setters are detected on beans during parsing.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_findFluentSetters BEAN_findFluentSetters}
	 * 	<li><b>Name:</b>  <js>"BeanContext.findFluentSetters.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.findFluentSetters</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_FINDFLUENTSETTERS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#findFluentSetters()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#findFluentSetters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#findFluentSetters()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_findFluentSetters = PREFIX + ".findFluentSetters.b";

	/**
	 * Configuration property:  Ignore invocation errors on getters.
	 *
	 * <p>
	 * When enabled, errors thrown when calling bean getter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_ignoreInvocationExceptionsOnGetters BEAN_ignoreInvocationExceptionsOnGetters}
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignoreInvocationExceptionsOnGetters.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.ignoreInvocationExceptionsOnGetters</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_IGNOREINVOCATIONEXCEPTIONONGETTERS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreInvocationExceptionsOnGetters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#ignoreInvocationExceptionsOnGetters()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnGetters = PREFIX + ".ignoreInvocationExceptionsOnGetters.b";

	/**
	 * Configuration property:  Ignore invocation errors on setters.
	 *
	 * <p>
	 * When enabled, errors thrown when calling bean setter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_ignoreInvocationExceptionsOnSetters BEAN_ignoreInvocationExceptionsOnSetters}
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignoreInvocationExceptionsOnSetters.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.ignoreInvocationExceptionsOnSetters</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_IGNOREINVOCATIONEXCEPTIONSONSETTERS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreInvocationExceptionsOnSetters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#ignoreInvocationExceptionsOnSetters()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnSetters = PREFIX + ".ignoreInvocationExceptionsOnSetters.b";

	/**
	 * Configuration property:  Ignore unknown properties.
	 *
	 * <p>
	 * When enabled, trying to set a value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_ignoreUnknownBeanProperties BEAN_ignoreUnknownBeanProperties}
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignoreUnknownBeanProperties.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.ignoreUnknownBeanProperties</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_IGNOREUNKNOWNBEANPROPERTIES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreUnknownBeanProperties()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#ignoreUnknownBeanProperties()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_ignoreUnknownBeanProperties = PREFIX + ".ignoreUnknownBeanProperties.b";

	/**
	 * Configuration property:  Locale.
	 *
	 * <p>
	 * Specifies the default locale for serializer and parser sessions when not specified via {@link BeanSessionArgs#locale(Locale)}.
	 * Typically used for POJO swaps that need to deal with locales such as swaps that convert <l>Date</l> and <l>Calendar</l>
	 * objects to strings by accessing it via the session passed into the {@link PojoSwap#swap(BeanSession, Object)} and
	 * {@link PojoSwap#unswap(BeanSession, Object, ClassMeta, String)} methods.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_locale CONTEXT_locale}
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
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#locale(Locale)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanSessionArgs#locale(Locale)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_locale = PREFIX + ".locale.s";

	/**
	 * Configuration property:  Media type.
	 *
	 * <p>
	 * Specifies the default media type for serializer and parser sessions when not specified via {@link BeanSessionArgs#mediaType(MediaType)}.
	 * Typically used for POJO swaps that need to serialize the same POJO classes differently depending on
	 * the specific requested media type.   For example, a swap could handle a request for media types <js>"application/json"</js>
	 * and <js>"application/json+foo"</js> slightly differently even though they're both being handled by the same JSON
	 * serializer or parser.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_mediaType CONTEXT_mediaType}
	 * 	<li><b>Name:</b>  <js>"Context.mediaType.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.http.header.MediaType}
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
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#mediaType(MediaType)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanSessionArgs#mediaType(MediaType)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_mediaType = PREFIX + ".mediaType.s";

	/**
	 * Configuration property:  Bean class exclusions.
	 *
	 * <p>
	 * List of classes that should not be treated as beans even if they appear to be bean-like.
	 * Not-bean classes are converted to <c>Strings</c> during serialization.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_notBeanClasses BEAN_notBeanClasses}
	 * 	<li><b>Name:</b>  <js>"BeanContext.notBeanClasses.sc"</js>
	 * 	<li><b>Data type:</b>  <c>Set&lt;Class&gt;</c>
	 * 	<li><b>Default:</b>  empty set
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanIgnore}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#notBeanClasses()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#notBeanClasses_replace()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanClasses(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanClasses_replace(Object...)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_notBeanClasses = PREFIX + ".notBeanClasses.sc";

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * <p>
	 * Used as a convenient way of defining the {@link #BEAN_notBeanClasses} property for entire packages.
	 * Any classes within these packages will be serialized to strings using {@link Object#toString()}.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_notBeanPackages BEAN_notBeanPackages}
	 * 	<li><b>Name:</b>  <js>"BeanContext.notBeanPackages.ss"</js>
	 * 	<li><b>Data type:</b>  <c>Set&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>BeanContext.notBeanPackages</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_NOTBEANPACKAGES</c>
	 * 	<li><b>Default:</b>
	 * 	<ul>
	 * 		<li><c>java.lang</c>
	 * 		<li><c>java.lang.annotation</c>
	 * 		<li><c>java.lang.ref</c>
	 * 		<li><c>java.lang.reflect</c>
	 * 		<li><c>java.io</c>
	 * 		<li><c>java.net</c>
	 * 		<li><c>java.nio.*</c>
	 * 		<li><c>java.util.*</c>
	 * 	</ul>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#notBeanPackages()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanPackages(Object...)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_notBeanPackages = PREFIX + ".notBeanPackages.ss";

	/**
	 * Configuration property:  Bean property namer.
	 *
	 * <p>
	 * The class to use for calculating bean property names..
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_propertyNamer BEAN_propertyNamer}
	 * 	<li><b>Name:</b>  <js>"BeanContext.propertyNamer.c"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;{@link org.apache.juneau.PropertyNamer}&gt;</code>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.BasicPropertyNamer}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#propertyNamer()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#propertyNamer()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#propertyNamer(Class)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_propertyNamer = PREFIX + ".propertyNamer.c";

	/**
	 * Configuration property:  Sort bean properties.
	 *
	 * <p>
	 * When enabled, all bean properties will be serialized and access in alphabetical order.
	 * Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 * On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
	 * On Oracle JVMs, the bean properties are not ordered (which follows the official JVM specs).
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_sortProperties BEAN_sortProperties}
	 * 	<li><b>Name:</b>  <js>"BeanContext.sortProperties.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.sortProperties</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_SORTPROPERTIES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#sort()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#sortProperties()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#sortProperties()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_sortProperties = PREFIX + ".sortProperties.b";

	/**
	 * Configuration property:  Java object swaps.
	 *
	 * <p>
	 * Swaps are used to "swap out" non-serializable classes with serializable equivalents during serialization,
	 * and "swap in" the non-serializable class during parsing..
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_swaps BEAN_swaps}
	 * 	<li><b>Name:</b>  <js>"BeanContext.swaps.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;Object&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Swap}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#swaps()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#swaps(Object...)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_swaps = PREFIX + ".swaps.lo";

	/**
	 * Configuration property:  Time zone.
	 *
	 * <p>
	 * Specifies the default time zone for serializer and parser sessions when not specified via {@link BeanSessionArgs#timeZone(TimeZone)}.
	 * Typically used for POJO swaps that need to deal with timezones such as swaps that convert <l>Date</l> and <l>Calendar</l>
	 * objects to strings by accessing it via the session passed into the {@link PojoSwap#swap(BeanSession, Object)} and
	 * {@link PojoSwap#unswap(BeanSession, Object, ClassMeta, String)} methods.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_timeZone CONTEXT_timeZone}
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
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#timeZone(TimeZone)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanSessionArgs#timeZone(TimeZone)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_timeZone = PREFIX + ".timeZone.s";

	/**
	 * Configuration property:  Use enum names.
	 *
	 * <p>
	 * When enabled, enums are always serialized by name, not using {@link Object#toString()}..
	 *
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_useEnumNames BEAN_useEnumNames}
	 * 	<li><b>Name:</b>  <js>"BeanContext.useEnumNames.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.useEnumNames</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_USEENUMNAMES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#useEnumNames()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#useEnumNames()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_useEnumNames = PREFIX + ".useEnumNames.b";

	/**
	 * Configuration property:  Use Java Introspector.
	 *
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 * <br>Most {@link Bean @Bean} annotations will be ignored..
	 *
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_useJavaBeanIntrospector BEAN_useJavaBeanIntrospector}
	 * 	<li><b>Name:</b>  <js>"BeanContext.useJavaBeanIntrospector.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.useJavaBeanIntrospector</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_USEJAVABEANINTROSPECTOR</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#useJavaBeanIntrospector()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#useJavaBeanIntrospector()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_useJavaBeanIntrospector = PREFIX + ".useJavaBeanIntrospector.b";

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


	// This map is important!
	// We may have many Context objects that have identical BeanContext properties.
	// This map ensures that if the BeanContext properties in the Context are the same,
	// then we reuse the same Class->ClassMeta cache map.
	// This significantly reduces the number of times we need to construct ClassMeta objects which can be expensive.
	private static final ConcurrentHashMap<ContextProperties,Map<Class,ClassMeta>> cmCacheCache
		= new ConcurrentHashMap<>();

	/** Default config.  All default settings. */
	public static final BeanContext DEFAULT = BeanContext.create().build();

	/** Default config.  All default settings except sort bean properties. */
	public static final BeanContext DEFAULT_SORTED = BeanContext.create().sortProperties().build();

	/** Default reusable unmodifiable session.  Can be used to avoid overhead of creating a session (for creating BeanMaps for example).*/
	public  static final BeanSession DEFAULT_SESSION = new BeanSession(DEFAULT, DEFAULT.createDefaultBeanSessionArgs().unmodifiable());

	private final boolean
		beansRequireDefaultConstructor,
		beansRequireSerializable,
		beansRequireSettersForGetters,
		beansRequireSomeProperties,
		beanMapPutReturnsOldValue,
		useInterfaceProxies,
		ignoreUnknownBeanProperties,
		ignoreUnknownNullBeanProperties,
		ignoreMissingSetters,
		ignoreTransientFields,
		ignoreInvocationExceptionsOnGetters,
		ignoreInvocationExceptionsOnSetters,
		useJavaBeanIntrospector,
		useEnumNames,
		sortProperties,
		findFluentSetters;

	private final Visibility
		beanConstructorVisibility,
		beanClassVisibility,
		beanMethodVisibility,
		beanFieldVisibility;

	private final Class<?>[] notBeanClasses;
	private final List<Class<?>> beanDictionaryClasses;
	private final String[] notBeanPackageNames, notBeanPackagePrefixes;
	private final PojoSwap<?,?>[] swaps;
	private final BeanRegistry beanRegistry;
	private final PropertyNamer propertyNamer;
	private final String typePropertyName;
	private final ReflectionMap<Annotation> annotations;

	private final Locale locale;
	private final TimeZone timeZone;
	private final MediaType mediaType;

	final Map<Class,ClassMeta> cmCache;
	private final ClassMeta<Object> cmObject;  // Reusable ClassMeta that represents general Objects.
	private final ClassMeta<String> cmString;  // Reusable ClassMeta that represents general Strings.
	private final ClassMeta<Class> cmClass;  // Reusable ClassMeta that represents general Classes.

	private volatile WriterSerializer beanToStringSerializer;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Typically only called from {@link ContextBuilder#build(Class)} method.
	 *
	 * @param cp The property store containing the unmodifiable configuration for this bean context.
	 */
	public BeanContext(ContextProperties cp) {
		super(cp, true);

		if (cp == null)
			cp = ContextProperties.DEFAULT;

		cp = cp.subset(new String[]{"Context","BeanContext"});

		ReflectionMapBuilder<Annotation> rmb = ReflectionMap.create(Annotation.class);
		for (Annotation a : cp.getList(BEAN_annotations, Annotation.class).orElse(emptyList())) {
			try {
				ClassInfo ci = ClassInfo.of(a.getClass());

				MethodInfo mi = ci.getMethod("onClass");
				if (mi != null) {
					if (! mi.getReturnType().is(Class[].class))
						throw new ConfigException("Invalid annotation @{0} used in BEAN_annotations property.  Annotation must define an onClass() method that returns a Class array.", a.getClass().getSimpleName());
					for (Class<?> c : (Class<?>[])mi.accessible().invoke(a))
						rmb.append(c.getName(), a);
				}

				mi = ci.getMethod("on");
				if (mi != null) {
					if (! mi.getReturnType().is(String[].class))
						throw new ConfigException("Invalid annotation @{0} used in BEAN_annotations property.  Annotation must define an on() method that returns a String array.", a.getClass().getSimpleName());
					for (String s : (String[])mi.accessible().invoke(a))
						rmb.append(s, a);
				}

			} catch (Exception e) {
				throw new ConfigException(e, "Invalid annotation @{0} used in BEAN_annotations property.", className(a));
			}
		}
		this.annotations = rmb.build();

		beansRequireDefaultConstructor = cp.getBoolean(BEAN_beansRequireDefaultConstructor).orElse(false);
		beansRequireSerializable = cp.getBoolean(BEAN_beansRequireSerializable).orElse(false);
		beansRequireSettersForGetters = cp.getBoolean(BEAN_beansRequireSettersForGetters).orElse(false);
		beansRequireSomeProperties = ! cp.getBoolean(BEAN_disableBeansRequireSomeProperties).orElse(false);
		beanMapPutReturnsOldValue = cp.getBoolean(BEAN_beanMapPutReturnsOldValue).orElse(false);
		useEnumNames = cp.getBoolean(BEAN_useEnumNames).orElse(false);
		useInterfaceProxies = ! cp.getBoolean(BEAN_disableInterfaceProxies).orElse(false);
		ignoreUnknownBeanProperties = cp.getBoolean(BEAN_ignoreUnknownBeanProperties).orElse(false);
		ignoreUnknownNullBeanProperties = ! cp.getBoolean(BEAN_disableIgnoreUnknownNullBeanProperties).orElse(false);
		ignoreMissingSetters = ! cp.getBoolean(BEAN_disableIgnoreMissingSetters).orElse(false);
		ignoreTransientFields = ! cp.getBoolean(BEAN_disableIgnoreTransientFields).orElse(false);
		ignoreInvocationExceptionsOnGetters = cp.getBoolean(BEAN_ignoreInvocationExceptionsOnGetters).orElse(false);
		ignoreInvocationExceptionsOnSetters = cp.getBoolean(BEAN_ignoreInvocationExceptionsOnSetters).orElse(false);
		useJavaBeanIntrospector = cp.getBoolean(BEAN_useJavaBeanIntrospector).orElse(false);
		sortProperties = cp.getBoolean(BEAN_sortProperties).orElse(false);
		findFluentSetters = cp.getBoolean(BEAN_findFluentSetters).orElse(false);
		typePropertyName = cp.getString(BEAN_typePropertyName).orElse("_type");

		beanConstructorVisibility = cp.get(BEAN_beanConstructorVisibility, Visibility.class).orElse(PUBLIC);
		beanClassVisibility = cp.get(BEAN_beanClassVisibility, Visibility.class).orElse(PUBLIC);
		beanMethodVisibility = cp.get(BEAN_beanMethodVisibility, Visibility.class).orElse(PUBLIC);
		beanFieldVisibility = cp.get(BEAN_beanFieldVisibility, Visibility.class).orElse(PUBLIC);

		notBeanClasses = cp.getClassArray(BEAN_notBeanClasses).orElse(DEFAULT_NOTBEAN_CLASSES);

		propertyNamer = cp.getInstance(BEAN_propertyNamer, PropertyNamer.class).orElseGet(BasicPropertyNamer::new);

		locale = cp.getInstance(BEAN_locale, Locale.class).orElseGet(()->Locale.getDefault());
		timeZone = cp.getInstance(BEAN_timeZone, TimeZone.class).orElse(null);
		mediaType = cp.getInstance(BEAN_mediaType, MediaType.class).orElse(null);

		List<String> l1 = new LinkedList<>();
		List<String> l2 = new LinkedList<>();
		for (String s : cp.getArray(BEAN_notBeanPackages, String.class).orElse(DEFAULT_NOTBEAN_PACKAGES)) {
			if (s.endsWith(".*"))
				l2.add(s.substring(0, s.length()-2));
			else
				l1.add(s);
		}
		notBeanPackageNames = l1.toArray(new String[l1.size()]);
		notBeanPackagePrefixes = l2.toArray(new String[l2.size()]);

		LinkedList<PojoSwap<?,?>> lpf = new LinkedList<>();
		for (Object o : cp.getList(BEAN_swaps, Object.class).orElse(emptyList())) {
			if (o instanceof Class) {
				ClassInfo ci = ClassInfo.of((Class<?>)o);
				if (ci.isChildOf(PojoSwap.class))
					lpf.add(castOrCreate(PojoSwap.class, ci.inner()));
				else if (ci.isChildOf(Surrogate.class))
					lpf.addAll(SurrogateSwap.findPojoSwaps(ci.inner(), this));
				else
					throw runtimeException("Invalid class {0} specified in BeanContext.swaps property.  Must be a subclass of PojoSwap or Surrogate.", ci.inner());
			} else if (o instanceof PojoSwap) {
				lpf.add((PojoSwap)o);
			}
		}
		swaps = lpf.toArray(new PojoSwap[lpf.size()]);

		if (! cmCacheCache.containsKey(cp)) {
			ConcurrentHashMap<Class,ClassMeta> cm = new ConcurrentHashMap<>();
			cm.putIfAbsent(String.class, new ClassMeta(String.class, this, findPojoSwaps(String.class), findChildPojoSwaps(String.class)));
			cm.putIfAbsent(Object.class, new ClassMeta(Object.class, this, findPojoSwaps(Object.class), findChildPojoSwaps(Object.class)));
			cmCacheCache.putIfAbsent(cp, cm);
		}
		cmCache = cmCacheCache.get(cp);
		cmString = cmCache.get(String.class);
		cmObject = cmCache.get(Object.class);
		cmClass = cmCache.get(Class.class);

		beanDictionaryClasses = AList.unmodifiable(cp.getClassArray(BEAN_beanDictionary).orElse(new Class[0]));
		beanRegistry = new BeanRegistry(this, null);
	}

	@Override /* Context */
	public BeanContextBuilder copy() {
		return new BeanContextBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link BeanContextBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> BeanContextBuilder()</code>.
	 *
	 * @return A new {@link JsonSerializerBuilder} object.
	 */
	public static BeanContextBuilder create() {
		return new BeanContextBuilder();
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
	@Override /* Context */
	public BeanSession createSession() {
		return createBeanSession(createDefaultSessionArgs());
	}

	/**
	 * Create a new bean session based on the properties defined on this context combined with the specified
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
	public BeanSession createSession(BeanSessionArgs args) {
		return createBeanSession(args);
	}

	@Override /* Context */
	public final Session createSession(SessionArgs args) {
		throw new NoSuchMethodError();
	}

	/**
	 * Same as {@link #createSession(BeanSessionArgs)} except always returns a {@link BeanSession} object unlike {@link #createSession(BeanSessionArgs)}
	 * which is meant to be overridden by subclasses.
	 *
	 * @param args The session arguments.
	 * @return A new session object.
	 */
	public final BeanSession createBeanSession(BeanSessionArgs args) {
		return new BeanSession(this, args);
	}

	/**
	 * Same as {@link #createSession()} except always returns a {@link BeanSession} object unlike {@link #createSession()}
	 * which is meant to be overridden by subclasses.
	 *
	 * @return A new session object.
	 */
 	public final BeanSession createBeanSession() {
		return new BeanSession(this, createDefaultBeanSessionArgs());
	}

 	@Override /* Context */
	public BeanSessionArgs createDefaultSessionArgs() {
 		return createDefaultBeanSessionArgs();
	}

	/**
	 * Same as {@link #createDefaultSessionArgs()} except always returns a {@link BeanSessionArgs} unlike
	 * {@link #createDefaultBeanSessionArgs()} which is meant to be overridden by subclasses.
	 *
	 * @return A new session arguments object.
	 */
	public final BeanSessionArgs createDefaultBeanSessionArgs() {
		return new BeanSessionArgs();
	}

	/**
	 * Returns <jk>true</jk> if the specified bean context shares the same cache as this bean context.
	 *
	 * <p>
	 * Useful for testing purposes.
	 *
	 * @param bc The bean context to compare to.
	 * @return <jk>true</jk> if the bean contexts have equivalent settings and thus share caches.
	 */
	public final boolean hasSameCache(BeanContext bc) {
		return bc.cmCache == this.cmCache;
	}

	/**
	 * Determines whether the specified class is ignored as a bean class based on the various exclusion parameters
	 * specified on this context class.
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
		ClassInfo ci = ClassInfo.of(c);
		for (Class exclude : notBeanClasses)
			if (ci.isChildOf(exclude))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is a bean.
	 *
	 * @param o The object to test.
	 * @return <jk>true</jk> if the specified object is a bean.  <jk>false</jk> if the bean is <jk>null</jk>.
	 */
	public boolean isBean(Object o) {
		if (o == null)
			return false;
		return getClassMetaForObject(o).isBean();
	}

	/**
	 * Prints meta cache statistics to <c>System.out</c>.
	 */
	protected static void dumpCacheStats() {
		try {
			int ctCount = 0;
			for (Map<Class,ClassMeta> cm : cmCacheCache.values())
				ctCount += cm.size();
			System.out.println(format("ClassMeta cache: {0} instances in {1} caches", ctCount, cmCacheCache.size())); // NOT DEBUG
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the {@link BeanMeta} class for the specified class.
	 *
	 * @param <T> The class type to get the meta-data on.
	 * @param c The class to get the meta-data on.
	 * @return
	 * 	The {@link BeanMeta} for the specified class, or <jk>null</jk> if the class is not a bean per the settings on
	 * 	this context.
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
	 * @return
	 * 	If the class is not an array, returns a cached {@link ClassMeta} object.
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public final <T> ClassMeta<T> getClassMeta(Class<T> type) {
		return getClassMeta(type, true);
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Class} object.
	 *
	 * @param <T> The class type being wrapped.
	 * @param type The class to resolve.
	 * @param waitForInit
	 * 	When enabled, wait for the ClassMeta constructor to finish before returning.
	 * @return
	 * 	If the class is not an array, returns a cached {@link ClassMeta} object.
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	final <T> ClassMeta<T> getClassMeta(Class<T> type, boolean waitForInit) {

		// This can happen if we have transforms defined against String or Object.
		if (cmCache == null)
			return null;

		ClassMeta<T> cm = cmCache.get(type);
		if (cm == null) {

			synchronized (this) {
				// Make sure someone didn't already set it while this thread was blocked.
				cm = cmCache.get(type);
				if (cm == null)
					cm = new ClassMeta<>(type, this, findPojoSwaps(type), findChildPojoSwaps(type));
			}
		}
		if (waitForInit)
			cm.waitForInit();
		return cm;
	}

	/**
	 * Used to resolve <c>ClassMetas</c> of type <c>Collection</c> and <c>Map</c> that have
	 * <c>ClassMeta</c> values that themselves could be collections or maps.
	 *
	 * <p>
	 * <c>Collection</c> meta objects are assumed to be followed by zero or one meta objects indicating the element type.
	 *
	 * <p>
	 * <c>Map</c> meta objects are assumed to be followed by zero or two meta objects indicating the key and value types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><code>getClassMeta(String.<jk>class</jk>);</code> - A normal type.
	 * 	<li><code>getClassMeta(List.<jk>class</jk>);</code> - A list containing objects.
	 * 	<li><code>getClassMeta(List.<jk>class</jk>, String.<jk>class</jk>);</code> - A list containing strings.
	 * 	<li><code>getClassMeta(LinkedList.<jk>class</jk>, String.<jk>class</jk>);</code> - A linked-list containing
	 * 		strings.
	 * 	<li><code>getClassMeta(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);</code> -
	 * 		A linked-list containing linked-lists of strings.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>);</code> - A map containing object keys/values.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);</code> - A map
	 * 		containing string keys/values.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);</code> -
	 * 		A map containing string keys and values of lists containing beans.
	 * </ul>
	 *
	 * @param type
	 * 	The class to resolve.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
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
		if (cm.isCollection() || cm.isOptional()) {
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

		if (o instanceof ClassMeta) {
			ClassMeta<?> cm = (ClassMeta)o;

			// This classmeta could have been created by a different context.
			// Need to re-resolve it to pick up PojoSwaps and stuff on this context.
			if (cm.getBeanContext() == this)
				return cm;
			if (cm.isMap())
				return getClassMeta(cm.innerClass, cm.getKeyType(), cm.getValueType());
			if (cm.isCollection() || cm.isOptional())
				return getClassMeta(cm.innerClass, cm.getElementType());
			return getClassMeta(cm.innerClass);
		}

		Class c = resolve(o, typeVarImpls);

		// This can happen when trying to resolve the "E getFirst()" method on LinkedList, whose type is a TypeVariable
		// These should just resolve to Object.
		if (c == null)
			return object();

		ClassMeta rawType = getClassMeta(c);

		// If this is a Map or Collection, and the parameter types aren't part
		// of the class definition itself (e.g. class AddressBook extends List<Person>),
		// then we need to figure out the parameters.
		if (rawType.isMap() || rawType.isCollection() || rawType.isOptional()) {
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
			if (rawType.isCollection() || rawType.isOptional()) {
				if (params.length != 1)
					return rawType;
				if (params[0].isObject())
					return rawType;
				return new ClassMeta(rawType, null, null, params[0]);
			}
		}

		if (rawType.isArray()) {
			if (o instanceof GenericArrayType) {
				GenericArrayType gat = (GenericArrayType)o;
				ClassMeta elementType = resolveClassMeta(gat.getGenericComponentType(), typeVarImpls);
				return new ClassMeta(rawType, null, null, elementType);
			}
		}

		return rawType;
	}

	/**
	 * Convert a Type to a Class if possible.
	 * Return null if not possible.
	 */
	final Class resolve(Type t, Map<Class<?>,Class<?>[]> typeVarImpls) {

		if (t instanceof Class)
			return (Class)t;

		if (t instanceof ParameterizedType)
			// A parameter (e.g. <String>.
			return (Class)((ParameterizedType)t).getRawType();

		if (t instanceof GenericArrayType) {
			// An array parameter (e.g. <byte[]>).
			Type gatct = ((GenericArrayType)t).getGenericComponentType();

			if (gatct instanceof Class)
				return Array.newInstance((Class)gatct, 0).getClass();

			if (gatct instanceof ParameterizedType)
				return Array.newInstance((Class)((ParameterizedType)gatct).getRawType(), 0).getClass();

			if (gatct instanceof GenericArrayType)
				return Array.newInstance(resolve(gatct, typeVarImpls), 0).getClass();

			return null;

		} else if (t instanceof TypeVariable) {
			if (typeVarImpls != null) {
				TypeVariable tv = (TypeVariable)t;
				String varName = tv.getName();
				int varIndex = -1;
				Class gc = (Class)tv.getGenericDeclaration();
				TypeVariable[] tvv = gc.getTypeParameters();
				for (int i = 0; i < tvv.length; i++) {
					if (tvv[i].getName().equals(varName)) {
						varIndex = i;
					}
				}
				if (varIndex != -1) {

					// If we couldn't find a type variable implementation, that means
					// the type was defined at runtime (e.g. Bean b = new Bean<Foo>();)
					// in which case the type is lost through erasure.
					// Assume java.lang.Object as the type.
					if (! typeVarImpls.containsKey(gc))
						return null;

					return typeVarImpls.get(gc)[varIndex];
				}
			}
		}
		return null;
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
				List<ClassMeta<?>> l = new LinkedList<>();
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
	 * Used for determining the class type on a method or field where a {@code @Beanp} annotation may be present.
	 *
	 * @param <T> The class type we're wrapping.
	 * @param p The property annotation on the type if there is one.
	 * @param t The type.
	 * @param typeVarImpls
	 * 	Contains known resolved type parameters on the specified class so that we can result
	 * 	{@code ParameterizedTypes} and {@code TypeVariables}.
	 * 	Can be <jk>null</jk> if the information is not known.
	 * @return The new {@code ClassMeta} object wrapped around the {@code Type} object.
	 */
	protected final <T> ClassMeta<T> resolveClassMeta(Beanp p, Type t, Map<Class<?>,Class<?>[]> typeVarImpls) {
		ClassMeta<T> cm = resolveClassMeta(t, typeVarImpls);
		ClassMeta<T> cm2 = cm;

		if (p != null) {

			if (p.type() != Null.class)
				cm2 = resolveClassMeta(p.type(), typeVarImpls);

			if (cm2.isMap()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class, Object.class} : p.params());
				if (pParams.length != 2)
					throw runtimeException("Invalid number of parameters specified for Map (must be 2): {0}", pParams.length);
				ClassMeta<?> keyType = resolveType(pParams[0], cm2.getKeyType(), cm.getKeyType());
				ClassMeta<?> valueType = resolveType(pParams[1], cm2.getValueType(), cm.getValueType());
				if (keyType.isObject() && valueType.isObject())
					return cm2;
				return new ClassMeta<>(cm2, keyType, valueType, null);
			}

			if (cm2.isCollection() || cm2.isOptional()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class} : p.params());
				if (pParams.length != 1)
					throw runtimeException("Invalid number of parameters specified for {1} (must be 1): {0}", pParams.length, (cm2.isCollection() ? "Collection" : cm2.isOptional() ? "Optional" : "Array"));
				ClassMeta<?> elementType = resolveType(pParams[0], cm2.getElementType(), cm.getElementType());
				if (elementType.isObject())
					return cm2;
				return new ClassMeta<>(cm2, null, null, elementType);
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
	 * Returns the {@link PojoSwap} associated with the specified class, or <jk>null</jk> if there is no POJO swap
	 * associated with the class.
	 *
	 * @param <T> The class associated with the swap.
	 * @param c The class associated with the swap.
	 * @return The swap associated with the class, or null if there is no association.
	 */
	private final <T> PojoSwap[] findPojoSwaps(Class<T> c) {
		// Note:  On first
		if (c != null) {
			List<PojoSwap> l = new ArrayList<>();
			for (PojoSwap f : swaps)
				if (f.getNormalClass().isParentOf(c))
					l.add(f);
			return l.size() == 0 ? null : l.toArray(new PojoSwap[l.size()]);
		}
		return null;
	}

	/**
	 * Checks whether a class has a {@link PojoSwap} associated with it in this bean context.
	 *
	 * @param c The class to check.
	 * @return <jk>true</jk> if the specified class or one of its subclasses has a {@link PojoSwap} associated with it.
	 */
	private final PojoSwap[] findChildPojoSwaps(Class<?> c) {
		if (c == null || swaps.length == 0)
			return null;
		List<PojoSwap> l = null;
		for (PojoSwap f : swaps) {
			if (f.getNormalClass().isChildOf(c)) {
				if (l == null)
					l = new ArrayList<>();
				l.add(f);
			}
		}
		return l == null ? null : l.toArray(new PojoSwap[l.size()]);
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <c>Object</c>.
	 *
	 * <p>
	 * This <c>ClassMeta</c> is often used to represent "any object type" when an object type is not known.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Object.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <c>Object</c> class.
	 */
	protected final ClassMeta<Object> object() {
		return cmObject;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <c>String</c>.
	 *
	 * <p>
	 * This <c>ClassMeta</c> is often used to represent key types in maps.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(String.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <c>String</c> class.
	 */
	protected final ClassMeta<String> string() {
		return cmString;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <c>Class</c>.
	 *
	 * <p>
	 * This <c>ClassMeta</c> is often used to represent key types in maps.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Class.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <c>String</c> class.
	 */
	protected final ClassMeta<Class> _class() {
		return cmClass;
	}

	/**
	 * Returns the lookup table for resolving bean types by name.
	 *
	 * @return The lookup table for resolving bean types by name.
	 */
	protected final BeanRegistry getBeanRegistry() {
		return beanRegistry;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MetaProvider methods
	//-----------------------------------------------------------------------------------------------------------------

	private static final boolean DISABLE_ANNOTATION_CACHING = ! Boolean.getBoolean("juneau.disableAnnotationCaching");

	private TwoKeyConcurrentCache<Class<?>,Class<? extends Annotation>,List<Annotation>> classAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Class<?>,Class<? extends Annotation>,List<Annotation>> declaredClassAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Method,Class<? extends Annotation>,List<Annotation>> methodAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Field,Class<? extends Annotation>,List<Annotation>> fieldAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Constructor<?>,Class<? extends Annotation>,List<Annotation>> constructorAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);

	/**
	 * Finds the specified annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Class<?> c) {
		if (a == null || c == null)
			return emptyList();
		List<Annotation> aa = classAnnotationCache.get(c, a);
		if (aa == null) {
			A[] x = c.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotations.appendAll(c, a, l);
			aa = l.unmodifiable();
			classAnnotationCache.put(c, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified declared annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getDeclaredAnnotations(Class<A> a, Class<?> c) {
		if (a == null || c == null)
			return emptyList();
		List<Annotation> aa = declaredClassAnnotationCache.get(c, a);
		if (aa == null) {
			A[] x = c.getDeclaredAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotations.appendAll(c, a, l);
			aa = l.unmodifiable();
			declaredClassAnnotationCache.put(c, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Method m) {
		if (a == null || m == null)
			return emptyList();
		List<Annotation> aa = methodAnnotationCache.get(m, a);
		if (aa == null) {
			A[] x = m.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotations.appendAll(m, a, l);
			aa = l.unmodifiable();
			methodAnnotationCache.put(m, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, MethodInfo m) {
		return getAnnotations(a, m == null ? null : m.inner());
	}

	/**
	 * Finds the last specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, Method m) {
		return last(getAnnotations(a, m));
	}

	/**
	 * Finds the last specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, MethodInfo m) {
		return last(getAnnotations(a, m));
	}

	/**
	 * Finds the specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Field f) {
		if (a == null || f == null)
			return emptyList();
		List<Annotation> aa = fieldAnnotationCache.get(f, a);
		if (aa == null) {
			A[] x = f.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotations.appendAll(f, a, l);
			aa = l.unmodifiable();
			fieldAnnotationCache.put(f, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, FieldInfo f) {
		return getAnnotations(a, f == null ? null: f.inner());
	}

	/**
	 * Finds the specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Constructor<?> c) {
		if (a == null || c == null)
			return emptyList();
		List<Annotation> aa = constructorAnnotationCache.get(c, a);
		if (aa == null) {
			A[] x = c.getAnnotationsByType(a);
			AList<Annotation> l = new AList(Arrays.asList(x));
			annotations.appendAll(c, a, l);
			aa = l.unmodifiable();
			constructorAnnotationCache.put(c, a, l);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, ConstructorInfo c) {
		return getAnnotations(a, c == null ? null : c.inner());
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The class being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified class.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, Class<?> c) {
		return getAnnotations(a, c).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The class being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified class.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, ClassInfo c) {
		return getAnnotations(a, c == null ? null : c.inner()).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,m)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param m The method being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified method.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, Method m) {
		return getAnnotations(a, m).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,m)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param m The method being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified method.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, MethodInfo m) {
		return getAnnotations(a, m == null ? null : m.inner()).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,f)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param f The field being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified field.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, FieldInfo f) {
		return getAnnotations(a, f == null ? null : f.inner()).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The constructor being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified constructor.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, ConstructorInfo c) {
		return getAnnotations(a, c == null ? null : c.inner()).size() > 0;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Minimum bean class visibility.
	 *
	 * @see #BEAN_beanClassVisibility
	 * @return
	 * 	Classes are not considered beans unless they meet the minimum visibility requirements.
	 */
	public final Visibility getBeanClassVisibility() {
		return beanClassVisibility;
	}

	/**
	 * Minimum bean constructor visibility.
	 *
	 * @see #BEAN_beanConstructorVisibility
	 * @return
	 * 	Only look for constructors with this specified minimum visibility.
	 */
	public final Visibility getBeanConstructorVisibility() {
		return beanConstructorVisibility;
	}

	/**
	 * Bean dictionary.
	 *
	 * @see #BEAN_beanDictionary
	 * @return
	 * 	The list of classes that make up the bean dictionary in this bean context.
	 */
	public final List<Class<?>> getBeanDictionaryClasses() {
		return beanDictionaryClasses;
	}

	/**
	 * Minimum bean field visibility.
	 *
	 *
	 * @see #BEAN_beanFieldVisibility
	 * @return
	 * 	Only look for bean fields with this specified minimum visibility.
	 */
	public final Visibility getBeanFieldVisibility() {
		return beanFieldVisibility;
	}

	/**
	 * BeanMap.put() returns old property value.
	 *
	 * @see #BEAN_beanMapPutReturnsOldValue
	 * @return
	 * 	<jk>true</jk> if the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property values.
	 * 	<br>Otherwise, it returns <jk>null</jk>.
	 */
	public final boolean isBeanMapPutReturnsOldValue() {
		return beanMapPutReturnsOldValue;
	}

	/**
	 * Minimum bean method visibility.
	 *
	 * @see #BEAN_beanMethodVisibility
	 * @return
	 * 	Only look for bean methods with this specified minimum visibility.
	 */
	public final Visibility getBeanMethodVisibility() {
		return beanMethodVisibility;
	}

	/**
	 * Beans require no-arg constructors.
	 *
	 * @see #BEAN_beansRequireDefaultConstructor
	 * @return
	 * 	<jk>true</jk> if a Java class must implement a default no-arg constructor to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireDefaultConstructor() {
		return beansRequireDefaultConstructor;
	}

	/**
	 * Beans require Serializable interface.
	 *
	 * @see #BEAN_beansRequireSerializable
	 * @return
	 * 	<jk>true</jk> if a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireSerializable() {
		return beansRequireSerializable;
	}

	/**
	 * Beans require setters for getters.
	 *
	 * @see #BEAN_beansRequireSettersForGetters
	 * @return
	 * 	<jk>true</jk> if only getters that have equivalent setters will be considered as properties on a bean.
	 * 	<br>Otherwise, they are ignored.
	 */
	public final boolean isBeansRequireSettersForGetters() {
		return beansRequireSettersForGetters;
	}

	/**
	 * Beans require at least one property.
	 *
	 * @see #BEAN_disableBeansRequireSomeProperties
	 * @return
	 * 	<jk>true</jk> if a Java class doesn't need to contain at least 1 property to be considered a bean.
	 * 	<br>Otherwise, the bean is serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireSomeProperties() {
		return beansRequireSomeProperties;
	}

	/**
	 * Bean type property name.
	 *
	 * @see #BEAN_typePropertyName
	 * @return
	 * The name of the bean property used to store the dictionary name of a bean type so that the parser knows the data type to reconstruct.
	 */
	public final String getBeanTypePropertyName() {
		return typePropertyName;
	}

	/**
	 * Find fluent setters.
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 *
	 * @see #BEAN_findFluentSetters
	 * @return
	 * 	<jk>true</jk> if fluent setters are detected on beans.
	 */
	public final boolean isFindFluentSetters() {
		return findFluentSetters;
	}

	/**
	 * Ignore invocation errors on getters.
	 *
	 * @see #BEAN_ignoreInvocationExceptionsOnGetters
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean getter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnGetters() {
		return ignoreInvocationExceptionsOnGetters;
	}

	/**
	 * Ignore invocation errors on setters.
	 *
	 * @see #BEAN_ignoreInvocationExceptionsOnSetters
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean setter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnSetters() {
		return ignoreInvocationExceptionsOnSetters;
	}

	/**
	 * Silently ignore missing setters.
	 *
	 * @see #BEAN_disableIgnoreMissingSetters
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a bean property without a setter should throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreMissingSetters() {
		return ignoreMissingSetters;
	}

	/**
	 * Iignore transient fields.
	 *
	 * @see #BEAN_disableIgnoreTransientFields
	 * @return
	 * 	<jk>true</jk> if fields and methods marked as transient should not be ignored.
	 */
	protected final boolean isIgnoreTransientFields() {
		return ignoreTransientFields;
	}

	/**
	 * Ignore unknown properties.
	 *
	 * @see #BEAN_ignoreUnknownBeanProperties
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a non-existent bean property is silently ignored.
	 * 	<br>Otherwise, a {@code RuntimeException} is thrown.
	 */
	public final boolean isIgnoreUnknownBeanProperties() {
		return ignoreUnknownBeanProperties;
	}

	/**
	 * Ignore unknown properties with null values.
	 *
	 * @see #BEAN_disableIgnoreUnknownNullBeanProperties
	 * @return
	 * 	<jk>true</jk> if trying to set a <jk>null</jk> value on a non-existent bean property should throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreUnknownNullBeanProperties() {
		return ignoreUnknownNullBeanProperties;
	}

	/**
	 * Bean class exclusions.
	 *
	 * @see #BEAN_notBeanClasses
	 * @return
	 * 	The list of classes that are explicitly not beans.
	 */
	protected final Class<?>[] getNotBeanClasses() {
		return notBeanClasses;
	}

	/**
	 * Bean package exclusions.
	 *
	 * @see #BEAN_notBeanPackages
	 * @return
	 * 	The list of fully-qualified package names to exclude from being classified as beans.
	 */
	public final String[] getNotBeanPackagesNames() {
		return notBeanPackageNames;
	}

	/**
	 * Bean package exclusions.
	 *
	 * @see #BEAN_notBeanPackages
	 * @return
	 * 	The list of package name prefixes to exclude from being classified as beans.
	 */
	protected final String[] getNotBeanPackagesPrefixes() {
		return notBeanPackagePrefixes;
	}

	/**
	 * Java object swaps.
	 *
	 * @see #BEAN_swaps
	 * @return
	 * 	The list POJO swaps defined.
	 */
	public final PojoSwap<?,?>[] getSwaps() {
		return swaps;
	}

	/**
	 * Bean property namer.
	 *
	 * @see #BEAN_propertyNamer
	 * @return
	 * 	The interface used to calculate bean property names.
	 */
	public final PropertyNamer getPropertyNamer() {
		return propertyNamer;
	}

	/**
	 * Sort bean properties.
	 *
	 * @see #BEAN_sortProperties
	 * @return
	 * 	<jk>true</jk> if all bean properties will be serialized and access in alphabetical order.
	 */
	public final boolean isSortProperties() {
		return sortProperties;
	}

	/**
	 * Use enum names.
	 *
	 * @see #BEAN_useEnumNames
	 * @return
	 * 	<jk>true</jk> if enums are always serialized by name, not using {@link Object#toString()}.
	 */
	public final boolean isUseEnumNames() {
		return useEnumNames;
	}

	/**
	 * Use interface proxies.
	 *
	 * @see #BEAN_disableInterfaceProxies
	 * @return
	 * 	<jk>true</jk> if interfaces will be instantiated as proxy classes through the use of an
	 * 	{@link InvocationHandler} if there is no other way of instantiating them.
	 */
	public final boolean isUseInterfaceProxies() {
		return useInterfaceProxies;
	}

	/**
	 * Use Java Introspector.
	 *
	 * @see #BEAN_useJavaBeanIntrospector
	 * @return
	 * 	<jk>true</jk> if the built-in Java bean introspector should be used for bean introspection.
	 */
	public final boolean isUseJavaBeanIntrospector() {
		return useJavaBeanIntrospector;
	}

	/**
	 * Locale.
	 *
	 * @see #BEAN_locale
	 * @return
	 * 	The default locale for serializer and parser sessions.
	 */
	public final Locale getDefaultLocale() {
		return locale;
	}

	/**
	 * Media type.
	 *
	 * @see #BEAN_mediaType
	 * @return
	 * 	The default media type value for serializer and parser sessions.
	 */
	public final MediaType getDefaultMediaType() {
		return mediaType;
	}

	/**
	 * Time zone.
	 *
	 * @see #BEAN_timeZone
	 * @return
	 * 	The default timezone for serializer and parser sessions.
	 */
	public final TimeZone getDefaultTimeZone() {
		return timeZone;
	}

	/**
	 * Returns the serializer to use for serializing beans when using the {@link BeanSession#convertToType(Object, Class)}
	 * and related methods.
	 *
	 * @return The serializer.  May be <jk>null</jk> if all initialization has occurred.
	 */
	protected WriterSerializer getBeanToStringSerializer() {
		if (beanToStringSerializer == null) {
			if (JsonSerializer.DEFAULT == null)
				return null;
			this.beanToStringSerializer = JsonSerializer.create().apply(getContextProperties()).sq().simpleMode().build();
		}
		return beanToStringSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"BeanContext",
				OMap
					.create()
					.filtered()
					.a("id", System.identityHashCode(this))
					.a("beanClassVisibility", beanClassVisibility)
					.a("beanConstructorVisibility", beanConstructorVisibility)
					.a("beanDictionaryClasses", beanDictionaryClasses)
					.a("beanFieldVisibility", beanFieldVisibility)
					.a("beanMethodVisibility", beanMethodVisibility)
					.a("beansRequireDefaultConstructor", beansRequireDefaultConstructor)
					.a("beansRequireSerializable", beansRequireSerializable)
					.a("beansRequireSettersForGetters", beansRequireSettersForGetters)
					.a("beansRequireSomeProperties", beansRequireSomeProperties)
					.a("ignoreTransientFields", ignoreTransientFields)
					.a("ignoreMissingSetters", ignoreMissingSetters)
					.a("ignoreInvocationExceptionsOnGetters", ignoreInvocationExceptionsOnGetters)
					.a("ignoreInvocationExceptionsOnSetters", ignoreInvocationExceptionsOnSetters)
					.a("ignoreUnknownBeanProperties", ignoreUnknownBeanProperties)
					.a("ignoreUnknownNullBeanProperties", ignoreUnknownNullBeanProperties)
					.a("notBeanClasses", notBeanClasses)
					.a("notBeanPackageNames", notBeanPackageNames)
					.a("notBeanPackagePrefixes", notBeanPackagePrefixes)
					.a("pojoSwaps", swaps)
					.a("sortProperties", sortProperties)
					.a("useEnumNames", useEnumNames)
					.a("useInterfaceProxies", useInterfaceProxies)
					.a("useJavaBeanIntrospector", useJavaBeanIntrospector)
			);
	}
}