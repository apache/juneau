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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link CborSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/CborBasics">CBOR Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource",   // Output streams managed by calling code
	"java:S110",  // Inheritance depth acceptable for serializer session hierarchy
	"java:S115"   // Constants use UPPER_snakeCase convention (e.g., CONST_value)
})
public class CborSerializerSession extends OutputStreamSerializerSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializerSession.Builder<Builder> {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(CborSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public CborSerializerSession build() {
			return new CborSerializerSession(this);
		}

	}

	private static class SimpleMapEntry {
		final Object key;
		final Object value;

		SimpleMapEntry(Object key, Object value) {
			this.key = key;
			this.value = value;
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(CborSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/*
	 * Converts the specified output target object to an {@link CborOutputStream}.
	 */
	private static CborOutputStream getCborOutputStream(SerializerPipe out) throws IOException {
		Object output = out.getRawOutput();
		if (output instanceof CborOutputStream output2)
			return output2;
		var os = new CborOutputStream(out.getOutputStream());
		out.setOutputStream(os);
		return os;
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected CborSerializerSession(Builder builder) {
		super(builder);
	}

	/*
	 * Workhorse method.
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 */
	@SuppressWarnings({
		"rawtypes",   // Raw types necessary for generic type handling
		"java:S3776"  // Cognitive complexity acceptable for serialization dispatch logic
	})
	private CborOutputStream serializeAnything(CborOutputStream out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {

		if (o == null)
			return out.appendNull();

		if (eType == null)
			eType = object();

		ClassMeta<?> aType = null;
		ClassMeta<?> sType = null;

		aType = push2(attrName, o, eType);

		if (aType == null)
			return out.appendNull();

		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}

		sType = aType;
		String typeName = getBeanTypeName(this, eType, aType, pMeta);

		var swap = aType.getSwap(this);
		if (nn(swap)) {
			o = swap(swap, o);
			sType = swap.getSwapClassMeta(this);

			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}

		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0))
			out.appendNull();
		else if (sType.isBean())
			serializeBeanMap(out, toBeanMap(o), typeName);
		else if (sType.isMap()) {
			if (o instanceof BeanMap o2)
				serializeBeanMap(out, o2, typeName);
			else
				serializeMap(out, (Map)o, eType);
		} else if (sType.isCollection()) {
			serializeCollection(out, (Collection)o, eType);
		} else if (sType.isByteArray()) {
			out.appendBinary((byte[])o);
		} else if (sType.isArray()) {
			serializeCollection(out, toList(sType.inner(), o), eType);
		} else if (sType.isBoolean()) {
			out.appendBoolean((Boolean)o);
		} else if (sType.isNumber()) {
			out.appendNumber((Number)o);
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			out.appendString(resolveUri(o.toString()));
		} else if (sType.isDate()) {
			out.appendString(serializeDate((Date)o, sType));
		} else if (sType.isCalendar()) {
			out.appendString(serializeCalendar(o, sType));
		} else if (sType.isTemporal()) {
			out.appendString(serializeTemporal((TemporalAccessor)o, sType));
		} else if (sType.isDuration()) {
			appendDuration(out, (Duration)o);
		} else if (sType.isPeriod()) {
			out.appendString(serializePeriod((Period)o));
		} else if (sType.isStreamable()) {
			serializeCollection(out, toListFromStreamable(o, sType), eType);
		} else if (sType.isReader()) {
			pipe((Reader)o, out, SerializerSession::handleThrown);
		} else if (sType.isInputStream()) {
			pipe((InputStream)o, out, SerializerSession::handleThrown);
		} else
			out.appendString(toString(o));

		pop();
		return out;
	}

	private void serializeBeanMap(CborOutputStream out, BeanMap<?> m, String typeName) throws SerializeException {

		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);

		var values = new ArrayList<BeanPropertyValue>();

		if (nn(typeName)) {
			BeanPropertyMeta pm = m.getMeta().getTypeProperty();
			values.add(new BeanPropertyValue(pm, pm.getName(), typeName, null));
		}

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			if (nn(thrown)) {
				onBeanGetterException(pMeta, thrown);
				return;
			}
			var p = new BeanPropertyValue(pMeta, key, value, null);

			if ((! isKeepNullProperties()) && willRecurse(p)) {
				return;
			}

			values.add(p);
		});

		out.startMap(values.size());

		values.forEach(x -> {
			BeanPropertyMeta pMeta = x.getMeta();
			if (pMeta.canRead()) {
				ClassMeta<?> cMeta = (ClassMeta<?>) x.getBeanInfo();
				String key = x.getName();
				Object value = x.getValue();
				serializeAnything(out, key, null, null, null);
				serializeAnything(out, value, cMeta, key, pMeta);
			}
		});
	}

	@SuppressWarnings({
		"rawtypes",  // Raw types necessary for generic collection handling
		"unchecked"  // Type erasure requires unchecked operations
	})
	private void serializeCollection(CborOutputStream out, Collection c, ClassMeta<?> type) throws SerializeException {
		var elementType = type.getElementType();
		List<Object> l = listOfSize(c.size());
		c = sort(c);
		l.addAll(c);
		out.startArray(l.size());
		l.forEach(x -> serializeAnything(out, x, elementType, "<iterator>", null));
	}

	@SuppressWarnings({
		"rawtypes",  // Raw types necessary for generic map handling
		"unchecked"  // Type erasure requires unchecked operations
	})
	private void serializeMap(CborOutputStream out, Map m, ClassMeta<?> type) throws SerializeException {

		var keyType = type.getKeyType();
		var valueType = type.getValueType();

		m = sort(m);

		List<SimpleMapEntry> entries = listOfSize(m.size());
		m.forEach((k, v) -> entries.add(new SimpleMapEntry(k, v)));

		out.startMap(entries.size());

		entries.forEach(x -> {
			Object value = x.value;
			Object key = generalize(x.key, keyType);
			serializeAnything(out, key, keyType, null, null);
			serializeAnything(out, value, valueType, null, null);
		});
	}

	private boolean willRecurse(BeanPropertyValue v) throws SerializeException {
		var aType = push2(v.getName(), v.getValue(), (ClassMeta<?>) v.getBeanInfo());
		if (nn(aType))
			pop();
		return aType == null;
	}

	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		serializeAnything(getCborOutputStream(out), o, getExpectedRootType(o), "root", null);
	}

	private void appendDuration(CborOutputStream out, Duration value) {
		var f = getDurationFormat();
		var s = serializeDuration(value);
		if (f == DurationFormat.NANOS || f == DurationFormat.MILLIS)
			out.appendNumber(Long.parseLong(s));
		else if (f == DurationFormat.SECONDS)
			out.appendNumber(Double.parseDouble(s));
		else
			out.appendString(s);
	}

}
