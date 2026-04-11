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
package org.apache.juneau.jsonl;

import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.swap.ObjectSwap;

/**
 * Serializes POJO models to JSONL (JSON Lines).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/jsonl, application/x-ndjson, text/jsonl</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/jsonl</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Serializes POJOs to JSONL format where each top-level value is written as a compact JSON
 * value on its own line.  The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>Maps and beans are converted to JSON objects, one per line.
 * 	<li>Collections and arrays are converted to one JSON value per line (no wrapping brackets).
 * 	<li>Strings are converted to JSON strings, one per line.
 * 	<li>Numbers are converted to JSON numbers, one per line.
 * 	<li>Booleans are converted to JSON booleans, one per line.
 * 	<li>Nulls are converted to JSON nulls, one per line.
 * 	<li>Nested objects and collections remain compact on a single line per outer element.
 * </ul>
 * <p>
 * Non-JSON-primitive types are transformed through
 * {@link ObjectSwap ObjectSwaps}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use the default serializer to serialize a list of POJOs</jc>
 * 	String <jv>jsonl</jv> = JsonlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myList</jv>);
 *
 * 	<jc>// Create a custom serializer</jc>
 * 	JsonlSerializer <jv>serializer</jv> = JsonlSerializer.<jsm>create</jsm>().build();
 *
 * 	<jc>// Clone an existing serializer</jc>
 * 	<jv>serializer</jv> = JsonlSerializer.<jsf>DEFAULT</jsf>.copy().build();
 *
 * 	<jc>// Serialize a list of POJOs to JSONL</jc>
 * 	String <jv>jsonl</jv> = <jv>serializer</jv>.serialize(<jv>myList</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (List of beans):</h5>
 * <p class='bjson'>
 * {"name":"Alice","age":30}
 * {"name":"Bob","age":25}
 * {"name":"Carol","age":35}
 * </p>
 *
 * <h5 class='figure'>Complex (bean with nested object and array):</h5>
 * <p class='bjson'>
 * {"name":"Alice","age":30,"address":{"street":"123 Main St","city":"Boston"},"tags":["a","b","c"]}
 * {"name":"Bob","age":25,"address":{"street":"456 Oak Ave","city":"Portland"},"tags":["d","e"]}
 * </p>
 *
 * <h5 class='section'>Limitations compared to JSON</h5>
 * <p>
 * <ul class='spaced-list'>
 * 	<li><b>No pretty-printing</b>: {@link org.apache.juneau.serializer.WriterSerializer.Builder#useWhitespace() useWhitespace()}
 * 		is always suppressed.  JSONL requires each JSON value to fit on a single line;
 * 		applying intra-line pretty-printing would embed newlines and produce invalid JSONL.
 * 		The setting is silently ignored regardless of its configured value.
 * 	<li><b>Top-level semantics differ</b>: Collections and arrays are unwrapped — each element
 * 		becomes one line.  A single non-collection object produces one JSONL line.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonlBasics">JSONL Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable
})
public class JsonlSerializer extends JsonSerializer {

	/** Default serializer. */
	public static final JsonlSerializer DEFAULT = new JsonlSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static JsonSerializer.Builder create() {
		return JsonSerializer.create()
			.produces("application/jsonl")
			.accept("application/jsonl,application/x-ndjson,text/jsonl")
			.type(JsonlSerializer.class)
			.useWhitespace(false);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonlSerializer(JsonSerializer.Builder builder) {
		super(builder.useWhitespace(false));
	}

	@Override /* Overridden from Context */
	public JsonlSerializerSession.Builder createSession() {
		return JsonlSerializerSession.create(this);
	}
}
