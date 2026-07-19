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

import java.io.*;
import java.math.*;

import org.apache.juneau.marshall.serializer.*;

/**
 * Public low-level push-generator that emits a Juneau-formatted document one structural event at a
 * time.
 *
 * <p>
 * The caller emits structural events ({@link #startObject()} / {@link #fieldName(String)} /
 * {@link #number(long)} / ...) and the implementation produces the corresponding bytes for its
 * format.  This is a format-neutral surface; the same writer methods work across every Juneau
 * format that implements {@link TokenWritable} (the structural-token role).
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder();
 * 	<jk>try</jk> (TokenWriter <jv>w</jv> = Json.<jsf>DEFAULT</jsf>.writeTokens(<jv>sb</jv>)) {
 * 		<jv>w</jv>.startObject();
 * 		<jv>w</jv>.fieldName(<js>"a"</js>);
 * 		<jv>w</jv>.number(1);
 * 		<jv>w</jv>.fieldName(<js>"b"</js>);
 * 		<jv>w</jv>.startArray();
 * 		<jv>w</jv>.bool(<jk>true</jk>);
 * 		<jv>w</jv>.nil();
 * 		<jv>w</jv>.endArray();
 * 		<jv>w</jv>.endObject();
 * 	}
 * 	<jc>// sb is now: {"a":1,"b":[true,null]}</jc>
 * </p>
 *
 * <h5 class='section'>Contract:</h5>
 * <ul>
 * 	<li>The writer is <b>purely structural</b>.  It bypasses object swaps and {@code @Schema}
 * 		annotations &mdash; that is the role of the POJO databind path on
 * 		{@link Serializer Serializer} and its bridges.
 * 	<li>It is the caller's responsibility to emit a well-formed event sequence (every
 * 		{@code startObject()} matched by an {@code endObject()}, every field name followed by
 * 		exactly one value or container, etc.).  Implementations are not required to validate the
 * 		sequence, though they may.
 * 	<li>Concrete implementations are not required to be thread-safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link TokenReader} &mdash; the pull-side counterpart.
 * 	<li>{@link TokenType} &mdash; the format-neutral token vocabulary.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // TokenWriter wraps the caller's writer; the underlying Closeable is caller-owned, so the resource-leak warning is by design.
})
public interface TokenWriter extends RecordWriter {

	/**
	 * Emits a {@link TokenType#START_OBJECT} event.
	 *
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter startObject() throws IOException;

	/**
	 * Emits a {@link TokenType#END_OBJECT} event.
	 *
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter endObject() throws IOException;

	/**
	 * Emits a {@link TokenType#START_ARRAY} event.
	 *
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter startArray() throws IOException;

	/**
	 * Emits a {@link TokenType#END_ARRAY} event.
	 *
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter endArray() throws IOException;

	/**
	 * Emits a {@link TokenType#FIELD_NAME} event for the next field of the enclosing object.
	 *
	 * @param name The field name.  Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter fieldName(String name) throws IOException;

	/**
	 * Emits a {@link TokenType#VALUE_STRING} event.
	 *
	 * @param value The value.  May be <jk>null</jk>, in which case a {@link TokenType#VALUE_NULL}
	 * 	event is emitted instead.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter string(String value) throws IOException;

	/**
	 * Emits a {@link TokenType#VALUE_NUMBER} event for an arbitrary {@link Number}.
	 *
	 * @param value The value.  May be <jk>null</jk>, in which case a {@link TokenType#VALUE_NULL}
	 * 	event is emitted instead.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter number(Number value) throws IOException;

	/**
	 * Emits a {@link TokenType#VALUE_NUMBER} event for a {@code long}.
	 *
	 * @param value The value.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter number(long value) throws IOException;

	/**
	 * Emits a {@link TokenType#VALUE_NUMBER} event for a {@code double}.
	 *
	 * @param value The value.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter number(double value) throws IOException;

	/**
	 * Emits a {@link TokenType#VALUE_NUMBER} event for a {@link BigDecimal}.
	 *
	 * @param value The value.  May be <jk>null</jk>, in which case a {@link TokenType#VALUE_NULL}
	 * 	event is emitted instead.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter number(BigDecimal value) throws IOException;

	/**
	 * Emits a {@link TokenType#VALUE_NUMBER} event for a {@link BigInteger}.
	 *
	 * @param value The value.  May be <jk>null</jk>, in which case a {@link TokenType#VALUE_NULL}
	 * 	event is emitted instead.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter number(BigInteger value) throws IOException;

	/**
	 * Emits a {@link TokenType#VALUE_BOOLEAN} event.
	 *
	 * @param value The value.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter bool(boolean value) throws IOException;

	/**
	 * Emits a {@link TokenType#VALUE_NULL} event.
	 *
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter nil() throws IOException;

	/**
	 * Emits an arbitrary POJO through the format's structural events.
	 *
	 * <p>
	 * The value is walked via {@link PojoWalker}: nulls / scalars / collections / arrays / maps /
	 * beans are introspected and re-emitted through this writer's
	 * {@link #startObject()}, {@link #fieldName(String)}, {@link #string(String)},
	 * {@link #number(long)}, {@link #bool(boolean)}, {@link #nil()}, and {@link #binary(byte[])}
	 * methods.  Per-format encoding (quoting, indentation, newlines, etc.) is whatever this
	 * writer is configured for.
	 *
	 * <p>
	 * <b>Cursor positioning:</b> the writer must be in a state where a value emit is legal &mdash;
	 * at root, after a {@link #fieldName(String)} call, or inside an array.  Calling
	 * {@code object(...)} from inside an object body without a preceding field name throws
	 * {@link IllegalStateException}, same as calling {@link #string(String)} would.
	 *
	 * <p>
	 * <b>Default behavior:</b> the default implementation throws
	 * {@link UnsupportedOperationException}.  Format-specific token writers override this by
	 * delegating to {@link PojoWalker#walk(TokenWriter, Object)}.
	 *
	 * <p>
	 * <b>Swaps and {@code @Schema}:</b> this method is purely structural.  Object swaps and
	 * schema annotations are NOT applied at the token layer &mdash; use the POJO databind path on
	 * {@link Serializer#write(Object)} for those.
	 *
	 * @param value The value to emit.  May be <jk>null</jk>.
	 * @return This object.
	 * @throws IOException If the underlying stream fails.
	 */
	default TokenWriter object(Object value) throws IOException {
		throw new UnsupportedOperationException(
			"object(Object) is not implemented for " + getClass().getName());
	}

	/**
	 * Implements {@link RecordWriter#write(Object)} by delegating to {@link #object(Object)}.
	 *
	 * @param value The value.  May be <jk>null</jk>.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 */
	@Override
	default TokenWriter write(Object value) throws IOException {
		object(value);
		return this;
	}

	/**
	 * Emits a {@link TokenType#VALUE_BINARY} event.
	 *
	 * <p>
	 * Text formats normally encode the value as base64-encoded {@link TokenType#VALUE_STRING}; the
	 * exact encoding is format-defined.
	 *
	 * @param value The value.  May be <jk>null</jk>, in which case a {@link TokenType#VALUE_NULL}
	 * 	event is emitted instead.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws SerializeException If the format cannot represent this event in the current state.
	 */
	TokenWriter binary(byte[] value) throws IOException;

	/**
	 * Structural token cursors are always O(1)-memory streamers.
	 *
	 * @return Always <jk>true</jk>.
	 */
	@Override
	default boolean isStreaming() { return true; }

	/**
	 * Flushes any buffered output to the underlying stream.
	 *
	 * @throws IOException If a problem occurred flushing the underlying stream.
	 */
	@Override
	void flush() throws IOException;

	/**
	 * Closes the writer, flushing any buffered output first.
	 *
	 * <p>
	 * Implementations should be idempotent &mdash; closing a closed writer is a no-op.
	 */
	@Override
	void close() throws IOException;

	// =================================================================================
	// Binary-native opt-in emits.  These default to UnsupportedOperationException so
	// formats override only the subset they support (CBOR overrides writeTag and
	// writeSimple; MsgPack overrides writeExt; text formats inherit the throwing
	// defaults).  See {@link BinaryNativeKind} for the catalog of native concepts.
	// =================================================================================

	/**
	 * Emits a semantic tag prefixing the next value emit.
	 *
	 * <p>
	 * Tags compose: {@code writeTag(1).writeTag(2).number(5)} writes {@code tag(1)(tag(2)(5))}.
	 * The tag header is written immediately and does NOT consume a map-key/value or array-element
	 * slot; that bookkeeping belongs to the wrapped value emit that follows.
	 *
	 * @param tagNumber The unsigned tag number (carried as a {@code long} for the full unsigned
	 * 	64-bit range).
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws UnsupportedOperationException If the format does not support semantic tags.
	 */
	default TokenWriter writeTag(long tagNumber) throws IOException {
		throw new UnsupportedOperationException("writeTag is not supported by this writer.");
	}

	/**
	 * Emits an opaque simple value.  The corresponding read-side token is
	 * {@link TokenType#VALUE_NULL} with {@link TokenReader#getSimpleValue()}.
	 *
	 * @param value The simple value.  Format-specific reserved encodings (e.g. CBOR major-7
	 * 	codepoints 20-23, 25-27, 31 collide with bool/null/undefined/float/break) must not be
	 * 	used.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws UnsupportedOperationException If the format does not support opaque simple values.
	 */
	default TokenWriter writeSimple(int value) throws IOException {
		throw new UnsupportedOperationException("writeSimple is not supported by this writer.");
	}

	/**
	 * Emits a typed-binary value (signed type byte + binary payload).  The corresponding
	 * read-side token is {@link TokenType#VALUE_BINARY} with {@link TokenReader#getExtType()}
	 * and {@link TokenReader#getBinary()}.
	 *
	 * @param type The signed type byte (range {@code -128..127}).
	 * @param payload The payload bytes.  Must not be {@code null}.
	 * @return This object.
	 * @throws IOException If a problem occurred writing to the underlying stream.
	 * @throws UnsupportedOperationException If the format does not support typed-binary values.
	 */
	default TokenWriter writeExt(int type, byte[] payload) throws IOException {
		throw new UnsupportedOperationException("writeExt is not supported by this writer.");
	}
}
