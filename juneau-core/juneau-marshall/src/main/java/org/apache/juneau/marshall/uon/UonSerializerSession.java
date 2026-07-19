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
package org.apache.juneau.marshall.uon;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.conversion.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session object that lives for the duration of a single use of {@link UonSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UonSupport">UON Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"resource", // Resource management handled externally
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115" // Constants use UPPER_snakeCase naming convention
})
public class UonSerializerSession extends WriterSerializerSession implements HttpPartSerializerSession, RecordWritable {

	// Property name constants
	private static final String PROP_encoding = "encoding";
	private static final String PROP_paramFormat = "paramFormat";
	private static final String PROP_UonSerializerSession_encoding = "UonSerializerSession.encoding";
	private static final String PROP_UonSerializerSession_paramFormat = "UonSerializerSession.paramFormat";

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends WriterSerializerSession.Builder<SELF> {

		private boolean encoding;
		private ParamFormat paramFormat;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(UonSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			encoding = ctx.isEncoding();
			paramFormat = ctx.getParamFormat();
		}

		@Override
		public UonSerializerSession build() {
			return new UonSerializerSession(this);
		}

		/**
		 * Encode non-valid URI characters.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public SELF encoding(boolean value) {
			encoding = value;
			return self();
		}

		/**
		 * Format to use for query/form-data/header values.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public SELF paramFormat(ParamFormat value) {
			paramFormat = value;
			return self();
		}

		@Override /* Overridden from Builder */
		public SELF property(String key, Object value) {
			if (key == null) { super.property(key, value); return self(); }
			switch (key) {
				case PROP_encoding, PROP_UonSerializerSession_encoding:
					return encoding(cvt(value, Boolean.class));
				case PROP_paramFormat, PROP_UonSerializerSession_paramFormat:
					return paramFormat(cvt(value, ParamFormat.class));
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

		DefaultBuilder(UonSerializer ctx) {
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
	public static Builder<?> create(UonSerializer ctx) {
		return new DefaultBuilder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final boolean encoding;
	private final ParamFormat paramFormat;
	private final boolean plainTextParams;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public UonSerializerSession(Builder<?> builder) {
		super(builder);
		encoding = builder.encoding;
		paramFormat = builder.paramFormat;
		plainTextParams = paramFormat == ParamFormat.PLAINTEXT;
	}

	@Override /* RecordWritable */
	public RecordWriter writeRecords(Object output) throws IOException {
		return RecordAdapter.writer(this, output);
	}

	@Override /* RecordWritable */
	public boolean isRecordStreaming() {
		return false;
	}

	@Override /* Overridden from HttpPartSerializer */
	public String write(HttpPartType type, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
		try {
			// Shortcut for simple types.
			var cm = getClassMetaForObject(value);
			if (nn(cm) && (schema == null || schema.getType() == HttpPartDataType.NO_TYPE)) {
				if (cm.isNumber() || cm.isBoolean())
					return BasicConverter.INSTANCE.to(value, String.class);
				if (cm.isString()) {
					var s = BasicConverter.INSTANCE.to(value, String.class);
					if (s.isEmpty() || ! UonUtils.needsQuotes(s))
						return s;
				}
			}
			var w = new StringWriter();
			writeAnything(getUonWriter(w).i(getInitialDepth()), value, getExpectedRootType(value), "root", null);
			return w.toString();
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	private final UonWriter getUonWriter(Writer out) {
		return new UonWriter(this, out, isUseWhitespace(), getMaxIndent(), isEncoding(), isTrimStrings(), plainTextParams, getQuoteChar(), getUriResolver());
	}

	private SerializerWriter writeBeanMap(UonWriter out, BeanMap<?> m, String typeName) throws SerializeException {

		if (! plainTextParams)
			out.append('(');

		var addComma = Flag.create();

		if (nn(typeName)) {
			var pm = m.getMeta().getTypeProperty();
			out.cr(indent).appendObject(pm.getName(), false).append('=').appendObject(typeName, false);
			addComma.set();
		}

		var checkNull = (Predicate<Object>)(x -> isKeepNullProperties() || nn(x));
		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			var cMeta = (ClassMeta<?>) pMeta.getBeanInfo();

			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);

			if (canIgnoreValue(pMeta, key, value))
				return;

			addComma.ifSet(() -> out.append(',')).set();

			out.cr(indent).appendObject(key, false).append('=');

			writeAnything(out, value, cMeta, key, pMeta);
		});

		if (m.size() > 0)
			out.cre(indent - 1);
		if (! plainTextParams)
			out.append(')');

		return out;
	}

	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic collection/map serialization
		"unchecked", // Type erasure requires unchecked casts in collection/map serialization
	})
	private SerializerWriter writeCollection(UonWriter out, Collection c, ClassMeta<?> type) throws SerializeException {

		var elementType = type.getElementType();

		if (! plainTextParams)
			out.append('@').append('(');

		var addComma = Flag.create();
		forEachEntry(c, x -> {
			addComma.ifSet(() -> out.append(',')).set();
			out.cr(indent);
			writeAnything(out, x, elementType, "<iterator>", null);
		});

		addComma.ifSet(() -> out.cre(indent - 1));
		if (! plainTextParams)
			out.append(')');

		return out;
	}

	private SerializerWriter writeStreamable(UonWriter out, Object o, ClassMeta<?> sType, ClassMeta<?> type) throws SerializeException {

		var elementType = type.getElementType();

		if (! plainTextParams)
			out.append('@').append('(');

		var addComma = Flag.create();
		forEachStreamableEntry(o, sType, x -> {
			addComma.ifSet(() -> out.append(',')).set();
			out.cr(indent);
			writeAnything(out, x, elementType, "<iterator>", null);
		});

		addComma.ifSet(() -> out.cre(indent - 1));
		if (! plainTextParams)
			out.append(')');

		return out;
	}

	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic collection/map serialization
		"unchecked", // Type erasure requires unchecked casts in collection/map serialization
	})
	private SerializerWriter writeMap(UonWriter out, Map m, ClassMeta<?> type) throws SerializeException {

		var keyType = type.getKeyType();
		var valueType = type.getValueType();

		if (! plainTextParams)
			out.append('(');

		var addComma = Flag.create();
		forEachEntry(m, x -> {
			addComma.ifSet(() -> out.append(',')).set();
			var value = x.getValue();
			var key = generalize(x.getKey(), keyType);
			out.cr(indent).appendObject(key, false).append('=');
			writeAnything(out, value, valueType, toString(key), null);
		});

		addComma.ifSet(() -> out.cre(indent - 1));

		if (! plainTextParams)
			out.append(')');

		return out;
	}

