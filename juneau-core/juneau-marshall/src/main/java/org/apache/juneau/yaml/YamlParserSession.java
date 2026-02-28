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
package org.apache.juneau.yaml;

import static org.apache.juneau.commons.lang.StateEnum.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.utils.Iso8601Utils;

/**
 * Session object that lives for the duration of a single use of {@link YamlParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 */
@SuppressWarnings({
	"java:S125",  // State-machine and parse-path comments are documentation, not commented-out code
	"unchecked",
	"rawtypes",
	"resource"
})
public class YamlParserSession extends ReaderParserSession {

	/**
	 * Builder class.
	 */
	public static class Builder extends ReaderParserSession.Builder {

		private YamlParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(YamlParser ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public YamlParserSession build() {
			return new YamlParserSession(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(YamlParser ctx) {
		return new Builder(ctx);
	}

	private final YamlParser ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected YamlParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override /* Overridden from ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (var r = pipe.getParserReader()) {
			if (r == null)
				return null;
			return parseAnything(type, r, getOuter(), null);
		}
	}

	@Override /* Overridden from ReaderParserSession */
	protected <E> Collection<E> doParseIntoCollection(ParserPipe pipe, Collection<E> c, Type elementType) throws IOException, ParseException, ExecutableException {
		try (var r = pipe.getParserReader()) {
			skipWhitespaceAndComments(r);
			int ch = r.peek();
			if (ch == '[') {
				parseFlowSequence(r, c, getClassMeta(elementType), null);
			} else {
				parseBlockSequence(r, c, getClassMeta(elementType), null, 0);
			}
			return c;
		}
	}

	@Override /* Overridden from ReaderParserSession */
	protected <K,V> Map<K,V> doParseIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws IOException, ParseException, ExecutableException {
		try (var r = pipe.getParserReader()) {
			skipWhitespaceAndComments(r);
			int ch = r.peek();
			if (ch == '{') {
				parseFlowMapping(r, m, (ClassMeta<K>)getClassMeta(keyType), (ClassMeta<V>)getClassMeta(valueType), null);
			} else {
				parseBlockMapping(r, m, (ClassMeta<K>)getClassMeta(keyType), (ClassMeta<V>)getClassMeta(valueType), null, 0);
			}
			return m;
		}
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for parser dispatch
	})
	private <T> T parseAnything(ClassMeta<?> eType, ParserReader r, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

		if (eType == null)
			eType = object();
		var swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		var builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (nn(builder))
			sType = builder.getBuilderClassMeta(this);
		else if (nn(swap))
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)opt(parseAnything(eType.getElementType(), r, outer, pMeta));

		setCurrentClass(sType);

		Object o = null;

		skipWhitespaceAndComments(r);
		skipDocumentMarker(r);
		skipWhitespaceAndComments(r);

		int contentColumn = r.getPosition().getColumn();
		int c = r.peek();
		if (c == -1) {
			// Let o be null.
		} else if (c == '{') {
			if (sType.isObject()) {
				var m2 = new JsonMap(this);
				parseFlowMapping(r, m2, string(), object(), pMeta);
				o = cast(m2, pMeta, eType);
			} else if (sType.isMap()) {
				Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
				o = parseFlowMapping(r, m, sType.getKeyType(), sType.getValueType(), pMeta);
			} else if (sType.isCollection()) {
				var m = new JsonMap(this);
				parseFlowMapping(r, m, string(), object(), pMeta);
				o = cast(m, pMeta, eType);
			} else if (nn(builder)) {
				var m = toBeanMap(builder.create(this, eType));
				o = builder.build(this, parseIntoBeanMap(r, m).getBean(), eType);
			} else if (sType.canCreateNewBean(outer)) {
				var m = newBeanMap(outer, sType.inner());
				o = parseIntoBeanMap(r, m).getBean();
			} else if (sType.isArray() || sType.isArgs()) {
				var m = new JsonMap(this);
				parseFlowMapping(r, m, string(), object(), pMeta);
				o = cast(m, pMeta, eType);
			} else {
				Map m = new JsonMap(this);
				parseFlowMapping(r, m, sType.getKeyType(), sType.getValueType(), pMeta);
				if (m.containsKey(getBeanTypePropertyName(eType)))
					o = cast((JsonMap)m, pMeta, eType);
				else if (nn(sType.getProxyInvocationHandler()))
					o = newBeanMap(outer, sType.inner()).load(m).getBean();
				else
					throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", cn(sType), sType.getNotABeanReason());
			}
		} else if (c == '[') {
			if (sType.isObject()) {
				o = parseFlowSequence(r, new JsonList(this), object(), pMeta);
			} else if (sType.isCollection()) {
				Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : new JsonList(this));
				o = parseFlowSequence(r, l, sType, pMeta);
			} else if (sType.isArray() || sType.isArgs()) {
				var l = (ArrayList)parseFlowSequence(r, list(), sType, pMeta);
				o = toArray(sType, l);
			} else {
				throw new ParseException(this, "Unrecognized syntax for class type ''{0}'', starting character ''{1}''", sType, (char)c);
			}
		} else if (c == '\'') {
			String s = parseSingleQuotedString(r);
			o = handleQuotedScalar(s, r, sType, eType, builder, outer, pMeta, contentColumn > 0 ? contentColumn - 1 : 0);
		} else if (c == '"') {
			String s = parseDoubleQuotedString(r);
			o = handleQuotedScalar(s, r, sType, eType, builder, outer, pMeta, contentColumn > 0 ? contentColumn - 1 : 0);
		} else if (c == '-') {
			int c2 = peekSecondChar(r);
			if (c2 == ' ' || c2 == '\n' || c2 == '\r') {
				if (sType.isObject()) {
					o = parseBlockSequence(r, new JsonList(this), object(), pMeta, 0);
				} else if (sType.isCollection()) {
					Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : new JsonList(this));
					o = parseBlockSequence(r, l, sType, pMeta, 0);
				} else if (sType.isArray() || sType.isArgs()) {
					var l = (ArrayList)parseBlockSequence(r, list(), sType, pMeta, 0);
					o = toArray(sType, l);
				} else {
					throw new ParseException(this, "Unrecognized syntax for class type ''{0}'', starting character ''{1}''", sType, (char)c);
				}
			} else {
				String s = parsePlainScalar(r, 0);
				o = convertToType(s, sType, eType, outer, pMeta);
			}
		} else if (c == '|' || c == '>') {
			String s = parseBlockScalar(r, (char)c);
			o = convertToType(s, sType, eType, outer, pMeta);
		} else if (c == '~') {
			r.read(); // consume '~'
			o = null;
		} else {
			String s = parsePlainScalar(r, 0);
			o = handlePlainScalar(s, r, sType, eType, builder, outer, pMeta, contentColumn > 0 ? contentColumn - 1 : 0);
		}

