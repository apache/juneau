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

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.transform.*;

/**
 * Builder class for building instances of serializers, parsers, and bean contexts.
 *
 * <p>
 * All serializers and parsers extend from this class.
 *
 * <p>
 * Provides a base set of common config property setters that allow you to build up serializers and parsers.
 *
 * <p class='bcode w800'>
 * 	WriterSerializer s = JsonSerializer
 * 		.<jsm>create</jsm>()
 * 		.set(<jsf>JSON_simpleMode</jsf>, <jk>true</jk>)
 * 		.set(<jsf>SERIALIZER_useWhitespace</jsf>, <jk>true</jk>)
 * 		.set(<jsf>SERIALIZER_quoteChar</jsf>, <js>"'"</js>)
 * 		.build();
 * </p>
 *
 * <p>
 * Additional convenience methods are provided for setting properties using reduced syntax.
 *
 * <p class='bcode w800'>
 * 	WriterSerializer s = JsonSerializer
 * 		.<jsm>create</jsm>()  <jc>// Create a JsonSerializerBuilder</jc>
 * 		.simple()  <jc>// Simple mode</jc>
 * 		.ws()  <jc>// Use whitespace</jc>
 * 		.sq()  <jc>// Use single quotes </jc>
 * 		.build();  <jc>// Create a JsonSerializer</jc>
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-marshall.ConfigurableProperties}
 * </ul>
 */
public class BeanContextBuilder extends ContextBuilder {

