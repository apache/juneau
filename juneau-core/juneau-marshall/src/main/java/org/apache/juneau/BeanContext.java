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

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Core class of the Juneau architecture.
 *
 * <p>
 * This class servers multiple purposes:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Provides the ability to wrap beans inside {@link Map} interfaces.
 * 	<li>
 * 		Serves as a repository for metadata on POJOs, such as associated {@link BeanFilter BeanFilters},
 * 		{@link PropertyNamer PropertyNamers}, etc...  which are used to tailor how POJOs are serialized and parsed.
 * </ul>
 *
 * <p>
 * All serializers and parsers extend from this context so that they can handle POJOs using a common framework.
 *
 * <h5 class='topic'>Bean Contexts</h5>
 *
 * Bean contexts are created through the {@link BeanContext#create() BeanContext.create()} and {@link BeanContextBuilder#build()} methods.
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
 * Whereas <c>BeanContext</c> objects are permanent, unchangeable, cached, and thread-safe,
 * {@link BeanSession} objects are ephemeral and not thread-safe.
 * They are meant to be used as quickly-constructed scratchpads for creating bean maps.
 * {@link BeanMap} objects can only be created through the session.
 *
 * <h5 class='topic'>BeanContext configuration properties</h5>
 *
 * <c>BeanContexts</c> have several configuration properties that can be used to tweak behavior on how beans are
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
 * <p class='bcode w800'>
 * 	<jc>// Construct a context from scratch.</jc>
 * 	BeanContext beanContext = BeanContext
 * 		.<jsm>create</jsm>()
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
 * <p class='bcode w800'>
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-marshall.ContextsBuildersSessionsPropertyStores}
 * </ul>
 */
@SuppressWarnings({"unchecked","rawtypes"})
@ConfigurableContext
public class BeanContext extends Context implements MetaProvider {

	static final String PREFIX = "BeanContext";

	/**
	 * Configuration property:  Annotations.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_annotations BEAN_annotations}
	 * 	<li><b>Name:</b>  <js>"BeanContext.annotations.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link java.lang.annotation.Annotation}&gt;</c>
	 * 	<li><b>Default:</b>  Empty list.
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#applyBean()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#applyBeanp()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#annotations(Annotation...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines annotations to apply to specific classes and methods.
	 *
	 * <p>
	 * Allows you to dynamically apply Juneau annotations typically applied directly to classes and methods.
	 * Useful in cases where you want to use the functionality of the annotation on beans and bean properties but
	 * do not have access to the code to do so.
	 *
	 * <p>
	 * As a rule, any Juneau annotation with an <c>on()</c> method can be used with this property.
	 *
	 * <p>
	 * The following example shows the equivalent methods for applying the {@link Bean @Bean} annotation:
	 * <p class='bpcode w800'>
	 * 	<jc>// Class with explicit annotation.</jc>
	 * 	<ja>@Bean</ja>(bpi=<js>"street,city,state"</js>)
	 * 	<jk>public class</jk> A {...}
	 *
	 * 	<jc>// Class with annotation applied via @BeanConfig</jc>
	 * 	<jk>public class</jk> B {...}
	 *
	 * 	<jc>// Java REST method with @BeanConfig annotation.</jc>
	 * 	<ja>@RestMethod</ja>(...)
	 * 	<ja>@BeanConfig</ja>(
	 * 		annotations={
	 * 			<ja>@Bean</ja>(on=<js>"B"</js>, bpi=<js>"street,city,state"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public void</jk> doFoo() {...}
	 * </p>
	 *
	 * <p>
	 * Concrete implementations of annotations are also provided that can be passed directly into serializer and parser
	 * builder classes:
	 * <p class='bpcode w800'>
	 * 	BeanAnnotation a = <jk>new</jk> BeanAnnotation(<js>"B"</js>).bpi(<js>"street,city,state"</js>);
	 * 	WriterSerializer ws = JsonSerializer.<jsm>create</jsm>().annotations(a).build();
	 * 	String json = ws.serialize(a);
	 * </p>
	 *
	 * <p>
	 * The following is the list of concrete annotations provided that can be constructed and passed into the builder
	 * class:
	 * <ul class='javatree'>
	 * 	<li class'jc'>{@link org.apache.juneau.annotation.BeanAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.annotation.BeancAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.annotation.BeanIgnoreAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.annotation.BeanpAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.annotation.ExampleAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.annotation.NamePropertyAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.annotation.ParentPropertyAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.annotation.SwapAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.annotation.UriAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.csv.annotation.CsvAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.html.annotation.HtmlAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.jso.annotation.JsoAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.json.annotation.JsonAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.jsonschema.annotation.SchemaAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.msgpack.annotation.MsgPackAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.oapi.annotation.OpenApiAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.plaintext.annotation.PlainTextAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.soap.annotation.SoapXmlAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.uon.annotation.UonAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.urlencoding.annotation.UrlEncodingAnnotation}
	 * 	<li class'jc'>{@link org.apache.juneau.xml.annotation.XmlAnnotation}
	 * </ul>
	 */
	public static final String BEAN_annotations = PREFIX + ".annotations.lo";

	/**
	 * Configuration property:  Minimum bean class visibility.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanClassVisibility BEAN_beanClassVisibility}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanClassVisibility.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Visibility}
	 * 	<li><b>System property:</b>  <c>BeanContext.beanClassVisibility</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANCLASSVISIBILITY</c>
	 * 	<li><b>Default:</b>  <js>"PUBLIC"</js>
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 *
	 * <p>
	 * For example, if the visibility is <c>PUBLIC</c> and the bean class is <jk>protected</jk>, then the class
	 * will not be interpreted as a bean class and be serialized as a string.
	 * <br>Use this setting to reduce the visibility requirement.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * </p>
	 */
	public static final String BEAN_beanClassVisibility = PREFIX + ".beanClassVisibility.s";

	/**
	 * Configuration property:  Minimum bean constructor visibility.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanConstructorVisibility BEAN_beanConstructorVisibility}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanConstructorVisibility.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Visibility}
	 * 	<li><b>System property:</b>  <c>BeanContext.beanConstructorVisibility</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANCONSTRUCTORVISIBILITY</c>
	 * 	<li><b>Default:</b>  <js>"PUBLIC"</js>
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Only look for constructors with the specified minimum visibility.
	 *
	 * <p>
	 * This setting affects the logic for finding no-arg constructors for bean.
	 * <br>Normally, only <jk>public</jk> no-arg constructors are used.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * </p>
	 */
	public static final String BEAN_beanConstructorVisibility = PREFIX + ".beanConstructorVisibility.s";

	/**
	 * Configuration property:  Bean dictionary.
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
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dictionary(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dictionary(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dictionaryReplace(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dictionaryReplace(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dictionaryRemove(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dictionaryRemove(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#dictionary(Class...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The list of classes that make up the bean dictionary in this bean context.
	 *
	 * <p>
	 * A dictionary is a name/class mapping used to find class types during parsing when they cannot be inferred
	 * through reflection.
	 * <br>The names are defined through the {@link Bean#typeName() @Bean(typeName)} annotation defined on the bean class.
	 * <br>For example, if a class <c>Foo</c> has a type-name of <js>"myfoo"</js>, then it would end up serialized
	 * as <js>"{_type:'myfoo',...}"</js>.
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
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser and tell it which classes to try to resolve.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.dictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>)
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
	 * 	<ja>@Bean</ja>(dictionary={Foo.<jk>class</jk>,Bar.<jk>class</jk>})
	 * 	<jk>public class</jk> MyBean {...}
	 *
	 * 	<jc>// Use the predefined HTML5 bean dictionary which is a BeanDictionaryList.</jc>
	 * 	ReaderParser p = HtmlParser
	 * 		.<jsm>create</jsm>()
	 * 		.dictionary(HtmlBeanDictionary.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.BeanDictionaries}
	 * </ul>
	 */
	public static final String BEAN_beanDictionary = PREFIX + ".beanDictionary.lc";

	/**
	 * Configuration property:  Add to bean dictionary.
	 */
	public static final String BEAN_beanDictionary_add = PREFIX + ".beanDictionary.lc/add";

	/**
	 * Configuration property:  Remove from bean dictionary.
	 */
	public static final String BEAN_beanDictionary_remove = PREFIX + ".beanDictionary.lc/remove";

	/**
	 * Configuration property:  Minimum bean field visibility.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanFieldVisibility BEAN_beanFieldVisibility}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanFieldVisibility.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Visibility}
	 * 	<li><b>System property:</b>  <c>BeanContext.beanFieldVisibility</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANFIELDVISIBILITY</c>
	 * 	<li><b>Default:</b>  <js>"PUBLIC"</js>
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Only look for bean fields with the specified minimum visibility.
	 *
	 * <p>
	 * This affects which fields on a bean class are considered bean properties.
	 * <br>Normally only <jk>public</jk> fields are considered.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * </p>
	 */
	public static final String BEAN_beanFieldVisibility = PREFIX + ".beanFieldVisibility.s";

