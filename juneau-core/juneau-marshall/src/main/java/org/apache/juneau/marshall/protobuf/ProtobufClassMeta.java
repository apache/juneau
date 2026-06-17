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

import java.math.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.protobuf.ProtobufFieldEntry.*;

/**
 * The core schema element for the protobuf binary codec:  a per-bean, bidirectional
 * <b>field-number &hArr; property + wire-scalar-type</b> table.
 *
 * <p>
 * Because the protobuf binary wire format is not self-describing, both serialization and parsing consult this
 * table.  Field numbers come from explicit {@link Protobuf#fieldNumber()} overrides first; remaining properties
 * are auto-assigned sequentially from 1, ordered by property name (alphabetical, computed locally here so the
 * numbering is stable regardless of {@code BeanMeta} ordering), skipping the reserved <c>19000&ndash;19999</c> band.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBinaryBasics">Protobuf Binary Format Basics</a>
 * </ul>
 */
public class ProtobufClassMeta extends ExtendedClassMeta {

	private static final int RESERVED_LOW = 19000;
	private static final int RESERVED_HIGH = 19999;

	private final Map<String,ProtobufFieldEntry> entriesByName;
	private final Map<Integer,ProtobufFieldEntry> entriesByNumber;
	private final List<ProtobufFieldEntry> entries;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this metadata is defined on.
	 * @param mp Protobuf metadata provider (for finding information about other artifacts).
	 */
	public ProtobufClassMeta(ClassMeta<?> cm, ProtobufMetaProvider mp) {
		super(cm);

		var byName = new LinkedHashMap<String,ProtobufFieldEntry>();
		var byNumber = new TreeMap<Integer,ProtobufFieldEntry>();

		var bm = cm.getBeanMeta();
		if (bm != null) {
			// R2:  compute the alphabetical property-name ordering locally, independent of BeanMeta ordering.
			var names = new ArrayList<>(bm.getProperties().keySet());
			Collections.sort(names);

			// Pass 1:  reserve all explicit field numbers.
			var used = new HashSet<Integer>();
			var explicit = new LinkedHashMap<String,Integer>();
			for (var name : names) {
				var bpm = bm.getProperties().get(name);
				var pMeta = mp.getProtobufBeanPropertyMeta(bpm);
				var fn = pMeta.getFieldNumber();
				if (fn > 0) {
					explicit.put(name, fn);
					used.add(fn);
				}
			}

			// Pass 2:  auto-assign field numbers into the gaps, in alphabetical order.
			var next = 1;
			for (var name : names) {
				var bpm = bm.getProperties().get(name);
				var pMeta = mp.getProtobufBeanPropertyMeta(bpm);
				int fn;
				if (explicit.containsKey(name)) {
					fn = explicit.get(name);
				} else {
					while (used.contains(next) || isReserved(next))
						next++;
					fn = next;
					used.add(fn);
				}
				var entry = buildEntry(fn, name, bpm, pMeta);
				byName.put(name, entry);
				byNumber.put(fn, entry);
			}
		}

		this.entriesByName = Collections.unmodifiableMap(byName);
		this.entriesByNumber = Collections.unmodifiableMap(byNumber);
		this.entries = List.copyOf(byNumber.values());
	}

	private static boolean isReserved(int fieldNumber) {
		return fieldNumber >= RESERVED_LOW && fieldNumber <= RESERVED_HIGH;
	}

	private static ProtobufFieldEntry buildEntry(int fieldNumber, String name, BeanPropertyMeta bpm, ProtobufBeanPropertyMeta pMeta) {
		var propType = (ClassMeta<?>) bpm.getBeanInfo();
		var override = pMeta.getType();
		var kind = kindOf(propType, elementScalarType(propType, override));
		ProtobufScalarType st;
		switch (kind) {
			case SCALAR -> st = override != ProtobufScalarType.AUTO ? override : defaultScalarType(propType);
			case PACKED_REPEATED, TAGGED_REPEATED -> st = elementScalarType(propType, override);
			default -> st = ProtobufScalarType.AUTO;
		}
		return new ProtobufFieldEntry(fieldNumber, name, bpm, propType, kind, st);
	}

	private static ProtobufScalarType elementScalarType(ClassMeta<?> cm, ProtobufScalarType override) {
		if (cm.isCollection() || cm.isArray()) {
			var el = cm.getElementType();
			if (override != ProtobufScalarType.AUTO)
				return override;
			return defaultScalarType(el);
		}
		return ProtobufScalarType.AUTO;
	}

	private static Kind kindOf(ClassMeta<?> cm, ProtobufScalarType elementScalarType) {
		if (cm.isByteArray())
			return Kind.SCALAR;
		if (cm.isBean())
			return Kind.MESSAGE;
		if (cm.isMap())
			return Kind.MAP;
		if (cm.isCollection() || cm.isArray()) {
			var el = cm.getElementType();
			if (el.isBean() || el.isMap() || el.isByteArray())
				return Kind.TAGGED_REPEATED;
			return elementScalarType.wireType() == WireType.LEN ? Kind.TAGGED_REPEATED : Kind.PACKED_REPEATED;
		}
		return Kind.SCALAR;
	}

	/**
	 * Computes the default protobuf scalar type for a Java type (the spec section C mapping).
	 *
	 * @param cm The class metadata of the Java type.
	 * @return The default scalar type.
	 */
	static ProtobufScalarType defaultScalarType(ClassMeta<?> cm) {
		if (cm.isBoolean())
			return ProtobufScalarType.BOOL;
		if (cm.isByteArray())
			return ProtobufScalarType.BYTES;
		if (cm.isFloat())
			return ProtobufScalarType.FLOAT;
		if (cm.isDouble())
			return ProtobufScalarType.DOUBLE;
		if (cm.isLong())
			return ProtobufScalarType.INT64;
		if (cm.isShort() || cm.isInteger() || cm.inner() == byte.class || cm.inner() == Byte.class)
			return ProtobufScalarType.INT32;
		if (cm.isEnum())
			return ProtobufScalarType.ENUM_INT;
		// String, char, BigInteger, BigDecimal, date/time and any other type fall back to a lossless string form.
		return ProtobufScalarType.STRING;
	}

	/**
	 * Returns whether the specified Java type is a {@link BigInteger}.
	 *
	 * @param cm The class metadata.
	 * @return <jk>true</jk> if the type is a {@link BigInteger}.
	 */
	static boolean isBigInteger(ClassMeta<?> cm) {
		return cm.inner() == BigInteger.class;
	}

	/**
	 * Returns the protobuf field number for the specified property name.
	 *
	 * @param name The bean property name.
	 * @return The field number, or <c>-1</c> if no such property.
	 */
	public int fieldNumber(String name) {
		var e = entriesByName.get(name);
		return e == null ? -1 : e.fieldNumber();
	}

	/**
	 * Returns the field entry for the specified property name.
	 *
	 * @param name The bean property name.
	 * @return The field entry, or <jk>null</jk> if no such property.
	 */
	public ProtobufFieldEntry entryForName(String name) {
		return entriesByName.get(name);
	}

	/**
	 * Returns the field entry for the specified field number.
	 *
	 * @param fieldNumber The protobuf field number.
	 * @return The field entry, or <jk>null</jk> if no field has that number.
	 */
	public ProtobufFieldEntry entryFor(int fieldNumber) {
		return entriesByNumber.get(fieldNumber);
	}

	/**
	 * Returns all field entries, ordered by field number (deterministic serialization order).
	 *
	 * @return An unmodifiable, field-number-ordered list of entries.
	 */
	public List<ProtobufFieldEntry> entries() {
		return entries;
	}
}