		if (nn(swap) && nn(o))
			o = unswap(swap, o, eType);

		if (nn(outer))
			setParent(eType, o, outer);

		return (T)o;
	}

	@SuppressWarnings("rawtypes")
	private <T> Object handleQuotedScalar(String s, ParserReader r, ClassMeta<?> sType, ClassMeta<?> eType, BuilderSwap<T,Object> builder, Object outer, BeanPropertyMeta pMeta, int keyIndent) throws IOException, ParseException, ExecutableException {
		if (looksLikeMappingKey(s, r)) {
			r.read(); // consume ':'
			int cp = r.peek();
			if (cp == ' ')
				r.read();

			if (sType.isObject()) {
				var m2 = new JsonMap(this);
				Object value = parseAnything(object(), r, m2, pMeta);
				String ts = trim(s);
				setName(object(), value, ts);
				m2.put(ts, value);
				parseBlockMappingRemainder(r, m2, string(), object(), pMeta, keyIndent);
				return cast(m2, pMeta, eType);
			} else if (sType.isMap()) {
				Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
				Object value = parseAnything(sType.getValueType(), r, m, pMeta);
				Object key = convertAttrToType(m, trim(s), sType.getKeyType());
				setName(sType.getValueType(), value, key);
				m.put(key, value);
				parseBlockMappingRemainder(r, m, sType.getKeyType(), sType.getValueType(), pMeta, keyIndent);
				return m;
			} else if (nn(builder)) {
				var m = toBeanMap(builder.create(this, eType));
				parseBeanProperty(r, m, s);
				parseIntoBeanMapBlockRemainder(r, m, keyIndent);
				return builder.build(this, m.getBean(), eType);
			} else if (sType.canCreateNewBean(outer)) {
				var m = newBeanMap(outer, sType.inner());
				parseBeanProperty(r, m, s);
				parseIntoBeanMapBlockRemainder(r, m, keyIndent);
				return m.getBean();
			} else {
				var m2 = new JsonMap(this);
				Object value = parseAnything(object(), r, m2, pMeta);
				m2.put(s, value);
				parseBlockMappingRemainder(r, m2, string(), object(), pMeta, keyIndent);
				if (m2.containsKey(getBeanTypePropertyName(eType)))
					return cast(m2, pMeta, eType);
				if (nn(sType.getProxyInvocationHandler()))
					return newBeanMap(outer, sType.inner()).load(m2).getBean();
				throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", cn(sType), sType.getNotABeanReason());
			}
		}
		if (sType.isObject())
			return trim(s);
		return convertToType(s, sType, eType, outer, pMeta);
	}

