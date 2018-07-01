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
package org.apache.juneau.httppart.oapi;

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.uon.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * OpenAPI part parser.
 */
public class OapiPartParser extends UonPartParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "OapiPartParser.";

	/**
	 * Configuration property:  OpenAPI schema description.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"OapiPartParser.schema"</js>
	 * 	<li><b>Data type:</b>  <code>HttpPartSchema</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link OapiPartParserBuilder#schema(HttpPartSchema)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines the OpenAPI schema for this part parser.
	 */
	public static final String OAPI_schema = PREFIX + "schema.o";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link OapiPartParser}. */
	public static final OapiPartParser DEFAULT = new OapiPartParser(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final HttpPartSchema schema;

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public OapiPartParser(PropertyStore ps) {
		super(
			ps.builder().build()
		);
		this.schema = getProperty(OAPI_schema, HttpPartSchema.class, HttpPartSchema.DEFAULT);
	}

	@Override /* Context */
	public UonPartParserBuilder builder() {
		return new UonPartParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UonPartParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UonPartParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link UonPartParserBuilder} object.
	 */
	public static UonPartParserBuilder create() {
		return new UonPartParserBuilder();
	}

	@Override /* HttpPartParser */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) throws ParseException, SchemaValidationParseException {
		schema = ObjectUtils.firstNonNull(schema, this.schema, HttpPartSchema.DEFAULT);
		T t = parseInner(partType, schema, in, type);
		if (t == null && type.isPrimitive())
			t = type.getPrimitiveDefault();
		schema.validateOutput(t, this);
		return t;
	}

	@SuppressWarnings({ "unchecked" })
	private<T> T parseInner(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) throws SchemaValidationParseException, ParseException {
		schema.validateInput(in);
		if (in == null) {
			if (schema.getDefault() == null)
				return null;
			in = schema.getDefault();
		} else {
			switch (schema.getType(type)) {
				case STRING: {
					if (type.isObject()) {
						switch (schema.getFormat()) {
							case BYTE:
								return (T)StringUtils.base64Decode(in);
							case DATE:
							case DATE_TIME:
								return (T)StringUtils.parseIsoDate(in);
							case BINARY:
								return (T)StringUtils.fromHex(in);
							case UON:
								return super.parse(partType, schema, in, type);
							default:
								return (T)in;
						}
					}
					switch (schema.getFormat()) {
						case BYTE:
							return toType(StringUtils.base64Decode(in), type);
						case DATE:
						case DATE_TIME:
							return toType(StringUtils.parseIsoDate(in), type);
						case BINARY:
							return toType(StringUtils.fromHex(in), type);
						case UON:
							return super.parse(partType, schema, in, type);
						default:
							return toType(in, type);
					}
				}
				case ARRAY: {
					if (type.isObject())
						type = (ClassMeta<T>)getClassMeta(ObjectList.class);

					ClassMeta<?> eType = type.isObject() ? string() : type.getElementType();
					if (eType == null)
						throw new ParseException("Value of type ARRAY cannot be converted to type {0}", type);

					String[] ss = new String[0];
					switch (schema.getCollectionFormat()) {
						case MULTI:
							ss = new String[]{in};
							break;
						case CSV:
							ss = split(in, ',');
							break;
						case PIPES:
							ss = split(in, '|');
							break;
						case SSV:
							ss = splitQuoted(in);
							break;
						case TSV:
							ss = split(in, '\t');
							break;
						case UON:
							return super.parse(partType, null, in, type);
						case NONE:
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
					return toType(o, type);
				}
				case BOOLEAN: {
					if (type.isObject())
						type = (ClassMeta<T>)getClassMeta(Boolean.class);
					return super.parse(partType, schema, in, type);
				}
				case INTEGER: {
					if (type.isObject()) {
						switch (schema.getFormat()) {
							case INT64:
								type = (ClassMeta<T>)getClassMeta(Long.class);
								break;
							default:
								type = (ClassMeta<T>)getClassMeta(Integer.class);

						}
					}
					return super.parse(partType, schema, in, type);
				}
				case NUMBER: {
					if (type.isObject()) {
						switch (schema.getFormat()) {
							case DOUBLE:
								type = (ClassMeta<T>)getClassMeta(Double.class);
								break;
							default:
								type = (ClassMeta<T>)getClassMeta(Float.class);
						}
					}
					return super.parse(partType, schema, in, type);
				}
				case OBJECT: {
					if (type.isObject())
						type = (ClassMeta<T>)getClassMeta(ObjectMap.class);
					switch (schema.getFormat()) {
						default:
							return super.parse(partType, schema, in, type);
					}
				}
				case FILE: {
					throw new ParseException("File part not supported.");
				}
				case NONE: {
					throw new ParseException("Invalid type.");
				}
			}
		}

		return super.parse(partType, schema, in, type);
	}

	private <T> T toType(Object in, ClassMeta<T> type) throws ParseException {
		try {
			return createBeanSession().convertToType(in, type);
		} catch (InvalidDataConversionException e) {
			throw new ParseException(e.getMessage());
		}
	}
}
