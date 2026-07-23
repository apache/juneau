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
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.marshall.bson.DataType.*;

import java.io.*;
import java.math.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link BsonSerializer}.
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
	"resource",   // Output streams managed by calling code
	"rawtypes",   // Raw types necessary for generic type handling
	"java:S110",  // Inheritance depth acceptable for serializer session hierarchy
	"java:S115",  // Constants use UPPER_snakeCase convention
	"java:S3776"  // Cognitive complexity acceptable for serialization dispatch logic
})
public class BsonSerializerSession extends OutputStreamSerializerSession implements RecordWritable, ArrayRecordWritable {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializerSession.Builder<Builder> {

		private BsonSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
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
	 * 	<br>Cannot be <jk>null</jk>.
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

	/**
	 * Opens a whole-value push generator targeting BSON output, bound to this live session.
	 * {@link RecordWriter#write(Object) write(Object)} delegates to
	 * {@link SerializerSession#write(Object, Object)}.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* RecordWritable */
	public RecordWriter writeRecords(Object output) throws IOException {
		return RecordAdapter.writer(this, output);
	}

	/**
	 * Buffered array-element {@link RecordWriter} for BSON, bound to this live session.  BSON's wire
	 * format requires the entire document byte-length up front (length prefix at every nesting
	 * level), so streaming without buffering is not possible.
	 *
	 * @param output The output.
	 * @return A buffered {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* ArrayRecordWritable */
	public RecordWriter writeArrayRecords(Object output) throws IOException {
		return RecordAdapter.arrayWriter(this, output);
	}

	/**
	 * The BSON record writer is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* RecordWritable */
	public boolean isRecordStreaming() { return false; }