	@SuppressWarnings("rawtypes")
	private <T> Object handlePlainScalar(String s, ParserReader r, ClassMeta<?> sType, ClassMeta<?> eType, BuilderSwap<T,Object> builder, Object outer, BeanPropertyMeta pMeta, int keyIndent) throws IOException, ParseException, ExecutableException {
		if (s.isEmpty())
			return null;

		if ("null".equals(s))
			return null;

		if ("true".equals(s) || "false".equals(s)) {
			if (sType.isObject() || sType.isBoolean())
				return Boolean.valueOf(s);
			return convertToType(s, sType, eType, outer, pMeta);
		}

		if (looksLikeMappingKey(s, r)) {
			r.read(); // consume ':'
			int cp = r.peek();
			if (cp == ' ')
				r.read(); // consume space after ':'

			if (sType.isObject()) {
				var m2 = new JsonMap(this);
				Object value = parseAnything(object(), r, m2, pMeta);
				String ts = trim(s);
				setName(object(), value, ts);
				m2.put(ts, value);
				parseBlockMappingRemainder(r, m2, string(), object(), pMeta, keyIndent);
				return cast(m2, pMeta, eType);
			} else if (sType.isMap()) {
				Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
				Object value = parseAnything(sType.getValueType(), r, m, pMeta);
				Object key = convertAttrToType(m, trim(s), sType.getKeyType());
				setName(sType.getValueType(), value, key);
				m.put(key, value);
				parseBlockMappingRemainder(r, m, sType.getKeyType(), sType.getValueType(), pMeta, keyIndent);
				return m;
			} else if (nn(builder)) {
				var m = toBeanMap(builder.create(this, eType));
				parseBeanProperty(r, m, s);
				parseIntoBeanMapBlockRemainder(r, m, keyIndent);
				return builder.build(this, m.getBean(), eType);
			} else if (sType.canCreateNewBean(outer)) {
				var m = newBeanMap(outer, sType.inner());
				parseBeanProperty(r, m, s);
				parseIntoBeanMapBlockRemainder(r, m, keyIndent);
				return m.getBean();
			} else {
				var m2 = new JsonMap(this);
				Object value = parseAnything(object(), r, m2, pMeta);
				m2.put(s, value);
				parseBlockMappingRemainder(r, m2, string(), object(), pMeta, keyIndent);
				if (m2.containsKey(getBeanTypePropertyName(eType)))
					return cast(m2, pMeta, eType);
				if (nn(sType.getProxyInvocationHandler()))
					return newBeanMap(outer, sType.inner()).load(m2).getBean();
				throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", cn(sType), sType.getNotABeanReason());
			}
		}

		return convertToType(s, sType, eType, outer, pMeta);
	}

	private Object convertToType(String s, ClassMeta<?> sType, ClassMeta<?> eType, Object outer, BeanPropertyMeta pMeta) throws ParseException {
		if (sType.isObject()) {
			return resolveScalarType(trim(s));
		} else if (sType.isBoolean()) {
			return bool(s);
		} else if (sType.isCharSequence()) {
			return trim(s);
		} else if (sType.isChar()) {
			return parseCharacter(s);
		} else if (sType.isNumber()) {
			return StringUtils.parseNumber(s, (Class<? extends Number>)sType.inner());
		} else if (sType.isDateOrCalendarOrTemporal() || sType.isDuration()) {
			return Iso8601Utils.parse(s, sType, getTimeZone());
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			return sType.newInstanceFromString(outer, s);
		} else {
			throw new ParseException(this, "Unrecognized syntax for class type ''{0}'', value ''{1}''", sType, s);
		}
	}

	private boolean looksLikeMappingKey(String s, ParserReader r) throws IOException {
		int c = r.peek();
		if (c != ':')
			return false;
		// Peek past ':' to make sure it's followed by space, newline, or EOF (not part of value)
		return true;
	}

	private <K,V> void parseBlockMappingRemainder(ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta, int parentIndent) throws IOException, ParseException, ExecutableException {
		parseBlockMapping(r, m, keyType, valueType, pMeta, parentIndent);
	}

	private <T> void parseIntoBeanMapBlockRemainder(ParserReader r, BeanMap<T> m, int parentIndent) throws IOException, ParseException, ExecutableException {
		parseIntoBeanMapBlock(r, m, parentIndent);
	}

	private <T> void parseBeanProperty(ParserReader r, BeanMap<T> m, String currAttr) throws IOException, ParseException, ExecutableException {
		var pm = m.getPropertyMeta(currAttr);
		setCurrentProperty(pm);
		if (pm == null) {
			onUnknownProperty(currAttr, m, parseAnything(object(), r, m.getBean(false), null));
		} else {
			var cm = pm.getClassMeta();
			Object value = parseAnything(cm, r, m.getBean(false), pm);
			setName(cm, value, currAttr);
			try {
				pm.set(m, currAttr, value);
			} catch (BeanRuntimeException e) {
				onBeanSetterException(pm, e);
				throw e;
			}
		}
		setCurrentProperty(null);
	}

	private boolean isNullBlockValue(ParserReader r, int blockIndent) throws IOException {
		int c = r.peek();
		if (c == -1)
			return true;
		if (c != '\n' && c != '\r')
			return false;
		int charsRead = 0;
		while (true) {
			c = r.read();
			if (c == -1) {
				for (int j = 0; j < charsRead; j++) r.unread();
				return true;
			}
			charsRead++;
			if (c != '\n' && c != '\r' && c != ' ' && c != '\t')
				break;
		}
		int nextColumn = r.getPosition().getColumn() - 1;
		for (int j = 0; j < charsRead; j++) r.unread();
		return nextColumn <= blockIndent;
	}

