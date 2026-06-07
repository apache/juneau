/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.serializer;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.soap.*;

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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase convention
	"rawtypes"
})
public class Serializer extends MarshallingTraverseContext {

	// Property name constants
	private static final String PROP_addBeanTypes = "addBeanTypes";
	private static final String PROP_addRootType = "addRootType";
	private static final String PROP_keepNullProperties = "keepNullProperties";
	private static final String PROP_listener = "listener";
	private static final String PROP_sortCollections = "sortCollections";
	private static final String PROP_sortMaps = "sortMaps";
	private static final String PROP_trimEmptyCollections = "trimEmptyCollections";
	private static final String PROP_trimEmptyMaps = "trimEmptyMaps";
	private static final String PROP_trimStrings = "trimStrings";
	private static final String PROP_uriContext = "uriContext";
	private static final String PROP_uriRelativity = "uriRelativity";
	private static final String PROP_uriResolution = "uriResolution";

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends MarshallingTraverseContext.Builder<SELF> {

		private boolean addBeanTypes;
		private boolean addRootType;
		private boolean keepNullProperties;
		private boolean sortCollections;
		private boolean sortMaps;
		private boolean trimEmptyCollections;
		private boolean trimEmptyMaps;
		private boolean trimStrings;
		private Class<? extends SerializerListener> listener;
		private String accept;
		private String produces;
		private UriContext uriContext;
		private UriRelativity uriRelativity;
		private UriResolution uriResolution;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
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
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
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
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Serializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
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
			uriContext = copyFrom.getUriContext();
			uriRelativity = copyFrom.getUriRelativity();
			uriResolution = copyFrom.getUriResolution();
			listener = copyFrom.listener;
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
		 * 	<br>Can be <jk>null</jk> (will default to the value specified by {@link #produces(String)}).
		 * @return This object.
		 */
		public SELF accept(String value) {
			accept = value;
			return self();
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
		 * 	<ja>@Marshalled</ja>(typeName=<js>"mybean"</js>)
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
		public SELF addBeanTypes() {
			return addBeanTypes(true);
		}

		/**
		 * Same as {@link #addBeanTypes()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF addBeanTypes(boolean value) {
			addBeanTypes = value;
			return self();
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
		 * For example, when serializing a top-level POJO with a {@link Marshalled#typeName() @Marshalled(typeName)} value, a
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
		 * 	<ja>@Marshalled</ja>(typeName=<js>"mybean"</js>)
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
		public SELF addRootType() {
			return addRootType(true);
		}

		/**
		 * Same as {@link #addRootType()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF addRootType(boolean value) {
			addRootType = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public Serializer build() {
			return build(Serializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

		/**
		 * Returns the current value for the 'accept' property.
		 *
		 * @return The current value for the 'accept' property.
		 */
		public String getAccept() { return accept; }

		/**
		 * Returns the current value for the 'produces' property.
		 *
		 * @return The current value for the 'produces' property.
		 */
		public String getProduces() { return produces; }

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
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
			// @formatter:on
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
		public SELF keepNullProperties() {
			return keepNullProperties(true);
		}

		/**
		 * Same as {@link #keepNullProperties()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF keepNullProperties(boolean value) {
			keepNullProperties = value;
			return self();
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
		 * 	<br>Can be <jk>null</jk> (no listener will be used, listener methods will not be called).
		 * @return This object.
		 */
		public SELF listener(Class<? extends SerializerListener> value) {
			listener = value;
			return self();
		}

		/**
		 * Specifies the media type that this serializer produces.
		 *
		 * @param value The value for this setting.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF produces(String value) {
			produces = value;
			return self();
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
		public SELF sortCollections() {
			return sortCollections(true);
		}

		/**
		 * Same as {@link #sortCollections()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF sortCollections(boolean value) {
			sortCollections = value;
			return self();
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
		public SELF sortMaps() {
			return sortMaps(true);
		}

		/**
		 * Same as {@link #sortMaps()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF sortMaps(boolean value) {
			sortMaps = value;
			return self();
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
		public SELF trimEmptyCollections() {
			return trimEmptyCollections(true);
		}

		/**
		 * Same as {@link #trimEmptyCollections()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF trimEmptyCollections(boolean value) {
			trimEmptyCollections = value;
			return self();
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
		public SELF trimEmptyMaps() {
			return trimEmptyMaps(true);
		}

		/**
		 * Same as {@link #trimEmptyMaps()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF trimEmptyMaps(boolean value) {
			trimEmptyMaps = value;
			return self();
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
		public SELF trimStrings() {
			return trimStrings(true);
		}

		/**
		 * Same as {@link #trimStrings()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF trimStrings(boolean value) {
			trimStrings = value;
			return self();
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
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallingUris">URIs</a>
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * 	<br>Can be <jk>null</jk> (defaults to <c>UriContext.DEFAULT</c>).
		 * @return This object.
		 */
		public SELF uriContext(UriContext value) {
			uriContext = value;
			return self();
		}

		/**
		 * URI relativity.
		 *
		 * <p>
		 * Defines what relative URIs are relative to when serializing any of the following:
		 * <ul>
		 * 	<li>{@link URI}
		 * 	<li>{@link URL}
		 * 	<li>Properties and classes annotated with {@link Uri @Uri}
		 * </ul>
		 *
		 * <p>
		 * See {@link #uriContext(UriContext)} for examples.
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jf'>{@link UriRelativity#RESOURCE}
		 * 		- Relative URIs should be considered relative to the servlet URI.
		 * 	<li class='jf'>{@link UriRelativity#PATH_INFO}
		 * 		- Relative URIs should be considered relative to the request URI.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallingUris">URIs</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link UriRelativity#RESOURCE}
		 * 	<br>Can be <jk>null</jk> (defaults to <c>UriRelativity.RESOURCE</c>).
		 * @return This object.
		 */
		public SELF uriRelativity(UriRelativity value) {
			uriRelativity = value;
			return self();
		}

		/**
		 * URI resolution.
		 *
		 * <p>
		 * Defines the resolution level for URIs when serializing any of the following:
		 * <ul>
		 * 	<li>{@link URI}
		 * 	<li>{@link URL}
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
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallingUris">URIs</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link UriResolution#NONE}
		 * 	<br>Can be <jk>null</jk> (defaults to <c>UriResolution.NONE</c>).
		 * @return This object.
		 */
		public SELF uriResolution(UriResolution value) {
			uriResolution = value;
			return self();
		}


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link Serializer#create()} / {@link Serializer#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(Serializer copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/**
	 * Represents no Serializer.
	 */
	public abstract static class Null extends Serializer {
		@SuppressWarnings({
			"java:S1186" // Constructor required by Serializer parent class, even though Null is abstract and never instantiated directly
		})
		private Null(Builder<?> builder) {
			super(builder);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Self-typed builder: Builder<?> is the only non-raw, leaf-free return; chaining + build() unaffected.
	})
	public static Builder<?> create() {
		return new DefaultBuilder();
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

	protected final boolean addBeanTypes;
	protected final boolean addRootType;
	protected final boolean keepNullProperties;
	protected final boolean sortCollections;
	protected final boolean sortMaps;
	protected final boolean trimEmptyCollections;
	protected final boolean trimEmptyMaps;
	protected final boolean trimStrings;
	protected final Class<? extends SerializerListener> listener;
	protected final String accept;
	protected final String produces;
	private final UriContext uriContext;
	private final UriRelativity uriRelativity;
	private final UriResolution uriResolution;
	private final MediaRanges acceptRanges;
	private final List<MediaType> acceptMediaTypes;
	private final MediaType producesMediaType;

	/**
	 * Constructor
	 *
	 * @param builder The builder this object.
	 */
	protected Serializer(Builder<?> builder) {
		super(builder);

		accept = builder.accept;
		addBeanTypes = builder.addBeanTypes;
		addRootType = builder.addRootType;
		keepNullProperties = builder.keepNullProperties;
		listener = builder.listener;
		produces = builder.produces;
		sortCollections = builder.sortCollections;
		sortMaps = builder.sortMaps;
		trimEmptyCollections = builder.trimEmptyCollections;
		trimEmptyMaps = builder.trimEmptyMaps;
		trimStrings = builder.trimStrings;
		uriContext = builder.uriContext;
		uriRelativity = builder.uriRelativity;
		uriResolution = builder.uriResolution;

		this.producesMediaType = MediaType.of(produces);
		this.acceptRanges = nn(accept) ? MediaRanges.of(accept) : MediaRanges.of(produces);
		this.acceptMediaTypes = u(nn(builder.accept) ? l(MediaType.ofAll(splita(builder.accept))) : l(this.producesMediaType));
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public SerializerSession.Builder<?> createSession() {
		return SerializerSession.create(this);
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
		for (var m : acceptMediaTypes)
			action.accept(m);
		return this;
	}

	/**
	 * Returns the media types handled based on the value of the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * Note that the order of these ranges are from high to low q-value.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final MediaRanges getMediaTypeRanges() { return acceptRanges; }

	/**
	 * Returns the first entry in the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * This signifies the 'primary' media type for this serializer.
	 *
	 * @return The media type.  Never <jk>null</jk>.
	 */
	public final MediaType getPrimaryMediaType() { return first(acceptMediaTypes).orElseThrow(() -> new IllegalStateException("No accept media types available")); }

	/**
	 * Optional method that returns the response <c>Content-Type</c> for this serializer if it is different from
	 * the matched media type.
	 *
	 * <p>
	 * This method is specified to override the content type for this serializer.
	 * For example, the {@link Json5Serializer} class returns that it handles media type
	 * <js>"text/json5"</js>, but returns <js>"text/json"</js> as the actual content type.
	 * This allows clients to request specific 'flavors' of content using specialized <c>Accept</c> header values.
	 *
	 * <p>
	 * This method is typically meaningless if the serializer is being used stand-alone (i.e. outside of a REST server
	 * or client).
	 *
	 * @return The response content type.  If <jk>null</jk>, then the matched media type is used.
	 */
	public final MediaType getResponseContentType() { return producesMediaType; }

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
		return mape();
	}

	@Override /* Overridden from Context */
	public SerializerSession getSession() { return createSession().build(); }

	/**
	 * Returns <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 *
	 * @return <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 */
	public boolean isWriterSerializer() { return false; }

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
		throw unsupportedOp();
	}

	/**
	 * Serializer listener.
	 *
	 * @see Serializer.Builder#listener(Class)
	 * @return
	 * 	Class used to listen for errors and warnings that occur during serialization.
	 */
	protected final Class<? extends SerializerListener> getListener() { return listener; }

	/**
	 * URI context bean.
	 *
	 * @see Serializer.Builder#uriContext(UriContext)
	 * @return
	 * 	Bean used for resolution of URIs to absolute or root-relative form.
	 */
	protected final UriContext getUriContext() { return uriContext; }

	/**
	 * URI relativity.
	 *
	 * @see Serializer.Builder#uriRelativity(UriRelativity)
	 * @return
	 * 	Defines what relative URIs are relative to when serializing any of the following:
	 */
	protected final UriRelativity getUriRelativity() { return uriRelativity; }

	/**
	 * URI resolution.
	 *
	 * @see Serializer.Builder#uriResolution(UriResolution)
	 * @return
	 * 	Defines the resolution level for URIs when serializing URIs.
	 */
	protected final UriResolution getUriResolution() { return uriResolution; }

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Serializer.Builder#addBeanTypes()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	protected boolean isAddBeanTypes() { return addBeanTypes; }

	/**
	 * Add type attribute to root nodes.
	 *
	 * @see Serializer.Builder#addRootType()
	 * @return
	 * 	<jk>true</jk> if type property should be added to root node.
	 */
	protected final boolean isAddRootType() { return addRootType; }

	/**
	 * Don't trim null bean property values.
	 *
	 * @see Serializer.Builder#keepNullProperties()
	 * @return
	 * 	<jk>true</jk> if null bean values are serialized to the output.
	 */
	protected final boolean isKeepNullProperties() { return keepNullProperties; }

	/**
	 * Sort arrays and collections alphabetically.
	 *
	 * @see Serializer.Builder#sortCollections()
	 * @return
	 * 	<jk>true</jk> if arrays and collections are copied and sorted before serialization.
	 */
	protected final boolean isSortCollections() { return sortCollections; }

	/**
	 * Sort maps alphabetically.
	 *
	 * @see Serializer.Builder#sortMaps()
	 * @return
	 * 	<jk>true</jk> if maps are copied and sorted before serialization.
	 */
	protected final boolean isSortMaps() { return sortMaps; }

	/**
	 * Trim empty lists and arrays.
	 *
	 * @see Serializer.Builder#trimEmptyCollections()
	 * @return
	 * 	<jk>true</jk> if empty lists and arrays are not serialized to the output.
	 */
	protected final boolean isTrimEmptyCollections() { return trimEmptyCollections; }

	/**
	 * Trim empty maps.
	 *
	 * @see Serializer.Builder#trimEmptyMaps()
	 * @return
	 * 	<jk>true</jk> if empty map values are not serialized to the output.
	 */
	protected final boolean isTrimEmptyMaps() { return trimEmptyMaps; }

	/**
	 * Trim strings.
	 *
	 * @see Serializer.Builder#trimStrings()
	 * @return
	 * 	<jk>true</jk> if string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 */
	protected final boolean isTrimStrings() { return trimStrings; }

	@Override /* Overridden from MarshallingTraverseContext */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypes, addBeanTypes)
			.a(PROP_addRootType, addRootType)
			.a(PROP_keepNullProperties, keepNullProperties)
			.a(PROP_listener, listener)
			.a(PROP_sortCollections, sortCollections)
			.a(PROP_sortMaps, sortMaps)
			.a(PROP_trimEmptyCollections, trimEmptyCollections)
			.a(PROP_trimEmptyMaps, trimEmptyMaps)
			.a(PROP_trimStrings, trimStrings)
			.a(PROP_uriContext, uriContext)
			.a(PROP_uriRelativity, uriRelativity)
			.a(PROP_uriResolution, uriResolution);
	}
}