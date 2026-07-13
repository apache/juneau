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
package org.apache.juneau.marshall.hjson;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session for parsing Hjson format into POJOs.
 */
@SuppressWarnings({
	"java:S110",  // Inheritance depth acceptable for parser session hierarchy
	"java:S115",  // ARG_ctx follows project assertion-param naming convention
	"java:S3776", // Cognitive complexity acceptable for Hjson parse logic
	"java:S6541", // Acceptable for session implementation
	"unchecked",  // (T) casts in doParse for generic return type
	"resource"    // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class HjsonParserSession extends ReaderParserSession implements RecordReadable {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for Hjson parser session.
	 */
	public static class Builder extends ReaderParserSession.Builder<Builder> {

		protected Builder(HjsonParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public HjsonParserSession build() {
			return new HjsonParserSession(this);
		}
	}

	/**
	 * Creates a session builder.
	 *
	 * @param ctx The parser context.
	 * @return The builder.
	 */
	public static Builder create(HjsonParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	protected HjsonParserSession(Builder builder) {
		super(builder);
	}

	@Override /* RecordReadable */
	public RecordReader parseRecords(Object input) throws IOException {
		return RecordAdapter.reader(this, input);
	}

	@Override /* RecordReadable */
	public boolean isRecordStreaming() {
		return false;
	}

	@Override
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		String inputStr;
		try {
			inputStr = pipe.asString();
		} catch (IOException e) {
			throw new ParseException(e);
		}
		if (inputStr == null)
			return null;
		var trimmed = inputStr.trim();
		if (trimmed.isEmpty())
			return null;
		var tokenizer = new HjsonTokenizer(new StringReader(inputStr));
		tokenizer.skipWhitespaceAndComments();
		var peek = tokenizer.peek();
		if (peek.type() == HjsonTokenizer.TokenType.EOF)
			return null;
		var peekType = peek.type();
		// LBRACE/LBRACKET -> parseValue (object/array)
		if (peekType == HjsonTokenizer.TokenType.LBRACE || peekType == HjsonTokenizer.TokenType.LBRACKET) {
			return (T) parseValue(tokenizer, type);
		}
		// QUOTELESS/STRING: distinguish braceless root "key: value" from lone value "string"
		if (peekType == HjsonTokenizer.TokenType.QUOTELESS || peekType == HjsonTokenizer.TokenType.STRING) {
			var firstToken = tokenizer.read();
			var firstVal = firstToken.stringValue();
			tokenizer.skipWhitespaceAndComments();
			var nextType = tokenizer.peek().type();
			// Target is String/number/boolean/etc - always lone value (not key:value map)
			if (!type.isMap() && !type.isBean())
				return (T) coerceMemberValue(firstVal, type);
			// Target is Map/Bean: COLON means key:value (braceless root)
			if (nextType == HjsonTokenizer.TokenType.COLON)
				return (T) parseRootBraceless(tokenizer, type, firstVal);
			// No COLON - lone value (e.g. "1.0" or "true" tokenized as QUOTELESS)
			return (T) coerceMemberValue(firstVal, type);
		}
		// NUMBER, TRUE, FALSE, NULL, etc - lone value; use (T) not type.cast (cast fails for boxed->primitive)
		return (T) coerceMemberValue(parseValue(tokenizer, type), type);
	}

	private Object parseRootBraceless(HjsonTokenizer t, ClassMeta<?> type, String firstKey) throws IOException, ParseException, ExecutableException {
		var result = newGenericMap();
		// Same per-property type threading as parseObject so braceless-root beans get typed-map keys (Bug #7b).
		var beanMeta = (type != null && type.isBean()) ? type.getBeanMeta() : null;
		if (t.read().type() != HjsonTokenizer.TokenType.COLON)
			throw new ParseException(this, "Expected ':' after key at line {0}", t.getLine());
		t.skipWhitespaceAndComments();
		var value = parseValue(t, propertyType(beanMeta, firstKey, type));
		result.put(firstKey, value);
		t.skipWhitespaceAndComments();
		if (t.peek().type() == HjsonTokenizer.TokenType.NEWLINE)
			t.read();
		while (t.peek().type() != HjsonTokenizer.TokenType.EOF) {
			var key = readKey(t);
			t.skipWhitespaceAndComments();
			if (t.read().type() != HjsonTokenizer.TokenType.COLON)
				throw new ParseException(this, "Expected ':' after key at line {0}", t.getLine());
			t.skipWhitespaceAndComments();
			value = parseValue(t, propertyType(beanMeta, key, type));
			result.put(key, value);
			t.skipWhitespaceAndComments();
			if (t.peek().type() == HjsonTokenizer.TokenType.NEWLINE)
				t.read();
		}
		return convertToBean(result, type);
	}

	private Object parseValue(HjsonTokenizer t, ClassMeta<?> type) throws IOException, ParseException, ExecutableException {
		var tok = t.read();
		Object raw = switch (tok.type()) {
			case STRING, QUOTELESS, MULTILINE -> isTrimStrings() ? tok.stringValue().trim() : tok.stringValue();
			case NUMBER -> tok.numberValue();
			case TRUE -> true;
			case FALSE -> false;
			case NULL -> null;
			case LBRACE -> parseObject(t, type);
			case LBRACKET -> parseArray(t, type);
			case EOF -> null;
			default -> throw new ParseException(this, "Unexpected token {0} at line {1}", tok.type(), t.getLine());
		};
		if (raw == null)
			return null;
		if (tok.type() == HjsonTokenizer.TokenType.LBRACE || tok.type() == HjsonTokenizer.TokenType.LBRACKET)
			return raw;
		return coerceMemberValue(raw, type);
	}

	private Object parseObject(HjsonTokenizer t, ClassMeta<?> type) throws IOException, ParseException, ExecutableException {
		var result = newGenericMap();
		// When parsing a typed Bean, look up per-property ClassMetas so that nested Map<K,V> properties
		// see their declared key/value types and the key coercion in the inner parseObject can fire
		// (matches JsonParserSession.parseIntoBeanMap2 / parseIntoMap2 pattern).
		var beanMeta = (type != null && type.isBean()) ? type.getBeanMeta() : null;
		t.skipWhitespaceAndComments();
		var next = t.peek();
		if (next.type() == HjsonTokenizer.TokenType.RBRACE) {
			t.read();
			return convertToBean(result, type);
		}
		while (true) {
			var key = readKey(t);
			t.skipWhitespaceAndComments();
			if (t.read().type() != HjsonTokenizer.TokenType.COLON)
				throw new ParseException(this, "Expected ':' after key at line {0}", t.getLine());
			t.skipWhitespaceAndComments();
			var valueType = propertyType(beanMeta, key, type);
			var value = parseValue(t, valueType);
			result.put(key, value);
			t.skipWhitespaceAndComments();
			next = t.peek();
			if (next.type() == HjsonTokenizer.TokenType.RBRACE) {
				t.read();
				break;
			}
			if (next.type() == HjsonTokenizer.TokenType.COMMA || next.type() == HjsonTokenizer.TokenType.NEWLINE) {
				t.read();
				t.skipWhitespaceAndComments();
			}
		}
		return convertToBean(result, type);
	}

	/**
	 * Coerces a parsed scalar value to the expected type, honoring session-aware default swaps for
	 * {@code byte[]} targets at the collection-element / top-level dispatch sites (Bug #12).
	 *
	 * <p>For all other targets this is a transparent wrapper over {@link #convertToMemberType} so the
	 * existing dispatch behavior is preserved.  The narrow {@code byte[]} hook lets the configured
	 * {@link BinaryFormat}'s variant {@link org.apache.juneau.marshall.swaps.BinarySwap} unswap the wire-form
	 * string back into raw bytes — these dispatch sites don't go through
	 * {@code MarshalledPropertyPostProcessor}'s per-property swap install, so without this they fall
	 * through to {@code BasicConverter}'s default {@code String → byte[]} UTF-8 coercion.
	 */
	private Object coerceMemberValue(Object raw, ClassMeta<?> type) throws ParseException {
		if (raw instanceof String s && type != null && type.inner() == byte[].class) {
			var swap = type.getSwap(this);
			if (swap != null)
				return unswap(swap, s, type);
		}
		return convertToMemberType(null, raw, type);
	}

	/**
	 * Resolves the {@link ClassMeta} to parse a bean-property value against, falling back to {@code object()}
	 * when the enclosing type isn't a bean or the key isn't a known property.  Threading the property type
	 * lets typed-map properties (e.g. {@code Map<TestEnum,String>}) coerce their keys via the converter's
	 * {@code Map → Map} path (Bug #7b).
	 *
	 * <p>
	 * Extended in Bug #12 to also surface {@link ClassMeta#isCollectionOrArray()} property types so the
	 * collection-element dispatch in {@link #parseArray} threads the parent's element type into recursion —
	 * that's where {@link #coerceMemberValue} reads the {@code byte[]} target and invokes the configured
	 * {@link org.apache.juneau.marshall.swaps.BinarySwap}'s unswap on the wire-form string.  Other property shapes
	 * (Optional, primitive, bean) still pass through as {@code object()} so this parser's lazy-typed
	 * conversion path keeps working unchanged.
	 */
	private ClassMeta<?> propertyType(BeanMeta<?> beanMeta, String key, ClassMeta<?> enclosingType) {
		if (beanMeta != null && key != null && !key.equals(getBeanTypePropertyName(enclosingType))) {
			var pMeta = beanMeta.getPropertyMeta(key);
			if (pMeta != null) {
				var cm = (ClassMeta<?>) pMeta.getBeanInfo();
				if (cm != null && (cm.isMap() || cm.isCollectionOrArray()))
					return cm;
			}
		}
		return object();
	}

	private String readKey(HjsonTokenizer t) throws IOException {
		var tok = t.read();
		return switch (tok.type()) {
			case STRING, QUOTELESS, MULTILINE -> isTrimStrings() ? tok.stringValue().trim() : tok.stringValue();
			case NULL -> null;  // null keyword used as key -> preserve as null (matches JSON parser behavior)
			case TRUE -> "true";
			case FALSE -> "false";
			case NUMBER -> tok.numberValue().toString();
			default -> throw new ParseException(this, "Expected key at line {0}", t.getLine());
		};
	}

	private Object parseArray(HjsonTokenizer t, ClassMeta<?> type) throws IOException, ParseException, ExecutableException {
		if (type == null)
			type = object();
		var result = new JsonList();
		t.skipWhitespaceAndComments();
		var next = t.peek();
		if (next.type() == HjsonTokenizer.TokenType.RBRACKET) {
			t.read();
			return convertToCollection(result, type);
		}
		while (true) {
			var eType = coalesce(type.getElementType(), object());
			result.add(parseValue(t, eType));
			t.skipWhitespaceAndComments();
			next = t.peek();
			if (next.type() == HjsonTokenizer.TokenType.RBRACKET) {
				t.read();
				break;
			}
			if (next.type() == HjsonTokenizer.TokenType.COMMA || next.type() == HjsonTokenizer.TokenType.NEWLINE) {
				t.read();
				t.skipWhitespaceAndComments();
			}
		}
		return convertToCollection(result, type);
	}

	private Object convertToBean(MarshalledMap map, ClassMeta<?> type) throws ExecutableException, ParseException {
		if (type == null)
			type = object();
		var casted = cast(map, null, type);
		if (casted != map)
			return casted;
		if (!type.isObject()) {
			// If target type has a swap (e.g. surrogate D1→D2), parse JsonMap as swap class first, then unswap.
			var swap = type.getSwap(this);
			if (nn(swap)) {
				var sType = swap.getSwapClassMeta(this);
				var swapped = convertToMemberType(null, map, sType);
				return unswap(swap, swapped, type);
			}
			var result = convertToMemberType(null, map, type);
			if (type.isBean() && result != null)
				injectAnnotations(map, result);
			return result;
		}
		return map;
	}

	private void injectAnnotations(MarshalledMap map, Object bean) throws ExecutableException {
		var bm = toBeanMap(bean);
		for (var entry : map.entrySet()) {
			var key = entry.getKey();
			var pm = bm.getPropertyMeta(key);
			var val = (pm == null) ? null : getBeanValueSafely(bm, key);
			if (pm == null || val == null)
				continue;
			var cm = (ClassMeta<?>) pm.getBeanInfo();
			if (cm.getNameProperty() != null)
				setName(cm, val, key);
			if (cm.getParentProperty() != null)
				setParent(cm, val, bean);
			if (cm.isMap() && val instanceof Map<?,?> val2 && !cm.getValueType().isObject() && cm.getValueType().getNameProperty() != null) {
				var valueType = cm.getValueType();
				for (Map.Entry<?, ?> e : ((Map<?,?>)val2).entrySet())
					setName(valueType, e.getValue(), e.getKey());
			}
		}
	}

	private static Object getBeanValueSafely(BeanMap<?> bm, String key) {
		try {
			return bm.get(key);
		} catch (@SuppressWarnings("unused") Exception e) {
			return null;
		}
	}

	private Object convertToCollection(MarshalledList list, ClassMeta<?> type) throws ExecutableException {
		if (type == null)
			type = object();
		var eType = coalesce(type.getElementType(), object());
		var converted = newGenericList();
		for (var item : list)
			converted.add(convertToMemberType(null, item, eType));
		// For array types, convert to the appropriate array
		if (type.inner() != null && type.inner().isArray())
			return toArray(type, converted);
		// For specific collection subtypes (e.g. Vector, LinkedList), convert via MarshallingSession
		if (!type.isObject())
			return convertToMemberType(null, converted, type);
		return converted;
	}
}