	private int peekSecondChar(ParserReader r) throws IOException {
		r.read();
		int c2 = r.peek();
		r.unread();
		return c2;
	}

	// ==========================================
	// Flow mapping: { key: value, ... }
	// ==========================================
	@SuppressWarnings({
		"java:S1168",
		"java:S135",
		"java:S2583",
		"java:S3776"
	})
	private <K,V> Map<K,V> parseFlowMapping(ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

		if (keyType == null)
			keyType = (ClassMeta<K>)string();

		// S1: Looking for outer {
		// S2: Looking for key start or }
		// S3: Found key, looking for :
		// S4: Found :, looking for value
		// S5: Looking for , or }
		// S6: Found , looking for key start

		var state = S1;
		String currKey = null;
		int c = 0;
		while (c != -1) {
			c = r.read();
			if (state == S1) {
				if (c == '{')
					state = S2;
				else if (isWhitespace(c))
					continue;
				else
					break;
			} else if (state == S2) {
				if (c == '}') {
					return m;
				} else if (isWhitespace(c)) {
					continue;
				} else {
					r.unread();
					currKey = parseFlowMappingKey(r);
					state = S3;
				}
			} else if (state == S3) {
				if (c == ':')
					state = S4;
				else if (isWhitespace(c))
					continue;
			} else if (state == S4) {
				if (isWhitespace(c)) {
					continue;
				} else {
					r.unread();
					K key = convertAttrToType(m, currKey, keyType);
					V value = parseAnything(valueType, r, m, pMeta);
					setName(valueType, value, key);
					m.put(key, value);
					state = S5;
				}
			} else if (state == S5) {
				if (c == ',') {
					state = S6;
				} else if (isWhitespace(c)) {
					continue;
				} else if (c == '}') {
					return m;
				} else {
					break;
				}
			} else if (state == S6) {
				if (c == '}') {
					break;
				} else if (isWhitespace(c)) {
					continue;
				} else {
					r.unread();
					currKey = parseFlowMappingKey(r);
					state = S3;
				}
			}
		}
		if (state == S1)
			throw new ParseException(this, "Expected '{' at beginning of YAML flow mapping.");
		if (state == S2)
			throw new ParseException(this, "Could not find key in YAML flow mapping.");
		if (state == S3)
			throw new ParseException(this, "Could not find ':' following key in YAML flow mapping.");
		if (state == S4)
			throw new ParseException(this, "Expected value in YAML flow mapping.");
		if (state == S5)
			throw new ParseException(this, "Could not find '}' marking end of YAML flow mapping.");
		if (state == S6)
			throw new ParseException(this, "Unexpected '}' found in YAML flow mapping.");

		return null; // Unreachable.
	}

	private String parseFlowMappingKey(ParserReader r) throws IOException, ParseException {
		int c = r.peek();
		if (c == '\'' )
			return parseSingleQuotedString(r);
		if (c == '"')
			return parseDoubleQuotedString(r);
		return parsePlainFlowKey(r);
	}

	private String parsePlainFlowKey(ParserReader r) throws IOException {
		var sb = new StringBuilder();
		int c;
		while ((c = r.read()) != -1) {
			if (c == ':' || c == ',' || c == '}' || c == '{' || c == '[' || c == ']') {
				r.unread();
				break;
			}
			sb.append((char)c);
		}
		return sb.toString().trim();
	}

	// ==========================================
	// Flow sequence: [ value, ... ]
	// ==========================================
	@SuppressWarnings({
		"java:S1168",
		"java:S135",
		"java:S2583",
		"java:S3776"
	})
	private <E> Collection<E> parseFlowSequence(ParserReader r, Collection<E> l, ClassMeta<?> type, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

		// S1: Looking for outermost [
		// S2: Looking for value or ]
		// S3: Looking for , or ]
		// S4: Looking for value

		int argIndex = 0;

		var state = S1;
		int c = 0;
		while (c != -1) {
			c = r.read();
			if (state == S1) {
				if (c == '[')
					state = S2;
				else if (isWhitespace(c))
					continue;
				else
					break;
			} else if (state == S2) {
				if (c == ']') {
					return l;
				} else if (isWhitespace(c)) {
					continue;
				} else if (c != -1) {
					r.unread();
					l.add((E)parseAnything(type.isArgs() ? type.getArg(argIndex++) : type.getElementType(), r, l, pMeta));
					state = S3;
				}
			} else if (state == S3) {
				if (c == ',') {
					state = S4;
				} else if (isWhitespace(c)) {
					continue;
				} else if (c == ']') {
					return l;
				} else {
					break;
				}
			} else if (state == S4) {
				if (isWhitespace(c)) {
					continue;
				} else if (c == ']') {
					break;
				} else if (c != -1) {
					r.unread();
					l.add((E)parseAnything(type.isArgs() ? type.getArg(argIndex++) : type.getElementType(), r, l, pMeta));
					state = S3;
				}
			}
		}
		if (state == S1)
			throw new ParseException(this, "Expected '[' at beginning of YAML flow sequence.");
		if (state == S2)
			throw new ParseException(this, "Expected value or ']' in YAML flow sequence.");
		if (state == S3)
			throw new ParseException(this, "Expected ',' or ']' in YAML flow sequence.");
		if (state == S4)
			throw new ParseException(this, "Unexpected trailing comma in YAML flow sequence.");

		return null; // Unreachable.
	}

