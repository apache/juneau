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
package org.apache.juneau.marshall.marshaller;

import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Convenience static methods for serializing objects with all supported Juneau marshalling formats.
 *
 * <p>
 * Designed for use with static imports to allow concise serialization code without explicitly
 * referencing marshaller classes.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.marshall.marshaller.MarshallUtils.*;
 *
 * 	<jc>// Serialize — char-based formats return String.</jc>
 * 	String <jv>a</jv> = json(<jv>myBean</jv>);
 * 	String <jv>b</jv> = json5(<jv>myBean</jv>);
 * 	String <jv>c</jv> = xml(<jv>myBean</jv>);
 * 	String <jv>d</jv> = html(<jv>myBean</jv>);
 * 	String <jv>e</jv> = yaml(<jv>myBean</jv>);
 *
 * 	<jc>// Serialize — write to a stream.</jc>
 * 	json(<jv>myBean</jv>, myWriter);
 *
 * 	<jc>// Serialize — binary formats return byte[].</jc>
 * 	<jk>byte</jk>[] <jv>f</jv> = msgPack(<jv>myBean</jv>);
 * 	<jk>byte</jk>[] <jv>g</jv> = cbor(<jv>myBean</jv>);
 *
 * 	<jc>// Parse — same method names, overloaded by input type.</jc>
 * 	MyBean <jv>b1</jv> = json(<jv>jsonString</jv>, MyBean.<jk>class</jk>);
 * 	MyBean <jv>b2</jv> = xml(<jv>xmlString</jv>, MyBean.<jk>class</jk>);
 * 	MyBean <jv>b3</jv> = msgPack(<jv>bytes</jv>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Parse — parameterized types.</jc>
 * 	List&lt;MyBean&gt; <jv>list</jv> = json(<jv>jsonString</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public final class MarshallUtils {

	private MarshallUtils() {}

	//-----------------------------------------------------------------------------------------------------------------
	// Char-based formats (return String)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Serializes a Java object to a JSON string.
	 *
	 * <p>
	 * A shortcut for calling <c>Json.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String json(Object object) throws SerializeException {
		return Json.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified JSON output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object json(Object object, Object output) throws SerializeException, IOException {
		Json.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a JSON input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T json(Object input, Class<T> type) throws ParseException, IOException {
		return Json.DEFAULT.to(input, type);
	}

	/**
	 * Parses a JSON input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T json(Object input, Type type, Type... args) throws ParseException, IOException {
		return Json.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a JSON string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The JSON input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T json(String o, Class<T> c) {
		return safe(() -> Json.DEFAULT.to(o, c));
	}

	/**
	 * Parses a JSON string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The JSON input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T json(String input, Type type, Type... args) throws ParseException {
		return Json.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a JSON5 string.
	 *
	 * <p>
	 * A shortcut for calling <c>Json5.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String json5(Object object) throws SerializeException {
		return Json5.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified JSON5 output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object json5(Object object, Object output) throws SerializeException, IOException {
		Json5.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a JSON5 input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T json5(Object input, Class<T> type) throws ParseException, IOException {
		return Json5.DEFAULT.to(input, type);
	}

	/**
	 * Parses a JSON5 input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T json5(Object input, Type type, Type... args) throws ParseException, IOException {
		return Json5.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a JSON5 string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The JSON5 input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T json5(String o, Class<T> c) {
		return safe(() -> Json5.DEFAULT.to(o, c));
	}

	/**
	 * Parses a JSON5 string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The JSON5 input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T json5(String input, Type type, Type... args) throws ParseException {
		return Json5.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a JSON Lines string.
	 *
	 * <p>
	 * A shortcut for calling <c>Jsonl.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String jsonl(Object object) throws SerializeException {
		return Jsonl.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified JSON Lines output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object jsonl(Object object, Object output) throws SerializeException, IOException {
		Jsonl.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a JSON Lines input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T jsonl(Object input, Class<T> type) throws ParseException, IOException {
		return Jsonl.DEFAULT.to(input, type);
	}

	/**
	 * Parses a JSON Lines input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T jsonl(Object input, Type type, Type... args) throws ParseException, IOException {
		return Jsonl.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a JSON Lines string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The JSON Lines input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T jsonl(String o, Class<T> c) {
		return safe(() -> Jsonl.DEFAULT.to(o, c));
	}

	/**
	 * Parses a JSON Lines string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The JSON Lines input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T jsonl(String input, Type type, Type... args) throws ParseException {
		return Jsonl.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a canonical JSON string (RFC 8785).
	 *
	 * <p>
	 * A shortcut for calling <c>Jcs.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String jcs(Object object) throws SerializeException {
		return Jcs.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified canonical JSON output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object jcs(Object object, Object output) throws SerializeException, IOException {
		Jcs.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a canonical JSON input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T jcs(Object input, Class<T> type) throws ParseException, IOException {
		return Jcs.DEFAULT.to(input, type);
	}

	/**
	 * Parses a canonical JSON input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T jcs(Object input, Type type, Type... args) throws ParseException, IOException {
		return Jcs.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a canonical JSON string (RFC 8785) to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The JCS input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T jcs(String o, Class<T> c) {
		return safe(() -> Jcs.DEFAULT.to(o, c));
	}

	/**
	 * Parses a canonical JSON string (RFC 8785) to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The JCS input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T jcs(String input, Type type, Type... args) throws ParseException {
		return Jcs.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to an HJSON string.
	 *
	 * <p>
	 * A shortcut for calling <c>Hjson.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String hjson(Object object) throws SerializeException {
		return Hjson.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified HJSON output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object hjson(Object object, Object output) throws SerializeException, IOException {
		Hjson.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses an HJSON input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T hjson(Object input, Class<T> type) throws ParseException, IOException {
		return Hjson.DEFAULT.to(input, type);
	}

	/**
	 * Parses an HJSON input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T hjson(Object input, Type type, Type... args) throws ParseException, IOException {
		return Hjson.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses an HJSON string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The HJSON input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T hjson(String o, Class<T> c) {
		return safe(() -> Hjson.DEFAULT.to(o, c));
	}

	/**
	 * Parses an HJSON string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The HJSON input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T hjson(String input, Type type, Type... args) throws ParseException {
		return Hjson.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to an XML string.
	 *
	 * <p>
	 * A shortcut for calling <c>Xml.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String xml(Object object) throws SerializeException {
		return Xml.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified XML output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object xml(Object object, Object output) throws SerializeException, IOException {
		Xml.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses an XML input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T xml(Object input, Class<T> type) throws ParseException, IOException {
		return Xml.DEFAULT.to(input, type);
	}

	/**
	 * Parses an XML input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T xml(Object input, Type type, Type... args) throws ParseException, IOException {
		return Xml.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses an XML string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The XML input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T xml(String o, Class<T> c) {
		return safe(() -> Xml.DEFAULT.to(o, c));
	}

	/**
	 * Parses an XML string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The XML input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T xml(String input, Type type, Type... args) throws ParseException {
		return Xml.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to an HTML string.
	 *
	 * <p>
	 * A shortcut for calling <c>Html.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String html(Object object) throws SerializeException {
		return Html.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified HTML output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object html(Object object, Object output) throws SerializeException, IOException {
		Html.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses an HTML input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T html(Object input, Class<T> type) throws ParseException, IOException {
		return Html.DEFAULT.to(input, type);
	}

	/**
	 * Parses an HTML input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T html(Object input, Type type, Type... args) throws ParseException, IOException {
		return Html.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses an HTML string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The HTML input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T html(String o, Class<T> c) {
		return safe(() -> Html.DEFAULT.to(o, c));
	}

	/**
	 * Parses an HTML string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The HTML input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T html(String input, Type type, Type... args) throws ParseException {
		return Html.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a UON string.
	 *
	 * <p>
	 * A shortcut for calling <c>Uon.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String uon(Object object) throws SerializeException {
		return Uon.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified UON output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object uon(Object object, Object output) throws SerializeException, IOException {
		Uon.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a UON input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T uon(Object input, Class<T> type) throws ParseException, IOException {
		return Uon.DEFAULT.to(input, type);
	}

	/**
	 * Parses a UON input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T uon(Object input, Type type, Type... args) throws ParseException, IOException {
		return Uon.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a UON string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The UON input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T uon(String o, Class<T> c) {
		return safe(() -> Uon.DEFAULT.to(o, c));
	}

	/**
	 * Parses a UON string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The UON input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T uon(String input, Type type, Type... args) throws ParseException {
		return Uon.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a URL-encoding string.
	 *
	 * <p>
	 * A shortcut for calling <c>UrlEncoding.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String urlEncoding(Object object) throws SerializeException {
		return UrlEncoding.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified URL-encoding output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object urlEncoding(Object object, Object output) throws SerializeException, IOException {
		UrlEncoding.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a URL-encoding input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T urlEncoding(Object input, Class<T> type) throws ParseException, IOException {
		return UrlEncoding.DEFAULT.to(input, type);
	}

	/**
	 * Parses a URL-encoding input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T urlEncoding(Object input, Type type, Type... args) throws ParseException, IOException {
		return UrlEncoding.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a URL-encoding string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The URL-encoding input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T urlEncoding(String o, Class<T> c) {
		return safe(() -> UrlEncoding.DEFAULT.to(o, c));
	}

	/**
	 * Parses a URL-encoding string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The URL-encoding input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T urlEncoding(String input, Type type, Type... args) throws ParseException {
		return UrlEncoding.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a YAML string.
	 *
	 * <p>
	 * A shortcut for calling <c>Yaml.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String yaml(Object object) throws SerializeException {
		return Yaml.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified YAML output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object yaml(Object object, Object output) throws SerializeException, IOException {
		Yaml.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a YAML input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T yaml(Object input, Class<T> type) throws ParseException, IOException {
		return Yaml.DEFAULT.to(input, type);
	}

	/**
	 * Parses a YAML input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T yaml(Object input, Type type, Type... args) throws ParseException, IOException {
		return Yaml.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a YAML string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The YAML input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T yaml(String o, Class<T> c) {
		return safe(() -> Yaml.DEFAULT.to(o, c));
	}

	/**
	 * Parses a YAML string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The YAML input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T yaml(String input, Type type, Type... args) throws ParseException {
		return Yaml.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a CSV string.
	 *
	 * <p>
	 * A shortcut for calling <c>Csv.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String csv(Object object) throws SerializeException {
		return Csv.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified CSV output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object csv(Object object, Object output) throws SerializeException, IOException {
		Csv.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a CSV input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T csv(Object input, Class<T> type) throws ParseException, IOException {
		return Csv.DEFAULT.to(input, type);
	}

	/**
	 * Parses a CSV input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T csv(Object input, Type type, Type... args) throws ParseException, IOException {
		return Csv.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a CSV string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The CSV input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T csv(String o, Class<T> c) {
		return safe(() -> Csv.DEFAULT.to(o, c));
	}

	/**
	 * Parses a CSV string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The CSV input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T csv(String input, Type type, Type... args) throws ParseException {
		return Csv.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to an OpenAPI string.
	 *
	 * <p>
	 * A shortcut for calling <c>OpenApi.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String openApi(Object object) throws SerializeException {
		return OpenApi.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified OpenAPI output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object openApi(Object object, Object output) throws SerializeException, IOException {
		OpenApi.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses an OpenAPI input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T openApi(Object input, Class<T> type) throws ParseException, IOException {
		return OpenApi.DEFAULT.to(input, type);
	}

	/**
	 * Parses an OpenAPI input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T openApi(Object input, Type type, Type... args) throws ParseException, IOException {
		return OpenApi.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses an OpenAPI string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The OpenAPI input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T openApi(String o, Class<T> c) {
		return safe(() -> OpenApi.DEFAULT.to(o, c));
	}

	/**
	 * Parses an OpenAPI string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The OpenAPI input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T openApi(String input, Type type, Type... args) throws ParseException {
		return OpenApi.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a plain-text string.
	 *
	 * <p>
	 * A shortcut for calling <c>PlainText.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String plainText(Object object) throws SerializeException {
		return PlainText.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified plain-text output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object plainText(Object object, Object output) throws SerializeException, IOException {
		PlainText.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a plain-text input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T plainText(Object input, Class<T> type) throws ParseException, IOException {
		return PlainText.DEFAULT.to(input, type);
	}

	/**
	 * Parses a plain-text input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T plainText(Object input, Type type, Type... args) throws ParseException, IOException {
		return PlainText.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a plain-text string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The plain-text input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T plainText(String o, Class<T> c) {
		return safe(() -> PlainText.DEFAULT.to(o, c));
	}

	/**
	 * Parses a plain-text string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The plain-text input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T plainText(String input, Type type, Type... args) throws ParseException {
		return PlainText.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to an inline Markdown string.
	 *
	 * <p>
	 * A shortcut for calling <c>Markdown.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String markdown(Object object) throws SerializeException {
		return Markdown.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified Markdown output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object markdown(Object object, Object output) throws SerializeException, IOException {
		Markdown.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a Markdown input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T markdown(Object input, Class<T> type) throws ParseException, IOException {
		return Markdown.DEFAULT.to(input, type);
	}

	/**
	 * Parses a Markdown input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T markdown(Object input, Type type, Type... args) throws ParseException, IOException {
		return Markdown.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses an inline Markdown string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The Markdown input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T markdown(String o, Class<T> c) {
		return safe(() -> Markdown.DEFAULT.to(o, c));
	}

	/**
	 * Parses an inline Markdown string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The Markdown input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T markdown(String input, Type type, Type... args) throws ParseException {
		return Markdown.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a document-mode Markdown string (headings + tables).
	 *
	 * <p>
	 * A shortcut for calling <c>MarkdownDoc.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String markdownDoc(Object object) throws SerializeException {
		return MarkdownDoc.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified document-mode Markdown output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object markdownDoc(Object object, Object output) throws SerializeException, IOException {
		MarkdownDoc.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a document-mode Markdown input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T markdownDoc(Object input, Class<T> type) throws ParseException, IOException {
		return MarkdownDoc.DEFAULT.to(input, type);
	}

	/**
	 * Parses a document-mode Markdown input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T markdownDoc(Object input, Type type, Type... args) throws ParseException, IOException {
		return MarkdownDoc.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a document-mode Markdown string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The Markdown input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T markdownDoc(String o, Class<T> c) {
		return safe(() -> MarkdownDoc.DEFAULT.to(o, c));
	}

	/**
	 * Parses a document-mode Markdown string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The Markdown input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T markdownDoc(String input, Type type, Type... args) throws ParseException {
		return MarkdownDoc.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to an INI/properties string.
	 *
	 * <p>
	 * A shortcut for calling <c>Ini.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String ini(Object object) throws SerializeException {
		return Ini.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified INI/properties output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object ini(Object object, Object output) throws SerializeException, IOException {
		Ini.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses an INI/properties input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T ini(Object input, Class<T> type) throws ParseException, IOException {
		return Ini.DEFAULT.to(input, type);
	}

	/**
	 * Parses an INI/properties input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T ini(Object input, Type type, Type... args) throws ParseException, IOException {
		return Ini.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses an INI/properties string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The INI input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T ini(String o, Class<T> c) {
		return safe(() -> Ini.DEFAULT.to(o, c));
	}

	/**
	 * Parses an INI/properties string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The INI input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T ini(String input, Type type, Type... args) throws ParseException {
		return Ini.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a TOML string.
	 *
	 * <p>
	 * A shortcut for calling <c>Toml.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String toml(Object object) throws SerializeException {
		return Toml.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified TOML output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object toml(Object object, Object output) throws SerializeException, IOException {
		Toml.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a TOML input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T toml(Object input, Class<T> type) throws ParseException, IOException {
		return Toml.DEFAULT.to(input, type);
	}

	/**
	 * Parses a TOML input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T toml(Object input, Type type, Type... args) throws ParseException, IOException {
		return Toml.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a TOML string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The TOML input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T toml(String o, Class<T> c) {
		return safe(() -> Toml.DEFAULT.to(o, c));
	}

	/**
	 * Parses a TOML string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The TOML input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T toml(String input, Type type, Type... args) throws ParseException {
		return Toml.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a HOCON string.
	 *
	 * <p>
	 * A shortcut for calling <c>Hocon.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String hocon(Object object) throws SerializeException {
		return Hocon.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified HOCON output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object hocon(Object object, Object output) throws SerializeException, IOException {
		Hocon.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a HOCON input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T hocon(Object input, Class<T> type) throws ParseException, IOException {
		return Hocon.DEFAULT.to(input, type);
	}

	/**
	 * Parses a HOCON input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T hocon(Object input, Type type, Type... args) throws ParseException, IOException {
		return Hocon.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a HOCON string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The HOCON input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T hocon(String o, Class<T> c) {
		return safe(() -> Hocon.DEFAULT.to(o, c));
	}

	/**
	 * Parses a HOCON string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The HOCON input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T hocon(String input, Type type, Type... args) throws ParseException {
		return Hocon.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to a Protocol Buffers text string.
	 *
	 * <p>
	 * A shortcut for calling <c>Prototext.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String proto(Object object) throws SerializeException {
		return Prototext.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified Protocol Buffers output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (Writer, OutputStream, File, or StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object proto(Object object, Object output) throws SerializeException, IOException {
		Prototext.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a Protocol Buffers input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T proto(Object input, Class<T> type) throws ParseException, IOException {
		return Prototext.DEFAULT.to(input, type);
	}

	/**
	 * Parses a Protocol Buffers input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, CharSequence, InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T proto(Object input, Type type, Type... args) throws ParseException, IOException {
		return Prototext.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses a Protocol Buffers text string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The Prototext input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T proto(String o, Class<T> c) {
		return safe(() -> Prototext.DEFAULT.to(o, c));
	}

	/**
	 * Parses a Protocol Buffers text string to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The Prototext input string.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T proto(String input, Type type, Type... args) throws ParseException {
		return Prototext.DEFAULT.to(input, type, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Binary formats (return byte[])
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Serializes a Java object to MessagePack bytes.
	 *
	 * <p>
	 * A shortcut for calling <c>MsgPack.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static byte[] msgPack(Object object) throws SerializeException {
		return MsgPack.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified MessagePack output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (OutputStream or File).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object msgPack(Object object, Object output) throws SerializeException, IOException {
		MsgPack.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a MessagePack input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T msgPack(Object input, Class<T> type) throws ParseException, IOException {
		return MsgPack.DEFAULT.to(input, type);
	}

	/**
	 * Parses a MessagePack input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T msgPack(Object input, Type type, Type... args) throws ParseException, IOException {
		return MsgPack.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses MessagePack bytes to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The MessagePack input bytes.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T msgPack(byte[] o, Class<T> c) {
		return safe(() -> MsgPack.DEFAULT.to(o, c));
	}

	/**
	 * Parses MessagePack bytes to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The MessagePack input bytes.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T msgPack(byte[] input, Type type, Type... args) throws ParseException {
		return MsgPack.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to CBOR bytes.
	 *
	 * <p>
	 * A shortcut for calling <c>Cbor.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static byte[] cbor(Object object) throws SerializeException {
		return Cbor.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified CBOR output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (OutputStream or File).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object cbor(Object object, Object output) throws SerializeException, IOException {
		Cbor.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a CBOR input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T cbor(Object input, Class<T> type) throws ParseException, IOException {
		return Cbor.DEFAULT.to(input, type);
	}

	/**
	 * Parses a CBOR input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T cbor(Object input, Type type, Type... args) throws ParseException, IOException {
		return Cbor.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses CBOR bytes to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The CBOR input bytes.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T cbor(byte[] o, Class<T> c) {
		return safe(() -> Cbor.DEFAULT.to(o, c));
	}

	/**
	 * Parses CBOR bytes to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The CBOR input bytes.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T cbor(byte[] input, Type type, Type... args) throws ParseException {
		return Cbor.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to BSON bytes.
	 *
	 * <p>
	 * A shortcut for calling <c>Bson.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static byte[] bson(Object object) throws SerializeException {
		return Bson.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified BSON output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (OutputStream or File).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object bson(Object object, Object output) throws SerializeException, IOException {
		Bson.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a BSON input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T bson(Object input, Class<T> type) throws ParseException, IOException {
		return Bson.DEFAULT.to(input, type);
	}

	/**
	 * Parses a BSON input to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (InputStream, byte[], or File).
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T bson(Object input, Type type, Type... args) throws ParseException, IOException {
		return Bson.DEFAULT.to(input, type, args);
	}

	/**
	 * Parses BSON bytes to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The BSON input bytes.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T bson(byte[] o, Class<T> c) {
		return safe(() -> Bson.DEFAULT.to(o, c));
	}

	/**
	 * Parses BSON bytes to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The BSON input bytes.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T bson(byte[] input, Type type, Type... args) throws ParseException {
		return Bson.DEFAULT.to(input, type, args);
	}

	/**
	 * Serializes a Java object to Apache Parquet bytes.
	 *
	 * <p>
	 * A shortcut for calling <c>Parquet.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * <p class='warnbox'>
	 * 	<b>Note:</b> Parquet is collection-oriented. A single bean is automatically wrapped in a one-element list.
	 * </p>
	 *
	 * @param object The object to serialize (Collection, array, or single bean).
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static byte[] parquet(Object object) throws SerializeException {
		return Parquet.DEFAULT.of(object);
	}

	/**
	 * Serializes a Java object to the specified Parquet output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object (OutputStream or File).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object parquet(Object object, Object output) throws SerializeException, IOException {
		Parquet.DEFAULT.of(object, output); return output;
	}

	/**
	 * Parses a Parquet input to a list of the specified Java type.
	 *
	 * <p class='warnbox'>
	 * 	<b>Note:</b> Parquet always returns a {@link List}; the parser never returns a bare instance.
	 * </p>
	 *
	 * @param <T> The element type.
	 * @param input The input (InputStream, byte[], or File).
	 * @param type The bean class (parser returns {@code List<T>}).
	 * @return The parsed list of beans.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	@SuppressWarnings({
		"unchecked" // Parquet always returns List<T>; generic cast is safe by contract.
	})
	public static <T> List<T> parquet(Object input, Class<T> type) throws ParseException, IOException {
		return (List<T>) Parquet.DEFAULT.to(input, List.class, type);
	}

	/**
	 * Parses Apache Parquet bytes to a list of the specified Java type.
	 *
	 * <p class='warnbox'>
	 * 	<b>Note:</b> Parquet always returns a {@link List}; the parser never returns a bare instance.
	 * </p>
	 *
	 * @param <T> The element type.
	 * @param o The Parquet input bytes.
	 * @param c The bean class (parser returns {@code List<T>}).
	 * @return The parsed list of beans.
	 */
	@SuppressWarnings({
		"unchecked" // Parquet always returns List<T>; generic cast is safe by contract.
	})
	public static <T> List<T> parquet(byte[] o, Class<T> c) {
		return (List<T>) safe(() -> Parquet.DEFAULT.to(o, List.class, c));
	}

	/**
	 * Parses Apache Parquet bytes to a list of the specified element type.
	 *
	 * @param <T> The element type.
	 * @param input The Parquet input bytes.
	 * @param type The element type (parser returns {@code List<T>}).
	 * @return The parsed list.
	 * @throws ParseException Malformed input encountered.
	 */
	@SuppressWarnings({
		"unchecked" // Parquet always returns List<T>; generic cast is safe by contract.
	})
	public static <T> List<T> parquet(byte[] input, Type type) throws ParseException {
		return (List<T>) Parquet.DEFAULT.to(input, List.class, type);
	}

	/**
	 * Parses Apache Parquet bytes to the specified parameterized Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The Parquet input bytes.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T parquet(byte[] input, Type type, Type... args) throws ParseException {
		return Parquet.DEFAULT.to(input, type, args);
	}
}
