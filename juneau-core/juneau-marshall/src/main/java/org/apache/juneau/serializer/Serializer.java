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

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;

/**
 * Parent class for all Juneau serializers.
 *
 * <h5 class='topic'>Description</h5>
 *
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
 * Subclasses should extend directly from {@link OutputStreamSerializer} or {@link WriterSerializer} depending on
 * whether it's a stream or character based serializer.
 */
@ConfigurableContext
public abstract class Serializer extends BeanTraverseContext {

	/**
	 * Represents no Serializer.
	 */
	public static abstract class Null extends Serializer {
		private Null(PropertyStore ps, String produces, String accept) {
			super(ps, produces, accept);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "Serializer";

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_addBeanTypes SERIALIZER_addBeanTypes}
	 * 	<li><b>Name:</b>  <js>"Serializer.addBeanTypes.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Serializer.addBeanTypes</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_ADDBEANTYPES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#addBeanTypes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#addBeanTypes()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * When enabled, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
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
	 * 	<li class='jf'>{@link #SERIALIZER_addRootType} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #SERIALIZER_addBeanTypes} - Affects whether <js>'_type'</js> is added to any nodes.
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_addBeanTypes</jsf>, <jk>true</jk>)
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
	 */
	public static final String SERIALIZER_addBeanTypes = PREFIX + ".addBeanTypes.b";

	/**
	 * Configuration property:  Add type attribute to root nodes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_addRootType SERIALIZER_addRootType}
	 * 	<li><b>Name:</b>  <js>"Serializer.addRootType.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Serializer.addRootType</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_ADDROOTTYPE</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#addRootType()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#addRootType()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * 	<li class='jf'>{@link #SERIALIZER_addRootType} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #SERIALIZER_addBeanTypes} - Affects whether <js>'_type'</js> is added to any nodes.
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_addRootType</jsf>, <jk>true</jk>)
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
	 */
	public static final String SERIALIZER_addRootType = PREFIX + ".addRootType.b";

	/**
	 * Configuration property:  Serializer listener.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_listener SERIALIZER_listener}
	 * 	<li><b>Name:</b>  <js>"Serializer.listener.c"</js>
	 * 	<li><b>Data type:</b>  <c>Class&lt;{@link org.apache.juneau.serializer.SerializerListener}&gt;</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#listener()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#listener(Class)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_listener</jsf>, MySerializerListener.<jk>class</jk>)
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
	 */
	public static final String SERIALIZER_listener = PREFIX + ".listener.c";

	/**
	 * Configuration property:  Don't trim null bean property values.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_keepNullProperties SERIALIZER_keepNullProperties}
	 * 	<li><b>Name:</b>  <js>"Serializer.keepNullProperties.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Serializer.keepNullProperties</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_KEEPNULLPROPERTIES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#keepNullProperties()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#keepNullProperties()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * When enabled, null bean values will be serialized to the output.
	 *
	 * <ul class='notes'>
	 * 	<li>Not enabling this setting will cause Maps with <jk>null</jk> values to be lost during parsing.
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_keepNullProperties</jsf>, <jk>true</jk>)
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
	 */
	public static final String SERIALIZER_keepNullProperties = PREFIX + ".keepNullProperties.b";

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_sortCollections SERIALIZER_sortCollections}
	 * 	<li><b>Name:</b>  <js>"Serializer.sortCollections.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Serializer.sortCollections</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_SORTCOLLECTIONS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#sortCollections()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#sortCollections()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_sortCollections</jsf>, <jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// An unsorted array</jc>
	 * 	String[] array = {<js>"foo"</js>,<js>"bar"</js>,<js>"baz"</js>}
	 *
	 * 	<jc>// Produces ["bar","baz","foo"]</jc>
	 * 	String json = s.serialize(array);
	 * </p>
	 */
	public static final String SERIALIZER_sortCollections = PREFIX + ".sortCollections.b";

