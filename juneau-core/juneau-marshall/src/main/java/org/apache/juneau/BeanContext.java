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
import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.beans.*;
import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.utils.*;

/**
 * Bean context.
 *
 * <p>
 * This class servers multiple purposes:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Provides the ability to wrap beans inside {@link Map} interfaces.
 * 	<li>
 * 		Serves as a repository for metadata on POJOs, such as associated {@link Bean @Bean} annotations,
 * 		{@link PropertyNamer PropertyNamers}, etc...  which are used to tailor how POJOs are serialized and parsed.
 * </ul>
 *
 * <p>
 * All serializers and parsers use this context so that they can handle POJOs using a common framework.
 *
 * <h5 class='topic'>Bean Contexts</h5>
 *
 * <p>
 * Bean contexts are created through the {@link BeanContext#create() BeanContext.create()} and {@link Builder#build()} methods.
 * <br>These context objects are read-only, reusable, and thread-safe.
 *
 * <p>
 * Each bean context maintains a cache of {@link ClassMeta} objects that describe information about classes encountered.
 * These <c>ClassMeta</c> objects are time-consuming to construct.
 * Therefore, instances of {@link BeanContext} that share the same <js>"BeanContext.*"</js> property values share
 * the same cache.  This allows for efficient reuse of <c>ClassMeta</c> objects so that the information about
 * classes only needs to be calculated once.
 * Because of this, many of the properties defined on the {@link BeanContext} class cannot be overridden on the session.
 *
 * <h5 class='topic'>Bean Sessions</h5>
 *
 * <p>
 * Whereas <c>BeanContext</c> objects are permanent, unchangeable, cached, and thread-safe,
 * {@link BeanSession} objects are ephemeral and not thread-safe.
 * They are meant to be used as quickly-constructed scratchpads for creating bean maps.
 * {@link BeanMap} objects can only be created through the session.
 *
 * <h5 class='topic'>BeanContext configuration properties</h5>
 *
 * <p>
 * <c>BeanContexts</c> have several configuration properties that can be used to tweak behavior on how beans are
 * handled.  These are denoted as the static <jsf>BEAN_*</jsf> fields on this class.
 *
 * <p>
 * Some settings (e.g. {@link Builder#beansRequireDefaultConstructor()}) are used to differentiate between bean
 * and non-bean classes.
 * Attempting to create a bean map around one of these objects will throw a {@link BeanRuntimeException}.
 * The purpose for this behavior is so that the serializers can identify these non-bean classes and convert them to
 * plain strings using the {@link Object#toString()} method.
 *
 * <p>
 * Some settings (e.g. {@link Builder#beanFieldVisibility(Visibility)}) are used to determine what kinds of properties are
 * detected on beans.
 *
 * <p>
 * Some settings (e.g. {@link Builder#beanMapPutReturnsOldValue()}) change the runtime behavior of bean maps.
 *
 * <h5 class='section'>Example:</h5>
 *
 * <p class='bjava'>
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
 * <p>
 * {@link BeanMap BeanMaps} are wrappers around Java beans that allow properties to be retrieved and
 * set using the common {@link Map#put(Object,Object)} and {@link Map#get(Object)} methods.
 *
 * <p>
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
 * <p class='bjava'>
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
 * 	BeanMap&lt;Person&gt; <jv>map1</jv> = <jv>session</jv>.toBeanMap(<jk>new</jk> Person());
 * 	<jv>map1</jv>.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	<jv>map1</jv>.put(<js>"age"</js>, 45);
 *
 * 	<jc>// Create a new bean instance wrapped in a new bean map</jc>
 * 	BeanMap&lt;Person&gt; <jv>map2</jv> = <jv>session</jv>.newBeanMap(Person.<jk>class</jk>);
 * 	<jv>map2</jv>.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	<jv>map2</jv>.put(<js>"age"</js>, 45);
 * 	Person <jv>person</jv> = <jv>map2</jv>.getBean();  <jc>// Get the bean instance that was created.</jc>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../index.html#jm.BeanContexts">Bean Contexts</a>
 * </ul>
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BeanContext extends Context {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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


	/** Default config.  All default settings. */
	public static final BeanContext DEFAULT = create().build();

	/** Default config.  All default settings except sort bean properties. */
	public static final BeanContext DEFAULT_SORTED = create().sortProperties().build();

	/** Default reusable unmodifiable session.  Can be used to avoid overhead of creating a session (for creating BeanMaps for example).*/
	public  static final BeanSession DEFAULT_SESSION = DEFAULT.createSession().unmodifiable().build();

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends Context.Builder {

		private static final Cache<HashKey,BeanContext> CACHE = Cache.of(HashKey.class, BeanContext.class).build();

		Visibility beanClassVisibility, beanConstructorVisibility, beanMethodVisibility, beanFieldVisibility;
		boolean disableBeansRequireSomeProperties, beanMapPutReturnsOldValue, beansRequireDefaultConstructor, beansRequireSerializable,
			beansRequireSettersForGetters, disableIgnoreTransientFields, disableIgnoreUnknownNullBeanProperties, disableIgnoreMissingSetters,
			disableInterfaceProxies, findFluentSetters, ignoreInvocationExceptionsOnGetters, ignoreInvocationExceptionsOnSetters,
			ignoreUnknownBeanProperties, ignoreUnknownEnumValues, sortProperties, useEnumNames, useJavaBeanIntrospector;
		String typePropertyName;
		MediaType mediaType;
		Locale locale;
		TimeZone timeZone;
		Class<? extends PropertyNamer> propertyNamer;
		List<Class<?>> beanDictionary;
		List<Object> swaps;
		Set<Class<?>> notBeanClasses;
		Set<String> notBeanPackages;

		/**
		 * Constructor.
		 *
		 * All default settings.
		 */
		protected Builder() {
			super();
			beanClassVisibility = env("BeanContext.beanClassVisibility", PUBLIC);
			beanConstructorVisibility = env("BeanContext.beanConstructorVisibility", PUBLIC);
			beanMethodVisibility = env("BeanContext.beanMethodVisibility", PUBLIC);
			beanFieldVisibility = env("BeanContext.beanFieldVisibility", PUBLIC);
			beanDictionary = null;
			swaps = null;
			notBeanClasses = null;
			notBeanPackages = null;
			disableBeansRequireSomeProperties = env("BeanContext.disableBeansRequireSomeProperties", false);
			beanMapPutReturnsOldValue = env("BeanContext.beanMapPutReturnsOldValue", false);
			beansRequireDefaultConstructor = env("BeanContext.beansRequireDefaultConstructor", false);
			beansRequireSerializable = env("BeanContext.beansRequireSerializable", false);
			beansRequireSettersForGetters = env("BeanContext.beansRequireSettersForGetters", false);
			disableIgnoreTransientFields = env("BeanContext.disableIgnoreTransientFields", false);
			disableIgnoreUnknownNullBeanProperties = env("BeanContext.disableIgnoreUnknownNullBeanProperties", false);
			disableIgnoreMissingSetters = env("BeanContext.disableIgnoreMissingSetters", false);
			disableInterfaceProxies = env("BeanContext.disableInterfaceProxies", false);
			findFluentSetters = env("BeanContext.findFluentSetters", false);
			ignoreInvocationExceptionsOnGetters = env("BeanContext.ignoreInvocationExceptionsOnGetters", false);
			ignoreInvocationExceptionsOnSetters = env("BeanContext.ignoreInvocationExceptionsOnSetters", false);
			ignoreUnknownBeanProperties = env("BeanContext.ignoreUnknownBeanProperties", false);
			ignoreUnknownEnumValues = env("BeanContext.ignoreUnknownEnumValues", false);
			sortProperties = env("BeanContext.sortProperties", false);
			useEnumNames = env("BeanContext.useEnumNames", false);
			useJavaBeanIntrospector = env("BeanContext.useJavaBeanIntrospector", false);
			typePropertyName = env("BeanContext.typePropertyName", "_type");
			mediaType = env("BeanContext.mediaType", (MediaType)null);
			timeZone = env("BeanContext.timeZone", (TimeZone)null);
			locale = env("BeanContext.locale", Locale.getDefault());
			propertyNamer = null;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(BeanContext copyFrom) {
			super(copyFrom);
			beanClassVisibility = copyFrom.beanClassVisibility;
			beanConstructorVisibility = copyFrom.beanConstructorVisibility;
			beanMethodVisibility = copyFrom.beanMethodVisibility;
			beanFieldVisibility = copyFrom.beanFieldVisibility;
			beanDictionary = listFrom(copyFrom.beanDictionary, true);
			swaps = listFrom(copyFrom.swaps, true);
			notBeanClasses = classSet(copyFrom.notBeanClasses, true);
			notBeanPackages = sortedSetFrom(copyFrom.notBeanPackages, true);
			disableBeansRequireSomeProperties = ! copyFrom.beansRequireSomeProperties;
			beanMapPutReturnsOldValue = copyFrom.beanMapPutReturnsOldValue;
			beansRequireDefaultConstructor = copyFrom.beansRequireDefaultConstructor;
			beansRequireSerializable = copyFrom.beansRequireSerializable;
			beansRequireSettersForGetters = copyFrom.beansRequireSettersForGetters;
			disableIgnoreTransientFields = ! copyFrom.ignoreTransientFields;
			disableIgnoreUnknownNullBeanProperties = ! copyFrom.ignoreUnknownNullBeanProperties;
			disableIgnoreMissingSetters = ! copyFrom.ignoreMissingSetters;
			disableInterfaceProxies = ! copyFrom.useInterfaceProxies;
			findFluentSetters = copyFrom.findFluentSetters;
			ignoreInvocationExceptionsOnGetters = copyFrom.ignoreInvocationExceptionsOnGetters;
			ignoreInvocationExceptionsOnSetters = copyFrom.ignoreInvocationExceptionsOnSetters;
			ignoreUnknownBeanProperties = copyFrom.ignoreUnknownBeanProperties;
			ignoreUnknownEnumValues = copyFrom.ignoreUnknownEnumValues;
			sortProperties = copyFrom.sortProperties;
			useEnumNames = copyFrom.useEnumNames;
			useJavaBeanIntrospector = copyFrom.useJavaBeanIntrospector;
			typePropertyName = copyFrom.typePropertyName;
			mediaType = copyFrom.mediaType;
			timeZone = copyFrom.timeZone;
			locale = copyFrom.locale;
			propertyNamer = copyFrom.propertyNamer;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			beanClassVisibility = copyFrom.beanClassVisibility;
			beanConstructorVisibility = copyFrom.beanConstructorVisibility;
			beanMethodVisibility = copyFrom.beanMethodVisibility;
			beanFieldVisibility = copyFrom.beanFieldVisibility;
			beanDictionary = copyOf(copyFrom.beanDictionary);
			swaps = copyOf(copyFrom.swaps);
			notBeanClasses = classSet(copyFrom.notBeanClasses);
			notBeanPackages = sortedSetFrom(copyFrom.notBeanPackages);
			disableBeansRequireSomeProperties = copyFrom.disableBeansRequireSomeProperties;
			beanMapPutReturnsOldValue = copyFrom.beanMapPutReturnsOldValue;
			beansRequireDefaultConstructor = copyFrom.beansRequireDefaultConstructor;
			beansRequireSerializable = copyFrom.beansRequireSerializable;
			beansRequireSettersForGetters = copyFrom.beansRequireSettersForGetters;
			disableIgnoreTransientFields = copyFrom.disableIgnoreTransientFields;
			disableIgnoreUnknownNullBeanProperties = copyFrom.disableIgnoreUnknownNullBeanProperties;
			disableIgnoreMissingSetters = copyFrom.disableIgnoreMissingSetters;
			disableInterfaceProxies = copyFrom.disableInterfaceProxies;
			findFluentSetters = copyFrom.findFluentSetters;
			ignoreInvocationExceptionsOnGetters = copyFrom.ignoreInvocationExceptionsOnGetters;
			ignoreInvocationExceptionsOnSetters = copyFrom.ignoreInvocationExceptionsOnSetters;
			ignoreUnknownBeanProperties = copyFrom.ignoreUnknownBeanProperties;
			ignoreUnknownEnumValues = copyFrom.ignoreUnknownEnumValues;
			sortProperties = copyFrom.sortProperties;
			useEnumNames = copyFrom.useEnumNames;
			useJavaBeanIntrospector = copyFrom.useJavaBeanIntrospector;
			typePropertyName = copyFrom.typePropertyName;
			mediaType = copyFrom.mediaType;
			timeZone = copyFrom.timeZone;
			locale = copyFrom.locale;
			propertyNamer = copyFrom.propertyNamer;
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public BeanContext build() {
			return cache(CACHE).build(BeanContext.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				beanClassVisibility,
				beanConstructorVisibility,
				beanMethodVisibility,
				beanFieldVisibility,
				beanDictionary,
				swaps,
				notBeanClasses,
				notBeanPackages,
				integer(
					disableBeansRequireSomeProperties,
					beanMapPutReturnsOldValue,
					beansRequireDefaultConstructor,
					beansRequireSerializable,
					beansRequireSettersForGetters,
					disableIgnoreTransientFields,
					disableIgnoreUnknownNullBeanProperties,
					disableIgnoreMissingSetters,
					disableInterfaceProxies,
					findFluentSetters,
					ignoreInvocationExceptionsOnGetters,
					ignoreInvocationExceptionsOnSetters,
					ignoreUnknownBeanProperties,
					ignoreUnknownEnumValues,
					sortProperties,
					useEnumNames,
					useJavaBeanIntrospector
				),
				typePropertyName,
				mediaType,
				timeZone,
				locale,
				propertyNamer
			);
		}

		private int integer(boolean...values) {
			int n = 0;
			for (boolean b : values)
				n = (n << 1) | (b ? 1 : 0);
			return n;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Minimum bean class visibility.
		 *
		 * <p>
		 * Classes are not considered beans unless they meet the minimum visibility requirements.
		 * For example, if the visibility is <jsf>PUBLIC</jsf> and the bean class is <jk>protected</jk>, then the class
		 * will not be interpreted as a bean class and be serialized as a string.
		 * Use this setting to reduce the visibility requirement.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a protected class and one field.</jc>
		 * 	<jk>protected class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that's capable of serializing the class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanClassVisibility(<jsf>PROTECTED</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo","bar"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Bean @Bean} annotation can be used on a non-public bean class to override this setting.
		 * 	<li class='note'>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a public bean class to ignore it as a bean.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanClassVisibility()}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link Visibility#PUBLIC}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanClassVisibility(Visibility value) {
			beanClassVisibility = value;
			return this;
		}

		/**
		 * Minimum bean constructor visibility.
		 *
		 * <p>
		 * Only look for constructors with the specified minimum visibility.
		 *
		 * <p>
		 * This setting affects the logic for finding no-arg constructors for bean.  Normally, only <jk>public</jk> no-arg
		 * constructors are used.  Use this setting if you want to reduce the visibility requirement.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a protected constructor and one field.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf>;
		 *
		 * 		<jk>protected</jk> MyBean() {}
		 * 	}
		 *
		 * 	<jc>// Create a parser capable of calling the protected constructor.</jc>
		 * 	ReaderParser <jv>parser</jv> = ReaderParser
		 * 		.<jsm>create</jsm>()
		 * 		.beanConstructorVisibility(<jsf>PROTECTED</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Use it.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'bar'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Beanc @Beanc} annotation can also be used to expose a non-public constructor.
		 * 	<li class='note'>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a public bean constructor to ignore it.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanConstructorVisibility()}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link Visibility#PUBLIC}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanConstructorVisibility(Visibility value) {
			beanConstructorVisibility = value;
			return this;
		}

		/**
		 * Minimum bean field visibility.
		 *
		 * <p>
		 * Only look for bean fields with the specified minimum visibility.
		 *
		 * <p>
		 * This affects which fields on a bean class are considered bean properties.  Normally only <jk>public</jk> fields are considered.
		 * Use this setting if you want to reduce the visibility requirement.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a protected field.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>protected</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that recognizes the protected field.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanFieldVisibility(<jsf>PROTECTED</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"bar"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <p>
		 * Bean fields can be ignored as properties entirely by setting the value to {@link Visibility#NONE}
		 *
		 * <p class='bjava'>
		 * 	<jc>// Disable using fields as properties entirely.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanFieldVisibility(<jsf>NONE</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Beanp @Beanp} annotation can also be used to expose a non-public field.
		 * 	<li class='note'>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a public bean field to ignore it as a bean property.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanFieldVisibility()}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link Visibility#PUBLIC}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanFieldVisibility(Visibility value) {
			beanFieldVisibility = value;
			return this;
		}

		/**
		 * Bean interceptor.
		 *
		 * <p>
		 * Bean interceptors can be used to intercept calls to getters and setters and alter their values in transit.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Interceptor that strips out sensitive information.</jc>
		 * 	<jk>public class</jk> AddressInterceptor <jk>extends</jk> BeanInterceptor&lt;Address&gt; {
		 *
		 * 		<jk>public</jk> Object readProperty(Address <jv>bean</jv>, String <jv>name</jv>, Object <jv>value</jv>) {
		 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(<jv>name</jv>))
		 * 				<jk>return</jk> <js>"redacted"</js>;
		 * 			<jk>return</jk> <jv>value</jv>;
		 * 		}
		 *
		 * 		<jk>public</jk> Object writeProperty(Address <jv>bean</jv>, String <jv>name</jv>, Object <jv>value</jv>) {
		 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(<jv>name</jv>) &amp;&amp; <js>"redacted"</js>.equals(<jv>value</jv>))
		 * 				<jk>return</jk> TaxInfoUtils.<jsm>lookup</jsm>(<jv>bean</jv>.getStreet(), <jv>bean</jv>.getCity(), <jv>bean</jv>.getState());
		 * 			<jk>return</jk> <jv>value</jv>;
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Our bean class.</jc>
		 * 	<jk>public class</jk> Address {
		 * 		<jk>public</jk> String getTaxInfo() {...}
		 * 		<jk>public void</jk> setTaxInfo(String <jv>value</jv>) {...}
		 * 	}
		 *
		 * 	<jc>// Register filter on serializer or parser.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanInterceptor(Address.<jk>class</jk>, AddressInterceptor.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"taxInfo":"redacted"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> Address());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jc'>{@link BeanInterceptor}
		 * 	<li class='ja'>{@link Bean#interceptor() Bean(interceptor)}
		 * </ul>
		 *
		 * @param on The bean that the filter applies to.
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanInterceptor(Class<?> on, Class<? extends BeanInterceptor<?>> value) {
			return annotations(BeanAnnotation.create(on).interceptor(value).build());
		}

		/**
		 * BeanMap.put() returns old property value.
		 *
		 * <p>
		 * When enabled, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
		 * values.  Otherwise, it returns <jk>null</jk>.
		 *
		 * <p>
		 * Disabled by default because it introduces a slight performance penalty during serialization.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a context that creates BeanMaps with normal put() behavior.</jc>
		 * 	BeanContext <jv>context</jv> = BeanContext
		 * 		.<jsm>create</jsm>()
		 * 		.beanMapPutReturnsOldValue()
		 * 		.build();
		 *
		 * 	BeanMap&lt;MyBean&gt; <jv>myBeanMap</jv> = <jv>context</jv>.createSession().toBeanMap(<jk>new</jk> MyBean());
		 * 	<jv>myBeanMap</jv>.put(<js>"foo"</js>, <js>"bar"</js>);
		 * 	Object <jv>oldValue</jv> = <jv>myBeanMap</jv>.put(<js>"foo"</js>, <js>"baz"</js>);  <jc>// oldValue == "bar"</jc>
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanMapPutReturnsOldValue()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanMapPutReturnsOldValue() {
			return beanMapPutReturnsOldValue(true);
		}

		/**
		 * Same as {@link #beanMapPutReturnsOldValue()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanMapPutReturnsOldValue(boolean value) {
			beanMapPutReturnsOldValue = value;
			return this;
		}

		/**
		 * Minimum bean method visibility.
		 *
		 * <p>
		 * Only look for bean methods with the specified minimum visibility.
		 *
		 * <p>
		 * This affects which methods are detected as getters and setters on a bean class. Normally only <jk>public</jk> getters and setters are considered.
		 * Use this setting if you want to reduce the visibility requirement.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a protected getter.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String getFoo() { <jk>return</jk> <js>"foo"</js>; }
		 * 		<jk>protected</jk> String getBar() { <jk>return</jk> <js>"bar"</js>; }
		 * 	}
		 *
		 * 	<jc>// Create a serializer that looks for protected getters and setters.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanMethodVisibility(<jsf>PROTECTED</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Beanp @Beanp} annotation can also be used to expose a non-public method.
		 * 	<li class='note'>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a public bean getter/setter to ignore it as a bean property.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanMethodVisibility()}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link Visibility#PUBLIC}
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanMethodVisibility(Visibility value) {
			beanMethodVisibility = value;
			return this;
		}

		/**
		 * Beans require no-arg constructors.
		 *
		 * <p>
		 * When enabled, a Java class must implement a default no-arg constructor to be considered a bean.
		 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean without a no-arg constructor.</jc>
		 * 	<jk>public class</jk> MyBean {
		 *
		 * 		<jc>// A property method.</jc>
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 *
		 * 		<jc>// A no-arg constructor</jc>
		 * 		<jk>public</jk> MyBean(String <jv>foo</jv>) {
		 * 			<jk>this</jk>.<jf>foo</jf> = <jv>foo</jv>;
		 * 		}
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> String toString() {
		 * 			<jk>return</jk> <js>"bar"</js>;
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a serializer that ignores beans without default constructors.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beansRequireDefaultConstructor()
		 * 		.build();
		 *
		 * 	<jc>// Produces:  "bar"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Bean @Bean} annotation can be used on a bean class to override this setting.
		 * 	<li class='note'>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a class to ignore it as a bean.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beansRequireDefaultConstructor()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder beansRequireDefaultConstructor() {
			return beansRequireDefaultConstructor(true);
		}

		/**
		 * Same as {@link #beansRequireDefaultConstructor()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beansRequireDefaultConstructor(boolean value) {
			beansRequireDefaultConstructor = value;
			return this;
		}

		/**
		 * Beans require Serializable interface.
		 *
		 * <p>
		 * When enabled, a Java class must implement the {@link Serializable} interface to be considered a bean.
		 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean without a Serializable interface.</jc>
		 * 	<jk>public class</jk> MyBean {
		 *
		 * 		<jc>// A property method.</jc>
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> String toString() {
		 * 			<jk>return</jk> <js>"bar"</js>;
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a serializer that ignores beans not implementing Serializable.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beansRequireSerializable()
		 * 		.build();
		 *
		 * 	<jc>// Produces:  "bar"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Bean @Bean} annotation can be used on a bean class to override this setting.
		 * 	<li class='note'>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a class to ignore it as a bean.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beansRequireSerializable()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder beansRequireSerializable() {
			return beansRequireSerializable(true);
		}

		/**
		 * Same as {@link #beansRequireSerializable()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beansRequireSerializable(boolean value) {
			beansRequireSerializable = value;
			return this;
		}

		/**
		 * Beans require setters for getters.
		 *
		 * <p>
		 * When enabled, ignore read-only properties (properties with getters but not setters).
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean without a Serializable interface.</jc>
		 * 	<jk>public class</jk> MyBean {
		 *
		 * 		<jc>// A read/write property.</jc>
		 * 		<jk>public</jk> String getFoo() { <jk>return</jk> <js>"foo"</js>; }
		 * 		<jk>public void</jk> setFoo(String <jv>foo</jv>) { ... }
		 *
		 * 		<jc>// A read-only property.</jc>
		 * 		<jk>public</jk> String getBar() { <jk>return</jk> <js>"bar"</js>; }
		 * 	}
		 *
		 * 	<jc>// Create a serializer that ignores bean properties without setters.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beansRequireSettersForGetters()
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"foo"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Beanp @Beanp} annotation can be used on the getter to override this setting.
		 * 	<li class='note'>The {@link BeanIgnore @BeanIgnore} annotation can also be used on getters to ignore them as bean properties.
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder beansRequireSettersForGetters() {
			return beansRequireSettersForGetters(true);
		}

		/**
		 * Same as {@link #beansRequireSettersForGetters()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beansRequireSettersForGetters(boolean value) {
			beansRequireSettersForGetters = value;
			return this;
		}

		/**
		 * Beans don't require at least one property.
		 *
		 * <p>
		 * When enabled, then a Java class doesn't need to contain at least 1 property to be considered a bean.
		 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
		 *
		 * <p>
		 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with no properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 	}
		 *
		 * 	<jc>// Create a serializer that serializes beans even if they have zero properties.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.disableBeansRequireSomeProperties()
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Bean @Bean} annotation can be used on the class to force it to be recognized as a bean class
		 * 		even if it has no properties.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableBeansRequireSomeProperties()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableBeansRequireSomeProperties() {
			return disableBeansRequireSomeProperties(true);
		}

		/**
		 * Same as {@link #disableBeansRequireSomeProperties()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableBeansRequireSomeProperties(boolean value) {
			disableBeansRequireSomeProperties = value;
			return this;
		}

		/**
		 * Bean property includes.
		 *
		 * <p>
		 * Specifies the set and order of names of properties associated with the bean class.
		 *
		 * <p>
		 * For example, <c>beanProperties(MyBean.<jk>class</jk>, <js>"foo,bar"</js>)</c> means only serialize the <c>foo</c> and
		 * <c>bar</c> properties on the specified bean.  Likewise, parsing will ignore any bean properties not specified
		 * and either throw an exception or silently ignore them depending on whether {@link #ignoreUnknownBeanProperties()}
		 * has been called.
		 *
		 * <p>
		 * This value is entirely optional if you simply want to expose all the getters and public fields on
		 * a class as bean properties.  However, it's useful if you want certain getters to be ignored or you want the properties to be
		 * serialized in a particular order.  Note that on IBM JREs, the property order is the same as the order in the source code,
		 * whereas on Oracle JREs, the order is entirely random.
		 *
		 * <p>
		 * Setting applies to specified class and all subclasses.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String
		 * 			<jf>foo</jf> = <js>"foo"</js>,
		 * 			<jf>bar</jf> = <js>"bar"</js>,
		 * 			<jf>baz</jf> = <js>"baz"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that includes only the 'foo' and 'bar' properties on the MyBean class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanProperties(MyBean.<jk>class</jk>, <js>"foo,bar"</js>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>beanClass</jv>).properties(<jv>properties</jv>).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#properties()}/{@link Bean#p()} - On an annotation on the bean class itself.
		 * </ul>
		 *
		 * @param beanClass The bean class.
		 * @param properties Comma-delimited list of property names.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanProperties(Class<?> beanClass, String properties) {
			return annotations(BeanAnnotation.create(beanClass).p(properties).build());
		}

		/**
		 * Bean property includes.
		 *
		 * <p>
		 * Specifies the set and order of names of properties associated with bean classes.
		 *
		 * <p>
		 * For example, <c>beanProperties(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"foo,bar"</js>))</c> means only serialize the <c>foo</c> and
		 * <c>bar</c> properties on the specified bean.  Likewise, parsing will ignore any bean properties not specified
		 * and either throw an exception or silently ignore them depending on whether {@link #ignoreUnknownBeanProperties()}
		 * has been called.
		 *
		 * <p>
		 * This value is entirely optional if you simply want to expose all the getters and public fields on
		 * a class as bean properties.  However, it's useful if you want certain getters to be ignored or you want the properties to be
		 * serialized in a particular order.  Note that on IBM JREs, the property order is the same as the order in the source code,
		 * whereas on Oracle JREs, the order is entirely random.
		 *
		 * <p>
		 * Setting applies to specified class and all subclasses.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String
		 * 			<jf>foo</jf> = <js>"foo"</js>,
		 * 			<jf>bar</jf> = <js>"bar"</js>,
		 * 			<jf>baz</jf> = <js>"baz"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that includes only the 'foo' and 'bar' properties on the MyBean class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanProperties(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"foo,bar"</js>))
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code for each entry:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>key</jv>).properties(<jv>value</jv>.toString()).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#properties()} / {@link Bean#p()}- On an annotation on the bean class itself.
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this builder.
		 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
		 * 	<br>Values are comma-delimited lists of property names.  Non-String objects are first converted to Strings.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanProperties(Map<String,Object> values) {
			values.forEach((k,v) -> annotations(BeanAnnotation.create(k).p(stringify(v)).build()));
			return this;
		}

		/**
		 * Bean property includes.
		 *
		 * <p>
		 * Specifies the set and order of names of properties associated with the bean class.
		 *
		 * <p>
		 * For example, <c>beanProperties(<js>"MyBean"</js>, <js>"foo,bar"</js>)</c> means only serialize the <c>foo</c> and
		 * <c>bar</c> properties on the specified bean.  Likewise, parsing will ignore any bean properties not specified
		 * and either throw an exception or silently ignore them depending on whether {@link #ignoreUnknownBeanProperties()}
		 * has been called.
		 *
		 * <p>
		 * This value is entirely optional if you simply want to expose all the getters and public fields on
		 * a class as bean properties.  However, it's useful if you want certain getters to be ignored or you want the properties to be
		 * serialized in a particular order.  Note that on IBM JREs, the property order is the same as the order in the source code,
		 * whereas on Oracle JREs, the order is entirely random.
		 *
		 * <p>
		 * Setting applies to specified class and all subclasses.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String
		 * 			<jf>foo</jf> = <js>"foo"</js>,
		 * 			<jf>bar</jf> = <js>"bar"</js>,
		 * 			<jf>baz</jf> = <js>"baz"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that includes only the 'foo' and 'bar' properties on the MyBean class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanProperties(<js>"MyBean"</js>, <js>"foo,bar"</js>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>beanClassName</jv>).properties(<jv>properties</jv>).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#properties()} / {@link Bean#p()} - On an annotation on the bean class itself.
		 * </ul>
		 *
		 * @param beanClassName
		 * 	The bean class name.
		 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
		 * @param properties Comma-delimited list of property names.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanProperties(String beanClassName, String properties) {
			return annotations(BeanAnnotation.create(beanClassName).p(properties).build());
		}

		/**
		 * Bean property excludes.
		 *
		 * <p>
		 * Specifies to exclude the specified list of properties for the specified bean class.
		 *
		 * <p>
		 * Same as {@link #beanProperties(Class, String)} except you specify a list of bean property names that you want to exclude from
		 * serialization.
		 *
		 * <p>
		 * Setting applies to specified class and all subclasses.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String
		 * 			<jf>foo</jf> = <js>"foo"</js>,
		 * 			<jf>bar</jf> = <js>"bar"</js>,
		 * 			<jf>baz</jf> = <js>"baz"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that excludes the "bar" and "baz" properties on the MyBean class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesExcludes(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"foo"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>beanClass</jv>).excludeProperties(<jv>properties</jv>).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#excludeProperties()} / {@link Bean#xp()}
		 * </ul>
		 *
		 * @param beanClass The bean class.
		 * @param properties Comma-delimited list of property names.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			return annotations(BeanAnnotation.create(beanClass).xp(properties).build());
		}

		/**
		 * Bean property excludes.
		 *
		 * <p>
		 * Specifies to exclude the specified list of properties for the specified bean classes.
		 *
		 * <p>
		 * Same as {@link #beanProperties(Map)} except you specify a list of bean property names that you want to exclude from
		 * serialization.
		 *
		 * <p>
		 * Setting applies to specified class and all subclasses.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String
		 * 			<jf>foo</jf> = <js>"foo"</js>,
		 * 			<jf>bar</jf> = <js>"bar"</js>,
		 * 			<jf>baz</jf> = <js>"baz"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that excludes the "bar" and "baz" properties on the MyBean class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesExcludes(AMap.of(<js>"MyBean"</js>, <js>"bar,baz"</js>))
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"foo"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code for each entry:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>key</jv>).excludeProperties(<jv>value</jv>.toString()).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#excludeProperties()} / {@link Bean#xp()}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this builder.
		 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
		 * 	<br>Values are comma-delimited lists of property names.  Non-String objects are first converted to Strings.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			values.forEach((k,v) -> annotations(BeanAnnotation.create(k).xp(stringify(v)).build()));
			return this;
		}

		/**
		 * Bean property excludes.
		 *
		 * <p>
		 * Specifies to exclude the specified list of properties for the specified bean class.
		 *
		 * <p>
		 * Same as {@link #beanPropertiesExcludes(String, String)} except you specify a list of bean property names that you want to exclude from
		 * serialization.
		 *
		 * <p>
		 * Setting applies to specified class and all subclasses.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String
		 * 			<jf>foo</jf> = <js>"foo"</js>,
		 * 			<jf>bar</jf> = <js>"bar"</js>,
		 * 			<jf>baz</jf> = <js>"baz"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that excludes the "bar" and "baz" properties on the MyBean class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesExcludes(<js>"MyBean"</js>, <js>"bar,baz"</js>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"foo"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>beanClassName</jv>).excludeProperties(<jv>properties</jv>).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#excludeProperties()} / {@link Bean#xp()}
		 * </ul>
		 *
		 * @param beanClassName
		 * 	The bean class name.
		 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
		 * @param properties Comma-delimited list of property names.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			return annotations(BeanAnnotation.create(beanClassName).xp(properties).build());
		}

		/**
		 * Read-only bean properties.
		 *
		 * <p>
		 * Specifies one or more properties on a bean that are read-only despite having valid getters.
		 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
		 * Note that this is different from the {@link #beanProperties(Class,String) beanProperties}/{@link #beanPropertiesExcludes(Class,String) beanPropertiesExcludes} settings which include or exclude properties
		 * for both serializers and parsers.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer with read-only property settings.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesReadOnly(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
		 * 		.build();
		 *
		 * 	<jc>// All 3 properties will be serialized.</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 *
		 * 	<jc>// Create a parser with read-only property settings.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesReadOnly(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
		 * 		.ignoreUnknownBeanProperties()
		 * 		.build();
		 *
		 * 	<jc>// Parser ignores bar and baz properties.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>beanClass</jv>).readOnlyProperties(<jv>properties</jv>).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#readOnlyProperties()} / {@link Bean#ro()}
		 * </ul>
		 *
		 * @param beanClass The bean class.
		 * @param properties Comma-delimited list of property names.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			return annotations(BeanAnnotation.create(beanClass).ro(properties).build());
		}

		/**
		 * Read-only bean properties.
		 *
		 * <p>
		 * Specifies one or more properties on beans that are read-only despite having valid getters.
		 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
		 * Note that this is different from the {@link #beanProperties(Class,String) beanProperties}/{@link #beanPropertiesExcludes(Class,String) beanPropertiesExcludes} settings which include or exclude properties
		 * for both serializers and parsers.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer with read-only property settings.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesReadOnly(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
		 * 		.build();
		 *
		 * 	<jc>// All 3 properties will be serialized.</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 *
		 * 	<jc>// Create a parser with read-only property settings.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesReadOnly(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
		 * 		.ignoreUnknownBeanProperties()
		 * 		.build();
		 *
		 * 	<jc>// Parser ignores bar and baz properties.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code for each entry:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>key</jv>).readOnlyProperties(<jv>value</jv>.toString()).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#readOnlyProperties()} / {@link Bean#ro()}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this builder.
		 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
		 * 	<br>Values are comma-delimited lists of property names.  Non-String objects are first converted to Strings.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			values.forEach((k,v) -> annotations(BeanAnnotation.create(k).ro(stringify(v)).build()));
			return this;
		}

		/**
		 * Read-only bean properties.
		 *
		 * <p>
		 * Specifies one or more properties on a bean that are read-only despite having valid getters.
		 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
		 * Note that this is different from the {@link #beanProperties(Class,String) beanProperties}/{@link #beanPropertiesExcludes(Class,String) beanPropertiesExcludes} settings which include or exclude properties
		 * for both serializers and parsers.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer with read-only property settings.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesReadOnly(<js>"MyBean"</js>, <js>"bar,baz"</js>)
		 * 		.build();
		 *
		 * 	<jc>// All 3 properties will be serialized.</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 *
		 * 	<jc>// Create a parser with read-only property settings.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesReadOnly(<js>"MyBean"</js>, <js>"bar,baz"</js>)
		 * 		.ignoreUnknownBeanProperties()
		 * 		.build();
		 *
		 * 	<jc>// Parser ignores bar and baz properties.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>beanClassName</jv>).readOnlyProperties(<jv>properties</jv>).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#readOnlyProperties()} / {@link Bean#ro()}
		 * </ul>
		 *
		 * @param beanClassName
		 * 	The bean class name.
		 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
		 * @param properties Comma-delimited list of property names.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			return annotations(BeanAnnotation.create(beanClassName).ro(properties).build());
		}

		/**
		 * Write-only bean properties.
		 *
		 * <p>
		 * Specifies one or more properties on a bean that are write-only despite having valid setters.
		 * Parsers will parse such properties as usual, but serializers will silently ignore them.
		 * Note that this is different from the {@link #beanProperties(Class,String) beanProperties}/{@link #beanPropertiesExcludes(Class,String) beanPropertiesExcludes} settings which include or exclude properties
		 * for both serializers and parsers.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer with write-only property settings.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesWriteOnly(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
		 * 		.build();
		 *
		 * 	<jc>// Only foo will be serialized.</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 *
		 * 	<jc>// Create a parser with write-only property settings.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesWriteOnly(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
		 * 		.build();
		 *
		 * 	<jc>// Parser parses all 3 properties.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>beanClass</jv>).writeOnlyProperties(<jv>properties</jv>).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#writeOnlyProperties()} / {@link Bean#wo()}
		 * </ul>
		 *
		 * @param beanClass The bean class.
		 * @param properties Comma-delimited list of property names.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			return annotations(BeanAnnotation.create(beanClass).wo(properties).build());
		}

		/**
		 * Write-only bean properties.
		 *
		 * <p>
		 * Specifies one or more properties on a bean that are write-only despite having valid setters.
		 * Parsers will parse such properties as usual, but serializers will silently ignore them.
		 * Note that this is different from the {@link #beanProperties(Class,String) beanProperties}/{@link #beanPropertiesExcludes(Class,String) beanPropertiesExcludes} settings which include or exclude properties
		 * for both serializers and parsers.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer with write-only property settings.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesWriteOnly(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
		 * 		.build();
		 *
		 * 	<jc>// Only foo will be serialized.</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 *
		 * 	<jc>// Create a parser with write-only property settings.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesWriteOnly(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
		 * 		.build();
		 *
		 * 	<jc>// Parser parses all 3 properties.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code for each entry:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>key</jv>).writeOnlyProperties(<jv>value</jv>.toString()).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#writeOnlyProperties()} / {@link Bean#wo()}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this builder.
		 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
		 * 	<br>Values are comma-delimited lists of property names.  Non-String objects are first converted to Strings.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			values.forEach((k,v) -> annotations(BeanAnnotation.create(k).wo(stringify(v)).build()));
			return this;
		}

		/**
		 * Write-only bean properties.
		 *
		 * <p>
		 * Specifies one or more properties on a bean that are write-only despite having valid setters.
		 * Parsers will parse such properties as usual, but serializers will silently ignore them.
		 * Note that this is different from the {@link #beanProperties(Class,String) beanProperties}/{@link #beanPropertiesExcludes(Class,String) beanPropertiesExcludes} settings which include or exclude properties
		 * for both serializers and parsers.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer with write-only property settings.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesWriteOnly(<js>"MyBean"</js>, <js>"bar,baz"</js>)
		 * 		.build();
		 *
		 * 	<jc>// Only foo will be serialized.</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 *
		 * 	<jc>// Create a parser with write-only property settings.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.beanPropertiesWriteOnly(<js>"MyBean"</js>, <js>"bar,baz"</js>)
		 * 		.build();
		 *
		 * 	<jc>// Parser parses all 3 properties.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <p>
		 * This method is functionally equivalent to the following code:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(BeanAnnotation.<jsm>create</jsm>(<jv>beanClassName</jv>).writeOnlyProperties(<jv>properties</jv>).build());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#writeOnlyProperties()} / {@link Bean#wo()}
		 * </ul>
		 *
		 * @param beanClassName
		 * 	The bean class name.
		 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
		 * @param properties Comma-delimited list of property names.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			return annotations(BeanAnnotation.create(beanClassName).wo(properties).build());
		}

		/**
		 * Bean dictionary.
		 *
		 * <p>
		 * The list of classes that make up the bean dictionary in this bean context.
		 *
		 * <p>
		 * Values are prepended to the list so that later calls can override classes of earlier calls.
		 *
		 * <p>
		 * A dictionary is a name/class mapping used to find class types during parsing when they cannot be inferred
		 * through reflection.  The names are defined through the {@link Bean#typeName() @Bean(typeName)} annotation defined
		 * on the bean class.  For example, if a class <c>Foo</c> has a type-name of <js>"myfoo"</js>, then it would end up
		 * serialized as <js>"{_type:'myfoo',...}"</js> in JSON
		 * or <js>"&lt;myfoo&gt;...&lt;/myfoo&gt;"</js> in XML.
		 *
		 * <p>
		 * This setting tells the parsers which classes to look for when resolving <js>"_type"</js> attributes.
		 *
		 * <p>
		 * Values can consist of any of the following types:
		 * <ul>
		 * 	<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean(typeName)}.
		 * 	<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
		 * 	<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
		 * 	<li>Any array or collection of the objects above.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// POJOs with @Bean(name) annotations.</jc>
		 * 	<ja>@Bean</ja>(typeName=<js>"foo"</js>)
		 * 	<jk>public class</jk> Foo {...}
		 * 	<ja>@Bean</ja>(typeName=<js>"bar"</js>)
		 * 	<jk>public class</jk> Bar {...}
		 *
		 * 	<jc>// Create a parser and tell it which classes to try to resolve.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.dictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>)
		 * 		.addBeanTypes()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a field with an indeterminate type.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>mySimpleField</jf>;
		 * 	}
		 *
		 * 	<jc>// Parse bean.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{mySimpleField:{_type:'foo',...}}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <p>
		 * Another option is to use the {@link Bean#dictionary()} annotation on the POJO class itself:
		 *
		 * <p class='bjava'>
		 * 	<jc>// Instead of by parser, define a bean dictionary on a class through an annotation.</jc>
		 * 	<jc>// This applies to all properties on this class and all subclasses.</jc>
		 * 	<ja>@Bean</ja>(dictionary={Foo.<jk>class</jk>,Bar.<jk>class</jk>})
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>mySimpleField</jf>;  <jc>// May contain Foo or Bar object.</jc>
		 * 		<jk>public</jk> Map&lt;String,Object&gt; <jf>myMapField</jf>;  <jc>// May contain Foo or Bar objects.</jc>
		 * 	}
		 * </p>
		 *
		 * <p>
		 * 	A typical usage is to allow for HTML documents to be parsed back into HTML beans:
		 * <p class='bjava'>
		 * 	<jc>// Use the predefined HTML5 bean dictionary which is a BeanDictionaryList.</jc>
		 * 	ReaderParser <jv>parser</jv> = HtmlParser
		 * 		.<jsm>create</jsm>()
		 * 		.dictionary(HtmlBeanDictionary.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Parse an HTML body into HTML beans.</jc>
		 * 	Body <jv>body</jv> = <jv>parser</jv>.parse(<js>"&lt;body&gt;&lt;ul&gt;&lt;li&gt;foo&lt;/li&gt;&lt;li&gt;bar&lt;/li&gt;&lt;/ul&gt;"</js>, Body.<jk>class</jk>);
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.Bean#dictionary()}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.Beanp#dictionary()}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#dictionary()}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#dictionary_replace()}
		 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanDictionary(Class...)}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanDictionary(Class<?>...values) {
			return beanDictionary(alist(values));
		}

		/**
		 * Same as {@link #beanDictionary(Class...)} but allows you to pass in a collection of classes.
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * @return This object.
		 * @see #beanDictionary(Class...)
		 */
		@FluentSetter
		public Builder beanDictionary(Collection<Class<?>> values) {
			beanDictionary().addAll(0, values);
			return this;
		}

		/**
		 * Returns the bean dictionary list.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #beanDictionary(Class...)}.
		 *
		 * @return The bean dictionary list.
		 * @see #beanDictionary(Class...)
		 */
		public List<Class<?>> beanDictionary() {
			if (beanDictionary == null)
				beanDictionary = list();
			return beanDictionary;
		}

		/**
		 * Bean dictionary.
		 *
		 * <p>
		 * This is identical to {@link #beanDictionary(Class...)}, but specifies a dictionary within the context of
		 * a single class as opposed to globally.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// POJOs with @Bean(name) annotations.</jc>
		 * 	<ja>@Bean</ja>(typeName=<js>"foo"</js>)
		 * 	<jk>public class</jk> Foo {...}
		 * 	<ja>@Bean</ja>(typeName=<js>"bar"</js>)
		 * 	<jk>public class</jk> Bar {...}
		 *
		 * 	<jc>// A bean with a field with an indeterminate type.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>mySimpleField</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a parser and tell it which classes to try to resolve.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.dictionaryOn(MyBean.<jk>class</jk>, Foo.<jk>class</jk>, Bar.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Parse bean.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{mySimpleField:{_type:'foo',...}}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <p>
		 * This is functionally equivalent to the {@link Bean#dictionary()} annotation.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jma'>{@link Bean#dictionary()}
		 * 	<li class='jm'>{@link #beanDictionary(Class...)}
		 * </ul>
		 *
		 * @param on The class that the dictionary values apply to.
		 * @param values
		 * 	The new values for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder dictionaryOn(Class<?> on, Class<?>...values) {
			return annotations(BeanAnnotation.create(on).dictionary(values).build());
		}

		/**
		 * POJO example.
		 *
		 * <p>
		 * Specifies an example of the specified class.
		 *
		 * <p>
		 * Examples are used in cases such as POJO examples in Swagger documents.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that excludes the 'foo' and 'bar' properties on the MyBean class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.example(MyBean.<jk>class</jk>, <jk>new</jk> MyBean().setFoo(<js>"foo"</js>).setBar(123))
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shorthand method for the following code:
		 * <p class='bjava'>
		 * 		<jv>builder</jv>.annotations(MarshalledAnnotation.<jsm>create</jsm>(<jv>pojoClass</jv>).example(Json5.<jsf>DEFAULT</jsf>.toString(<jv>object</jv>)).build())
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>Using this method assumes the serialized form of the object is the same as that produced
		 * 		by the default serializer.  This may not be true based on settings or swaps on the constructed serializer.
		 * </ul>
		 *
		 * <p>
		 * POJO examples can also be defined on classes via the following:
		 * <ul class='spaced-list'>
		 * 	<li>The {@link Marshalled#example()} annotation on the class itself.
		 * 	<li>A static field annotated with {@link Example @Example}.
		 * 	<li>A static method annotated with {@link Example @Example} with zero arguments or one {@link BeanSession} argument.
		 * 	<li>A static method with name <c>example</c> with no arguments or one {@link BeanSession} argument.
		 * </ul>
		 *
		 * @param <T> The POJO class.
		 * @param pojoClass The POJO class.
		 * @param o
		 * 	An instance of the POJO class used for examples.
		 * @return This object.
		 */
		@FluentSetter
		public <T> Builder example(Class<T> pojoClass, T o) {
			return annotations(MarshalledAnnotation.create(pojoClass).example(Json5.of(o)).build());
		}

		/**
		 * POJO example.
		 *
		 * <p>
		 * Specifies an example in JSON of the specified class.
		 *
		 * <p>
		 * Examples are used in cases such as POJO examples in Swagger documents.
		 *
		 * <p>
		 * Setting applies to specified class and all subclasses.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that excludes the 'foo' and 'bar' properties on the MyBean class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.example(MyBean.<jk>class</jk>, <js>"{foo:'bar'}"</js>)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shorthand method for the following code:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.annotations(MarshalledAnnotation.<jsm>create</jsm>(<jv>pojoClass</jv>).example(<jv>json</jv>).build())
		 * </p>
		 *
		 * <p>
		 * POJO examples can also be defined on classes via the following:
		 * <ul class='spaced-list'>
		 * 	<li>A static field annotated with {@link Example @Example}.
		 * 	<li>A static method annotated with {@link Example @Example} with zero arguments or one {@link BeanSession} argument.
		 * 	<li>A static method with name <c>example</c> with no arguments or one {@link BeanSession} argument.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#example()}
		 * </ul>
		 *
		 * @param <T> The POJO class type.
		 * @param pojoClass The POJO class.
		 * @param json The JSON 5 representation of the example.
		 * @return This object.
		 */
		@FluentSetter
		public <T> Builder example(Class<T> pojoClass, String json) {
			return annotations(MarshalledAnnotation.create(pojoClass).example(json).build());
		}

		/**
		 * Find fluent setters.
		 *
		 * <p>
		 * When enabled, fluent setters are detected on beans during parsing.
		 *
		 * <p>
		 * Fluent setters must have the following attributes:
		 * <ul>
		 * 	<li>Public.
		 * 	<li>Not static.
		 * 	<li>Take in one parameter.
		 * 	<li>Return the bean itself.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a fluent setter.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> MyBean foo(String <jv>value</jv>) {...}
		 * 	}
		 *
		 * 	<jc>// Create a parser that finds fluent setters.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.findFluentSetters()
		 * 		.build();
		 *
		 * 	<jc>// Parse into bean using fluent setter.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'bar'}"</js>);
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Beanp @Beanp} annotation can also be used on methods to individually identify them as fluent setters.
		 * 	<li class='note'>The {@link Bean#findFluentSetters() @Bean.fluentSetters()} annotation can also be used on classes to specify to look for fluent setters.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.Bean#findFluentSetters()}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#findFluentSetters()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder findFluentSetters() {
			return findFluentSetters(true);
		}

		/**
		 * Same as {@link #findFluentSetters()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder findFluentSetters(boolean value) {
			findFluentSetters = value;
			return this;
		}

		/**
		 * Find fluent setters.
		 *
		 * <p>
		 * Identical to {@link #findFluentSetters()} but enables it on a specific class only.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a fluent setter.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> MyBean foo(String <jv>value</jv>) {...}
		 * 	}
		 *
		 * 	<jc>// Create a parser that finds fluent setters.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.findFluentSetters(MyBean.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Parse into bean using fluent setter.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'bar'}"</js>);
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This method is functionally equivalent to using the {@link Bean#findFluentSetters()} annotation.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Bean#findFluentSetters()}
		 * 	<li class='jm'>{@link #findFluentSetters()}
		 * </ul>
		 *
		 * @param on The class that this applies to.
		 * @return This object.
		 */
		@FluentSetter
		public Builder findFluentSetters(Class<?> on) {
			return annotations(BeanAnnotation.create(on).findFluentSetters(true).build());
		}

		/**
		 * Ignore invocation errors on getters.
		 *
		 * <p>
		 * When enabled, errors thrown when calling bean getter methods will silently be ignored.
		 * Otherwise, a {@code BeanRuntimeException} is thrown.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a property that throws an exception.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String getFoo() {
		 * 			<jk>throw new</jk> RuntimeException(<js>"foo"</js>);
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a serializer that ignores bean getter exceptions.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.ingoreInvocationExceptionsOnGetters()
		 * 		.build();
		 *
		 * 	<jc>// Exception is ignored.</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreInvocationExceptionsOnGetters()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreInvocationExceptionsOnGetters() {
			return ignoreInvocationExceptionsOnGetters(true);
		}

		/**
		 * Same as {@link #ignoreInvocationExceptionsOnGetters()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreInvocationExceptionsOnGetters(boolean value) {
			ignoreInvocationExceptionsOnGetters = value;
			return this;
		}

		/**
		 * Ignore invocation errors on setters.
		 *
		 * <p>
		 * When enabled, errors thrown when calling bean setter methods will silently be ignored.
		 * Otherwise, a {@code BeanRuntimeException} is thrown.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a property that throws an exception.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public void</jk> setFoo(String <jv>foo</jv>) {
		 * 			<jk>throw new</jk> RuntimeException(<js>"foo"</js>);
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a parser that ignores bean setter exceptions.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.ignoreInvocationExceptionsOnSetters()
		 * 		.build();
		 *
		 * 	<jc>// Exception is ignored.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'bar'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreInvocationExceptionsOnSetters()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreInvocationExceptionsOnSetters() {
			return ignoreInvocationExceptionsOnSetters(true);
		}

		/**
		 * Same as {@link #ignoreInvocationExceptionsOnSetters()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreInvocationExceptionsOnSetters(boolean value) {
			ignoreInvocationExceptionsOnSetters = value;
			return this;
		}

		/**
		 * Don't silently ignore missing setters.
		 *
		 * <p>
		 * When enabled, trying to set a value on a bean property without a setter will throw a {@link BeanRuntimeException}.
		 * Otherwise, it will be silently ignored.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a property with a getter but not a setter.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public void</jk> getFoo() {
		 * 			<jk>return</jk> <js>"foo"</js>;
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a parser that throws an exception if a setter is not found but a getter is.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.disableIgnoreMissingSetters()
		 * 		.build();
		 *
		 * 	<jc>// Throws a ParseException.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'bar'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link BeanIgnore @BeanIgnore} annotation can also be used on getters and fields to ignore them.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableIgnoreMissingSetters()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableIgnoreMissingSetters() {
			return disableIgnoreMissingSetters(true);
		}

		/**
		 * Same as {@link #disableIgnoreMissingSetters()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableIgnoreMissingSetters(boolean value) {
			disableIgnoreMissingSetters = value;
			return this;
		}

		/**
		 * Don't ignore transient fields.
		 *
		 * <p>
		 * When enabled, methods and fields marked as <jk>transient</jk> will not be ignored as bean properties.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a transient field.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public transient</jk> String <jf>foo</jf> = <js>"foo"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that doesn't ignore transient fields.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.disableIgnoreTransientFields()
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo":"foo"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Beanp @Beanp} annotation can also be used on transient fields to keep them from being ignored.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableIgnoreTransientFields()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableIgnoreTransientFields() {
			return disableIgnoreTransientFields(true);
		}

		/**
		 * Same as {@link #disableIgnoreTransientFields()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableIgnoreTransientFields(boolean value) {
			disableIgnoreTransientFields = value;
			return this;
		}

		/**
		 * Ignore unknown properties.
		 *
		 * <p>
		 * When enabled, trying to set a value on a non-existent bean property will silently be ignored.
		 * Otherwise, a {@code BeanRuntimeException} is thrown.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a single property.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a parser that ignores missing bean properties.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.ignoreUnknownBeanProperties()
		 * 		.build();
		 *
		 * 	<jc>// Doesn't throw an exception on unknown 'bar' property.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'foo',bar:'bar'}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreUnknownBeanProperties()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreUnknownBeanProperties() {
			return ignoreUnknownBeanProperties(true);
		}

		/**
		 * Same as {@link #ignoreUnknownBeanProperties()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreUnknownBeanProperties(boolean value) {
			ignoreUnknownBeanProperties = value;
			return this;
		}

		/**
		 * Ignore unknown properties.
		 *
		 * <p>
		 * When enabled, unknown enum values will be set to <jk>null</jk> instead of throwing an exception.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreUnknownEnumValues()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreUnknownEnumValues() {
			return ignoreUnknownEnumValues(true);
		}

		/**
		 * Same as {@link #ignoreUnknownEnumValues()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreUnknownEnumValues(boolean value) {
			ignoreUnknownEnumValues = value;
			return this;
		}

		/**
		 * Don't ignore unknown properties with null values.
		 *
		 * <p>
		 * When enabled, trying to set a <jk>null</jk> value on a non-existent bean property will throw a {@link BeanRuntimeException}.
		 * Otherwise it will be silently ignored.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a single property.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a parser that throws an exception on an unknown property even if the value being set is null.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.disableIgnoreUnknownNullBeanProperties()
		 * 		.build();
		 *
		 * 	<jc>// Throws a BeanRuntimeException wrapped in a ParseException on the unknown 'bar' property.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"{foo:'foo',bar:null}"</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableIgnoreUnknownNullBeanProperties()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableIgnoreUnknownNullBeanProperties() {
			return disableIgnoreUnknownNullBeanProperties(true);
		}

		/**
		 * Same as {@link #disableIgnoreUnknownNullBeanProperties()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableIgnoreUnknownNullBeanProperties(boolean value) {
			disableIgnoreUnknownNullBeanProperties = value;
			return this;
		}

		/**
		 * Implementation classes.
		 *
		 * <p>
		 * For interfaces and abstract classes this method can be used to specify an implementation class for the
		 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
		 * parse).
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean interface.</jc>
		 * 	<jk>public interface</jk> MyBean {
		 * 		...
		 * 	}
		 *
		 * 	<jc>// A bean implementation.</jc>
		 * 	<jk>public class</jk> MyBeanImpl <jk>implements</jk> MyBean {
		 * 		...
		 * 	}

		 * 	<jc>// Create a parser that instantiates MyBeanImpls when parsing MyBeans.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.implClass(MyBean.<jk>class</jk>, MyBeanImpl.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Instantiates a MyBeanImpl,</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"..."</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * @param interfaceClass The interface class.
		 * @param implClass The implementation class.
		 * @return This object.
		 */
		@FluentSetter
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			return annotations(MarshalledAnnotation.create(interfaceClass).implClass(implClass).build());
		}

		/**
		 * Implementation classes.
		 *
		 * <p>
		 * For interfaces and abstract classes this method can be used to specify an implementation class for the
		 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
		 * parse).
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a single property.</jc>
		 * 	<jk>public interface</jk> MyBean {
		 * 		...
		 * 	}
		 *
		 * 	<jc>// A bean with a single property.</jc>
		 * 	<jk>public class</jk> MyBeanImpl <jk>implements</jk> MyBean {
		 * 		...
		 * 	}

		 * 	<jc>// Create a parser that instantiates MyBeanImpls when parsing MyBeans.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.implClasses(AMap.<jsm>of</jsm>(MyBean.<jk>class</jk>, MyBeanImpl.<jk>class</jk>))
		 * 		.build();
		 *
		 * 	<jc>// Instantiates a MyBeanImpl,</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<js>"..."</js>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * @param values
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			values.forEach((k,v) -> annotations(MarshalledAnnotation.create(k).implClass(v).build()));
			return this;
		}

		/**
		 * Identifies a class to be used as the interface class for the specified class and all subclasses.
		 *
		 * <p>
		 * When specified, only the list of properties defined on the interface class will be used during serialization.
		 * Additional properties on subclasses will be ignored.
		 *
		 * <p class='bjava'>
		 * 	<jc>// Parent class or interface</jc>
		 * 	<jk>public abstract class</jk> A {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"foo"</js>;
		 * 	}
		 *
		 * 	<jc>// Sub class</jc>
		 * 	<jk>public class</jk> A1 <jk>extends</jk> A {
		 * 		<jk>public</jk> String <jf>bar</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer and define our interface class mapping.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.interfaceClass(A1.<jk>class</jk>, A.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces "{"foo":"foo"}"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> A1());
		 * </p>
		 *
		 * <p>
		 * This annotation can be used on the parent class so that it filters to all child classes, or can be set
		 * individually on the child classes.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Bean#interfaceClass() @Bean(interfaceClass)} annotation is the equivalent annotation-based solution.
		 * </ul>
		 *
		 * @param on The class that the interface class applies to.
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			return annotations(BeanAnnotation.create(on).interfaceClass(value).build());
		}

		/**
		 * Identifies a set of interfaces.
		 *
		 * <p>
		 * When specified, only the list of properties defined on the interface class will be used during serialization
		 * of implementation classes.  Additional properties on subclasses will be ignored.
		 *
		 * <p class='bjava'>
		 * 	<jc>// Parent class or interface</jc>
		 * 	<jk>public abstract class</jk> A {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"foo"</js>;
		 * 	}
		 *
		 * 	<jc>// Sub class</jc>
		 * 	<jk>public class</jk> A1 <jk>extends</jk> A {
		 * 		<jk>public</jk> String <jf>bar</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer and define our interface class mapping.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.interfaces(A.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces "{"foo":"foo"}"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> A1());
		 * </p>
		 *
		 * <p>
		 * This annotation can be used on the parent class so that it filters to all child classes, or can be set
		 * individually on the child classes.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Bean#interfaceClass() @Bean(interfaceClass)} annotation is the equivalent annotation-based solution.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder interfaces(Class<?>...value) {
			for (Class<?> v : value)
				annotations(BeanAnnotation.create(v).interfaceClass(v).build());
			return this;
		}

		/**
		 * <i><l>Context</l> configuration property:&emsp;</i>  Locale.
		 *
		 * <p>
		 * Specifies the default locale for serializer and parser sessions when not specified via {@link BeanSession.Builder#locale(Locale)}.
		 * Typically used for POJO swaps that need to deal with locales such as swaps that convert <l>Date</l> and <l>Calendar</l>
		 * objects to strings by accessing it via the session passed into the {@link ObjectSwap#swap(BeanSession, Object)} and
		 * {@link ObjectSwap#unswap(BeanSession, Object, ClassMeta, String)} methods.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Define a POJO swap that skips serializing beans if we're in the UK.</jc>
		 * 	<jk>public class</jk> MyBeanSwap <jk>extends</jk> StringSwap&lt;MyBean&gt; {
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> String swap(BeanSession <jv>session</jv>, MyBean <jv>bean</jv>) <jk>throws</jk> Exception {
		 * 			<jk>if</jk> (<jv>session</jv>.getLocale().equals(Locale.<jsf>UK</jsf>))
		 * 				<jk>return null</jk>;
		 * 			<jk>return</jk> <jv>bean</jv>.toString();
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a serializer that uses the specified locale if it's not passed in through session args.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.locale(Locale.<jsf>UK</jsf>)
		 * 		.swaps(MyBeanSwap.<jk>class</jk>)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#locale()}
		 * 	<li class='jm'>{@link org.apache.juneau.BeanSession.Builder#locale(Locale)}
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder locale(Locale value) {
			locale = value;
			return this;
		}

		/**
		 * <i><l>Context</l> configuration property:&emsp;</i>  Media type.
		 *
		 * <p>
		 * Specifies the default media type for serializer and parser sessions when not specified via {@link BeanSession.Builder#mediaType(MediaType)}.
		 * Typically used for POJO swaps that need to serialize the same POJO classes differently depending on
		 * the specific requested media type.   For example, a swap could handle a request for media types <js>"application/json"</js>
		 * and <js>"application/json+foo"</js> slightly differently even though they're both being handled by the same JSON
		 * serializer or parser.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Define a POJO swap that skips serializing beans if the media type is application/json.</jc>
		 * 	<jk>public class</jk> MyBeanSwap <jk>extends</jk> StringSwap&lt;MyBean&gt; {
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> String swap(BeanSession <jv>session</jv>, MyBean <jv>bean</jv>) <jk>throws</jk> Exception {
		 * 			<jk>if</jk> (<jv>session</jv>.getMediaType().equals(<js>"application/json"</js>))
		 * 				<jk>return null</jk>;
		 * 			<jk>return</jk> <jv>bean</jv>.toString();
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a serializer that uses the specified media type if it's not passed in through session args.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.mediaType(MediaType.<jsf>JSON</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#mediaType()}
		 * 	<li class='jm'>{@link org.apache.juneau.BeanSession.Builder#mediaType(MediaType)}
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder mediaType(MediaType value) {
			mediaType = value;
			return this;
		}

		/**
		 * Bean class exclusions.
		 *
		 * <p>
		 * List of classes that should not be treated as beans even if they appear to be bean-like.
		 * Not-bean classes are converted to <c>Strings</c> during serialization.
		 *
		 * <p>
		 * Values can consist of any of the following types:
		 * <ul>
		 * 	<li>Classes.
		 * 	<li>Arrays and collections of classes.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a single property.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 *
		 * 		<jk>public</jk> String toString() {
		 * 			<jk>return</jk> <js>"baz"</js>;
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a serializer that doesn't treat MyBean as a bean class.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.notBeanClasses(MyBean.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces "baz" instead of {"foo":"bar"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link BeanIgnore @BeanIgnore} annotation can also be used on classes to prevent them from being recognized as beans.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanIgnore}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#notBeanClasses()}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>Values can consist of any of the following types:
		 * 	<ul>
		 * 		<li>Classes.
		 * 		<li>Arrays and collections of classes.
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder notBeanClasses(Class<?>...values) {
			return notBeanClasses(alist(values));
		}

		/**
		 * Same as {@link #notBeanClasses(Class...)} but allows you to pass in a collection of classes.
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * @return This object.
		 * @see #notBeanClasses(Class...)
		 */
		@FluentSetter
		public Builder notBeanClasses(Collection<Class<?>> values) {
			notBeanClasses().addAll(values);
			return this;
		}

		/**
		 * Returns the list of not-bean classes.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #notBeanClasses(Class...)}.
		 *
		 * @return The list of not-bean classes.
		 * @see #notBeanClasses(Class...)
		 */
		public Set<Class<?>> notBeanClasses() {
			if (notBeanClasses == null)
				notBeanClasses = classSet();
			return notBeanClasses;
		}

		/**
		 * Bean package exclusions.
		 *
		 * <p>
		 * Used as a convenient way of defining the {@link #notBeanClasses(Class...)} property for entire packages.
		 * Any classes within these packages will be serialized to strings using {@link Object#toString()}.
		 *
		 * <p>
		 * Note that you can specify suffix patterns to include all subpackages.
		 *
		 * <p>
		 * Values can consist of any of the following types:
		 * <ul>
		 * 	<li>Strings.
		 * 	<li>Arrays and collections of strings.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that ignores beans in the specified packages.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.notBeanPackages(<js>"org.apache.foo"</js>, <js>"org.apache.bar.*"</js>)
		 * 		.build();
		 * </p>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>Values can consist of any of the following types:
		 * 	<ul>
		 * 		<li>{@link Package} objects.
		 * 		<li>Strings.
		 * 		<li>Arrays and collections of anything in this list.
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder notBeanPackages(String...values) {
			return notBeanPackages(alist(values));
		}

		/**
		 * Same as {@link #notBeanPackages(String...)} but allows you to pass in a collection of classes.
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * @return This object.
		 * @see #notBeanPackages(String...)
		 */
		@FluentSetter
		public Builder notBeanPackages(Collection<String> values) {
			notBeanPackages().addAll(values);
			return this;
		}

		/**
		 * Returns the list of not-bean Java package names.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #notBeanPackages(String...)}.
		 *
		 * @return The list of not-bean Java package names.
		 * @see #notBeanPackages(String...)
		 */
		public Set<String> notBeanPackages() {
			if (notBeanPackages == null)
				notBeanPackages = new TreeSet<>();
			return notBeanPackages;
		}

		/**
		 * Bean property namer
		 *
		 * <p>
		 * The class to use for calculating bean property names.
		 *
		 * <p>
		 * Predefined classes:
		 * <ul>
		 * 	<li>{@link BasicPropertyNamer} - Default.
		 * 	<li>{@link PropertyNamerDLC} - Dashed-lower-case names.
		 * 	<li>{@link PropertyNamerULC} - Dashed-upper-case names.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a single property.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>fooBarBaz</jf> = <js>"fooBarBaz"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that uses Dashed-Lower-Case property names.</jc>
		 * 	<jc>// (e.g. "foo-bar-baz" instead of "fooBarBaz")</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.propertyNamer(PropertyNamerDLC.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo-bar-baz":"fooBarBaz"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link BasicPropertyNamer}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder propertyNamer(Class<? extends PropertyNamer> value) {
			propertyNamer = value;
			return this;
		}

		/**
		 * Bean property namer
		 *
		 * <p>
		 * Same as {@link #propertyNamer(Class)} but allows you to specify a namer for a specific class.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with a single property.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>fooBarBaz</jf> = <js>"fooBarBaz"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that uses Dashed-Lower-Case property names for the MyBean class only.</jc>
		 * 	<jc>// (e.g. "foo-bar-baz" instead of "fooBarBaz")</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.propertyNamer(MyBean.<jk>class</jk>, PropertyNamerDLC.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"foo-bar-baz":"fooBarBaz"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Bean#propertyNamer() Bean(propertyNamer)}
		 * 	<li class='jm'>{@link #propertyNamer(Class)}
		 * </ul>
		 *
		 * @param on The class that the namer applies to.
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link BasicPropertyNamer}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder propertyNamer(Class<?> on, Class<? extends PropertyNamer> value) {
			return annotations(BeanAnnotation.create(on).propertyNamer(value).build());
		}

		/**
		 * Sort bean properties.
		 *
		 * <p>
		 * When enabled, all bean properties will be serialized and access in alphabetical order.
		 * Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
		 * On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
		 * On Oracle JVMs, the bean properties are not ordered (which follows the official JVM specs).
		 *
		 * <p>
		 * this setting is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
		 * to force bean properties to be in a particular order and can just alter the order of the fields/methods
		 * in the Java file.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>c</jf> = <js>"1"</js>;
		 * 		<jk>public</jk> String <jf>b</jf> = <js>"2"</js>;
		 * 		<jk>public</jk> String <jf>a</jf> = <js>"3"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that sorts bean properties.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.sortProperties()
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"a":"3","b":"2","c":"1"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Bean#sort() @Bean.sort()} annotation can also be used to sort properties on just a single class.
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder sortProperties() {
			sortProperties = true;
			return sortProperties(true);
		}

		/**
		 * Same as {@link #sortProperties()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder sortProperties(boolean value) {
			sortProperties = value;
			return this;
		}

		/**
		 * Sort bean properties.
		 *
		 * <p>
		 * Same as {@link #sortProperties()} but allows you to specify individual bean classes instead of globally.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A bean with 3 properties.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>c</jf> = <js>"1"</js>;
		 * 		<jk>public</jk> String <jf>b</jf> = <js>"2"</js>;
		 * 		<jk>public</jk> String <jf>a</jf> = <js>"3"</js>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that sorts properties on MyBean.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.sortProperties(MyBean.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"a":"3","b":"2","c":"1"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Bean#sort() Bean(sort)}
		 * 	<li class='jm'>{@link #sortProperties()}
		 * </ul>
		 *
		 * @param on The bean classes to sort properties on.
		 * @return This object.
		 */
		@FluentSetter
		public Builder sortProperties(Class<?>...on) {
			for (Class<?> c : on)
				annotations(BeanAnnotation.create(c).sort(true).build());
			return this;
		}

		/**
		 * Identifies a stop class for the annotated class.
		 *
		 * <p>
		 * Identical in purpose to the stop class specified by {@link Introspector#getBeanInfo(Class, Class)}.
		 * Any properties in the stop class or in its base classes will be ignored during analysis.
		 *
		 * <p>
		 * For example, in the following class hierarchy, instances of <c>C3</c> will include property <c>p3</c>,
		 * but not <c>p1</c> or <c>p2</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jk>public class</jk> C1 {
		 * 		<jk>public int</jk> getP1();
		 * 	}
		 *
		 * 	<jk>public class</jk> C2 <jk>extends</jk> C1 {
		 * 		<jk>public int</jk> getP2();
		 * 	}
		 *
		 * 	<jk>public class</jk> C3 <jk>extends</jk> C2 {
		 * 		<jk>public int</jk> getP3();
		 * 	}
		 *
		 * 	<jc>// Create a serializer specifies a stop class for C3.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.stopClass(C3.<jk>class</jk>, C2.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"p3":"..."}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> C3());
		 * </p>
		 *
		 * @param on The class on which the stop class is being applied.
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder stopClass(Class<?> on, Class<?> value) {
			return annotations(BeanAnnotation.create(on).stopClass(value).build());
		}

		/**
		 * Java object swaps.
		 *
		 * <p>
		 * Swaps are used to "swap out" non-serializable classes with serializable equivalents during serialization,
		 * and "swap in" the non-serializable class during parsing.
		 *
		 * <p>
		 * An example of a swap would be a <c>Calendar</c> object that gets swapped out for an ISO8601 string.
		 *
		 * <p>
		 * Multiple swaps can be associated with a single class.
		 * When multiple swaps are applicable to the same class, the media type pattern defined by
		 * {@link ObjectSwap#forMediaTypes()} or {@link Swap#mediaTypes() @Swap(mediaTypes)} are used to come up with the best match.
		 *
		 * <p>
		 * Values can consist of any of the following types:
		 * <ul>
		 * 	<li>Any subclass of {@link ObjectSwap}.
		 * 	<li>Any instance of {@link ObjectSwap}.
		 * 	<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
		 * 	<li>Any array or collection of the objects above.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Sample swap for converting Dates to ISO8601 strings.</jc>
		 * 	<jk>public class</jk> MyDateSwap <jk>extends</jk> StringSwap&lt;Date&gt; {
		 * 		<jc>// ISO8601 formatter.</jc>
		 * 		<jk>private</jk> DateFormat <jf>format</jf> = <jk>new</jk> SimpleDateFormat(<js>"yyyy-MM-dd'T'HH:mm:ssZ"</js>);
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> String swap(BeanSession <jv>session</jv>, Date <jv>date</jv>) {
		 * 			<jk>return</jk> <jf>format</jf>.format(<jv>date</jv>);
		 * 		}
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> Date unswap(BeanSession <jv>session</jv>, String <jv>string</jv>, ClassMeta <jv>hint</jv>) <jk>throws</jk> Exception {
		 * 			<jk>return</jk> <jf>format</jf>.parse(<jv>string</jv>);
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Sample bean with a Date field.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Date <jf>date</jf> = <jk>new</jk> Date(112, 2, 3, 4, 5, 6);
		 * 	}
		 *
		 * 	<jc>// Create a serializer that uses our date swap.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.swaps(MyDateSwap.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"date":"2012-03-03T04:05:06-0500"}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 *
		 * 	<jc>// Create a serializer that uses our date swap.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.swaps(MyDateSwap.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Use our parser to parse a bean.</jc>
		 * 	MyBean <jv>bean</jv> = <jv>parser</jv>.parse(<jv>json</jv>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link Swap @Swap} annotation can also be used on classes to identify swaps for the class.
		 * 	<li class='note'>The {@link Swap @Swap} annotation can also be used on bean methods and fields to identify swaps for values of those bean properties.
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>Values can consist of any of the following types:
		 * 	<ul>
		 * 		<li>Any subclass of {@link ObjectSwap}.
		 * 		<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
		 * 		<li>Any array or collection of the objects above.
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder swaps(Class<?>...values) {
			return swaps(alist(values));
		}

		/**
		 * Same as {@link #swaps(Class...)} but allows you to pass in a collection of classes.
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * @return This object.
		 * @see #swaps(Class...)
		 */
		@FluentSetter
		public Builder swaps(Collection<Class<?>> values) {
			swaps().addAll(0, values);
			return this;
		}

		/**
		 * A shortcut for defining a {@link FunctionalSwap}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that performs a custom format for DAte objects.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.swap(Date.<jk>class</jk>, String.<jk>class</jk>, <jv>x</jv> -&gt; <jsm>format</jsm>(<jv>x</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param <T> The object type being swapped out.
		 * @param <S> The object type being swapped in.
		 * @param normalClass The object type being swapped out.
		 * @param swappedClass The object type being swapped in.
		 * @param swapFunction The function to convert the object.
		 * @return This object.
		 */
		@FluentSetter
		public <T,S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			return swap(normalClass, swappedClass, swapFunction, null);
		}

		/**
		 * A shortcut for defining a {@link FunctionalSwap}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that performs a custom format for Date objects.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.swap(Date.<jk>class</jk>, String.<jk>class</jk>, <jv>x</jv> -&gt; <jsm>format</jsm>(<jv>x</jv>), <jv>x</jv> -&gt; <jsm>parse</jsm>(<jv>x</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param <T> The object type being swapped out.
		 * @param <S> The object type being swapped in.
		 * @param normalClass The object type being swapped out.
		 * @param swappedClass The object type being swapped in.
		 * @param swapFunction The function to convert the object during serialization.
		 * @param unswapFunction The function to convert the object during parsing.
		 * @return This object.
		 */
		@FluentSetter
		public <T,S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			swaps().add(0, new FunctionalSwap<>(normalClass, swappedClass, swapFunction, unswapFunction));
			return this;
		}

		/**
		 * Returns the bean swaps list.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #swaps(Class...)}.
		 *
		 * @return The bean swaps list.
		 * @see #swaps(Class...)
		 */
		public List<Object> swaps() {
			if (swaps == null)
				swaps = list();
			return swaps;
		}

		/**
		 * <i><l>Context</l> configuration property:&emsp;</i>  TimeZone.
		 *
		 * <p>
		 * Specifies the default time zone for serializer and parser sessions when not specified via {@link BeanSession.Builder#timeZone(TimeZone)}.
		 * Typically used for POJO swaps that need to deal with timezones such as swaps that convert <l>Date</l> and <l>Calendar</l>
		 * objects to strings by accessing it via the session passed into the {@link ObjectSwap#swap(BeanSession, Object)} and
		 * {@link ObjectSwap#unswap(BeanSession, Object, ClassMeta, String)} methods.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Define a POJO swap that skips serializing beans if the time zone is GMT.</jc>
		 * 	<jk>public class</jk> MyBeanSwap <jk>extends</jk> StringSwap&lt;MyBean&gt; {
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> String swap(BeanSession <jv>session</jv>, MyBean <jv>bean</jv>) <jk>throws</jk> Exception {
		 * 			<jk>if</jk> (<jv>session</jv>.getTimeZone().equals(TimeZone.<jsf>GMT</jsf>))
		 * 				<jk>return null</jk>;
		 * 			<jk>return</jk> <jv>bean</jv>.toString();
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a serializer that uses GMT if the timezone is not specified in the session args.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.timeZone(TimeZone.<jsf>GMT</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#timeZone()}
		 * 	<li class='jm'>{@link org.apache.juneau.BeanSession.Builder#timeZone(TimeZone)}
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder timeZone(TimeZone value) {
			timeZone = value;
			return this;
		}

		/**
		 * An identifying name for this class.
		 *
		 * <p>
		 * The name is used to identify the class type during parsing when it cannot be inferred through reflection.
		 * For example, if a bean property is of type <c>Object</c>, then the serializer will add the name to the
		 * output so that the class can be determined during parsing.
		 *
		 * <p>
		 * It is also used to specify element names in XML.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Use _type='mybean' to identify this bean.</jc>
		 * 	<jk>public class</jk> MyBean {...}
		 *
		 * 	<jc>// Create a serializer and specify the type name..</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.typeName(MyBean.<jk>class</jk>, <js>"mybean"</js>)
		 * 		.build();
		 *
		 * 	<jc>// Produces:  {"_type":"mybean",...}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>Equivalent to the {@link Bean#typeName() Bean(typeName)} annotation.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jc'>{@link Bean#typeName() Bean(typeName)}
		 * 	<li class='jm'>{@link #beanDictionary(Class...)}
		 * </ul>
		 *
		 * @param on
		 * 	The class the type name is being defined on.
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder typeName(Class<?> on, String value) {
			return annotations(BeanAnnotation.create(on).typeName(value).build());
		}

		/**
		 * Bean type property name.
		 *
		 * <p>
		 * This specifies the name of the bean property used to store the dictionary name of a bean type so that the
		 * parser knows the data type to reconstruct.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// POJOs with @Bean(name) annotations.</jc>
		 * 	<ja>@Bean</ja>(typeName=<js>"foo"</js>)
		 * 	<jk>public class</jk> Foo {...}
		 * 	<ja>@Bean</ja>(typeName=<js>"bar"</js>)
		 * 	<jk>public class</jk> Bar {...}
		 *
		 * 	<jc>// Create a serializer that uses 't' instead of '_type' for dictionary names.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.typePropertyName(<js>"t"</js>)
		 * 		.dictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Create a serializer that uses 't' instead of '_type' for dictionary names.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.typePropertyName(<js>"t"</js>)
		 * 		.dictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// A bean with a field with an indeterminate type.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>mySimpleField</jf>;
		 * 	}
		 *
		 * 	<jc>// Produces "{mySimpleField:{t:'foo',...}}".</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 *
		 * 	<jc>// Parse bean.</jc>
		 * 	MyBean <jv>bean</jv> = <jv>parser</jv>.parse(<jv>json</jv>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.Bean#typePropertyName()}
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#typePropertyName()}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <js>"_type"</js>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder typePropertyName(String value) {
			typePropertyName = value;
			return this;
		}

		/**
		 * Bean type property name.
		 *
		 * <p>
		 * Same as {@link #typePropertyName(String)} except targets a specific bean class instead of globally.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// POJOs with @Bean(name) annotations.</jc>
		 * 	<ja>@Bean</ja>(typeName=<js>"foo"</js>)
		 * 	<jk>public class</jk> Foo {...}
		 * 	<ja>@Bean</ja>(typeName=<js>"bar"</js>)
		 * 	<jk>public class</jk> Bar {...}
		 *
		 * 	<jc>// A bean with a field with an indeterminate type.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>mySimpleField</jf>;
		 * 	}
		 *
		 * 	<jc>// Create a serializer that uses 't' instead of '_type' for dictionary names.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.typePropertyName(MyBean.<jk>class</jk>, <js>"t"</js>)
		 * 		.dictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Produces "{mySimpleField:{t:'foo',...}}".</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Bean#typePropertyName() Bean(typePropertyName)}
		 * </ul>
		 *
		 * @param on The class the type property name applies to.
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <js>"_type"</js>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder typePropertyName(Class<?> on, String value) {
			return annotations(BeanAnnotation.create(on).typePropertyName(value).build());
		}

		/**
		 * Use enum names.
		 *
		 * <p>
		 * When enabled, enums are always serialized by name, not using {@link Object#toString()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer with debug enabled.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.useEnumNames()
		 * 		.build();
		 *
		 * 	<jc>// Enum with overridden toString().</jc>
		 * 	<jc>// Will be serialized as ONE/TWO/THREE even though there's a toString() method.</jc>
		 * 	<jk>public enum</jk> Option {
		 * 		<jsf>ONE</jsf>(1),
		 * 		<jsf>TWO</jsf>(2),
		 * 		<jsf>THREE</jsf>(3);
		 *
		 * 		<jk>private int</jk> <jf>value</jf>;
		 *
		 * 		Option(<jk>int</jk> <jv>value</jv>) {
		 * 			<jk>this</jk>.<jf>value</jf> = <jv>value</jv>;
		 * 		}
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> String toString() {
		 * 			<jk>return</jk> String.<jsm>valueOf</jsm>(<jf>value</jf>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder useEnumNames() {
			return useEnumNames(true);
		}

		/**
		 * Same as {@link #useEnumNames()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder useEnumNames(boolean value) {
			useEnumNames = value;
			return this;
		}

		/**
		 * Don't use interface proxies.
		 *
		 * <p>
		 * When enabled, interfaces will be instantiated as proxy classes through the use of an
		 * {@link InvocationHandler} if there is no other way of instantiating them.
		 * Otherwise, throws a {@link BeanRuntimeException}.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#disableInterfaceProxies()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableInterfaceProxies() {
			return disableInterfaceProxies(true);
		}

		/**
		 * Same as {@link #disableInterfaceProxies()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableInterfaceProxies(boolean value) {
			disableInterfaceProxies = value;
			return this;
		}

		/**
		 * Use Java Introspector.
		 *
		 * <p>
		 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
		 * <br>Most {@link Bean @Bean} annotations will be ignored.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that only uses the built-in java bean introspector for finding properties.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.useJavaBeanIntrospector()
		 * 		.build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder useJavaBeanIntrospector() {
			return useJavaBeanIntrospector(true);
		}

		/**
		 * Same as {@link #useJavaBeanIntrospector()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder useJavaBeanIntrospector(boolean value) {
			useJavaBeanIntrospector = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>

		//-----------------------------------------------------------------------------------------------------------------
		// Helpers
		//-----------------------------------------------------------------------------------------------------------------

		private static Set<Class<?>> classSet() {
			return new TreeSet<>(Comparator.comparing(Class::getName));
		}

		private static Set<Class<?>> classSet(Collection<Class<?>> copy) {
			return classSet(copy, false);
		}

		private static Set<Class<?>> classSet(Collection<Class<?>> copy, boolean nullIfEmpty) {
			if (copy == null || (nullIfEmpty && copy.isEmpty()))
				return null;
			Set<Class<?>> x = classSet();
			x.addAll(copy);
			return x;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final boolean
		beansRequireDefaultConstructor,
		beansRequireSerializable,
		beansRequireSettersForGetters,
		beansRequireSomeProperties,
		beanMapPutReturnsOldValue,
		useInterfaceProxies,
		ignoreUnknownBeanProperties,
		ignoreUnknownNullBeanProperties,
		ignoreUnknownEnumValues,
		ignoreMissingSetters,
		ignoreTransientFields,
		ignoreInvocationExceptionsOnGetters,
		ignoreInvocationExceptionsOnSetters,
		useJavaBeanIntrospector,
		useEnumNames,
		sortProperties,
		findFluentSetters;
	final Visibility
		beanConstructorVisibility,
		beanClassVisibility,
		beanMethodVisibility,
		beanFieldVisibility;
	final String typePropertyName;
	final Locale locale;
	final TimeZone timeZone;
	final MediaType mediaType;
	final Class<? extends PropertyNamer> propertyNamer;
	final List<Class<?>> beanDictionary, notBeanClasses;
	final List<Object> swaps;
	final List<String> notBeanPackages;
	final HashKey hashKey;

	final Map<Class,ClassMeta> cmCache;

	private final String[] notBeanPackageNames, notBeanPackagePrefixes;
	private final BeanRegistry beanRegistry;
	private final PropertyNamer propertyNamerBean;
	private final ObjectSwap[] swapArray;
	private final Class<?>[] notBeanClassesArray;
	private final ClassMeta<Object> cmObject;  // Reusable ClassMeta that represents general Objects.
	private final ClassMeta<String> cmString;  // Reusable ClassMeta that represents general Strings.
	private final ClassMeta<Class> cmClass;  // Reusable ClassMeta that represents general Classes.

	private final BeanSession defaultSession;
	private volatile WriterSerializer beanToStringSerializer;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public BeanContext(Builder builder) {
		super(builder);

		hashKey = builder.hashKey();

		beanConstructorVisibility = builder.beanConstructorVisibility;
		beanClassVisibility = builder.beanClassVisibility;
		beanMethodVisibility = builder.beanMethodVisibility;
		beanFieldVisibility = builder.beanFieldVisibility;
		beansRequireDefaultConstructor = builder.beansRequireDefaultConstructor;
		beansRequireSerializable = builder.beansRequireSerializable;
		beansRequireSettersForGetters = builder.beansRequireSettersForGetters;
		beansRequireSomeProperties = ! builder.disableBeansRequireSomeProperties;
		beanMapPutReturnsOldValue = builder.beanMapPutReturnsOldValue;
		useEnumNames = builder.useEnumNames;
		useInterfaceProxies = ! builder.disableInterfaceProxies;
		ignoreUnknownBeanProperties = builder.ignoreUnknownBeanProperties;
		ignoreUnknownNullBeanProperties = ! builder.disableIgnoreUnknownNullBeanProperties;
		ignoreUnknownEnumValues = builder.ignoreUnknownEnumValues;
		ignoreMissingSetters = ! builder.disableIgnoreMissingSetters;
		ignoreTransientFields = ! builder.disableIgnoreTransientFields;
		ignoreInvocationExceptionsOnGetters = builder.ignoreInvocationExceptionsOnGetters;
		ignoreInvocationExceptionsOnSetters = builder.ignoreInvocationExceptionsOnSetters;
		useJavaBeanIntrospector = builder.useJavaBeanIntrospector;
		sortProperties = builder.sortProperties;
		findFluentSetters = builder.findFluentSetters;
		typePropertyName = builder.typePropertyName != null ? builder.typePropertyName : "_type";
		locale = builder.locale != null ? builder.locale : Locale.getDefault();
		timeZone = builder.timeZone;
		mediaType = builder.mediaType;
		beanDictionary = optional(builder.beanDictionary).map(Collections::unmodifiableList).orElse(emptyList());
		swaps = optional(builder.swaps).map(Collections::unmodifiableList).orElse(emptyList());
		notBeanClasses = optional(builder.notBeanClasses).map(ArrayList::new).map(Collections::unmodifiableList).orElse(emptyList());
		notBeanPackages = optional(builder.notBeanPackages).map(ArrayList::new).map(Collections::unmodifiableList).orElse(emptyList());
		propertyNamer = builder.propertyNamer != null ? builder.propertyNamer : BasicPropertyNamer.class;

		notBeanClassesArray = notBeanClasses.isEmpty() ? DEFAULT_NOTBEAN_CLASSES : Stream.of(notBeanClasses, alist(DEFAULT_NOTBEAN_CLASSES)).flatMap(Collection::stream).toArray(Class[]::new);

		String[] _notBeanPackages = notBeanPackages.isEmpty() ? DEFAULT_NOTBEAN_PACKAGES : Stream.of(notBeanPackages, alist(DEFAULT_NOTBEAN_PACKAGES)).flatMap(Collection::stream).toArray(String[]::new);
		notBeanPackageNames = Stream.of(_notBeanPackages).filter(x -> ! x.endsWith(".*")).toArray(String[]::new);
		notBeanPackagePrefixes = Stream.of(_notBeanPackages).filter(x -> x.endsWith(".*")).map(x -> x.substring(0, x.length()-2)).toArray(String[]::new);

		try {
			propertyNamerBean = propertyNamer.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw asRuntimeException(e);
		}

		LinkedList<ObjectSwap<?,?>> _swaps = new LinkedList<>();
		swaps.forEach(x -> {
			if (x instanceof ObjectSwap) {
				_swaps.add((ObjectSwap<?,?>)x);
			} else {
				ClassInfo ci = ClassInfo.of((Class<?>)x);
				if (ci.isChildOf(ObjectSwap.class))
					_swaps.add(BeanCreator.of(ObjectSwap.class).type(ci).run());
				else if (ci.isChildOf(Surrogate.class))
					_swaps.addAll(SurrogateSwap.findObjectSwaps(ci.inner(), this));
				else
					throw new BasicRuntimeException("Invalid class {0} specified in BeanContext.swaps property.  Must be a subclass of ObjectSwap or Surrogate.", ci.inner());
			}
		});
		swapArray = _swaps.toArray(new ObjectSwap[_swaps.size()]);

		cmCache = new ConcurrentHashMap<>();
		cmCache.put(String.class, new ClassMeta(String.class, this, findObjectSwaps(String.class), findChildObjectSwaps(String.class)));
		cmCache.put(Object.class, new ClassMeta(Object.class, this, findObjectSwaps(Object.class), findChildObjectSwaps(Object.class)));
		cmString = cmCache.get(String.class);
		cmObject = cmCache.get(Object.class);
		cmClass = cmCache.get(Class.class);

		beanRegistry = new BeanRegistry(this, null);
		defaultSession = createSession().unmodifiable().build();
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public BeanSession.Builder createSession() {
		return BeanSession.create(this);
	}

	@Override /* Context */
	public BeanSession getSession() {
		return defaultSession;
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
	 * Wraps an object inside a {@link BeanMap} object (a modifiable {@link Map}).
	 *
	 * <p>
	 * This is a shortcut for the following code:  <c>createSession().build().toBeanMap(<jv>object</jv>);</c>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Construct a bean map around a bean instance</jc>
	 * 	BeanMap&lt;Person&gt; <jv>beanMap</jv> = BeanContext.<jsf>DEFAULT</jsf>.toBeanMap(<jk>new</jk> Person());
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param object The object to wrap in a map interface.  Must not be null.
	 * @return The wrapped object.
	 * @see BeanSession#toBeanMap(Object)
	 */
	public <T> BeanMap<T> toBeanMap(T object) {
		return defaultSession.toBeanMap(object);
	}

	/**
	 * Creates a new {@link BeanMap} object (a modifiable {@link Map}) of the given class with uninitialized
	 * property values.
	 *
	 * <p>
	 * This is a shortcut for the following code:  <c>createSession().build().newBeanMap(<jv>_class</jv>);</c>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Construct a new bean map wrapped around a new Person object</jc>
	 * 	BeanMap&lt;Person&gt; <jv>beanMap</jv> = BeanContext.<jsf>DEFAULT</jsf>.newBeanMap(Person.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param c The name of the class to create a new instance of.
	 * @return A new instance of the class.
	 * @see BeanSession#newBeanMap(Class)
	 */
	public <T> BeanMap<T> newBeanMap(Class<T> c) {
		return defaultSession.newBeanMap(c);
	}

	/**
	 * Converts the specified value to the specified class type.
	 *
	 * <p>
	 * This is a shortcut for the following code:  <c>createSession().build().convertToType(<jv>value</jv>, <jv>type</jv>);</c>
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 * @see BeanSession#convertToType(Object, Class)
	 */
	public final <T> T convertToType(Object value, Class<T> type) throws InvalidDataConversionException {
		return defaultSession.convertToType(value, type);
	}

	/**
	 * Same as {@link #convertToType(Object, Class)}, except used for instantiating inner member classes that must
	 * be instantiated within another class instance.
	 *
	 * <p>
	 * This is a shortcut for the following code:  <c>createSession().build().convertToMemberType(<jv>outer</jv>, <jv>value</jv>, <jv>type</jv>);</c>
	 *
	 * @param <T> The class type to convert the value to.
	 * @param outer
	 * 	If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 * @see BeanSession#convertToMemberType(Object, Object, Class)
	 */
	public final <T> T convertToMemberType(Object outer, Object value, Class<T> type) throws InvalidDataConversionException {
		return defaultSession.convertToMemberType(outer, value, getClassMeta(type));
	}

	/**
	 * Same as {@link #convertToType(Object, Class)}, but allows for complex data types consisting of collections or maps.
	 *
	 * <p>
	 * This is a shortcut for the following code:  <c>createSession().build().convertToType(<jv>value</jv>, <jv>type</jv>, <jv>args</jv>);</c>
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to be converted.
	 * @param type The target object type.
	 * @param args The target object parameter types.
	 * @return The converted type.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @see BeanSession#convertToType(Object, Type, Type...)
	 */
	public final <T> T convertToType(Object value, Type type, Type...args) throws InvalidDataConversionException {
		return (T)defaultSession.convertToMemberType(null, value, getClassMeta(type, args));
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
		for (Class exclude : notBeanClassesArray)
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
					cm = new ClassMeta<>(type, this, findObjectSwaps(type), findChildObjectSwaps(type));
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
	 * @param <T>
	 * 	The class to resolve.
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
			// Need to re-resolve it to pick up ObjectSwaps and stuff on this context.
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
				if (params.length != 2 || (params[0].isObject() && params[1].isObject()))
					return rawType;
				return new ClassMeta(rawType, params[0], params[1], null);
			}
			if (rawType.isCollection() || rawType.isOptional()) {
				if (params.length != 1 || params[0].isObject())
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

			if (isNotVoid(p.type()))
				cm2 = resolveClassMeta(p.type(), typeVarImpls);

			if (cm2.isMap()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class, Object.class} : p.params());
				if (pParams.length != 2)
					throw new BasicRuntimeException("Invalid number of parameters specified for Map (must be 2): {0}", pParams.length);
				ClassMeta<?> keyType = resolveType(pParams[0], cm2.getKeyType(), cm.getKeyType());
				ClassMeta<?> valueType = resolveType(pParams[1], cm2.getValueType(), cm.getValueType());
				if (keyType.isObject() && valueType.isObject())
					return cm2;
				return new ClassMeta<>(cm2, keyType, valueType, null);
			}

			if (cm2.isCollection() || cm2.isOptional()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class} : p.params());
				if (pParams.length != 1)
					throw new BasicRuntimeException("Invalid number of parameters specified for {1} (must be 1): {0}", pParams.length, (cm2.isCollection() ? "Collection" : cm2.isOptional() ? "Optional" : "Array"));
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
	 * Returns the {@link ObjectSwap} associated with the specified class, or <jk>null</jk> if there is no POJO swap
	 * associated with the class.
	 *
	 * @param <T> The class associated with the swap.
	 * @param c The class associated with the swap.
	 * @return The swap associated with the class, or null if there is no association.
	 */
	private final <T> ObjectSwap[] findObjectSwaps(Class<T> c) {
		// Note:  On first
		if (c != null) {
			List<ObjectSwap> l = list();
			for (ObjectSwap f : swapArray)
				if (f.getNormalClass().isParentOf(c))
					l.add(f);
			return l.size() == 0 ? null : l.toArray(new ObjectSwap[l.size()]);
		}
		return null;
	}

	/**
	 * Checks whether a class has a {@link ObjectSwap} associated with it in this bean context.
	 *
	 * @param c The class to check.
	 * @return <jk>true</jk> if the specified class or one of its subclasses has a {@link ObjectSwap} associated with it.
	 */
	private final ObjectSwap[] findChildObjectSwaps(Class<?> c) {
		if (c == null || swapArray.length == 0)
			return null;
		List<ObjectSwap> l = null;
		for (ObjectSwap f : swapArray) {
			if (f.getNormalClass().isChildOf(c)) {
				if (l == null)
					l = list();
				l.add(f);
			}
		}
		return l == null ? null : l.toArray(new ObjectSwap[l.size()]);
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
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Minimum bean class visibility.
	 *
	 * @see BeanContext.Builder#beanClassVisibility(Visibility)
	 * @return
	 * 	Classes are not considered beans unless they meet the minimum visibility requirements.
	 */
	public final Visibility getBeanClassVisibility() {
		return beanClassVisibility;
	}

	/**
	 * Minimum bean constructor visibility.
	 *
	 * @see BeanContext.Builder#beanConstructorVisibility(Visibility)
	 * @return
	 * 	Only look for constructors with this specified minimum visibility.
	 */
	public final Visibility getBeanConstructorVisibility() {
		return beanConstructorVisibility;
	}

	/**
	 * Bean dictionary.
	 *
	 * @see BeanContext.Builder#beanDictionary()
	 * @return
	 * 	The list of classes that make up the bean dictionary in this bean context.
	 */
	public final List<Class<?>> getBeanDictionary() {
		return beanDictionary;
	}

	/**
	 * Minimum bean field visibility.
	 *
	 *
	 * @see BeanContext.Builder#beanFieldVisibility(Visibility)
	 * @return
	 * 	Only look for bean fields with this specified minimum visibility.
	 */
	public final Visibility getBeanFieldVisibility() {
		return beanFieldVisibility;
	}

	/**
	 * BeanMap.put() returns old property value.
	 *
	 * @see BeanContext.Builder#beanMapPutReturnsOldValue()
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
	 * @see BeanContext.Builder#beanMethodVisibility(Visibility)
	 * @return
	 * 	Only look for bean methods with this specified minimum visibility.
	 */
	public final Visibility getBeanMethodVisibility() {
		return beanMethodVisibility;
	}

	/**
	 * Beans require no-arg constructors.
	 *
	 * @see BeanContext.Builder#beansRequireDefaultConstructor()
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
	 * @see BeanContext.Builder#beansRequireSerializable()
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
	 * @see BeanContext.Builder#beansRequireSettersForGetters()
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
	 * @see BeanContext.Builder#disableBeansRequireSomeProperties()
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
	 * @see BeanContext.Builder#typePropertyName(String)
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
	 * @see BeanContext.Builder#findFluentSetters()
	 * @return
	 * 	<jk>true</jk> if fluent setters are detected on beans.
	 */
	public final boolean isFindFluentSetters() {
		return findFluentSetters;
	}

	/**
	 * Ignore invocation errors on getters.
	 *
	 * @see BeanContext.Builder#ignoreInvocationExceptionsOnGetters()
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean getter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnGetters() {
		return ignoreInvocationExceptionsOnGetters;
	}

	/**
	 * Ignore invocation errors on setters.
	 *
	 * @see BeanContext.Builder#ignoreInvocationExceptionsOnSetters()
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean setter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnSetters() {
		return ignoreInvocationExceptionsOnSetters;
	}

	/**
	 * Silently ignore missing setters.
	 *
	 * @see BeanContext.Builder#disableIgnoreMissingSetters()
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a bean property without a setter should throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreMissingSetters() {
		return ignoreMissingSetters;
	}

	/**
	 * Ignore transient fields.
	 *
	 * @see BeanContext.Builder#disableIgnoreTransientFields()
	 * @return
	 * 	<jk>true</jk> if fields and methods marked as transient should not be ignored.
	 */
	protected final boolean isIgnoreTransientFields() {
		return ignoreTransientFields;
	}

	/**
	 * Ignore unknown properties.
	 *
	 * @see BeanContext.Builder#ignoreUnknownBeanProperties()
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a non-existent bean property is silently ignored.
	 * 	<br>Otherwise, a {@code RuntimeException} is thrown.
	 */
	public final boolean isIgnoreUnknownBeanProperties() {
		return ignoreUnknownBeanProperties;
	}

	/**
	 * Ignore unknown enum values.
	 *
	 * @see BeanContext.Builder#ignoreUnknownEnumValues()
	 * @return
	 * 	<jk>true</jk> if unknown enum values should be set as <jk>null</jk> instead of throwing an exception.
	 */
	public final boolean isIgnoreUnknownEnumValues() {
		return ignoreUnknownEnumValues;
	}

	/**
	 * Ignore unknown properties with null values.
	 *
	 * @see BeanContext.Builder#disableIgnoreUnknownNullBeanProperties()
	 * @return
	 * 	<jk>true</jk> if trying to set a <jk>null</jk> value on a non-existent bean property should throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreUnknownNullBeanProperties() {
		return ignoreUnknownNullBeanProperties;
	}

	/**
	 * Bean class exclusions.
	 *
	 * @see BeanContext.Builder#notBeanClasses(Class...)
	 * @return
	 * 	The list of classes that are explicitly not beans.
	 */
	protected final Class<?>[] getNotBeanClasses() {
		return notBeanClassesArray;
	}

	/**
	 * Bean package exclusions.
	 *
	 * @see BeanContext.Builder#notBeanPackages(String...)
	 * @return
	 * 	The list of fully-qualified package names to exclude from being classified as beans.
	 */
	public final String[] getNotBeanPackagesNames() {
		return notBeanPackageNames;
	}

	/**
	 * Bean package exclusions.
	 *
	 * @see BeanContext.Builder#notBeanPackages(String...)
	 * @return
	 * 	The list of package name prefixes to exclude from being classified as beans.
	 */
	protected final String[] getNotBeanPackagesPrefixes() {
		return notBeanPackagePrefixes;
	}

	/**
	 * Java object swaps.
	 *
	 * @see BeanContext.Builder#swaps(Class...)
	 * @return
	 * 	The list POJO swaps defined.
	 */
	public final ObjectSwap<?,?>[] getSwaps() {
		return swapArray;
	}

	/**
	 * Bean property namer.
	 *
	 * @see BeanContext.Builder#propertyNamer(Class)
	 * @return
	 * 	The interface used to calculate bean property names.
	 */
	public final PropertyNamer getPropertyNamer() {
		return propertyNamerBean;
	}

	/**
	 * Sort bean properties.
	 *
	 * @see BeanContext.Builder#sortProperties()
	 * @return
	 * 	<jk>true</jk> if all bean properties will be serialized and access in alphabetical order.
	 */
	public final boolean isSortProperties() {
		return sortProperties;
	}

	/**
	 * Use enum names.
	 *
	 * @see BeanContext.Builder#useEnumNames()
	 * @return
	 * 	<jk>true</jk> if enums are always serialized by name, not using {@link Object#toString()}.
	 */
	public final boolean isUseEnumNames() {
		return useEnumNames;
	}

	/**
	 * Use interface proxies.
	 *
	 * @see BeanContext.Builder#disableInterfaceProxies()
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
	 * @see BeanContext.Builder#useJavaBeanIntrospector()
	 * @return
	 * 	<jk>true</jk> if the built-in Java bean introspector should be used for bean introspection.
	 */
	public final boolean isUseJavaBeanIntrospector() {
		return useJavaBeanIntrospector;
	}

	/**
	 * Locale.
	 *
	 * @see BeanContext.Builder#locale(Locale)
	 * @return
	 * 	The default locale for serializer and parser sessions.
	 */
	public final Locale getDefaultLocale() {
		return locale;
	}

	/**
	 * Media type.
	 *
	 * @see BeanContext.Builder#mediaType(MediaType)
	 * @return
	 * 	The default media type value for serializer and parser sessions.
	 */
	public final MediaType getDefaultMediaType() {
		return mediaType;
	}

	/**
	 * Time zone.
	 *
	 * @see BeanContext.Builder#timeZone(TimeZone)
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
			this.beanToStringSerializer = JsonSerializer.create().beanContext(this).sq().simpleAttrs().build();
		}
		return beanToStringSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap()
			.append("id", System.identityHashCode(this))
			.append("beanClassVisibility", beanClassVisibility)
			.append("beanConstructorVisibility", beanConstructorVisibility)
			.append("beanDictionary", beanDictionary)
			.append("beanFieldVisibility", beanFieldVisibility)
			.append("beanMethodVisibility", beanMethodVisibility)
			.append("beansRequireDefaultConstructor", beansRequireDefaultConstructor)
			.append("beansRequireSerializable", beansRequireSerializable)
			.append("beansRequireSettersForGetters", beansRequireSettersForGetters)
			.append("beansRequireSomeProperties", beansRequireSomeProperties)
			.append("ignoreTransientFields", ignoreTransientFields)
			.append("ignoreInvocationExceptionsOnGetters", ignoreInvocationExceptionsOnGetters)
			.append("ignoreInvocationExceptionsOnSetters", ignoreInvocationExceptionsOnSetters)
			.append("ignoreUnknownBeanProperties", ignoreUnknownBeanProperties)
			.append("ignoreUnknownNullBeanProperties", ignoreUnknownNullBeanProperties)
			.append("notBeanClasses", notBeanClasses)
			.append("notBeanPackageNames", notBeanPackageNames)
			.append("notBeanPackagePrefixes", notBeanPackagePrefixes)
			.append("swaps", swaps)
			.append("sortProperties", sortProperties)
			.append("useEnumNames", useEnumNames)
			.append("useInterfaceProxies", useInterfaceProxies)
			.append("useJavaBeanIntrospector", useJavaBeanIntrospector);
	}
}