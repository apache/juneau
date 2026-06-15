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
package org.apache.juneau.marshall.stream;

/**
 * Format-neutral token vocabulary for Juneau's low-level token-streaming API.
 *
 * <p>
 * Returned by {@link TokenReader#next()} and supplied implicitly by the corresponding
 * {@link TokenWriter} emit methods.
 *
 * <p>
 * The vocabulary is small on purpose &mdash; it is the structural lowest-common-denominator
 * across Juneau's text formats (json, json5, jsonl, xml, html, yaml, toml, hocon, hjson, csv, ...)
 * and binary formats (cbor, msgpack, bson, proto, parquet).  Family-specific information
 * (XML attribute-vs-element, namespaces, CSV row boundaries, UON type-prefixes, binary-native
 * value types like CBOR tags / MsgPack ext / BSON ObjectId &amp; Decimal128 / Parquet logical
 * types) is surfaced via optional metadata accessors on the cursor and via the capability tier
 * declared by the format, not via additional top-level token types.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public enum TokenType {

	/**
	 * Initial cursor state &mdash; no token has been read yet, or the cursor was reset.
	 *
	 * <p>
	 * Returned by {@link TokenReader#getCurrentToken()} before the first call to
	 * {@link TokenReader#next()}.  Never returned by {@link TokenReader#next()} itself.
	 */
	NOT_AVAILABLE,

	/** Start of an object / map / record. */
	START_OBJECT,

	/** End of an object / map / record. */
	END_OBJECT,

	/** Start of an array / list / sequence. */
	START_ARRAY,

	/** End of an array / list / sequence. */
	END_ARRAY,

	/** Field / property / map-key name within an object. */
	FIELD_NAME,

	/** String scalar value. */
	VALUE_STRING,

	/**
	 * Numeric scalar value.
	 *
	 * <p>
	 * Both the original lexeme and a parsed {@link Number} are available via
	 * {@link TokenReader#getNumberLexeme()} and {@link TokenReader#getNumber()} respectively.
	 */
	VALUE_NUMBER,

	/** Boolean scalar value (<jk>true</jk>/<jk>false</jk>). */
	VALUE_BOOLEAN,

	/** Explicit null scalar value. */
	VALUE_NULL,

	/**
	 * Binary scalar value &mdash; raw bytes carried by binary formats (cbor, msgpack, bson, ...).
	 *
	 * <p>
	 * Text formats normally do not emit this token; binary content embedded in text formats is
	 * delivered as {@link #VALUE_STRING} (typically base64-encoded) per format convention.
	 */
	VALUE_BINARY,

	/** End of input.  Returned by {@link TokenReader#next()} once the underlying stream is exhausted. */
	END_OF_STREAM
}
