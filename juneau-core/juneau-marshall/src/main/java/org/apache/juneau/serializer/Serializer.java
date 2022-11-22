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

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.utils.*;

/**
 * Parent class for all Juneau serializers.
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Base serializer class that serves as the parent class for all serializers.
 *
 * <p>
 * The purpose of this class is:
 * <ul>
 * 	<li>Maintain a read-only configuration state of a serializer.
 * 	<li>Create session objects used for serializing POJOs (i.e. {@link SerializerSession}).
 * 	<li>Provide convenience methods for serializing POJOs without having to construct session objects.
 * </ul>
 *
 * <p>
 * Subclasses should (but are not required to) extend directly from {@link OutputStreamSerializer} or {@link WriterSerializer} depending on
 * whether it's a stream or character based serializer.
 *
 * <p>
 * Subclasses must implement parsing via one of the following methods:
 * <ul class='javatree'>
 * 	<li class='jmp'>{@link #doSerialize(SerializerSession, SerializerPipe, Object)}
 * 	<li class='jmp'>{@link SerializerSession#doSerialize(SerializerPipe, Object)}
 * </ul>
 * <br>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class Serializer extends BeanTraverseContext {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Represents no Serializer.
	 */
	public static abstract class Null extends Serializer {
		private Null(Builder builder) {
			super(builder);
		}
	}

	/**
	 * Instantiates a builder of the specified serializer class.
	 *
	 * <p>
	 * Looks for a public static method called <c>create</c> that returns an object that can be passed into a public
	 * or protected constructor of the class.
	 *
	 * @param c The builder to create.
	 * @return A new builder.
	 */
	public static Builder createSerializerBuilder(Class<? extends Serializer> c) {
		return (Builder)Context.createBuilder(c);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanTraverseContext.Builder {

		boolean addBeanTypes, addRootType, keepNullProperties, sortCollections, sortMaps, trimEmptyCollections,
			trimEmptyMaps, trimStrings;
		String produces, accept;
		UriContext uriContext;
		UriRelativity uriRelativity;
		UriResolution uriResolution;
		Class<? extends SerializerListener> listener;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			produces = null;
			accept = null;
			addBeanTypes = env("Serializer.addBeanTypes", false);
			addRootType = env("Serializer.addRootType", false);
			keepNullProperties = env("Serializer.keepNullProperties", false);
			sortCollections = env("Serializer.sortCollections", false);
			sortMaps = env("Serializer.sortMaps", false);
			trimEmptyCollections = env("Serializer.trimEmptyCollections", false);
			trimEmptyMaps = env("Serializer.trimEmptyMaps", false);
			trimStrings = env("Serializer.trimStrings", false);
			uriContext = UriContext.DEFAULT;
			uriRelativity = UriRelativity.RESOURCE;
			uriResolution = UriResolution.NONE;
			listener = null;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(Serializer copyFrom) {
			super(copyFrom);
			produces = copyFrom.produces;
			accept = copyFrom.accept;
			addBeanTypes = copyFrom.addBeanTypes;
			addRootType = copyFrom.addRootType;
			keepNullProperties = copyFrom.keepNullProperties;
			sortCollections = copyFrom.sortCollections;
			sortMaps = copyFrom.sortMaps;
			trimEmptyCollections = copyFrom.trimEmptyCollections;
			trimEmptyMaps = copyFrom.trimEmptyMaps;
			trimStrings = copyFrom.trimStrings;
			uriContext = copyFrom.uriContext;
			uriRelativity = copyFrom.uriRelativity;
			uriResolution = copyFrom.uriResolution;
			listener = copyFrom.listener;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			produces = copyFrom.produces;
			accept = copyFrom.accept;
			addBeanTypes = copyFrom.addBeanTypes;
			addRootType = copyFrom.addRootType;
			keepNullProperties = copyFrom.keepNullProperties;
			sortCollections = copyFrom.sortCollections;
			sortMaps = copyFrom.sortMaps;
			trimEmptyCollections = copyFrom.trimEmptyCollections;
			trimEmptyMaps = copyFrom.trimEmptyMaps;
			trimStrings = copyFrom.trimStrings;
			uriContext = copyFrom.uriContext;
			uriRelativity = copyFrom.uriRelativity;
			uriResolution = copyFrom.uriResolution;
			listener = copyFrom.listener;
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public Serializer build() {
			return build(Serializer.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				produces,
				accept,
				addBeanTypes,
				addRootType,
				keepNullProperties,
				sortCollections,
				sortMaps,
				trimEmptyCollections,
				trimEmptyMaps,
				trimStrings,
				uriContext,
				uriRelativity,
				uriResolution,
				listener
			);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Specifies the media type that this serializer produces.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder produces(String value) {
			this.produces = value;
			return this;
		}

		/**
		 * Returns the current value for the 'produces' property.
		 *
		 * @return The current value for the 'produces' property.
		 */
		public String getProduces() {
			return produces;
		}

		/**
		 * 	Specifies the accept media types that the serializer can handle.
		 *
		 * 	<p>
		 * 	Can contain meta-characters per the <c>media-type</c> specification of <a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">RFC2616/14.1</a>
		 * 	<p>
		 * 	If empty, then assumes the only media type supported is <c>produces</c>.
		 * 	<p>
		 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
		 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
		 * 	<p class='bjava'>
		 * 		<jv>builder</jv>.produces(<js>"application/json"</js>);
		 * 		<jv>builder</jv>.accept(<js>"application/json,text/json"</js>);
		 * 	</p>
		 * <p>
		 * The accept value can also contain q-values.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder accept(String value) {
			this.accept = value;
			return this;
		}

		/**
		 * Returns the current value for the 'accept' property.
		 *
		 * @return The current value for the 'accept' property.
		 */
		public String getAccept() {
			return accept;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
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
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that adds _type to nodes.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.addBeanTypes()
		 * 		.build();
		 *
		 * 	<jc>// Our map of beans to serialize.</jc>
		 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 * 	JsonMap <jv>myMap</jv> = JsonMap.of(<js>"foo"</js>, <jk>new</jk> MyBean());
		 *
		 * 	<jc>// Will contain:  {"foo":{"_type":"mybean","foo":"bar"}}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>myMap</jv>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder addBeanTypes() {
			return addBeanTypes(true);
		}

		/**
		 * Same as {@link #addBeanTypes()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addBeanTypes(boolean value) {
			addBeanTypes = value;
			return this;
		}

		/**
		 * Add type attribute to root nodes.
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
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that adds _type to root node.</jc>
		 * 	WriterSerializer <jv>serializer</jv>= JsonSerializer
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
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder addRootType() {
			return addRootType(true);
		}

		/**
		 * Same as {@link #addRootType()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addRootType(boolean value) {
			addRootType = value;
			return this;
		}

		/**
		 * Don't trim null bean property values.
		 *
		 * <p>
		 * When enabled, null bean values will be serialized to the output.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>Not enabling this setting will cause <c>Map</c>s with <jk>null</jk> values to be lost during parsing.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that serializes null properties.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
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
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder keepNullProperties() {
			return keepNullProperties(true);
		}

		/**
		 * Same as {@link #keepNullProperties()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder keepNullProperties(boolean value) {
			keepNullProperties = value;
			return this;
		}

		/**
		 * Serializer listener.
		 *
		 * <p>
		 * Class used to listen for errors and warnings that occur during serialization.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Define our serializer listener.</jc>
		 * 	<jc>// Simply captures all errors.</jc>
		 * 	<jk>public class</jk> MySerializerListener <jk>extends</jk> SerializerListener {
		 *
		 * 		<jc>// A simple property to store our events.</jc>
		 * 		<jk>public</jk> List&lt;String&gt; <jf>events</jf> = <jk>new</jk> LinkedList&lt;&gt;();
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> &lt;T&gt; <jk>void</jk> onError(SerializerSession <jv>session</jv>, Throwable <jv>throwable</jv>, String <jv>msg</jv>) {
		 * 			<jf>events</jf>.add(<jv>session</jv>.getLastLocation() + <js>","</js> + <jv>msg</jv> + <js>","</js> + <jv>throwable</jv>);
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a serializer using our listener.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.listener(MySerializerListener.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Create a session object.</jc>
		 * 	<jc>// Needed because listeners are created per-session.</jc>
		 * 	<jk>try</jk> (WriterSerializerSession <jv>session</jv> = <jv>serializer</jv>.createSession()) {
		 *
		 * 		<jc>// Serialize a bean.</jc>
		 * 		String <jv>json</jv> = <jv>session</jv>.serialize(<jk>new</jk> MyBean());
		 *
		 * 		<jc>// Get the listener.</jc>
		 * 		MySerializerListener <jv>listener</jv> = <jv>session</jv>.getListener(MySerializerListener.<jk>class</jk>);
		 *
		 * 		<jc>// Dump the results to the console.</jc>
		 * 		Json5.<jsf>DEFAULT</jsf>.println(<jv>listener</jv>.<jf>events</jf>);
		 * 	}
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder listener(Class<? extends SerializerListener> value) {
			listener = value;
			return this;
		}

		/**
		 * Sort arrays and collections alphabetically.
		 *
		 * <p>
		 * When enabled, copies and sorts the contents of arrays and collections before serializing them.
		 *
		 * <p>
		 * Note that this introduces a performance penalty since it requires copying the existing collection.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that sorts arrays and collections before serialization.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.sortCollections()
		 * 		.build();
		 *
		 * 	<jc>// An unsorted array</jc>
		 * 	String[] <jv>myArray</jv> = {<js>"foo"</js>,<js>"bar"</js>,<js>"baz"</js>};
		 *
		 * 	<jc>// Produces ["bar","baz","foo"]</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>myArray</jv>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder sortCollections() {
			return sortCollections(true);
		}

		/**
		 * Same as {@link #sortCollections()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder sortCollections(boolean value) {
			sortCollections = value;
			return this;
		}

		/**
		 * Sort maps alphabetically.
		 *
		 * <p>
		 * When enabled, copies and sorts the contents of maps by their keys before serializing them.
		 *
		 * <p>
		 * Note that this introduces a performance penalty.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that sorts maps before serialization.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.sortMaps()
		 * 		.build();
		 *
		 * 	<jc>// An unsorted map.</jc>
		 * 	JsonMap <jv>myMap</jv> = JsonMap.<jsm>of</jsm>(<js>"foo"</js>,1,<js>"bar"</js>,2,<js>"baz"</js>,3);
		 *
		 * 	<jc>// Produces {"bar":2,"baz":3,"foo":1}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>myMap</jv>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder sortMaps() {
			return sortMaps(true);
		}

		/**
		 * Same as {@link #sortMaps()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder sortMaps(boolean value) {
			sortMaps = value;
			return this;
		}

		/**
		 * Trim empty lists and arrays.
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
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that skips empty arrays and collections.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
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
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimEmptyCollections() {
			return trimEmptyCollections(true);
		}

		/**
		 * Same as {@link #trimEmptyCollections()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimEmptyCollections(boolean value) {
			trimEmptyCollections = value;
			return this;
		}

		/**
		 * Trim empty maps.
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
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that skips empty maps.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.trimEmptyMaps()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a field with an empty map.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> JsonMap <jf>foo</jf> = JsonMap.<jsm>of</jsm>();
		 * 	}
		 *
		 * 	<jc>// Produces {}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimEmptyMaps() {
			return trimEmptyMaps(true);
		}

		/**
		 * Same as {@link #trimEmptyMaps()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimEmptyMaps(boolean value) {
			trimEmptyMaps = value;
			return this;
		}

		/**
		 * Trim strings.
		 *
		 * <p>
		 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that trims strings before serialization.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.trimStrings()
		 * 		.build();
		 *
		 *	<jc>// A map with space-padded keys/values</jc>
		 * 	JsonMap <jv>myMap</jv> = JsonMap.<jsm>of</jsm>(<js>" foo "</js>, <js>" bar "</js>);
		 *
		 * 	<jc>// Produces "{foo:'bar'}"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.toString(<jv>myMap</jv>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimStrings() {
			return trimStrings(true);
		}

		/**
		 * Same as {@link #trimStrings()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimStrings(boolean value) {
			trimStrings = value;
			return this;
		}

		/**
		 * URI context bean.
		 *
		 * <p>
		 * Bean used for resolution of URIs to absolute or root-relative form.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Our URI contextual information.</jc>
		 * 	String <jv>authority</jv> = <js>"http://localhost:10000"</js>;
		 * 	String <jv>contextRoot</jv> = <js>"/myContext"</js>;
		 * 	String <jv>servletPath</jv> = <js>"/myServlet"</js>;
		 * 	String <jv>pathInfo</jv> = <js>"/foo"</js>;
		 *
		 * 	<jc>// Create a UriContext object.</jc>
		 * 	UriContext <jv>uriContext</jv> = <jk>new</jk> UriContext(<jv>authority</jv>, <jv>contextRoot</jv>, <jv>servletPath</jv>, <jv>pathInfo</jv>);
		 *
		 * 	<jc>// Associate it with our serializer.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.uriContext(<jv>uriContext</jv>)
		 * 		.uriRelativity(<jsf>RESOURCE</jsf>)  <jc>// Assume relative paths are relative to servlet.</jc>
		 * 		.uriResolution(<jsf>ABSOLUTE</jsf>)  <jc>// Serialize URLs as absolute paths.</jc>
		 * 		.build();
		 *
		 * 	<jc>// A relative URL</jc>
		 * 	URL <jv>myUrl</jv> = <jk>new</jk> URL(<js>"bar"</js>);
		 *
		 * 	<jc>// Produces "http://localhost:10000/myContext/myServlet/foo/bar"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.toString(<jv>myUrl</jv>);
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.MarshallingUris">URIs</a>
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriContext(UriContext value) {
			uriContext = value;
			return this;
		}

		/**
		 * URI relativity.
		 *
		 * <p>
		 * Defines what relative URIs are relative to when serializing any of the following:
		 * <ul>
		 * 	<li>{@link java.net.URI}
		 * 	<li>{@link java.net.URL}
		 * 	<li>Properties and classes annotated with {@link Uri @Uri}
		 * </ul>
		 *
		 * <p>
		 * See {@link #uriContext(UriContext)} for examples.
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jf'>{@link org.apache.juneau.UriRelativity#RESOURCE}
		 * 		- Relative URIs should be considered relative to the servlet URI.
		 * 	<li class='jf'>{@link org.apache.juneau.UriRelativity#PATH_INFO}
		 * 		- Relative URIs should be considered relative to the request URI.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.MarshallingUris">URIs</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link UriRelativity#RESOURCE}
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriRelativity(UriRelativity value) {
			uriRelativity = value;
			return this;
		}

		/**
		 * URI resolution.
		 *
		 * <p>
		 * Defines the resolution level for URIs when serializing any of the following:
		 * <ul>
		 * 	<li>{@link java.net.URI}
		 * 	<li>{@link java.net.URL}
		 * 	<li>Properties and classes annotated with {@link Uri @Uri}
		 * </ul>
		 *
		 * <p>
		 * See {@link #uriContext(UriContext)} for examples.
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jf'>{@link UriResolution#ABSOLUTE}
		 * 		- Resolve to an absolute URL (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
		 * 	<li class='jf'>{@link UriResolution#ROOT_RELATIVE}
		 * 		- Resolve to a root-relative URL (e.g. <js>"/context-root/servlet-path/path-info"</js>).
		 * 	<li class='jf'>{@link UriResolution#NONE}
		 * 		- Don't do any URL resolution.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.MarshallingUris">URIs</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link UriResolution#NONE}
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriResolution(UriResolution value) {
			uriResolution = value;
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

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder swaps(java.lang.Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions(boolean value) {
			super.detectRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions(boolean value) {
			super.ignoreRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final String produces, accept;
	final boolean
		addBeanTypes,
		keepNullProperties,
		trimEmptyCollections,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps,
		addRootType;
	final UriContext uriContext;
	final UriResolution uriResolution;
	final UriRelativity uriRelativity;
	final Class<? extends SerializerListener> listener;

	private final MediaRanges acceptRanges;
	private final MediaType[] acceptMediaTypes;
	private final MediaType producesMediaType;

	/**
	 * Constructor
	 *
	 * @param builder The builder this object.
	 */
	protected Serializer(Builder builder) {
		super(builder);

		produces = builder.produces;
		accept = builder.accept;
		addBeanTypes = builder.addBeanTypes;
		keepNullProperties = builder.keepNullProperties;
		trimEmptyCollections = builder.trimEmptyCollections;
		trimEmptyMaps = builder.trimEmptyMaps;
		trimStrings = builder.trimStrings;
		sortCollections = builder.sortCollections;
		sortMaps = builder.sortMaps;
		addRootType = builder.addRootType;
		uriContext = builder.uriContext;
		uriResolution = builder.uriResolution;
		uriRelativity = builder.uriRelativity;
		listener = builder.listener;

		this.producesMediaType = MediaType.of(produces);
		this.acceptRanges = accept != null ? MediaRanges.of(accept) : MediaRanges.of(produces);
		this.acceptMediaTypes = builder.accept != null ? MediaType.ofAll(split(builder.accept)) : new MediaType[] {this.producesMediaType};
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public SerializerSession.Builder createSession() {
		return SerializerSession.create(this);
	}

	@Override /* Context */
	public SerializerSession getSession() {
		return createSession().build();
	}

	/**
	 * Returns <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 *
	 * @return <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 */
	public boolean isWriterSerializer() {
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 *
	 * <p>
	 * Equivalent to calling <c>serializer.createSession().serialize(o, output);</c>
	 *
	 * @param o The object to serialize.
	 * @param output
	 * 	The output object.
	 * 	<br>Character-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link Writer}
	 * 		<li>{@link OutputStream} - Output will be written as UTF-8 encoded stream.
	 * 		<li>{@link File} - Output will be written as system-default encoded stream.
	 * 		<li>{@link StringBuilder} - Output will be written to the specified string builder.
	 * 	</ul>
	 * 	<br>Stream-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link OutputStream}
	 * 		<li>{@link File}
	 * 	</ul>
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public final void serialize(Object o, Object output) throws SerializeException, IOException {
		getSession().serialize(o, output);
	}

	/**
	 * Shortcut method for serializing objects directly to either a <c>String</c> or <code><jk>byte</jk>[]</code>
	 * depending on the serializer type.
	 *
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * 	<br>Character-based serializers will return a <c>String</c>
	 * 	<br>Stream-based serializers will return a <code><jk>byte</jk>[]</code>
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public Object serialize(Object o) throws SerializeException {
		return getSession().serialize(o);
	}

	/**
	 * Convenience method for serializing an object to a String.
	 *
	 * <p>
	 * For writer-based serializers, this is identical to calling {@link #serialize(Object)}.
	 * <br>For stream-based serializers, this converts the returned byte array to a string based on
	 * the {@link OutputStreamSerializer.Builder#binaryFormat(BinaryFormat)} setting.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final String serializeToString(Object o) throws SerializeException {
		return getSession().serializeToString(o);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Serializes a POJO to the specified pipe.
	 *
	 * @param session The current session.
	 * @param pipe Where to send the output from the serializer.
	 * @param o The object to serialize.
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException Problem occurred trying to serialize object.
	 */
	protected void doSerialize(SerializerSession session, SerializerPipe pipe, Object o) throws IOException, SerializeException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Optional method that specifies HTTP request headers for this serializer.
	 *
	 * <p>
	 * For example, {@link SoapXmlSerializer} needs to set a <c>SOAPAction</c> header.
	 *
	 * <p>
	 * This method is typically meaningless if the serializer is being used stand-alone (i.e. outside of a REST server
	 * or client).
	 *
	 * @param session The current session.
	 * @return
	 * 	The HTTP headers to set on HTTP requests.
	 * 	Never <jk>null</jk>.
	 */
	public Map<String,String> getResponseHeaders(SerializerSession session) {
		return Collections.emptyMap();
	}

	/**
	 * Returns the media types handled based on the value of the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * Note that the order of these ranges are from high to low q-value.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final MediaRanges getMediaTypeRanges() {
		return acceptRanges;
	}

	/**
	 * Returns the first entry in the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * This signifies the 'primary' media type for this serializer.
	 *
	 * @return The media type.  Never <jk>null</jk>.
	 */
	public final MediaType getPrimaryMediaType() {
		return acceptMediaTypes[0];
	}

	/**
	 * Performs an action on the media types handled based on the value of the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * The order of the media types are the same as those in the <c>accept</c> parameter.
	 *
	 * @param action The action to perform on the media types.
	 * @return This object.
	 */
	public final Serializer forEachAcceptMediaType(Consumer<MediaType> action) {
		for (MediaType m : acceptMediaTypes)
			action.accept(m);
		return this;
	}

	/**
	 * Optional method that returns the response <c>Content-Type</c> for this serializer if it is different from
	 * the matched media type.
	 *
	 * <p>
	 * This method is specified to override the content type for this serializer.
	 * For example, the {@link org.apache.juneau.json.Json5Serializer} class returns that it handles media type
	 * <js>"text/json5"</js>, but returns <js>"text/json"</js> as the actual content type.
	 * This allows clients to request specific 'flavors' of content using specialized <c>Accept</c> header values.
	 *
	 * <p>
	 * This method is typically meaningless if the serializer is being used stand-alone (i.e. outside of a REST server
	 * or client).
	 *
	 * @return The response content type.  If <jk>null</jk>, then the matched media type is used.
	 */
	public final MediaType getResponseContentType() {
		return producesMediaType;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Serializer.Builder#addBeanTypes()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	protected boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Add type attribute to root nodes.
	 *
	 * @see Serializer.Builder#addRootType()
	 * @return
	 * 	<jk>true</jk> if type property should be added to root node.
	 */
	protected final boolean isAddRootType() {
		return addRootType;
	}

	/**
	 * Serializer listener.
	 *
	 * @see Serializer.Builder#listener(Class)
	 * @return
	 * 	Class used to listen for errors and warnings that occur during serialization.
	 */
	protected final Class<? extends SerializerListener> getListener() {
		return listener;
	}

	/**
	 * Sort arrays and collections alphabetically.
	 *
	 * @see Serializer.Builder#sortCollections()
	 * @return
	 * 	<jk>true</jk> if arrays and collections are copied and sorted before serialization.
	 */
	protected final boolean isSortCollections() {
		return sortCollections;
	}

	/**
	 * Sort maps alphabetically.
	 *
	 * @see Serializer.Builder#sortMaps()
	 * @return
	 * 	<jk>true</jk> if maps are copied and sorted before serialization.
	 */
	protected final boolean isSortMaps() {
		return sortMaps;
	}

	/**
	 * Trim empty lists and arrays.
	 *
	 * @see Serializer.Builder#trimEmptyCollections()
	 * @return
	 * 	<jk>true</jk> if empty lists and arrays are not serialized to the output.
	 */
	protected final boolean isTrimEmptyCollections() {
		return trimEmptyCollections;
	}

	/**
	 * Trim empty maps.
	 *
	 * @see Serializer.Builder#trimEmptyMaps()
	 * @return
	 * 	<jk>true</jk> if empty map values are not serialized to the output.
	 */
	protected final boolean isTrimEmptyMaps() {
		return trimEmptyMaps;
	}

	/**
	 * Don't trim null bean property values.
	 *
	 * @see Serializer.Builder#keepNullProperties()
	 * @return
	 * 	<jk>true</jk> if null bean values are serialized to the output.
	 */
	protected final boolean isKeepNullProperties() {
		return keepNullProperties;
	}

	/**
	 * Trim strings.
	 *
	 * @see Serializer.Builder#trimStrings()
	 * @return
	 * 	<jk>true</jk> if string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 */
	protected final boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * URI context bean.
	 *
	 * @see Serializer.Builder#uriContext(UriContext)
	 * @return
	 * 	Bean used for resolution of URIs to absolute or root-relative form.
	 */
	protected final UriContext getUriContext() {
		return uriContext;
	}

	/**
	 * URI relativity.
	 *
	 * @see Serializer.Builder#uriRelativity(UriRelativity)
	 * @return
	 * 	Defines what relative URIs are relative to when serializing any of the following:
	 */
	protected final UriRelativity getUriRelativity() {
		return uriRelativity;
	}

	/**
	 * URI resolution.
	 *
	 * @see Serializer.Builder#uriResolution(UriResolution)
	 * @return
	 * 	Defines the resolution level for URIs when serializing URIs.
	 */
	protected final UriResolution getUriResolution() {
		return uriResolution;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap()
			.append("addBeanTypes", addBeanTypes)
			.append("keepNullProperties", keepNullProperties)
			.append("trimEmptyCollections", trimEmptyCollections)
			.append("trimEmptyMaps", trimEmptyMaps)
			.append("trimStrings", trimStrings)
			.append("sortCollections", sortCollections)
			.append("sortMaps", sortMaps)
			.append("addRootType", addRootType)
			.append("uriContext", uriContext)
			.append("uriResolution", uriResolution)
			.append("uriRelativity", uriRelativity)
			.append("listener", listener);
	}
}