	@Override /* Overridden from Serializer */
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {
		writeAnything(getUonWriter(out).i(getInitialDepth()), o, getExpectedRootType(o), "root", null);
	}

	/**
	 * Format to use for query/form-data/header values.
	 *
	 * @see UonSerializer.Builder#paramFormat(ParamFormat)
	 * @return
	 * 	Specifies the format to use for URL GET parameter keys and values.
	 */
	protected final ParamFormat getParamFormat() { return paramFormat; }

	/**
	 * Converts the specified output target object to an {@link UonWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in an {@link UonWriter}.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected final UonWriter getUonWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof UonWriter output2)
			return output2;
		var w = new UonWriter(this, out.getWriter(), isUseWhitespace(), getMaxIndent(), isEncoding(), isTrimStrings(), plainTextParams, getQuoteChar(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	@Override
	public final boolean isAddBeanTypes() { return super.isAddBeanTypes(); }

	/**
	 * Encode non-valid URI characters.
	 *
	 * @see UonSerializer.Builder#encoding()
	 * @return
	 * 	<jk>true</jk> if non-valid URI characters should be encoded with <js>"%xx"</js> constructs.
	 */
	protected final boolean isEncoding() { return encoding; }

	/**
	 * Workhorse method.
	 *
	 * <p>
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 *
	 * @param out The writer to serialize to.
	 * @param o The object being serialized.
	 * @param eType The expected type of the object if this is a bean property.
	 * @param attrName
	 * 	The bean property name if this is a bean property.
	 * 	<jk>null</jk> if this isn't a bean property being serialized.
	 * @param pMeta The bean property metadata.
	 * @return The same writer passed in.
	 * @throws SerializeException Generic serialization error occurred.
	 */
	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic type handling
		"java:S3776", // Cognitive complexity acceptable for this specific logic
	})
	protected SerializerWriter writeAnything(UonWriter out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {

		if (o == null) {
			out.appendObject(null, false);
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
		var typeName = getBeanTypeName(this, eType, aType, pMeta);

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
			out.appendObject(null, false);
		else if (sType.isBean())
			writeBeanMap(out, toBeanMap(o), typeName);
		else if (sType.isMap()) {
			if (sType.isBeanMap())
				writeBeanMap(out, (BeanMap)o, typeName);
			else
				writeMap(out, (Map)o, eType);
		} else if (sType.isCollection()) {
			writeCollection(out, (Collection)o, eType);
		} else if (sType.isArray()) {
			writeCollection(out, toList(sType.inner(), o), eType);
		} else if (sType.isBoolean()) {
			out.appendBoolean(o);
		} else if (sType.isNumber()) {
			out.appendNumber(o);
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			out.appendUri(o);
		} else if (sType.isDate()) {
			out.appendObject(writeDate((Date)o, sType), false);
		} else if (sType.isCalendar()) {
			out.appendObject(writeCalendar(o, sType), false);
		} else if (sType.isTemporal()) {
			out.appendObject(writeTemporal((TemporalAccessor)o, sType), false);
		} else if (sType.isDuration()) {
			out.appendObject(writeDuration((Duration)o), false);
		} else if (sType.isPeriod()) {
			out.appendObject(writePeriod((Period)o), false);
		} else if (sType.isStreamable()) {
			writeStreamable(out, o, sType, eType);
		} else if (sType.isReader()) {
			pipe((Reader)o, out, SerializerSession::handleThrown);
		} else if (sType.isInputStream()) {
			pipe((InputStream)o, out, SerializerSession::handleThrown);
		} else {
			out.appendObject(o, false);
		}

		if (! isRecursion)
			pop();
		return out;
	}
}