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
import org.apache.juneau.http.*;
import org.apache.juneau.parser.*;

/**
 * Parent class for all Juneau serializers.
 *
 * <h5 class='section'>Description:</h5>
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
public abstract class Serializer extends BeanContext {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "Serializer.";

	/**
	 * Configuration property:  Abridged output.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.abridged.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#abridged(boolean)}
	 * 			<li class='jm'>{@link SerializerBuilder#abridged()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
	 * type information that might normally be included to determine the data type will not be serialized.
	 *
	 * <p>
	 * For example, when serializing a top-level POJO with a {@link Bean#typeName() @Bean.typeName()} value, a 
	 * <js>'_type'</js> attribute will only be added when this setting is enabled.
	 * 
	 * <p>
	 * Note the differences between the following settings:
	 * <ul>
	 * 	<li class='jf'>{@link #SERIALIZER_abridged} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #SERIALIZER_addBeanTypeProperties} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer that doesn't add _type to root node.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.abridged()
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_abridged</jsf>, <jk>true</jk>)
	 * 		.build();
	 * 
	 * 	<jc>// The bean we want to serialize.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * 
	 * 	<jc>// Will not contain '_type' attribute even though there's a type name on the bean.</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * 	
	 * 	<jc>// '_type' wasn't needed on the parse side because we know the type being parsed.</jc>
	 * 	MyBean myBean = JsonParser.<jsf>DEFAULT</jsf>.parse(json, MyBean.<jk>class</jk>);
	 * </p>
	 */
	public static final String SERIALIZER_abridged = PREFIX + "abridged.b";

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.addBeanTypeProperties.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#addBeanTypeProperties(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * 
	 * <p>
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * <br>For example, when serializing a {@code Map<String,Object>} field where the bean class cannot be determined from
	 * the type of the values.
	 * 
	 * <p>
	 * Note the differences between the following settings:
	 * <ul>
	 * 	<li class='jf'>{@link #SERIALIZER_abridged} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #SERIALIZER_addBeanTypeProperties} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer that never adds _type to nodes.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addBeanTypeProperties(<jk>false</jk>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_addBeanTypeProperties</jsf>, <jk>false</jk>)
	 * 		.build();
	 * 
	 * 	<jc>// A map of objects we want to serialize.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * 
	 * 	Map&ltString,Object&gt; m = new HashMap&lt;&gt;();
	 * 	m.put(<js>"foo"</js>, <jk>new</jk> MyBean());
	 * 
	 * 	<jc>// Will not contain '_type' attribute even though type name is on bean and we're serializing</jc>
	 * 	<jc>// a map of generic objects.</jc>
	 * 	String json = s.serialize(m);
	 * </p>
	 */
	public static final String SERIALIZER_addBeanTypeProperties = PREFIX + "addBeanTypeProperties.b";

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.detectRecursions.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#detectRecursions(boolean)}
	 * 			<li class='jm'>{@link SerializerBuilder#detectRecursions()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies that recursions should be checked for during serialization.
	 *
	 * <p>
	 * Recursions can occur when serializing models that aren't true trees but rather contain loops.
	 * <br>In general, unchecked recursions cause stack-overflow-errors.
	 * <br>These show up as {@link ParseException ParseExceptions} with the message <js>"Depth too deep.  Stack overflow occurred."</js>.
	 *
	 * <p>
	 * The behavior when recursions are detected depends on the value for {@link #SERIALIZER_ignoreRecursions}.
	 *
	 * <p>
	 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
	 * 	the following when <jsf>SERIALIZER_ignoreRecursions</jsf> is <jk>true</jk>...
	 * 
	 * <p class='bcode'>
	 * 	{A:{B:{C:<jk>null</jk>}}}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Checking for recursion can cause a small performance penalty.
	 * </ul>
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer that never adds _type to nodes.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.detectRecursions()
	 * 		.ignoreRecursions()
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_detectRecursions</jsf>, <jk>true</jk>)
	 * 		.set(<jsf>SERIALIZER_ignoreRecursions</jsf>, <jk>true</jk>)
	 * 		.build();
	 * 
	 * 	<jc>// Create a POJO model with a recursive loop.</jc>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> Object <jf>f</jf>;
	 * 	}
	 * 	A a = <jk>new</jk> A();
	 * 	a.<jf>f</jf> = a;
	 * 
	 * 	<jc>// Produces "{f:null}"</jc>
	 * 	String json = s.serialize(a);
	 * </p>
	 */
	public static final String SERIALIZER_detectRecursions = PREFIX + "detectRecursions.b";

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.ignoreRecursions.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#ignoreRecursions(boolean)}
	 * 			<li class='jm'>{@link SerializerBuilder#ignoreRecursions()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used in conjunction with {@link #SERIALIZER_detectRecursions}.
	 * <br>Setting is ignored if <jsf>SERIALIZER_detectRecursions</jsf> is <jk>false</jk>.
	 *
	 * <p>
	 * If <jk>true</jk>, when we encounter the same object when serializing a tree, we set the value to <jk>null</jk>.
	 * <br>Otherwise, a {@link SerializeException} is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 */
	public static final String SERIALIZER_ignoreRecursions = PREFIX + "ignoreRecursions.b";

	/**
	 * Configuration property:  Initial depth.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.initialDepth.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>0</code>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#initialDepth(int)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The initial indentation level at the root.
	 * <br>Useful when constructing document fragments that need to be indented at a certain level.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer with whitespace enabled and an initial depth of 2.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.ws()
	 * 		.initialDepth(2)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_useWhitespace</jsf>, <jk>true</jk>)
	 * 		.set(<jsf>SERIALIZER_initialDepth</jsf>, 2)
	 * 		.build();
	 * 
	 * 	<jc>// Produces "\t\t{\n\t\t\t'foo':'bar'\n\t\t}\n"</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 */
	public static final String SERIALIZER_initialDepth = PREFIX + "initialDepth.i";

	/**
	 * Configuration property:  Serializer listener.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.listener.c"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? extends SerializerListener&gt;</code>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#listener(Class)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
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
	 * 	WriterSerializer s = JsonSerializer.
	 * 		.<jsm>create</jsm>()
	 * 		.listener(MySerializerListener.<jk>class</jk>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer.
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
	 * 		JsonSerializer.<jsf>DEFAULT_LAX</jsf>.println(l.<jf>events</jf>);
	 * 	}
	 * </p>
	 */
	public static final String SERIALIZER_listener = PREFIX + "listener.c";

	/**
	 * Configuration property:  Max serialization depth.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.maxDepth.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>100</code>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#maxDepth(int)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Abort serialization if specified depth is reached in the POJO tree.
	 * <br>If this depth is exceeded, an exception is thrown.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer that throws an exception if the depth is greater than 20.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.maxDepth(20)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_maxDepth</jsf>, 20)
	 * 		.build();
	 * </p>
	 */
	public static final String SERIALIZER_maxDepth = PREFIX + "maxDepth.i";

	/**
	 * Configuration property:  Maximum indentation.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.maxIndent.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>100</code>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#maxIndent(int)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <p>
	 * This setting does not apply to the MessagePack or RDF serializers.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer that indents a maximum of 20 tabs.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.maxIndent(20)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_maxIndent</jsf>, 20)
	 * 		.build();
	 * </p>
	 */
	public static final String SERIALIZER_maxIndent = PREFIX + "maxIndent.i";

	/**
	 * Configuration property:  Quote character.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.quoteChar.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"\""</js>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#quoteChar(char)}
	 * 			<li class='jm'>{@link SerializerBuilder#sq()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * This is the character used for quoting attributes and values.
	 *
	 * <p>
	 * This setting does not apply to the MessagePack or RDF serializers.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer that uses single quotes.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sq()
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_quoteChar</jsf>, <js>'\''</js>)
	 * 		.build();
	 * </p>
	 */
	public static final String SERIALIZER_quoteChar = PREFIX + "quoteChar.s";

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.sortCollections.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#sortCollections(boolean)}
	 * 			<li class='jm'>{@link SerializerBuilder#sortCollections()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Copies and sorts the contents of arrays and collections before serializing them.
	 * 
	 * <p>
	 * Note that this introduces a performance penalty.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
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
	 * </p>
	 */
	public static final String SERIALIZER_sortCollections = PREFIX + "sortCollections.b";

	/**
	 * Configuration property:  Sort maps alphabetically.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.sortMaps.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#sortMaps(boolean)}
	 * 			<li class='jm'>{@link SerializerBuilder#sortMaps()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Copies and sorts the contents of maps by their keys before serializing them.
	 * 
	 * <p>
	 * Note that this introduces a performance penalty.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
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
	 * </p>
	 */
	public static final String SERIALIZER_sortMaps = PREFIX + "sortMaps.b";

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.trimEmptyCollections.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#trimEmptyCollections(boolean)}
	 * 			<li class='jm'>{@link SerializerBuilder#trimEmptyCollections()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * If <jk>true</jk>, empty lists and arrays will not be serialized.
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
	 * <p class='bcode'>
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
	 * </p>
	 */
	public static final String SERIALIZER_trimEmptyCollections = PREFIX + "trimEmptyCollections.b";

	/**
	 * Configuration property:  Trim empty maps.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.trimEmptyMaps.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#trimEmptyMaps(boolean)}
	 * 			<li class='jm'>{@link SerializerBuilder#trimEmptyMaps()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, empty map values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Bean properties with empty map values will not be set.
	 * </ul>
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
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
	 * </p>
	 */
	public static final String SERIALIZER_trimEmptyMaps = PREFIX + "trimEmptyMaps.b";

	/**
	 * Configuration property:  Trim null bean property values.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.trimNullProperties.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#trimNullProperties(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, null bean values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Map entries with <jk>null</jk> values will be lost.
	 * </ul>
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer that serializes null properties.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.trimNullProperties(<jk>false</jk>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_trimNullProperties</jsf>, <jk>false</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String SERIALIZER_trimNullProperties = PREFIX + "trimNullProperties.b";

	/**
	 * Configuration property:  Trim strings.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.trimStrings.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#trimStrings(boolean)}
	 * 			<li class='jm'>{@link SerializerBuilder#trimStrings()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
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
	 * 	Map&lt;String,String&gt; m = <jk>new</jk> HashMap&lt;&gt;();
	 * 	m.put(<js>" foo "</js>, <js>" bar "</js>);
	 * 
	 * 	<jc>// Produces "{foo:'bar'}"</jc>
	 * 	String json = JsonSerializer.<jsf>DEFAULT_LAX</jsf>.toString(m);
	 * </p>
	 */
	public static final String SERIALIZER_trimStrings = PREFIX + "trimStrings.b";

	/**
	 * Configuration property:  URI context bean.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.uriContext.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> (JSON object representing a {@link UriContext})
	 * 	<li><b>Default:</b>  <js>"{}"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#uriContext(UriContext)}
	 * 			<li class='jm'>{@link SerializerBuilder#uriContext(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
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
	 * 		.build();
	 * 
	 * 	<jc>// Same, but specify as a JSON string.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.uriContext(<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>)
	 * 		.build();
	 * 
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_uriContext</jsf>, uriContext)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but define it on the session args instead.</jc>
	 * 	SerializerSessionArgs sessionArgs = <jk>new</jk> SerializerSessionArgs().uriContext(uriContext);
	 * 	<jk>try</jk> (WriterSerializerSession session = s.createSession(sessionArgs)) {
	 * 		...
	 * 	}
	 * </p>
	 * 
	 * <h5 class='section'>Documentation:</h5>
	 * <ul>
	 * 	<li><a class="doclink" href="../../../../overview-summary.html#juneau-marshall.URIs">Overview &gt; URIs</a>
	 * </ul>
	 */
	public static final String SERIALIZER_uriContext = PREFIX + "uriContext.s";

	/**
	 * Configuration property:  URI relativity.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.uriRelativity.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> ({@link UriRelativity})
	 * 	<li><b>Default:</b>  <js>"RESOURCE"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#uriRelativity(UriRelativity)}
	 * 			<li class='jm'>{@link SerializerBuilder#uriRelativity(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * <ul>
	 * 	<li class='jf'>{@link UriRelativity#RESOURCE}
	 * 		- Relative URIs should be considered relative to the servlet URI.
	 * 	<li class='jf'>{@link UriRelativity#PATH_INFO}
	 * 		- Relative URIs should be considered relative to the request URI.
	 * </ul>
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	<jc>// Define a serializer that converts resource-relative URIs to absolute form.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.uriContext(<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>)
	 * 		.uriResolution(<jsf>ABSOLUTE</jsf>)
	 * 		.uriRelativity(<jsf>RESOURCE</jsf>)
	 * 		.build();
	 * </p>
	 * 
	 * <h5 class='section'>Documentation:</h5>
	 * <ul>
	 * 	<li><a class="doclink" href="../../../../overview-summary.html#juneau-marshall.URIs">Overview &gt; URIs</a>
	 * </ul>
	 */
	public static final String SERIALIZER_uriRelativity = PREFIX + "uriRelativity.s";

	/**
	 * Configuration property:  URI resolution.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.uriResolution.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code> ({@link UriResolution})
	 * 	<li><b>Default:</b>  <js>"NONE"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#uriResolution(UriResolution)}
	 * 			<li class='jm'>{@link SerializerBuilder#uriResolution(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	<jc>// Define a serializer that converts resource-relative URIs to absolute form.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.uriContext(<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>)
	 * 		.uriResolution(<jsf>ABSOLUTE</jsf>)
	 * 		.uriRelativity(<jsf>RESOURCE</jsf>)
	 * 		.build();
	 * </p>
	 * 
	 * <h5 class='section'>Documentation:</h5>
	 * <ul>
	 * 	<li><a class="doclink" href="../../../../overview-summary.html#juneau-marshall.URIs">Overview &gt; URIs</a>
	 * </ul>
	 */
	public static final String SERIALIZER_uriResolution = PREFIX + "uriResolution.s";

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Serializer.useWhitespace.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link SerializerBuilder#useWhitespace(boolean)}
	 * 			<li class='jm'>{@link SerializerBuilder#useWhitespace()}
	 * 			<li class='jm'>{@link SerializerBuilder#ws()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 *
	 * <p>
	 * This setting does not apply to the MessagePack serializer.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer with whitespace enabled.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.ws()
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_useWhitespace</jsf>, <jk>true</jk>)
	 * 		.build();
	 * 
	 * 	<jc>// Produces "\{\n\t'foo': 'bar'\n\}\n"</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 */
	public static final String SERIALIZER_useWhitespace = PREFIX + "useWhitespace.b";

	
	static final Serializer DEFAULT = new Serializer(PropertyStore.create().build(), "") {
		@Override
		public SerializerSession createSession(SerializerSessionArgs args) {
			throw new NoSuchMethodError();
		}
	};
	
	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final int maxDepth, initialDepth, maxIndent;
	final boolean
		detectRecursions,
		ignoreRecursions,
		useWhitespace,
		addBeanTypeProperties,
		trimNulls,
		trimEmptyCollections,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps,
		abridged;
	final char quoteChar;
	final UriContext uriContext;
	final UriResolution uriResolution;
	final UriRelativity uriRelativity;
	final Class<? extends SerializerListener> listener;

	private final MediaType[] accept;
	private final MediaType produces;

	// Hidden constructors to force subclass from OuputStreamSerializer or WriterSerializer.
	Serializer(PropertyStore ps, String produces, String...accept) {
		super(ps);
		
		maxDepth = getProperty(SERIALIZER_maxDepth, Integer.class, 100);
		initialDepth = getProperty(SERIALIZER_initialDepth, Integer.class, 0);
		detectRecursions = getProperty(SERIALIZER_detectRecursions, boolean.class, false);
		ignoreRecursions = getProperty(SERIALIZER_ignoreRecursions, boolean.class, false);
		useWhitespace = getProperty(SERIALIZER_useWhitespace, boolean.class, false);
		maxIndent = getProperty(SERIALIZER_maxIndent, Integer.class, 100);
		addBeanTypeProperties = getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true);
		trimNulls = getProperty(SERIALIZER_trimNullProperties, boolean.class, true);
		trimEmptyCollections = getProperty(SERIALIZER_trimEmptyCollections, boolean.class, false);
		trimEmptyMaps = getProperty(SERIALIZER_trimEmptyMaps, boolean.class, false);
		trimStrings = getProperty(SERIALIZER_trimStrings, boolean.class, false);
		sortCollections = getProperty(SERIALIZER_sortCollections, boolean.class, false);
		sortMaps = getProperty(SERIALIZER_sortMaps, boolean.class, false);
		abridged = getProperty(SERIALIZER_abridged, boolean.class, false);
		quoteChar = getProperty(SERIALIZER_quoteChar, String.class, "\"").charAt(0);
		uriContext = getProperty(SERIALIZER_uriContext, UriContext.class, UriContext.DEFAULT);
		uriResolution = getProperty(SERIALIZER_uriResolution, UriResolution.class, UriResolution.NONE);
		uriRelativity = getProperty(SERIALIZER_uriRelativity, UriRelativity.class, UriRelativity.RESOURCE);
		listener = getClassProperty(SERIALIZER_listener, SerializerListener.class, null);

		this.produces = MediaType.forString(produces);
		if (accept.length == 0) {
			this.accept = new MediaType[]{this.produces};
		} else {
			this.accept = new MediaType[accept.length];
			for (int i = 0; i < accept.length; i++) {
				this.accept[i] = MediaType.forString(accept[i]);
			}
		}
	}

	@Override /* Context */
	public SerializerBuilder builder() {
		return null;
	}

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

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


	//--------------------------------------------------------------------------------
	// Convenience methods
	//--------------------------------------------------------------------------------

	@Override /* Context */
	public final SerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Context */
	public final SerializerSessionArgs createDefaultSessionArgs() {
		return new SerializerSessionArgs(ObjectMap.EMPTY_MAP, null, null, null, getResponseContentType(), null);
	}

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 *
	 * <p>
	 * Equivalent to calling <code>serializer.createSession().serialize(o, output);</code>
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
	 */
	public final void serialize(Object o, Object output) throws SerializeException {
		createSession().serialize(o, output);
	}

	/**
	 * Shortcut method for serializing objects directly to either a <code>String</code> or <code><jk>byte</jk>[]</code>
	 * depending on the serializer type.
	 *
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * 	<br>Character-based serializers will return a <code>String</code>
	 * 	<br>Stream-based serializers will return a <code><jk>byte</jk>[]</code>
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public Object serialize(Object o) throws SerializeException {
		return createSession().serialize(o);
	}

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the media types handled based on the value of the <code>accept</code> parameter passed into the constructor.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final MediaType[] getMediaTypes() {
		return accept;
	}

	/**
	 * Optional method that returns the response <code>Content-Type</code> for this serializer if it is different from
	 * the matched media type.
	 *
	 * <p>
	 * This method is specified to override the content type for this serializer.
	 * For example, the {@link org.apache.juneau.json.JsonSerializer.Simple} class returns that it handles media type
	 * <js>"text/json+simple"</js>, but returns <js>"text/json"</js> as the actual content type.
	 * This allows clients to request specific 'flavors' of content using specialized <code>Accept</code> header values.
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
	
	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("Serializer", new ObjectMap()
				.append("maxDepth", maxDepth)
				.append("initialDepth", initialDepth)
				.append("detectRecursions", detectRecursions)
				.append("ignoreRecursions", ignoreRecursions)
				.append("useWhitespace", useWhitespace)
				.append("maxIndent", maxIndent)
				.append("addBeanTypeProperties", addBeanTypeProperties)
				.append("trimNulls", trimNulls)
				.append("trimEmptyCollections", trimEmptyCollections)
				.append("trimEmptyMaps", trimEmptyMaps)
				.append("trimStrings", trimStrings)
				.append("sortCollections", sortCollections)
				.append("sortMaps", sortMaps)
				.append("parserKnowsRootTypes", abridged)
				.append("quoteChar", quoteChar)
				.append("uriContext", uriContext)
				.append("uriResolution", uriResolution)
				.append("uriRelativity", uriRelativity)
				.append("listener", listener)
			);
	}
}
