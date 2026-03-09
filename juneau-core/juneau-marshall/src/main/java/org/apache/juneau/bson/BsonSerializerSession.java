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

import static org.apache.juneau.bson.DataType.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.math.*;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.Iso8601Utils;

/**
 * Session object that lives for the duration of a single use of {@link BsonSerializer}.
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
	"resource",   // Output streams managed by calling code
	"rawtypes",   // Raw types necessary for generic type handling
	"java:S110",  // Inheritance depth acceptable for serializer session hierarchy
	"java:S115",  // Constants use UPPER_snakeCase convention
	"java:S3776"  // Cognitive complexity acceptable for serialization dispatch logic
})
public class BsonSerializerSession extends OutputStreamSerializerSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializerSession.Builder {

		private BsonSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(BsonSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public BsonSerializerSession build() {
			return new BsonSerializerSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(BsonSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private static BsonOutputStream getBsonOutputStream(SerializerPipe out) throws IOException {
		Object output = out.getRawOutput();
		if (output instanceof BsonOutputStream output2)
			return output2;
		var os = new BsonOutputStream(out.getOutputStream());
		out.setOutputStream(os);
		return os;
	}

	private final BsonSerializer ctx;

	protected BsonSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	private void writeElement(BsonOutputStream out, String name, Object o, ClassMeta<?> eType, BeanPropertyMeta pMeta) throws SerializeException {
		if (o == null) {
			out.writeElement(NULL.value, name);
			return;
		}
		if (eType == null)
			eType = object();

		var aType = push2(name, o, eType);
		if (aType == null) {
			out.writeElement(NULL.value, name);
			pop();
			return;
		}

		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}

		var sType = aType;
		var swap = aType.getSwap(this);
		if (nn(swap)) {
			o = swap(swap, o);
			sType = swap.getSwapClassMeta(this);
			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}

		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0)) {
			out.writeElement(NULL.value, name);
		} else if (sType.isBoolean()) {
			out.writeElement(BOOLEAN.value, name);
			out.writeBoolean((Boolean)o);
		} else if (sType.isNumber()) {
			if (sType.inner() == int.class || sType.inner() == short.class || sType.inner() == byte.class
				|| sType.inner() == Integer.class || sType.inner() == Short.class || sType.inner() == Byte.class) {
				out.writeElement(INT32.value, name);
				out.writeInt32(((Number)o).intValue());
			} else if (sType.inner() == long.class || sType.inner() == Long.class) {
				out.writeElement(INT64.value, name);
				out.writeInt64(((Number)o).longValue());
			} else if (sType.inner() == BigDecimal.class) {
				out.writeElement(DECIMAL128.value, name);
				out.writeDecimal128((BigDecimal)o);
			} else if (sType.inner() == BigInteger.class) {
				out.writeElement(DECIMAL128.value, name);
				out.writeDecimal128(new BigDecimal((BigInteger)o));
			} else {
				out.writeElement(DOUBLE.value, name);
				out.writeDouble(((Number)o).doubleValue());
			}
		} else if (sType.isDateOrCalendarOrTemporal() && ctx.writeDatesAsDatetime) {
			out.writeElement(DATETIME.value, name);
			out.writeDateTime(toEpochMillis(o));
		} else if (sType.isDateOrCalendarOrTemporal() && !ctx.writeDatesAsDatetime) {
			out.writeElement(STRING.value, name);
			out.writeString(Iso8601Utils.format(o, sType, getTimeZone()));
		} else if (sType.isDuration()) {
			out.writeElement(STRING.value, name);
			out.writeString(o.toString());
		} else if (sType.isBean()) {
			out.writeElement(DOCUMENT.value, name);
			var child = out.createChild();
			child.startDocument();
			serializeBeanMap(child, toBeanMap(o), getBeanTypeName(this, eType, aType, pMeta));
			out.writeChildDocument(child);
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			out.writeElement(STRING.value, name);
			out.writeString(resolveUri(o.toString()));
		} else if (sType.isMap()) {
			if (o instanceof BeanMap o2) {
				out.writeElement(DOCUMENT.value, name);
				var child = out.createChild();
				child.startDocument();
				serializeBeanMap(child, o2, getBeanTypeName(this, eType, aType, pMeta));
				out.writeChildDocument(child);
			} else {
				out.writeElement(DOCUMENT.value, name);
				var child = out.createChild();
				child.startDocument();
				serializeMap(child, (Map)o, eType);
				out.writeChildDocument(child);
			}
		} else if (sType.isByteArray()) {
			out.writeElement(BINARY.value, name);
			out.writeBinary((byte[])o);
		} else if (sType.isCollection() || sType.isArray()) {
			out.writeElement(ARRAY.value, name);
			var child = out.createChild();
			child.startDocument();
			serializeArray(child, sType.isArray() ? toList(sType.inner(), o) : (Collection)o, eType);
			out.writeChildDocument(child);
		} else if (sType.isStreamable()) {
			out.writeElement(ARRAY.value, name);
			var child = out.createChild();
			child.startDocument();
			serializeArray(child, toListFromStreamable(o, sType), eType);
			out.writeChildDocument(child);
		} else if (sType.isCharSequence() || sType.isChar() || sType.isEnum()) {
			out.writeElement(STRING.value, name);
			out.writeString(trim(o));
		} else {
			out.writeElement(STRING.value, name);
			out.writeString(toString(o));
		}
		pop();
	}

	private void serializeBeanMap(BsonOutputStream out, BeanMap<?> m, String typeName) throws SerializeException {
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		var values = new ArrayList<BeanPropertyValue>();

		if (nn(typeName)) {
			var pm = m.getMeta().getTypeProperty();
			if (pm != null)
				values.add(new BeanPropertyValue(pm, pm.getName(), typeName, null));
		}

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			if (nn(thrown)) {
				onBeanGetterException(pMeta, thrown);
				return;
			}
			var p = new BeanPropertyValue(pMeta, key, value, null);
			if (!isKeepNullProperties() && willRecurse(p))
				return;
			values.add(p);
		});

		for (var x : values) {
			var pMeta = x.getMeta();
			if (pMeta.canRead())
				writeElement(out, x.getName(), x.getValue(), x.getClassMeta(), pMeta);
		}
	}

	@SuppressWarnings("unchecked")
	private void serializeArray(BsonOutputStream out, Collection c, ClassMeta<?> type) throws SerializeException {
		var elementType = type.getElementType();
		c = sort(c);
		var idx = 0;
		for (var x : c)
			writeElement(out, String.valueOf(idx++), x, elementType, null);
	}

	@SuppressWarnings("unchecked")
	private void serializeMap(BsonOutputStream out, Map m, ClassMeta<?> type) throws SerializeException {
		var valueType = type.getValueType();
		var keyType = type.getKeyType();
		m = sort(m);
		for (Map.Entry<?,?> entry : ((Map<?,?>)m).entrySet()) {
			var key = generalize(entry.getKey(), keyType);
			var name = (key == null) ? ctx.nullKeyString : trim(key);
			writeElement(out, name, entry.getValue(), valueType, null);
		}
	}

	private static long toEpochMillis(Object o) {
		if (o instanceof java.util.Date d)
			return d.getTime();
		if (o instanceof java.util.Calendar c)
			return c.getTimeInMillis();
		if (o instanceof Instant i)
			return i.toEpochMilli();
		if (o instanceof Temporal t)
			return Instant.from(t).toEpochMilli();
		return 0L;
	}

	private boolean willRecurse(BeanPropertyValue v) throws SerializeException {
		var aType = push2(v.getName(), v.getValue(), v.getClassMeta());
		if (nn(aType))
			pop();
		return aType == null;
	}

	@Override
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		var bsonOut = getBsonOutputStream(out);
		bsonOut.startDocument();

		var eType = getExpectedRootType(o);
		var aType = push2("root", o, eType);
		ClassMeta<?> sType = aType;
		if (nn(aType)) {
			var swap = aType.getSwap(this);
			if (nn(swap)) {
				o = swap(swap, o);
				sType = swap.getSwapClassMeta(this);
			}
		}

		if (sType != null && sType.isBean()) {
			pop();
			serializeBeanMap(bsonOut, toBeanMap(o), getBeanTypeName(this, eType, sType, null));
		} else if (sType != null && sType.isMap() && !(o instanceof BeanMap)) {
			pop();
			serializeMap(bsonOut, (Map)o, eType);
		} else {
			pop();
			writeElement(bsonOut, "value", o, eType, null);
		}

		bsonOut.writeDocumentTo(out.getOutputStream());
	}

	@Override
	protected boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}
}
