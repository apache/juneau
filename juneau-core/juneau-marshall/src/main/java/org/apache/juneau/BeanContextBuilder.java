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

import java.beans.*;
import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.reflect.*;
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
 * 		.set(<jsf>JSON_simpleMode</jsf>)
 * 		.set(<jsf>SERIALIZER_useWhitespace</jsf>)
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
 * 	<li class='link'>{@doc GlossaryConfigurableProperties}
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
	 * @param cp The initial configuration settings for this builder.
	 */
	public BeanContextBuilder(ContextProperties cp) {
		super(cp);
	}

	@Override /* ContextBuilder */
	public BeanContext build() {
		return build(BeanContext.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Dynamically applied POJO annotations.
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
	 * 	<ja>@Bean</ja>(properties=<js>"street,city,state"</js>)
	 * 	<jk>public class</jk> A {...}
	 *
	 * 	<jc>// Class with annotation applied via @BeanConfig</jc>
	 * 	<jk>public class</jk> B {...}
	 *
	 * 	<jc>// Java REST method with @BeanConfig annotation.</jc>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<ja>@BeanConfig</ja>(
	 * 		annotations={
	 * 			<ja>@Bean</ja>(on=<js>"B"</js>, properties=<js>"street,city,state"</js>)
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
	 * 	BeanAnnotation a = <jk>new</jk> BeanAnnotation(<js>"B"</js>).properties(<js>"street,city,state"</js>);
	 *
	 * 	<jc>// Apply it to a serializer.</jc>
	 * 	WriterSerializer ws = JsonSerializer.<jsm>create</jsm>().annotations(a).build();
	 *
	 * 	<jc>// Serialize a bean with the dynamically applied annotation.</jc>
	 * 	String json = ws.serialize(<jk>new</jk> B());
	 * </p>
	 *
	 * <p>
	 * The following is the list of annotations builders provided that can be constructed
	 * and passed into the builder class:
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_annotations}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder annotations(Annotation...values) {
		return prependTo(BEAN_annotations, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Minimum bean class visibility.
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beanClassVisibility</jsf>, <js>"PROTECTED"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo","bar"}</jc>
	 * 	String json = w.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Bean @Bean} annotation can be used on a non-public bean class to override this setting.
	 * 	<li>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a public bean class to ignore it as a bean.
	 * </ul>
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
	@FluentSetter
	public BeanContextBuilder beanClassVisibility(Visibility value) {
		return set(BEAN_beanClassVisibility, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Minimum bean constructor visibility.
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
	 * 	ReaderParser p = ReaderParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanConstructorVisibility(<jsf>PROTECTED</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beanConstructorVisibility</jsf>, <js>"PROTECTED"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Use it.</jc>
	 * 	MyBean c = r.parse(<js>"{foo:'bar'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Beanc @Beanc} annotation can also be used to expose a non-public constructor.
	 * 	<li>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a public bean constructor to ignore it.
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
	@FluentSetter
	public BeanContextBuilder beanConstructorVisibility(Visibility value) {
		return set(BEAN_beanConstructorVisibility, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Minimum bean field visibility.
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beanFieldVisibility</jsf>, <js>"PROTECTED"</js>)
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
	 * 	<li>The {@link Beanp @Beanp} annotation can also be used to expose a non-public field.
	 * 	<li>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a public bean field to ignore it as a bean property.
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
	@FluentSetter
	public BeanContextBuilder beanFieldVisibility(Visibility value) {
		return set(BEAN_beanFieldVisibility, value);
	}

	/**
	 * Bean interceptor.
	 *
	 * <p>
	 * Bean interceptors can be used to intercept calls to getters and setters and alter their values in transit.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Interceptor that strips out sensitive information.</jc>
	 * 	<jk>public class</jk> AddressInterceptor <jk>extends</jk> BeanInterceptor&lt;Address&gt; {
	 *
	 * 		<jk>public</jk> Object readProperty(Address bean, String name, Object value) {
	 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(name))
	 * 				<jk>return</jk> <js>"redacted"</js>;
	 * 			<jk>return</jk> value;
	 * 		}
	 *
	 * 		<jk>public</jk> Object writeProperty(Address bean, String name, Object value) {
	 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(name) &amp;&amp; <js>"redacted"</js>.equals(value))
	 * 				<jk>return</jk> TaxInfoUtils.<jsm>lookup</jsm>(bean.getStreet(), bean.getCity(), bean.getState());
	 * 			<jk>return</jk> value;
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Our bean class.</jc>
	 * 	<jk>public class</jk> Address {
	 * 		<jk>public</jk> String getTaxInfo() {...}
	 * 		<jk>public void</jk> setTaxInfo(String s) {...}
	 * 	}
	 *
	 * 	<jc>// Register filter on serializer or parser.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanInterceptor(Address.<jk>class</jk>, AddressInterceptor.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"taxInfo":"redacted"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> Address());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link BeanInterceptor}
	 * 	<li class='ja'>{@link Bean#interceptor() Bean(interceptor)}
	 * </ul>
	 *
	 * @param on The bean that the filter applies to.
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanInterceptor(Class<?> on, Class<? extends BeanInterceptor<?>> value) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(on).interceptor(value).build());
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  BeanMap.put() returns old property value.
	 *
	 * <p>
	 * When enabled, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
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
	 * 		.set(<jsf>BEAN_beanMapPutReturnsOldValue</jsf>)
	 * 		.build();
	 *
	 * 	BeanMap&lt;MyBean&gt; bm = s.createSession().toBeanMap(<jk>new</jk> MyBean());
	 * 	bm.put(<js>"foo"</js>, <js>"bar"</js>);
	 * 	Object oldValue = bm.put(<js>"foo"</js>, <js>"baz"</js>);  <jc>// oldValue == "bar"</jc>
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanMapPutReturnsOldValue}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanMapPutReturnsOldValue() {
		return set(BEAN_beanMapPutReturnsOldValue);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Minimum bean method visibility.
	 *
	 * <p>
	 * Only look for bean methods with the specified minimum visibility.
	 *
	 * <p>
	 * This affects which methods are detected as getters and setters on a bean class. Normally only <jk>public</jk> getters and setters are considered.
	 * Use this setting if you want to reduce the visibility requirement.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a protected getter.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String getFoo() { <jk>return</jk> <js>"foo"</js>; }
	 * 		<jk>protected</jk> String getBar() { <jk>return</jk> <js>"bar"</js>; }
	 * 	}
	 *
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
	 *
	 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Beanp @Beanp} annotation can also be used to expose a non-public method.
	 * 	<li>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a public bean getter/setter to ignore it as a bean property.
	 * </ul>
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
	@FluentSetter
	public BeanContextBuilder beanMethodVisibility(Visibility value) {
		return set(BEAN_beanMethodVisibility, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Beans require no-arg constructors.
	 *
	 * <p>
	 * When enabled, a Java class must implement a default no-arg constructor to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean without a no-arg constructor.</jc>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// A property method.</jc>
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 *
	 * 		<jc>// A no-arg constructor</jc>
	 * 		<jk>public</jk> MyBean(String foo) {
	 * 			<jk>this</jk>.<jf>foo</jf> = foo;
	 * 		}
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> String toString() {
	 * 			<jk>return</jk> <js>"bar"</js>;
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a serializer that ignores beans without default constructors.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beansRequireDefaultConstructor()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beansRequireDefaultConstructor</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  "bar"</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Bean @Bean} annotation can be used on a bean class to override this setting.
	 * 	<li>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a class to ignore it as a bean.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireDefaultConstructor}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beansRequireDefaultConstructor() {
		return set(BEAN_beansRequireDefaultConstructor);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Beans require Serializable interface.
	 *
	 * <p>
	 * When enabled, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beansRequireSerializable()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beansRequireSerializable</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  "bar"</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Bean @Bean} annotation can be used on a bean class to override this setting.
	 * 	<li>The {@link BeanIgnore @BeanIgnore} annotation can also be used on a class to ignore it as a bean.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSerializable}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beansRequireSerializable() {
		return set(BEAN_beansRequireSerializable);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Beans require setters for getters.
	 *
	 * <p>
	 * When enabled, ignore read-only properties (properties with getters but not setters).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean without a Serializable interface.</jc>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// A read/write property.</jc>
	 * 		<jk>public</jk> String getFoo() { <jk>return</jk> <js>"foo"</js>; }
	 * 		<jk>public void</jk> setFoo(String foo) { ... }
	 *
	 * 		<jc>// A read-only property.</jc>
	 * 		<jk>public</jk> String getBar() { <jk>return</jk> <js>"bar"</js>; }
	 * 	}
	 *
	 * 	<jc>// Create a serializer that ignores bean properties without setters.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beansRequireSettersForGetters()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_beansRequireSettersForGetters</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Beanp @Beanp} annotation can be used on the getter to override this setting.
	 * 	<li>The {@link BeanIgnore @BeanIgnore} annotation can also be used on getters to ignore them as bean properties.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSettersForGetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beansRequireSettersForGetters() {
		return set(BEAN_beansRequireSettersForGetters);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Beans don't require at least one property.
	 *
	 * <p>
	 * When enabled, then a Java class doesn't need to contain at least 1 property to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with no properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 	}
	 *
	 * 	<jc>// Create a serializer that serializes beans even if they have zero properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.disableBeansRequireSomeProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_disableBeansRequireSomeProperties</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Bean @Bean} annotation can be used on the class to force it to be recognized as a bean class
	 * 		even if it has no properties.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_disableBeansRequireSomeProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder disableBeansRequireSomeProperties() {
		return set(BEAN_disableBeansRequireSomeProperties);
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
	 * 		.beanProperties(MyBean.<jk>class</jk>, <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(beanClass.getName()).properties(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#properties()}/{@link Bean#p()} - On an annotation on the bean class itself.
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanProperties(Class<?> beanClass, String properties) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(beanClass).p(properties).build());
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
	 * 		.beanProperties(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"foo,bar"</js>))
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code for each entry:
	 * <p class='bcode w800'>
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(key).properties(value.toString()));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#properties()} / {@link Bean#p()}- On an annotation on the bean class itself.
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this builder.
	 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * 	<br>Values are comma-delimited lists of property names.  Non-String objects are first converted to Strings.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanProperties(Map<String,Object> values) {
		for (Map.Entry<String,Object> e : values.entrySet())
			prependTo(BEAN_annotations, BeanAnnotation.create(e.getKey()).p(stringify(e.getValue())).build());
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
	 * 		.beanProperties(<js>"MyBean"</js>, <js>"foo,bar"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo","bar":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(beanClassName).properties(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#properties()} / {@link Bean#p()} - On an annotation on the bean class itself.
	 * </ul>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanProperties(String beanClassName, String properties) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(beanClassName).p(properties).build());
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
	 * 		.beanPropertiesExcludes(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(beanClass.getName()).excludeProperties(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#excludeProperties()} / {@link Bean#xp()}
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(beanClass).xp(properties).build());
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
	 * 		.beanPropertiesExcludes(AMap.of(<js>"MyBean"</js>, <js>"bar,baz"</js>))
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code for each entry:
	 * <p class='bcode w800'>
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(key).excludeProperties(value.toString()));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#excludeProperties()} / {@link Bean#xp()}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this builder.
	 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * 	<br>Values are comma-delimited lists of property names.  Non-String objects are first converted to Strings.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanPropertiesExcludes(Map<String,Object> values) {
		for (Map.Entry<String,Object> e : values.entrySet())
			prependTo(BEAN_annotations, BeanAnnotation.create(e.getKey()).xp(stringify(e.getValue())).build());
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
	 * 		.beanPropertiesExcludes(<js>"MyBean"</js>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(beanClassName).excludeProperties(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#excludeProperties()} / {@link Bean#xp()}
	 * </ul>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(beanClassName).xp(properties).build());
	}

	/**
	 * Read-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 * Note that this is different from the <l>beanProperties</l>/<l>beanPropertiesExcludes</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with read-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesReadOnly(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// All 3 properties will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with read-only property settings.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesReadOnly(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
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
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(beanClass.getName()).readOnlyProperties(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#readOnlyProperties()} / {@link Bean#ro()}
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(beanClass).ro(properties).build());
	}

	/**
	 * Read-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on beans that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 * Note that this is different from the <l>beanProperties</l>/<l>beanPropertyExcludes</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with read-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesReadOnly(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
	 * 		.build();
	 *
	 * 	<jc>// All 3 properties will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with read-only property settings.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesReadOnly(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
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
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(key).readOnlyProperties(value.toString()));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#readOnlyProperties()} / {@link Bean#ro()}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this builder.
	 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * 	<br>Values are comma-delimited lists of property names.  Non-String objects are first converted to Strings.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		for (Map.Entry<String,Object> e : values.entrySet())
			prependTo(BEAN_annotations, BeanAnnotation.create(e.getKey()).ro(stringify(e.getValue())).build());
		return this;
	}

	/**
	 * Read-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 * Note that this is different from the <l>beanProperties</l>/<l>beanPropertyExcludes</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with read-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesReadOnly(<js>"MyBean"</js>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// All 3 properties will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with read-only property settings.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesReadOnly(<js>"MyBean"</js>, <js>"bar,baz"</js>)
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
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(beanClass.getName).readOnlyProperties(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#readOnlyProperties()} / {@link Bean#ro()}
	 * </ul>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(beanClassName).ro(properties).build());
	}

	/**
	 * Write-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 * Note that this is different from the <l>beanProperties</l>/<l>beanPropertiesExcludes</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with write-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesWriteOnly(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Only foo will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with write-only property settings.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesWriteOnly(MyBean.<jk>class</jk>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Parser parses all 3 properties.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(beanClass.getName).writeOnlyProperties(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#writeOnlyProperties()} / {@link Bean#wo()}
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(beanClass).wo(properties).build());
	}

	/**
	 * Write-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 * Note that this is different from the <l>beanProperties</l>/<l>beanPropertiesExcludes</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with write-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesWriteOnly(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
	 * 		.build();
	 *
	 * 	<jc>// Only foo will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with write-only property settings.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesWriteOnly(AMap.<jsm>of</jsm>(<js>"MyBean"</js>, <js>"bar,baz"</js>))
	 * 		.build();
	 *
	 * 	<jc>// Parser parses all 3 properties.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code for each entry:
	 * <p class='bcode w800'>
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(key).writeOnlyProperties(value.toString()));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#writeOnlyProperties()} / {@link Bean#wo()}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this builder.
	 * 	<br>Keys are bean class names which can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * 	<br>Values are comma-delimited lists of property names.  Non-String objects are first converted to Strings.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		for (Map.Entry<String,Object> e : values.entrySet())
			prependTo(BEAN_annotations, BeanAnnotation.create(e.getKey()).wo(stringify(e.getValue())).build());
		return this;
	}

	/**
	 * Write-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 * Note that this is different from the <l>beanProperties</l>/<l>beanPropertiesExcludes</l> settings which include or exclude properties
	 * for both serializers and parsers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf>, <jf>bar</jf>, <jf>baz</jf>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer with write-only property settings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesWriteOnly(<js>"MyBean"</js>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Only foo will be serialized.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a parser with write-only property settings.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanPropertiesWriteOnly(<js>"MyBean"</js>, <js>"bar,baz"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Parser parses all 3 properties.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar',baz:'baz'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method is functionally equivalent to the following code:
	 * <p class='bcode w800'>
	 * 	builder.annotations(<jk>new</jk> BeanAnnotation(beanClassName).writeOnlyProperties(properties));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link Bean#writeOnlyProperties()} / {@link Bean#wo()}
	 * </ul>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(beanClassName).wo(properties).build());
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Bean dictionary.
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
	 * 		.addBeanTypes()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_beanDictionary</jsf>, Foo.<jk>class</jk>)
	 * 		.addTo(<jsf>BEAN_beanDictionary</jsf>, Bar.<jk>class</jk>)
	 * 		.set(<jsf>SERIALIZER_addBeanTypes</jsf>)
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
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder dictionary(Object...values) {
		return prependTo(BEAN_beanDictionary, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Bean dictionary.
	 *
	 * <p>
	 * This is identical to {@link #dictionary(Object...)}, but specifies a dictionary within the context of
	 * a single class as opposed to globally.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.dictionaryOn(MyBean.class, Foo.<jk>class</jk>, Bar.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Parse bean.</jc>
	 * 	MyBean b = p.parse(<js>"{mySimpleField:{_type:'foo',...}}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This is functionally equivalent to the {@link Bean#dictionary()} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#dictionary()}
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param on The class that the dictionary values apply to.
	 * @param values
	 * 	The new values for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder dictionaryOn(Class<?> on, Class<?>...values) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(on).dictionary(values).build());
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
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that excludes the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.example(MyBean.<jk>class</jk>, <jk>new</jk> MyBean().foo(<js>"foo"</js>).bar(123))
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shorthand method for the following code:
	 * <p class='bcode w800'>
	 * 		<jv>builder</jv>.annotations(<jk>new</jk> MarshalledAnnotation().onClass(<jv>pojoClass</jv>).example(SimpleJson.<jsf>DEFAULT</jsf>.toString(<jv>o</jv>)))
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Using this method assumes the serialized form of the object is the same as that produced
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
	 * @param pojoClass The POJO class.
	 * @param o
	 * 	An instance of the POJO class used for examples.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public <T> BeanContextBuilder example(Class<T> pojoClass, T o) {
		return annotations(MarshalledAnnotation.create(pojoClass).example(SimpleJson.DEFAULT.toString(o)).build());
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
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that excludes the 'foo' and 'bar' properties on the MyBean class.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.example(MyBean.<jk>class</jk>, <js>"{foo:'bar'}"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shorthand method for the following code:
	 * <p class='bcode w800'>
	 * 		<jv>builder</jv>.annotations(<jk>new</jk> MarshalledAnnotation().onClass(<jv>pojoClass</jv>).example(<jv>json</jv>))
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
	 * 	<li class='ja'>{@link Marshalled#example()}
	 * </ul>
	 *
	 * @param <T> The POJO class type.
	 * @param pojoClass The POJO class.
	 * @param json The simple JSON representation of the example.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public <T> BeanContextBuilder example(Class<T> pojoClass, String json) {
		return annotations(MarshalledAnnotation.create(pojoClass).example(json).build());
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Find fluent setters.
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
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a fluent setter.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> MyBean foo(String value) {...}
	 * 	}
	 *
	 * 	<jc>// Create a parser that finds fluent setters.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.findFluentSetters()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_findFluentSetters</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Parse into bean using fluent setter.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Beanp @Beanp} annotation can also be used on methods to individually identify them as fluent setters.
	 * 	<li>The {@link Bean#findFluentSetters() @Bean.fluentSetters()} annotation can also be used on classes to specify to look for fluent setters.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_findFluentSetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder findFluentSetters() {
		return set(BEAN_findFluentSetters);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Find fluent setters.
	 *
	 * <p>
	 * Identical to {@link #findFluentSetters()} but enables it on a specific class only.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a fluent setter.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> MyBean foo(String value) {...}
	 * 	}
	 *
	 * 	<jc>// Create a parser that finds fluent setters.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.findFluentSetters(MyBean.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Parse into bean using fluent setter.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>This method is functionally equivalent to using the {@link Bean#findFluentSetters()} annotation.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#findFluentSetters()}
	 * 	<li class='jf'>{@link BeanContext#BEAN_findFluentSetters}
	 * </ul>
	 *
	 * @param on The class that this applies to.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder findFluentSetters(Class<?> on) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(on).findFluentSetters(true).build());
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Ignore invocation errors on getters.
	 *
	 * <p>
	 * When enabled, errors thrown when calling bean getter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a property that throws an exception.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String getFoo() {
	 * 			<jk>throw new</jk> RuntimeException(<js>"foo"</js>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a serializer that ignores bean getter exceptions.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.ingoreInvocationExceptionsOnGetters()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_ignoreInvocationExceptionsOnGetters</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Exception is ignored.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreInvocationExceptionsOnGetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder ignoreInvocationExceptionsOnGetters() {
		return set(BEAN_ignoreInvocationExceptionsOnGetters);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Ignore invocation errors on setters.
	 *
	 * <p>
	 * When enabled, errors thrown when calling bean setter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a property that throws an exception.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public void</jk> setFoo(String foo) {
	 * 			<jk>throw new</jk> RuntimeException(<js>"foo"</js>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a parser that ignores bean setter exceptions.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.ignoreInvocationExceptionsOnSetters()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_ignoreInvocationExceptionsOnSetters</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Exception is ignored.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'bar'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreInvocationExceptionsOnSetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder ignoreInvocationExceptionsOnSetters() {
		return set(BEAN_ignoreInvocationExceptionsOnSetters);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Don't silently ignore missing setters.
	 *
	 * <p>
	 * When enabled, trying to set a value on a bean property without a setter will throw a {@link BeanRuntimeException}.
	 * Otherwise, it will be silently ignored.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a property with a getter but not a setter.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public void</jk> getFoo() {
	 * 			<jk>return</jk> <js>"foo"</js>;
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a parser that throws an exception if a setter is not found but a getter is.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.disableIgnoreMissingSetters()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_disableIgnoreMissingSetters</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Throws a ParseException.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'bar'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link BeanIgnore @BeanIgnore} annotation can also be used on getters and fields to ignore them.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_disableIgnoreMissingSetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder disableIgnoreMissingSetters() {
		return set(BEAN_disableIgnoreMissingSetters);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Don't ignore transient fields.
	 *
	 * <p>
	 * When enabled, methods and fields marked as <jk>transient</jk> will not be ignored as bean properties.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a transient field.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public transient</jk> String <jf>foo</jf> = <js>"foo"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a parser that doesn't ignore transient fields.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.disableIgnoreTransientFields()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_disableIgnoreTransientFields</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo":"foo"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Beanp @Beanp} annotation can also be used on transient fields to keep them from being ignored.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_disableIgnoreTransientFields}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder disableIgnoreTransientFields() {
		return set(BEAN_disableIgnoreTransientFields);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Ignore unknown properties.
	 *
	 * <p>
	 * When enabled, trying to set a value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a single property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf>;
	 * 	}
	 *
	 * 	<jc>// Create a parser that ignores missing bean properties.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.ignoreUnknownBeanProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_ignoreUnknownBeanProperties</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Doesn't throw an exception on unknown 'bar' property.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:'bar'}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreUnknownBeanProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder ignoreUnknownBeanProperties() {
		return set(BEAN_ignoreUnknownBeanProperties);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Don't ignore unknown properties with null values.
	 *
	 * <p>
	 * When enabled, trying to set a <jk>null</jk> value on a non-existent bean property will throw a {@link BeanRuntimeException}.
	 * Otherwise it will be silently ignored.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a single property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf>;
	 * 	}
	 *
	 * 	<jc>// Create a parser that throws an exception on an unknown property even if the value being set is null.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.disableIgnoreUnknownNullBeanProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_disableIgnoreUnknownNullBeanProperties</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Throws a BeanRuntimeException wrapped in a ParseException on the unknown 'bar' property.</jc>
	 * 	MyBean b = p.parse(<js>"{foo:'foo',bar:null}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_disableIgnoreUnknownNullBeanProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder disableIgnoreUnknownNullBeanProperties() {
		return set(BEAN_disableIgnoreUnknownNullBeanProperties);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Implementation classes.
	 *
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation class for the
	 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
	 * parse).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.implClass(MyBean.<jk>class</jk>, MyBeanImpl.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_implClasses</jsf>, MyBean.<jk>class</jk>.getName(), MyBeanImpl.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Instantiates a MyBeanImpl,</jc>
	 * 	MyBean b = p.parse(<js>"..."</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		return annotations(MarshalledAnnotation.create(interfaceClass).implClass(implClass).build());
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Implementation classes.
	 *
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation class for the
	 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
	 * parse).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.implClasses(AMap.<jsm>of</jsm>(MyBean.<jk>class</jk>, MyBeanImpl.<jk>class</jk>))
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_implClasses</jsf>, AMap.<jsm>of</jsm>(MyBean.<jk>class</jk>.getName(), MyBeanImpl.<jk>class</jk>))
	 * 		.build();
	 *
	 * 	<jc>// Instantiates a MyBeanImpl,</jc>
	 * 	MyBean b = p.parse(<js>"..."</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param values
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder implClasses(Map<Class<?>,Class<?>> values) {
		for (Map.Entry<Class<?>,Class<?>> e : values.entrySet())
			annotations(MarshalledAnnotation.create(e.getKey()).implClass(e.getValue()).build());
		return this;
	}

	/**
	 * Identifies a class to be used as the interface class for the specified class and all subclasses.
	 *
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization.
	 * Additional properties on subclasses will be ignored.
	 *
	 * <p class='bcode w800'>
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
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.interfaceClass(A1.<jk>class</jk>, A.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Produces "{"foo":"foo"}"</jc>
	 * 	String json = s.serialize(<jk>new</jk> A1());
	 * </p>
	 *
	 * <p>
	 * This annotation can be used on the parent class so that it filters to all child classes, or can be set
	 * individually on the child classes.
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Bean#interfaceClass() @Bean(interfaceClass)} annotation is the equivalent annotation-based solution.
	 * </ul>
	 *
	 * @param on The class that the interface class applies to.
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder interfaceClass(Class<?> on, Class<?> value) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(on).interfaceClass(value).build());
	}

	/**
	 * Identifies a set of interfaces.
	 *
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization
	 * of implementation classes.  Additional properties on subclasses will be ignored.
	 *
	 * <p class='bcode w800'>
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
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.interfaces(A.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Produces "{"foo":"foo"}"</jc>
	 * 	String json = s.serialize(<jk>new</jk> A1());
	 * </p>
	 *
	 * <p>
	 * This annotation can be used on the parent class so that it filters to all child classes, or can be set
	 * individually on the child classes.
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Bean#interfaceClass() @Bean(interfaceClass)} annotation is the equivalent annotation-based solution.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder interfaces(Class<?>...value) {
		for (Class<?> v : value)
			prependTo(BEAN_annotations, BeanAnnotation.create(v).interfaceClass(v).build());
		return this;
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Bean class exclusions.
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
	 * <p class='bcode w800'>
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
	 *
	 * 	<jc>// Produces "baz" instead of {"foo":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link BeanIgnore @BeanIgnore} annotation can also be used on classes to prevent them from being recognized as beans.
	 * </ul>
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
	@FluentSetter
	public BeanContextBuilder notBeanClasses(Object...values) {
		return addTo(BEAN_notBeanClasses, values);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Bean package exclusions.
	 *
	 * <p>
	 * Used as a convenient way of defining the {@link #notBeanClasses(Object...)} property for entire packages.
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
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>{@link Package} objects.
	 * 		<li>Strings.
	 * 		<li>Arrays and collections of anything in this list.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder notBeanPackages(Object...values) {
		for (Object o : values) {
			if (o instanceof Package)
				addTo(BEAN_notBeanPackages, ((Package) o).getName());
			else if (o instanceof String)
				addTo(BEAN_notBeanPackages, o.toString());
			else if (o instanceof Collection) {
				for (Object o2 : (Collection<?>)o)
					notBeanPackages(o2);
			} else if (o.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(o); i++)
					notBeanPackages(Array.get(o, i));
			}
		}
		return this;
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Bean property namer
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
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a single property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>fooBarBaz</jf> = <js>"fooBarBaz"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that uses Dashed-Lower-Case property names.</jc>
	 * 	<jc>// (e.g. "foo-bar-baz" instead of "fooBarBaz")</jc>
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
	 *
	 * 	<jc>// Produces:  {"foo-bar-baz":"fooBarBaz"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_propertyNamer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicPropertyNamer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder propertyNamer(Class<? extends PropertyNamer> value) {
		return set(BEAN_propertyNamer, value);
	}

	/**
	 * Bean property namer
	 *
	 * <p>
	 * Same as {@link #propertyNamer(Class)} but allows you to specify a namer for a specific class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with a single property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>fooBarBaz</jf> = <js>"fooBarBaz"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that uses Dashed-Lower-Case property names for the MyBean class only.</jc>
	 * 	<jc>// (e.g. "foo-bar-baz" instead of "fooBarBaz")</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.propertyNamer(MyBean.<jk>class</jk>, PropertyNamerDLC.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"foo-bar-baz":"fooBarBaz"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#propertyNamer() Bean(propertyNamer)}
	 * 	<li class='jf'>{@link BeanContext#BEAN_propertyNamer}
	 * </ul>
	 *
	 * @param on The class that the namer applies to.
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicPropertyNamer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder propertyNamer(Class<?> on, Class<? extends PropertyNamer> value) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(on).propertyNamer(value).build());
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Sort bean properties.
	 *
	 * <p>
	 * When enabled, all bean properties will be serialized and access in alphabetical order.
	 * Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 * On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
	 * On Oracle JVMs, the bean properties are not ordered (which follows the official JVM specs).
	 *
	 * <p>
	 * This property is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
	 * to force bean properties to be in a particular order and can just alter the order of the fields/methods
	 * in the Java file.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>c</jf> = <js>"1"</js>;
	 * 		<jk>public</jk> String <jf>b</jf> = <js>"2"</js>;
	 * 		<jk>public</jk> String <jf>a</jf> = <js>"3"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that sorts bean properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sortProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_sortProperties</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"a":"3","b":"2","c":"1"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Bean#sort() @Bean.sort()} annotation can also be used to sort properties on just a single class.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_sortProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder sortProperties() {
		return set(BEAN_sortProperties);
	}

	/**
	 * Sort bean properties.
	 *
	 * <p>
	 * Same as {@link #sortProperties()} but allows you to specify individual bean classes instead of globally.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A bean with 3 properties.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>c</jf> = <js>"1"</js>;
	 * 		<jk>public</jk> String <jf>b</jf> = <js>"2"</js>;
	 * 		<jk>public</jk> String <jf>a</jf> = <js>"3"</js>;
	 * 	}
	 *
	 * 	<jc>// Create a serializer that sorts properties on MyBean.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sortProperties(MyBean.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"a":"3","b":"2","c":"1"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#sort() Bean(sort)}
	 * 	<li class='jf'>{@link BeanContext#BEAN_sortProperties}
	 * </ul>
	 *
	 * @param on The bean classes to sort properties on.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder sortProperties(Class<?>...on) {
		for (Class<?> c : on)
			prependTo(BEAN_annotations, BeanAnnotation.create(c).sort(true).build());
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
	 * <p class='bcode w800'>
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
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.stopClass(C3.<jk>class</jk>, C2.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"p3":"..."}</jc>
	 * 	String json = s.serialize(<jk>new</jk> C3());
	 * </p>
	 *
	 * @param on The class on which the stop class is being applied.
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder stopClass(Class<?> on, Class<?> value) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(on).stopClass(value).build());
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Java object swaps.
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
	 * 		.swaps(MyDateSwap.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_swaps</jsf>, MyDateSwap.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  {"date":"2012-03-03T04:05:06-0500"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 *
	 * 	<jc>// Create a serializer that uses our date swap.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.swaps(MyDateSwap.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Use our parser to parse a bean.</jc>
	 * 	MyBean bean = p.parse(json, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link Swap @Swap} annotation can also be used on classes to identify swaps for the class.
	 * 	<li>The {@link Swap @Swap} annotation can also be used on bean methods and fields to identify swaps for values of those bean properties.
	 * </ul>
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
	@FluentSetter
	public BeanContextBuilder swaps(Object...values) {
		return appendTo(BEAN_swaps, values);
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
	 * <p class='bcode w800'>
	 * 	<jc>// Use _type='mybean' to identify this bean.</jc>
	 * 	<jk>public class</jk> MyBean {...}
	 *
	 * 	<jc>// Create a serializer and specify the type name..</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.typeName(MyBean.<jk>class</jk>, <js>"mybean"</js>)
	 * 		.build();
	 *
	 * 		<jc>// Produces:  {"_type":"mybean",...}</jc>
	 * 		String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Equivalent to the {@link Bean#typeName() Bean(typeName)} annotation.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link Bean#typeName() Bean(typeName)}
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param on
	 * 	The class the type name is being defined on.
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder typeName(Class<?> on, String value) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(on).typeName(value).build());
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Bean type property name.
	 *
	 * <p>
	 * This specifies the name of the bean property used to store the dictionary name of a bean type so that the
	 * parser knows the data type to reconstruct.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// POJOs with @Bean(name) annotations.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"foo"</js>)
	 * 	<jk>public class</jk> Foo {...}
	 * 	<ja>@Bean</ja>(typeName=<js>"bar"</js>)
	 * 	<jk>public class</jk> Bar {...}
	 *
	 * 	<jc>// Create a serializer that uses 't' instead of '_type' for dictionary names.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.typePropertyName(<js>"t"</js>)
	 * 		.dictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_typePropertyName</jsf>, <js>"t"</js>)
	 * 		.addTo(<jsf>BEAN_beanDictionary</jsf>, Foo.<jk>class</jk>)
	 * 		.addTo(<jsf>BEAN_beanDictionary</jsf>, Bar.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// A bean with a field with an indeterminate type.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> Object <jf>mySimpleField</jf>;
	 * 	}
	 *
	 * 	<jc>// Parse bean.</jc>
	 * 	MyBean b = p.parse(<js>"{mySimpleField:{t:'foo',...}}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_typePropertyName}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"_type"</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder typePropertyName(String value) {
		return set(BEAN_typePropertyName, value);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Bean type property name.
	 *
	 * <p>
	 * Same as {@link #typePropertyName(String)} except targets a specific bean class instead of globally.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.typePropertyName(MyBean.<jk>class</jk>, <js>"t"</js>)
	 * 		.dictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Parse bean.</jc>
	 * 	MyBean b = p.parse(<js>"{mySimpleField:{t:'foo',...}}"</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#typePropertyName() Bean(typePropertyName)}
	 * 	<li class='jf'>{@link BeanContext#BEAN_typePropertyName}
	 * </ul>
	 *
	 * @param on The class the type property name applies to.
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"_type"</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder typePropertyName(Class<?> on, String value) {
		return prependTo(BEAN_annotations, BeanAnnotation.create(on).typePropertyName(value).build());
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Use enum names.
	 *
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
	 * 		.set(<jsf>BEAN_useEnumNames</jsf>)
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
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useEnumNames}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder useEnumNames() {
		return set(BEAN_useEnumNames);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Don't use interface proxies.
	 *
	 * <p>
	 * When enabled, interfaces will be instantiated as proxy classes through the use of an
	 * {@link InvocationHandler} if there is no other way of instantiating them.
	 * Otherwise, throws a {@link BeanRuntimeException}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_disableInterfaceProxies}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder disableInterfaceProxies() {
		return set(BEAN_disableInterfaceProxies);
	}

	/**
	 * <i><l>BeanContext</l> configuration property:&emsp;</i>  Use Java Introspector.
	 *
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 * <br>Most {@link Bean @Bean} annotations will be ignored.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that only uses the built-in java bean introspector for finding properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.useJavaBeanIntrospector()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_useJavaBeanIntrospector</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useJavaBeanIntrospector}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BeanContextBuilder useJavaBeanIntrospector() {
		return set(BEAN_useJavaBeanIntrospector);
	}

	// <FluentSetters>

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
	public BeanContextBuilder apply(ContextProperties copyFrom) {
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
	public BeanContextBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder mediaType(MediaType value) {
		super.mediaType(value);
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
	public BeanContextBuilder set(String name) {
		super.set(name);
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

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public BeanContextBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	// </FluentSetters>
}