	/**
	 * Configuration property:  Bean filters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanFilters BEAN_beanFilters}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanFilters.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;Object&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanFilters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanFilters(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanFilters(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanFiltersReplace(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanFiltersReplace(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanFiltersRemove(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanFiltersRemove(Class...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * This is a programmatic equivalent to the {@link Bean @Bean} annotation.
	 * <br>It's useful when you want to use the <c>@Bean</c> annotation functionality, but you don't have the ability to alter
	 * the bean classes.
	 *
	 * <p>
	 * Values can consist of any of the following types:
	 * <ul class='spaced-list'>
	 * 	<li>Any subclass or instance of {@link BeanFilterBuilder}.
	 * 		<br>These must have a public no-arg constructor.
	 * 	<li>Any instance of {@link BeanFilter}.
	 * 	<li>Any bean interfaces.
	 * 		<br>A shortcut for defining a {@link InterfaceBeanFilterBuilder}.
	 * 		<br>Any subclasses of an interface class will only have properties defined on the interface.
	 * 		All other bean properties will be ignored.
	 * 	<li>Any array or collection of the objects above.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a bean filter for the MyBean class.</jc>
	 * 	<jk>public class</jk> MyBeanFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 *
	 * 		<jc>// Must provide a no-arg constructor!</jc>
	 * 		<jk>public</jk> MyBeanFilter() {
	 * 			bpi(<js>"foo,bar,baz"</js>);  <jc>// The properties we want exposed.</jc>
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
	 *
	 * 	<jc>// Same but pass in constructed filter.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(BeanFilter.<jsm>create</jsm>(MyBeanFilter.<jk>class</jk>).bpi(<js>"foo,bar,baz"</js>).build())
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.BeanFilters}
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.InterfaceFilters}
	 * </ul>
	 */
	public static final String BEAN_beanFilters = PREFIX + ".beanFilters.lo";

	/**
	 * Configuration property:  Add to bean filters.
	 */
	public static final String BEAN_beanFilters_add = PREFIX + ".beanFilters.lo/add";

	/**
	 * Configuration property:  Remove from bean filters.
	 */
	public static final String BEAN_beanFilters_remove = PREFIX + ".beanFilters.lo/remove";

	/**
	 * Configuration property:  BeanMap.put() returns old property value.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
	 * values.
	 * <br>Otherwise, it returns <jk>null</jk>.
	 *
	 * <p>
	 * Disabled by default because it introduces a slight performance penalty during serialization.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that creates BeanMaps with normal put() behavior.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanMapPutReturnsOldValue()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beanMapPutReturnsOldValue</jsf>, <jk>true</jk>)
	 * 		.build();
	 *
	 * 	BeanMap&lt;MyBean&gt; bm = s.createSession().toBeanMap(<jk>new</jk> MyBean());
	 * 	bm.put(<js>"foo"</js>, <js>"bar"</js>);
	 * 	Object oldValue = bm.put(<js>"foo"</js>, <js>"baz"</js>);  <jc>// oldValue == "bar"</jc>
	 * </p>
	 */
	public static final String BEAN_beanMapPutReturnsOldValue = PREFIX + ".beanMapPutReturnsOldValue.b";

	/**
	 * Configuration property:  Minimum bean method visibility.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanMethodVisibility BEAN_beanMethodVisibility}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanMethodVisibility.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Visibility}
	 * 	<li><b>System property:</b>  <c>BeanContext.beanMethodVisibility</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANMETHODVISIBILITY</c>
	 * 	<li><b>Default:</b>  <js>"PUBLIC"</js>
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Only look for bean methods with the specified minimum visibility.
	 *
	 * <p>
	 * This affects which methods are detected as getters and setters on a bean class.
	 * <br>Normally only <jk>public</jk> getters and setters are considered.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * </p>
	 */
	public static final String BEAN_beanMethodVisibility = PREFIX + ".beanMethodVisibility.s";

	/**
	 * Configuration property:  Beans require no-arg constructors.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, a Java class must implement a default no-arg constructor to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that ignores beans without default constructors.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beansRequireDefaultConstructor()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beansRequireDefaultConstructor</jsf>, <jk>true</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_beansRequireDefaultConstructor = PREFIX + ".beansRequireDefaultConstructor.b";

	/**
	 * Configuration property:  Beans require Serializable interface.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that ignores beans not implementing Serializable.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beansRequireSerializable()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beansRequireSerializable</jsf>, <jk>true</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_beansRequireSerializable = PREFIX + ".beansRequireSerializable.b";

	/**
	 * Configuration property:  Beans require setters for getters.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, only getters that have equivalent setters will be considered as properties on a bean.
	 * <br>Otherwise, they will be ignored.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that ignores bean properties without setters.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beansRequireSettersForGetter()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beansRequireSettersForGetters</jsf>, <jk>true</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_beansRequireSettersForGetters = PREFIX + ".beansRequireSettersForGetters.b";

	/**
	 * Configuration property:  Beans require at least one property.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beansRequireSomeProperties BEAN_beansRequireSomeProperties}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beansRequireSomeProperties.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.beansRequireSomeProperties</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANSREQUIRESOMEPROPERTIES</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beansRequireSomeProperties()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beansDontRequireSomeProperties()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then a Java class must contain at least 1 property to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that serializes beans even if they have zero properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beansRequireSomeProperties(<jk>false</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beansRequireSomeProperties</jsf>, <jk>false</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_beansRequireSomeProperties = PREFIX + ".beansRequireSomeProperties.b";

	/**
	 * Configuration property:  Bean type property name.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_beanTypePropertyName BEAN_beanTypePropertyName}
	 * 	<li><b>Name:</b>  <js>"BeanContext.beanTypePropertyName.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>BeanContext.beanTypePropertyName</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BEANTYPEPROPERTYNAME</c>
	 * 	<li><b>Default:</b>  <js>"_type"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#typePropertyName()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#beanTypePropertyName()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#beanTypePropertyName(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <p>
	 * This specifies the name of the bean property used to store the dictionary name of a bean type so that the
	 * parser knows the data type to reconstruct.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that uses 'type' instead of '_type' for dictionary names.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanTypePropertyName(<js>"type"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beanTypePropertyName</jsf>, <js>"type"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.BeanDictionaries}
	 * </ul>
	 */
	public static final String BEAN_beanTypePropertyName = PREFIX + ".beanTypePropertyName.s";

	/**
	 * Configuration property:  Bean property includes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_bpi BEAN_bpi}
	 * 	<li><b>Name:</b>  <js>"BeanContext.bpi.sms"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>System property:</b>  <c>BeanContext.bpi</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BPI</c>
	 * 	<li><b>Default:</b>  <c>{}</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#bpi()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Beanp#properties()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#bpi()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpi(Class, String)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpi(String, String)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpi(Map)}
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#bpi(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the set and order of names of properties associated with the bean class.
	 *
	 * <p>
	 * The keys are either fully-qualified or simple class names, and the values are comma-delimited lists of property
	 * names.
	 * The key <js>"*"</js> means all bean classes.
	 *
	 * <p>
	 * For example, <code>{Bean1:<js>'foo,bar'</js>}</code> means only serialize the <c>foo</c> and
	 * <c>bar</c> properties on the specified bean.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that includes only the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpi(MyBean.<jk>class</jk>, <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_bpi</jsf>, MyBean.<jk>class</jk>.getName(), <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Alternate using JSON object.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_bpi</jsf>, <js>"{'org.apache.MyBean':'foo,bar'}"</js>)
	 * 		.build();
	 * </p>
	 * @deprecated Use {@link Bean#bpi()} and {@link BeanConfig#bpi()}
	 */
	@Deprecated
	public static final String BEAN_bpi = PREFIX + ".bpi.sms";

	/**
	 * Configuration property:  Bean property excludes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_bpx BEAN_bpx}
	 * 	<li><b>Name:</b>  <js>"BeanContext.bpx.sms"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>System property:</b>  <c>BeanContext.bpx</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BPX</c>
	 * 	<li><b>Default:</b>  <c>{}</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#dictionary()}
	 * 		</ul>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#bpx()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#bpx()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpx(Class, String)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpx(String, String)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpx(Map)}
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#bpx(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies to exclude the specified list of properties for the specified bean class.
	 *
	 * <p>
	 * The keys are either fully-qualified or simple class names, and the values are comma-delimited lists of property
	 * names.
	 * The key <js>"*"</js> means all bean classes.
	 *
	 * <p>
	 * For example, <code>{Bean1:<js>'foo,bar'</js>}</code> means don't serialize the <c>foo</c> and
	 * <c>bar</c> properties on any beans whose simple class name is <c>Bean1</c>.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that excludes the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpx(MyBean.<jk>class</jk>, <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_bpx</jsf>, MyBean.<jk>class</jk>.getName(), <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Alternate using JSON object.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_bpx</jsf>, <js>"{'org.apache.MyBean':'foo,bar'}"</js>)
	 * 		.build();
	 * </p>
	 * @deprecated Use {@link Bean#bpx()} and {@link BeanConfig#bpx()}
	 */
	@Deprecated
	public static final String BEAN_bpx = PREFIX + ".bpx.sms";

