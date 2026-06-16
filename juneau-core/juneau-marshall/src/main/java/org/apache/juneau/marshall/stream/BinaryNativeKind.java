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
 * Discriminant returned by {@link TokenReader#getNativeKind()} that classifies the
 * binary-native metadata (if any) carried by the cursor's current token.
 *
 * <p>
 * A {@link TokenReader} surfaces format-native value types as <b>metadata on the existing
 * token vocabulary</b> rather than as new top-level events.  A native wrapper rides on whatever
 * token the wrapped value emits ({@code VALUE_*} or {@code START_*}); this enum lets a
 * format-agnostic consumer branch on the kind of native metadata without having to test the
 * cursor's runtime type.
 *
 * <p>
 * Each enum value names the concrete native concept it represents.  New values may be added in
 * future minor releases as additional formats expose native opt-in metadata.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link TokenReader} &mdash; the cursor that exposes this discriminant.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public enum BinaryNativeKind {

	/** The current token carries no binary-native metadata. */
	NONE,

	/**
	 * The current token is wrapped by one or more semantic tags (CBOR major type 6).  Surfaced
	 * via {@link TokenReader#getTagCount()} / {@link TokenReader#getTag(int)}.
	 */
	CBOR_TAG,

	/**
	 * The current token represents a CBOR simple value (major type 7).  Token type is
	 * {@link TokenType#VALUE_NULL}; the simple int is surfaced via
	 * {@link TokenReader#getSimpleValue()}.
	 */
	CBOR_SIMPLE,

	/**
	 * The current token represents a MsgPack {@code ext} value (signed type byte + binary
	 * payload).  Token type is {@link TokenType#VALUE_BINARY}; the type byte is surfaced via
	 * {@link TokenReader#getExtType()}.
	 */
	MSGPACK_EXT
}
