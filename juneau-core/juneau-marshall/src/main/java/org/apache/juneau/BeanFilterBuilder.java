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

import static org.apache.juneau.internal.StringUtils.*;

import java.beans.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.transform.*;

/**
 * Builder class for {@link BeanFilter} objects.
 *
 * <p>
 * This class is the programmatic equivalent to the aggregation of one or more {@link Bean @Bean} annotations.
 */
public class BeanFilterBuilder {

	Class<?> beanClass;
	String typeName, example;
	ASet<String>
		bpi = ASet.of(),
		bpx = ASet.of(),
		bpro = ASet.of(),
		bpwo = ASet.of();
	Class<?> implClass, interfaceClass, stopClass;
	boolean sortProperties, fluentSetters;
	Object propertyNamer;
	List<Class<?>> dictionary;
	Object interceptor;

	/**
	 * Constructor.
	 *
	 * @param beanClass The bean class that this filter applies to.
	 */
	public BeanFilterBuilder(Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	/**
	 * Applies the information in the specified list of {@link Bean @Bean} annotations to this filter.
	 *
	 * @param annotations The annotations to apply.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder applyAnnotations(List<Bean> annotations) {

		for (Bean b : annotations) {

			if (! b.bpi().isEmpty())
				bpi(split(b.bpi()));

			if (! b.typeName().isEmpty())
				typeName(b.typeName());

			if (b.sort())
				sortProperties(true);

			if (b.fluentSetters())
				fluentSetters(true);

			if (! b.bpx().isEmpty())
				bpx(split(b.bpx()));

			if (! b.bpro().isEmpty())
				bpro(split(b.bpro()));

			if (! b.bpwo().isEmpty())
				bpwo(split(b.bpwo()));

			if (b.propertyNamer() != BasicPropertyNamer.class)
				propertyNamer(b.propertyNamer());

			if (b.interfaceClass() != Null.class)
				interfaceClass(b.interfaceClass());

			if (b.stopClass() != Null.class)
				stopClass(b.stopClass());

			if (b.dictionary().length > 0)
				dictionary(b.dictionary());

			if (b.interceptor() != BeanInterceptor.Default.class)
				interceptor(b.interceptor());

			if (b.implClass() != Null.class)
				implClass(b.implClass());

			if (! b.example().isEmpty())
				example(b.example());
		}
		return this;
	}

	/**
	 * Configuration property:  Bean dictionary type name.
	 *
	 * <p>
	 * Specifies the dictionary type name for this bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			typeName(<js>"mybean"</js>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a serializer or parser.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Produces:  "{_type:'mybean', ...}"</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#typeName()}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder typeName(String value) {
		this.typeName = value;
		return this;
	}

	/**
	 * Bean implementation class.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder implClass(Class<?> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * Bean interface class.
	 *
	 * Identifies a class to be used as the interface class for this and all subclasses.
	 *
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization.
	 * <br>Additional properties on subclasses will be ignored.
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Parent class</jc>
	 * 	<jk>public abstract class</jk> A {
	 * 		<jk>public</jk> String <jf>f0</jf> = <js>"f0"</js>;
	 * 	}
	 *
	 * 	<jc>// Sub class</jc>
	 * 	<jk>public class</jk> A1 <jk>extends</jk> A {
	 * 		<jk>public</jk> String <jf>f1</jf> = <js>"f1"</js>;
	 * 	}
	 *
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> AFilter <jk>extends</jk> BeanFilterBuilder&lt;A&gt; {
	 * 		<jk>public</jk> AFilter() {
	 * 			interfaceClass(A.<jk>class</jk>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a serializer.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(AFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Use it.</jc>
	 * 	A1 a1 = <jk>new</jk> A1();
	 * 	String r = s.serialize(a1);
	 * 	<jsm>assertEquals</jsm>(<js>"{f0:'f0'}"</js>, r);  <jc>// Note f1 is not serialized</jc>
	 * </p>
	 *
	 * <p>
	 * Note that this filter can be used on the parent class so that it filters to all child classes, or can be set
	 * individually on the child classes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#interfaceClass()}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder interfaceClass(Class<?> value) {
		this.interfaceClass = value;
		return this;
	}

	/**
	 * Configuration property:  Bean stop class.
	 *
	 * <p>
	 * Identifies a stop class for this class and all subclasses.
	 *
	 * <p>
	 * Identical in purpose to the stop class specified by {@link Introspector#getBeanInfo(Class, Class)}.
	 * <br>Any properties in the stop class or in its base classes will be ignored during analysis.
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
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> C3Filter <jk>extends</jk> BeanFilterBuilder&lt;C3&gt; {
	 * 		<jk>public</jk> C3Filter() {
	 * 			stopClass(C2.<jk>class</jk>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a serializer.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(C3Filter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Serializes property 'p3', but NOT 'p1' or 'p2'.</jc>
	 * 	String json = s.serialize(<jk>new</jk> C3());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#stopClass()}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder stopClass(Class<?> value) {
		this.stopClass = value;
		return this;
	}

	/**
	 * Configuration property:  Sort bean properties.
	 *
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * <br>Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			sortProperties();
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a serializer.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Properties will be sorted alphabetically.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#sort()}
	 * 	<li class='jf'>{@link BeanContext#BEAN_sortProperties}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder sortProperties(boolean value) {
		this.sortProperties = value;
		return this;
	}

	/**
	 * Configuration property:  Sort bean properties.
	 *
	 * <p>
	 * Shortcut for calling <code>sortProperties(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#sort()}
	 * 	<li class='jf'>{@link BeanContext#BEAN_sortProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder sortProperties() {
		this.sortProperties = true;
		return this;
	}

	/**
	 * Configuration property:  Find fluent setters.
	 *
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
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			fluentSetters();
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#fluentSetters()}
	 * 	<li class='jf'>{@link BeanContext#BEAN_fluentSetters}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder fluentSetters(boolean value) {
		this.fluentSetters = value;
		return this;
	}

	/**
	 * Configuration property:  Find fluent setters.
	 *
	 * <p>
	 * Shortcut for calling <code>fluentSetters(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#fluentSetters()}
	 * 	<li class='jf'>{@link BeanContext#BEAN_fluentSetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder fluentSetters() {
		this.fluentSetters = true;
		return this;
	}

	/**
	 * Configuration property:  Bean property namer
	 *
	 * <p>
	 * The class to use for calculating bean property names.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			<jc>// Use Dashed-Lower-Case property names.</jc>
	 * 			<jc>// (e.g. "foo-bar-url" instead of "fooBarURL")</jc>
	 * 			propertyNamer(PropertyNamerDLC.<jk>class</jk>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a serializer or parser.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Properties names will be Dashed-Lower-Case.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#propertyNamer()}
	 * 	<li class='jf'>{@link BeanContext#BEAN_propertyNamer}
	 * 	<li class='jc'>{@link PropertyNamer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicPropertyNamer}.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder propertyNamer(Class<? extends PropertyNamer> value) {
		this.propertyNamer = value;
		return this;
	}

	/**
	 * Configuration property:  Bean property includes.
	 *
	 * <p>
	 * Specifies the set and order of names of properties associated with the bean class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			bpi(<js>"foo,bar,baz"</js>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a serializer.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Only serializes the properties 'foo', 'bar', and 'baz'.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#bpi()}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpi(Class, String)}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpi(String, String)}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpi(Map)}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>Values can contain comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder bpi(String...value) {
		this.bpi = ASet.of();
		for (String v : value)
			bpi.a(split(v));
		return this;
	}

	/**
	 * Configuration property:  Bean property excludes.
	 *
	 * <p>
	 * Specifies properties to exclude from the bean class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			bpx(<js>"foo,bar"</js>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a serializer.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Serializes all properties except for 'foo' and 'bar'.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#bpx()}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpx(Class, String)}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpx(String, String)}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpx(Map)}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>Values can contain comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder bpx(String...value) {
		this.bpx = ASet.of();
		for (String v : value)
			bpx.a(split(v));
		return this;
	}

	/**
	 * Configuration property:  Read-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			bpro(<js>"foo,bar"</js>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a parser.</jc>
	 *  ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Parsers all properties except for 'foo' and 'bar'.</jc>
	 * 	MyBean b = p.parse(<js>"..."</js>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#bpro()}
	 * 	<li class='ja'>{@link Beanp#ro()}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpro(Class, String)}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpro(String, String)}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpro(Map)}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>Values can contain comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder bpro(String...value) {
		this.bpro = ASet.of();
		for (String v : value)
			bpro.a(split(v));
		return this;
	}

	/**
	 * Configuration property:  Write-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			bpwo(<js>"foo,bar"</js>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a serializer.</jc>
	 *  WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Serializes all properties except for 'foo' and 'bar'.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#bpwo()}
	 * 	<li class='ja'>{@link Beanp#wo()}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpwo(Class, String)}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpwo(String, String)}
	 * 	<li class='jm'>{@link BeanContextBuilder#bpwo(Map)}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>Values can contain comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder bpwo(String...value) {
		this.bpwo = ASet.of();
		for (String v : value)
			bpwo.a(split(v));
		return this;
	}

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * <p>
	 * Adds to the list of classes that make up the bean dictionary for this bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			<jc>// Our bean contains generic collections of Foo and Bar objects.</jc>
	 * 			beanDictionary(Foo.<jk>class</jk>, Bar.<jk>class</jk>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a parser.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyFilter.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Instantiate our bean.</jc>
	 * 	MyBean myBean = p.parse(json);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#dictionary()}
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder dictionary(Class<?>...values) {
		if (dictionary == null)
			dictionary = Arrays.asList(values);
		else for (Class<?> cc : values)
			dictionary.add(cc);
		return this;
	}

	/**
	 * Example.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder example(String value) {
		this.example = value;
		return this;
	}

	/**
	 * Configuration property:  Bean interceptor.
	 *
	 * <p>
	 * The interceptor to use for intercepting and altering getter and setter calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our filter.</jc>
	 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;MyBean&gt; {
	 * 		<jk>public</jk> MyFilter() {
	 * 			<jc>// Our bean contains generic collections of Foo and Bar objects.</jc>
	 * 			interceptor(AddressInterceptor.<jk>class</jk>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Register it with a serializer or parser.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.beanFilters(MyFilter.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Bean#interceptor()}
	 * 	<li class='jc'>{@link BeanInterceptor}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link BeanInterceptor}.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder interceptor(Class<?> value) {
		this.interceptor = value;
		return this;
	}

	/**
	 * Creates a {@link BeanFilter} with settings in this builder class.
	 *
	 * @return A new {@link BeanFilter} instance.
	 */
	public BeanFilter build() {
		return new BeanFilter(this);
	}
}