	// ==========================================
	// Block mapping
	// ==========================================
	private <K,V> Map<K,V> parseBlockMapping(ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta, int parentIndent) throws IOException, ParseException, ExecutableException {

		if (keyType == null)
			keyType = (ClassMeta<K>)string();

		int blockIndent = -1;

		while (true) {
			int consumed = skipBlanksAndCountIndent(r);
			int lineIndent = r.getPosition().getColumn();
			int c = r.peek();
			if (c == -1)
				break;

			if (blockIndent == -1) {
				if (lineIndent <= parentIndent && parentIndent > 0) {
					unreadSpaces(r, consumed);
					break;
				}
				blockIndent = lineIndent;
			}

			if (lineIndent < blockIndent) {
				unreadSpaces(r, consumed);
				break;
			}

			c = r.peek();
			if (c == '#') {
				skipToEndOfLine(r);
				continue;
			}

			String keyStr = trim(parseBlockMappingKey(r));

			c = r.read(); // Should be ':'
			if (c != ':')
				throw new ParseException(this, "Expected ':' after key in YAML block mapping, found ''{0}''", (char)c);

			c = r.peek();
			if (c == ' ')
				r.read();
			K key = convertAttrToType(m, keyStr, keyType);
			V value = (V)parseAnything(valueType, r, m, pMeta);
			setName(valueType, value, key);
			m.put(key, value);
		}

		return m;
	}

	private int skipBlanksAndCountIndent(ParserReader r) throws IOException {
		int indent = 0;
		int c;
		while ((c = r.read()) != -1) {
			if (c == ' ') {
				indent++;
			} else if (c == '\n' || c == '\r') {
				indent = 0;
			} else if (c == '\t') {
				indent++;
			} else {
				r.unread();
				break;
			}
		}
		return indent;
	}

	private void unreadSpaces(ParserReader r, int count) throws IOException {
		for (int i = 0; i < count; i++)
			r.unread();
	}

	private String parseBlockMappingKey(ParserReader r) throws IOException, ParseException {
		int c = r.peek();
		if (c == '\'')
			return parseSingleQuotedString(r);
		if (c == '"')
			return parseDoubleQuotedString(r);
		var sb = new StringBuilder();
		while ((c = r.read()) != -1) {
			if (c == ':') {
				int next = r.peek();
				if (next == ' ' || next == '\n' || next == '\r' || next == -1) {
					r.unread();
					break;
				}
				sb.append((char)c);
			} else if (c == '\n' || c == '\r') {
				r.unread();
				break;
			} else {
				sb.append((char)c);
			}
		}
		String key = sb.toString().trim();
		if ("null".equals(key) || "Null".equals(key) || "NULL".equals(key) || "~".equals(key))
			return null;
		return key;
	}

	// ==========================================
	// Block mapping for beans
	// ==========================================
	@SuppressWarnings({
		"java:S3776"
	})
	private <T> BeanMap<T> parseIntoBeanMap(ParserReader r, BeanMap<T> m) throws IOException, ParseException, ExecutableException {
		int c = r.peek();
		if (c == '{') {
			return parseIntoBeanMapFlow(r, m);
		}
		return parseIntoBeanMapBlock(r, m, 0);
	}

