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
package org.apache.juneau.bson;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link BsonParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"rawtypes",
	"unchecked",
	"java:S115",
	"java:S125",  // Explanatory comments contain BSON/JSON syntax (e.g. {"value":x}) that Sonar misreads as commented-out code
	"java:S3776",
	"java:S6541"
})
public class BsonParserSession extends InputStreamParserSession {

	private static final String ARG_ctx = "ctx";
	private static final String BSON_VALUE_KEY = "value";

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParserSession.Builder {

		private final String nullKeyString;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(BsonParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			nullKeyString = ctx.getNullKeyString();
		}

		@Override
		public BsonParserSession build() {
			return new BsonParserSession(this);
		}
	}

	/**
	 * Creates a new builder for a {@link BsonParserSession}.
	 *
	 * @param ctx The BSON parser context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(BsonParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final String nullKeyString;

	protected BsonParserSession(Builder builder) {
		super(builder);
		nullKeyString = builder.nullKeyString;
	}

	private String trimKey(String name) {
		return isTrimStrings() && nn(name) ? name.trim() : name;
	}

	private Object readTypedValue(BsonInputStream is, int elementType, ClassMeta<?> targetType, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {
		Object o = switch (elementType) {
			case 0x01 -> is.readDouble();
			case 0x02 -> trim(is.readString());
			case 0x03 -> parseDocument(is, targetType, outer, pMeta);
			case 0x04 -> parseArray(is, targetType, outer, pMeta);
			case 0x05 -> is.readBinary();
			case 0x07 -> is.readObjectId();
			case 0x08 -> is.readBoolean();
			case 0x09 -> is.readDateTime();
			case 0x0A -> null;
			case 0x10 -> is.readInt32();
			case 0x12 -> is.readInt64();
			case 0x13 -> is.readDecimal128();
			default -> {
				is.skipValue(elementType);
				yield null;
			}
		};
		if (nn(outer) && nn(o))
			setParent(targetType, o, outer);
		return o;
	}

	@SuppressWarnings({
		"resource" // is is caller-owned; this method does not close it
	})
	private <T> T parseDocument(BsonInputStream is, ClassMeta<?> eType, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {
		is.readDocumentSize();
		if (eType == null)
			eType = object();

		var swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		var builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType;
		if (nn(builder))
			sType = builder.getBuilderClassMeta(this);
		else if (nn(swap))
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		// Do not recurse for Optional here - BSON wraps non-bean/map roots as {"value":x}.
		// We must parse the document first, then unwrap via the map.size()==1 / "value" path below.
		var wrapInOptional = eType.isOptional();
		if (sType.isOptional())
			sType = eType.getElementType();

		setCurrentClass(sType);

		Object result;
		// Skip map path for Optional - we need fallback to unwrap {"value":x}
		if (!eType.isOptional() && sType.isMap()) {
			var map = sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType);
			while (!is.isDocumentEnd()) {
				var et = is.readElementType();
				var name = is.readElementName();
				var key = name.equals(nullKeyString) ? null : trimKey(name);
				var value = readTypedValue(is, et, sType.getValueType(), map, pMeta);
				setName(sType.getValueType(), value, key);
				map.put(key, value);
			}
			is.readDocumentTerminator();
			var raw = map instanceof JsonMap jsonmap ? cast(jsonmap, pMeta, eType) : map;
			// Convert JsonMap to target map type (TreeMap, LinkedHashMap, etc.) when needed
			if (eType.isMap() && raw instanceof Map mr && !eType.inner().isInstance(raw))
				result = convertToMemberType(null, mr, eType);
			else
				result = raw;
		} else if (!eType.isOptional() && (nn(builder) || sType.canCreateNewBean(outer))) {
			var beanMap = builder == null ? newBeanMap(outer, sType.inner()) : toBeanMap(builder.create(this, eType));
			while (!is.isDocumentEnd()) {
				var et = is.readElementType();
				var name = is.readElementName();
				var key = trimKey(name);
				var bpm = beanMap.getPropertyMeta(key);
				Object value;
				if ("_type".equals(name)) {
					value = readTypedValue(is, et, string(), null, null);
					if (nn(value))
						beanMap = applyTypeProperty(beanMap, value.toString(), eType);
				} else if (bpm != null) {
					value = readTypedValue(is, et, bpm.getClassMeta(), beanMap.getBean(false), bpm);
					setName(bpm.getClassMeta(), value, key);
					try {
						bpm.set(beanMap, key, value);
					} catch (BeanRuntimeException e) {
						onBeanSetterException(nn(pMeta) ? pMeta : bpm, e);
						throw e;
					}
				} else {
					onUnknownProperty(key, beanMap, readTypedValue(is, et, object(), null, null));
				}
			}
			is.readDocumentTerminator();
			result = builder == null ? beanMap.getBean() : builder.build(this, beanMap.getBean(), eType);
		} else {
			// Fallback: read document elements. Handles scalar, array, Optional roots (all wrapped as {"value":x}).
			// Do NOT call parseArray here - at root the next bytes are type+name of first element, not array doc.
			var map = new JsonMap(this);
			while (!is.isDocumentEnd()) {
				var et = is.readElementType();
				var name = is.readElementName();
				var key = name.equals(nullKeyString) ? null : trimKey(name);
			map.put(key, readTypedValue(is, et, BSON_VALUE_KEY.equals(name) ? eType : object(), map, null));
		}
		is.readDocumentTerminator();
		// Unwrap {"value":x} when target is Optional or when scalar-like (BSON root wrap convention)
		if (map.size() == 1 && map.containsKey(BSON_VALUE_KEY)) {
			var wrapped = map.get(BSON_VALUE_KEY);
				if (eType.isOptional() || (!sType.isMap() && !sType.isBean() && !sType.isObject())
					|| (wrapped == null && sType.isObject())) {
					result = convertToMemberType(null, wrapped, eType);
					if (eType.isOptional())
						wrapInOptional = false; // convertToMemberType already wrapped
				} else {
					var castResult = cast(map, pMeta, eType);
					result = (eType.isMap() && castResult instanceof Map && !eType.inner().isInstance(castResult))
						? convertToMemberType(null, castResult, eType) : castResult;
				}
			} else {
				var castResult = cast(map, pMeta, eType);
				result = (eType.isMap() && castResult instanceof Map && !eType.inner().isInstance(castResult))
					? convertToMemberType(null, castResult, eType) : castResult;
			}
		}
		// Final fallback: when target is Optional and we have map with "value", unwrap
		if (eType.isOptional() && result instanceof Map m && m.size() == 1 && m.containsKey(BSON_VALUE_KEY)) {
			result = convertToMemberType(null, m.get(BSON_VALUE_KEY), eType);
			wrapInOptional = false; // convertToMemberType already wrapped
		}
		// Convert JsonMap to target Map type (TreeMap, LinkedHashMap, etc.) when needed
		if (eType.isMap() && result instanceof Map m2 && !eType.inner().isInstance(result))
			result = convertToMemberType(null, m2, eType);
		// Unswap when result is in swapped form (e.g. JsonMap for bean with Map swap, String for Class).
		// Skip when convertToMemberType already produced normal form (e.g. Class from String).
		if (nn(swap) && nn(result) && swap.isSwappedObject(result))
			result = unswap(swap, result, eType);
		return (T)(wrapInOptional ? opt(result) : result);
	}

	private BeanMap<?> applyTypeProperty(BeanMap<?> beanMap, String typeName, ClassMeta<?> eType) throws ExecutableException {
		var cm = getBeanRegistry().getClassMeta(typeName);
		if (nn(cm) && nn(eType) && eType.isAssignableFrom(cm))
			return toBeanMap(newBean(cm.inner()));
		return beanMap;
	}

	@SuppressWarnings({
		"unused",    // pMeta kept for API consistency with other parseXxx methods
		"java:S1172" // Same as above
	})
	private Object parseArray(BsonInputStream is, ClassMeta<?> eType, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {
		is.readDocumentSize();
		if (eType == null)
			eType = object();

		var sType = eType;
		// Do not recurse for Optional - parse array elements with inner type, wrap at end
		if (sType.isOptional())
			sType = eType.getElementType();

		// Array types (String[], int[][], etc.) must use a Collection to gather elements; toArray() converts at the end.
		// ClassMeta.newInstance() for array types returns an empty array, not a Collection - never use it for arrays.
		// Only use sType.newInstance() for actual Collection types (List, Set, etc.).
		Collection coll;
		if (sType.isArray() || sType.isArgs() || sType.inner().isArray())
			coll = new JsonList(this);
		else {
			var instance = sType.canCreateNewInstance(outer) ? sType.newInstance(outer) : null;
			coll = (instance instanceof Collection c) ? c : new JsonList(this);
		}
		var elementType = def(sType.getElementType(), object());

		while (!is.isDocumentEnd()) {
			var et = is.readElementType();
			is.readElementName();
			coll.add(readTypedValue(is, et, elementType, coll, null));
		}
		is.readDocumentTerminator();

		Object result = sType.isArray() || sType.isArgs() ? toArray(sType, coll) : coll;
		return eType.isOptional() ? opt(result) : result;
	}

	@Override
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (var is = new BsonInputStream(pipe)) {
			return (T)parseDocument(is, type, getOuter(), null);
		}
	}
}
