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
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.math.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link ProtobufParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Protobuf">Protobuf Binary Format Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"rawtypes",  // Raw types necessary for generic type handling
	"unchecked", // Type erasure requires unchecked casts
	"java:S110", // Inheritance depth acceptable for parser session hierarchy
	"resource",  // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
	"java:S115"  // Constants use UPPER_camelCase convention (e.g., ARG_ctx)
})
public class ProtobufParserSession extends InputStreamParserSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParserSession.Builder<Builder> {

		private final ProtobufParser ctx;

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(ProtobufParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public ProtobufParserSession build() {
			return new ProtobufParserSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(ProtobufParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final ProtobufParser ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ProtobufParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override /* Overridden from ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		var is = new ProtobufReader(pipe.getInputStream());
		if (! type.isBean())
			throw new ParseException(this, "Protobuf binary parsing requires a bean target type, but got '%s'", cn(type.inner()));
		return (T)parseMessage(type, is, getOuter());
	}

	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for the tag-loop dispatch
		"java:S6541"  // Brain method acceptable for the parse workhorse
	})
	private Object parseMessage(ClassMeta<?> type, ProtobufReader is, Object outer) throws IOException, ParseException, ExecutableException {
		var pcm = ctx.getProtobufClassMeta(type);
		var m = newBeanMap(outer, type.inner());

		var repeated = new LinkedHashMap<Integer,List<Object>>();
		var maps = new LinkedHashMap<Integer,Map<Object,Object>>();

		long tag;
		while ((tag = is.readTag()) != ProtobufReader.EOF) {
			var fn = ProtobufReader.fieldNumber(tag);
			var wt = ProtobufReader.wireType(tag);
			var entry = pcm.entryFor(fn);
			if (entry == null) {
				is.skipField(wt);
				continue;
			}
			switch (entry.kind()) {
				case SCALAR -> setProperty(m, entry, decodeSingle(entry.scalarType(), entry.propertyType(), is, m.getBean(false)));
				case MESSAGE -> setProperty(m, entry, decodeSingle(ProtobufScalarType.AUTO, entry.propertyType(), is, m.getBean(false)));
				case PACKED_REPEATED -> {
					var list = repeated.computeIfAbsent(fn, k -> new ArrayList<>());
					var elType = entry.propertyType().getElementType();
					if (wt == WireType.LEN) {
						var sub = new ProtobufReader(is.readLenDelimited());
						while (sub.available() > 0)
							list.add(decodeScalar(entry.scalarType(), elType, sub));
					} else {
						// Interop tolerance:  accept unpacked repeated scalars too.
						list.add(decodeScalar(entry.scalarType(), elType, is));
					}
				}
				case TAGGED_REPEATED -> {
					var list = repeated.computeIfAbsent(fn, k -> new ArrayList<>());
					var elType = entry.propertyType().getElementType();
					var elSwap = elType == null ? null : elType.getSwap(this);
					if (elSwap != null)
						list.add(decodeSwapped(elSwap, elType, is, m.getBean(false)));
					else if (elType.isBean() || elType.isMap())
						list.add(parseMessage(elType, new ProtobufReader(is.readLenDelimited()), m.getBean(false)));
					else
						list.add(decodeScalar(entry.scalarType(), elType, is));
				}
				case MAP -> {
					var map = maps.computeIfAbsent(fn, k -> new LinkedHashMap<>());
					parseMapEntry(entry.propertyType(), new ProtobufReader(is.readLenDelimited()), map);
				}
			}
		}

		repeated.forEach((fn, list) -> {
			var entry = pcm.entryFor(fn);
			setProperty(m, entry, convertToType(list, entry.propertyType()));
		});
		maps.forEach((fn, map) -> {
			var entry = pcm.entryFor(fn);
			setProperty(m, entry, convertToType(map, entry.propertyType()));
		});

		return m.getBean();
	}

	/**
	 * Decodes a single scalar or message value, honoring any session-level {@link ObjectSwap} registered on the
	 * declared type.
	 *
	 * <p>
	 * Mirrors the serializer's {@code trySerializeSwapped} step:  the protobuf field table is built from declared/raw
	 * types and is session-independent, so per-property swaps are resolved here at parse time via
	 * {@link ClassMeta#getSwap(MarshallingSession) getSwap(this)}.  When a swap is present
	 * the wire value is decoded according to the <i>swapped</i> type's shape (message vs. scalar) and then run back
	 * through {@link #unswap(ObjectSwap, Object, ClassMeta) unswap}.  When absent the original behavior is preserved:
	 * an {@link ProtobufScalarType#AUTO} hint indicates a {@code MESSAGE} field (length-delimited sub-message), any
	 * other hint a plain scalar.
	 *
	 * <p>
	 * Property-level swaps already installed on the bean property are not re-applied here (the declared type is then
	 * the swapped type, whose {@code getSwap()} is <jk>null</jk>); {@link #setProperty(BeanMap, ProtobufFieldEntry, Object)}'s
	 * setter performs that unswap.  Only class/session-registered swaps surface here.
	 *
	 * @param declaredScalar The precomputed scalar type, or {@link ProtobufScalarType#AUTO} for a message field.
	 * @param declaredType The declared property type.
	 * @param is The protobuf reader positioned at the value.
	 * @param outer The outer bean (for nested-message parent wiring).
	 * @return The decoded (and unswapped, if applicable) value.
	 */
	private Object decodeSingle(ProtobufScalarType declaredScalar, ClassMeta<?> declaredType, ProtobufReader is, Object outer) throws IOException, ParseException, ExecutableException {
		var swap = declaredType == null ? null : declaredType.getSwap(this);
		if (swap != null)
			return decodeSwapped(swap, declaredType, is, outer);
		if (declaredScalar == ProtobufScalarType.AUTO)
			return parseMessage(declaredType, new ProtobufReader(is.readLenDelimited()), outer);
		return decodeScalar(declaredScalar, declaredType, is);
	}

	/**
	 * Decodes a value whose declared type has a registered {@link ObjectSwap}, then unswaps it back to the declared
	 * type.  The on-wire shape is determined by the swap's swap-class:  a bean/map swap-class is read as a
	 * length-delimited sub-message, anything else as a scalar of the swap-class's default scalar type.
	 *
	 * @param swap The resolved object swap.
	 * @param declaredType The declared (unswapped) type, used as the unswap hint.
	 * @param is The protobuf reader positioned at the value.
	 * @param outer The outer bean (for nested-message parent wiring).
	 * @return The unswapped value.
	 */
	private Object decodeSwapped(ObjectSwap<?,?> swap, ClassMeta<?> declaredType, ProtobufReader is, Object outer) throws IOException, ParseException, ExecutableException {
		var sType = swap.getSwapClassMeta(this);
		Object sval;
		if (sType.isBean() || sType.isMap())
			sval = parseMessage(sType, new ProtobufReader(is.readLenDelimited()), outer);
		else
			sval = decodeScalar(ProtobufClassMeta.defaultScalarType(sType), sType, is);
		return unswap(swap, sval, declaredType);
	}

	private void setProperty(BeanMap<?> m, ProtobufFieldEntry entry, Object value) {
		var cm = entry.propertyType();
		try {
			entry.property().set(m, entry.name(), convertToType(value, cm));
		} catch (BeanRuntimeException e) {
			onBeanSetterException(entry.property(), e);
			throw e;
		}
	}

	private Object decodeScalar(ProtobufScalarType st, ClassMeta<?> cm, ProtobufReader is) throws IOException, ParseException {
		switch (st) {
			case BOOL: return is.readVarint() != 0;
			case INT32: return is.readVarint();
			case INT64: return is.readVarint();
			case UINT32: return is.readVarint() & 0xFFFFFFFFL;
			case UINT64: return decodeUint64(cm, is);
			case SINT32: return (long)is.readZigZag32();
			case SINT64: return is.readZigZag64();
			case FIXED32: return is.readFixed32() & 0xFFFFFFFFL;
			case SFIXED32: return (long)is.readFixed32();
			case FIXED64: return is.readFixed64();
			case SFIXED64: return is.readFixed64();
			case FLOAT: return Float.intBitsToFloat(is.readFixed32());
			case DOUBLE: return Double.longBitsToDouble(is.readFixed64());
			case STRING: return decodeString(cm, is.readString());
			case BYTES: return is.readLenDelimited();
			case ENUM_INT: return decodeEnumOrdinal(cm, (int)is.readVarint());
			case ENUM_STRING: return is.readString();
			default: return is.readString();  // HTT: AUTO is resolved to a concrete type before reaching here.
		}
	}

	private static Object decodeUint64(ClassMeta<?> cm, ProtobufReader is) throws IOException {
		var raw = is.readVarint();
		if (ProtobufClassMeta.isBigInteger(cm))
			return new BigInteger(Long.toUnsignedString(raw));
		return raw;
	}

	private static Object decodeEnumOrdinal(ClassMeta<?> cm, int ordinal) {
		var constants = cm.inner().getEnumConstants();
		if (constants != null && ordinal >= 0 && ordinal < constants.length)
			return constants[ordinal];
		return ordinal;
	}

	private Object decodeString(ClassMeta<?> cm, String s) throws ParseException {
		if (cm.isDate())
			return parseDate(s, cm);
		if (cm.isCalendar())
			return parseCalendar(s, cm);
		if (cm.isTemporal())
			return parseTemporal(s, cm);
		if (cm.isDuration())
			return parseDuration(s);
		if (cm.isPeriod())
			return parsePeriod(s);
		return s;
	}

	private void parseMapEntry(ClassMeta<?> mapType, ProtobufReader is, Map map) throws IOException, ParseException, ExecutableException {
		var keyType = mapType.getKeyType();
		var valueType = mapType.getValueType();
		Object key = null;
		Object value = null;
		long tag;
		while ((tag = is.readTag()) != ProtobufReader.EOF) {
			var fn = ProtobufReader.fieldNumber(tag);
			var wt = ProtobufReader.wireType(tag);
			if (fn == 1)
				key = decodeMapComponent(keyType, is);
			else if (fn == 2)
				value = decodeMapComponent(valueType, is);
			else
				is.skipField(wt);
		}
		map.put(convertToType(key, keyType), convertToType(value, valueType));
	}

	private Object decodeMapComponent(ClassMeta<?> cm, ProtobufReader is) throws IOException, ParseException, ExecutableException {
		var swap = cm == null ? null : cm.getSwap(this);
		if (swap != null)
			return decodeSwapped(swap, cm, is, null);
		if (cm.isBean() || cm.isMap())
			return parseMessage(cm, new ProtobufReader(is.readLenDelimited()), null);
		return decodeScalar(ProtobufClassMeta.defaultScalarType(cm), cm, is);
	}
}