	/**
	 * Configuration property:  Read-only bean properties.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_bpro BEAN_bpro}
	 * 	<li><b>Name:</b>  <js>"BeanContext.bpro.sms"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>System property:</b>  <c>BeanContext.bpro</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BPRO</c>
	 * 	<li><b>Default:</b>  <c>{}</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#bpro()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Beanp#ro()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#bpro()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpro(Class, String)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpro(String, String)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpro(Map)}
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#bpro(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies one or more properties on a bean that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 *
	 * <p>
	 * The keys are either fully-qualified or simple class names, and the values are comma-delimited lists of property
	 * names.
	 * The key <js>"*"</js> means all bean classes.
	 *
	 * <p>
	 * For example, <code>{Bean1:<js>'foo,bar'</js>}</code> means the <c>foo</c> and
	 * <c>bar</c> properties on any beans whose simple class name is <c>Bean1</c>.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that excludes the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.bpro(MyBean.<jk>class</jk>, <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_bpro</jsf>, MyBean.<jk>class</jk>.getName(), <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Alternate using JSON object.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_bpro</jsf>, <js>"{'org.apache.MyBean':'foo,bar'}"</js>)
	 * 		.build();
	 * </p>
	 * @deprecated Use {@link Bean#bpro()} and {@link BeanConfig#bpro()}
	 */
	@Deprecated
	public static final String BEAN_bpro = PREFIX + ".bpro.sms";

