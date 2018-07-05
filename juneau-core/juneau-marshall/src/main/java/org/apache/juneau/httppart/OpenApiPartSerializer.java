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

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to values suitable for transmission as HTTP headers, query/form-data parameters, and path variables.
 *
 * <p>
 * This serializer uses UON notation for all parts by default.  This allows for arbitrary POJOs to be losslessly
 * serialized as any of the specified HTTP types.
 */
public class OpenApiPartSerializer extends UonPartSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "OpenApiPartSerializer.";

	/**
	 * Configuration property:  OpenAPI schema description.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"OpenApiPartSerializer.schema"</js>
	 * 	<li><b>Data type:</b>  <code>HttpPartSchema</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link OpenPartSerializerBuilder#schema(HttpPartSchema)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines the OpenAPI schema for this part serializer.
	 */
	public static final String OAPI_schema = PREFIX + "schema.o";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link OpenApiPartSerializer}, all default settings. */
	public static final OpenApiPartSerializer DEFAULT = new OpenApiPartSerializer(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final HttpPartSchema schema;

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public OpenApiPartSerializer(PropertyStore ps) {
		super(
			ps.builder()
				.set(UON_encoding, false)
				.build()
		);
		this.schema = getProperty(OAPI_schema, HttpPartSchema.class, HttpPartSchema.DEFAULT);
	}

	@Override /* Context */
	public UonPartSerializerBuilder builder() {
		return new UonPartSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UonPartSerializerBuilder} object.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link UonPartSerializerBuilder} object.
	 */
	public static UonPartSerializerBuilder create() {
		return new UonPartSerializerBuilder();
	}

	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* PartSerializer */
	public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
		schema = ObjectUtils.firstNonNull(schema, this.schema, HttpPartSchema.DEFAULT);
//		ClassMeta<?> type = getClassMetaForObject(value);
//		String out;
//
//		switch (schema.getType()) {
//			case STRING: {
//				if (type.isObject()) {
//					switch (schema.getFormat()) {
//						case BYTE:
//							return (T)StringUtils.base64Decode(in);
//						case DATE:
//						case DATE_TIME:
//							return (T)StringUtils.parseIsoCalendar(in);
//						case BINARY:
//							return (T)StringUtils.fromHex(in);
//						case BINARY_SPACED:
//							return (T)StringUtils.fromSpacedHex(in);
//						case UON:
//							return super.parse(partType, schema, in, type);
//						default:
//							return (T)in;
//					}
//				}
//				switch (schema.getFormat()) {
//					case BYTE:
//						return toType(StringUtils.base64Decode(in), type);
//					case DATE:
//					case DATE_TIME:
//						return toType(StringUtils.parseIsoCalendar(in), type);
//					case BINARY:
//						return toType(StringUtils.fromHex(in), type);
//					case BINARY_SPACED:
//						return toType(StringUtils.fromSpacedHex(in), type);
//					case UON:
//						return super.parse(partType, schema, in, type);
//					default:
//						return toType(in, type);
//				}
//			}
//			case ARRAY: {
//				if (type.isObject())
//					type = (ClassMeta<T>)CM_ObjectList;
//
//				ClassMeta<?> eType = type.isObject() ? string() : type.getElementType();
//				if (eType == null)
//					eType = schema.getParsedType().getElementType();
//
//				String[] ss = new String[0];
//				switch (schema.getCollectionFormat()) {
//					case MULTI:
//						ss = new String[]{in};
//						break;
//					case CSV:
//						ss = split(in, ',');
//						break;
//					case PIPES:
//						ss = split(in, '|');
//						break;
//					case SSV:
//						ss = splitQuoted(in);
//						break;
//					case TSV:
//						ss = split(in, '\t');
//						break;
//					case UON:
//						return super.parse(partType, null, in, type);
//					case NONE:
//						if (firstNonWhitespaceChar(in) == '@' && lastNonWhitespaceChar(in) == ')')
//							return super.parse(partType, null, in, type);
//						ss = split(in, ',');
//				}
//				Object[] o = null;
//				if (schema.getItems() != null) {
//					o = new Object[ss.length];
//					for (int i = 0; i < ss.length; i++)
//						o[i] = parse(partType, schema.getItems(), ss[i], eType);
//				} else {
//					o = ss;
//				}
//				if (type.getTransform(schema.getParsedType().getInnerClass()) != null)
//					return toType(toType(o, schema.getParsedType()), type);
//				return toType(o, type);
//			}
//			case BOOLEAN: {
//				if (type.isObject())
//					type = (ClassMeta<T>)CM_Boolean;
//				if (type.isBoolean())
//					return super.parse(partType, schema, in, type);
//				return toType(super.parse(partType, schema, in, CM_Boolean), type);
//			}
//			case INTEGER: {
//				if (type.isObject()) {
//					switch (schema.getFormat()) {
//						case INT64:
//							type = (ClassMeta<T>)CM_Long;
//							break;
//						default:
//							type = (ClassMeta<T>)CM_Integer;
//
//					}
//				}
//				if (type.isNumber())
//					return super.parse(partType, schema, in, type);
//				return toType(super.parse(partType, schema, in, CM_Integer), type);
//			}
//			case NUMBER: {
//				if (type.isObject()) {
//					switch (schema.getFormat()) {
//						case DOUBLE:
//							type = (ClassMeta<T>)CM_Double;
//							break;
//						default:
//							type = (ClassMeta<T>)CM_Float;
//					}
//				}
//				if (type.isNumber())
//					return super.parse(partType, schema, in, type);
//				return toType(super.parse(partType, schema, in, CM_Integer), type);
//			}
//			case OBJECT: {
//				if (type.isObject())
//					type = (ClassMeta<T>)CM_ObjectMap;
//				if (schema.hasProperties() && type.isMapOrBean()) {
//					try {
//						if (type.isBean()) {
//							BeanMap<T> m = BC.createBeanSession().newBeanMap(type.getInnerClass());
//							for (Map.Entry<String,Object> e : parse(partType, DEFAULT_SCHEMA, in, CM_ObjectMap).entrySet()) {
//								String key = e.getKey();
//								BeanPropertyMeta bpm = m.getPropertyMeta(key);
//								m.put(key, parse(partType, schema.getProperty(key), asString(e.getValue()), bpm == null ? object() : bpm.getClassMeta()));
//							}
//							return m.getBean();
//						}
//						Map<String,Object> m = (Map<String,Object>)type.newInstance();
//						for (Map.Entry<String,Object> e : parse(partType, DEFAULT_SCHEMA, in, CM_ObjectMap).entrySet()) {
//							String key = e.getKey();
//							m.put(key, parse(partType, schema.getProperty(key), asString(e.getValue()), object()));
//						}
//						return (T)m;
//					} catch (Exception e1) {
//						throw new ParseException(e1, "Could not instantiate type ''{0}''.", type);
//					}
//				}
//				return super.parse(partType, schema, in, type);
//			}
//			case FILE: {
//				throw new ParseException("File part not supported.");
//			}
//			case NONE: {
//				// This should never be returned by HttpPartSchema.getType(ClassMeta).
//				throw new ParseException("Invalid type.");
//			}
//		}
//	}




		String out = super.serialize(partType, schema, value);
		schema.validateInput(out);
		return out;
	}
}
