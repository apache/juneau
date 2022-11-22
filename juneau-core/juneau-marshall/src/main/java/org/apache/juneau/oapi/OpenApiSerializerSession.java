// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.oapi;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartCollectionFormat.*;
import static org.apache.juneau.httppart.HttpPartDataType.*;
import static org.apache.juneau.httppart.HttpPartFormat.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.time.temporal.*;
import java.util.*;
import java.util.Date;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;
import org.apache.juneau.uon.*;

/**
 * Session object that lives for the duration of a single use of {@link OpenApiSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.OpenApiDetails">OpenAPI Details</a>
 * </ul>
 */
public class OpenApiSerializerSession extends UonSerializerSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	// Cache these for faster lookup
	private static final BeanContext BC = BeanContext.DEFAULT;
	private static final ClassMeta<byte[]> CM_ByteArray = BC.getClassMeta(byte[].class);
	private static final ClassMeta<String[]> CM_StringArray = BC.getClassMeta(String[].class);
	private static final ClassMeta<Calendar> CM_Calendar = BC.getClassMeta(Calendar.class);
	private static final ClassMeta<Long> CM_Long = BC.getClassMeta(Long.class);
	private static final ClassMeta<Integer> CM_Integer = BC.getClassMeta(Integer.class);
	private static final ClassMeta<Double> CM_Double = BC.getClassMeta(Double.class);
	private static final ClassMeta<Float> CM_Float = BC.getClassMeta(Float.class);
	private static final ClassMeta<Boolean> CM_Boolean = BC.getClassMeta(Boolean.class);

	private static final HttpPartSchema DEFAULT_SCHEMA = HttpPartSchema.DEFAULT;

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(OpenApiSerializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends UonSerializerSession.Builder {

		OpenApiSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(OpenApiSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public OpenApiSerializerSession build() {
			return new OpenApiSerializerSession(this);
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder useWhitespace(Boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonSerializerSession.Builder */
		public Builder encoding(boolean value) {
			super.encoding(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final OpenApiSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected OpenApiSerializerSession(Builder builder) {
		super(builder.encoding(false));
		ctx = builder.ctx;
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		try {
			out.getWriter().write(serialize(HttpPartType.BODY, getSchema(), o));
		} catch (SchemaValidationException e) {
			throw new SerializeException(e);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override /* PartSerializer */
	public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {

		ClassMeta<?> type = getClassMetaForObject(value);
		if (type == null)
			type = object();

		// Swap if necessary
		ObjectSwap swap = type.getSwap(this);
		if (swap != null && ! type.isDateOrCalendarOrTemporal()) {
			value = swap(swap, value);
			type = swap.getSwapClassMeta(this);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (type.isObject())
				type = getClassMetaForObject(value);
		}

		schema = ObjectUtils.firstNonNull(schema, DEFAULT_SCHEMA);

		HttpPartDataType t = schema.getType(type);

		HttpPartFormat f = schema.getFormat(type);
		if (f == HttpPartFormat.NO_FORMAT)
			f = ctx.getFormat();

		HttpPartCollectionFormat cf = schema.getCollectionFormat();
		if (cf == HttpPartCollectionFormat.NO_COLLECTION_FORMAT)
			cf = ctx.getCollectionFormat();

		String out = null;

		schema.validateOutput(value, ctx.getBeanContext());

		if (type.hasMutaterTo(schema.getParsedType()) || schema.getParsedType().hasMutaterFrom(type)) {
			value = toType(value, schema.getParsedType());
			type = schema.getParsedType();
		}

		if (type.isUri()) {
			value = getUriResolver().resolve(value);
			type = string();
		}

		if (value != null) {

			if (t == STRING) {

				if (f == BYTE) {
					out = base64Encode(toType(value, CM_ByteArray));
				} else if (f == BINARY) {
					out = toHex(toType(value, CM_ByteArray));
				} else if (f == BINARY_SPACED) {
					out = toSpacedHex(toType(value, CM_ByteArray));
				} else if (f == DATE) {
					try {
						if (value instanceof Calendar)
							out = TemporalCalendarSwap.IsoDate.DEFAULT.swap(this, (Calendar)value);
						else if (value instanceof Date)
							out = TemporalDateSwap.IsoDate.DEFAULT.swap(this, (Date)value);
						else if (value instanceof Temporal)
							out = TemporalSwap.IsoDate.DEFAULT.swap(this, (Temporal)value);
						else
							out = value.toString();
					} catch (Exception e) {
						throw new SerializeException(e);
					}
				} else if (f == DATE_TIME) {
					try {
						if (value instanceof Calendar)
							out = TemporalCalendarSwap.IsoInstant.DEFAULT.swap(this, (Calendar)value);
						else if (value instanceof Date)
							out = TemporalDateSwap.IsoInstant.DEFAULT.swap(this, (Date)value);
						else if (value instanceof Temporal)
							out = TemporalSwap.IsoInstant.DEFAULT.swap(this, (Temporal)value);
						else
							out = value.toString();
					} catch (Exception e) {
						throw new SerializeException(e);
					}
				} else if (f == HttpPartFormat.UON) {
					out = super.serialize(partType, schema, value);
				} else {
					out = toType(value, string());
				}

			} else if (t == BOOLEAN) {

				out = stringify(toType(value, CM_Boolean));

			} else if (t == INTEGER) {

				if (f == INT64)
					out = stringify(toType(value, CM_Long));
				else
					out = stringify(toType(value, CM_Integer));

			} else if (t == NUMBER) {

				if (f == DOUBLE)
					out = stringify(toType(value, CM_Double));
				else
					out = stringify(toType(value, CM_Float));

			} else if (t == ARRAY) {

				if (cf == HttpPartCollectionFormat.UONC)
					out = super.serialize(partType, null, toList(partType, type, value, schema));
				else {

					HttpPartSchema items = schema.getItems();
					ClassMeta<?> vt = getClassMetaForObject(value);
					OapiStringBuilder sb = new OapiStringBuilder(cf);

					if (type.isArray()) {
						for (int i = 0; i < Array.getLength(value); i++)
							sb.append(serialize(partType, items, Array.get(value, i)));
					} else if (type.isCollection()) {
						((Collection<?>)value).forEach(x -> sb.append(serialize(partType, items, x)));
					} else if (vt.hasMutaterTo(String[].class)) {
						String[] ss = toType(value, CM_StringArray);
						for (int i = 0; i < ss.length; i++)
							sb.append(serialize(partType, items, ss[i]));
					} else {
						throw new SerializeException("Input is not a valid array type: " + type);
					}

					out = sb.toString();
				}

			} else if (t == OBJECT) {

				if (cf == HttpPartCollectionFormat.UONC) {
					if (schema.hasProperties() && type.isMapOrBean())
						value = toMap(partType, type, value, schema);
					out = super.serialize(partType, null, value);

				} else if (type.isBean()) {
					OapiStringBuilder sb = new OapiStringBuilder(cf);
					Predicate<Object> checkNull = x -> isKeepNullProperties() || x != null;
					HttpPartSchema schema2 = schema;

					toBeanMap(value).forEachValue(checkNull, (pMeta,key,val,thrown) -> {
						if (thrown == null)
							sb.append(key, serialize(partType, schema2.getProperty(key), val));
					});
					out = sb.toString();

				} else if (type.isMap()) {
					OapiStringBuilder sb = new OapiStringBuilder(cf);
					HttpPartSchema schema2 = schema;
					((Map<?,?>)value).forEach((k,v) -> sb.append(k, serialize(partType, schema2.getProperty(stringify(k)), v)));
					out = sb.toString();

				} else {
					throw new SerializeException("Input is not a valid object type: " + type);
				}

			} else if (t == FILE) {
				throw new SerializeException("File part not supported.");

			} else if (t == NO_TYPE) {
				// This should never be returned by HttpPartSchema.getType(ClassMeta).
				throw new SerializeException("Invalid type.");
			}
		}

		schema.validateInput(out);
		if (out == null)
			out = schema.getDefault();
		if (out == null)
			out = "null";
		return out;
	}

	private static class OapiStringBuilder {
		static final AsciiSet EQ = AsciiSet.create("=\\");
		static final AsciiSet PIPE = AsciiSet.create("|\\");
		static final AsciiSet PIPE_OR_EQ = AsciiSet.create("|=\\");
		static final AsciiSet COMMA = AsciiSet.create(",\\");
		static final AsciiSet COMMA_OR_EQ = AsciiSet.create(",=\\");

		private final StringBuilder sb = new StringBuilder();
		private final HttpPartCollectionFormat cf;
		private boolean first = true;

		OapiStringBuilder(HttpPartCollectionFormat cf) {
			this.cf = cf;
		}

		private void delim(HttpPartCollectionFormat cf) {
			if (cf == PIPES)
				sb.append('|');
			else if (cf == SSV)
				sb.append(' ');
			else if (cf == TSV)
				sb.append('\t');
			else
				sb.append(',');
		}

		OapiStringBuilder append(Object o) {
			if (! first)
				delim(cf);
			first = false;
			if (cf == PIPES)
				sb.append(escapeChars(stringify(o), PIPE));
			else if (cf == SSV || cf == TSV)
				sb.append(stringify(o));
			else
				sb.append(escapeChars(stringify(o), COMMA));
			return this;
		}

		OapiStringBuilder append(Object key, Object val) {
			if (! first)
				delim(cf);
			first = false;
			if (cf == PIPES)
				sb.append(escapeChars(stringify(key), PIPE_OR_EQ)).append('=').append(escapeChars(stringify(val), PIPE_OR_EQ));
			else if (cf == SSV || cf == TSV)
				sb.append(escapeChars(stringify(key), EQ)).append('=').append(escapeChars(stringify(val), EQ));
			else
				sb.append(escapeChars(stringify(key), COMMA_OR_EQ)).append('=').append(escapeChars(stringify(val), COMMA_OR_EQ));
			return this;
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}

	private Map<String,Object> toMap(HttpPartType partType, ClassMeta<?> type, Object o, HttpPartSchema s) throws SerializeException, SchemaValidationException {
		if (s == null)
			s = DEFAULT_SCHEMA;
		JsonMap m = new JsonMap();
		if (type.isBean()) {
			Predicate<Object> checkNull = x -> isKeepNullProperties() || x != null;
			HttpPartSchema s2 = s;
			toBeanMap(o).forEachValue(checkNull, (pMeta,key,val,thrown) -> {
				if (thrown == null)
					m.put(key, toObject(partType, val, s2.getProperty(key)));
			});
		} else {
			HttpPartSchema s2 = s;
			((Map<?,?>)o).forEach((k,v) -> m.put(stringify(k), toObject(partType, v, s2.getProperty(stringify(k)))));
		}
		if (isSortMaps())
			return sort(m);
		return m;
	}

	@SuppressWarnings("rawtypes")
	private List toList(HttpPartType partType, ClassMeta<?> type, Object o, HttpPartSchema s) throws SerializeException, SchemaValidationException {
		if (s == null)
			s = DEFAULT_SCHEMA;
		JsonList l = new JsonList();
		HttpPartSchema items = s.getItems();
		if (type.isArray()) {
			for (int i = 0; i < Array.getLength(o); i++)
				l.add(toObject(partType, Array.get(o, i), items));
		} else if (type.isCollection()) {
			((Collection<?>)o).forEach(x -> l.add(toObject(partType, x, items)));
		} else {
			l.add(toObject(partType, o, items));
		}
		if (isSortCollections())
			return sort(l);
		return l;
	}

	@SuppressWarnings("rawtypes")
	private Object toObject(HttpPartType partType, Object o, HttpPartSchema s) throws SerializeException, SchemaValidationException {
		if (o == null)
			return null;
		if (s == null)
			s = DEFAULT_SCHEMA;
		ClassMeta cm = getClassMetaForObject(o);
		HttpPartDataType t = s.getType(cm);
		HttpPartFormat f = s.getFormat(cm);
		HttpPartCollectionFormat cf = s.getCollectionFormat();

		if (t == STRING) {
			if (f == BYTE)
				return base64Encode(toType(o, CM_ByteArray));
			if (f == BINARY)
				return toHex(toType(o, CM_ByteArray));
			if (f == BINARY_SPACED)
				return toSpacedHex(toType(o, CM_ByteArray));
			if (f == DATE)
				return toIsoDate(toType(o, CM_Calendar));
			if (f == DATE_TIME)
				return toIsoDateTime(toType(o, CM_Calendar));
			return o;
		} else if (t == ARRAY) {
			List l = toList(partType, getClassMetaForObject(o), o, s);
			if (cf == CSV)
				return joine(l, ',');
			if (cf == PIPES)
				return joine(l, '|');
			if (cf == SSV)
				return join(l, ' ');
			if (cf == TSV)
				return join(l, '\t');
			return l;
		} else if (t == OBJECT) {
			return toMap(partType, getClassMetaForObject(o), o, s);
		}

		return o;
	}

	private <T> T toType(Object in, ClassMeta<T> type) throws SerializeException {
		try {
			return convertToType(in, type);
		} catch (InvalidDataConversionException e) {
			throw new SerializeException(e);
		}
	}
}
