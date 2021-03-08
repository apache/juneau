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

import static org.apache.juneau.Context.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.csv.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jso.annotation.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.msgpack.annotation.*;
import org.apache.juneau.oapi.annotation.*;
import org.apache.juneau.parser.annotation.*;
import org.apache.juneau.plaintext.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.soap.annotation.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.uon.annotation.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Base builder class for building instances of any context objects configured through property stores.
 */
@FluentSetters
public abstract class ContextBuilder {

	/** Contains all the modifiable settings for the implementation class. */
	private final ContextPropertiesBuilder cpb;

	/**
	 * Constructor.
	 * Default settings.
	 */
	public ContextBuilder() {
		this.cpb = ContextProperties.create();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public ContextBuilder(Context copyFrom) {
		this.cpb = copyFrom == null ? ContextProperties.DEFAULT.copy() : copyFrom.properties.copy();
	}

	/**
	 * Build the object.
	 *
	 * @return
	 * 	The built object.
	 * 	<br>Subsequent calls to this method will create new instances (unless context object is cacheable).
	 */
	public abstract Context build();

	/**
	 * Copies the settings from the specified property store into this builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a free-form set of properties.</jc>
	 * 	ContextProperties <jv>properties</jv> = ContextProperties
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_sortMaps</jsf>)
	 * 		.set(<jsf>BEAN_sortProperties</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Create a serializer that uses those settings.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.apply(<jv>properties</jv>)
	 * 		.build();
	 * </p>
	 *
	 * @param copyFrom The property store whose settings are being copied.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder apply(ContextProperties copyFrom) {
		this.cpb.apply(copyFrom);
		return this;
	}

	/**
	 * Applies a set of annotations to this property store.
	 *
	 * <p>
	 * The {@link AnnotationList} object is an ordered list of annotations and the classes/methods/packages they were found on.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A class annotated with a config annotation.</jc>
	 * 	<ja>@BeanConfig</ja>(sortProperties=<js>"$S{sortProperties,false}"</js>)
	 * 	<jk>public class</jk> MyClass {...}
	 *
	 * 	<jc>// Find all annotations that themselves are annotated with @ContextPropertiesApply.</jc>
	 * 	AnnotationList <jv>al</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>)
	 * 		.getAnnotationList(ConfigAnnotationFilter.<jsf>INSTANCE</jsf>);
	 *
	 * 	<jc>// Use the default VarResolver to resolve any variables in the annotation fields.</jc>
	 * 	VarResolverSession <jv>vs</jv> = VarResolver.<jsf>DEFAULT</jsf>.createSession();
	 *
	 * 	<jc>// Apply any settings found on the annotations.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.applyAnnotations(<jv>al</jv>, <jv>vs</jv>)
	 * 		.build();
	 * </p>
	 *
	 * @param al The list of all annotations annotated with {@link ContextPropertiesApply}.
	 * @param r The string resolver for resolving variables in annotation values.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		this.cpb.applyAnnotations(al, r);
		return this;
	}

	/**
	 * Applies any of the various <ja>@XConfig</ja> annotations on the specified class to this context.
	 *
	 * <p>
	 * Any annotations found that themselves are annotated with {@link ContextPropertiesApply} will be resolved and
	 * applied as properties to this builder.  These annotations include:
	 * <ul class='javatree'>
	 * 	<li class ='ja'>{@link BeanConfig}
	 * 	<li class ='ja'>{@link CsvConfig}
	 * 	<li class ='ja'>{@link HtmlConfig}
	 * 	<li class ='ja'>{@link HtmlDocConfig}
	 * 	<li class ='ja'>{@link JsoConfig}
	 * 	<li class ='ja'>{@link JsonConfig}
	 * 	<li class ='ja'>{@link JsonSchemaConfig}
	 * 	<li class ='ja'>{@link MsgPackConfig}
	 * 	<li class ='ja'>{@link OpenApiConfig}
	 * 	<li class ='ja'>{@link ParserConfig}
	 * 	<li class ='ja'>{@link PlainTextConfig}
	 * 	<li class ='ja'>{@link SerializerConfig}
	 * 	<li class ='ja'>{@link SoapXmlConfig}
	 * 	<li class ='ja'>{@link UonConfig}
	 * 	<li class ='ja'>{@link UrlEncodingConfig}
	 * 	<li class ='ja'>{@link XmlConfig}
	 * 	<li class ='ja'><c>RdfConfig</c>
	 * </ul>
	 *
	 * <p>
	 * Annotations on classes are appended in the following order:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * <p>
	 * The default var resolver {@link VarResolver#DEFAULT} is used to resolve any variables in annotation field values.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A class annotated with a config annotation.</jc>
	 * 	<ja>@BeanConfig</ja>(sortProperties=<js>"$S{sortProperties,false}"</js>)
	 * 	<jk>public class</jk> MyClass {...}
	 *
	 * 	<jc>// Apply any settings found on the annotations.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.applyAnnotations(MyClass.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * @param fromClasses The classes on which the annotations are defined.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder applyAnnotations(Class<?>...fromClasses) {
		for (Class<?> c : fromClasses)
			applyAnnotations(ClassInfo.of(c).getAnnotationList(ConfigAnnotationFilter.INSTANCE), VarResolver.DEFAULT.createSession());
		return this;
	}

	/**
	 * Applies any of the various <ja>@XConfig</ja> annotations on the specified method to this context.
	 *
	 * <p>
	 * Any annotations found that themselves are annotated with {@link ContextPropertiesApply} will be resolved and
	 * applied as properties to this builder.  These annotations include:
	 * <ul class='javatree'>
	 * 	<li class ='ja'>{@link BeanConfig}
	 * 	<li class ='ja'>{@link CsvConfig}
	 * 	<li class ='ja'>{@link HtmlConfig}
	 * 	<li class ='ja'>{@link HtmlDocConfig}
	 * 	<li class ='ja'>{@link JsoConfig}
	 * 	<li class ='ja'>{@link JsonConfig}
	 * 	<li class ='ja'>{@link JsonSchemaConfig}
	 * 	<li class ='ja'>{@link MsgPackConfig}
	 * 	<li class ='ja'>{@link OpenApiConfig}
	 * 	<li class ='ja'>{@link ParserConfig}
	 * 	<li class ='ja'>{@link PlainTextConfig}
	 * 	<li class ='ja'>{@link SerializerConfig}
	 * 	<li class ='ja'>{@link SoapXmlConfig}
	 * 	<li class ='ja'>{@link UonConfig}
	 * 	<li class ='ja'>{@link UrlEncodingConfig}
	 * 	<li class ='ja'>{@link XmlConfig}
	 * 	<li class ='ja'><c>RdfConfig</c>
	 * </ul>
	 *
	 * <p>
	 * Annotations on methods are appended in the following order:
	 * <ol>
	 * 	<li>On the package of the method class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On the method class.
	 * 	<li>On this method and matching methods ordered parent-to-child.
	 * </ol>
	 *
	 * <p>
	 * The default var resolver {@link VarResolver#DEFAULT} is used to resolve any variables in annotation field values.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A method annotated with a config annotation.</jc>
	 * 	<jk>public class</jk> MyClass {
	 * 		<ja>@BeanConfig</ja>(sortProperties=<js>"$S{sortProperties,false}"</js>)
	 * 		<jk>public void</jk> myMethod() {...}
	 * 	}
	 *
	 * 	<jc>// Apply any settings found on the annotations.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.applyAnnotations(MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param fromMethods The methods on which the annotations are defined.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder applyAnnotations(Method...fromMethods) {
		for (Method m : fromMethods)
			applyAnnotations(MethodInfo.of(m).getAnnotationList(ConfigAnnotationFilter.INSTANCE), VarResolver.DEFAULT.createSession());
		return this;
	}

	/**
	 * Build a new instance of the specified object.
	 *
	 * <p>
	 * Creates a new instance of the specified context-based class, or an existing instance if one with the equivalent
	 * property store was already created.
	 *
	 * @param c The subclass of {@link Context} to instantiate.
	 * @return A new object using the settings defined in this builder.
	 */

	public <T extends Context> T build(Class<T> c) {
		return ContextCache.INSTANCE.create(c, getContextProperties());
	}

	/**
	 * Returns a read-only snapshot of the current properties on this builder.
	 *
	 * @return A property store object.
	 */
	public ContextProperties getContextProperties() {
		return cpb.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>Context</l> configuration property:&emsp;</i>  Debug mode.
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
	 * 		.set(<jsf>BEAN_debug</jsf>)
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
	 * 	<li class='jf'>{@link Context#CONTEXT_debug}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder debug() {
		return set(CONTEXT_debug);
	}

	/**
	 * <i><l>Context</l> configuration property:&emsp;</i>  Locale.
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
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Context#CONTEXT_locale}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder locale(Locale value) {
		return set(CONTEXT_locale, value);
	}

	/**
	 * <i><l>Context</l> configuration property:&emsp;</i>  Media type.
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
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Context#CONTEXT_mediaType}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder mediaType(MediaType value) {
		return set(CONTEXT_mediaType, value);
	}

	/**
	 * <i><l>Context</l> configuration property:&emsp;</i>  TimeZone.
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
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Context#CONTEXT_timeZone}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder timeZone(TimeZone value) {
		return set(CONTEXT_timeZone, value);
	}

	/**
	 * Sets a free-form configuration property on this object.
	 *
	 * <p>
	 * Provides the ability to specify configuration property values in a generic fashion.
	 *
	 * <p>
	 * Property names must have the following format that identify their datatype...
	 * <p class='bcode w800'>
	 * 	<js>"{class}.{name}.{type}"</js>
	 * </p>
	 * <p>
	 * ...where the parts consist of the following...
	 * <ul>
	 * 	<li><js>"{class}"</js> - The group name of the property (e.g. <js>"JsonSerializer"</js>).
	 * 		<br>It's always going to be the simple class name of the class it's associated with.
	 * 	<li><js>"{name}"</js> - The property name (e.g. <js>"useWhitespace"</js>).
	 * 	<li><js>"{type}"</js> - The property data type.
	 * 		<br>A 1 or 2 character string that identifies the data type of the property.
	 * 		<br>Valid values are:
	 * 		<ul>
	 * 			<li><js>"s"</js> - <c>String</c>
	 * 			<li><js>"b"</js> - <c>Boolean</c>
	 * 			<li><js>"i"</js> - <c>Integer</c>
	 * 			<li><js>"c"</js> - <c>Class</c>
	 * 			<li><js>"o"</js> - <c>Object</c>
	 * 			<li><js>"ss"</js> - <c>TreeSet&lt;String&gt;</c>
	 * 			<li><js>"si"</js> - <c>TreeSet&lt;Integer&gt;</c>
	 * 			<li><js>"sc"</js> - <c>TreeSet&lt;Class&gt;</c>
	 * 			<li><js>"ls"</js> - <c>Linkedlist&lt;String&gt;</c>
	 * 			<li><js>"li"</js> - <c>Linkedlist&lt;Integer&gt;</c>
	 * 			<li><js>"lc"</js> - <c>Linkedlist&lt;Class&gt;</c>
	 * 			<li><js>"lo"</js> - <c>Linkedlist&lt;Object&gt;</c>
	 * 			<li><js>"sms"</js> - <c>TreeMap&lt;String,String&gt;</c>
	 * 			<li><js>"smi"</js> - <c>TreeMap&lt;String,Integer&gt;</c>
	 * 			<li><js>"smc"</js> - <c>TreeMap&lt;String,Class&gt;</c>
	 * 			<li><js>"smo"</js> - <c>TreeMap&lt;String,Object&gt;</c>
	 * 			<li><js>"oms"</js> - <c>LinkedHashMap&lt;String,String&gt;</c>
	 * 			<li><js>"omi"</js> - <c>LinkedHashMap&lt;String,Integer&gt;</c>
	 * 			<li><js>"omc"</js> - <c>LinkedHashMap&lt;String,Class&gt;</c>
	 * 			<li><js>"omo"</js> - <c>LinkedHashMap&lt;String,Object&gt;</c>
	 * 		</ul>
	 * </ul>
	 *
	 * <p>
	 * For example, <js>"BeanContext.swaps.lc"</js> refers to a property on the <l>BeanContext</l> class
	 * called <l>swaps</l> that has a data type of <l>List&lt;Class&gt;</l>.
	 *
	 * <p>
	 * Property values get 'normalized' when they get set.
	 * For example, calling <c>set(<js>"BeanContext.debug.b"</js>, <js>"true"</js>)</c> will cause the property
	 * value to be converted to a boolean.  This allows the underlying {@link ContextProperties} class to be comparable
	 * and useful in determining whether a cached instance of a context object can be returned.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that sorts maps and bean properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sortMaps()
	 * 		.sortProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use generic set() method.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEAN_sortMaps</jsf>)
	 * 		.set(<jsf>BEAN_sortProperties</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * As a general rule, builders don't typically have "unsetter" methods.  For example, once you've set strict
	 * mode on the <l>ParserBuilder</l> class, a method does not exist for unsetting it.
	 * This method can be used in these rare cases where you need to unset a value by setting it to <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Clone an existing serializer and unset the sort settings.</jc>
	 * 	s = s.<jsm>builder</jsm>()
	 * 		.set(<jsf>BEAN_sortMaps</jsf>, <jk>null</jk>)
	 * 		.set(<jsf>BEAN_sortProperties</jsf>, <jk>null</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * 	The property value.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"s"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"b"</js> - Any <l>Object</l> converted to a <l>Boolean</l> using <c>Boolean.<jsm>parseBoolean</jsm>(value.toString())</c>.
	 * 		<li><js>"i"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"c"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"o"</js> - Left as-is.
	 * 		<li><js>"ss"</js>,<js>"si"</js>,<js>"sc"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"ls"</js>,<js>"li"</js>,<js>"lc"</js>,<js>"lo"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"sms"</js>,<js>"smi"</js>,<js>"smc"</js>,<js>"smo"</js> - Any sorted map of any convertible <l>Objects</l> or a JSON Object string.
	 * 		<li><js>"oms"</js>,<js>"omi"</js>,<js>"omc"</js>,<js>"omo"</js> - Any ordered map of any convertible <l>Objects</l> or a JSON Object string.
	 * 	</ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder set(String name, Object value) {
		cpb.set(name, value);
		return this;
	}

	/**
	 * Convenience method for setting a boolean property to <jk>true</jk>.
	 *
	 * @param name The property name.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder set(String name) {
		Assertions.assertString(name).msg("Property ''{0}'' is not boolean.", name).endsWith(".b");
		cpb.set(name);
		return this;
	}

	/**
	 * Convenience method for setting a property to <jk>null</jk>.
	 *
	 * @param name The property name.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder unset(String name) {
		cpb.unset(name);
		return this;
	}

	/**
	 * Peeks at a free-form configuration property on this object.
	 *
	 * <p>
	 * Allows you to look at the raw value of a configuration property while it's still in the builder.
	 *
	 * @param key The property name.
	 * @return
	 * 	The raw value of the configuration property as it was passed in through this API.
	 * 	<br><jk>null</jk> if not set.
	 */
	public Object peek(String key) {
		return cpb.peek(key);
	}

	/**
	 * Peeks at a configuration property on this object.
	 *
	 * <p>
	 * Allows you to look at the raw value of a configuration property while it's still in the builder.
	 *
	 * <p>
	 * Converts the value from the current raw form into the specified data type.
	 *
	 * @param c The type to convert to.
	 * @param key The property name.
	 * @return
	 * 	The converted value of the configuration property after conversion from the raw value.
	 * 	<br><jk>null</jk> if not set.
	 * @param <T> The type to convert to.
	 */
	public <T> T peek(Class<T> c, String key) {
		return cpb.peek(c, key);
	}

	/**
	 * Sets multiple free-form configuration properties on this object replacing all previous values.
	 *
	 * <p>
	 * Identical in function to {@link #set(String, Object)} but allows you to specify multiple values at once.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that sorts maps and bean properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sortMaps()
	 * 		.sortProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use generic set(Map) method.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(
	 * 			AMap.<jsm>of</jsm>(
	 * 				<jsf>BEAN_sortMaps</jsf>, <jk>true</jk>,
	 * 				<jsf>BEAN_sortProperties</jsf>, <jk>true</jk>
	 * 			)
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param properties
	 * 	The properties to set on this class.
	 * 	<br>The keys must be strings.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"s"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"b"</js> - Any <l>Object</l> converted to a <l>Boolean</l> using <c>Boolean.<jsm>parseBoolean</jsm>(value.toString())</c>.
	 * 		<li><js>"i"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"c"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"o"</js> - Left as-is.
	 * 		<li><js>"ss"</js>,<js>"si"</js>,<js>"sc"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"ls"</js>,<js>"li"</js>,<js>"lc"</js>,<js>"lo"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"sms"</js>,<js>"smi"</js>,<js>"smc"</js>,<js>"smo"</js> - Any sorted map of any convertible <l>Objects</l> or a JSON Object string.
	 * 		<li><js>"oms"</js>,<js>"omi"</js>,<js>"omc"</js>,<js>"omo"</js> - Any ordered map of any convertible <l>Objects</l> or a JSON Object string.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder set(Map<String,Object> properties) {
		cpb.set(properties);
		return this;
	}

	/**
	 * Adds multiple free-form configuration properties on this object without first clearing out any previous values.
	 *
	 * <p>
	 * Identical in function to {@link #set(String, Object)} but allows you to specify multiple values at once.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that sorts bean properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sortMaps()
	 * 		.sortProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use generic add() method.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.add(
	 * 			AMap.<jsm>of</jsm>(
	 * 				<jsf>BEAN_sortMaps</jsf>, <jk>true</jk>,
	 * 				<jsf>BEAN_sortProperties</jsf>, <jk>true</jk>
	 * 			)
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param properties
	 * 	The properties to set on this class.
	 * 	<br>The keys must be strings.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"s"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"b"</js> - Any <l>Object</l> converted to a <l>Boolean</l> using <c>Boolean.<jsm>parseBoolean</jsm>(value.toString())</c>.
	 * 		<li><js>"i"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"c"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"o"</js> - Left as-is.
	 * 		<li><js>"ss"</js>,<js>"si"</js>,<js>"sc"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"ls"</js>,<js>"li"</js>,<js>"lc"</js>,<js>"lo"</js> - Any collection or array of any convertible <l>Objects</l> or a JSON Array string.
	 * 		<li><js>"sms"</js>,<js>"smi"</js>,<js>"smc"</js>,<js>"smo"</js> - Any sorted map of any convertible <l>Objects</l> or a JSON Object string.
	 * 		<li><js>"oms"</js>,<js>"omi"</js>,<js>"omc"</js>,<js>"omo"</js> - Any ordered map of any convertible <l>Objects</l> or a JSON Object string.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ContextBuilder add(Map<String,Object> properties) {
		cpb.add(properties);
		return this;
	}

	/**
	 * Adds a free-form value to a SET property.
	 *
	 * <p>
	 * SET properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"ss"</js> - <c>TreeSet&lt;String&gt;</c>
	 * 	<li><js>"si"</js> - <c>TreeSet&lt;Integer&gt;</c>
	 * 	<li><js>"sc"</js> - <c>TreeSet&lt;Class&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * For example, the {@link BeanContext#BEAN_notBeanClasses} property which has the value <js>"BeanContext.notBeanClasses.sc"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that forces MyNotBean classes to be converted to strings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.notBeanClasses(MyNotBean.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use generic addTo() method.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addTo(<jsf>BEAN_notBeanClasses</jsf>, MyNotBean.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * 	The new value to add to the SET property.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"ss"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"si"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"sc"</js> - Only <l>Class</l> objects are allowed.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	@FluentSetter
	public ContextBuilder addTo(String name, Object value) {
		cpb.addTo(name, value);
		return this;
	}

	/**
	 * Adds a free-form value to the end of a LIST property.
	 *
	 * <p>
	 * LIST properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"ls"</js> - <c>Linkedlist&lt;String&gt;</c>
	 * 	<li><js>"li"</js> - <c>Linkedlist&lt;Integer&gt;</c>
	 * 	<li><js>"lc"</js> - <c>Linkedlist&lt;Class&gt;</c>
	 * 	<li><js>"lo"</js> - <c>Linkedlist&lt;Object&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * For example, the {@link BeanContext#BEAN_swaps} property which has the value <js>"BeanContext.swaps.lo"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that converts Temporal objects to Basic ISO date strings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.swaps(TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use generic appendTo() method.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.appendTo(<jsf>BEAN_swaps</jsf>, TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * The new value to add to the LIST property.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"ls"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"li"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"lc"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"lo"</js> - Left as-is.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a LIST property.
	 */
	@FluentSetter
	public ContextBuilder appendTo(String name, Object value) {
		cpb.appendTo(name, value);
		return this;
	}

	/**
	 * Adds a free-form value to the beginning of a LIST property.
	 *
	 * <p>
	 * LIST properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"ls"</js> - <c>Linkedlist&lt;String&gt;</c>
	 * 	<li><js>"li"</js> - <c>Linkedlist&lt;Integer&gt;</c>
	 * 	<li><js>"lc"</js> - <c>Linkedlist&lt;Class&gt;</c>
	 * 	<li><js>"lo"</js> - <c>Linkedlist&lt;Object&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * For example, the {@link BeanContext#BEAN_swaps} property which has the value <js>"BeanContext.swaps.lo"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that converts Temporal objects to Basic ISO date strings.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.swaps(TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use generic prependTo() method.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.prependTo(<jsf>BEAN_swaps</jsf>, TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * 	The new value to add to the LIST property.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"ls"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"li"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"lc"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"lo"</js> - Left as-is.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a LIST property.
	 */
	@FluentSetter
	public ContextBuilder prependTo(String name, Object value) {
		cpb.prependTo(name, value);
		return this;
	}

	/**
	 * Adds or overwrites a free-form entry in a MAP property.
	 *
	 * <p>
	 * MAP properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"sms"</js> - <c>TreeMap&lt;String,String&gt;</c>
	 * 	<li><js>"smi"</js> - <c>TreeMap&lt;String,Integer&gt;</c>
	 * 	<li><js>"smc"</js> - <c>TreeMap&lt;String,Class&gt;</c>
	 * 	<li><js>"smo"</js> - <c>TreeMap&lt;String,Object&gt;</c>
	 * 	<li><js>"oms"</js> - <c>LinkedHashMap&lt;String,String&gt;</c>
	 * 	<li><js>"omi"</js> - <c>LinkedHashMap&lt;String,Integer&gt;</c>
	 * 	<li><js>"omc"</js> - <c>LinkedHashMap&lt;String,Class&gt;</c>
	 * 	<li><js>"omo"</js> - <c>LinkedHashMap&lt;String,Object&gt;</c>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value
	 * 	The property value map value.
	 * 	<br>The valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"sms"</js>,<js>"oms"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"smi"</js>,<js>"omi"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"smc"</js>,<js>"omc"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"smo"</js>,<js>"omo"</js> - Left as-is.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	@FluentSetter
	public ContextBuilder putTo(String name, String key, Object value) {
		cpb.putTo(name, key, value);
		return this;
	}

	/**
	 * Adds or overwrites multiple free-form entries in a MAP property.
	 *
	 * <p>
	 * MAP properties are those properties with one of the following type parts:
	 * <ul>
	 * 	<li><js>"sms"</js> - <c>TreeMap&lt;String,String&gt;</c>
	 * 	<li><js>"smi"</js> - <c>TreeMap&lt;String,Integer&gt;</c>
	 * 	<li><js>"smc"</js> - <c>TreeMap&lt;String,Class&gt;</c>
	 * 	<li><js>"smo"</js> - <c>TreeMap&lt;String,Object&gt;</c>
	 * 	<li><js>"oms"</js> - <c>LinkedHashMap&lt;String,String&gt;</c>
	 * 	<li><js>"omi"</js> - <c>LinkedHashMap&lt;String,Integer&gt;</c>
	 * 	<li><js>"omc"</js> - <c>LinkedHashMap&lt;String,Class&gt;</c>
	 * 	<li><js>"omo"</js> - <c>LinkedHashMap&lt;String,Object&gt;</c>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value
	 * 	Either a JSON Object string or a {@link Map} whose valid value types depend on the property type:
	 * 	<ul>
	 * 		<li><js>"sms"</js>,<js>"oms"</js> - Any <l>Object</l> converted to a <l>String</l> using <c>value.toString()</c>.
	 * 		<li><js>"smi"</js>,<js>"omi"</js> - Any <l>Object</l> converted to an <l>Integer</l> using <c>Integer.<jsm>valueOf</jsm>(value.toString())</c>.
	 * 		<li><js>"smc"</js>,<js>"omc"</js> - Only <l>Class</l> objects are allowed.
	 * 		<li><js>"smo"</js>,<js>"omo"</js> - Left as-is.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	@FluentSetter
	public ContextBuilder putAllTo(String name, Object value) {
		cpb.putAllTo(name, value);
		return this;
	}

	/**
	 * Removes a free-form value from a SET, LIST, or MAP property.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that specifies the concrete implementation class for an interface.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.swaps(TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Clone the previous serializer but remove the swap.</jc>
	 * 	s = s
	 * 		.<jsm>builder</jsm>()
	 * 		.removeFrom(<jsf>BEAN_swaps</jsf>, TemoralCalendarSwap.BasicIsoDate.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link ContextProperties}
	 * 	<li class='jm'>{@link #set(String, Object)}
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value The property value in the SET/LIST/MAP property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET/LIST/MAP property.
	 */
	@FluentSetter
	public ContextBuilder removeFrom(String name, Object value) {
		cpb.removeFrom(name, value);
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}