	@SuppressWarnings({
		"java:S1168",
		"java:S2583",
		"java:S3776"
	})
	private <T> BeanMap<T> parseIntoBeanMapFlow(ParserReader r, BeanMap<T> m) throws IOException, ParseException, ExecutableException {

		// S1: Looking for outer {
		// S2: Looking for attrName start.
		// S3: Found attrName end, looking for :.
		// S4: Found :, looking for valStart.
		// S5: Looking for , or }

		var state = S1;
		var currAttr = "";
		int c = 0;
		mark();
		try {
			while (c != -1) {
				c = r.read();
				if (state == S1) {
					if (c == '{')
						state = S2;
					else if (isWhitespace(c))
						continue;
					else
						break;
				} else if (state == S2) {
					if (c == '}') {
						return m;
					} else if (isWhitespace(c)) {
						continue;
					} else {
						r.unread();
						mark();
						currAttr = parseFlowMappingKey(r);
						state = S3;
					}
				} else if (state == S3) {
					if (c == ':')
						state = S4;
					else if (isWhitespace(c))
						continue;
				} else if (state == S4) {
					if (isWhitespace(c)) {
						continue;
					} else {
						if (! currAttr.equals(getBeanTypePropertyName(m.getClassMeta()))) {
							var pm = m.getPropertyMeta(currAttr);
							setCurrentProperty(pm);
							if (pm == null) {
								onUnknownProperty(currAttr, m, parseAnything(object(), r.unread(), m.getBean(false), null));
								unmark();
							} else {
								unmark();
								var cm = pm.getClassMeta();
								Object value = parseAnything(cm, r.unread(), m.getBean(false), pm);
								setName(cm, value, currAttr);
								try {
									pm.set(m, currAttr, value);
								} catch (BeanRuntimeException e) {
									onBeanSetterException(pm, e);
									throw e;
								}
							}
							setCurrentProperty(null);
						}
						state = S5;
					}
				} else if (state == S5) {
					if (c == ',')
						state = S2;
					else if (isWhitespace(c))
						continue;
					else if (c == '}') {
						return m;
					}
				}
			}
			if (state == S1)
				throw new ParseException(this, "Expected '{' at beginning of YAML flow mapping.");
			if (state == S2)
				throw new ParseException(this, "Could not find attribute name in YAML flow mapping.");
			if (state == S3)
				throw new ParseException(this, "Could not find ':' following attribute name in YAML flow mapping.");
			if (state == S4)
				throw new ParseException(this, "Expected value in YAML flow mapping.");
			if (state == S5)
				throw new ParseException(this, "Could not find '}' marking end of YAML flow mapping.");
		} finally {
			unmark();
		}

		return null; // Unreachable.
	}

	@SuppressWarnings({
		"java:S3776"
	})
	private <T> BeanMap<T> parseIntoBeanMapBlock(ParserReader r, BeanMap<T> m, int parentIndent) throws IOException, ParseException, ExecutableException {

		int blockIndent = -1;

		mark();
		try {
			while (true) {
				int consumed = skipBlanksAndCountIndent(r);
				int lineIndent = r.getPosition().getColumn();
				int c = r.peek();
				if (c == -1)
					break;

				if (blockIndent == -1) {
					if (lineIndent <= parentIndent && parentIndent > 0) {
						unreadSpaces(r, consumed);
						break;
					}
					blockIndent = lineIndent;
				}

				if (lineIndent < blockIndent) {
					unreadSpaces(r, consumed);
					break;
				}

				c = r.peek();
				if (c == '#') {
					skipToEndOfLine(r);
					continue;
				}

				String currAttr = trim(parseBlockMappingKey(r));

				c = r.read(); // Should be ':'
				if (c != ':')
					throw new ParseException(this, "Expected ':' after attribute name in YAML block mapping, found ''{0}''", (char)c);

				if (! currAttr.equals(getBeanTypePropertyName(m.getClassMeta()))) {
					var pm = m.getPropertyMeta(currAttr);
					setCurrentProperty(pm);
					if (pm == null) {
						c = r.peek();
						if (c == ' ')
							r.read();
						Object v = isNullBlockValue(r, blockIndent) ? null : parseAnything(object(), r, m.getBean(false), null);
						onUnknownProperty(currAttr, m, v);
						unmark();
					} else {
						unmark();
						var cm = pm.getClassMeta();
						c = r.peek();
						if (c == ' ')
							r.read();
						Object value = isNullBlockValue(r, blockIndent) ? null : parseAnything(cm, r, m.getBean(false), pm);
						setName(cm, value, currAttr);
						try {
							pm.set(m, currAttr, value);
						} catch (BeanRuntimeException e) {
							onBeanSetterException(pm, e);
							throw e;
						}
					}
					setCurrentProperty(null);
				}
				mark();
			}
		} finally {
			unmark();
		}

		return m;
	}

	// ==========================================
	// Block sequence: - value\n- value\n...
	// ==========================================
	private <E> Collection<E> parseBlockSequence(ParserReader r, Collection<E> l, ClassMeta<?> type, BeanPropertyMeta pMeta, int parentIndent) throws IOException, ParseException, ExecutableException {

		int blockIndent = -1;
		int argIndex = 0;

		while (true) {
			int consumed = skipBlanksAndCountIndent(r);
			int lineIndent = r.getPosition().getColumn();
			int c = r.peek();
			if (c == -1)
				break;

			if (blockIndent == -1) {
				if (lineIndent <= parentIndent && parentIndent > 0) {
					unreadSpaces(r, consumed);
					break;
				}
				blockIndent = lineIndent;
			}

			if (lineIndent < blockIndent) {
				unreadSpaces(r, consumed);
				break;
			}

			c = r.peek();
			if (c == '#') {
				skipToEndOfLine(r);
				continue;
			}
			if (c != '-') {
				unreadSpaces(r, lineIndent);
				break;
			}

			r.read(); // consume '-'
			c = r.peek();
			if (c != ' ' && c != '\n' && c != '\r') {
				unreadSpaces(r, lineIndent);
				break;
			}

			if (c == ' ')
				r.read(); // consume space after '-'

			ClassMeta<?> elementType = type.isArgs() ? type.getArg(argIndex++) : type.getElementType();
			l.add((E)parseAnything(elementType, r, l, pMeta));
		}

		return l;
	}

