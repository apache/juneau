package org.apache.juneau.marshaller;
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

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.serializer.*;

/**
 * Top-level class for a pairing of a {@link Serializer} and {@link Parser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Marshaller <jv>json</jv> = <jk>new</jk> Json();
 * 	MyPojo <jv>myPojo</jv> = <jv>json</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>json</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Json.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Json.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Marshallers">Marshallers</a>
 * </ul>
 */
public abstract class Marshaller {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Serializer s;
	private final Parser p;

	/**
	 * Constructor.
	 *
	 * @param s
	 * 	The serializer to use for serializing output.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param p
	 * 	The parser to use for parsing input.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	protected Marshaller(Serializer s, Parser p) {
		this.s = s;
		this.p = p;
	}

	/**
	 * Returns the serializer associated with this marshaller.
	 *
	 * @return The serializer associated with this marshaller.
	 */
	public Serializer getSerializer() {
		return s;
	}

	/**
	 * Returns the parser associated with this marshaller.
	 *
	 * @return The parser associated with this marshaller.
	 */
	public Parser getParser() {
		return p;
	}

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 *
	 * <p>
	 * Equivalent to calling <c>serializer.createSession().serialize(o, output);</c>
	 *
	 * @param object The object to serialize.
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
	 * @throws IOException Thrown by underlying stream.
	 */
	public final void write(Object object, Object output) throws SerializeException, IOException {
		s.serialize(object, output);
	}

	/**
	 * Parses input into the specified object type.
	 *
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	Marshaller <jv>marshaller</jv> = Json.<jsf>DEFAULT</jsf>;
	 *
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List <jv>list1</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List <jv>list2</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List <jv>list3</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map <jv>map1</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map <jv>map2</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * <c>Collection</c> classes are assumed to be followed by zero or one objects indicating the element type.
	 *
	 * <p>
	 * <c>Map</c> classes are assumed to be followed by zero or two meta objects indicating the key and value types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Use the {@link #read(Object, Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param input
	 * 	The input.
	 * 	<br>Character-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li>{@link File} containing system encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#fileCharset(Charset)} property value).
	 * 	</ul>
	 * 	<br>Stream-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence} containing encoded bytes according to the {@link org.apache.juneau.parser.InputStreamParser.Builder#binaryFormat(BinaryFormat)} setting.
	 * 	</ul>
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public final <T> T read(Object input, Type type, Type...args) throws ParseException, IOException {
		return p.parse(input, type, args);
	}

	/**
	 * Same as {@link #read(Object, Type, Type...)} except optimized for a non-parameterized class.
	 *
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	Marshaller <jv>marshaller</jv>  = Json.<jsf>DEFAULT</jsf>;
	 *
	 * 	<jc>// Parse into a string.</jc>
	 * 	String <jv>string</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] <jv>beanArray</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List <jv>list</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map <jv>map</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class type of the object being created.
	 * @param input
	 * 	The input.
	 * 	<br>Character-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li>{@link File} containing system encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#fileCharset(Charset)} property value).
	 * 	</ul>
	 * 	<br>Stream-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence} containing encoded bytes according to the {@link org.apache.juneau.parser.InputStreamParser.Builder#binaryFormat(BinaryFormat)} setting.
	 * 	</ul>
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public final <T> T read(Object input, Class<T> type) throws ParseException, IOException {
		return p.parse(input, type);
	}
}
