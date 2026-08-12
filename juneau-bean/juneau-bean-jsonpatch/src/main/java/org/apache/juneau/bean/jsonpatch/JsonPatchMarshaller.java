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
package org.apache.juneau.bean.jsonpatch;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A pairing of a {@link JsonSerializer} and {@link JsonParser} into a single class with convenience to/of methods,
 * pre-wired for JSON Patch's {@link JsonPatchOperation} discriminator.
 *
 * <p>
 * Every {@link JsonSerializer}/{@link JsonParser} used to read or write {@link JsonPatchOperation} (or
 * {@link JsonPatch}) needs to be configured with
 * <c>.typePropertyName(JsonPatchOperation.<jk>class</jk>, <js>"op"</js>)</c> so that polymorphic dispatch across
 * {@link AddOp}, {@link RemoveOp}, {@link ReplaceOp}, {@link MoveOp}, {@link CopyOp}, and {@link TestOp} resolves
 * via the RFC 6902 {@code "op"} member rather than the default {@code "_type"} property. This class hand-wires
 * that configuration once so callers don't have to repeat it.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	JsonPatchMarshaller <jv>m</jv> = <jk>new</jk> JsonPatchMarshaller();
 * 	JsonPatch <jv>patch</jv> = <jv>m</jv>.read(<jv>string</jv>, JsonPatch.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>m</jv>.write(<jv>patch</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	JsonPatch <jv>patch</jv> = JsonPatchMarshaller.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, JsonPatch.<jk>class</jk>);
 * 	String <jv>string</jv> = JsonPatchMarshaller.<jsf>DEFAULT</jsf>.write(<jv>patch</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using static shortcuts.</jc>
 * 	JsonPatch <jv>patch</jv> = JsonPatchMarshaller.<jsm>to</jsm>(<jv>string</jv>, JsonPatch.<jk>class</jk>);
 * 	String <jv>string</jv> = JsonPatchMarshaller.<jsm>of</jsm>(<jv>patch</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output:</h5>
 * <p class='bjson'>
 * 	[{<js>"op"</js>:<js>"add"</js>,<js>"path"</js>:<js>"/a/b/c"</js>,<js>"value"</js>:<js>"foo"</js>},{<js>"op"</js>:<js>"remove"</js>,<js>"path"</js>:<js>"/a/b/c"</js>}]
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonPatch">juneau-bean-jsonpatch</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor shortcut methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class JsonPatchMarshaller extends CharMarshaller {

	/**
	 * Default {@link JsonSerializer}, pre-wired with the {@code "op"} discriminator for {@link JsonPatchOperation}.
	 */
	public static final JsonSerializer SERIALIZER = JsonSerializer.create()
		.addBeanTypes()
		.addRootType()
		.typePropertyName(JsonPatchOperation.class, "op")
		.build();

	/**
	 * Default {@link JsonParser}, pre-wired with the {@code "op"} discriminator for {@link JsonPatchOperation}.
	 */
	public static final JsonParser PARSER = JsonParser.create()
		.typePropertyName(JsonPatchOperation.class, "op")
		.build();

	/** Default marshaller instance. */
	public static final JsonPatchMarshaller DEFAULT = new JsonPatchMarshaller();

	/**
	 * Serializes a POJO to a <c>String</c> using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Parses an input into the specified object type using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>)</c>.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(String input, Class<T> type) throws ParseException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses an input into the specified parameterized object type using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>, <jv>args</jv>)</c>.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(String input, Type type, Type... args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses the contents of a {@link Reader} into the specified object type using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>)</c> that catches any
	 * {@link IOException} from the underlying stream and rethrows it as an unchecked {@link ParseException}, so the
	 * caller is not burdened with a checked exception.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input reader.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered or an I/O error occurred on the underlying stream.
	 */
	public static <T> T to(Reader input, Class<T> type) throws ParseException {
		try {
			return DEFAULT.read(input, type);
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Parses the contents of a {@link Reader} into the specified parameterized object type using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>, <jv>args</jv>)</c> that catches any
	 * {@link IOException} from the underlying stream and rethrows it as an unchecked {@link ParseException}, so the
	 * caller is not burdened with a checked exception.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input reader.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered or an I/O error occurred on the underlying stream.
	 */
	public static <T> T to(Reader input, Type type, Type... args) throws ParseException {
		try {
			return DEFAULT.read(input, type, args);
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Serializes a POJO to the specified {@link Writer} using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>, <jv>output</jv>)</c> that catches any
	 * {@link IOException} from the underlying stream and rethrows it as an unchecked {@link SerializeException}, so the
	 * caller is not burdened with a checked exception.
	 *
	 * @param object The object to serialize.
	 * @param output The writer to serialize to.
	 * @throws SerializeException If a problem occurred trying to convert the output or an I/O error occurred on the underlying stream.
	 */
	public static void of(Object object, Writer output) throws SerializeException {
		try {
			DEFAULT.write(object, output);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	/** Creates using the pre-wired {@link #SERIALIZER} and {@link #PARSER}. */
	public JsonPatchMarshaller() {
		this(SERIALIZER, PARSER);
	}

	/**
	 * Creates with custom serializer and parser.
	 *
	 * <p>
	 * Callers overriding these should preserve the <c>.typePropertyName(JsonPatchOperation.class, "op")</c>
	 * configuration (and, on the serializer side, <c>.addBeanTypes()</c>/<c>.addRootType()</c>) or polymorphic
	 * dispatch across the {@link JsonPatchOperation} subclasses will not round-trip correctly.
	 *
	 * @param s The serializer.
	 * @param p The parser.
	 */
	public JsonPatchMarshaller(JsonSerializer s, JsonParser p) {
		super(s, p);
	}
}
