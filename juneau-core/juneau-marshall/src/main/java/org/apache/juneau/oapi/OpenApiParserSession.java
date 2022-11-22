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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartCollectionFormat.*;
import static org.apache.juneau.httppart.HttpPartDataType.*;
import static org.apache.juneau.httppart.HttpPartFormat.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;
import org.apache.juneau.uon.*;

/**
 * Session object that lives for the duration of a single use of {@link OpenApiParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.OpenApiDetails">OpenAPI Details</a>
 * </ul>
 */
public class OpenApiParserSession extends UonParserSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	// Cache these for faster lookup
	private static final BeanContext BC = BeanContext.DEFAULT;
	private static final ClassMeta<Long> CM_Long = BC.getClassMeta(Long.class);
	private static final ClassMeta<Integer> CM_Integer = BC.getClassMeta(Integer.class);
	private static final ClassMeta<Double> CM_Double = BC.getClassMeta(Double.class);
	private static final ClassMeta<Float> CM_Float = BC.getClassMeta(Float.class);
	private static final ClassMeta<Boolean> CM_Boolean = BC.getClassMeta(Boolean.class);
	private static final ClassMeta<JsonList> CM_JsonList = BC.getClassMeta(JsonList.class);
	private static final ClassMeta<JsonMap> CM_JsonMap = BC.getClassMeta(JsonMap.class);

	private static final HttpPartSchema DEFAULT_SCHEMA = HttpPartSchema.DEFAULT;

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(OpenApiParser ctx) {
		return new Builder(ctx);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends UonParserSession.Builder {

		OpenApiParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(OpenApiParser ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public OpenApiParserSession build() {
			return new OpenApiParserSession(this);
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

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParserSession.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParserSession.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonParserSession.Builder */
		public Builder decoding(boolean value) {
			super.decoding(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final OpenApiParser ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected OpenApiParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}


	@SuppressWarnings("unchecked")
	@Override /* HttpPartParser */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) throws ParseException, SchemaValidationException {
		if (partType == null)
			partType = HttpPartType.OTHER;

		boolean isOptional = type.isOptional();

		while (type != null && type.isOptional())
			type = (ClassMeta<T>)type.getElementType();

		if (type == null)
			type = (ClassMeta<T>)object();

		schema = ObjectUtils.firstNonNull(schema, getSchema(), DEFAULT_SCHEMA);

		T t = parseInner(partType, schema, in, type);
		if (t == null && type.isPrimitive())
			t = type.getPrimitiveDefault();
		schema.validateOutput(t, ctx.getBeanContext());

		if (isOptional)
			t = (T)optional(t);

		return t;
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		return parseInner(null, HttpPartSchema.DEFAULT, pipe.asString(), type);
	}

	@SuppressWarnings({ "unchecked" })
	private<T> T parseInner(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) throws SchemaValidationException, ParseException {
		schema.validateInput(in);
		if (in == null || "null".equals(in)) {
			if (schema.getDefault() == null)
				return null;
			in = schema.getDefault();
		} else {

			ObjectSwap<T,Object> swap = (ObjectSwap<T,Object>)type.getSwap(this);
			BuilderSwap<T,Object> builder = (BuilderSwap<T,Object>)type.getBuilderSwap(this);
			ClassMeta<?> sType = null;
			if (builder != null)
				sType = builder.getBuilderClassMeta(this);
			else if (swap != null)
				sType = swap.getSwapClassMeta(this);
			else
				sType = type;

			if (sType.isOptional())
				return (T)optional(parseInner(partType, schema, in, sType.getElementType()));

			HttpPartDataType t = schema.getType(sType);
			if (partType == null)
				partType = HttpPartType.OTHER;

			HttpPartFormat f = schema.getFormat(sType);
			if (f == HttpPartFormat.NO_FORMAT)
				f = ctx.getFormat();

			if (t == STRING) {
				if (sType.isObject()) {
					if (f == BYTE)
						return toType(base64Decode(in), type);
					if (f == DATE || f == DATE_TIME)
						return toType(parseIsoCalendar(in), type);
					if (f == BINARY)
						return toType(fromHex(in), type);
					if (f == BINARY_SPACED)
						return toType(fromSpacedHex(in), type);
					if (f == HttpPartFormat.UON)
						return super.parse(partType, schema, in, type);
					return toType(in, type);
				}
				if (f == BYTE)
					return toType(base64Decode(in), type);
				if (f == DATE) {
					try {
						if (type.isCalendar())
							return toType(TemporalCalendarSwap.IsoDate.DEFAULT.unswap(this, in, type), type);
						if (type.isDate())
							return toType(TemporalDateSwap.IsoDate.DEFAULT.unswap(this, in, type), type);
						if (type.isTemporal())
							return toType(TemporalSwap.IsoDate.DEFAULT.unswap(this, in, type), type);
						return toType(in, type);
					} catch (Exception e) {
						throw new ParseException(e);
					}
				}
				if (f == DATE_TIME) {
					try {
						if (type.isCalendar())
							return toType(TemporalCalendarSwap.IsoDateTime.DEFAULT.unswap(this, in, type), type);
						if (type.isDate())
							return toType(TemporalDateSwap.IsoDateTime.DEFAULT.unswap(this, in, type), type);
						if (type.isTemporal())
							return toType(TemporalSwap.IsoDateTime.DEFAULT.unswap(this, in, type), type);
						return toType(in, type);
					} catch (Exception e) {
						throw new ParseException(e);
					}
				}
				if (f == BINARY)
					return toType(fromHex(in), type);
				if (f == BINARY_SPACED)
					return toType(fromSpacedHex(in), type);
				if (f == HttpPartFormat.UON)
					return super.parse(partType, schema, in, type);
				return toType(in, type);

			} else if (t == BOOLEAN) {
				if (type.isObject())
					type = (ClassMeta<T>)CM_Boolean;
				if (type.isBoolean())
					return super.parse(partType, schema, in, type);
				return toType(super.parse(partType, schema, in, CM_Boolean), type);

			} else if (t == INTEGER) {
				if (type.isObject()) {
					if (f == INT64)
						type = (ClassMeta<T>)CM_Long;
					else
						type = (ClassMeta<T>)CM_Integer;
				}
				if (type.isNumber())
					return super.parse(partType, schema, in, type);
				return toType(super.parse(partType, schema, in, CM_Integer), type);

			} else if (t == NUMBER) {
				if (type.isObject()) {
					if (f == DOUBLE)
						type = (ClassMeta<T>)CM_Double;
					else
						type = (ClassMeta<T>)CM_Float;
				}
				if (type.isNumber())
					return super.parse(partType, schema, in, type);
				return toType(super.parse(partType, schema, in, CM_Double), type);

			} else if (t == ARRAY) {

				HttpPartCollectionFormat cf = schema.getCollectionFormat();
				if (cf == HttpPartCollectionFormat.NO_COLLECTION_FORMAT)
					cf = ctx.getCollectionFormat();

				if (cf == HttpPartCollectionFormat.UONC)
					return super.parse(partType, schema, in, type);

				if (type.isObject())
					type = (ClassMeta<T>)CM_JsonList;

				ClassMeta<?> eType = type.isObject() ? string() : type.getElementType();
				if (eType == null)
					eType = schema.getParsedType().getElementType();
				if (eType == null)
					eType = string();

				String[] ss = {};

				if (cf == MULTI)
					ss = new String[]{in};
				else if (cf == CSV)
					ss = split(in, ',');
				else if (cf == PIPES)
					ss = split(in, '|');
				else if (cf == SSV)
					ss = splitQuoted(in);
				else if (cf == TSV)
					ss = split(in, '\t');
				else if (cf == HttpPartCollectionFormat.UONC)
					return super.parse(partType, null, in, type);
				else if (cf == NO_COLLECTION_FORMAT) {
					if (firstNonWhitespaceChar(in) == '@' && lastNonWhitespaceChar(in) == ')')
						return super.parse(partType, null, in, type);
					ss = split(in, ',');
				}

				HttpPartSchema items = schema.getItems();
				if (items == null)
					items = HttpPartSchema.DEFAULT;
				Object o = Array.newInstance(eType.getInnerClass(), ss.length);
				for (int i = 0; i < ss.length; i++)
					Array.set(o, i, parse(partType, items, ss[i], eType));
				if (type.hasMutaterFrom(schema.getParsedType()) || schema.getParsedType().hasMutaterTo(type))
					return toType(toType(o, schema.getParsedType()), type);
				return toType(o, type);

			} else if (t == OBJECT) {

				HttpPartCollectionFormat cf = schema.getCollectionFormat();
				if (cf == HttpPartCollectionFormat.NO_COLLECTION_FORMAT)
					cf = ctx.getCollectionFormat();

				if (cf == HttpPartCollectionFormat.UONC)
					return super.parse(partType, schema, in, type);

				if (type.isObject())
					type = (ClassMeta<T>)CM_JsonMap;

				if (! type.isMapOrBean())
					throw new ParseException("Invalid type {0} for part type OBJECT.", type);

				String[] ss = {};

				if (cf == MULTI)
					ss = new String[]{in};
				else if (cf == CSV)
					ss = split(in, ',');
				else if (cf == PIPES)
					ss = split(in, '|');
				else if (cf == SSV)
					ss = splitQuoted(in);
				else if (cf == TSV)
					ss = split(in, '\t');
				else if (cf == HttpPartCollectionFormat.UONC)
					return super.parse(partType, null, in, type);
				else if (cf == NO_COLLECTION_FORMAT) {
					if (firstNonWhitespaceChar(in) == '@' && lastNonWhitespaceChar(in) == ')')
						return super.parse(partType, null, in, type);
					ss = split(in, ',');
				}

				if (type.isBean()) {
					BeanMap<T> m = ctx.getBeanContext().newBeanMap(type.getInnerClass());
					for (String s : ss) {
						String[] kv = split(s, '=', 2);
						if (kv.length != 2)
							throw new ParseException("Invalid input {0} for part type OBJECT.", in);
						String key = kv[0], value = kv[1];
						BeanPropertyMeta bpm = m.getPropertyMeta(key);
						if (bpm == null && ! isIgnoreUnknownBeanProperties())
							throw new ParseException("Invalid input {0} for part type OBJECT.  Cannot find property {1}", in, key);
						m.put(key, parse(partType, schema.getProperty(key), value, ((ClassMeta<T>)(bpm == null ? object() : bpm.getClassMeta()))));
					}
					return m.getBean();
				}

				ClassMeta<?> eType = type.isObject() ? string() : type.getValueType();
				if (eType == null)
					eType = schema.getParsedType().getValueType();
				if (eType == null)
					eType = string();

				try {
					Map<String,Object> m = (Map<String,Object>)type.newInstance();
					if (m == null)
						m = JsonMap.create();

					for (String s : ss) {
						String[] kv = split(s, '=', 2);
						if (kv.length != 2)
							throw new ParseException("Invalid input {0} for part type OBJECT.", in);
						String key = kv[0], value = kv[1];
						m.put(key, parse(partType, schema.getProperty(key), value, eType));
					}
					return (T)m;
				} catch (ExecutableException e) {
					throw new ParseException(e);
				}

			} else if (t == FILE) {
				throw new ParseException("File part not supported.");

			} else if (t == NO_TYPE) {
				// This should never be returned by HttpPartSchema.getType(ClassMeta).
				throw new ParseException("Invalid type.");
			}
		}

		return super.parse(partType, schema, in, type);
	}

	private <T> T toType(Object in, ClassMeta<T> type) throws ParseException {
		try {
			return convertToType(in, type);
		} catch (InvalidDataConversionException e) {
			throw new ParseException(e.getMessage());
		}
	}
}