	// ==========================================
	// Single-quoted string: 'hello ''world'''
	// ==========================================
	private String parseSingleQuotedString(ParserReader r) throws IOException, ParseException {
		int c = r.read(); // consume opening '
		if (c != '\'')
			throw new ParseException(this, "Expected single quote to start string.");

		var sb = new StringBuilder();
		while ((c = r.read()) != -1) {
			if (c == '\'') {
				int next = r.peek();
				if (next == '\'') {
					r.read(); // consume escaped '
					sb.append('\'');
				} else {
					return sb.toString();
				}
			} else {
				sb.append((char)c);
			}
		}
		throw new ParseException(this, "Could not find end of single-quoted YAML string.");
	}

	// ==========================================
	// Double-quoted string: "hello \"world\"\n"
	// ==========================================
	@SuppressWarnings({
		"java:S3776"
	})
	private String parseDoubleQuotedString(ParserReader r) throws IOException, ParseException {
		int c = r.read(); // consume opening "
		if (c != '"')
			throw new ParseException(this, "Expected double quote to start string.");

		var sb = new StringBuilder();
		while ((c = r.read()) != -1) {
			if (c == '\\') {
				c = r.read();
				// @formatter:off
				switch (c) {
					case '\\': sb.append('\\'); break;
					case '"': sb.append('"'); break;
					case 'n': sb.append('\n'); break;
					case 't': sb.append('\t'); break;
					case 'r': sb.append('\r'); break;
					case '0': sb.append('\0'); break;
					case 'a': sb.append('\u0007'); break;
					case 'b': sb.append('\b'); break;
					case 'f': sb.append('\f'); break;
					case 'e': sb.append('\u001b'); break;
					case '/': sb.append('/'); break;
					case 'x': {
						String hex = r.read(2);
						try {
							sb.append((char)Integer.parseInt(hex, 16));
						} catch (@SuppressWarnings("unused") NumberFormatException e) {
							throw new ParseException(this, "Invalid \\x escape sequence in YAML string.");
						}
						break;
					}
					case 'u': {
						String hex = r.read(4);
						try {
							sb.append((char)Integer.parseInt(hex, 16));
						} catch (@SuppressWarnings("unused") NumberFormatException e) {
							throw new ParseException(this, "Invalid \\u escape sequence in YAML string.");
						}
						break;
					}
					default:
						throw new ParseException(this, "Invalid escape sequence '\\{0}' in YAML string.", (char)c);
				}
				// @formatter:on
			} else if (c == '"') {
				return sb.toString();
			} else {
				sb.append((char)c);
			}
		}
		throw new ParseException(this, "Could not find end of double-quoted YAML string.");
	}

	// ==========================================
	// Plain scalar (unquoted)
	// ==========================================
	private String parsePlainScalar(ParserReader r, int indent) throws IOException {
		var sb = new StringBuilder();
		int c;
		while ((c = r.read()) != -1) {
			if (c == '\n' || c == '\r') {
				r.unread();
				break;
			}
			if (c == ':') {
				int next = r.peek();
				if (next == ' ' || next == '\n' || next == '\r' || next == -1) {
					r.unread(); // unread ':'
					break;
				}
				sb.append((char)c);
			} else if (c == ' ') {
				int next = r.peek();
				if (next == '#') {
					// rest of line is comment
					r.unread();
					break;
				}
				sb.append((char)c);
			} else if (c == ',' || c == '}' || c == ']') {
				r.unread();
				break;
			} else if (c == '#' && sb.isEmpty()) {
				r.unread();
				break;
			} else {
				sb.append((char)c);
			}
		}
		String result = sb.toString();
		// Trim trailing whitespace
		int end = result.length();
		while (end > 0 && result.charAt(end - 1) == ' ')
			end--;
		return result.substring(0, end);
	}

