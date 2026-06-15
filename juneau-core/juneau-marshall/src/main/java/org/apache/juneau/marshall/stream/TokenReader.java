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
import java.lang.reflect.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Public low-level pull-parser cursor over a Juneau-formatted document.
 *
 * <p>
 * A StAX-like enum cursor that emits a stream of {@link TokenType} events without materializing
 * the whole object graph.  This is a format-neutral surface; the same cursor methods work across
 * every Juneau format that implements {@link TokenReadable} (the structural-token role).
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Tokenize a JSON document without materializing it.</jc>
 * 	<jk>try</jk> (TokenReader <jv>r</jv> = Json.<jsf>DEFAULT</jsf>.parseTokens(<js>"{\"a\":1,\"b\":[true,null]}"</js>)) {
 * 		<jk>while</jk> (<jv>r</jv>.next() != TokenType.<jsf>END_OF_STREAM</jsf>) {
 * 			<jk>switch</jk> (<jv>r</jv>.getCurrentToken()) {
 * 				<jk>case</jk> <jsf>FIELD_NAME</jsf>   -&gt; handleKey(<jv>r</jv>.getFieldName());
 * 				<jk>case</jk> <jsf>VALUE_STRING</jsf> -&gt; handleString(<jv>r</jv>.getString());
 * 				<jk>case</jk> <jsf>VALUE_NUMBER</jsf> -&gt; handleNumber(<jv>r</jv>.getNumber());
 * 				<jk>default</jk> -&gt; { <jc>// structural </jc>}
 * 			}
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Contract:</h5>
 * <ul>
 * 	<li>The cursor is <b>purely structural</b>.  It bypasses object swaps and {@code @Schema}
 * 		annotations &mdash; that is the role of the POJO databind path on
 * 		{@link Parser Parser} and its bridges.
 * 	<li>For numeric values both the original lexeme and a parsed {@link Number} are available
 * 		(see {@link #getNumberLexeme()} and {@link #getNumber()}); precision-sensitive
 * 		consumers should prefer the lexeme.
 * 	<li>Concrete implementations are not required to be thread-safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li>{@link TokenWriter} &mdash; the push-side counterpart.
 * 	<li>{@link TokenType} &mdash; the format-neutral token vocabulary.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public interface TokenReader extends RecordReader {

	/**
	 * Canonical message thrown as an {@link IllegalStateException} when {@link #read(Class)} is
	 * called while the cursor is not positioned at a value boundary.
	 *
	 * <p>
	 * Shared across all formats so the positioning/error contract is uniform (see the
	 * <c>read</c> contract); implementations throw <c><jk>new</jk> IllegalStateException(
	 * {@link #MSG_READ_NON_VALUE_STATE})</c> rather than inventing their own wording.
	 */
	String MSG_READ_NON_VALUE_STATE =
		"read called from a non-value cursor state.  The cursor must be positioned such that next() "
		+ "would emit START_OBJECT / START_ARRAY / VALUE_*; the current state would emit a structural "
		+ "separator, field name, end-container, or end-of-stream instead.";

	/**
	 * Advances the cursor to the next token.
	 *
	 * @return The token type of the new current token, or {@link TokenType#END_OF_STREAM} when
	 * 	the underlying input is exhausted.  Never returns {@link TokenType#NOT_AVAILABLE} &mdash;
	 * 	that value is reserved for the initial pre-{@code next()} state.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed for the format.
	 */
	TokenType next() throws IOException, ParseException;

	/**
	 * Returns the current token type without advancing.
	 *
	 * @return The current token type.  Returns {@link TokenType#NOT_AVAILABLE} before the first
	 * 	call to {@link #next()}, and {@link TokenType#END_OF_STREAM} once the stream is exhausted.
	 */
	TokenType getCurrentToken();

	/**
	 * Returns the current depth of the cursor.
	 *
	 * <p>
	 * Depth starts at <c>0</c> at the document root and increases by 1 for each
	 * {@link TokenType#START_OBJECT} / {@link TokenType#START_ARRAY} encountered (and decreases on
	 * the matching end tokens).
	 *
	 * @return The current depth.
	 */
	int getDepth();

	/**
	 * Returns the field name when the current token is {@link TokenType#FIELD_NAME}.
	 *
	 * @return The field name.
	 * @throws IllegalStateException If the current token is not {@link TokenType#FIELD_NAME}.
	 */
	String getFieldName();

	/**
	 * Returns the current scalar value as a string.
	 *
	 * <p>
	 * Defined for {@link TokenType#VALUE_STRING} (the natural value), {@link TokenType#FIELD_NAME}
	 * (returns the field name), {@link TokenType#VALUE_NUMBER} (returns the lexeme), and
	 * {@link TokenType#VALUE_BOOLEAN} (returns <js>"true"</js>/<js>"false"</js>).  For
	 * {@link TokenType#VALUE_NULL} returns <jk>null</jk>.
	 *
	 * @return The string view of the current value.
	 * @throws IllegalStateException If the current token has no string view.
	 */
	String getString();

	/**
	 * Returns the current numeric value as a parsed {@link Number}.
	 *
	 * <p>
	 * Implementations may parse lazily; the returned {@link Number} subtype is implementation- and
	 * format-defined (e.g. {@link Long} / {@link Double} / {@link java.math.BigDecimal}).  Use
	 * {@link #getNumberLexeme()} to recover the original textual form.
	 *
	 * @return The parsed numeric value.
	 * @throws IllegalStateException If the current token is not {@link TokenType#VALUE_NUMBER}.
	 * @throws ParseException If the lexeme cannot be parsed.
	 */
	Number getNumber() throws ParseException;

	/**
	 * Returns the original numeric lexeme for the current numeric token.
	 *
	 * <p>
	 * For text formats this is the exact substring that produced the number.  For binary formats
	 * the implementation returns a canonical text rendering of the native numeric token.
	 *
	 * @return The numeric lexeme.
	 * @throws IllegalStateException If the current token is not {@link TokenType#VALUE_NUMBER}.
	 */
	String getNumberLexeme();

	/**
	 * Returns the current boolean value.
	 *
	 * @return The boolean value.
	 * @throws IllegalStateException If the current token is not {@link TokenType#VALUE_BOOLEAN}.
	 */
	boolean getBool();

	/**
	 * Returns the current binary value.
	 *
	 * <p>
	 * Binary scalars come from binary formats only (cbor, msgpack, bson, ...).  Text formats
	 * deliver embedded binary content as base64-encoded {@link TokenType#VALUE_STRING}.
	 *
	 * @return The binary value.
	 * @throws IllegalStateException If the current token is not {@link TokenType#VALUE_BINARY}.
	 */
	byte[] getBinary();

	/**
	 * Returns <jk>true</jk> if {@link #read(Class)} is currently callable on this cursor &mdash;
	 * i.e. the cursor is positioned at a value boundary.
	 *
	 * <p>
	 * This is the natural loop predicate when streaming records:
	 * <p class='bjava'>
	 * 	<jk>try</jk> (TokenReader <jv>r</jv> = Json.<jsf>DEFAULT</jsf>.parseTokens(<jv>input</jv>)) {
	 * 		<jv>r</jv>.next();  <jc>// consume the opening [ </jc>
	 * 		<jk>while</jk> (<jv>r</jv>.canRead()) {
	 * 			Item <jv>item</jv> = <jv>r</jv>.read(Item.<jk>class</jk>);
	 * 			process(<jv>item</jv>);
	 * 		}
	 * 		<jv>r</jv>.next();  <jc>// consume the closing ]</jc>
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if the next operation can be {@code read}; <jk>false</jk> if the
	 * 	cursor is positioned at a structural separator, field name, end-container, or end-of-stream.
	 * @throws IOException If a problem occurred peeking at the underlying stream.
	 * @throws ParseException If the input is malformed for the format.
	 */
	default boolean canRead() throws IOException, ParseException {
		return false;
	}

	/**
	 * Consumes the upcoming value (object, array, or scalar) and binds it to a POJO via the
	 * format's existing parser.
	 *
	 * <p>
	 * <b>Cursor positioning:</b> the cursor must be in a state where the next call to {@link #next()}
	 * would emit a value-shaped token (i.e. {@link TokenType#START_OBJECT}, {@link TokenType#START_ARRAY},
	 * or one of the {@code VALUE_*} tokens).  Calling {@code read} consumes that upcoming value
	 * wholesale and advances the cursor's depth back to where it was before the value would have
	 * started &mdash; structurally equivalent to calling {@link #next()} on a scalar, or
	 * {@link #next()} + {@link #skipChildren()} on a container.
	 *
	 * <p>
	 * <b>Do NOT call {@link #next()} before {@code read}.</b>  Once the structural opener
	 * (e.g. <c>{</c> or <c>[</c>) has been consumed by {@link #next()}, the underlying parser cannot
	 * re-bind the value.  The contract is "instead-of-next, not after-next."
	 *
	 * <p>
	 * <b>Default behavior:</b> the default implementation throws
	 * {@link UnsupportedOperationException}.  Format-specific token readers override this by
	 * delegating to their existing {@code *ParserSession} at the cursor's current input position
	 * and resyncing the cursor's state machine afterward.
	 *
	 * @param <T> The expected return type.
	 * @param type The target Java type.
	 * @return The bound POJO.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed for the format or cannot be bound to the type.
	 * @throws IllegalStateException If the cursor is not positioned at a value boundary.
	 * @throws UnsupportedOperationException If this format's reader has not implemented the bridge
	 * 	(e.g. writer-only formats with no parser, or formats whose Phase-2 increment did not include
	 * 	the bridge).
	 */
	default <T> T read(Class<T> type) throws IOException, ParseException {
		throw new UnsupportedOperationException(
			"read is not implemented for " + getClass().getName());
	}

	/**
	 * Same as {@link #read(Class)} but accepts a pre-resolved {@link ClassMeta}.
	 *
	 * @param <T> The expected return type.
	 * @param type The target type.
	 * @return The bound POJO.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed or cannot be bound to the type.
	 */
	default <T> T read(ClassMeta<T> type) throws IOException, ParseException {
		throw new UnsupportedOperationException(
			"read is not implemented for " + getClass().getName());
	}

	/**
	 * Same as {@link #read(Class)} but accepts a parameterized {@link Type} and optional type
	 * arguments (the same shape as {@link Parser#parse(Object, Type, Type...)}).
	 *
	 * @param <T> The expected return type.
	 * @param type The target type.
	 * @param args Optional type arguments for parameterized types.
	 * @return The bound POJO.
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed or cannot be bound to the type.
	 */
	default <T> T read(Type type, Type... args) throws IOException, ParseException {
		throw new UnsupportedOperationException(
			"read is not implemented for " + getClass().getName());
	}

	/**
	 * Skips over the children of the current structural token.
	 *
	 * <p>
	 * If the current token is {@link TokenType#START_OBJECT} or {@link TokenType#START_ARRAY},
	 * the cursor is advanced to the matching {@link TokenType#END_OBJECT} or
	 * {@link TokenType#END_ARRAY} (i.e. the entire sub-tree is consumed).  For any other token this
	 * is a no-op.
	 *
	 * @throws IOException If a problem occurred reading from the underlying stream.
	 * @throws ParseException If the input is malformed for the format.
	 */
	void skipChildren() throws IOException, ParseException;

	/**
	 * Structural token cursors are always O(1)-memory streamers.
	 *
	 * @return Always <jk>true</jk>.
	 */
	@Override
	default boolean isStreaming() { return true; }

	/**
	 * Closes the underlying stream.
	 *
	 * <p>
	 * Implementations should be idempotent &mdash; closing a closed reader is a no-op.
	 */
	@Override
	void close() throws IOException;
}
