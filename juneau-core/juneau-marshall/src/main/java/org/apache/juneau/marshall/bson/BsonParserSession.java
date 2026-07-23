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
package org.apache.juneau.marshall.bson;

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
import org.apache.juneau.marshall.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link BsonParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Bson">BSON Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"rawtypes",
	"unchecked",
	"java:S115",
	"java:S125",  // Explanatory comments contain BSON/JSON syntax (e.g. {"value":x}) that Sonar misreads as commented-out code
	"java:S3776",
	"java:S6541",
	"resource"    // RecordReader returned by RecordAdapter is a Closeable owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class BsonParserSession extends InputStreamParserSession implements RecordReadable, ArrayRecordReadable {

	private static final String ARG_ctx = "ctx";
	private static final String BSON_VALUE_KEY = "value";

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParserSession.Builder<Builder> {

		private final String nullKeyString;
		private final int maxLength;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(BsonParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			nullKeyString = ctx.getNullKeyString();
			maxLength = ctx.getMaxLength();
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
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(BsonParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final String nullKeyString;
	private final int maxLength;

	protected BsonParserSession(Builder builder) {
		super(builder);
		nullKeyString = builder.nullKeyString;
		maxLength = builder.maxLength;
	}

	private String trimKey(String name) {
		return isTrimStrings() && nn(name) ? name.trim() : name;
	}

	private Object readTypedValue(BsonInputStream is, int elementType, ClassMeta<?> targetType, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {
		Object o = switch (elementType) {
			case 0x01 -> is.readDouble();
			case 0x02 -> trim(is.readString());
			case 0x03 -> readDocument(is, targetType, outer, pMeta);
			case 0x04 -> readArray(is, targetType, outer, pMeta);
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

	private <T> T readDocument(BsonInputStream is, ClassMeta<?> eType, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {
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
			// Coerce string keys to the declared key type when non-String (e.g. Map<TestEnum,String>)
			// so map.get(EnumConstant) works.  Matches the JSON-family readIntoMap2 pattern.
			var keyType = sType.getKeyType();
			var coerceKeys = nn(keyType) && !keyType.isObject() && !keyType.isString();
			while (!is.isDocumentEnd()) {
				var et = is.readElementType();
				var name = is.readElementName();
				var key = name.equals(nullKeyString) ? null : trimKey(name);
				var value = readTypedValue(is, et, sType.getValueType(), map, pMeta);
				setName(sType.getValueType(), value, key);
				var resolvedKey = coerceKeys ? convertAttrToType(map, key, keyType) : key;
				map.put(resolvedKey, value);
			}
			is.readDocumentTerminator();
			var raw = map instanceof MarshalledMap mm ? cast(mm, pMeta, eType) : map;
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
				if (name.equals(getBeanTypePropertyName(eType))) {
					value = readTypedValue(is, et, string(), null, null);
					if (nn(value))
						beanMap = applyTypeProperty(beanMap, value.toString(), eType);
				} else if (bpm != null) {
					var bcm = (ClassMeta<?>) bpm.getBeanInfo();
					value = readTypedValue(is, et, bcm, beanMap.getBean(false), bpm);
					setName(bcm, value, key);
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
			// Do NOT call readArray here - at root the next bytes are type+name of first element, not array doc.
			var map = newGenericMap();
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
		return (T)(wrapInOptional ? o(result) : result);
	}

	private BeanMap<?> applyTypeProperty(BeanMap<?> beanMap, String typeName, ClassMeta<?> eType) throws ExecutableException {
		var cm = getBeanRegistry().getClassMeta(typeName);
		if (nn(cm) && nn(eType) && eType.isAssignableFrom(cm))
			return toBeanMap(newBean(cm.inner()));
		return beanMap;
	}

	@SuppressWarnings({
		"unused",    // pMeta kept for API consistency with other readXxx methods
		"java:S1172" // Same as above
	})
	private Object readArray(BsonInputStream is, ClassMeta<?> eType, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {
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
			coll = newGenericList();
		else {
			var instance = sType.canCreateNewInstance(outer) ? sType.newInstance(outer) : null;
			coll = (instance instanceof Collection c) ? c : newGenericList();
		}
		var elementType = coalesce(sType.getElementType(), object());

		while (!is.isDocumentEnd()) {
			var et = is.readElementType();
			is.readElementName();
			coll.add(readTypedValue(is, et, elementType, coll, null));
		}
		is.readDocumentTerminator();

		Object result = sType.isArray() || sType.isArgs() ? toArray(sType, coll) : coll;
		return eType.isOptional() ? o(result) : result;
	}

	@Override
	protected <T> T doRead(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (var is = new BsonInputStream(pipe)) {
			is.setMaxLength(maxLength);
			return (T)readDocument(is, type, getOuter(), null);
		}
	}

	/**
	 * Opens a whole-value pull-parser cursor over a BSON document, bound to this live session.
	 * {@link RecordReader#read(Class) read(...)} delegates to the polymorphic
	 * {@link ParserSession#read(Object, Class)} entry point.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader} cursor.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* RecordReadable */
	public RecordReader readRecords(Object input) throws IOException {
		return RecordAdapter.reader(this, input);
	}

	/**
	 * Buffered array-element {@link RecordReader} for BSON, bound to this live session.  Calls
	 * {@code parse(input, List.class, Object.class)} once and iterates the result.
	 *
	 * @param input The input.
	 * @return A buffered {@link RecordReader}.
	 * @throws IOException If a problem occurred reading the input.
	 */
	@Override /* ArrayRecordReadable */
	public RecordReader readArrayRecords(Object input) throws IOException {
		return RecordAdapter.arrayReader(this, input);
	}

	/**
	 * The BSON record cursor is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* RecordReadable */
	public boolean isRecordStreaming() { return false; }

	/**
	 * The BSON array-record cursor is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* ArrayRecordReadable */
	public boolean isArrayRecordStreaming() { return false; }
}
