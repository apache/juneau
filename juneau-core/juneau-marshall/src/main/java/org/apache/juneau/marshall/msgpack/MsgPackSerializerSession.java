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
package org.apache.juneau.marshall.msgpack;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link MsgPackSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MessagePackSupport">MessagePack Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"resource",   // Output streams managed by calling code
	"java:S110",  // Inheritance depth acceptable for serializer session hierarchy
	"java:S115"   // Constants use UPPER_snakeCase convention (e.g., CONST_value)
})
public class MsgPackSerializerSession extends OutputStreamSerializerSession implements TokenWritable, ArrayRecordWritable {

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
		protected Builder(MsgPackSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public MsgPackSerializerSession build() {
			return new MsgPackSerializerSession(this);
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
	public static Builder create(MsgPackSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/*
	 * Converts the specified output target object to an {@link MsgPackOutputStream}.
	 */
	private static MsgPackOutputStream getMsgPackOutputStream(SerializerPipe out) throws IOException {
		Object output = out.getRawOutput();
		if (output instanceof MsgPackOutputStream output2)
			return output2;
		var os = new MsgPackOutputStream(out.getOutputStream());
		out.setOutputStream(os);
		return os;
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MsgPackSerializerSession(Builder builder) {
		super(builder);
	}

	/**
	 * Opens a low-level push generator that emits MessagePack one structural event at a time,
	 * bound to this live session.
	 *
	 * <p>
	 * MsgPack containers are length-prefixed with no indefinite-length encoding, so the writer
	 * <b>buffers</b> each open container's body bytes until the matching {@code endXxx} call;
	 * memory cost is O(largest open container).
	 *
	 * <h5 class='section'>Builder properties honored:</h5>
	 * <c>keepNullProperties</c>, <c>trimEmptyMaps</c>, <c>trimEmptyCollections</c>, <c>sortMaps</c>,
	 * <c>sortCollections</c>, <c>trimStrings</c> &mdash; all flow through to
	 * {@link TokenWriter#object(Object) object(Object)}.
	 *
	 * <h5 class='section'>Builder properties NOT honored:</h5>
	 * <c>useWhitespace</c>, <c>maxIndent</c>, <c>quoteChar</c>, <c>escapeSolidus</c> (binary
	 * format); <c>uriContext</c>, <c>uriResolution</c>, <c>uriRelativity</c>, <c>listener</c>.
	 *
	 * @param output The output.
	 * @return A new {@link MsgPackTokenWriter}.
	 * @throws IOException If the output type is not supported or could not be opened.
	 */
	@Override /* TokenWritable */
	public TokenWriter serializeTokens(Object output) throws IOException {
		var walk = new PojoWalker.Options(
			isKeepNullProperties(),
			isTrimEmptyMaps(),
			isTrimEmptyCollections(),
			isSortMaps(),
			isSortCollections(),
			isTrimStrings(),
			getMarshallingContext());
		return MsgPackTokenWriter.forOutput(output, new MsgPackTokenWriter.Settings(walk));
	}

	/**
	 * Buffered array-element {@link RecordWriter}.  MsgPack's array wire format is
	 * length-prefixed, so streaming without a known element count requires either buffering
	 * (this method) or the {@link #serializeArrayRecords(Object, int)} overload that takes a count.
	 *
	 * @param output The output.
	 * @return A buffered {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* ArrayRecordWritable */
	public RecordWriter serializeArrayRecords(Object output) throws IOException {
		return RecordAdapter.arrayWriter(this, output);
	}

	@Override /* ArrayRecordWritable */
	public boolean isArrayRecordStreaming() { return false; }

	/**
	 * Streaming array-element {@link RecordWriter} that pre-declares the element count and
	 * writes each element directly to the output stream &mdash; no per-element buffering.
	 * The caller must pass exactly {@code expectedCount} {@link RecordWriter#write(Object)}
	 * calls before {@link RecordWriter#close()}; mismatch throws on close.
	 *
	 * @param output The output (must be an {@link OutputStream}).
	 * @param expectedCount The number of elements that will be written.
	 * @return A streaming {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* ArrayRecordWritable */
	public RecordWriter serializeArrayRecords(Object output, int expectedCount) throws IOException {
		if (!(output instanceof OutputStream))
			throw new IOException("MsgPack streaming arrayRecordWriter requires an OutputStream");
		var os = (OutputStream) output;
		var mpos = new MsgPackOutputStream(os);
		mpos.startArray(expectedCount);
		return new RecordWriter() {
			private int written;
			private boolean closed;

			@Override public RecordWriter write(Object value) throws IOException {
				if (closed)
					throw new IllegalStateException("Array stream is closed.");
				if (written >= expectedCount)
					throw new IllegalStateException(
						"Array stream is full; declared " + expectedCount + " elements, attempted to write more.");
				try {
					MsgPackSerializerSession.this.serialize(value, os);
				} catch (SerializeException e) {
					throw new IOException(e);
				}
				written++;
				return this;
			}
			@Override public boolean isStreaming() {
				return true;
			}
			@Override public void flush() throws IOException {
				os.flush();
			}
			@Override public void close() throws IOException {
				if (closed)
					return;
				closed = true;
				if (written != expectedCount)
					throw new IOException(
						"Array stream count mismatch: declared " + expectedCount + " but wrote " + written);
				os.close();
			}
		};
	}

	/*
	 * Workhorse method.
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 */
	@SuppressWarnings({
		"rawtypes",   // Raw types necessary for generic type handling
		"java:S3776"  // Cognitive complexity acceptable for serialization dispatch logic
	})
	private MsgPackOutputStream serializeAnything(MsgPackOutputStream out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {

		if (o == null)
			return out.appendNull();

		if (eType == null)
			eType = object();

		ClassMeta<?> aType = null;			// The actual type
		ClassMeta<?> sType = null;			// The serialized type

		aType = push2(attrName, o, eType);

		// Handle recursion
		if (aType == null)
			return out.appendNull();

		// Handle Optional<X>
		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}

		sType = aType;
		String typeName = getBeanTypeName(this, eType, aType, pMeta);

		// Swap if necessary
		var swap = aType.getSwap(this);
		if (nn(swap)) {
			o = swap(swap, o);
			sType = swap.getSwapClassMeta(this);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0))
			out.appendNull();
		else if (sType.isBean())
			serializeBeanMap(out, toBeanMap(o), typeName);
		else if (sType.isMap()) {
			if (sType.isBeanMap())
				serializeBeanMap(out, (BeanMap)o, typeName);
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
			// MsgPack protocol requires array size in header (startArray(size)), so materialization is unavoidable.
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

	private void serializeBeanMap(MsgPackOutputStream out, BeanMap<?> m, String typeName) throws SerializeException {

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
				return; // Must handle the case where recursion occurs and property is not serialized.
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
	private void serializeCollection(MsgPackOutputStream out, Collection c, ClassMeta<?> type) throws SerializeException {
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
	private void serializeMap(MsgPackOutputStream out, Map m, ClassMeta<?> type) throws SerializeException {

		var keyType = type.getKeyType();
		var valueType = type.getValueType();

		m = sort(m);

		// The map size may change as we're iterating over it, so
		// grab a snapshot of the entries in a separate list.
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
		serializeAnything(getMsgPackOutputStream(out), o, getExpectedRootType(o), "root", null);
	}

	@Override
	public boolean isAddBeanTypes() { return super.isAddBeanTypes(); }

	private void appendDuration(MsgPackOutputStream out, Duration value) {
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