	/**
	 * The BSON array-record writer is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* ArrayRecordWritable */
	public boolean isArrayRecordStreaming() { return false; }

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
		} else if (sType.isBean()) {
			out.writeElement(DOCUMENT.value, name);
			var child = out.createChild();
			child.startDocument();
			writeBeanMap(child, toBeanMap(o), getBeanTypeName(this, eType, aType, pMeta));
			out.writeChildDocument(child);
		} else if (sType.isMap()) {
			if (sType.isBeanMap()) {
				out.writeElement(DOCUMENT.value, name);
				var child = out.createChild();
				child.startDocument();
				writeBeanMap(child, (BeanMap)o, getBeanTypeName(this, eType, aType, pMeta));
				out.writeChildDocument(child);
			} else {
				out.writeElement(DOCUMENT.value, name);
				var child = out.createChild();
				child.startDocument();
				writeMap(child, (Map)o, eType);
				out.writeChildDocument(child);
			}
		} else if (sType.isByteArray()) {
			out.writeElement(BINARY.value, name);
			out.writeBinary((byte[])o);
		} else if (sType.isCollection() || sType.isArray()) {
			out.writeElement(ARRAY.value, name);
			var child = out.createChild();
			child.startDocument();
			writeArray(child, sType.isArray() ? toList(sType.inner(), o) : (Collection)o, eType);
			out.writeChildDocument(child);
		} else if (sType.isCharSequence() || sType.isChar() || sType.isEnum()) {
			out.writeElement(STRING.value, name);
			out.writeString(trim(o));
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
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			out.writeElement(STRING.value, name);
			out.writeString(resolveUri(o.toString()));
		} else if (sType.isDate()) {
			if (ctx.writeDatesAsDatetime) {
				out.writeElement(DATETIME.value, name);
				out.writeDateTime(toEpochMillis(o));
			} else {
				out.writeElement(STRING.value, name);
				out.writeString(writeDate((Date)o, sType));
			}
		} else if (sType.isCalendar()) {
			if (ctx.writeDatesAsDatetime) {
				out.writeElement(DATETIME.value, name);
				out.writeDateTime(toEpochMillis(o));
			} else {
				out.writeElement(STRING.value, name);
				out.writeString(writeCalendar(o, sType));
			}
		} else if (sType.isTemporal()) {
			// BSON datetime (0x09) is int64 millis-since-epoch, so only instant-bearing temporals can use it.
			// Local temporals (LocalDate/LocalDateTime/LocalTime/Year/...) cannot supply an instant; fall back to
			// the ISO-string path instead of crashing in Instant.from(...) (matches CBOR/MsgPack temporal handling).
			if (ctx.writeDatesAsDatetime && isInstantCapable(o)) {
				out.writeElement(DATETIME.value, name);
				out.writeDateTime(toEpochMillis(o));
			} else {
				out.writeElement(STRING.value, name);
				out.writeString(writeTemporal((TemporalAccessor)o, sType));
			}
		} else if (sType.isDuration()) {
			writeDuration(out, name, (Duration)o);
		} else if (sType.isPeriod()) {
			out.writeElement(STRING.value, name);
			out.writeString(writePeriod((Period)o));
		} else if (sType.isStreamable()) {
			out.writeElement(ARRAY.value, name);
			var child = out.createChild();
			child.startDocument();
			writeArray(child, toListFromStreamable(o, sType), eType);
			out.writeChildDocument(child);
		} else {
			out.writeElement(STRING.value, name);
			out.writeString(toString(o));
		}
		pop();
	}

	private void writeBeanMap(BsonOutputStream out, BeanMap<?> m, String typeName) throws SerializeException {
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
				writeElement(out, x.getName(), x.getValue(), (ClassMeta<?>) x.getBeanInfo(), pMeta);
		}
	}

	@SuppressWarnings({
		"unchecked" // Cast from raw type is safe by checked conditional above.
	})
	private void writeArray(BsonOutputStream out, Collection c, ClassMeta<?> type) throws SerializeException {
		var elementType = type.getElementType();
		c = sort(c);
		var idx = 0;
		for (var x : c)
			writeElement(out, String.valueOf(idx++), x, elementType, null);
	}

	@SuppressWarnings({
		"unchecked" // Cast from raw type is safe by checked conditional above.
	})
	private void writeMap(BsonOutputStream out, Map m, ClassMeta<?> type) throws SerializeException {
		var valueType = type.getValueType();
		var keyType = type.getKeyType();
		m = sort(m);
		for (Map.Entry<?,?> entry : ((Map<?,?>)m).entrySet()) {
			var key = generalize(entry.getKey(), keyType);
			var name = (key == null) ? ctx.nullKeyString : trim(key);
			writeElement(out, name, entry.getValue(), valueType, null);
		}
	}

	/**
	 * Returns whether the given value can supply an instant for BSON datetime (0x09) encoding.
	 *
	 * <p>
	 * Only instant-bearing temporals (such as {@link Instant}, {@link OffsetDateTime}, and {@link ZonedDateTime})
	 * support {@link ChronoField#INSTANT_SECONDS}.  Local temporals ({@link LocalDate}, {@link LocalDateTime},
	 * {@link LocalTime}, {@link Year}, {@link YearMonth}, {@link MonthDay}) cannot, so they must fall back to the
	 * ISO-string path rather than crashing in {@link Instant#from(TemporalAccessor)}.
	 *
	 * @param o The value being serialized.
	 * @return <jk>true</jk> if the value is an instant-bearing temporal.
	 */
	private static boolean isInstantCapable(Object o) {
		return o instanceof TemporalAccessor o2 && o2.isSupported(ChronoField.INSTANT_SECONDS);
	}

	private static long toEpochMillis(Object o) {
		if (o instanceof Date o2)
			return o2.getTime();
		if (o instanceof Calendar o2)
			return o2.getTimeInMillis();
		if (o instanceof Instant o2)
			return o2.toEpochMilli();
		if (o instanceof Temporal o2)
			return Instant.from(o2).toEpochMilli();
		return 0L;
	}

	private boolean willRecurse(BeanPropertyValue v) throws SerializeException {
		var aType = push2(v.getName(), v.getValue(), (ClassMeta<?>) v.getBeanInfo());
		if (nn(aType))
			pop();
		return aType == null;
	}

	@Override
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {
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
			writeBeanMap(bsonOut, toBeanMap(o), getBeanTypeName(this, eType, sType, null));
		} else if (sType != null && sType.isMap() && !sType.isBeanMap()) {
			pop();
			writeMap(bsonOut, (Map)o, eType);
		} else {
			pop();
			writeElement(bsonOut, "value", o, eType, null);
		}

		bsonOut.writeDocumentTo(out.getOutputStream());
	}

	private void writeDuration(BsonOutputStream out, String name, Duration value) {
		var f = getDurationFormat();
		var s = writeDuration(value);
		if (f == DurationFormat.NANOS || f == DurationFormat.MILLIS) {
			out.writeElement(INT64.value, name);
			out.writeInt64(Long.parseLong(s));
		} else if (f == DurationFormat.SECONDS) {
			out.writeElement(DOUBLE.value, name);
			out.writeDouble(Double.parseDouble(s));
		} else {
			out.writeElement(STRING.value, name);
			out.writeString(s);
		}
	}

}