	/**
	 * Configuration property:  Write-only bean properties.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_bpwo BEAN_bpwo}
	 * 	<li><b>Name:</b>  <js>"BeanContext.bpwo.sms"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>System property:</b>  <c>BeanContext.bpwo</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_BPWO</c>
	 * 	<li><b>Default:</b>  <c>{}</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#bpwo()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Beanp#wo()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#bpwo()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpwo(Class, String)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpwo(String, String)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#bpwo(Map)}
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#bpwo(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 *
	 * <p>
	 * The keys are either fully-qualified or simple class names, and the values are comma-delimited lists of property
	 * names.
	 * The key <js>"*"</js> means all bean classes.
	 *
	 * <p>
	 * For example, <code>{Bean1:<js>'foo,bar'</js>}</code> means the <c>foo</c> and
	 * <c>bar</c> properties on any beans whose simple class name is <c>Bean1</c>.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that excludes the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpwo(MyBean.<jk>class</jk>, <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_bpwo</jsf>, MyBean.<jk>class</jk>.getName(), <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Alternate using JSON object.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_bpwo</jsf>, <js>"{'org.apache.MyBean':'foo,bar'}"</js>)
	 * 		.build();
	 * </p>
	 * @deprecated Use {@link Bean#bpwo()} and {@link BeanConfig#bpwo()}
	 */
	@Deprecated
	public static final String BEAN_bpwo = PREFIX + ".bpwo.sms";

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_debug BEAN_debug}
	 * 	<li><b>Name:</b>  <js>"BeanContext.debug.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.debug</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_DEBUG</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#debug()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#debug()}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanSessionArgs#debug(Boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * </p>
	 */
	public static final String BEAN_debug = PREFIX + ".debug.b";

	/**
	 * Configuration property:  POJO examples.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_examples BEAN_examples}
	 * 	<li><b>Name:</b>  <js>"BeanContext.examples.smo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,Object&gt;</c>
	 * 	<li><b>Default:</b>  <c>{}</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Example}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#example()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#examples()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#example(Class,Object)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies an example of the specified class.
	 *
	 * <p>
	 * Examples are used in cases such as POJO examples in Swagger documents.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that excludes the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.example(MyBean.<jk>class</jk>, <jk>new</jk> MyBean().foo(<js>"foo"</js>).bar(123))
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_examples</jsf>, MyBean.<jk>class</jk>.getName(), <jk>new</jk> MyBean().foo(<js>"foo"</js>).bar(123))
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * POJO examples can also be defined on classes via the following:
	 * <ul class='spaced-list'>
	 * 	<li>A static field annotated with {@link Example @Example}.
	 * 	<li>A static method annotated with {@link Example @Example} with zero arguments or one {@link BeanSession} argument.
	 * 	<li>A static method with name <c>example</c> with no arguments or one {@link BeanSession} argument.
	 * </ul>
	 */
	public static final String BEAN_examples = PREFIX + ".examples.smo";

	/**
	 * Configuration property:  Bean property excludes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #BEAN_bpx}
	 * </div>
	 */
	@Deprecated public static final String BEAN_excludeProperties = BEAN_bpx;

	/**
	 * Configuration property:  Find fluent setters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_fluentSetters BEAN_fluentSetters}
	 * 	<li><b>Name:</b>  <js>"BeanContext.fluentSetters.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.fluentSetters</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_FLUENTSETTERS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#fluentSetters()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#fluentSetters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#fluentSetters()}
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#fluentSetters(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#fluentSetters()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, fluent setters are detected on beans.
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
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that finds fluent setters.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.fluentSetters()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_fluentSetters</jsf>, <jk>true</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_fluentSetters = PREFIX + ".fluentSetters.b";

	/**
	 * Configuration property:  Ignore invocation errors on getters.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean getter methods will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that ignores bean getter exceptions.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.ingoreInvocationExceptionsOnGetters()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_ignoreInvocationExceptionsOnGetters</jsf>, <jk>true</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnGetters = PREFIX + ".ignoreInvocationExceptionsOnGetters.b";

	/**
	 * Configuration property:  Ignore invocation errors on setters.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean setter methods will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that ignores bean setter exceptions.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.ignoreInvocationExceptionsOnSetters()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_ignoreInvocationExceptionsOnSetters</jsf>, <jk>true</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnSetters = PREFIX + ".ignoreInvocationExceptionsOnSetters.b";

	/**
	 * Configuration property:  Ignore properties without setters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_ignorePropertiesWithoutSetters BEAN_ignorePropertiesWithoutSetters}
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignorePropertiesWithoutSetters.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.ignorePropertiesWithoutSetters</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_IGNOREPROPERTIESWITHOUTSETTERS</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignorePropertiesWithoutSetters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dontIgnorePropertiesWithoutSetters()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a bean property without a setter will silently be ignored.
	 * <br>Otherwise, a {@code RuntimeException} is thrown.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that throws an exception if a setter is not found but a getter is.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.ignorePropertiesWithoutSetters(<jk>false</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_ignorePropertiesWithoutSetters</jsf>, <jk>false</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_ignorePropertiesWithoutSetters = PREFIX + ".ignorePropertiesWithoutSetters.b";

	/**
	 * Configuration property:  Ignore transient fields.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_ignoreTransientFields BEAN_ignoreTransientFields}
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignoreTransientFields.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.ignoreTransientFields</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_IGNORETRANSIENTFIELDS</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreTransientFields()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dontIgnoreTransientFields()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, methods and fields marked as <jk>transient</jk> or annotated with {@link java.beans.Transient}
	 * will be ignored as bean properties.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that doesn't ignore transient fields.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.ignoreTransientFields(<jk>false</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_ignoreTransientFields</jsf>, <jk>false</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_ignoreTransientFields = PREFIX + ".ignoreTransientFields.b";

	/**
	 * Configuration property:  Ignore unknown properties.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a non-existent bean property will silently be ignored.
	 * <br>Otherwise, a {@code RuntimeException} is thrown.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that ignores missing bean properties.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.ignoreUnknownBeanProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_ignoreUnknownBeanProperties</jsf>, <jk>true</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_ignoreUnknownBeanProperties = PREFIX + ".ignoreUnknownBeanProperties.b";

	/**
	 * Configuration property:  Ignore unknown properties with null values.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_ignoreUnknownNullBeanProperties BEAN_ignoreUnknownNullBeanProperties}
	 * 	<li><b>Name:</b>  <js>"BeanContext.ignoreUnknownNullBeanProperties.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.ignoreUnknownNullBeanProperties</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_IGNOREUNKNOWNNULLBEANPROPERTIES</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreUnknownNullBeanProperties()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dontIgnoreUnknownNullBeanProperties()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, trying to set a <jk>null</jk> value on a non-existent bean property will silently be ignored.
	 * <br>Otherwise, a {@code RuntimeException} is thrown.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that throws an exception on an unknown property even if the value being set is null.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.ignoreUnknownNullBeanProperties(<jk>false</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_ignoreUnknownNullBeanProperties</jsf>, <jk>false</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_ignoreUnknownNullBeanProperties = PREFIX + ".ignoreUnknownNullBeanProperties.b";

	/**
	 * Configuration property:  Implementation classes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_implClasses BEAN_implClasses}
	 * 	<li><b>Name:</b>  <js>"BeanContext.implClasses.smc"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,Class&gt;</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#implClasses()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#implClasses(Map)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#implClass(Class, Class)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation class for the
	 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
	 * parse).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that instantiates MyBeanImpls when parsing MyBeanInterfaces.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.implClass(MyBeanInterface.<jk>class</jk>, MyBeanImpl.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_implClasses</jsf>, MyBeanInterface.<jk>class</jk>.getName(), MyBeanImpl.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_implClasses = PREFIX + ".implClasses.smc";

	/**
	 * Configuration property:  Bean property includes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #BEAN_bpi}
	 * </div>
	 */
	@Deprecated public static final String BEAN_includeProperties = BEAN_bpi;

	/**
	 * Configuration property:  Locale.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_locale BEAN_locale}
	 * 	<li><b>Name:</b>  <js>"BeanContext.locale.s"</js>
	 * 	<li><b>Data type:</b>  {@link java.util.Locale}
	 * 	<li><b>System property:</b>  <c>BeanContext.locale</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_LOCALE</c>
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the default locale for serializer and parser sessions.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that uses the specified locale if it's not passed in through session args.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.locale(Locale.<jsf>UK</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_locale</jsf>, Locale.<jsf>UK</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Define on session-args instead.</jc>
	 * 	SerializerSessionArgs sessionArgs = <jk>new</jk> SerializerSessionArgs().locale(Locale.<jsf>UK</jsf>);
	 * 	<jk>try</jk> (WriterSerializerSession session = s.createSession(sessionArgs)) {
	 * 		...
	 * 	}
	 * </p>
	 */
	public static final String BEAN_locale = PREFIX + ".locale.s";

	/**
	 * Configuration property:  Media type.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_mediaType BEAN_mediaType}
	 * 	<li><b>Name:</b>  <js>"BeanContext.mediaType.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.http.MediaType}
	 * 	<li><b>System property:</b>  <c>BeanContext.mediaType</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_MEDIATYPE</c>
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the default media type value for serializer and parser sessions.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that uses the specified media type if it's not passed in through session args.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.mediaType(MediaType.<jsf>JSON</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_mediaType</jsf>, MediaType.<jsf>JSON</jsf>)
	 * 		.build();
	 *
	 * <jc>// Define on session-args instead.</jc>
	 * 	SerializerSessionArgs sessionArgs = <jk>new</jk> SerializerSessionArgs().mediaType(MediaType.<jsf>JSON</jsf>);
	 * 	<jk>try</jk> (WriterSerializerSession session = s.createSession(sessionArgs)) {
	 * 		...
	 * 	}
	 * </p>
	 */
	public static final String BEAN_mediaType = PREFIX + ".mediaType.s";

	/**
	 * Configuration property:  Bean class exclusions.
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
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanClasses(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanClasses(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanClassesReplace(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanClassesReplace(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanClassesRemove(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanClassesRemove(Object...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * List of classes that should not be treated as beans even if they appear to be bean-like.
	 * <br>Not-bean classes are converted to <c>Strings</c> during serialization.
	 *
	 * <p>
	 * Values can consist of any of the following types:
	 * <ul>
	 * 	<li>Classes.
	 * 	<li>Arrays and collections of classes.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that doesn't treat MyBean as a bean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.notBeanClasses(MyBean.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_notBeanClasses</jsf>, MyBean.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_notBeanClasses = PREFIX + ".notBeanClasses.sc";

	/**
	 * Configuration property:  Add to classes that should not be considered beans.
	 */
	public static final String BEAN_notBeanClasses_add = PREFIX + ".notBeanClasses.sc/add";

	/**
	 * Configuration property:  Remove from classes that should not be considered beans.
	 */
	public static final String BEAN_notBeanClasses_remove = PREFIX + ".notBeanClasses.sc/remove";

	/**
	 * Configuration property:  Bean package exclusions.
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
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanPackages(String...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanPackagesReplace(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanPackagesReplace(String...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanPackagesRemove(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#notBeanPackagesRemove(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When specified, the current list of ignore packages are appended to.
	 *
	 * <p>
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
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that ignores beans in the specified packages.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.notBeanPackages(<js>"org.apache.foo"</js>, <js>"org.apache.bar.*"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_notBeanPackages</jsf>, <js>"org.apache.foo"</js>)
	 * 		.addTo(<jsf>BEAN_notBeanPackages</jsf>, <js>"org.apache.bar.*"</js>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_notBeanPackages = PREFIX + ".notBeanPackages.ss";

	/**
	 * Configuration property:  Add to packages whose classes should not be considered beans.
	 */
	public static final String BEAN_notBeanPackages_add = PREFIX + ".notBeanPackages.ss/add";

	/**
	 * Configuration property:  Remove from packages whose classes should not be considered beans.
	 */
	public static final String BEAN_notBeanPackages_remove = PREFIX + ".notBeanPackages.ss/remove";

	/**
	 * Configuration property:  POJO swaps.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_pojoSwaps BEAN_pojoSwaps}
	 * 	<li><b>Name:</b>  <js>"BeanContext.pojoSwaps.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;Object&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Swap}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Swaps}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#pojoSwaps()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#pojoSwaps(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#pojoSwaps(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#pojoSwapsReplace(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#pojoSwapsReplace(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#pojoSwapsRemove(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#pojoSwapsRemove(Class...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * POJO swaps are used to "swap out" non-serializable classes with serializable equivalents during serialization,
	 * and "swap in" the non-serializable class during parsing.
	 *
	 * <p>
	 * An example of a POJO swap would be a <c>Calendar</c> object that gets swapped out for an ISO8601 string.
	 *
	 * <p>
	 * Multiple POJO swaps can be associated with a single class.
	 * <br>When multiple swaps are applicable to the same class, the media type pattern defined by
	 * {@link PojoSwap#forMediaTypes()} or {@link Swap#mediaTypes() @Swap(mediaTypes)} are used to come up with the best match.
	 *
	 * <p>
	 * Values can consist of any of the following types:
	 * <ul>
	 * 	<li>Any subclass of {@link PojoSwap}.
	 * 	<li>Any instance of {@link PojoSwap}.
	 * 	<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 	<li>Any array or collection of the objects above.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * 	<jc>// Create a serializer that uses our date swap.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.pojoSwaps(MyDateSwap.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_pojoSwaps</jsf>, MyDateSwap.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Produces "{date:'2012-03-03T04:05:06-0500'}"</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a serializer that uses our date swap.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.pojoSwaps(MyDateSwap.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Use our parser to parse a bean.</jc>
	 * 	MyBean bean = p.parse(json, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.PojoSwaps}
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.PerMediaTypePojoSwaps}
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.OneWayPojoSwaps}
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.SwapAnnotation}
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.AutoPojoSwaps}
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.SurrogateClasses}
	 * </ul>
	 */
	public static final String BEAN_pojoSwaps = PREFIX + ".pojoSwaps.lo";

	/**
	 * Configuration property:  Add to POJO swap classes.
	 */
	public static final String BEAN_pojoSwaps_add = PREFIX + ".pojoSwaps.lo/add";

	/**
	 * Configuration property:  Remove from POJO swap classes.
	 */
	public static final String BEAN_pojoSwaps_remove = PREFIX + ".pojoSwaps.lo/remove";

	/**
	 * Configuration property:  Bean property namer.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_propertyNamer BEAN_propertyNamer}
	 * 	<li><b>Name:</b>  <js>"BeanContext.propertyNamer.c"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;{@link org.apache.juneau.PropertyNamer}&gt;</code>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.PropertyNamerDefault}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.Bean#propertyNamer()}
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#propertyNamer()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#propertyNamer(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#propertyNamer(Class)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that uses Dashed-Lower-Case property names.</jc>
	 * 	<jc>// (e.g. "foo-bar-url" instead of "fooBarURL")</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.propertyNamer(PropertyNamerDLC.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_propertyNamer</jsf>, PropertyNamerDLC.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_propertyNamer = PREFIX + ".propertyNamer.c";

	/**
	 * Configuration property:  Sort bean properties.
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
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#sortProperties(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#sortProperties()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * <br>Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 * <br>On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
	 * <br>On Oracle JVMs, the bean properties are not ordered (which follows the official JVM specs).
	 *
	 * <p>
	 * This property is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
	 * to force bean properties to be in a particular order and can just alter the order of the fields/methods
	 * in the Java file.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that sorts bean properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sortProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_sortProperties</jsf>, <jk>true</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_sortProperties = PREFIX + ".sortProperties.b";

	/**
	 * Configuration property:  Time zone.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_timeZone BEAN_timeZone}
	 * 	<li><b>Name:</b>  <js>"BeanContext.timeZone.s"</js>
	 * 	<li><b>Data type:</b>  {@link java.util.TimeZone}
	 * 	<li><b>System property:</b>  <c>BeanContext.timeZone</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_TIMEZONE</c>
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the default timezone for serializer and parser sessions.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that uses GMT if the timezone is not specified in the session args.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.timeZone(TimeZone.<jsf>GMT</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_timeZone</jsf>, TimeZone.<jsf>GMT</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Define on session-args instead.</jc>
	 * 	SerializerSessionArgs sessionArgs = <jk>new</jk> SerializerSessionArgs().timeZone(TimeZone.<jsf>GMT</jsf>);
	 * 	<jk>try</jk> (WriterSerializerSession ss = JsonSerializer.<jsf>DEFAULT</jsf>.createSession(sessionArgs)) {
	 * 		String json = s.serialize(<jk>new</jk> MyBean());
	 * 	}
	 * </p>
	 */
	public static final String BEAN_timeZone = PREFIX + ".timeZone.s";

	/**
	 * Configuration property:  Use enum names.
	 *
	 * <h5 class='section'>Property:</h5>
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, enums are always serialized by name, not using {@link Object#toString()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer with debug enabled.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.useEnumNames()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_useEnumNames</jsf>, <jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Enum with overridden toString().</jc>
	 * 	<jc>// Will be serialized as ONE/TWO/THREE even though there's a toString() method.</jc>
	 * 	<jk>public enum</jk> Option {
	 * 		<jsf>ONE</jsf>(1),
	 * 		<jsf>TWO</jsf>(2),
	 * 		<jsf>THREE</jsf>(3);
	 *
	 * 		<jk>private int</jk> <jf>i</jf>;
	 *
	 * 		Option(<jk>int</jk> i) {
	 * 			<jk>this</jk>.<jf>i</jf> = i;
	 * 		}
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> String toString() {
	 * 			<jk>return</jk> String.<jsm>valueOf</jsm>(<jf>i</jf>);
	 * 		}
	 * </p>
	 */
	public static final String BEAN_useEnumNames = PREFIX + ".useEnumNames.b";

	/**
	 * Configuration property:  Use interface proxies.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanContext#BEAN_useInterfaceProxies BEAN_useInterfaceProxies}
	 * 	<li><b>Name:</b>  <js>"BeanContext.useInterfaceProxies.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanContext.useInterfaceProxies</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANCONTEXT_USEINTERFACEPROXIES</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#useInterfaceProxies()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanContextBuilder#dontUseInterfaceProxies()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then interfaces will be instantiated as proxy classes through the use of an
	 * {@link InvocationHandler} if there is no other way of instantiating them.
	 * <br>Otherwise, throws a {@link BeanRuntimeException}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that doesn't try to make interface proxies.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.useInterfaceProxies(<jk>false</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_useInterfaceProxies</jsf>, <jk>false</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String BEAN_useInterfaceProxies = PREFIX + ".useInterfaceProxies.b";

	/**
	 * Configuration property:  Use Java Introspector.
	 *
	 * <h5 class='section'>Property:</h5>
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 * <br>Most {@link Bean @Bean} annotations will be ignored.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that only uses the built-in java bean introspector for finding properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.useJavaBeanIntrospector(<jk>false</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_useJavaBeanIntrospector</jsf>, <jk>false</jk>)
	 * 		.build();
	 * </p>
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
	private static final ConcurrentHashMap<Integer,Map<Class,ClassMeta>> cmCacheCache
		= new ConcurrentHashMap<>();

	/** Default config.  All default settings. */
	public static final BeanContext DEFAULT = BeanContext.create().build();

	/** Default config.  All default settings except sort bean properties. */
	public static final BeanContext DEFAULT_SORTED = BeanContext.create().sortProperties().build();

	private final boolean
		beansRequireDefaultConstructor,
		beansRequireSerializable,
		beansRequireSettersForGetters,
		beansRequireSomeProperties,
		beanMapPutReturnsOldValue,
		useInterfaceProxies,
		ignoreUnknownBeanProperties,
		ignoreUnknownNullBeanProperties,
		ignorePropertiesWithoutSetters,
		ignoreTransientFields,
		ignoreInvocationExceptionsOnGetters,
		ignoreInvocationExceptionsOnSetters,
		useJavaBeanIntrospector,
		useEnumNames,
		sortProperties,
		fluentSetters,
		debug;

	private final Visibility
		beanConstructorVisibility,
		beanClassVisibility,
		beanMethodVisibility,
		beanFieldVisibility;

	private final Class<?>[] notBeanClasses;
	private final List<Class<?>> beanDictionaryClasses;
	private final String[] notBeanPackageNames, notBeanPackagePrefixes;
	private final BeanFilter[] beanFilters;
	private final PojoSwap<?,?>[] pojoSwaps;
	private final Map<String,?> examples;
	private final BeanRegistry beanRegistry;
	private final Map<String,ClassInfo> implClasses;
	private final Locale locale;
	private final TimeZone timeZone;
	private final MediaType mediaType;
	private final Map<String,Set<String>> bpi, bpx, bpro, bpwo;
	private final PropertyNamer propertyNamer;
	private final String beanTypePropertyName;
	private final int beanHashCode;
	private final ReflectionMap<Annotation> annotations;

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
	 * @param ps The property store containing the unmodifiable configuration for this bean context.
	 */
	public BeanContext(PropertyStore ps) {
		super(ps, true);

		if (ps == null)
			ps = PropertyStore.DEFAULT;


		ReflectionMap.Builder<Annotation> rmb = ReflectionMap.create(Annotation.class);
		for (Annotation a : ps.getListProperty(BEAN_annotations, Annotation.class)) {
			try {
				Method m = a.getClass().getMethod("on");
				m.setAccessible(true);
				String on = (String)m.invoke(a);
				rmb.append(on, a);
			} catch (NoSuchMethodException e) {
				throw new ConfigException("Invalid annotation @{0} used in BEAN_annotations property.  Annotation must define an on() method.", a.getClass().getSimpleName());
			} catch (Exception e) {
				throw new ConfigException(e, "Invalid annotation @{0} used in BEAN_annotations property.", a.getClass().getName());
			}
		}
		this.annotations = rmb.build();

		beanHashCode = ps.hashCode("BeanContext");

		beansRequireDefaultConstructor = getBooleanProperty(BEAN_beansRequireDefaultConstructor, false);
		beansRequireSerializable = getBooleanProperty(BEAN_beansRequireSerializable, false);
		beansRequireSettersForGetters = getBooleanProperty(BEAN_beansRequireSettersForGetters, false);
		beansRequireSomeProperties = getBooleanProperty(BEAN_beansRequireSomeProperties, true);
		beanMapPutReturnsOldValue = getBooleanProperty(BEAN_beanMapPutReturnsOldValue, false);
		useEnumNames = getBooleanProperty(BEAN_useEnumNames, false);
		useInterfaceProxies = getBooleanProperty(BEAN_useInterfaceProxies, true);
		ignoreUnknownBeanProperties = getBooleanProperty(BEAN_ignoreUnknownBeanProperties, false);
		ignoreUnknownNullBeanProperties = getBooleanProperty(BEAN_ignoreUnknownNullBeanProperties, true);
		ignorePropertiesWithoutSetters = getBooleanProperty(BEAN_ignorePropertiesWithoutSetters, true);
		ignoreTransientFields = getBooleanProperty(BEAN_ignoreTransientFields, true);
		ignoreInvocationExceptionsOnGetters = getBooleanProperty(BEAN_ignoreInvocationExceptionsOnGetters, false);
		ignoreInvocationExceptionsOnSetters = getBooleanProperty(BEAN_ignoreInvocationExceptionsOnSetters, false);
		useJavaBeanIntrospector = getBooleanProperty(BEAN_useJavaBeanIntrospector, false);
		sortProperties = getBooleanProperty(BEAN_sortProperties, false);
		fluentSetters = getBooleanProperty(BEAN_fluentSetters, false);
		beanTypePropertyName = getStringProperty(BEAN_beanTypePropertyName, "_type");
		debug = getBooleanProperty(BEAN_debug, false);

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
		for (Object o : getListProperty(BEAN_beanFilters, Object[].class)) {
			ClassInfo ci = o instanceof Class ? ClassInfo.of((Class)o) : ClassInfo.of(o);
			if (ci.isChildOf(BeanFilter.class))
				lbf.add(castOrCreate(BeanFilter.class, o));
			else if (ci.isChildOf(BeanFilterBuilder.class))
				lbf.add(castOrCreate(BeanFilterBuilder.class, o).build());
			else if (o instanceof Class)
				lbf.add(new InterfaceBeanFilterBuilder((Class<?>)o, this).build());
		}
		beanFilters = lbf.toArray(new BeanFilter[0]);

		LinkedList<PojoSwap<?,?>> lpf = new LinkedList<>();
		for (Object o : getListProperty(BEAN_pojoSwaps, Object.class)) {
			if (o instanceof Class) {
				ClassInfo ci = ClassInfo.of((Class<?>)o);
				if (ci.isChildOf(PojoSwap.class))
					lpf.add(castOrCreate(PojoSwap.class, ci.inner()));
				else if (ci.isChildOf(Surrogate.class))
					lpf.addAll(SurrogateSwap.findPojoSwaps(ci.inner(), this));
				else
					throw new BasicRuntimeException("Invalid class {0} specified in BeanContext.pojoSwaps property.  Must be a subclass of PojoSwap or Surrogate.", ci.inner());
			} else if (o instanceof PojoSwap) {
				lpf.add((PojoSwap)o);
			}
		}
		pojoSwaps = lpf.toArray(new PojoSwap[lpf.size()]);

		examples = getMapProperty(BEAN_examples, Object.class);

		AMap<String,ClassInfo> icm = AMap.of();
		for (Map.Entry<String,Class<?>> e : getClassMapProperty(BEAN_implClasses).entrySet())
			icm.put(e.getKey(), ClassInfo.of(e.getValue()));
		implClasses = icm.unmodifiable();

		AMap<String,Set<String>> m2 = AMap.of();
		for (Map.Entry<String,String> e : getMapProperty(BEAN_bpi, String.class).entrySet())
			m2.put(e.getKey(), ASet.of(split(e.getValue())).unmodifiable());
		bpi = m2.unmodifiable();

		m2 = AMap.of();
		for (Map.Entry<String,String> e : getMapProperty(BEAN_bpx, String.class).entrySet())
			m2.put(e.getKey(), ASet.of(split(e.getValue())).unmodifiable());
		bpx = m2.unmodifiable();

		m2 = AMap.of();
		for (Map.Entry<String,String> e : getMapProperty(BEAN_bpro, String.class).entrySet())
			m2.put(e.getKey(), ASet.of(split(e.getValue())).unmodifiable());
		bpro = m2.unmodifiable();

		m2 = AMap.of();
		for (Map.Entry<String,String> e : getMapProperty(BEAN_bpwo, String.class).entrySet())
			m2.put(e.getKey(), ASet.of(split(e.getValue())).unmodifiable());
		bpwo = m2.unmodifiable();

		locale = getInstanceProperty(BEAN_locale, Locale.class, Locale.getDefault());
		timeZone = getInstanceProperty(BEAN_timeZone, TimeZone.class, null);
		mediaType = getInstanceProperty(BEAN_mediaType, MediaType.class, null);

		if (! cmCacheCache.containsKey(beanHashCode)) {
			ConcurrentHashMap<Class,ClassMeta> cm = new ConcurrentHashMap<>();
			cm.putIfAbsent(String.class, new ClassMeta(String.class, this, null, null, findPojoSwaps(String.class), findChildPojoSwaps(String.class), findExample(String.class)));
			cm.putIfAbsent(Object.class, new ClassMeta(Object.class, this, null, null, findPojoSwaps(Object.class), findChildPojoSwaps(Object.class), findExample(Object.class)));
			cmCacheCache.putIfAbsent(beanHashCode, cm);
		}
		cmCache = cmCacheCache.get(beanHashCode);
		cmString = cmCache.get(String.class);
		cmObject = cmCache.get(Object.class);
		cmClass = cmCache.get(Class.class);

		beanDictionaryClasses = AList.unmodifiable(getClassArrayProperty(BEAN_beanDictionary));
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
	 * 	If <jk>true</jk>, wait for the ClassMeta constructor to finish before returning.
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
					cm = new ClassMeta<>(type, this, findImplClass(type), findBeanFilter(type), findPojoSwaps(type), findChildPojoSwaps(type), findExample(type));
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
	 * @param px The property annotation on the type if there is one.
	 * @param p The property annotation on the type if there is one.
	 * @param t The type.
	 * @param typeVarImpls
	 * 	Contains known resolved type parameters on the specified class so that we can result
	 * 	{@code ParameterizedTypes} and {@code TypeVariables}.
	 * 	Can be <jk>null</jk> if the information is not known.
	 * @return The new {@code ClassMeta} object wrapped around the {@code Type} object.
	 */
	@SuppressWarnings("deprecation")
	protected final <T> ClassMeta<T> resolveClassMeta(BeanProperty px, Beanp p, Type t, Map<Class<?>,Class<?>[]> typeVarImpls) {
		ClassMeta<T> cm = resolveClassMeta(t, typeVarImpls);
		ClassMeta<T> cm2 = cm;
		if (px != null) {

			if (px.type() != Object.class)
				cm2 = resolveClassMeta(px.type(), typeVarImpls);

			if (cm2.isMap()) {
				Class<?>[] pParams = (px.params().length == 0 ? new Class[]{Object.class, Object.class} : px.params());
				if (pParams.length != 2)
					throw new BasicRuntimeException("Invalid number of parameters specified for Map (must be 2): {0}", pParams.length);
				ClassMeta<?> keyType = resolveType(pParams[0], cm2.getKeyType(), cm.getKeyType());
				ClassMeta<?> valueType = resolveType(pParams[1], cm2.getValueType(), cm.getValueType());
				if (keyType.isObject() && valueType.isObject())
					return cm2;
				return new ClassMeta<>(cm2, keyType, valueType, null);
			}

			if (cm2.isCollection() || cm2.isOptional()) {
				Class<?>[] pParams = (px.params().length == 0 ? new Class[]{Object.class} : px.params());
				if (pParams.length != 1)
					throw new BasicRuntimeException("Invalid number of parameters specified for "+(cm2.isCollection() ? "Collection" : cm2.isOptional() ? "Optional" : "Array")+" (must be 1): {0}", pParams.length);
				ClassMeta<?> elementType = resolveType(pParams[0], cm2.getElementType(), cm.getElementType());
				if (elementType.isObject())
					return cm2;
				return new ClassMeta<>(cm2, null, null, elementType);
			}

			return cm2;
		}

		if (p != null) {

			if (p.type() != Object.class)
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
					throw new BasicRuntimeException("Invalid number of parameters specified for "+(cm2.isCollection() ? "Collection" : cm2.isOptional() ? "Optional" : "Array")+" (must be 1): {0}", pParams.length);
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
				if (f.getNormalClass().isParentOf(c))
					l.add(f);
			return l.size() == 0 ? null : l.toArray(new PojoSwap[l.size()]);
		}
		return null;
	}

	private final Object findExample(Class<?> c) {
		if (c != null) {
			Object o = examples.get(c.getName());
			if (o != null)
				return o;
			o = examples.get(c.getSimpleName());
			if (o != null)
				return o;
			Class<?> c2 = findImplClass(c);
			if (c2 == null)
				return null;
			o = examples.get(c2.getName());
			if (o != null)
				return o;
			o = examples.get(c2.getSimpleName());
			return o;
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
			if (f.getNormalClass().isChildOf(c)) {
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
				if (ClassInfo.of(f.getBeanClass()).isParentOf(c))
					return f;
		return null;
	}

	/**
	 * Gets the no-arg constructor for the specified class.
	 *
	 * @param <T> The class to check.
	 * @param c The class to check.
	 * @param v The minimum visibility for the constructor.
	 * @return The no arg constructor, or <jk>null</jk> if the class has no no-arg constructor.
	 */
	protected final <T> ConstructorInfo getImplClassConstructor(Class<T> c, Visibility v) {
		if (implClasses.isEmpty())
			return null;
		Class cc = c;
		while (cc != null) {
			ClassInfo implClass = implClasses.get(cc.getName());
			if (implClass != null)
				return implClass.getNoArgConstructor(v);
			for (Class ic : cc.getInterfaces()) {
				implClass = implClasses.get(ic.getName());
				if (implClass != null)
					return implClass.getNoArgConstructor(v);
			}
			cc = cc.getSuperclass();
		}
		return null;
	}

	private final <T> Class<T> findImplClass(Class<T> c) {
		if (implClasses.isEmpty())
			return null;
		Class cc = c;
		while (cc != null) {
			ClassInfo implClass = implClasses.get(cc.getName());
			if (implClass != null)
				return implClass.<T>inner();
			for (Class ic : cc.getInterfaces()) {
				implClass = implClasses.get(ic.getName());
				if (implClass != null)
					return implClass.<T>inner();
			}
			cc = cc.getSuperclass();
		}
		return null;
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
			A x = c.getAnnotation(a);
			AList<Annotation> l = new AList<>(x == null ? 0 : 1);
			l.aifnn(x);
			annotations.appendAll(c, a, l);
			aa = l.unmodifiable();
			classAnnotationCache.put(c, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, ClassInfo c) {
		return getAnnotations(a, c == null ? null : c.inner());
	}

	/**
	 * Finds the last specified annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, Class<?> c) {
		return last(getAnnotations(a, c));
	}

	/**
	 * Finds the last specified annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, ClassInfo c) {
		return last(getAnnotations(a, c));
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
			A x = c.getDeclaredAnnotation(a);
			AList<Annotation> l = new AList<>(x == null ? 0 : 1);
			l.aifnn(x);
			annotations.appendAll(c, a, l);
			aa = l.unmodifiable();
			declaredClassAnnotationCache.put(c, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified declared annotation on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getDeclaredAnnotations(Class<A> a, ClassInfo c) {
		return getDeclaredAnnotations(a, c == null ? null : c.inner());
	}


	/**
	 * Finds the last specified declared annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastDeclaredAnnotation(Class<A> a, Class<?> c) {
		return last(getDeclaredAnnotations(a, c));
	}

	/**
	 * Finds the last specified declared annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastDeclaredAnnotation(Class<A> a, ClassInfo c) {
		return last(getDeclaredAnnotations(a, c));
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
			A x = m.getAnnotation(a);
			AList<Annotation> l = new AList<>(x == null ? 0 : 1);
			l.aifnn(x);
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
			A x = f.getAnnotation(a);
			AList<Annotation> l = new AList<>(x == null ? 0 : 1);
			l.aifnn(x);
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
	 * Finds the last specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, Field f) {
		return last(getAnnotations(a, f));
	}

	/**
	 * Finds the last specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, FieldInfo f) {
		return last(getAnnotations(a, f));
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
			A x = c.getAnnotation(a);
			AList<Annotation> l = new AList(x == null ? 0 : 1);
			l.aifnn(x);
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
	 * Finds the last specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, Constructor<?> c) {
		return last(getAnnotations(a, c));
	}

	/**
	 * Finds the last specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, ConstructorInfo c) {
		return last(getAnnotations(a, c));
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
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The class being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified class.
	 */
	public <A extends Annotation> boolean hasDeclaredAnnotation(Class<A> a, Class<?> c) {
		return getDeclaredAnnotations(a, c).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getDeclaredAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The class being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified class.
	 */
	public <A extends Annotation> boolean hasDeclaredAnnotation(Class<A> a, ClassInfo c) {
		return getDeclaredAnnotations(a, c == null ? null : c.inner()).size() > 0;
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
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, Field f) {
		return getAnnotations(a, f).size() > 0;
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
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, Constructor<?> c) {
		return getAnnotations(a, c).size() > 0;
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
	 * Configuration property:  Minimum bean class visibility.
	 *
	 * @see #BEAN_beanClassVisibility
	 * @return
	 * 	Classes are not considered beans unless they meet the minimum visibility requirements.
	 */
	public final Visibility getBeanClassVisibility() {
		return beanClassVisibility;
	}

	/**
	 * Configuration property:  Minimum bean constructor visibility.
	 *
	 * @see #BEAN_beanConstructorVisibility
	 * @return
	 * 	Only look for constructors with this specified minimum visibility.
	 */
	public final Visibility getBeanConstructorVisibility() {
		return beanConstructorVisibility;
	}

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * @see #BEAN_beanDictionary
	 * @return
	 * 	The list of classes that make up the bean dictionary in this bean context.
	 */
	protected final List<Class<?>> getBeanDictionaryClasses() {
		return beanDictionaryClasses;
	}

	/**
	 * Configuration property:  Minimum bean field visibility.
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
	 * Configuration property:  Bean filters.
	 *
	 *
	 * @see BeanContext#BEAN_beanFilters
	 * @return
	 * 	Only look for bean fields with this specified minimum visibility.
	 */
	protected final BeanFilter[] getBeanFilters() {
		return beanFilters;
	}

	/**
	 * Configuration property:  BeanMap.put() returns old property value.
	 *
	 * @see #BEAN_beanMapPutReturnsOldValue
	 * @return
	 * 	<jk>true</jk> if the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property values.
	 * 	<br>Otherwise, it returns <jk>null</jk>.
	 */
	protected final boolean isBeanMapPutReturnsOldValue() {
		return beanMapPutReturnsOldValue;
	}

	/**
	 * Configuration property:  Minimum bean method visibility.
	 *
	 * @see #BEAN_beanMethodVisibility
	 * @return
	 * 	Only look for bean methods with this specified minimum visibility.
	 */
	public final Visibility getBeanMethodVisibility() {
		return beanMethodVisibility;
	}

	/**
	 * Configuration property:  Beans require no-arg constructors.
	 *
	 * @see #BEAN_beansRequireDefaultConstructor
	 * @return
	 * 	<jk>true</jk> if a Java class must implement a default no-arg constructor to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	protected final boolean isBeansRequireDefaultConstructor() {
		return beansRequireDefaultConstructor;
	}

	/**
	 * Configuration property:  Beans require Serializable interface.
	 *
	 * @see #BEAN_beansRequireSerializable
	 * @return
	 * 	<jk>true</jk> if a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	protected final boolean isBeansRequireSerializable() {
		return beansRequireSerializable;
	}

	/**
	 * Configuration property:  Beans require setters for getters.
	 *
	 * @see #BEAN_beansRequireSettersForGetters
	 * @return
	 * 	<jk>true</jk> if only getters that have equivalent setters will be considered as properties on a bean.
	 * 	<br>Otherwise, they are ignored.
	 */
	protected final boolean isBeansRequireSettersForGetters() {
		return beansRequireSettersForGetters;
	}

	/**
	 * Configuration property:  Beans require at least one property.
	 *
	 * @see #BEAN_beansRequireSomeProperties
	 * @return
	 * 	<jk>true</jk> if a Java class must contain at least 1 property to be considered a bean.
	 * 	<br>Otherwise, the bean is serialized as a string using the {@link Object#toString()} method.
	 */
	protected final boolean isBeansRequireSomeProperties() {
		return beansRequireSomeProperties;
	}

	/**
	 * Configuration property:  Bean type property name.
	 *
	 * @see #BEAN_beanTypePropertyName
	 * @return
	 * The name of the bean property used to store the dictionary name of a bean type so that the parser knows the data type to reconstruct.
	 */
	protected final String getBeanTypePropertyName() {
		return beanTypePropertyName;
	}

	/**
	 * Configuration property:  Bean property includes.
	 *
	 * @see #BEAN_bpi
	 * @return
	 * 	Include properties keyed by class name.
	 */
	protected final Map<String,Set<String>> getBpi() {
		return bpi;
	}

	/**
	 * Returns the {@link #BEAN_bpi} setting for the specified class.
	 *
	 * @param c The class.
	 * @return The properties to include for the specified class, or an empty set if it's not defined for the class.
	 */
	protected Set<String> getBpi(Class<?> c) {
		if (bpi.isEmpty())
			return emptySet();
		ClassInfo ci = ClassInfo.of(c);
		for (ClassInfo c2 : ci.getAllParentsChildFirst()) {
			for (String n : c2.getNames()) {
				Set<String> s = bpi.get(n);
				if (s != null)
					return s;
			}
		}
		if (bpi.containsKey("*"))
			return bpi.get("*");
		return emptySet();
	}

	/**
	 * Configuration property:  Bean property excludes.
	 *
	 * @see #BEAN_bpx
	 * @return
	 * 	The list of property names to exclude keyed by class name.
	 */
	protected final Map<String,Set<String>> getBpx() {
		return bpx;
	}

	/**
	 * Returns the {@link #BEAN_bpx} setting for the specified class.
	 *
	 * @param c The class.
	 * @return The properties to exclude for the specified class, or an empty set if it's not defined for the class.
	 */
	protected Set<String> getBpx(Class<?> c) {
		if (bpx.isEmpty())
			return emptySet();
		ClassInfo ci = ClassInfo.of(c);
		for (ClassInfo c2 : ci.getAllParentsChildFirst()) {
			for (String n : c2.getNames()) {
				Set<String> s = bpx.get(n);
				if (s != null)
					return s;
			}
		}
		if (bpx.containsKey("*"))
			return bpx.get("*");
		return emptySet();
	}

	/**
	 * Configuration property:  Read-only bean properties.
	 *
	 * @see #BEAN_bpro
	 * @return
	 * 	The list of read-only property names keyed by class name.
	 */
	protected final Map<String,Set<String>> getBpro() {
		return bpro;
	}

	/**
	 * Returns the {@link #BEAN_bpro} setting for the specified class.
	 *
	 * @param c The class.
	 * @return The read-only properties for the specified class, or an empty set if it's not defined for the class.
	 */
	protected Set<String> getBpro(Class<?> c) {
		if (bpro.isEmpty())
			return emptySet();
		ClassInfo ci = ClassInfo.of(c);
		for (ClassInfo c2 : ci.getAllParentsChildFirst()) {
			for (String n : c2.getNames()) {
				Set<String> s = bpro.get(n);
				if (s != null)
					return s;
			}
		}
		if (bpro.containsKey("*"))
			return bpro.get("*");
		return emptySet();
	}

	/**
	 * Configuration property:  Write-only bean properties.
	 *
	 * @see #BEAN_bpwo
	 * @return
	 * 	The list of write-only property names keyed by class name.
	 */
	protected final Map<String,Set<String>> getBpwo() {
		return bpwo;
	}

	/**
	 * Returns the {@link #BEAN_bpwo} setting for the specified class.
	 *
	 * @param c The class.
	 * @return The write-only properties for the specified class, or an empty set if it's not defined for the class.
	 */
	protected Set<String> getBpwo(Class<?> c) {
		if (bpwo.isEmpty())
			return emptySet();
		ClassInfo ci = ClassInfo.of(c);
		for (ClassInfo c2 : ci.getAllParentsChildFirst()) {
			for (String n : c2.getNames()) {
				Set<String> s = bpwo.get(n);
				if (s != null)
					return s;
			}
		}
		if (bpwo.containsKey("*"))
			return bpwo.get("*");
		return emptySet();
	}

	/**
	 * Configuration property:  Debug mode.
	 *
	 * @see #BEAN_debug
	 * @return
	 * 	<jk>true</jk> if debug mode is enabled.
	 */
	protected boolean isDebug() {
		return debug;
	}

	/**
	 * Configuration property:  POJO examples.
	 *
	 * @see #BEAN_examples
	 * @return
	 * 	A map of POJO examples keyed by class name.
	 */
	protected final Map<String,?> getExamples() {
		return examples;
	}

	/**
	 * Configuration property:  Find fluent setters.
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 *
	 * @see #BEAN_fluentSetters
	 * @return
	 * 	<jk>true</jk> if fluent setters are detected on beans.
	 */
	protected final boolean isFluentSetters() {
		return fluentSetters;
	}

	/**
	 * Configuration property:  Ignore invocation errors on getters.
	 *
	 * @see #BEAN_ignoreInvocationExceptionsOnGetters
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean getter methods are silently ignored.
	 */
	protected final boolean isIgnoreInvocationExceptionsOnGetters() {
		return ignoreInvocationExceptionsOnGetters;
	}

	/**
	 * Configuration property:  Ignore invocation errors on setters.
	 *
	 * @see #BEAN_ignoreInvocationExceptionsOnSetters
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean setter methods are silently ignored.
	 */
	protected final boolean isIgnoreInvocationExceptionsOnSetters() {
		return ignoreInvocationExceptionsOnSetters;
	}

	/**
	 * Configuration property:  Ignore properties without setters.
	 *
	 * @see #BEAN_ignorePropertiesWithoutSetters
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a bean property without a setter is silently ignored.
	 */
	protected final boolean isIgnorePropertiesWithoutSetters() {
		return ignorePropertiesWithoutSetters;
	}

	/**
	 * Configuration property:  Ignore transient fields.
	 *
	 * @see #BEAN_ignoreTransientFields
	 * @return
	 * 	<jk>true</jk> if fields and methods marked as transient should be ignored.
	 */
	protected final boolean isIgnoreTransientFields() {
		return ignoreTransientFields;
	}

	/**
	 * Configuration property:  Ignore unknown properties.
	 *
	 * @see #BEAN_ignoreUnknownBeanProperties
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a non-existent bean property is silently ignored.
	 * 	<br>Otherwise, a {@code RuntimeException} is thrown.
	 */
	protected final boolean isIgnoreUnknownBeanProperties() {
		return ignoreUnknownBeanProperties;
	}

	/**
	 * Configuration property:  Ignore unknown properties with null values.
	 *
	 * @see #BEAN_ignoreUnknownNullBeanProperties
	 * @return
	 * 	<jk>true</jk> if trying to set a <jk>null</jk> value on a non-existent bean property is silently ignored.
	 */
	protected final boolean isIgnoreUnknownNullBeanProperties() {
		return ignoreUnknownNullBeanProperties;
	}

	/**
	 * Configuration property:  Implementation classes.
	 *
	 * @see #BEAN_implClasses
	 * @return
	 * 	Implementation classes keyed by interface class names.
	 */
	protected final Map<String,ClassInfo> getImplClasses() {
		return implClasses;
	}

	/**
	 * Configuration property:  Locale.
	 *
	 * @see #BEAN_locale
	 * @return
	 * 	The default locale for serializer and parser sessions.
	 */
	protected final Locale getLocale() {
		return locale;
	}

	/**
	 * Configuration property:  Media type.
	 *
	 * @see #BEAN_mediaType
	 * @return
	 * 	The default media type value for serializer and parser sessions.
	 */
	protected final MediaType getMediaType() {
		return mediaType;
	}

	/**
	 * Configuration property:  Bean class exclusions.
	 *
	 * @see #BEAN_notBeanClasses
	 * @return
	 * 	The list of classes that are explicitly not beans.
	 */
	protected final Class<?>[] getNotBeanClasses() {
		return notBeanClasses;
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * @see #BEAN_notBeanPackages
	 * @return
	 * 	The list of fully-qualified package names to exclude from being classified as beans.
	 */
	protected final String[] getNotBeanPackagesNames() {
		return notBeanPackageNames;
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * @see #BEAN_notBeanPackages
	 * @return
	 * 	The list of package name prefixes to exclude from being classified as beans.
	 */
	protected final String[] getNotBeanPackagesPrefixes() {
		return notBeanPackagePrefixes;
	}

	/**
	 * Configuration property:  POJO swaps.
	 *
	 * @see #BEAN_pojoSwaps
	 * @return
	 * 	The list POJO swaps defined.
	 */
	protected final PojoSwap<?,?>[] getPojoSwaps() {
		return pojoSwaps;
	}

	/**
	 * Configuration property:  Bean property namer.
	 *
	 * @see #BEAN_propertyNamer
	 * @return
	 * 	The interface used to calculate bean property names.
	 */
	protected final PropertyNamer getPropertyNamer() {
		return propertyNamer;
	}

	/**
	 * Configuration property:  Sort bean properties.
	 *
	 * @see #BEAN_sortProperties
	 * @return
	 * 	<jk>true</jk> if all bean properties will be serialized and access in alphabetical order.
	 */
	protected final boolean isSortProperties() {
		return sortProperties;
	}

	/**
	 * Configuration property:  Time zone.
	 *
	 * @see #BEAN_timeZone
	 * @return
	 * 	The default timezone for serializer and parser sessions.
	 */
	protected final TimeZone getTimeZone() {
		return timeZone;
	}

	/**
	 * Configuration property:  Use enum names.
	 *
	 * @see #BEAN_useEnumNames
	 * @return
	 * 	<jk>true</jk> if enums are always serialized by name, not using {@link Object#toString()}.
	 */
	protected final boolean isUseEnumNames() {
		return useEnumNames;
	}

	/**
	 * Configuration property:  Use interface proxies.
	 *
	 * @see #BEAN_useInterfaceProxies
	 * @return
	 * 	<jk>true</jk> if interfaces will be instantiated as proxy classes through the use of an
	 * 	{@link InvocationHandler} if there is no other way of instantiating them.
	 */
	protected final boolean isUseInterfaceProxies() {
		return useInterfaceProxies;
	}

	/**
	 * Configuration property:  Use Java Introspector.
	 *
	 * @see #BEAN_useJavaBeanIntrospector
	 * @return
	 * 	<jk>true</jk> if the built-in Java bean introspector should be used for bean introspection.
	 */
	protected final boolean isUseJavaBeanIntrospector() {
		return useJavaBeanIntrospector;
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
			this.beanToStringSerializer = JsonSerializer.create().apply(this.getPropertyStore()).sq().simple().build();
		}
		return beanToStringSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a("BeanContext", new DefaultFilteringOMap()
				.a("id", System.identityHashCode(this))
				.a("beanClassVisibility", beanClassVisibility)
				.a("beanConstructorVisibility", beanConstructorVisibility)
				.a("beanDictionaryClasses", beanDictionaryClasses)
				.a("beanFieldVisibility", beanFieldVisibility)
				.a("beanFilters", beanFilters)
				.a("beanMapPutReturnsOldValue", beanMapPutReturnsOldValue)
				.a("beanMethodVisibility", beanMethodVisibility)
				.a("beansRequireDefaultConstructor", beansRequireDefaultConstructor)
				.a("beansRequireSerializable", beansRequireSerializable)
				.a("beansRequireSettersForGetters", beansRequireSettersForGetters)
				.a("beansRequireSomeProperties", beansRequireSomeProperties)
				.a("bpi", bpi)
				.a("bpx", bpx)
				.a("ignoreInvocationExceptionsOnGetters", ignoreInvocationExceptionsOnGetters)
				.a("ignoreInvocationExceptionsOnSetters", ignoreInvocationExceptionsOnSetters)
				.a("ignorePropertiesWithoutSetters", ignorePropertiesWithoutSetters)
				.a("ignoreTransientProperties", ignoreTransientFields)
				.a("ignoreUnknownBeanProperties", ignoreUnknownBeanProperties)
				.a("ignoreUnknownNullBeanProperties", ignoreUnknownNullBeanProperties)
				.a("implClasses", implClasses)
				.a("locale", locale)
				.a("mediaType", mediaType)
				.a("notBeanClasses", notBeanClasses)
				.a("notBeanPackageNames", notBeanPackageNames)
				.a("notBeanPackagePrefixes", notBeanPackagePrefixes)
				.a("pojoSwaps", pojoSwaps)
				.a("sortProperties", sortProperties)
				.a("timeZone", timeZone)
				.a("useEnumNames", useEnumNames)
				.a("useInterfaceProxies", useInterfaceProxies)
				.a("useJavaBeanIntrospector", useJavaBeanIntrospector)
			);
	}
}