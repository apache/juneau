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
package org.apache.juneau.marshall.prototext;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Parses Protobuf Text Format into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>text/protobuf, text/x-protobuf</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This parser uses a tokenizer to parse protobuf text format without requiring an intermediate DOM
 * representation.
 *
 * <p>
 * Handles all valid Protobuf Text Format syntax including:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Message fields using brace delimiters ({@code field { ... }}).
 * 	<li>
 * 		Scalar fields using colon syntax ({@code field: value}).
 * 	<li>
 * 		List syntax ({@code field: [v1, v2]}) for collections of simple values.
 * 	<li>
 * 		Repeated field names aggregated into collections of beans.
 * 	<li>
 * 		Integers: decimal, hex ({@code 0x...}), octal ({@code 0...}).
 * 	<li>
 * 		Floats including special values {@code inf}, {@code -inf}, and {@code nan}.
 * 	<li>
 * 		Single and double quoted strings with C-style escape sequences ({@code \n}, {@code \t},
 * 		{@code \\}, {@code \"}, &#92;xHH, &#92;OOO, &#92;uHHHH).
 * 	<li>
 * 		Multi-part adjacent string concatenation (e.g. {@code "hello" "world"}).
 * 	<li>
 * 		Angle-bracket message delimiters ({@code < >}) as an alternative to braces.
 * 	<li>
 * 		Comments ({@code # ...} to end of line).
 * 	<li>
 * 		Optional field separators: newlines, semicolons, or commas.
 * </ul>
 *
 * <p>
 * This parser converts protobuf messages to {@link JsonMap JsonMaps}.
 * If a <c>_type</c> field is present, an attempt is made to convert the message to the specified
 * Java bean class.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use the default parser to parse into a bean</jc>
 * 	MyBean <jv>bean</jv> = PrototextParser.<jsf>DEFAULT</jsf>.parse(<jv>input</jv>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Parse into an untyped map</jc>
 * 	JsonMap <jv>map</jv> = PrototextParser.<jsf>DEFAULT</jsf>.parse(<jv>input</jv>, JsonMap.<jk>class</jk>);
 *
 * 	<jc>// Create a parser with validate-end enabled</jc>
 * 	PrototextParser <jv>parser</jv> = PrototextParser.<jsm>create</jsm>().build();
 * 	MyBean <jv>bean</jv> = <jv>parser</jv>.parse(<jv>input</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Prototext">Protobuf Text Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Builder pattern requires many parameters
	"java:S115",  // ARG_ prefix follows framework convention
	"resource" // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class PrototextParser extends ReaderParser implements PrototextMetaProvider, RecordReadable {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder for {@link PrototextParser}.
	 */
	public static class Builder extends ReaderParser.Builder<Builder> {

		private static final Cache<HashKey, PrototextParser> CACHE =
			Cache.of(HashKey.class, PrototextParser.class).build();

		protected Builder() {
			consumes("text/protobuf,text/x-protobuf");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		protected Builder(PrototextParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override
		public PrototextParser build() {
			return cache(CACHE).build(PrototextParser.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}
	}

	/** Default parser instance. */
	public static final PrototextParser DEFAULT = new PrototextParser(create());

	private final Map<ClassMeta<?>, PrototextClassMeta> prototextClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta, PrototextBeanPropertyMeta> prototextBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public PrototextParser(Builder builder) {
		super(builder);
	}

	@Override
	public PrototextParserSession.Builder createSession() {
		return PrototextParserSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* PrototextMetaProvider */
	public PrototextBeanPropertyMeta getPrototextBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return PrototextBeanPropertyMeta.DEFAULT;
		return prototextBeanPropertyMetas.computeIfAbsent(bpm, k -> new PrototextBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* PrototextMetaProvider */
	public PrototextClassMeta getPrototextClassMeta(ClassMeta<?> cm) {
		return prototextClassMetas.computeIfAbsent(cm, k -> new PrototextClassMeta(k, this));
	}

	/**
	 * Convenience delegator that opens a {@link RecordReader} over the input using
	 * <b>default session arguments</b> (mirrors {@link #parse(Object, Class)}).
	 *
	 * <p>
	 * The real implementation lives on {@link PrototextParserSession#parseRecords(Object)}.  Callers
	 * that need request-derived configuration (locale, timezone, schema, swaps) should call
	 * {@link #createSession()} and invoke {@link PrototextParserSession#parseRecords(Object)} on the
	 * built session instead.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader} cursor.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* RecordReadable */
	public RecordReader parseRecords(Object input) throws IOException {
		return ((RecordReadable) getSession()).parseRecords(input);
	}

	@Override /* RecordReadable */
	public boolean isRecordStreaming() {
		return false;
	}
}
