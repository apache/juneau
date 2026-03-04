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

import java.util.Collection;

import org.apache.juneau.collections.JsonList;
import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.json.JsonParser;

/**
 * Parses JSONL (JSON Lines) input into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/jsonl, application/x-ndjson, text/jsonl</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Parses JSONL input where each non-empty line is a complete JSON value.  Blank lines are ignored.
 * The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>JSON objects (<c>{...}</c>) are converted to {@link JsonMap JsonMaps}
 * 		or Java beans if a target type is provided.
 * 	<li>JSON arrays (<c>[...]</c>) are converted to {@link JsonList JsonLists}.
 * 	<li>JSON strings are converted to {@link String Strings}.
 * 	<li>JSON numbers are converted to {@link Integer Integers}, {@link Long Longs},
 * 		{@link Float Floats}, or {@link Double Doubles}.
 * 	<li>JSON booleans are converted to {@link Boolean Booleans}.
 * 	<li>JSON nulls are converted to <jk>null</jk>.
 * </ul>
 * <p>
 * When the target type is a {@link Collection} or array, each line is parsed
 * as one element.  When the target type is a single value, only the first non-empty line is parsed.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use the default parser to parse JSONL into a list of POJOs</jc>
 * 	List&lt;MyBean&gt; <jv>list</jv> = JsonlParser.<jsf>DEFAULT</jsf>.parse(<jv>jsonlInput</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Create a custom parser</jc>
 * 	JsonlParser <jv>parser</jv> = JsonlParser.<jsm>create</jsm>().build();
 *
 * 	<jc>// Clone an existing parser</jc>
 * 	<jv>parser</jv> = JsonlParser.<jsf>DEFAULT</jsf>.copy().build();
 * </p>
 *
 * <h5 class='figure'>Example input:</h5>
 * <p class='bjson'>
 * {"name":"Alice","age":30}
 * {"name":"Bob","age":25}
 * {"name":"Carol","age":35}
 * </p>
 *
 * <h5 class='section'>Limitations compared to JSON</h5>
 * <p>
 * <ul class='spaced-list'>
 * 	<li><b>No multi-line JSON input</b>: Each JSON value must be on a single line.
 * 		Multi-line (pretty-printed) JSON piped as JSONL input will cause parse errors.
 * 	<li><b>First-line-only for non-collection targets</b>: When parsing JSONL to a single
 * 		non-collection type, only the first non-empty line is parsed; trailing lines are
 * 		silently ignored.
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
public class JsonlParser extends JsonParser {

	/** Default parser. */
	public static final JsonlParser DEFAULT = new JsonlParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static JsonParser.Builder create() {
		return JsonParser.create()
			.consumes("application/jsonl,application/x-ndjson,text/jsonl")
			.type(JsonlParser.class);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonlParser(JsonParser.Builder builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public JsonlParserSession.Builder createSession() {
		return JsonlParserSession.create(this);
	}
}
