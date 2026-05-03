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
package org.apache.juneau.marshaller;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.serializer.*;

/**
 * Convenience static methods for serializing objects with all supported Juneau marshalling formats.
 *
 * <p>
 * Designed for use with static imports to allow concise serialization code without explicitly
 * referencing marshaller classes.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.marshaller.MarshallUtils.*;
 *
 * 	<jc>// Serialize — char-based formats return String.</jc>
 * 	String <jv>a</jv> = json(<jv>myBean</jv>);
 * 	String <jv>b</jv> = json5(<jv>myBean</jv>);
 * 	String <jv>c</jv> = xml(<jv>myBean</jv>);
 * 	String <jv>d</jv> = html(<jv>myBean</jv>);
 * 	String <jv>e</jv> = yaml(<jv>myBean</jv>);
 *
 * 	<jc>// Serialize — binary formats return byte[].</jc>
 * 	<jk>byte</jk>[] <jv>f</jv> = msgPack(<jv>myBean</jv>);
 * 	<jk>byte</jk>[] <jv>g</jv> = cbor(<jv>myBean</jv>);
 *
 * 	<jc>// Parse — same method names, overloaded by input type.</jc>
 * 	MyBean <jv>b1</jv> = json(<jv>jsonString</jv>, MyBean.<jk>class</jk>);
 * 	MyBean <jv>b2</jv> = xml(<jv>xmlString</jv>, MyBean.<jk>class</jk>);
 * 	MyBean <jv>b3</jv> = msgPack(<jv>bytes</jv>, MyBean.<jk>class</jk>);
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
		return Json.of(object);
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
		return Json5.of(object);
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
		return Jsonl.of(object);
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
		return Jcs.of(object);
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
		return Hjson.of(object);
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
		return Xml.of(object);
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
		return Html.of(object);
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
		return Uon.of(object);
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
		return UrlEncoding.of(object);
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
		return Yaml.of(object);
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
		return Csv.of(object);
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
		return OpenApi.of(object);
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
		return PlainText.of(object);
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
		return Markdown.of(object);
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
		return MarkdownDoc.of(object);
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
		return Ini.of(object);
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
		return Toml.of(object);
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
		return Hocon.of(object);
	}

	/**
	 * Serializes a Java object to a Protocol Buffers text string.
	 *
	 * <p>
	 * A shortcut for calling <c>Proto.<jsm>of</jsm>(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String proto(Object object) throws SerializeException {
		return Proto.of(object);
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
		return MsgPack.of(object);
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
		return Cbor.of(object);
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
		return Bson.of(object);
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
		return Parquet.of(object);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parse methods (overloads of the serialize methods, distinguished by input type)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Parses a JSON string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The JSON input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T json(String o, Class<T> c) {
		return safe(() -> Json.to(o, c));
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
		return safe(() -> Json5.to(o, c));
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
		return safe(() -> Jsonl.to(o, c));
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
		return safe(() -> Jcs.to(o, c));
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
		return safe(() -> Hjson.to(o, c));
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
		return safe(() -> Xml.to(o, c));
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
		return safe(() -> Html.to(o, c));
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
		return safe(() -> Uon.to(o, c));
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
		return safe(() -> UrlEncoding.to(o, c));
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
		return safe(() -> Yaml.to(o, c));
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
		return safe(() -> Csv.to(o, c));
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
		return safe(() -> OpenApi.to(o, c));
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
		return safe(() -> PlainText.to(o, c));
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
		return safe(() -> Markdown.to(o, c));
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
		return safe(() -> MarkdownDoc.to(o, c));
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
		return safe(() -> Ini.to(o, c));
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
		return safe(() -> Toml.to(o, c));
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
		return safe(() -> Hocon.to(o, c));
	}

	/**
	 * Parses a Protocol Buffers text string to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param o The Proto input string.
	 * @param c The object type to create.
	 * @return The parsed object.
	 */
	public static <T> T proto(String o, Class<T> c) {
		return safe(() -> Proto.to(o, c));
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
		return safe(() -> MsgPack.to(o, c));
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
		return safe(() -> Cbor.to(o, c));
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
		return safe(() -> Bson.to(o, c));
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
	public static <T> List<T> parquet(byte[] o, Class<T> c) {
		return safe(() -> Parquet.to(o, c));
	}
}
