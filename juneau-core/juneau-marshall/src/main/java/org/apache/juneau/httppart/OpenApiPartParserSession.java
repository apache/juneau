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
package org.apache.juneau.httppart;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartSchema.Type.*;
import static org.apache.juneau.httppart.HttpPartSchema.Format.*;
import static org.apache.juneau.httppart.HttpPartSchema.CollectionFormat.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * Session object that lives for the duration of a single use of {@link OpenApiPartParser}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public class OpenApiPartParserSession extends UonPartParserSession {

	// Cache these for faster lookup
	private static final BeanContext BC = BeanContext.DEFAULT;
	private static final ClassMeta<Long> CM_Long = BC.getClassMeta(Long.class);
	private static final ClassMeta<Integer> CM_Integer = BC.getClassMeta(Integer.class);
	private static final ClassMeta<Double> CM_Double = BC.getClassMeta(Double.class);
	private static final ClassMeta<Float> CM_Float = BC.getClassMeta(Float.class);
	private static final ClassMeta<Boolean> CM_Boolean = BC.getClassMeta(Boolean.class);
	private static final ClassMeta<ObjectList> CM_ObjectList = BC.getClassMeta(ObjectList.class);
	private static final ClassMeta<ObjectMap> CM_ObjectMap = BC.getClassMeta(ObjectMap.class);

	private static final HttpPartSchema DEFAULT_SCHEMA = HttpPartSchema.DEFAULT;

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final OpenApiPartParser ctx;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected OpenApiPartParserSession(OpenApiPartParser ctx, ParserSessionArgs args) {
		super(ctx, args);
		this.ctx = ctx;
	}


	@Override /* HttpPartParser */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) throws ParseException, SchemaValidationException {
		schema = ObjectUtils.firstNonNull(schema, ctx.getSchema(), DEFAULT_SCHEMA);
		T t = parseInner(partType, schema, in, type);
		if (t == null && type.isPrimitive())
			t = type.getPrimitiveDefault();
		schema.validateOutput(t, ctx);
		return t;
	}

	@SuppressWarnings({ "unchecked" })
	private<T> T parseInner(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) throws SchemaValidationException, ParseException {
		schema.validateInput(in);
		if (in == null) {
			if (schema.getDefault() == null)
				return null;
			in = schema.getDefault();
		} else {
			HttpPartSchema.Type t = schema.getType(type);
			HttpPartSchema.Format f = schema.getFormat(type);

			if (t == STRING) {
				if (type.isObject()) {
					if (f == BYTE)
						return (T)base64Decode(in);
					if (f == DATE || f == DATE_TIME)
						return (T)parseIsoCalendar(in);
					if (f == BINARY)
						return (T)fromHex(in);
					if (f == BINARY_SPACED)
						return (T)fromSpacedHex(in);
					if (f == HttpPartSchema.Format.UON)
						return super.parse(partType, schema, in, type);
					return (T)in;
				}
				if (f == BYTE)
					return toType(base64Decode(in), type);
				if (f == DATE || f == DATE_TIME)
					return toType(parseIsoCalendar(in), type);
				if (f == BINARY)
					return toType(fromHex(in), type);
				if (f == BINARY_SPACED)
					return toType(fromSpacedHex(in), type);
				if (f == HttpPartSchema.Format.UON)
					return super.parse(partType, schema, in, type);
				return toType(in, type);

			} else if (t == ARRAY) {
				if (type.isObject())
					type = (ClassMeta<T>)CM_ObjectList;

				ClassMeta<?> eType = type.isObject() ? string() : type.getElementType();
				if (eType == null)
					eType = schema.getParsedType().getElementType();

				HttpPartSchema.CollectionFormat cf = schema.getCollectionFormat();
				String[] ss = new String[0];

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
				else if (cf == HttpPartSchema.CollectionFormat.UON)
					return super.parse(partType, null, in, type);
				else if (cf == NO_COLLECTION_FORMAT) {
					if (firstNonWhitespaceChar(in) == '@' && lastNonWhitespaceChar(in) == ')')
						return super.parse(partType, null, in, type);
					ss = split(in, ',');
				}

				Object[] o = null;
				if (schema.getItems() != null) {
					o = new Object[ss.length];
					for (int i = 0; i < ss.length; i++)
						o[i] = parse(partType, schema.getItems(), ss[i], eType);
				} else {
					o = ss;
				}
				if (type.hasTransformFrom(schema.getParsedType()) || schema.getParsedType().hasTransformTo(type))
					return toType(toType(o, schema.getParsedType()), type);
				return toType(o, type);

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
				return toType(super.parse(partType, schema, in, CM_Integer), type);

			} else if (t == OBJECT) {
				if (type.isObject())
					type = (ClassMeta<T>)CM_ObjectMap;
				if (schema.hasProperties() && type.isMapOrBean()) {
					try {
						if (type.isBean()) {
							BeanMap<T> m = BC.createBeanSession().newBeanMap(type.getInnerClass());
							for (Map.Entry<String,Object> e : parse(partType, DEFAULT_SCHEMA, in, CM_ObjectMap).entrySet()) {
								String key = e.getKey();
								BeanPropertyMeta bpm = m.getPropertyMeta(key);
								m.put(key, parse(partType, schema.getProperty(key), asString(e.getValue()), bpm == null ? object() : bpm.getClassMeta()));
							}
							return m.getBean();
						}
						Map<String,Object> m = (Map<String,Object>)type.newInstance();
						for (Map.Entry<String,Object> e : parse(partType, DEFAULT_SCHEMA, in, CM_ObjectMap).entrySet()) {
							String key = e.getKey();
							m.put(key, parse(partType, schema.getProperty(key), asString(e.getValue()), object()));
						}
						return (T)m;
					} catch (Exception e1) {
						throw new ParseException(e1, "Could not instantiate type ''{0}''.", type);
					}
				}
				return super.parse(partType, schema, in, type);

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
