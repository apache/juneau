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
package org.apache.juneau.serializer;

import static org.apache.juneau.serializer.Serializer.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for building instances of serializers.
 */
public class SerializerBuilder extends BeanTraverseBuilder {

	/**
	 * Constructor, default settings.
	 */
	public SerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public SerializerBuilder(PropertyStore ps) {
		super(ps);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Add <js>"_type"</js> properties when needed.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #addBeanTypes()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder addBeanTypes(boolean value) {
		return set(SERIALIZER_addBeanTypes, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * When enabled, <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * <br>For example, when serializing a <c>Map&lt;String,Object&gt;</c> field where the bean class cannot be determined from
	 * the type of the values.
	 *
	 * <p>
	 * Note the differences between the following settings:
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link #addRootType()} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #addBeanTypes()} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that adds _type to nodes.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addBeanTypes()
	 * 		.build();
	 *
	 * 	<jc>// Our map of beans to serialize.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 * 	OMap map = OMap.of(<js>"foo"</js>, <jk>new</jk> MyBean());
	 *
	 * 	<jc>// Will contain:  {"foo":{"_type":"mybean","foo":"bar"}}</jc>
	 * 	String json = s.serialize(map);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder addBeanTypes() {
		return set(SERIALIZER_addBeanTypes, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Add type attribute to root nodes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #addRootType()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder addRootType(boolean value) {
		return set(SERIALIZER_addRootType, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Add type attribute to root nodes.
	 *
	 * <p>
	 * When enabled, <js>"_type"</js> properties will be added to top-level beans.
	 *
	 * <p>
	 * When disabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
	 * type information that might normally be included to determine the data type will not be serialized.
	 *
	 * <p>
	 * For example, when serializing a top-level POJO with a {@link Bean#typeName() @Bean(typeName)} value, a
	 * <js>'_type'</js> attribute will only be added when this setting is enabled.
	 *
	 * <p>
	 * Note the differences between the following settings:
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link #addRootType()} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #addBeanTypes()} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that adds _type to root node.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addRootType()
	 * 		.build();
	 *
	 * 	<jc>// Our bean to serialize.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Will contain:  {"_type":"mybean","foo":"bar"}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder addRootType() {
		return set(SERIALIZER_addRootType, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Don't trim null bean property values.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #keepNullProperties()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder keepNullProperties(boolean value) {
		return set(SERIALIZER_keepNullProperties, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Don't trim null bean property values.
	 *
	 * <p>
	 * When enabled, null bean values will be serialized to the output.
	 *
	 * <ul class='notes'>
	 * 	<li>Not enabling this setting will cause <c>Map</c>s with <jk>null</jk> values to be lost during parsing.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that serializes null properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.keepNullProperties()
	 * 		.build();
	 *
	 * 	<jc>// Our bean to serialize.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <jk>null</jk>;
	 * 	}
	 *
	 * 	<jc>// Will contain "{foo:null}".</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_keepNullProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder keepNullProperties() {
		return set(SERIALIZER_keepNullProperties, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Serializer listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our serializer listener.</jc>
	 * 	<jc>// Simply captures all errors.</jc>
	 * 	<jk>public class</jk> MySerializerListener <jk>extends</jk> SerializerListener {
	 *
	 * 		<jc>// A simple property to store our events.</jc>
	 * 		<jk>public</jk> List&lt;String&gt; <jf>events</jf> = <jk>new</jk> LinkedList&lt;&gt;();
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> &lt;T&gt; <jk>void</jk> onError(SerializerSession session, Throwable t, String msg) {
	 * 			<jf>events</jf>.add(session.getLastLocation() + <js>","</js> + msg + <js>","</js> + t);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a serializer using our listener.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.listener(MySerializerListener.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Create a session object.</jc>
	 * 	<jc>// Needed because listeners are created per-session.</jc>
	 * 	<jk>try</jk> (WriterSerializerSession ss = s.createSession()) {
	 *
	 * 		<jc>// Serialize a bean.</jc>
	 * 		String json = ss.serialize(<jk>new</jk> MyBean());
	 *
	 * 		<jc>// Get the listener.</jc>
	 * 		MySerializerListener l = ss.getListener(MySerializerListener.<jk>class</jk>);
	 *
	 * 		<jc>// Dump the results to the console.</jc>
	 * 		SimpleJsonSerializer.<jsf>DEFAULT</jsf>.println(l.<jf>events</jf>);
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder listener(Class<? extends SerializerListener> value) {
		return set(SERIALIZER_listener, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Sort arrays and collections alphabetically.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #sortCollections()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder sortCollections(boolean value) {
		return set(SERIALIZER_sortCollections, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Sort arrays and collections alphabetically.
	 *
	 * <p>
	 * When enabled, copies and sorts the contents of arrays and collections before serializing them.
	 *
	 * <p>
	 * Note that this introduces a performance penalty since it requires copying the existing collection.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that sorts arrays and collections before serialization.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sortCollections()
	 * 		.build();
	 *
	 * 	<jc>// An unsorted array</jc>
	 * 	String[] array = {<js>"foo"</js>,<js>"bar"</js>,<js>"baz"</js>}
	 *
	 * 	<jc>// Produces ["bar","baz","foo"]</jc>
	 * 	String json = s.serialize(array);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder sortCollections() {
		return set(SERIALIZER_sortCollections, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Sort maps alphabetically.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #sortMaps()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder sortMaps(boolean value) {
		return set(SERIALIZER_sortMaps, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Sort maps alphabetically.
	 *
	 * <p>
	 * When enabled, copies and sorts the contents of maps by their keys before serializing them.
	 *
	 * <p>
	 * Note that this introduces a performance penalty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that sorts maps before serialization.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sortMaps()
	 * 		.build();
	 *
	 * 	<jc>// An unsorted map.</jc>
	 * 	OMap map = OMap.<jsm>of</jsm>(<js>"foo"</js>,1,<js>"bar"</js>,2,<js>"baz"</js>,3);
	 *
	 * 	<jc>// Produces {"bar":2,"baz":3,"foo":1}</jc>
	 * 	String json = s.serialize(map);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder sortMaps() {
		return set(SERIALIZER_sortMaps, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim empty lists and arrays.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #trimEmptyCollections()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder trimEmptyCollections(boolean value) {
		return set(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim empty lists and arrays.
	 *
	 * <p>
	 * When enabled, empty lists and arrays will not be serialized.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Map entries with empty list values will be lost.
	 * 	<li>
	 * 		Bean properties with empty list values will not be set.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that skips empty arrays and collections.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.trimEmptyCollections()
	 * 		.build();
	 *
	 * 	<jc>// A bean with a field with an empty array.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String[] <jf>foo</jf> = {};
	 * 	}
	 *
	 * 	<jc>// Produces {}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder trimEmptyCollections() {
		return set(SERIALIZER_trimEmptyCollections, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim empty maps.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #trimEmptyMaps()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder trimEmptyMaps(boolean value) {
		return set(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim empty maps.
	 *
	 * <p>
	 * When enabled, empty map values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Bean properties with empty map values will not be set.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that skips empty maps.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.trimEmptyMaps()
	 * 		.build();
	 *
	 * 	<jc>// A bean with a field with an empty map.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> OMap <jf>foo</jf> = OMap.<jsm>of</jsm>();
	 * 	}
	 *
	 * 	<jc>// Produces {}</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder trimEmptyMaps() {
		return set(SERIALIZER_trimEmptyMaps, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim null bean property values.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #keepNullProperties()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder trimNullProperties(boolean value) {
		return set(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim null bean property values.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #keepNullProperties()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder dontTrimNullProperties() {
		return set(SERIALIZER_trimNullProperties, false);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim strings.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #trimStrings()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@ConfigurationProperty
	@Deprecated
	public SerializerBuilder trimStrings(boolean value) {
		return set(SERIALIZER_trimStrings, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim strings.
	 *
	 * <p>
	 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that trims strings before serialization.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.trimStrings()
	 * 		.build();
	 *
	 *	<jc>// A map with space-padded keys/values</jc>
	 * 	OMap map = OMap.<jsm>of</jsm>(<js>" foo "</js>, <js>" bar "</js>);
	 *
	 * 	<jc>// Produces "{foo:'bar'}"</jc>
	 * 	String json = s.toString(map);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder trimStrings() {
		return set(SERIALIZER_trimStrings, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  URI context bean.
	 *
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our URI contextual information.</jc>
	 * 	String authority = <js>"http://localhost:10000"</js>;
	 * 	String contextRoot = <js>"/myContext"</js>;
	 * 	String servletPath = <js>"/myServlet"</js>;
	 * 	String pathInfo = <js>"/foo"</js>;
	 *
	 * 	<jc>// Create a UriContext object.</jc>
	 * 	UriContext uriContext = <jk>new</jk> UriContext(authority, contextRoot, servletPath, pathInfo);
	 *
	 * 	<jc>// Associate it with our serializer.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.uriContext(uriContext)
	 * 		.uriRelativity(<jsf>RESOURCE</jsf>)  <jc>// Assume relative paths are relative to servlet.</jc>
	 * 		.uriResolution(<jsf>ABSOLUTE</jsf>)  <jc>// Serialize URLs as absolute paths.</jc>
	 * 		.build();
	 *
	 * 	<jc>// A relative URL</jc>
	 * 	URL url = <jk>new</jk> URL(<js>"bar"</js>);
	 *
	 * 	<jc>// Produces "http://localhost:10000/myContext/myServlet/foo/bar"</jc>
	 * 	String json = s.toString(url);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
	 * 	<li class='link'>{@doc juneau-marshall.URIs}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder uriContext(UriContext value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  URI relativity.
	 *
	 * <p>
	 * Defines what relative URIs are relative to when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link org.apache.juneau.UriRelativity#RESOURCE}
	 * 		- Relative URIs should be considered relative to the servlet URI.
	 * 	<li class='jf'>{@link org.apache.juneau.UriRelativity#PATH_INFO}
	 * 		- Relative URIs should be considered relative to the request URI.
	 * </ul>
	 *
	 * <p>
	 * See {@link #uriContext(UriContext)} for examples.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
	 * 	<li class='link'>{@doc juneau-marshall.URIs}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriRelativity#RESOURCE}
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder uriRelativity(UriRelativity value) {
		return set(SERIALIZER_uriRelativity, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  URI resolution.
	 *
	 * <p>
	 * Defines the resolution level for URIs when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li class='jf'>{@link UriResolution#ABSOLUTE}
	 * 		- Resolve to an absolute URL (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
	 * 	<li class='jf'>{@link UriResolution#ROOT_RELATIVE}
	 * 		- Resolve to a root-relative URL (e.g. <js>"/context-root/servlet-path/path-info"</js>).
	 * 	<li class='jf'>{@link UriResolution#NONE}
	 * 		- Don't do any URL resolution.
	 * </ul>
	 *
	 * <p>
	 * See {@link #uriContext(UriContext)} for examples.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
	 * 	<li class='link'>{@doc juneau-marshall.URIs}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriResolution#NONE}
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public SerializerBuilder uriResolution(UriResolution value) {
		return set(SERIALIZER_uriResolution, value);
	}

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public SerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpi(Map<String,Object> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpro(Map<String,Object> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpwo(Map<String,Object> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpx(Map<String,Object> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> SerializerBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> SerializerBuilder exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder fluentSetters(Class<?> on) {
		super.fluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder propertyFilter(Class<?> on, Class<? extends org.apache.juneau.transform.PropertyFilter> value) {
		super.propertyFilter(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public SerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public SerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public SerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public SerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public SerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>

	@Override /* Context */
	public Serializer build() {
		return null;
	}
}