	// ==========================================
	// Block scalar: | or >
	// ==========================================
	@SuppressWarnings({
		"java:S3776"
	})
	private String parseBlockScalar(ParserReader r, char indicator) throws IOException, ParseException {
		r.read(); // consume '|' or '>'

		// Parse indicator line for chomping
		char chomping = ' '; // default = clip
		int c;
		while ((c = r.read()) != -1) {
			if (c == '-') {
				chomping = '-'; // strip
			} else if (c == '+') {
				chomping = '+'; // keep
			} else if (c == '\n' || c == '\r') {
				if (c == '\r') {
					int next = r.peek();
					if (next == '\n')
						r.read();
				}
				break;
			} else if (c >= '1' && c <= '9') {
				// explicit indent indicator, ignore for now (auto-detect below)
			} else if (c == ' ' || c == '\t') {
				continue;
			} else {
				throw new ParseException(this, "Unexpected character ''{0}'' in YAML block scalar indicator.", (char)c);
			}
		}

		// Read the block content
		int blockIndent = -1;
		var lines = new ArrayList<String>();
		int trailingNewlines = 0;

		while (true) {
			// Count leading spaces
			int spaces = 0;
			while ((c = r.read()) != -1) {
				if (c == ' ')
					spaces++;
				else
					break;
			}

			if (c == -1) {
				break;
			}

			if (c == '\n' || c == '\r') {
				// Empty line
				trailingNewlines++;
				lines.add("");
				if (c == '\r' && r.peek() == '\n')
					r.read();
				continue;
			}

			if (blockIndent == -1) {
				blockIndent = spaces;
			}

			if (spaces < blockIndent) {
				// End of block, push back everything
				r.unread(); // push back non-space char
				for (int i = 0; i < spaces; i++)
					r.unread();
				break;
			}

			// Read rest of line
			var lineSb = new StringBuilder();
			// Include extra spaces beyond blockIndent
			for (int i = blockIndent; i < spaces; i++)
				lineSb.append(' ');
			lineSb.append((char)c);
			while ((c = r.read()) != -1) {
				if (c == '\n' || c == '\r') {
					if (c == '\r' && r.peek() == '\n')
						r.read();
					break;
				}
				lineSb.append((char)c);
			}

			// Account for any blank lines that preceded this content line
			for (int i = 0; i < trailingNewlines; i++)
				lines.add("");
			trailingNewlines = 0;
			lines.add(lineSb.toString());

			if (c == -1)
				break;
		}

		// Build the result based on indicator and chomping
		var sb = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (indicator == '|') {
				// Literal: preserve newlines
				if (i > 0)
					sb.append('\n');
				sb.append(line);
			} else {
				// Folded: single newlines -> space, multiple newlines preserved
				if (i > 0) {
					if (line.isEmpty() || lines.get(i - 1).isEmpty()) {
						sb.append('\n');
					} else {
						sb.append(' ');
					}
				}
				sb.append(line);
			}
		}

		// Apply chomping
		if (chomping == '-') {
			// strip: no trailing newline
		} else if (chomping == '+') {
			// keep: preserve all trailing newlines
			sb.append('\n');
			for (int i = 0; i < trailingNewlines; i++)
				sb.append('\n');
		} else {
			// clip (default): single trailing newline
			sb.append('\n');
		}

		return sb.toString();
	}

	// ==========================================
	// Helper methods
	// ==========================================

	private void skipWhitespaceAndComments(ParserReader r) throws IOException {
		int c;
		while ((c = r.read()) != -1) {
			if (c == '#') {
				// Skip to end of line
				while ((c = r.read()) != -1) {
					if (c == '\n' || c == '\r')
						break;
				}
			} else if (!isWhitespace(c)) {
				r.unread();
				return;
			}
		}
	}

	private void skipDocumentMarker(ParserReader r) throws IOException {
		int c = r.peek();
		if (c == '-') {
			r.read();
			int c2 = r.peek();
			if (c2 == '-') {
				r.read();
				int c3 = r.peek();
				if (c3 == '-') {
					r.read();
					// '---' found, skip rest of line
					skipToEndOfLine(r);
					return;
				}
				r.unread(); // unread second '-'
			}
			r.unread(); // unread first '-'
		} else if (c == '.') {
			r.read();
			int c2 = r.peek();
			if (c2 == '.') {
				r.read();
				int c3 = r.peek();
				if (c3 == '.') {
					r.read();
					// '...' found, skip rest of line
					skipToEndOfLine(r);
					return;
				}
				r.unread();
			}
			r.unread();
		}
	}

	private void skipToEndOfLine(ParserReader r) throws IOException {
		int c;
		while ((c = r.read()) != -1) {
			if (c == '\n' || c == '\r')
				return;
		}
	}

	private boolean isWhitespace(int c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r';
	}

	private Object resolveScalarType(String s) {
		if (s == null || "null".equals(s) || "~".equals(s) || s.isEmpty())
			return null;
		if ("true".equals(s) || "True".equals(s) || "TRUE".equals(s))
			return Boolean.TRUE;
		if ("false".equals(s) || "False".equals(s) || "FALSE".equals(s))
			return Boolean.FALSE;

		// Try parsing as number
		if (!s.isEmpty()) {
			char first = s.charAt(0);
			if (first == '-' || first == '+' || (first >= '0' && first <= '9') || first == '.') {
				try {
					if (s.contains(".") || s.contains("e") || s.contains("E")) {
						Double d = Double.parseDouble(s);
						if (!d.isInfinite() && !d.isNaN())
							return d;
					} else {
						try {
							return Integer.parseInt(s);
						} catch (@SuppressWarnings("unused") NumberFormatException e2) {
							try {
								return Long.parseLong(s);
							} catch (@SuppressWarnings("unused") NumberFormatException e3) {
								// Fall through to string
							}
						}
					}
				} catch (@SuppressWarnings("unused") NumberFormatException e) {
					// Fall through to string
				}
			}
		}

		return s;
	}
}
