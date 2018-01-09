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
import static org.apache.juneau.internal.StringUtils.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Core class of the Juneau architecture.
 *
 * <p>
 * This class servers multiple purposes:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Provides the ability to wrap beans inside {@link Map} interfaces.
 * 	<li>
 * 		Serves as a repository for metadata on POJOs, such as associated {@link BeanFilter beanFilters},
 * 		{@link PropertyNamer propertyNamers}, etc...  which are used to tailor how POJOs are serialized and parsed.
 * </ul>
 *
 * <p>
 * All serializers and parsers extend from this context so that they can handle POJOs using a common framework.
 *
 * <h5 class='topic'>Bean Contexts</h5>
 *
 * Bean contexts are created through the {@link BeanContext#create()} and {@link BeanContextBuilder#build()} methods.
 * These context objects are read-only, reusable, and thread-safe.
 *
 * <p>
 * Each bean context maintains a cache of {@link ClassMeta} objects that describe information about classes encountered.
 * These <code>ClassMeta</code> objects are time-consuming to construct.
 * Therefore, instances of {@link BeanContext} that share the same <js>"BeanContext.*"</js> property values share
 * the same cache.  This allows for efficient reuse of <code>ClassMeta</code> objects so that the information about
 * classes only needs to be calculated once.
 * Because of this, many of the properties defined on the {@link BeanContext} class cannot be overridden on the session.
 *
 * <h5 class='topic'>Bean Sessions</h5>
 *
 * Whereas <code>BeanContext</code> objects are permanent, unchangeable, cached, and thread-safe,
 * {@link BeanSession} objects are ephemeral and not thread-safe.
 * They are meant to be used as quickly-constructed scratchpads for creating bean maps.
 * {@link BeanMap} objects can only be created through the session.
 *
 * <h5 class='topic'>BeanContext configuration properties</h5>
 *
 * <code>BeanContexts</code> have several configuration properties that can be used to tweak behavior on how beans are
 * handled.  These are denoted as the static <jsf>BEAN_*</jsf> fields on this class.
 *
 * <p>
 * Some settings (e.g. {@link #BEAN_beansRequireDefaultConstructor}) are used to differentiate between bean
 * and non-bean classes.
 * Attempting to create a bean map around one of these objects will throw a {@link BeanRuntimeException}.
 * The purpose for this behavior is so that the serializers can identify these non-bean classes and convert them to
 * plain strings using the {@link Object#toString()} method.
 *
 * <p>
 * Some settings (e.g. {@link #BEAN_beanFieldVisibility}) are used to determine what kinds of properties are
 * detected on beans.
 *
 * <p>
 * Some settings (e.g. {@link #BEAN_beanMapPutReturnsOldValue}) change the runtime behavior of bean maps.
 *
 * <p>
 * Settings are specified using the {@link BeanContextBuilder#set(String, Object)} method and related convenience
 * methods.
 *
 * <h5 class='section'>Example:</h5>
 *
 * <p class='bcode'>
 * 	<jc>// Construct a context from scratch.</jc>
 * 	BeanContext beanContext = BeanContext.<jsm>create</jsm>()
 * 		.set(BeanContext.<jsf>BEAN_beansRequireDefaultConstructor</jsf>, <jk>true</jk>)
 * 		.notBeanClasses(Foo.<jk>class</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='topic'>Bean Maps</h5>
 *
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
 * 	BeanSession session = BeanContext.<jsf>DEFAULT</jsf>.createSession();
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
 *
 * This package contains annotations that can be applied to class definitions to override what properties are detected
 * on a bean.
 *
 * <h5 class='section'>Example:</h5>
 *
 * <p class='bcode'>
 * 	<jc>// Bean class definition where only property 'name' is detected.</jc>
 * 	<ja>&#64;Bean</ja>(properties=<js>"name"</js>)
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> String getName();
 * 		<jk>public void</jk> setName(String name);
 * 		<jk>public int</jk> getAge();
 * 		<jk>public void</jk> setAge(<jk>int</jk> age);
 * 	}
 * </p>
 *
 * <p>
 * See {@link Bean @Bean} and {@link BeanProperty @BeanProperty} for more information.
 *
 * <h5 class='topic'>Beans with read-only properties</h5>
 *
 * Bean maps can also be defined on top of beans with read-only properties by adding a
 * {@link BeanConstructor @BeanConstructor} annotation to one of the constructors on the
 * bean class.  This will allow read-only properties to be set through constructor arguments.
 *
 * <p>
 * When the <code>@BeanConstructor</code> annotation is present, bean instantiation is delayed until the call to
 * {@link BeanMap#getBean()}.
 * Until then, bean property values are stored in a local cache until <code>getBean()</code> is called.
 * Because of this additional caching step, parsing into read-only beans tends to be slower and use more memory than
 * parsing into beans with writable properties.
 *
 * <p>
 * Attempting to call {@link BeanMap#put(String,Object)} on a read-only property after calling {@link BeanMap#getBean()}
 * will result in a {@link BeanRuntimeException} being thrown.
 * Multiple calls to {@link BeanMap#getBean()} will return the same bean instance.
 *
 * <p>
 * Beans can be defined with a combination of read-only and read-write properties.
 *
 * <p>
 * See {@link BeanConstructor @BeanConstructor} for more information.
 *
 * <h5 class='topic'>ClassMetas</h5>
 *
 * The {@link ClassMeta} class is a wrapper around {@link Class} object that provides cached information about that
 * class (e.g. whether it's a {@link Map} or {@link Collection} or bean).
 *
 * <p>
 * As a general rule, it's best to reuse bean contexts (and therefore serializers and parsers too) whenever possible
 * since it takes some time to populate the internal {@code ClassMeta} object cache.
 * By reusing bean contexts, the class type metadata only needs to be calculated once which significantly improves
 * performance.
 *
 * <p>
 * See {@link ClassMeta} for more information.
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BeanContext extends Context {

	static final String PREFIX = "BeanContext.";

	/**
	 * Configuration property:  Minimum bean class visibility.
	 * 
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanClassVisibility.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> ({@link Visibility})
	 * 	<li><b>Default:</b>  <js>"PUBLIC"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beanClassVisibility(Visibility)}
	 * 		</ul>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 * 
	 * <p>
	 * For example, if the visibility is <code>PUBLIC</code> and the bean class is <jk>protected</jk>, then the class
	 * will not be interpreted as a bean class and be serialized as a string.
	 * <br>Use this setting to reduce the visibility requirement.
	 * 
	 *	<h5 class='section'>Example:</h5>
	 *	<p class='bcode'>
	 * 	<jc>// Create a serializer that serializes protected classes.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanClassVisibility(<jsf>PROTECTED</jsf>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beanClassVisibility</jsf>, <js>"PROTECTED"</js>)
	 * 		.build();
	 *	</p>
	 */
	public static final String BEAN_beanClassVisibility = PREFIX + "beanClassVisibility.s";

	/**
	 * Configuration property:  Minimum bean constructor visibility.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanConstructorVisibility.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> ({@link Visibility})
	 * 	<li><b>Default:</b>  <js>"PUBLIC"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beanConstructorVisibility(Visibility)}
	 * 		</ul>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Only look for constructors with the specified minimum visibility.
	 * 
	 * <p>
	 * This setting affects the logic for finding no-arg constructors for bean.  
	 * <br>Normally, only <jk>public</jk> no-arg constructors are used.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 * 
	 *	<h5 class='section'>Example:</h5>
	 *	<p class='bcode'>
	 * 	<jc>// Create a serializer that looks for protected no-arg constructors.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanConstructorVisibility(<jsf>PROTECTED</jsf>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beanConstructorVisibility</jsf>, <js>"PROTECTED"</js>)
	 * 		.build();
	 *	</p>
	 */
	public static final String BEAN_beanConstructorVisibility = PREFIX + "beanConstructorVisibility.s";

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanDictionary.lc"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b> 
	 * 		<ul>
	 * 			<li class='ja'>{@link Bean#beanDictionary()} 
	 * 			<li class='ja'>{@link BeanProperty#beanDictionary()} 
	 * 		</ul>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beanDictionary(Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#beanDictionary(Class...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#beanDictionary(boolean,Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#beanDictionaryRemove(Object...)}
	 * 			<li class='jm'>{@link BeanFilterBuilder#beanDictionary(Class...)}
	 * 			<li class='jm'>{@link BeanFilterBuilder#beanDictionary(boolean,Class...)}
	 * 		</ul>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * The list of classes that make up the bean dictionary in this bean context.
	 * 
	 * <p>
	 * A dictionary is a name/class mapping used to find class types during parsing when they cannot be inferred
	 * through reflection.
	 * <br>The names are defined through the {@link Bean#typeName()} annotation defined on the bean class.
	 * <br>For example, if a class <code>Foo</code> has a type-name of <js>"myfoo"</js>, then it would end up serialized
	 * as <js>"{_type:'myfoo',...}"</js>.
	 * 
	 * <p>
	 * This setting tells the parsers which classes to look for when resolving <js>"_type"</js> attributes.
	 * 
	 * <p>
	 * Values can consist of any of the following types:
	 *	<ul>
	 * 	<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean.typeName()}.
	 * 	<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
	 * 	<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
	 * 	<li>Any array or collection of the objects above.
	 * </ul>
	 *
	 *	<h5 class='section'>Example:</h5>
	 *	<p class='bcode'>
	 * 	<jc>// Create a parser and tell it which classes to try to resolve.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanDictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_beanDictionary</jsf>, Foo.<jk>class</jk>)
	 * 		.addTo(<jsf>BEAN_beanDictionary</jsf>, Bar.<jk>class</jk>)
	 * 		.build();
	 * 
	 * 	<jc>// Instead of by parser, define a bean dictionary on a class through an annotation.</jc>
	 * 	<jc>// This applies to all properties on this class and all subclasses.</jc>
	 * 	<ja>@Bean</ja>(beanDictionary={Foo.<jk>class</jk>,Bar.<jk>class</jk>})
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 * 
	 *	<h5 class='section'>Documentation:</h5>
	 *	<ul>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.BeanDictionaries">Overview &gt; Bean Names and Dictionaries</a>
	 *	</ul>
	 */
	public static final String BEAN_beanDictionary = PREFIX + "beanDictionary.lc";

	/**
	 * Configuration property:  Add to bean dictionary.
	 */
	public static final String BEAN_beanDictionary_add = PREFIX + "beanDictionary.lc/add";

	/**
	 * Configuration property:  Remove from bean dictionary.
	 */
	public static final String BEAN_beanDictionary_remove = PREFIX + "beanDictionary.lc/remove";

	/**
	 * Configuration property:  Minimum bean field visibility.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanFieldVisibility.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> ({@link Visibility})
	 * 	<li><b>Default:</b>  <js>"PUBLIC"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beanFieldVisibility(Visibility)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Only look for bean fields with the specified minimum visibility.
	 * 
	 * <p>
	 * This affects which fields on a bean class are considered bean properties.
	 * <br>Normally only <jk>public</jk> fields are considered.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 * 
	 *	<h5 class='section'>Example:</h5>
	 *	<p class='bcode'>
	 * 	<jc>// Create a serializer that looks for protected fields.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFieldVisibility(<jsf>PROTECTED</jsf>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beanFieldVisibility</jsf>, <js>"PROTECTED"</js>)
	 * 		.build();
	 * 
	 * 	<jc>// Disable using fields as properties entirely.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFieldVisibility(<jsf>NONE</jsf>)
	 * 		.build();
	 *	</p>
	 */
	public static final String BEAN_beanFieldVisibility = PREFIX + "beanFieldVisibility.s";

	/**
	 * Configuration property:  Bean filters.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanFilters.lc"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b> 
	 * 		<ul>
	 * 			<li class='ja'>{@link Bean} 
	 * 		</ul>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beanFilters(Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#beanFilters(Class...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#beanFilters(boolean,Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#beanFiltersRemove(Object...)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * This is a programmatic equivalent to the {@link Bean @Bean} annotation.
	 * <br>It's useful when you want to use the Bean annotation functionality, but you don't have the ability to alter 
	 * the bean classes.
	 *
	 * <p>
	 * Values can consist of any of the following types:
	 * <ul class='spaced-list'>
	 * 	<li>Any subclass of {@link BeanFilterBuilder}.
	 * 		<br>These must have a public no-arg constructor.
	 * 	<li>Any bean interfaces.
	 * 		<br>A shortcut for defining a {@link InterfaceBeanFilterBuilder}.
	 * 		<br>Any subclasses of an interface class will only have properties defined on the interface.
	 * 		All other bean properties will be ignored.
	 * 	<li>Any array or collection of the objects above.
	 * </ul>
	 * 
	 *	<h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a bean filter for our class.</jc>
	 * 	<jk>public class</jk> MyBeanFilter <jk>extends</jk> BeanFilterBuilder {
	 * 		<jc>// Must provide a no-arg constructor!</jc>
	 * 		<jk>public</jk> MyBeanFilter() {
	 * 			<jk>super</jk>(MyBean.<jk>class</jk>);  <jc>// The bean class that this filter applies to.</jc>
	 * 			properties(<js>"foo,bar,baz"</js>);  <jc>// The properties we want exposed.</jc>
	 * 		}
	 * 	}	
	 * 
	 * 	<jc>// Associate our bean filter with a serializer.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyBeanFilter.<jk>class</jk>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_beanFilters</jsf>, MyBeanFilter.<jk>class</jk>)
	 * 		.build();
	 * </p>		
	 * 
	 *	<h5 class='section'>Documentation:</h5>
	 *	<ul>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.BeanAnnotation">Overview &gt; @Bean Annotation</a>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.BeanFilters">Overview &gt; BeanFilters</a>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.StopClasses">Overview &gt; Stop Classes</a>
	 *	</ul>
	 */
	public static final String BEAN_beanFilters = PREFIX + "beanFilters.lc";

	/**
	 * Configuration property:  Add to bean filters.
	 */
	public static final String BEAN_beanFilters_add = PREFIX + "beanFilters.lc/add";

	/**
	 * Configuration property:  Remove from bean filters.
	 */
	public static final String BEAN_beanFilters_remove = PREFIX + "beanFilters.lc/remove";

	/**
	 * Configuration property:  {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
	 * value.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanMapPutReturnsOldValue.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beanMapPutReturnsOldValue(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
	 * values.
	 *
	 * <p>
	 * Disabled by default because it introduces a slight performance penalty.
	 */
	public static final String BEAN_beanMapPutReturnsOldValue = PREFIX + "beanMapPutReturnsOldValue.b";

	/**
	 * Configuration property:  Minimum bean method visibility.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanMethodVisibility.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> ({@link Visibility})
	 * 	<li><b>Default:</b>  <js>"PUBLIC"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beanMethodVisibility(Visibility)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Only look for bean methods with the specified minimum visibility.
	 * 
	 * <p>
	 * This affects which methods are detected as getters and setters on a bean class.
	 * <br>Normally only <jk>public</jk> getters and setters are considered.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 * 
	 *	<h5 class='section'>Example:</h5>
	 *	<p class='bcode'>
	 * 	<jc>// Create a serializer that looks for protected getters and setters.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanMethodVisibility(<jsf>PROTECTED</jsf>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beanMethodVisibility</jsf>, <js>"PROTECTED"</js>)
	 * 		.build();
	 *	</p>
	 */
	public static final String BEAN_beanMethodVisibility = PREFIX + "beanMethodVisibility.s";

	/**
	 * Configuration property:  Beans require no-arg constructors.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beansRequireDefaultConstructor.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beansRequireDefaultConstructor(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, a Java class must implement a default no-arg constructor to be considered a bean.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 */
	public static final String BEAN_beansRequireDefaultConstructor = PREFIX + "beansRequireDefaultConstructor.b";

	/**
	 * Configuration property:  Beans require {@link Serializable} interface.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beansRequireSerializable.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beansRequireSerializable(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 */
	public static final String BEAN_beansRequireSerializable = PREFIX + "beansRequireSerializable.b";

	/**
	 * Configuration property:  Beans require setters for getters.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beansRequireSettersForGetters.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beansRequireSettersForGetters(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, only getters that have equivalent setters will be considered as properties on a bean.
	 * Otherwise, they will be ignored.
	 */
	public static final String BEAN_beansRequireSettersForGetters = PREFIX + "beansRequireSettersForGetters.b";

	/**
	 * Configuration property:  Beans require at least one property.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beansRequireSomeProperties.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beansRequireSomeProperties(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then a Java class must contain at least 1 property to be considered a bean.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 */
	public static final String BEAN_beansRequireSomeProperties = PREFIX + "beansRequireSomeProperties.b";

	/**
	 * Configuration property:  Name to use for the bean type properties used to represent a bean type.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanTypePropertyName.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"_type"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b> 
	 * 		<ul>
	 * 			<li class='ja'>{@link Bean#typePropertyName()}
	 * 		</ul>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#beanTypePropertyName(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_beanTypePropertyName = PREFIX + "beanTypePropertyName.s";

	/**
	 * Configuration property:  Debug mode.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.debug.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#debug()}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>
	 * 		Enables {@link Serializer#SERIALIZER_detectRecursions}.
	 * </ul>
	 *
	 * <p>
	 * Enables the following additional information during parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean setters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * </ul>
	 */
	public static final String BEAN_debug = PREFIX + "debug.b";

	/**
	 * Configuration property:  Exclude specified properties from beans.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.excludeProperties.sms"</js>
	 * 	<li><b>Data type:</b>  <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b>  <code>{}</code>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b> 
	 * 		<ul>
	 * 			<li class='ja'>{@link Bean#excludeProperties()} 
	 * 		</ul>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#excludeProperties(Class, String)}
	 * 			<li class='jm'>{@link BeanContextBuilder#excludeProperties(String, String)}
	 * 			<li class='jm'>{@link BeanContextBuilder#excludeProperties(Map)}
	 * 			<li class='jm'>{@link BeanFilterBuilder#excludeProperties(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies to exclude the specified list of properties for the specified bean class.
	 *
	 * <p>
	 * The keys are either fully-qualified or simple class names, and the values are comma-delimited lists of property
	 * names.
	 * The key <js>"*"</js> means all bean classes.
	 *
	 * <p>
	 * For example, <code>{Bean1:<js>'foo,bar'</js>}</code> means don't serialize the <code>foo</code> and
	 * <code>bar</code> properties on the specified bean.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 */
	public static final String BEAN_excludeProperties = PREFIX + "excludeProperties.sms";

	/**
	 * Configuration property:  Ignore invocation errors on getters.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignoreInvocationExceptionsOnGetters.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#ignoreInvocationExceptionsOnGetters(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean getter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnGetters = PREFIX + "ignoreInvocationExceptionsOnGetters.b";

	/**
	 * Configuration property:  Ignore invocation errors on setters.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignoreInvocationExceptionsOnSetters.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#ignoreInvocationExceptionsOnSetters(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean setter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnSetters = PREFIX + "ignoreInvocationExceptionsOnSetters.b";

	/**
	 * Configuration property:  Ignore properties without setters.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignorePropertiesWithoutSetters.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#ignorePropertiesWithoutSetters(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a bean property without a setter will silently be ignored.
	 * Otherwise, a {@code RuntimeException} is thrown.
	 */
	public static final String BEAN_ignorePropertiesWithoutSetters = PREFIX + "ignorePropertiesWithoutSetters.b";

	/**
	 * Configuration property:  Ignore unknown properties.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignoreUnknownBeanProperties.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#ignoreUnknownBeanProperties(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code RuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreUnknownBeanProperties = PREFIX + "ignoreUnknownBeanProperties.b";

	/**
	 * Configuration property:  Ignore unknown properties with null values.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignoreUnknownNullBeanProperties.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#ignoreUnknownNullBeanProperties(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, trying to set a <jk>null</jk> value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code RuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreUnknownNullBeanProperties = PREFIX + "ignoreUnknownNullBeanProperties.b";

	/**
	 * Configuration property:  Implementation classes for interfaces and abstract classes.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.implClasses.smc"</js>
	 * 	<li><b>Data type:</b>  <code>Map&lt;String,Class&gt;</code>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#implClasses(Map)}
	 * 			<li class='jm'>{@link BeanContextBuilder#implClass(Class, Class)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation class for the
	 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
	 * parse).
	 */
	public static final String BEAN_implClasses = PREFIX + "implClasses.smc";

	/**
	 * Configuration property:  Explicitly specify visible bean properties.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.includeProperties.sms"</js>
	 * 	<li><b>Data type:</b>  <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b>  <code>{}</code>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b> 
	 * 		<ul>
	 * 			<li class='ja'>{@link Bean#properties()} 
	 * 			<li class='ja'>{@link BeanProperty#properties()} 
	 * 		</ul>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#includeProperties(Class, String)}
	 * 			<li class='jm'>{@link BeanContextBuilder#includeProperties(String, String)}
	 * 			<li class='jm'>{@link BeanContextBuilder#includeProperties(Map)}
	 * 			<li class='jm'>{@link BeanFilterBuilder#properties(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies to only include the specified list of properties for the specified bean classes.
	 *
	 * <p>
	 * The keys are either fully-qualified or simple class names, and the values are comma-delimited lists of property
	 *	names.
	 * The key <js>"*"</js> means all bean classes.
	 *
	 * <p>
	 * For example, <code>{Bean1:<js>'foo,bar'</js>}</code> means only serialize the <code>foo</code> and
	 * <code>bar</code> properties on the specified bean.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 */
	public static final String BEAN_includeProperties = PREFIX + "properties.sms";

	/**
	 * Configuration property:  Locale.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.locale.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> ({@link Locale})
	 * 	<li><b>Default:</b>  <jk>null</jk> (defaults to {@link Locale#getDefault()})
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#locale(Locale)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEAN_locale = PREFIX + "locale.s";

	/**
	 * Configuration property:  Media type.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.mediaType.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> ({@link MediaType})
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#mediaType(MediaType)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies a default media type value for serializer and parser sessions.
	 */
	public static final String BEAN_mediaType = PREFIX + "mediaType.s";

	/**
	 * Configuration property:  Classes to be excluded from consideration as being beans.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.notBeanClasses.sc"</js>
	 * 	<li><b>Data type:</b>  <code>Set&lt;Class&gt;</code>
	 * 	<li><b>Default:</b>  empty set
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b> 
	 * 		<ul>
	 * 			<li class='ja'>{@link BeanIgnore} 
	 * 		</ul>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#notBeanClasses(Class...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#notBeanClasses(Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#notBeanClasses(boolean, Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#notBeanClassesRemove(Object...)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Not-bean classes are typically converted to <code>Strings</code> during serialization even if they appear to be
	 * bean-like.
	 */
	public static final String BEAN_notBeanClasses = PREFIX + "notBeanClasses.sc";

	/**
	 * Configuration property:  Add to classes that should not be considered beans.
	 */
	public static final String BEAN_notBeanClasses_add = PREFIX + "notBeanClasses.sc/add";

	/**
	 * Configuration property:  Remove from classes that should not be considered beans.
	 */
	public static final String BEAN_notBeanClasses_remove = PREFIX + "notBeanClasses.sc/remove";

	/**
	 * Configuration property:  Packages whose classes should not be considered beans.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.notBeanPackages.ss"</js>
	 * 	<li><b>Data type:</b>  <code>Set&lt;String&gt;</code>
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
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#notBeanPackages(Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#notBeanPackages(String...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#notBeanPackages(boolean, Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#notBeanPackagesRemove(Object...)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * When specified, the current list of ignore packages are appended to.
	 *
	 * <p>
	 * Any classes within these packages will be serialized to strings using {@link Object#toString()}.
	 *
	 * <p>
	 * Note that you can specify prefix patterns to include all subpackages.
	 */
	public static final String BEAN_notBeanPackages = PREFIX + "notBeanPackages.ss";

	/**
	 * Configuration property:  Add to packages whose classes should not be considered beans.
	 */
	public static final String BEAN_notBeanPackages_add = PREFIX + "notBeanPackages.ss/add";

	/**
	 * Configuration property:  Remove from packages whose classes should not be considered beans.
	 */
	public static final String BEAN_notBeanPackages_remove = PREFIX + "notBeanPackages.ss/remove";

	/**
	 * Configuration property:  POJO swaps.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.pojoSwaps.lc"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b> 
	 * 		<ul>
	 * 			<li class='ja'>{@link Swap} 
	 * 			<li class='ja'>{@link Swaps} 
	 * 		</ul>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#pojoSwaps(Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#pojoSwaps(Class...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#pojoSwaps(boolean, Object...)}
	 * 			<li class='jm'>{@link BeanContextBuilder#pojoSwapsRemove(Object...)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * POJO swaps are used to "swap out" non-serializable classes with serializable equivalents during serialization,
	 * and "swap in" the non-serializable class during parsing.
	 * 
	 * <p>
	 * An example of a POJO swap would be a <code>Calendar</code> object that gets swapped out for an ISO8601 string.
	 * 
	 * <p>
	 * Multiple POJO swaps can be associated with a single class.
	 * <br>When multiple swaps are applicable to the same class, the media type pattern defined by
	 * {@link PojoSwap#forMediaTypes()} or {@link Swap#mediaTypes()} are used to come up with the best match.
	 * 
	 * <p>
	 * Values can consist of any of the following types:
	 * <ul>
	 * 	<li>Any subclass of {@link PojoSwap}.
	 * 	<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 	<li>Any array or collection of the objects above.
	 * </ul>
	 * 
	 *	<h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Sample swap for converting Dates to ISO8601 strings.</jc>
	 * 	<jk>public class</jk> MyDateSwap <jk>extends</jk> StringSwap&lt;Date&gt; {
	 * 		<jc>// ISO8601 formatter.</jc>
	 * 		<jk>private</jk> DateFormat <jf>format</jf> = <jk>new</jk> SimpleDateFormat(<js>"yyyy-MM-dd'T'HH:mm:ssZ"</js>);
	 * 		
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> String swap(BeanSession session, Date o) {
	 * 			<jk>return</jk> <jf>format</jf>.format(o);
	 * 		}
	 * 		
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> Date unswap(BeanSession session, String o, ClassMeta hint) <jk>throws</jk> Exception {
	 * 			<jk>return</jk> <jf>format</jf>.parse(o);
	 * 		}
	 * 	}
	 * 
	 * 	<jc>// Sample bean with a Date field.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> Date <jf>date</jf> = <jk>new</jk> Date(112, 2, 3, 4, 5, 6);
	 * 	}
	 * 
	 * 	<jc>// Create a new JSON serializer, associate our date swap with it, and serialize a sample bean.</jc>
	 * 	WriterSerializer s = JsonSerializer.<jsm>create</jsm>().pojoSwaps(MyDateSwap.<jk>class</jk>).build();
	 * 	String json = s.serialize(<jk>new</jk> MyBean());	<jc>// == "{date:'2012-03-03T04:05:06-0500'}"</jc>
	 * 	
	 * 	<jc>// Create a JSON parser, associate our date swap with it, and reconstruct our bean (including the date).</jc>
	 * 	ReaderParser p = JsonParser.<jsm>create</jsm>().pojoSwaps(MyDateSwap.<jk>class</jk>).build();
	 * 	MyBean bean = p.parse(json, MyBean.<jk>class</jk>);
	 * 	<jk>int</jk> day = bean.<jf>date</jf>.getDay(); 						<jc>// == 3</jc>
	 * </p>
	 * 
	 *	<h5 class='section'>Documentation:</h5>
	 *	<ul>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.PojoSwaps">Overview &gt; PojoSwaps</a>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.PerMediaTypePojoSwaps">Overview &gt; Per-media-type PojoSwaps</a>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.OneWayPojoSwaps">Overview &gt; One-way PojoSwaps</a>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.SwapAnnotation">Overview &gt; @Swap Annotation</a>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.SwapMethods">Overview &gt; Swap Methods</a>
	 *		<li><a class="doclink" href="../../../overview-summary.html#juneau-marshall.SurrogateClasses">Overview &gt; Surrogate Classes</a>
	 *	</ul>
	 */
	public static final String BEAN_pojoSwaps = PREFIX + "pojoSwaps.lc";

	/**
	 * Configuration property:  Add to POJO swap classes.
	 */
	public static final String BEAN_pojoSwaps_add = PREFIX + "pojoSwaps.lc/add";

	/**
	 * Configuration property:  Remove from POJO swap classes.
	 */
	public static final String BEAN_pojoSwaps_remove = PREFIX + "pojoSwaps.lc/remove";

	/**
	 * Configuration property:  Bean property namer.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.propertyNamer.c"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>implements</jk> {@link PropertyNamer}&gt;</code>
	 * 	<li><b>Default:</b>  {@link PropertyNamerDefault}
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b> 
	 * 		<ul>
	 * 			<li class='ja'>{@link Bean#propertyNamer()}
	 * 		</ul>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#propertyNamer(Class)}
	 * 			<li class='jm'>{@link BeanFilterBuilder#propertyNamer(PropertyNamer)}
	 * 		</ul>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * The class to use for calculating bean property names.
	 * 
	 * <p>
	 * Predefined classes:
	 * <ul>
	 * 	<li>{@link PropertyNamerDefault} - Default.
	 * 	<li>{@link PropertyNamerDLC} - Dashed-lower-case names.
	 * 	<li>{@link PropertyNamerULC} - Dashed-upper-case names.
	 * </ul>
	 */
	public static final String BEAN_propertyNamer = PREFIX + "propertyNamer.c";

	/**
	 * Configuration property:  Sort bean properties in alphabetical order.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.sortProperties.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b> 
	 * 		<ul>
	 * 			<li class='ja'>{@link Bean#sort()} 
	 * 		</ul>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#sortProperties(boolean)}
	 * 			<li class='jm'>{@link BeanFilterBuilder#sortProperties(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 * On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
	 * On Oracle JVMs, the bean properties are not ordered (which follows the official JVM specs).
	 *
	 * <p>
	 * This property is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
	 * to force bean properties to be in a particular order and can just alter the order of the fields/methods
	 * in the Java file.
	 */
	public static final String BEAN_sortProperties = PREFIX + "sortProperties.b";

	/**
	 * Configuration property:  TimeZone.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.timeZone.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> ({@link Locale})
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#timeZone(TimeZone)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Used in the in the {@link BeanSession#convertToType(Object, Class)} method.
	 */
	public static final String BEAN_timeZone = PREFIX + "timeZone.s";

	/**
	 * Configuration property:  Use interface proxies.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.useInterfaceProxies.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#useInterfaceProxies(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then interfaces will be instantiated as proxy classes through the use of an
	 * {@link InvocationHandler} if there is no other way of instantiating them.
	 */
	public static final String BEAN_useInterfaceProxies = PREFIX + "useInterfaceProxies.b";

	/**
	 * Configuration property:  Use Java {@link Introspector} for determining bean properties.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanContext.useJavaBeanIntrospector.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanContextBuilder#useJavaBeanIntrospector(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 * Most {@link Bean @Bean} annotations will be ignored.
	 */
	public static final String BEAN_useJavaBeanIntrospector = PREFIX + "useJavaBeanIntrospector.b";

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
	private static final ConcurrentHashMap<Integer,Map<Class,ClassMeta>> cmCacheCache
		= new ConcurrentHashMap<>();

	/** Default config.  All default settings. */
	public static final BeanContext DEFAULT = BeanContext.create().build();

	/** Default config.  All default settings except sort bean properties. */
	public static final BeanContext DEFAULT_SORTED = BeanContext.create().sortProperties(true).build();

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
	final Map<String,Class<?>> implClasses;
	final Locale locale;
	final TimeZone timeZone;
	final MediaType mediaType;
	final Map<String,String[]> includeProperties, excludeProperties;
	final PropertyNamer propertyNamer;

	final Map<Class,ClassMeta> cmCache;
	final ClassMeta<Object> cmObject;  // Reusable ClassMeta that represents general Objects.
	final ClassMeta<String> cmString;  // Reusable ClassMeta that represents general Strings.
	final ClassMeta<Class> cmClass;  // Reusable ClassMeta that represents general Classes.

	final String beanTypePropertyName;

	final int beanHashCode;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Typically only called from {@link ContextBuilder#build(Class)} method.
	 *
	 * @param ps The property store containing the unmodifiable configuration for this bean context.
	 */
	public BeanContext(PropertyStore ps) {
		super(ps);

		beanHashCode = ps.hashCode("BeanContext");

		beansRequireDefaultConstructor = getProperty(BEAN_beansRequireDefaultConstructor, boolean.class, false);
		beansRequireSerializable = getProperty(BEAN_beansRequireSerializable, boolean.class, false);
		beansRequireSettersForGetters = getProperty(BEAN_beansRequireSettersForGetters, boolean.class, false);
		beansRequireSomeProperties = getProperty(BEAN_beansRequireSomeProperties, boolean.class, true);
		beanMapPutReturnsOldValue = getProperty(BEAN_beanMapPutReturnsOldValue, boolean.class, false);
		useInterfaceProxies = getProperty(BEAN_useInterfaceProxies, boolean.class, true);
		ignoreUnknownBeanProperties = getProperty(BEAN_ignoreUnknownBeanProperties, boolean.class, false);
		ignoreUnknownNullBeanProperties = getProperty(BEAN_ignoreUnknownNullBeanProperties, boolean.class, true);
		ignorePropertiesWithoutSetters = getProperty(BEAN_ignorePropertiesWithoutSetters, boolean.class, true);
		ignoreInvocationExceptionsOnGetters = getProperty(BEAN_ignoreInvocationExceptionsOnGetters, boolean.class, false);
		ignoreInvocationExceptionsOnSetters = getProperty(BEAN_ignoreInvocationExceptionsOnSetters, boolean.class, false);
		useJavaBeanIntrospector = getProperty(BEAN_useJavaBeanIntrospector, boolean.class, false);
		sortProperties = getProperty(BEAN_sortProperties, boolean.class, false);
		beanTypePropertyName = getProperty(BEAN_beanTypePropertyName, String.class, "_type");
		debug = getProperty(BEAN_debug, boolean.class, false);

		beanConstructorVisibility = getProperty(BEAN_beanConstructorVisibility, Visibility.class, PUBLIC);
		beanClassVisibility = getProperty(BEAN_beanClassVisibility, Visibility.class, PUBLIC);
		beanMethodVisibility = getProperty(BEAN_beanMethodVisibility, Visibility.class, PUBLIC);
		beanFieldVisibility = getProperty(BEAN_beanFieldVisibility, Visibility.class, PUBLIC);

		notBeanClasses = getClassArrayProperty(BEAN_notBeanClasses, DEFAULT_NOTBEAN_CLASSES);
		
		propertyNamer = getInstanceProperty(BEAN_propertyNamer, PropertyNamer.class, PropertyNamerDefault.class);

		List<String> l1 = new LinkedList<>();
		List<String> l2 = new LinkedList<>();
		for (String s : getArrayProperty(BEAN_notBeanPackages, String.class, DEFAULT_NOTBEAN_PACKAGES)) {
			if (s.endsWith(".*"))
				l2.add(s.substring(0, s.length()-2));
			else
				l1.add(s);
		}
		notBeanPackageNames = l1.toArray(new String[l1.size()]);
		notBeanPackagePrefixes = l2.toArray(new String[l2.size()]);

		LinkedList<BeanFilter> lbf = new LinkedList<>();
		for (Class<?> c : getClassListProperty(BEAN_beanFilters)) {
			if (isParentClass(BeanFilter.class, c))
				lbf.add(newInstance(BeanFilter.class, c));
			else if (isParentClass(BeanFilterBuilder.class, c))
				lbf.add(newInstance(BeanFilterBuilder.class, c).build());
			else
				lbf.add(new InterfaceBeanFilterBuilder(c).build());
		}
		beanFilters = lbf.toArray(new BeanFilter[0]);

		LinkedList<PojoSwap<?,?>> lpf = new LinkedList<>();
		for (Class<?> c : getClassListProperty(BEAN_pojoSwaps)) {
			if (isParentClass(PojoSwap.class, c))
				lpf.add(newInstance(PojoSwap.class, c));
			else if (isParentClass(Surrogate.class, c))
				lpf.addAll(SurrogateSwap.findPojoSwaps(c));
			else
				throw new FormattedRuntimeException("Invalid class {0} specified in BeanContext.pojoSwaps property.  Must be a subclass of PojoSwap or Surrogate.", c);
		}
		pojoSwaps = lpf.toArray(new PojoSwap[lpf.size()]);
		
		implClasses = getClassMapProperty(BEAN_implClasses);
		
		Map<String,String[]> m2 = new HashMap<>();
		for (Map.Entry<String,String> e : getMapProperty(BEAN_includeProperties, String.class).entrySet())
			m2.put(e.getKey(), StringUtils.split(e.getValue()));
		includeProperties = Collections.unmodifiableMap(m2);

		m2 = new HashMap<>();
		for (Map.Entry<String,String> e : getMapProperty(BEAN_excludeProperties, String.class).entrySet())
			m2.put(e.getKey(), StringUtils.split(e.getValue()));
		excludeProperties = Collections.unmodifiableMap(m2);

		locale = getInstanceProperty(BEAN_locale, Locale.class, null);
		timeZone = getInstanceProperty(BEAN_timeZone, TimeZone.class, null);
		mediaType = getInstanceProperty(BEAN_mediaType, MediaType.class, null);
		
		if (! cmCacheCache.containsKey(beanHashCode)) {
			ConcurrentHashMap<Class,ClassMeta> cm = new ConcurrentHashMap<>();
			cm.putIfAbsent(String.class, new ClassMeta(String.class, this, null, null, findPojoSwaps(String.class), findChildPojoSwaps(String.class)));
			cm.putIfAbsent(Object.class, new ClassMeta(Object.class, this, null, null, findPojoSwaps(Object.class), findChildPojoSwaps(Object.class)));
			cmCacheCache.putIfAbsent(beanHashCode, cm);
		}
		cmCache = cmCacheCache.get(beanHashCode);
		cmString = cmCache.get(String.class);
		cmObject = cmCache.get(Object.class);
		cmClass = cmCache.get(Class.class);

		beanDictionaryClasses = getClassArrayProperty(BEAN_beanDictionary);
		beanRegistry = new BeanRegistry(this, null);
	}

	@Override /* Context */
	public BeanContextBuilder builder() {
		return new BeanContextBuilder(getPropertyStore());
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
		return new BeanSessionArgs(null, null, null, null);
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
		for (Class exclude : notBeanClasses)
			if (isParentClass(exclude, c))
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
	 * Prints meta cache statistics to <code>System.out</code>.
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
	 * 	If <jk>true</jk>, wait for the ClassMeta constructor to finish before returning.
	 * @return
	 * 	If the class is not an array, returns a cached {@link ClassMeta} object.
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	final <T> ClassMeta<T> getClassMeta(Class<T> type, boolean waitForInit) {

		// If this is an array, then we want it wrapped in an uncached ClassMeta object.
		// Note that if it has a pojo swap, we still want to cache it so that
		// we can cache something like byte[] with ByteArrayBase64Swap.
		if (type.isArray() && findPojoSwaps(type) == null)
			return new ClassMeta(type, this, findImplClass(type), findBeanFilter(type), findPojoSwaps(type), findChildPojoSwaps(type));

		// This can happen if we have transforms defined against String or Object.
		if (cmCache == null)
			return null;

		ClassMeta<T> cm = cmCache.get(type);
		if (cm == null) {

			synchronized (this) {
				// Make sure someone didn't already set it while this thread was blocked.
				cm = cmCache.get(type);
				if (cm == null)
					cm = new ClassMeta<>(type, this, findImplClass(type), findBeanFilter(type), findPojoSwaps(type), findChildPojoSwaps(type));
			}
		}
		if (waitForInit)
			cm.waitForInit();
		return cm;
	}

	/**
	 * Used to resolve <code>ClassMetas</code> of type <code>Collection</code> and <code>Map</code> that have
	 * <code>ClassMeta</code> values that themselves could be collections or maps.
	 *
	 * <p>
	 * <code>Collection</code> meta objects are assumed to be followed by zero or one meta objects indicating the element type.
	 *
	 * <p>
	 * <code>Map</code> meta objects are assumed to be followed by zero or two meta objects indicating the key and value types.
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
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
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

		if (o instanceof ClassMeta) {
			ClassMeta<?> cm = (ClassMeta)o;

			// This classmeta could have been created by a different context.
			// Need to re-resolve it to pick up PojoSwaps and stuff on this context.
			if (cm.getBeanContext() == this)
				return cm;
			if (cm.isMap())
				return getClassMeta(cm.innerClass, cm.getKeyType(), cm.getValueType());
			if (cm.isCollection())
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
	 * Used for determining the class type on a method or field where a {@code @BeanProperty} annotation may be present.
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
	protected final <T> ClassMeta<T> resolveClassMeta(BeanProperty p, Type t, Map<Class<?>,Class<?>[]> typeVarImpls) {
		ClassMeta<T> cm = resolveClassMeta(t, typeVarImpls);
		ClassMeta<T> cm2 = cm;
		if (p != null) {

			if (p.type() != Object.class)
				cm2 = resolveClassMeta(p.type(), typeVarImpls);

			if (cm2.isMap()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class, Object.class} : p.params());
				if (pParams.length != 2)
					throw new FormattedRuntimeException("Invalid number of parameters specified for Map (must be 2): {0}", pParams.length);
				ClassMeta<?> keyType = resolveType(pParams[0], cm2.getKeyType(), cm.getKeyType());
				ClassMeta<?> valueType = resolveType(pParams[1], cm2.getValueType(), cm.getValueType());
				if (keyType.isObject() && valueType.isObject())
					return cm2;
				return new ClassMeta<>(cm2, keyType, valueType, null);
			}

			if (cm2.isCollection()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class} : p.params());
				if (pParams.length != 1)
					throw new FormattedRuntimeException("Invalid number of parameters specified for Collection (must be 1): {0}", pParams.length);
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
			for (PojoSwap f : pojoSwaps)
				if (isParentClass(f.getNormalClass(), c))
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
		if (c == null || pojoSwaps.length == 0)
			return null;
		List<PojoSwap> l = null;
		for (PojoSwap f : pojoSwaps) {
			if (isParentClass(c, f.getNormalClass())) {
				if (l == null)
					l = new ArrayList<>();
				l.add(f);
			}
		}
		return l == null ? null : l.toArray(new PojoSwap[l.size()]);
	}

	/**
	 * Returns the {@link BeanFilter} associated with the specified class, or <jk>null</jk> if there is no bean filter
	 * associated with the class.
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
	 * Returns the type property name as defined by {@link #BEAN_beanTypePropertyName}.
	 *
	 * @return The type property name.  Never <jk>null</jk>.
	 */
	protected final String getBeanTypePropertyName() {
		return beanTypePropertyName;
	}

	/**
	 * Returns the bean registry defined in this bean context defined by {@link #BEAN_beanDictionary}.
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
			Class implClass = implClasses.get(cc.getName());
			if (implClass != null)
				return findNoArgConstructor(implClass, v);
			for (Class ic : cc.getInterfaces()) {
				implClass = implClasses.get(ic.getName());
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
			Class implClass = implClasses.get(cc.getName());
			if (implClass != null)
				return implClass;
			for (Class ic : cc.getInterfaces()) {
				implClass = implClasses.get(ic.getName());
				if (implClass != null)
					return implClass;
			}
			cc = cc.getSuperclass();
		}
		return null;
	}

	/**
	 * Returns the {@link #BEAN_includeProperties} setting for the specified class.
	 *
	 * @param c The class.
	 * @return The properties to include for the specified class, or <jk>null</jk> if it's not defined for the class.
	 */
	public String[] getIncludeProperties(Class<?> c) {
		if (includeProperties.isEmpty())
			return null;
		String[] s = null;
		for (Iterator<Class<?>> i = ClassUtils.getParentClasses(c, false, true); i.hasNext();) {
			Class<?> c2 = i.next();
			s = includeProperties.get(c2.getName());
			if (s != null)
				return s;
			s = includeProperties.get(c2.getSimpleName());
			if (s != null)
				return s;
		}
		return includeProperties.get("*");
	}

	/**
	 * Returns the {@link #BEAN_excludeProperties} setting for the specified class.
	 *
	 * @param c The class.
	 * @return The properties to exclude for the specified class, or <jk>null</jk> if it's not defined for the class.
	 */
	public String[] getExcludeProperties(Class<?> c) {
		if (excludeProperties.isEmpty())
			return null;
		String[] s = null;
		for (Iterator<Class<?>> i = ClassUtils.getParentClasses(c, false, true); i.hasNext();) {
			Class<?> c2 = i.next();
			s = excludeProperties.get(c2.getName());
			if (s != null)
				return s;
			s = excludeProperties.get(c2.getSimpleName());
			if (s != null)
				return s;
		}
		return excludeProperties.get("*");
	}

	/**
	 * Creates an instance of the specified class.
	 *
	 * @param c 
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @return 
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws 
	 * 	RuntimeException if constructor could not be found or called.
	 */
	public <T> T newInstance(Class<T> c, Object c2) {
		return ClassUtils.newInstance(c, c2);
	}

	/**
	 * Creates an instance of the specified class.
	 *
	 * @param c 
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @param fuzzyArgs 
	 * 	Use fuzzy constructor arg matching.  
	 * 	<br>When <jk>true</jk>, constructor args can be in any order and extra args are ignored.
	 * 	<br>No-arg constructors are also used if no other constructors are found.
	 * @param args 
	 * 	The arguments to pass to the constructor.
	 * @return 
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws 
	 * 	RuntimeException if constructor could not be found or called.
	 */
	public <T> T newInstance(Class<T> c, Object c2, boolean fuzzyArgs, Object...args) {
		return ClassUtils.newInstance(c, c2, fuzzyArgs, args);
	}

	/**
	 * Creates an instance of the specified class from within the context of another object.
	 * 
	 * @param outer
	 * 	The outer object.
	 * 	Can be <jk>null</jk>.
	 * @param c 
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @param fuzzyArgs 
	 * 	Use fuzzy constructor arg matching.  
	 * 	<br>When <jk>true</jk>, constructor args can be in any order and extra args are ignored.
	 * 	<br>No-arg constructors are also used if no other constructors are found.
	 * @param args 
	 * 	The arguments to pass to the constructor.
	 * @return 
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws 
	 * 	RuntimeException if constructor could not be found or called.
	 */
	public <T> T newInstanceFromOuter(Object outer, Class<T> c, Object c2, boolean fuzzyArgs, Object...args) {
		return ClassUtils.newInstanceFromOuter(outer, c, c2, fuzzyArgs, args);
	}
	
	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>Object</code>.
	 *
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent "any object type" when an object type is not known.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Object.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>Object</code> class.
	 */
	protected final ClassMeta<Object> object() {
		return cmObject;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>String</code>.
	 *
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent key types in maps.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(String.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>String</code> class.
	 */
	protected final ClassMeta<String> string() {
		return cmString;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>Class</code>.
	 *
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent key types in maps.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Class.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>String</code> class.
	 */
	protected final ClassMeta<Class> _class() {
		return cmClass;
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("BeanContext", new ObjectMap()
				.append("id", System.identityHashCode(this))
				.append("beanClassVisibility", beanClassVisibility)
				.append("beanConstructorVisibility", beanConstructorVisibility)
				.append("beanDictionaryClasses", beanDictionaryClasses)
				.append("beanFieldVisibility", beanFieldVisibility)
				.append("beanFilters", beanFilters)
				.append("beanMapPutReturnsOldValue", beanMapPutReturnsOldValue)
				.append("beanMethodVisibility", beanMethodVisibility)
				.append("beansRequireDefaultConstructor", beansRequireDefaultConstructor)
				.append("beansRequireSerializable", beansRequireSerializable)
				.append("beansRequireSettersForGetters", beansRequireSettersForGetters)
				.append("beansRequireSomeProperties", beansRequireSomeProperties)
				.append("excludeProperties", excludeProperties)
				.append("ignoreInvocationExceptionsOnGetters", ignoreInvocationExceptionsOnGetters)
				.append("ignoreInvocationExceptionsOnSetters", ignoreInvocationExceptionsOnSetters)
				.append("ignorePropertiesWithoutSetters", ignorePropertiesWithoutSetters)
				.append("ignoreUnknownBeanProperties", ignoreUnknownBeanProperties)
				.append("ignoreUnknownNullBeanProperties", ignoreUnknownNullBeanProperties)
				.append("implClasses", implClasses)
				.append("properties", includeProperties)
				.append("locale", locale)
				.append("mediaType", mediaType)
				.append("notBeanClasses", notBeanClasses)
				.append("notBeanPackageNames", notBeanPackageNames)
				.append("notBeanPackagePrefixes", notBeanPackagePrefixes)
				.append("pojoSwaps", pojoSwaps)
				.append("sortProperties", sortProperties)
				.append("timeZone", timeZone)
				.append("useInterfaceProxies", useInterfaceProxies)
				.append("useJavaBeanIntrospector", useJavaBeanIntrospector)
			);
	}
}