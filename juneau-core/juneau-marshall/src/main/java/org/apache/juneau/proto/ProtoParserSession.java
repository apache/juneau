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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.Map.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.utils.Iso8601Utils;
import org.apache.juneau.commons.bean.BeanMap;

/**
 * Session for parsing Protobuf Text Format into POJOs.
 */
@SuppressWarnings({
	"rawtypes", // Raw types necessary for generic Map/List handling
	"unchecked", // Type erasure requires unchecked casts in convertValue
	"java:S3776", // Cognitive complexity acceptable for parseMessage
	"java:S6541", // Brain method acceptable for parseMessage
	"java:S135", // Multiple breaks acceptable in parse loop
	"java:S115"   // ARG_ prefix follows framework convention
})
public class ProtoParserSession extends ReaderParserSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for Proto parser session.
	 */
	public static class Builder extends ReaderParserSession.Builder<Builder> {

		protected Builder(ProtoParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public ProtoParserSession build() {
			return new ProtoParserSession(this);
		}
	}

	/**
	 * Creates a new builder.
	 *
	 * @param ctx The parser context.
	 * @return A new builder.
	 */
	public static Builder create(ProtoParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	protected ProtoParserSession(Builder builder) {
		super(builder);
	}

	@Override
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (Reader r = pipe.getParserReader()) {
			if (r == null)
				return null;
			var t = new ProtoTokenizer(r);
			Map<String, Object> root = parseMessage(t, false);
			if (isEmpty(root))
				return type.canCreateNewBean(getOuter()) ? type.newInstance(getOuter()) : null;
			if (root.size() == 1 && root.containsKey("_value")) {
				var raw = root.get("_value");
				// Bug #12 (top-level only): when the root payload is a String-shaped byte[], consult
				// the configured BinaryFormat's variant BinarySwap before falling through to convertValue's
				// default String → byte[] UTF-8 coercion.  Collection-element route is intentionally not
				// touched here — that surface is Bug #11 (Proto's repeated bytes field).
				if (raw instanceof String s && type != null && type.inner() == byte[].class) {
					var swap = type.getSwap(this);
					if (swap != null)
						return (T) unswap(swap, s, type);
				}
				return (T) convertValue(raw, type);
			}
			return convertMapToType(root, type);
		}
	}

	private Map<String, Object> parseMessage(ProtoTokenizer t, boolean requireBraces) throws IOException, ParseException {
		ProtoToken.TokenType closeBrace = null;
		if (requireBraces) {
			var tok = t.read();
			if (tok.type() == ProtoToken.TokenType.LBRACE)
				closeBrace = ProtoToken.TokenType.RBRACE;
			else if (tok.type() == ProtoToken.TokenType.LANGLE)
				closeBrace = ProtoToken.TokenType.RANGLE;
			else
				return new LinkedHashMap<>();
		}

		var result = new LinkedHashMap<String, Object>();

		while (true) {
			t.skipWhitespaceAndComments();
			var t2 = t.peek();
			if (t2.type() == ProtoToken.TokenType.EOF)
				break;
			if (closeBrace != null && t2.type() == closeBrace) {
				t.read();
				break;
			}

			var fieldName = readFieldName(t);
			if (isEmpty(fieldName))
				break;

			t.skipWhitespaceAndComments();
			t2 = t.peek();

			Object value;
			if (t2.type() == ProtoToken.TokenType.COLON) {
				t.read();
				t.skipWhitespaceAndComments();
				t2 = t.peek();
				if (t2.type() == ProtoToken.TokenType.LBRACKET) {
					value = parseList(t);
				} else if (t2.type() == ProtoToken.TokenType.LBRACE || t2.type() == ProtoToken.TokenType.LANGLE) {
					value = parseMessage(t, true);
				} else {
					value = parseScalarValue(t);
				}
			} else if (t2.type() == ProtoToken.TokenType.LBRACE || t2.type() == ProtoToken.TokenType.LANGLE) {
				value = parseMessage(t, true);
			} else {
				throw t.parseException("Expected ':' or '{' after field name");
			}

			if (result.containsKey(fieldName)) {
				var existing = result.get(fieldName);
				if (!(existing instanceof List)) {
					var list = new ArrayList<>();
					list.add(existing);
					result.put(fieldName, list);
					existing = list;
				}
				((List) existing).add(value);
			} else {
				result.put(fieldName, value);
			}

			t.skipWhitespaceAndComments();
			t2 = t.peek();
			if (t2.type() == ProtoToken.TokenType.SEMICOLON || t2.type() == ProtoToken.TokenType.COMMA)
				t.read();
		}
		return result;
	}

	private static String readFieldName(ProtoTokenizer t) throws IOException, ParseException {
		var tok = t.peek();
		if (tok.type() == ProtoToken.TokenType.IDENT)
			return t.read().stringValue();
		if (tok.type() == ProtoToken.TokenType.STRING)
			return t.read().stringValue();
		// Bare integer / hex / octal field tags (e.g. {@code 0: "first"}).  Required for the
		// Bug #7b residual fix on the serialize side, which emits numeric Map<K,V> keys as bare
		// integer tags rather than quoted strings.  The string-form of the numeric value is the
		// field-name; downstream key-type coercion in convertMapToType / convertValue handles the
		// conversion back to the bean property's declared key type.
		if (tok.type() == ProtoToken.TokenType.DEC_INT
			|| tok.type() == ProtoToken.TokenType.HEX_INT
			|| tok.type() == ProtoToken.TokenType.OCT_INT)
			return Long.toString(t.read().numberValue().longValue());
		return null;
	}

	private List<Object> parseList(ProtoTokenizer t) throws IOException, ParseException {
		var tok = t.read();
		if (tok.type() != ProtoToken.TokenType.LBRACKET)
			throw t.parseException("Expected '['");
		var list = new ArrayList<>();
		t.skipWhitespaceAndComments();
		tok = t.peek();
		if (tok.type() == ProtoToken.TokenType.RBRACKET) {
			t.read();
			return list;
		}
		while (true) {
			list.add(parseScalarOrMessageInList(t));
			t.skipWhitespaceAndComments();
			tok = t.peek();
			if (tok.type() == ProtoToken.TokenType.RBRACKET) {
				t.read();
				break;
			}
			if (tok.type() == ProtoToken.TokenType.COMMA) {
				t.read();
				t.skipWhitespaceAndComments();
			} else {
				throw t.parseException("Expected ',' or ']'");
			}
		}
		return list;
	}

	private Object parseScalarOrMessageInList(ProtoTokenizer t) throws IOException, ParseException {
		var tok = t.peek();
		if (tok.type() == ProtoToken.TokenType.LBRACE || tok.type() == ProtoToken.TokenType.LANGLE)
			return parseMessage(t, true);
		return parseScalarValue(t);
	}

	private static Object parseScalarValue(ProtoTokenizer t) throws IOException, ParseException {
		var tok = t.read();
		if (tok.type() == ProtoToken.TokenType.STRING) {
			var sb = new StringBuilder(tok.stringValue());
			while (true) {
				t.skipWhitespaceAndComments();
				var next = t.peek();
				if (next.type() != ProtoToken.TokenType.STRING)
					break;
				t.read();
				sb.append(next.stringValue());
			}
			return sb.toString();
		}
		return switch (tok.type()) {
			case DEC_INT, OCT_INT, HEX_INT -> tok.numberValue();
			case FLOAT -> tok.numberValue().doubleValue();
			case IDENT -> {
				var s = tok.stringValue();
				if (s == null) yield null;
				var lower = s.toLowerCase();
				if (lower.equals("true") || lower.equals("t") || lower.equals("1"))
					yield Boolean.TRUE;
				if (lower.equals("false") || lower.equals("f") || lower.equals("0"))
					yield Boolean.FALSE;
				if (lower.equals("inf") || lower.equals("infinity"))
					yield Double.POSITIVE_INFINITY;
				if (lower.equals("-inf"))
					yield Double.NEGATIVE_INFINITY;
				if (lower.equals("nan"))
					yield Double.NaN;
				yield s;
			}
			default -> throw t.parseException("Expected scalar value, got " + tok.type());
		};
	}

	private <T> T convertMapToType(Map<String, Object> map, ClassMeta<T> type) throws ParseException, ExecutableException {
		if (type.isMap()) {
			var inner = type.inner();
			if (inner != null && JsonMap.class.isAssignableFrom(inner)) {
				if (map instanceof JsonMap jm)
					return (T) jm;
				return (T) toJsonMap(map);
			}
			// Coerce keys when the declared key type isn't String/Object (Bug #7b).
			var keyType = type.getKeyType();
			if (keyType != null && !keyType.isObject() && !keyType.isString())
				return convertToMemberType(null, map, type);
			return (T) map;
		}
		var bm = toBeanMap(type.newInstance(getOuter()));
		populateBeanMap(bm, map);
		return bm.getBean();
	}

	private JsonMap toJsonMap(Map<?, ?> map) throws ParseException, ExecutableException {
		var jm = new JsonMap();
		for (Entry<?, ?> e : map.entrySet()) {
			var k = e.getKey();
			var v = e.getValue();
			Object converted = v instanceof Map m ? toJsonMap(m) : convertValue(v, object());
			jm.put(k == null ? "null" : k.toString(), converted);
		}
		return jm;
	}

	private void populateBeanMap(BeanMap<?> bm, Map<String, Object> map) throws ParseException, ExecutableException {
		var typePropName = getBeanTypePropertyName((ClassMeta<?>) bm.getBeanInfo());
		for (Entry<String, Object> e : map.entrySet()) {
			var key = e.getKey();
			if (key.equals(typePropName))
				continue;
			var val = e.getValue();
			var pMeta = bm.getMeta().getProperties().get(key);
			if (pMeta == null && isIgnoreUnknownBeanProperties())
				continue;
			if (pMeta == null)
				throw new ParseException(this, "Unknown property ''{0}''", key);
			var targetType = (ClassMeta<?>) pMeta.getBeanInfo();
			var converted = convertValue(val, targetType);
			bm.put(key, converted);
		}
	}

	private Object convertValue(Object val, ClassMeta<?> targetType) throws ParseException, ExecutableException {
		if (val == null)
			return null;
		if (val instanceof Map val2) {
			if (JsonMap.class.isAssignableFrom(targetType.inner()))
				return toJsonMap(val2);
			if (targetType.isBean()) {
				var typeName = (String) val2.get(getBeanTypePropertyName(targetType));
				var beanType = targetType;
				if (!isEmpty(typeName)) {
					var resolved = getClassMeta(typeName, null, targetType);
					if (resolved != null)
						beanType = resolved;
				}
				var child = toBeanMap(beanType.newInstance(getOuter()));
				populateBeanMap(child, val2);
				return child.getBean();
			}
			if (targetType.isMap()) {
				// When the bean property's declared key type is non-String (e.g. Map<TestEnum,String>),
				// route through the converter's Map→Map path so keys are coerced to the declared type.
				// toJsonMap unconditionally toString-keys the entries which loses the enum/typed key (Bug #7b).
				var keyType = targetType.getKeyType();
				if (keyType != null && !keyType.isObject() && !keyType.isString())
					return convertToMemberType(null, val2, targetType);
				return toJsonMap(val2);
			}
		}
		if (val instanceof List val2 && targetType.isCollectionOrArray()) {
			var elType = targetType.getElementType();
			var result = new ArrayList<>();
			for (Object item : val2)
				result.add(convertValue(item, elType));
			return targetType.isArray() ? toArray(targetType, result) : result;
		}
		if (val instanceof Number val2) {
			if (targetType.isNumber())
				return convertToMemberType(null, val2, targetType);
			if (targetType.isDateOrCalendarOrTemporal())
				return Iso8601Utils.fromEpochMillis(val2.longValue(), targetType, getTimeZone());
		}
		if (val instanceof CharSequence val2) {
			if (targetType.isDate())
				return parseDate(val2.toString(), targetType);
			if (targetType.isCalendar())
				return parseCalendar(val2.toString(), targetType);
			if (targetType.isTemporal())
				return parseTemporal(val2.toString(), targetType);
			if (targetType.isDuration())
				return parseDuration(val2.toString());
			if (targetType.isPeriod())
				return parsePeriod(val2.toString());
		}
		// String → byte[] dispatch at the collection-element / map-value / top-level call site.
		// Mirrors the Bug #12 top-level fix in doParse but for the collection-element path that
		// was intentionally carved out of that turn.  Two routes:
		//   (1) non-NOT_SET: consult the variant BinarySwap installed on byte[].class via
		//       targetType.getSwap(this); the wire is a hex / base64 string emitted by the
		//       swap's swap() on the serializer side.
		//   (2) NOT_SET: BinarySwap.match returns 0 so no swap fires.  The Proto serializer's
		//       bytesValue path emits each byte as a hex-escaped char ("\xFF" etc.); the proto
		//       tokenizer decodes those escapes back into a Java String where each char's code
		//       point equals the byte's unsigned value.  Reconstruct byte[] by iterating chars.
		if (val instanceof String s && targetType != null && targetType.inner() == byte[].class) {
			var swap = targetType.getSwap(this);
			if (swap != null)
				return unswap(swap, s, targetType);
			var bytes = new byte[s.length()];
			for (var i = 0; i < s.length(); i++)
				bytes[i] = (byte) s.charAt(i);
			return bytes;
		}
		return convertToMemberType(null, val, targetType);
	}
}