	/**
	 * Constructor.
	 *
	 * All default settings.
	 */
	public BeanContextBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public BeanContextBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public BeanContext build() {
		return build(BeanContext.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Dynamically applied POJO annotations.
	 *
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
	 * <p class='bcode w800'>
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
	 * In general, the underlying framework uses this method when it finds dynamically applied annotations on
	 * config annotations.  However, concrete implementations of annotations are also provided that can be passed
	 * directly into builder classes like so:
	 * <p class='bcode w800'>
	 * 	<jc>// Create a concrete @Bean annotation.</jc>
	 * 	BeanAnnotation a = <jk>new</jk> BeanAnnotation(<js>"B"</js>).bpi(<js>"street,city,state"</js>);
	 *
	 * 	<jc>// Apply it to a serializer.</jc>
	 * 	WriterSerializer ws = JsonSerializer.<jsm>create</jsm>().annotations(a).build();
	 *
	 * 	<jc>// Serialize a bean with the dynamically applied annotation.</jc>
	 * 	String json = ws.serialize(<jk>new</jk> B());
	 * </p>
	 *
	 * <p>
	 * The following is the list of concrete annotations provided that can be constructed and passed into the builder
	 * class:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeancAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanIgnoreAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanpAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.ExampleAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.NamePropertyAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.ParentPropertyAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.SwapAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.UriAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.csv.annotation.CsvAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.jso.annotation.JsoAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.json.annotation.JsonAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.SchemaAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.msgpack.annotation.MsgPackAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.oapi.annotation.OpenApiAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.plaintext.annotation.PlainTextAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.soap.annotation.SoapXmlAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.uon.annotation.UonAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.urlencoding.annotation.UrlEncodingAnnotation}
	 * 	<li class='ja'>{@link org.apache.juneau.xml.annotation.XmlAnnotation}
	 * </ul>
	 *
	 * <p>
	 * The syntax for the <l>on()</l> pattern match parameter depends on whether it applies to a class, method, field, or constructor.
	 * The valid pattern matches are:
	 * <ul class='spaced-list'>
	 *  <li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass"</js>
	 * 				</ul>
	 * 			<li>Fully qualified inner class:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass"</js>
	 * 				</ul>
	 * 			<li>Simple inner:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2"</js>
	 * 					<li><js>"Inner1$Inner2"</js>
	 * 					<li><js>"Inner2"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>Fully qualified with args:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple with args:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner2.myMethod"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Fields:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner2.myField"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Constructors:
	 * 		<ul>
	 * 			<li>Fully qualified with args:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass(String,int)"</js>
	 * 					<li><js>"com.foo.MyClass(java.lang.String,int)"</js>
	 * 					<li><js>"com.foo.MyClass()"</js>
	 * 				</ul>
	 * 			<li>Simple with args:
	 * 				<ul>
	 * 					<li><js>"MyClass(String,int)"</js>
	 * 					<li><js>"MyClass(java.lang.String,int)"</js>
	 * 					<li><js>"MyClass()"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2()"</js>
	 * 					<li><js>"Inner1$Inner2()"</js>
	 * 					<li><js>"Inner2()"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder annotations(Annotation...values) {
		return prependTo(BEAN_annotations, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Minimum bean class visibility.
	 *
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <jsf>PUBLIC</jsf> and the bean class is <jk>protected</jk>, then the class
	 * will not be interpreted as a bean class and be serialized as a string.
	 * Use this setting to reduce the visibility requirement.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a protected class and one field.</jc>
	 * 	<jk>protected class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that's capable of serializing the class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanClassVisibility(<jsf>PROTECTED</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo","bar"}</jc>
	 * 	String json = w.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanClassVisibility}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link Visibility#PUBLIC}.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beanClassVisibility(Visibility value) {
		return set(BEAN_beanClassVisibility, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Minimum bean constructor visibility.
	 *
	 * <p>
	 * Only look for constructors with the specified minimum visibility.
	 *
	 * <p>
	 * This setting affects the logic for finding no-arg constructors for bean.  Normally, only <jk>public</jk> no-arg
	 * constructors are used.  Use this setting if you want to reduce the visibility requirement.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a protected constructor and one field.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf>;
	 *
	 * 		<jk>protected</jk> MyBean() {}
	 * 	}
	 *
	 * 	<jc>// Create a parser capable of calling the protected constructor.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanConstructorVisibility(<jsf>PROTECTED</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Use it.</jc>
	 * 	MyBean c = r.parse(<js>"{foo:'bar'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Beanc @Beanc} annotation can also be used to expose a constructor with non-public visibility.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanConstructorVisibility}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link Visibility#PUBLIC}.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beanConstructorVisibility(Visibility value) {
		return set(BEAN_beanConstructorVisibility, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean dictionary.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dictionary(Object...)}
	 * </div>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beanDictionary(Object...values) {
		return prependTo(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean dictionary.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dictionary(Object...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beanDictionary(Class<?>...values) {
		return prependTo(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean dictionary.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dictionaryReplace(Object...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beanDictionaryReplace(Class<?>...values) {
		return set(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean dictionary.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dictionaryReplace(Object...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beanDictionaryReplace(Object...values) {
		return set(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean dictionary.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dictionaryRemove(Object...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beanDictionaryRemove(Class<?>...values) {
		return removeFrom(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean dictionary.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dictionaryRemove(Object...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beanDictionaryRemove(Object...values) {
		return removeFrom(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Minimum bean field visibility.
	 *
	 * <p>
	 * Only look for bean fields with the specified minimum visibility.
	 *
	 * <p>
	 * This affects which fields on a bean class are considered bean properties.  Normally only <jk>public</jk> fields are considered.
	 * Use this setting if you want to reduce the visibility requirement.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a protected field.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>protected</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that recognizes the protected field.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFieldVisibility(<jsf>PROTECTED</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * Bean fields can be ignored as properties entirely by setting the value to {@link Visibility#NONE}
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Disable using fields as properties entirely.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFieldVisibility(<jsf>NONE</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Beanp @Beanp} annotation can also be used to expose a field with non-public visibility.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFieldVisibility}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link Visibility#PUBLIC}.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beanFieldVisibility(Visibility value) {
		return set(BEAN_beanFieldVisibility, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean filters.
	 *
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
	 * 	<jc>// A bean with multiple properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String
	 * 			<jf>foo</jf> = <js>"foo"</js>,
	 * 			<jf>bar</jf> = <js>"bar"</js>,
	 * 			<jf>baz</jf> = <js>"baz"</js>;  <jc>// Ignore this field.</jc>
	 * 	}
	 *
	 * 	<jc>// Create a bean filter for the MyBean class.</jc>
	 * 	<jk>public class</jk> MyBeanFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 *
	 * 		<jc>// Must provide a no-arg constructor!</jc>
	 * 		<jk>public</jk> MyBeanFilter() {
	 * 			bpi(<js>"foo,bar"</js>);  <jc>// The properties we want exposed (bean property include).</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Associate our bean filter with a serializer.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyBeanFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same but pass in constructed filter.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(BeanFilter.<jsm>create</jsm>(MyBeanFilter.<jk>class</jk>).bpi(<js>"foo,bar"</js>).build())
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * An alternate approach for specifying bean filters is by using concrete dynamically applied annotations:
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Create a concrete @Bean annotation.</jc>
	 * 	BeanAnnotation a = <jk>new</jk> BeanAnnotation(<js>"MyBean"</js>).bpi(<js>"foo,bar"</js>);
	 *
	 * 	<jc>// Apply it to a serializer.</jc>
	 * 	WriterSerializer ws = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.annotations(a)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.BeanFilters}
	 * 	<li class='link'>{@doc juneau-marshall.Transforms.InterfaceFilters}
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * 	<li class='jf'>{@link #BEAN_annotations}
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beanFilters(Object...values) {
		return prependTo(BEAN_beanFilters, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean filters.
	 *
	 * <p>
	 * Same as {@link #beanFilters(Object...)} but replaces the existing values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 *
	 * @param values
	 * 	The new values for this property.
	 * 	<p>
	 * 	Values can consist of any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>Any subclass or instance of {@link BeanFilterBuilder}.
	 * 			<br>These must have a public no-arg constructor when a class.
	 * 		<li>Any instance of {@link BeanFilter}.
	 * 		<li>Any bean interfaces.
	 * 			<br>A shortcut for defining a {@link InterfaceBeanFilterBuilder}.
	 * 			<br>Any subclasses of an interface class will only have properties defined on the interface.
	 * 			All other bean properties will be ignored.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beanFiltersReplace(Object...values) {
		return set(BEAN_beanFilters, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean filters.
	 *
	 * <p>
	 * Removes from the list of classes that make up the bean filters in this bean context.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<p>
	 * 	Values can consist of any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>Any subclass or instance of {@link BeanFilterBuilder}.
	 * 			<br>These must have a public no-arg constructor when a class.
	 * 		<li>Any instance of {@link BeanFilter}.
	 * 		<li>Any bean interfaces.
	 * 			<br>A shortcut for defining a {@link InterfaceBeanFilterBuilder}.
	 * 			<br>Any subclasses of an interface class will only have properties defined on the interface.
	 * 			All other bean properties will be ignored.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beanFiltersRemove(Object...values) {
		return removeFrom(BEAN_beanFilters, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  BeanMap.put() returns old property value.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #beanMapPutReturnsOldValue()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public BeanContextBuilder beanMapPutReturnsOldValue(boolean value) {
		return set(BEAN_beanMapPutReturnsOldValue, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  BeanMap.put() returns old property value.
	 *
	 * <p>
	 * If <jk>true</jk>, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
	 * values.  Otherwise, it returns <jk>null</jk>.
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
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanMapPutReturnsOldValue}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beanMapPutReturnsOldValue() {
		return set(BEAN_beanMapPutReturnsOldValue, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Minimum bean method visibility.
	 *
	 * <p>
	 * Only look for bean methods with the specified minimum visibility.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanMethodVisibility}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link Visibility#PUBLIC}
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beanMethodVisibility(Visibility value) {
		return set(BEAN_beanMethodVisibility, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Beans require no-arg constructors.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #beansRequireDefaultConstructor()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beansRequireDefaultConstructor(boolean value) {
		return set(BEAN_beansRequireDefaultConstructor, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Beans require no-arg constructors.
	 *
	 * <p>
	 * Shortcut for calling <code>beansRequireDefaultConstructor(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireDefaultConstructor}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beansRequireDefaultConstructor() {
		return set(BEAN_beansRequireDefaultConstructor, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Beans require Serializable interface.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #beansRequireSerializable()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beansRequireSerializable(boolean value) {
		return set(BEAN_beansRequireSerializable, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Beans require Serializable interface.
	 *
	 * <p>
	 * Shortcut for calling <code>beansRequireSerializable(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSerializable}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beansRequireSerializable() {
		return set(BEAN_beansRequireSerializable, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Beans require setters for getters.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #beansRequireSettersForGetters()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beansRequireSettersForGetters(boolean value) {
		return set(BEAN_beansRequireSettersForGetters, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Beans require setters for getters.
	 *
	 * <p>
	 * Shortcut for calling <code>beansRequireSettersForGetters(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSettersForGetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beansRequireSettersForGetters() {
		return set(BEAN_beansRequireSettersForGetters, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Beans require at least one property.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #beansDontRequireSomeProperties()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder beansRequireSomeProperties(boolean value) {
		return set(BEAN_beansRequireSomeProperties, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Beans require at least one property.
	 *
	 * <p>
	 * If <jk>true</jk>, then a Java class must contain at least 1 property to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSomeProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beansDontRequireSomeProperties() {
		return set(BEAN_beansRequireSomeProperties, false);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean type property name.
	 *
	 * <p>
	 * This specifies the name of the bean property used to store the dictionary name of a bean type so that the
	 * parser knows the data type to reconstruct.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanTypePropertyName}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"_type"</js>.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder beanTypePropertyName(String value) {
		return set(BEAN_beanTypePropertyName, value);
	}

	/**
	 * Bean property includes.
	 *
	 * <p>
	 * Specifies the set and order of names of properties associated with the bean class.
	 *
	 * <p>
	 * For example, <c>bpi(MyBean.<jk>class</jk>, <js>"foo,bar"</js>)</c> means only serialize the <c>foo</c> and
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
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String
	 * 			<jf>foo</jf> = <js>"foo"</js>,
	 * 			<jf>bar</jf> = <js>"bar"</js>,
	 * 			<jf>baz</jf> = <js>"baz"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that includes only the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpi(MyBean.<jk>class</jk>, <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(beanClass.getName()).bpi(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#bpi()} - On an annotation on the bean class itself.
	 * 	<li class='jm'>{@link BeanConfig#bpi()} - On a bean config annotation (see {@link #annotations(Annotation...)}.
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpi(Class<?> beanClass, String properties) {
		return prependTo(BEAN_annotations, new BeanAnnotation(beanClass.getName()).bpi(properties));
	}

	/**
	 * Bean property includes.
	 *
	 * <p>
	 * Specifies the set and order of names of properties associated with bean classes.
	 *
	 * <p>
	 * For example, <c>bpi(OMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"foo,bar"</js>))</c> means only serialize the <c>foo</c> and
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
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String
	 * 			<jf>foo</jf> = <js>"foo"</js>,
	 * 			<jf>bar</jf> = <js>"bar"</js>,
	 * 			<jf>baz</jf> = <js>"baz"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that includes only the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpi(OMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"foo,bar"</js>))
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code for each entry:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(key).bpi(value.toString()));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#bpi()} - On an annotation on the bean class itself.
	 * 	<li class='jm'>{@link BeanConfig#bpi()} - On a bean config annotation (see {@link #annotations(Annotation...)}.
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this builder.
	 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * 	<br>Values are comma-delimited lists of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpi(Map<String,Object> values) {
		for (Map.Entry<String,Object> e : values.entrySet())
			prependTo(BEAN_annotations, new BeanAnnotation(e.getKey()).bpi(asString(e.getValue())));
		return this;
	}

	/**
	 * Bean property includes.
	 *
	 * <p>
	 * Specifies the set and order of names of properties associated with the bean class.
	 *
	 * <p>
	 * For example, <c>bpi(<js>"MyBean"</js>, <js>"foo,bar"</js>)</c> means only serialize the <c>foo</c> and
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
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String
	 * 			<jf>foo</jf> = <js>"foo"</js>,
	 * 			<jf>bar</jf> = <js>"bar"</js>,
	 * 			<jf>baz</jf> = <js>"baz"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that includes only the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpi(<js>"MyBean"</js>, <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(beanClassName).bpi(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#bpi()} - On an annotation on the bean class itself.
	 * 	<li class='jm'>{@link BeanConfig#bpi()} - On a bean config annotation (see {@link #annotations(Annotation...)}.
	 * </ul>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpi(String beanClassName, String properties) {
		return prependTo(BEAN_annotations, new BeanAnnotation(beanClassName).bpi(properties));
	}

	/**
	 * Bean property excludes.
	 *
	 * <p>
	 * Specifies to exclude the specified list of properties for the specified bean class.
	 *
	 * <p>
	 * Same as {@link #bpi(Class, String)} except you specify a list of bean property names that you want to exclude from
	 * serialization.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String
	 * 			<jf>foo</jf> = <js>"foo"</js>,
	 * 			<jf>bar</jf> = <js>"bar"</js>,
	 * 			<jf>baz</jf> = <js>"baz"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that excludes the "bar" and "baz" properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpx(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(beanClass.getName()).bpx(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanConfig#bpx()}
	 * 	<li class='jm'>{@link Bean#bpx()}
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpx(Class<?> beanClass, String properties) {
		return prependTo(BEAN_annotations, new BeanAnnotation(beanClass.getName()).bpx(properties));
	}

	/**
	 * Bean property excludes.
	 *
	 * <p>
	 * Specifies to exclude the specified list of properties for the specified bean classes.
	 *
	 * <p>
	 * Same as {@link #bpi(Map)} except you specify a list of bean property names that you want to exclude from
	 * serialization.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String
	 * 			<jf>foo</jf> = <js>"foo"</js>,
	 * 			<jf>bar</jf> = <js>"bar"</js>,
	 * 			<jf>baz</jf> = <js>"baz"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that excludes the "bar" and "baz" properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpx(OMap.of(<js>"MyBean"</js>, <js>"bar,baz"</js>))
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code for each entry:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(key).bpx(value.toString()));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanConfig#bpx()}
	 * 	<li class='jm'>{@link Bean#bpx()}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this builder.
	 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * 	<br>Values are comma-delimited lists of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpx(Map<String,Object> values) {
		for (Map.Entry<String,Object> e : values.entrySet())
			prependTo(BEAN_annotations, new BeanAnnotation(e.getKey()).bpx(asString(e.getValue())));
		return this;
	}

	/**
	 * Bean property excludes.
	 *
	 * <p>
	 * Specifies to exclude the specified list of properties for the specified bean class.
	 *
	 * <p>
	 * Same as {@link #bpx(String, String)} except you specify a list of bean property names that you want to exclude from
	 * serialization.
	 *
	 * <p>
	 * Setting applies to specified class and all subclasses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String
	 * 			<jf>foo</jf> = <js>"foo"</js>,
	 * 			<jf>bar</jf> = <js>"bar"</js>,
	 * 			<jf>baz</jf> = <js>"baz"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that excludes the "bar" and "baz" properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpx(<js>"MyBean"</js>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(beanClassName).bpx(properties));
	 * </p>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpx(String beanClassName, String properties) {
		return prependTo(BEAN_annotations, new BeanAnnotation(beanClassName).bpx(properties));
	}

	/**
	 * Read-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 * Note that this is different from the <l>bpi</l>/<l>bpx</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String foo, bar, baz;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with read-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpro(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// All 3 properties will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with read-only property settings.</jc>
	 * 	// to read-only.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.bpro(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
	 * 		.ignoreUnknownBeanProperties()
	 * 		.build();
	 *
	 * 	<jc>// Parser ignores bar and baz properties.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(beanClass.getName()).bpro(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanConfig#bpro()}
	 * 	<li class='jm'>{@link Bean#bpro()}
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpro(Class<?> beanClass, String properties) {
		return prependTo(BEAN_annotations, new BeanAnnotation(beanClass.getName()).bpro(properties));
	}

	/**
	 * Read-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on beans that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 * Note that this is different from the <l>bpi</l>/<l>bpx</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String foo, bar, baz;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with read-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpro(OMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
	 * 		.build();
	 *
	 * 	<jc>// All 3 properties will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with read-only property settings.</jc>
	 * 	// to read-only.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.bpro(OMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
	 * 		.ignoreUnknownBeanProperties()
	 * 		.build();
	 *
	 * 	<jc>// Parser ignores bar and baz properties.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code for each entry:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(key).bpro(value.toString()));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanConfig#bpro()}
	 * 	<li class='jm'>{@link Bean#bpro()}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this builder.
	 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * 	<br>Values are comma-delimited lists of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpro(Map<String,Object> values) {
		for (Map.Entry<String,Object> e : values.entrySet())
			prependTo(BEAN_annotations, new BeanAnnotation(e.getKey()).bpro(asString(e.getValue())));
		return this;
	}

	/**
	 * Read-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 * Note that this is different from the <l>bpi</l>/<l>bpx</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String foo, bar, baz;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with read-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpro(<js>"MyBean"</js>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// All 3 properties will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with read-only property settings.</jc>
	 * 	// to read-only.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.bpro(<js>"MyBean"</js>, <js>"bar,baz"</js>)
	 * 		.ignoreUnknownBeanProperties()
	 * 		.build();
	 *
	 * 	<jc>// Parser ignores bar and baz properties.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(beanClass.getName).bpro(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanConfig#bpro()}
	 * 	<li class='jm'>{@link Bean#bpro()}
	 * </ul>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpro(String beanClassName, String properties) {
		return prependTo(BEAN_annotations, new BeanAnnotation(beanClassName).bpro(properties));
	}

	/**
	 * Write-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 * Note that this is different from the <l>bpi</l>/<l>bpx</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String foo, bar, baz;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with write-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpwo(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Only foo will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with write-only property settings.</jc>
	 * 	// to read-only.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.bpwo(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Parser parses all 3 properties.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(beanClass.getName).bpwo(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanConfig#bpwo()}
	 * 	<li class='jm'>{@link Bean#bpwo()}
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpwo(Class<?> beanClass, String properties) {
		return prependTo(BEAN_annotations, new BeanAnnotation(beanClass.getName()).bpwo(properties));
	}

	/**
	 * Write-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 * Note that this is different from the <l>bpi</l>/<l>bpx</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String foo, bar, baz;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with write-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpwo(OMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
	 * 		.build();
	 *
	 * 	<jc>// Only foo will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with write-only property settings.</jc>
	 * 	// to read-only.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.bpwo(OMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
	 * 		.build();
	 *
	 * 	<jc>// Parser parses all 3 properties.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code for each entry:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(key).bpwo(value.toString()));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanConfig#bpwo()}
	 * 	<li class='jm'>{@link Bean#bpwo()}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this builder.
	 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * 	<br>Values are comma-delimited lists of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpwo(Map<String,Object> values) {
		for (Map.Entry<String,Object> e : values.entrySet())
			prependTo(BEAN_annotations, new BeanAnnotation(e.getKey()).bpwo(asString(e.getValue())));
		return this;
	}

	/**
	 * Write-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 * Note that this is different from the <l>bpi</l>/<l>bpx</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String foo, bar, baz;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with write-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.bpwo(<js>"MyBean"</js>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Only foo will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with write-only property settings.</jc>
	 * 	// to read-only.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.bpwo(<js>"MyBean"</js>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Parser parses all 3 properties.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 		builder.anntations(<jk>new</jk> BeanAnnotation(beanClassName).bpwo(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanConfig#bpwo()}
	 * 	<li class='jm'>{@link Bean#bpwo()}
	 * </ul>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder bpwo(String beanClassName, String properties) {
		return prependTo(BEAN_annotations, new BeanAnnotation(beanClassName).bpwo(properties));
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Debug mode.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #debug()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder debug(boolean value) {
		return set(BEAN_debug, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Debug mode.
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
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_debug}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder debug() {
		return set(BEAN_debug, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary in this bean context.
	 *
	 * <p>
	 * A dictionary is a name/class mapping used to find class types during parsing when they cannot be inferred
	 * through reflection.  The names are defined through the {@link Bean#typeName() @Bean(typeName)} annotation defined
	 * on the bean class.  For example, if a class <c>Foo</c> has a type-name of <js>"myfoo"</js>, then it would end up
	 * serialized as <js>"{_type:'myfoo',...}"</js> in JSON (depending on <l>addBeanTypes</l>/<l>addRootType</l> properties)
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
	 * <p class='bcode w800'>
	 * 	<jc>// POJOs with @Bean(name) annotations.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"foo"</js>)
	 * 	<jk>public class</jk> Foo {...}
	 * 	<ja>@Bean</ja>(typeName=<js>"bar"</js>)
	 * 	<jk>public class</jk> Bar {...}
	 *
	 * 	<jc>// Create a parser and tell it which classes to try to resolve.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.dictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// A bean with a field with an indeterminate type.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> Object <jf>mySimpleField</jf>;
	 * 	}
	 *
	 * 	<jc>// Parse bean.</jc>
	 * 	MyBean b = p.parse(<js>"{mySimpleField:{_type:'foo',...}}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * Another option is to use the {@link Bean#dictionary()} annotation on the POJO class itself:
	 *
	 * <p class='bcode w800'>
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
	 * <p class='bcode w800'>
	 * 	<jc>// Use the predefined HTML5 bean dictionary which is a BeanDictionaryList.</jc>
	 * 	ReaderParser p = HtmlParser
	 * 		.<jsm>create</jsm>()
	 * 		.dictionary(HtmlBeanDictionary.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Parse an HTML body into HTML beans.</jc>
	 * 	Body body = p.parse(<js>"&lt;body&gt;&lt;ul&gt;&lt;li&gt;foo&lt;/li&gt;&lt;li&gt;bar&lt;/li&gt;&lt;/ul&gt;"</js>, Body.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * 	<li class='link'>{@doc juneau-marshall.BeanDictionaries}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder dictionary(Object...values) {
		return prependTo(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean dictionary.
	 *
	 * <p>
	 * Same as {@link #beanDictionary(Object...)} but replaces the existing value.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param values
	 * 	The new values for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder dictionaryReplace(Object...values) {
		return set(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean dictionary.
	 *
	 * <p>
	 * Removes from the list of classes that make up the bean dictionary in this bean context.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder dictionaryRemove(Object...values) {
		return removeFrom(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  POJO example.
	 *
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_examples}
	 * </ul>
	 *
	 * @param pojoClass The POJO class.
	 * @param o An instance of the POJO class used for examples.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public <T> BeanContextBuilder example(Class<T> pojoClass, T o) {
		return putTo(BEAN_examples, pojoClass.getName(), o);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  POJO example.
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
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that excludes the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.example(MyBean.<jk>class</jk>, <js>"{foo:'bar'}"</js>)
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
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_examples}
	 * </ul>
	 *
	 * @param <T> The POJO class type.
	 * @param pojoClass The POJO class.
	 * @param json The simple JSON representation of the example.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public <T> BeanContextBuilder exampleJson(Class<T> pojoClass, String json) {
		try {
			return putTo(BEAN_examples, pojoClass.getName(), SimpleJson.DEFAULT.read(json, pojoClass));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean property excludes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #bpx(Class, String)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated public BeanContextBuilder excludeProperties(Class<?> beanClass, String properties) {
		return putTo(BEAN_bpx, beanClass.getName(), properties);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean property excludes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #bpx(Map)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated public BeanContextBuilder excludeProperties(Map<String,String> values) {
		return set(BEAN_bpx, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean property excludes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #bpx(String, String)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated public BeanContextBuilder excludeProperties(String beanClassName, String value) {
		return putTo(BEAN_bpx, beanClassName, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Find fluent setters.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #fluentSetters()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder fluentSetters(boolean value) {
		return set(BEAN_fluentSetters, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Find fluent setters.
	 *
	 * <p>
	 * Shortcut for calling <code>fluentSetters(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_fluentSetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder fluentSetters() {
		return set(BEAN_fluentSetters, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore invocation errors on getters.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #ignoreInvocationExceptionsOnGetters()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnGetters, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore invocation errors on getters.
	 *
	 * <p>
	 * Shortcut for calling <code>ignoreInvocationExceptionsOnGetters(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreInvocationExceptionsOnGetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder ignoreInvocationExceptionsOnGetters() {
		return set(BEAN_ignoreInvocationExceptionsOnGetters, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore invocation errors on setters.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #ignoreInvocationExceptionsOnSetters()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnSetters, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore invocation errors on setters.
	 *
	 * <p>
	 * Shortcut for calling <code>ignoreInvocationExceptionsOnSetters(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreInvocationExceptionsOnSetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder ignoreInvocationExceptionsOnSetters() {
		return set(BEAN_ignoreInvocationExceptionsOnSetters, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore properties without setters.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dontIgnorePropertiesWithoutSetters()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder ignorePropertiesWithoutSetters(boolean value) {
		return set(BEAN_ignorePropertiesWithoutSetters, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore properties without setters.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a bean property without a setter will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignorePropertiesWithoutSetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder dontIgnorePropertiesWithoutSetters() {
		return set(BEAN_ignorePropertiesWithoutSetters, false);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore transient fields.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dontIgnoreTransientFields()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder ignoreTransientFields(boolean value) {
		return set(BEAN_ignoreTransientFields, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore transient fields.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreTransientFields}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder dontIgnoreTransientFields() {
		return set(BEAN_ignoreTransientFields, false);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore unknown properties.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #ignoreUnknownBeanProperties()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder ignoreUnknownBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownBeanProperties, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore unknown properties.
	 *
	 * <p>
	 * Shortcut for calling <code>ignoreUnknownBeanProperties(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreUnknownBeanProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder ignoreUnknownBeanProperties() {
		return set(BEAN_ignoreUnknownBeanProperties, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore unknown properties with null values.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dontIgnoreUnknownNullBeanProperties()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder ignoreUnknownNullBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownNullBeanProperties, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Ignore unknown properties with null values.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a <jk>null</jk> value on a non-existent bean property will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreUnknownNullBeanProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder dontIgnoreUnknownNullBeanProperties() {
		return set(BEAN_ignoreUnknownNullBeanProperties, false);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Implementation classes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_implClasses}
	 * </ul>
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		return putTo(BEAN_implClasses, interfaceClass.getName(), implClass);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Implementation classes.
	 *
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation class for the
	 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
	 * parse).
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_implClasses}
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder implClasses(Map<String,Class<?>> values) {
		return set(BEAN_implClasses, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean property includes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #bpi(Class, String)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated public BeanContextBuilder includeProperties(Class<?> beanClass, String value) {
		return putTo(BEAN_bpi, beanClass.getName(), value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean property includes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #bpi(Map)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated public BeanContextBuilder includeProperties(Map<String,String> values) {
		return set(BEAN_bpi, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean property includes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #bpi(String, String)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated public BeanContextBuilder includeProperties(String beanClassName, String value) {
		return putTo(BEAN_bpi, beanClassName, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Locale.
	 *
	 * <p>
	 * Specifies a default locale for serializer and parser sessions.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_locale}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder locale(Locale value) {
		return set(BEAN_locale, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Media type.
	 *
	 * <p>
	 * Specifies a default media type value for serializer and parser sessions.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_mediaType}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder mediaType(MediaType value) {
		return set(BEAN_mediaType, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean class exclusions.
	 *
	 * <p>
	 * List of classes that should not be treated as beans even if they appear to be bean-like.
	 * <br>Not-bean classes are converted to <c>Strings</c> during serialization.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Classes.
	 * 		<li>Arrays and collections of classes.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder notBeanClasses(Object...values) {
		return addTo(BEAN_notBeanClasses, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean class exclusions.
	 *
	 * <p>
	 * Not-bean classes are converted to <c>Strings</c> during serialization even if they appear to be
	 * bean-like.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 *
	 * @param values
	 * 	The new value for this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Classes.
	 * 		<li>Arrays and collections of classes.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder notBeanClassesReplace(Object...values) {
		return set(BEAN_notBeanClasses, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean class exclusions.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Classes.
	 * 		<li>Arrays and collections of classes.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder notBeanClassesRemove(Object...values) {
		return removeFrom(BEAN_notBeanClasses, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean package exclusions.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Strings.
	 * 		<li>Arrays and collections of strings.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder notBeanPackages(Object...values) {
		return addTo(BEAN_notBeanPackages, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean package exclusions.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values
	 * 	<br>Values can consist of any of the following types:
	 * 	<br>Possible values are:
	 * 	<ul>
	 * 		<li>Strings.
	 * 		<li>Arrays and collections of strings.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder notBeanPackagesReplace(Object...values) {
		return set(BEAN_notBeanPackages, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean package exclusions.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values
	 * 	<br>Values can consist of any of the following types:
	 * 	<br>Possible values are:
	 * 	<ul>
	 * 		<li>Strings.
	 * 		<li>Arrays and collections of strings.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder notBeanPackagesRemove(Object...values) {
		return removeFrom(BEAN_notBeanPackages, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  POJO swaps.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #swaps(Object...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder pojoSwaps(Object...values) {
		return appendTo(BEAN_pojoSwaps, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  POJO swaps.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #swapsReplace(Object...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder pojoSwapsReplace(Object...values) {
		return set(BEAN_pojoSwaps, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  POJO swaps.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #swapsRemove(Object...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder pojoSwapsRemove(Object...values) {
		return removeFrom(BEAN_pojoSwaps, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Bean property namer
	 *
	 * <p>
	 * The class to use for calculating bean property names.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_propertyNamer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link PropertyNamerDefault}.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder propertyNamer(Class<? extends PropertyNamer> value) {
		return set(BEAN_propertyNamer, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Sort bean properties.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #sortProperties()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder sortProperties(boolean value) {
		return set(BEAN_sortProperties, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Sort bean properties.
	 *
	 * <p>
	 * Shortcut for calling <code>sortProperties(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_sortProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder sortProperties() {
		return set(BEAN_sortProperties, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Java object swaps.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_swaps}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any subclass of {@link PojoSwap}.
	 * 		<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder swaps(Object...values) {
		return appendTo(BEAN_swaps, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Java object swaps.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_swaps}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any subclass of {@link PojoSwap}.
	 * 		<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder swapsReplace(Object...values) {
		return set(BEAN_swaps, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Java object swaps.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_swaps}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any subclass of {@link PojoSwap}.
	 * 		<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder swapsRemove(Object...values) {
		return removeFrom(BEAN_swaps, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  TimeZone.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_timeZone}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder timeZone(TimeZone value) {
		return set(BEAN_timeZone, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Use enum names.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #useEnumNames()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder useEnumNames(boolean value) {
		return set(BEAN_useEnumNames, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Use enum names.
	 *
	 * <p>
	 * When enabled, enums are always serialized by name instead of using {@link Object#toString()}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useEnumNames}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder useEnumNames() {
		return set(BEAN_useEnumNames, true);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Use interface proxies.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #dontUseInterfaceProxies()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder useInterfaceProxies(boolean value) {
		return set(BEAN_useInterfaceProxies, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Use interface proxies.
	 *
	 * <p>
	 * If <jk>true</jk>, then interfaces will be instantiated as proxy classes through the use of an
	 * {@link InvocationHandler} if there is no other way of instantiating them.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useInterfaceProxies}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder dontUseInterfaceProxies() {
		return set(BEAN_useInterfaceProxies, false);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Use Java Introspector.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #useJavaBeanIntrospector()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public BeanContextBuilder useJavaBeanIntrospector(boolean value) {
		return set(BEAN_useJavaBeanIntrospector, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:</i>  Use Java Introspector.
	 *
	 * <p>
	 * Shortcut for calling <code>useJavaBeanIntrospector(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useJavaBeanIntrospector}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public BeanContextBuilder useJavaBeanIntrospector() {
		return set(BEAN_useJavaBeanIntrospector, true);
	}

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
}