	/**
	 * Configuration property:  Sort maps alphabetically.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_sortMaps SERIALIZER_sortMaps}
	 * 	<li><b>Name:</b>  <js>"Serializer.sortMaps.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Serializer.sortMaps</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_SORTMAPS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#sortMaps()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#sortMaps()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_sortMaps</jsf>, <jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// An unsorted map.</jc>
	 * 	OMap map = OMap.<jsm>of</jsm>(<js>"foo"</js>,1,<js>"bar"</js>,2,<js>"baz"</js>,3);
	 *
	 * 	<jc>// Produces {"bar":2,"baz":3,"foo":1}</jc>
	 * 	String json = s.serialize(map);
	 * </p>
	 */
	public static final String SERIALIZER_sortMaps = PREFIX + ".sortMaps.b";

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_trimEmptyCollections SERIALIZER_trimEmptyCollections}
	 * 	<li><b>Name:</b>  <js>"Serializer.trimEmptyCollections.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Serializer.trimEmptyCollections</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_TRIMEMPTYCOLLECTIONS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#trimEmptyCollections()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#trimEmptyCollections()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_trimEmptyCollections</jsf>, <jk>true</jk>)
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
	 */
	public static final String SERIALIZER_trimEmptyCollections = PREFIX + ".trimEmptyCollections.b";

	/**
	 * Configuration property:  Trim empty maps.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_trimEmptyMaps SERIALIZER_trimEmptyMaps}
	 * 	<li><b>Name:</b>  <js>"Serializer.trimEmptyMaps.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Serializer.trimEmptyMaps</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_TRIMEMPTYMAPS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#trimEmptyMaps()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#trimEmptyMaps()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_trimEmptyMaps</jsf>, <jk>true</jk>)
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
	 */
	public static final String SERIALIZER_trimEmptyMaps = PREFIX + ".trimEmptyMaps.b";

	/**
	 * Configuration property:  Trim null bean property values.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_trimNullProperties SERIALIZER_trimNullProperties}
	 * 	<li><b>Name:</b>  <js>"Serializer.trimNullProperties.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Serializer.trimNullProperties</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_TRIMNULLPROPERTIES</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#trimNullProperties()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#trimNullProperties(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#dontTrimNullProperties()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * When enabled, null bean values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Map entries with <jk>null</jk> values will be lost.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that serializes null properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.dontTrimNullProperties()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_trimNullProperties</jsf>, <jk>false</jk>)
	 * 		.build();
	 * </p>
	 * @deprecated Use {@link #SERIALIZER_keepNullProperties}
	 */
	@Deprecated
	public static final String SERIALIZER_trimNullProperties = PREFIX + ".trimNullProperties.b";

	/**
	 * Configuration property:  Trim strings.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_trimStrings SERIALIZER_trimStrings}
	 * 	<li><b>Name:</b>  <js>"Serializer.trimStrings.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Serializer.trimStrings</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_TRIMSTRINGS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#trimStrings()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#trimStrings()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_trimStrings</jsf>, <jk>true</jk>)
	 * 		.build();
	 *
	 *	<jc>// A map with space-padded keys/values</jc>
	 * 	OMap map = OMap.<jsm>of</jsm>(<js>" foo "</js>, <js>" bar "</js>);
	 *
	 * 	<jc>// Produces "{foo:'bar'}"</jc>
	 * 	String json = s.toString(map);
	 * </p>
	 */
	public static final String SERIALIZER_trimStrings = PREFIX + ".trimStrings.b";

	/**
	 * Configuration property:  URI context bean.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_uriContext SERIALIZER_uriContext}
	 * 	<li><b>Name:</b>  <js>"Serializer.uriContext.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.UriContext}
	 * 	<li><b>System property:</b>  <c>Serializer.uriContext</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_URICONTEXT</c>
	 * 	<li><b>Default:</b>  <js>"{}"</js>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#uriContext()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#uriContext(UriContext)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * 	<jc>// Same, but use properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_uriContext</jsf>, uriContext)
	 * 		.set(<jsf>SERIALIZER_uriRelativity</jsf>, <js>"RESOURCE"</js>)
	 * 		.set(<jsf>SERIALIZER_uriResolution</jsf>, <js>"ABSOLUTE"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but define it on the session args instead.</jc>
	 * 	SerializerSessionArgs sessionArgs = <jk>new</jk> SerializerSessionArgs().uriContext(uriContext);
	 * 	<jk>try</jk> (WriterSerializerSession session = s.createSession(sessionArgs)) {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// A relative URL</jc>
	 * 	URL url = <jk>new</jk> URL(<js>"bar"</js>);
	 *
	 * 	<jc>// Produces "http://localhost:10000/myContext/myServlet/foo/bar"</jc>
	 * 	String json = s.toString(url);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc MarshallingUris}
	 * </ul>
	 */
	public static final String SERIALIZER_uriContext = PREFIX + ".uriContext.s";

	/**
	 * Configuration property:  URI relativity.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_uriRelativity SERIALIZER_uriRelativity}
	 * 	<li><b>Name:</b>  <js>"Serializer.uriRelativity.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.UriRelativity}
	 * 	<li><b>System property:</b>  <c>Serializer.uriRelativity</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_URIRELATIVITY</c>
	 * 	<li><b>Default:</b>  <js>"RESOURCE"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#uriRelativity()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#uriRelativity(UriRelativity)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * See {@link org.apache.juneau.serializer.Serializer#SERIALIZER_uriContext} for examples.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc MarshallingUris}
	 * </ul>
	 */
	public static final String SERIALIZER_uriRelativity = PREFIX + ".uriRelativity.s";

	/**
	 * Configuration property:  URI resolution.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.Serializer#SERIALIZER_uriResolution SERIALIZER_uriResolution}
	 * 	<li><b>Name:</b>  <js>"Serializer.uriResolution.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.UriResolution}
	 * 	<li><b>System property:</b>  <c>Serializer.uriResolution</c>
	 * 	<li><b>Environment variable:</b>  <c>SERIALIZER_URIRESOLUTION</c>
	 * 	<li><b>Default:</b>  <js>"NONE"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#uriResolution()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.SerializerBuilder#uriResolution(UriResolution)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * See {@link org.apache.juneau.serializer.Serializer#SERIALIZER_uriContext} for examples.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc MarshallingUris}
	 * </ul>
	 */
	public static final String SERIALIZER_uriResolution = PREFIX + ".uriResolution.s";

	static final Serializer DEFAULT = new Serializer(PropertyStore.create().build(), "", "") {
		@Override
		public SerializerSession createSession(SerializerSessionArgs args) {
			throw new NoSuchMethodError();
		}
	};

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final boolean
		addBeanTypes,
		keepNullProperties,
		trimEmptyCollections,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps,
		addRootType;
	private final UriContext uriContext;
	private final UriResolution uriResolution;
	private final UriRelativity uriRelativity;
	private final Class<? extends SerializerListener> listener;

	private final MediaRanges accept;
	private final MediaType[] accepts;
	private final MediaType produces;

	/**
	 * Constructor
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <c>media-type</c> specification of {@doc ExtRFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <c>produces</c>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	protected Serializer(PropertyStore ps, String produces, String accept) {
		super(ps);

		addBeanTypes = getBooleanProperty(SERIALIZER_addBeanTypes, false);
		keepNullProperties = getBooleanProperty(SERIALIZER_keepNullProperties, ! getBooleanProperty(SERIALIZER_trimNullProperties, true));
		trimEmptyCollections = getBooleanProperty(SERIALIZER_trimEmptyCollections, false);
		trimEmptyMaps = getBooleanProperty(SERIALIZER_trimEmptyMaps, false);
		trimStrings = getBooleanProperty(SERIALIZER_trimStrings, false);
		sortCollections = getBooleanProperty(SERIALIZER_sortCollections, false);
		sortMaps = getBooleanProperty(SERIALIZER_sortMaps, false);
		addRootType = getBooleanProperty(SERIALIZER_addRootType, false);
		uriContext = getProperty(SERIALIZER_uriContext, UriContext.class, UriContext.DEFAULT);
		uriResolution = getProperty(SERIALIZER_uriResolution, UriResolution.class, UriResolution.NONE);
		uriRelativity = getProperty(SERIALIZER_uriRelativity, UriRelativity.class, UriRelativity.RESOURCE);
		listener = getClassProperty(SERIALIZER_listener, SerializerListener.class, null);

		this.produces = MediaType.of(produces);
		this.accept = accept == null ? MediaRanges.of(produces) : MediaRanges.of(accept);
		this.accepts = accept == null ? new MediaType[] {this.produces} : MediaType.ofAll(StringUtils.split(accept, ','));
	}

	@Override /* Context */
	public SerializerBuilder builder() {
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 *
	 * @return <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 */
	public boolean isWriterSerializer() {
		return true;
	}

	/**
	 * Create the session object used for actual serialization of objects.
	 *
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and serializer
	 * 	contexts.
	 * @return
	 * 	The new session object.
	 */
	public abstract SerializerSession createSession(SerializerSessionArgs args);


	//-----------------------------------------------------------------------------------------------------------------
	// Convenience methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public SerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Context */
	public final SerializerSessionArgs createDefaultSessionArgs() {
		return new SerializerSessionArgs().mediaType(getResponseContentType());
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
		createSession().serialize(o, output);
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
		return createSession().serialize(o);
	}

	/**
	 * Convenience method for serializing an object to a String.
	 *
	 * <p>
	 * For writer-based serializers, this is identical to calling {@link #serialize(Object)}.
	 * <br>For stream-based serializers, this converts the returned byte array to a string based on
	 * the {@link OutputStreamSerializer#OSSERIALIZER_binaryFormat} setting.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final String serializeToString(Object o) throws SerializeException {
		return createSession().serializeToString(o);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the media types handled based on the value of the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * Note that the order of these ranges are from high to low q-value.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final MediaRanges getMediaTypeRanges() {
		return accept;
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
		return accepts[0];
	}

	/**
	 * Returns the media types handled based on the value of the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * The order of the media types are the same as those in the <c>accept</c> parameter.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final MediaType[] getAcceptMediaTypes() {
		return accepts;
	}

	/**
	 * Optional method that returns the response <c>Content-Type</c> for this serializer if it is different from
	 * the matched media type.
	 *
	 * <p>
	 * This method is specified to override the content type for this serializer.
	 * For example, the {@link org.apache.juneau.json.SimpleJsonSerializer} class returns that it handles media type
	 * <js>"text/json+simple"</js>, but returns <js>"text/json"</js> as the actual content type.
	 * This allows clients to request specific 'flavors' of content using specialized <c>Accept</c> header values.
	 *
	 * <p>
	 * This method is typically meaningless if the serializer is being used stand-alone (i.e. outside of a REST server
	 * or client).
	 *
	 * @return The response content type.  If <jk>null</jk>, then the matched media type is used.
	 */
	public final MediaType getResponseContentType() {
		return produces;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see #SERIALIZER_addBeanTypes
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
	 * @see #SERIALIZER_addRootType
	 * @return
	 * 	<jk>true</jk> if type property should be added to root node.
	 */
	protected final boolean isAddRootType() {
		return addRootType;
	}

	/**
	 * Serializer listener.
	 *
	 * @see #SERIALIZER_listener
	 * @return
	 * 	Class used to listen for errors and warnings that occur during serialization.
	 */
	protected final Class<? extends SerializerListener> getListener() {
		return listener;
	}

	/**
	 * Sort arrays and collections alphabetically.
	 *
	 * @see #SERIALIZER_sortCollections
	 * @return
	 * 	<jk>true</jk> if arrays and collections are copied and sorted before serialization.
	 */
	protected final boolean isSortCollections() {
		return sortCollections;
	}

	/**
	 * Sort maps alphabetically.
	 *
	 * @see #SERIALIZER_sortMaps
	 * @return
	 * 	<jk>true</jk> if maps are copied and sorted before serialization.
	 */
	protected final boolean isSortMaps() {
		return sortMaps;
	}

	/**
	 * Trim empty lists and arrays.
	 *
	 * @see #SERIALIZER_trimEmptyCollections
	 * @return
	 * 	<jk>true</jk> if empty lists and arrays are not serialized to the output.
	 */
	protected final boolean isTrimEmptyCollections() {
		return trimEmptyCollections;
	}

	/**
	 * Trim empty maps.
	 *
	 * @see #SERIALIZER_trimEmptyMaps
	 * @return
	 * 	<jk>true</jk> if empty map values are not serialized to the output.
	 */
	protected final boolean isTrimEmptyMaps() {
		return trimEmptyMaps;
	}

	/**
	 * Don't trim null bean property values.
	 *
	 * @see #SERIALIZER_keepNullProperties
	 * @return
	 * 	<jk>true</jk> if null bean values are serialized to the output.
	 */
	protected final boolean isKeepNullProperties() {
		return keepNullProperties;
	}

	/**
	 * Trim strings.
	 *
	 * @see #SERIALIZER_trimStrings
	 * @return
	 * 	<jk>true</jk> if string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 */
	protected final boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * URI context bean.
	 *
	 * @see #SERIALIZER_uriContext
	 * @return
	 * 	Bean used for resolution of URIs to absolute or root-relative form.
	 */
	protected final UriContext getUriContext() {
		return uriContext;
	}

	/**
	 * URI relativity.
	 *
	 * @see #SERIALIZER_uriRelativity
	 * @return
	 * 	Defines what relative URIs are relative to when serializing any of the following:
	 */
	protected final UriRelativity getUriRelativity() {
		return uriRelativity;
	}

	/**
	 * URI resolution.
	 *
	 * @see #SERIALIZER_uriResolution
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
	public OMap toMap() {
		return super.toMap()
			.a("Serializer", new DefaultFilteringOMap()
				.a("addBeanTypes", addBeanTypes)
				.a("keepNullProperties", keepNullProperties)
				.a("trimEmptyCollections", trimEmptyCollections)
				.a("trimEmptyMaps", trimEmptyMaps)
				.a("trimStrings", trimStrings)
				.a("sortCollections", sortCollections)
				.a("sortMaps", sortMaps)
				.a("addRootType", addRootType)
				.a("uriContext", uriContext)
				.a("uriResolution", uriResolution)
				.a("uriRelativity", uriRelativity)
				.a("listener", listener)
			);
	}
}
