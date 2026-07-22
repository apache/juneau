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
package org.apache.juneau.marshall.json;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonSupport">JSON Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource",   // Resource management handled externally
	"rawtypes",   // Raw types necessary for generic collection/map serialization throughout this session
	"unchecked",  // Type erasure requires unchecked casts throughout this session
	"java:S110",  // Inheritance depth acceptable for this class hierarchy
	"java:S115"   // Constants use UPPER_snakeCase naming convention
})
public class JsonSerializerSession extends WriterSerializerSession implements TokenWritable, ArrayRecordWritable {

	// Property name constants
	private static final String PROP_escapeSolidus = "escapeSolidus";
	private static final String PROP_JsonSerializerSession_escapeSolidus = "JsonSerializerSession.escapeSolidus";

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends WriterSerializerSession.Builder<SELF> {

		private boolean escapeSolidus;
		private JsonSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(JsonSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
			escapeSolidus = ctx.isEscapeSolidus();
		}

		@Override
		public JsonSerializerSession build() {
			return new JsonSerializerSession(this);
		}

		/**
		 * Prefix solidus characters with escapes.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public SELF escapeSolidus(boolean value) {
			escapeSolidus = value;
			return self();
		}

		@Override /* Overridden from Builder */
		public SELF property(String key, Object value) {
			if (key == null) { super.property(key, value); return self(); }
			switch (key) {
				case PROP_escapeSolidus, PROP_JsonSerializerSession_escapeSolidus:
					return escapeSolidus(cvt(value, Boolean.class));
				default:
					super.property(key, value);
					return self();
			}
		}

	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@code create()} path (CRTP terminal).
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder(JsonSerializer ctx) {
			super(ctx);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers use it to construct session instances polymorphically
	})
	public static Builder<?> create(JsonSerializer ctx) {
		return new DefaultBuilder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final JsonSerializer ctx;
	private final boolean escapeSolidus;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsonSerializerSession(Builder<?> builder) {
		super(builder);
		this.ctx = builder.ctx;
		escapeSolidus = builder.escapeSolidus;
	}

	@SuppressWarnings({
		"java:S1452" // Returns the passed-in writer whose concrete CRTP self-type is not nameable here.
	})
	protected SerializerWriter<?> writeBeanMap(JsonWriter<?> out, BeanMap<?> m, String typeName) throws SerializeException {
		int i = indent;
		out.w('{');

		var addComma = Flag.create();
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);

		if (nn(typeName)) {
			BeanPropertyMeta pm = m.getMeta().getTypeProperty();
			out.cr(i).attr(pm.getName()).w(':').s(i).stringValue(typeName);
			addComma.set();
		}

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			var cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);

			if (canIgnoreValue(pMeta, key, value))
				return;

			addComma.ifSet(() -> out.append(',').smi(i)).set();

			out.cr(i).attr(key).w(':').s(i);

			writeAnything(out, value, cMeta, key, pMeta);
		});

		out.cre(i - 1).w('}');
		return out;
	}

	
	private SerializerWriter<?> writeCollection(JsonWriter<?> out, Collection c, ClassMeta<?> type) throws SerializeException {

		var elementType = type.getElementType();

		out.w('[');
		var addComma = Flag.create();
		forEachEntry(c, x -> {
			addComma.ifSet(() -> out.w(',').smi(indent)).set();
			out.cr(indent);
			writeAnything(out, x, elementType, "<iterator>", null);
		});

		out.cre(indent - 1).w(']');
		return out;
	}

	private SerializerWriter<?> writeStreamable(JsonWriter<?> out, Object o, ClassMeta<?> sType, ClassMeta<?> type) throws SerializeException {
		var elementType = type.getElementType();

		out.w('[');
		var addComma = Flag.create();
		forEachStreamableEntry(o, sType, x -> {
			addComma.ifSet(() -> out.w(',').smi(indent)).set();
			out.cr(indent);
			writeAnything(out, x, elementType, "<iterator>", null);
		});

		out.cre(indent - 1).w(']');
		return out;
	}

	
	@SuppressWarnings({
		"java:S1452" // Returns the passed-in writer whose concrete CRTP self-type is not nameable here.
	})
	protected SerializerWriter<?> writeMap(JsonWriter<?> out, Map m, ClassMeta<?> type) throws SerializeException {

		var keyType = type.getKeyType();
		var valueType = type.getValueType();

		int i = indent;
		out.w('{');

		var addComma = Flag.create();
		forEachEntry(m, x -> {
			addComma.ifSet(() -> out.w(',').smi(i)).set();
			Object value = x.getValue();
			Object key = generalize(x.getKey(), keyType);
			out.cr(i).attr(toString(key)).w(':').s(i);
			writeAnything(out, value, valueType, (key == null ? null : toString(key)), null);
		});

		out.cre(i - 1).w('}');

		return out;
	}

	@Override /* Overridden from SerializerSesssion */
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {
		writeAnything(getJsonWriter(out).i(getInitialDepth()), o, getExpectedRootType(o), "root", null);
	}

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected JsonClassMeta getJsonClassMeta(ClassMeta<?> cm) {
		return ctx.getJsonClassMeta(cm);
	}

	/**
	 * Converts the specified output target object to an {@link JsonWriter}.
	 *
	 * @param out The output target object.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return The output target object wrapped in an {@link JsonWriter}.
	 */
	@SuppressWarnings({
		"java:S1452" // Raw output may be any JsonWriter CRTP leaf; the concrete self-type is not nameable here.
	})
	protected JsonWriter<?> getJsonWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof JsonWriter<?> output2)
			return output2;
		var w = new BasicJsonWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isEscapeSolidus(), getQuoteChar(), false, isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	@Override
	public final boolean isAddBeanTypes() { return super.isAddBeanTypes(); }

	/**
	 * Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * @see JsonSerializer.Builder#escapeSolidus()
	 * @return
	 * 	<jk>true</jk> if solidus (e.g. slash) characters should be escaped.
	 */
	protected final boolean isEscapeSolidus() { return escapeSolidus; }

	/**
	 * Whether the token writer emits unquoted (simple) attribute names.
	 *
	 * @return <jk>false</jk> for RFC-8259 strict JSON; JSON5 overrides to <jk>true</jk>.
	 */
	protected boolean isSimpleAttrs() { return false; }

	/**
	 * Opens a low-level push generator that emits JSON one structural event at a time, bound to
	 * this session's live config.
	 *
	 * <p>
	 * The writer is purely structural &mdash; object swaps and {@code @Schema} annotations are not
	 * applied at this layer.  See {@link TokenWriter} for the contract.  The higher-level
	 * {@link TokenWriter#object(Object)} bridge walks an arbitrary POJO via {@link PojoWalker} and
	 * DOES honor the databind settings (<c>keepNullProperties</c>, <c>trimEmptyMaps</c>,
	 * <c>trimEmptyCollections</c>, <c>sortMaps</c>, <c>sortCollections</c>, <c>trimStrings</c>).
	 *
	 * @param output The output.  Accepts {@link Writer} or {@link OutputStream}.
	 * @return A new {@link JsonTokenWriter}.
	 * @throws IOException If the output type is not supported or could not be opened.
	 */
	@Override /* TokenWritable */
	public TokenWriter writeTokens(Object output) throws IOException {
		var walk = new PojoWalker.Options(
			isKeepNullProperties(),
			isTrimEmptyMaps(),
			isTrimEmptyCollections(),
			isSortMaps(),
			isSortCollections(),
			isTrimStrings(),
			getMarshallingContext());
		var settings = new JsonTokenWriter.Settings(
			isUseWhitespace(),
			getMaxIndent(),
			getQuoteChar(),
			isEscapeSolidus(),
			isTrimStrings(),
			isSimpleAttrs(),
			walk,
			false /* disableObject */);
		return JsonTokenWriter.forOutput(output, settings);
	}

	/**
	 * Streaming array-element {@link RecordWriter} backed by {@link JsonTokenWriter}.
	 * Memory is O(1) in the array length &mdash; elements are emitted as they arrive.
	 *
	 * @param output The output.
	 * @return A new element-streamed {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* ArrayRecordWritable */
	public RecordWriter writeArrayRecords(Object output) throws IOException {
		return StreamingArrayRecord.writer(writeTokens(output));
	}

	/**
	 * Workhorse method.
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 *
	 * @param out The output writer.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param o The object to serialize.
	 * 	<br>Can be <jk>null</jk> (written as the JSON literal <js>"null"</js>).
	 * @param eType The expected type.
	 * 	<br>Can be <jk>null</jk> (defaults to {@code Object}).
	 * @param attrName The attribute name.
	 * @param pMeta The bean property currently being serialized.
	 * 	<br>Can be <jk>null</jk> (the bean-property-level dictionary lookup and URI detection are skipped).
	 * @return The same writer passed in.
	 * @throws SerializeException General serialization error occurred.
	 */
	
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for this specific logic
		"java:S1452" // Returns the passed-in writer whose concrete CRTP self-type is not nameable here.
	})
	protected JsonWriter<?> writeAnything(JsonWriter<?> out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {

		if (o == null) {
			out.append("null");
			return out;
		}

		if (eType == null)
			eType = object();

		ClassMeta<?> aType = null;			// The actual type
		ClassMeta<?> sType = null;			// The serialized type

		aType = push2(attrName, o, eType);
		boolean isRecursion = aType == null;

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

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

		String wrapperAttr = getJsonClassMeta(sType).getWrapperAttr();
		if (nn(wrapperAttr)) {
			out.w('{').cr(indent).attr(wrapperAttr).w(':').s(indent);
			indent++;
		}

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0)) {
			out.append("null");
		} else if (sType.isBean()) {
			writeBeanMap(out, toBeanMap(o), typeName);
		} else if (sType.isMap()) {
			if (sType.isBeanMap())
				writeBeanMap(out, (BeanMap)o, typeName);
			else
				writeMap(out, (Map)o, eType);
		} else if (sType.isCollection()) {
			writeCollection(out, (Collection)o, eType);
		} else if (sType.isArray()) {
			writeCollection(out, toList(sType.inner(), o), eType);
		} else if (sType.isNumber() || sType.isBoolean()) {
			out.append(o);
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			out.uriValue(o);
		} else if (sType.isDate()) {
			out.stringValue(writeDate((Date)o, sType));
		} else if (sType.isCalendar()) {
			out.stringValue(writeCalendar(o, sType));
		} else if (sType.isTemporal()) {
			out.stringValue(writeTemporal((TemporalAccessor)o, sType));
		} else if (sType.isDuration()) {
			var value = writeDuration((Duration)o);
			if (getDurationFormat().isNumeric())
				out.append(value);
			else
				out.stringValue(value);
		} else if (sType.isPeriod()) {
			out.stringValue(writePeriod((Period)o));
		} else if (sType.isStreamable()) {
			writeStreamable(out, o, sType, eType);
		} else if (sType.isReader()) {
			pipe((Reader)o, out, SerializerSession::handleThrown);
		} else if (sType.isInputStream()) {
			pipe((InputStream)o, out, SerializerSession::handleThrown);
		} else {
			out.stringValue(toString(o));
		}

		if (nn(wrapperAttr)) {
			indent--;
			out.cre(indent - 1).w('}');
		}

		if (! isRecursion)
			pop();
		return out;
	}

	/**
	 * Serializes an object to a JSON string using this session's configuration.
	 *
	 * <p>
	 * Protected extension hook that subclasses can call or override when they need to serialize a
	 * value to JSON (e.g. for embedding a nested JSON fragment).
	 *
	 * @param o The object to serialize.
	 * @return The serialized object.
	 * @throws Exception Error occurred.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method for serialization hooks
	})
	protected String writeJson(Object o) throws Exception {
		var sw = new StringWriter();
		writeAnything(getJsonWriter(createPipe(sw)).i(getInitialDepth()), o, getExpectedRootType(o), "root", null);
		return sw.toString();
	}
}