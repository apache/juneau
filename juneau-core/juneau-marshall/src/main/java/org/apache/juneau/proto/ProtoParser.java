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
package org.apache.juneau.proto;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.parser.*;

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
 * This parser converts protobuf messages to {@link org.apache.juneau.collections.JsonMap JsonMaps}.
 * If a <c>_type</c> field is present, an attempt is made to convert the message to the specified
 * Java bean class.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use the default parser to parse into a bean</jc>
 * 	MyBean <jv>bean</jv> = ProtoParser.<jsf>DEFAULT</jsf>.parse(<jv>input</jv>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Parse into an untyped map</jc>
 * 	JsonMap <jv>map</jv> = ProtoParser.<jsf>DEFAULT</jsf>.parse(<jv>input</jv>, JsonMap.<jk>class</jk>);
 *
 * 	<jc>// Create a strict-mode parser</jc>
 * 	ProtoParser <jv>parser</jv> = ProtoParser.<jsm>create</jsm>().strict().build();
 * 	MyBean <jv>bean</jv> = <jv>parser</jv>.parse(<jv>input</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Builder pattern requires many parameters
	"java:S115"   // ARG_ prefix follows framework convention
})
public class ProtoParser extends ReaderParser implements ProtoMetaProvider {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder for {@link ProtoParser}.
	 */
	public static class Builder extends ReaderParser.Builder {

		private static final Cache<HashKey, ProtoParser> CACHE =
			Cache.of(HashKey.class, ProtoParser.class).build();

		protected Builder() {
			consumes("text/protobuf,text/x-protobuf");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		protected Builder(ProtoParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override
		public ProtoParser build() {
			return cache(CACHE).build(ProtoParser.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}
	}

	/** Default parser instance. */
	public static final ProtoParser DEFAULT = new ProtoParser(create());

	private final Map<ClassMeta<?>, ProtoClassMeta> protoClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta, ProtoBeanPropertyMeta> protoBeanPropertyMetas = new ConcurrentHashMap<>();

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
	public ProtoParser(Builder builder) {
		super(builder);
	}

	@Override
	public ProtoParserSession.Builder createSession() {
		return ProtoParserSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* ProtoMetaProvider */
	public ProtoBeanPropertyMeta getProtoBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return ProtoBeanPropertyMeta.DEFAULT;
		return protoBeanPropertyMetas.computeIfAbsent(bpm, k -> new ProtoBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* ProtoMetaProvider */
	public ProtoClassMeta getProtoClassMeta(ClassMeta<?> cm) {
		return protoClassMetas.computeIfAbsent(cm, k -> new ProtoClassMeta(k, this));
	}
}
