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
package org.apache.juneau.marshall.protobuf;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.math.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.utils.Utils;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link ProtobufSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBinaryBasics">Protobuf Binary Format Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource",  // Output streams managed by calling code
	"java:S110", // Inheritance depth acceptable for serializer session hierarchy
	"rawtypes",  // Raw types necessary for generic Map/Collection handling
	"unchecked"  // Raw Map passed to forEachEntry requires an unchecked conversion to Map<K,V>
})
public class ProtobufSerializerSession extends OutputStreamSerializerSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializerSession.Builder<Builder> {

		private final ProtobufSerializer ctx;

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(ProtobufSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public ProtobufSerializerSession build() {
			return new ProtobufSerializerSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(ProtobufSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final ProtobufSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ProtobufSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	private static ProtobufWriter getProtobufWriter(SerializerPipe out) throws IOException {
		var output = out.getRawOutput();
		if (output instanceof ProtobufWriter output2)
			return output2;
		var w = new ProtobufWriter(out.getOutputStream());
		out.setOutputStream(w);
		return w;
	}

	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		var w = getProtobufWriter(out);
		if (o == null)
			return;
		var cm = getClassMetaForObject(o);
		if (cm != null && (cm.isMap() && ! cm.isBean()) && ! (o instanceof BeanMap))
			throw new SerializeException("Protobuf binary serialization requires a bean root type, not a raw Map.");
		serializeBean(w, toBeanMap(o), getClassMetaForObject(o));
		w.flush();
	}

	private void serializeBean(ProtobufWriter out, BeanMap<?> m, ClassMeta<?> cm) throws SerializeException {
		var pcm = ctx.getProtobufClassMeta(cm);

		// Collect non-null property values keyed by name (presence model:  null => omit).
		var values = new HashMap<String,Object>();
		m.forEachValue(Utils::nn, (pMeta, key, value, thrown) -> {
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			else
				values.put(key, value);
		});

		// Emit in field-number order for deterministic output.
		for (var entry : pcm.entries()) {
			var value = values.get(entry.name());
			if (value == null)
				continue;
			serializeField(out, entry, value);
		}
	}

	private void serializeField(ProtobufWriter out, ProtobufFieldEntry entry, Object value) throws SerializeException {
		var fn = entry.fieldNumber();
		switch (entry.kind()) {
			case SCALAR, MESSAGE -> serializeSingle(out, fn, entry.scalarType(), value, entry.propertyType());
			case PACKED_REPEATED -> serializePackedField(out, fn, entry, value);
			case TAGGED_REPEATED -> serializeTaggedRepeatedField(out, fn, entry, value);
			case MAP -> serializeMapField(out, fn, (Map)value, entry.propertyType());
		}
	}

	/**
	 * Serializes a single scalar or message value at the given field number, honoring any session-level
	 * {@link ObjectSwap} and deriving the on-wire shape (message vs. scalar) from the value's <b>runtime</b> type.
	 *
	 * <p>
	 * The protobuf field table ({@link ProtobufClassMeta}) is built once per bean from declared/raw property types and
	 * is session-independent, so the precomputed {@link ProtobufFieldEntry#kind() kind}/{@link
	 * ProtobufFieldEntry#scalarType() scalarType} cannot account for session-registered swaps.  This method therefore
	 * re-derives the shape at dispatch time, mirroring {@code CborSerializerSession.serializeAnything}'s
	 * {@link ClassMeta#getSwap(org.apache.juneau.marshall.MarshallingSession) getSwap(this)} step.
	 *
	 * <p>
	 * Resolving the swap from the <b>runtime</b> type (not the declared type) is what keeps this correct in both
	 * directions:
	 * <ul>
	 * 	<li>Bean <i>property</i> values whose declared type has a registered swap are already pre-swapped by the bean
	 * 		layer ({@code MarshalledPropertyPostProcessor.installSwapAwareTransforms} installs a read transform when the
	 * 		property's raw type {@code hasChildSwaps()}), so the value arriving here is the swap-class instance whose
	 * 		runtime {@code getSwap()} is <jk>null</jk> &mdash; no double swap, and the shape follows the swapped value.
	 * 	<li>Raw {@code List}/{@code Map} elements are <b>not</b> pre-swapped (the transform installs on the element type,
	 * 		not on the container property), so their runtime {@code getSwap()} fires here exactly once.
	 * </ul>
	 *
	 * <p>
	 * The no-swap path stays byte-identical to the golden vectors:  a non-bean/non-map runtime type is emitted as a
	 * scalar using the precomputed {@code declaredScalar} (preserving {@code @Protobuf(type=…)} overrides), and a
	 * bean/map runtime type is emitted as a length-delimited sub-message.
	 *
	 * @param out The protobuf writer.
	 * @param fn The protobuf field number.
	 * @param declaredScalar The precomputed scalar type for the no-swap scalar path, or {@link ProtobufScalarType#AUTO}.
	 * @param value The non-null value to serialize.
	 * @param declaredType The declared property (or element/key/value) type, used only as a runtime-type hint.
	 */
	private void serializeSingle(ProtobufWriter out, int fn, ProtobufScalarType declaredScalar, Object value, ClassMeta<?> declaredType) throws SerializeException {
		var rType = getClassMetaForObject(value, declaredType);
		var swap = rType.getSwap(this);
		var v = value;
		var st = declaredScalar;
		if (swap != null) {
			v = swap(swap, value);
			if (v == null)
				return;  // Swapped to null => omit the field entirely (presence model).
			rType = swap.getSwapClassMeta(this);
			if (rType.isObject())
				rType = getClassMetaForObject(v);
			st = ProtobufClassMeta.defaultScalarType(rType);
		}
		if (rType.isBean() || rType.isMap())
			serializeMessageField(out, fn, v);
		else
			serializeScalarField(out, fn, st == ProtobufScalarType.AUTO ? ProtobufClassMeta.defaultScalarType(rType) : st, v, rType);
	}

	private void serializeMessageField(ProtobufWriter out, int fn, Object value) throws SerializeException {
		var aType = push2("field", value, getClassMetaForObject(value));
		if (aType == null) {  // Recursion detected.
			pop();
			return;
		}
		try {
			var block = new ByteArrayOutputStream();
			serializeBean(new ProtobufWriter(block), toBeanMap(value), aType);
			out.writeTag(fn, WireType.LEN);
			out.writeLenDelimited(block.toByteArray());
		} finally {
			pop();
		}
	}

	private void serializePackedField(ProtobufWriter out, int fn, ProtobufFieldEntry entry, Object value) throws SerializeException {
		var elements = toElementList(value);
		if (elements.isEmpty())
			return;
		var st = entry.scalarType();
		var elType = entry.propertyType().getElementType();
		var block = new ByteArrayOutputStream();
		var bw = new ProtobufWriter(block);
		for (var el : elements) {
			if (el == null)
				continue;
			encodeScalarValue(bw, st, el, elType);
		}
		out.writeTag(fn, WireType.LEN);
		out.writeLenDelimited(block.toByteArray());
	}

	private void serializeTaggedRepeatedField(ProtobufWriter out, int fn, ProtobufFieldEntry entry, Object value) throws SerializeException {
		var elements = toElementList(value);
		var elType = entry.propertyType().getElementType();
		for (var el : elements) {
			if (el == null)
				continue;
			serializeSingle(out, fn, entry.scalarType(), el, elType);
		}
	}

	private void serializeMapField(ProtobufWriter out, int fn, Map map, ClassMeta<?> mapType) throws SerializeException {
		var keyType = mapType.getKeyType();
		var valueType = mapType.getValueType();
		var keySt = ProtobufClassMeta.defaultScalarType(keyType);
		var valSt = ProtobufClassMeta.defaultScalarType(valueType);
		forEachEntry(map, e -> {
			var k = e.getKey();
			var v = e.getValue();
			if (k == null || v == null)
				return;
			var block = new ByteArrayOutputStream();
			var bw = new ProtobufWriter(block);
			// Entry message:  key=field 1, value=field 2.  Keys and values honor any registered swap.
			serializeSingle(bw, 1, keySt, k, keyType);
			serializeSingle(bw, 2, valSt, v, valueType);
			out.writeTag(fn, WireType.LEN);
			out.writeLenDelimited(block.toByteArray());
		});
	}

	private void serializeScalarField(ProtobufWriter out, int fn, ProtobufScalarType scalarType, Object value, ClassMeta<?> cm) throws SerializeException {
		var st = scalarType;
		out.writeTag(fn, st.wireType());
		encodeScalarValue(out, st, value, cm);
	}

	private void encodeScalarValue(ProtobufWriter out, ProtobufScalarType st, Object value, ClassMeta<?> cm) throws SerializeException {
		switch (st) {
			case BOOL -> out.writeVarint(toBoolean(value) ? 1 : 0);
			case INT32 -> out.writeVarint(((Number)value).intValue());
			case INT64 -> out.writeVarint(((Number)value).longValue());
			case UINT32 -> out.writeVarint(((Number)value).intValue() & 0xFFFFFFFFL);
			case UINT64 -> out.writeVarint(toUint64Bits(value));
			case SINT32 -> out.writeZigZag32(((Number)value).intValue());
			case SINT64 -> out.writeZigZag64(((Number)value).longValue());
			case FIXED32, SFIXED32 -> out.writeFixed32(((Number)value).intValue());
			case FIXED64, SFIXED64 -> out.writeFixed64(((Number)value).longValue());
			case FLOAT -> out.writeFixed32(Float.floatToRawIntBits(((Number)value).floatValue()));
			case DOUBLE -> out.writeFixed64(Double.doubleToRawLongBits(((Number)value).doubleValue()));
			case STRING -> out.writeString(scalarToString(value, cm));
			case BYTES -> out.writeLenDelimited((byte[])value);
			case ENUM_INT -> out.writeVarint(((Enum<?>)value).ordinal());
			case ENUM_STRING -> out.writeString(((Enum<?>)value).name());
			default -> out.writeString(toString(value));  // HTT: AUTO is resolved to a concrete type before reaching here.
		}
	}

	private static List<Object> toElementList(Object value) {
		if (value instanceof Collection<?> value2)
			return new ArrayList<>(value2);
		if (value != null && value.getClass().isArray())
			return toList(value.getClass(), value);
		return new ArrayList<>();
	}

	private static boolean toBoolean(Object value) {
		if (value instanceof Boolean value2)
			return value2;
		return Boolean.parseBoolean(value.toString());
	}

	private static long toUint64Bits(Object value) {
		if (value instanceof BigInteger value2)
			return value2.longValue();
		return ((Number)value).longValue();
	}

	private String scalarToString(Object value, ClassMeta<?> cm) throws SerializeException {
		if (cm.isDate())
			return serializeDate((Date)value, cm);
		if (cm.isCalendar())
			return serializeCalendar(value, cm);
		if (cm.isTemporal())
			return serializeTemporal((TemporalAccessor)value, cm);
		if (cm.isDuration())
			return serializeDuration((Duration)value);
		if (cm.isPeriod())
			return serializePeriod((Period)value);
		return toString(value);
	}
}
