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

import static org.apache.juneau.httppart.HttpPartSchema.CollectionFormat.*;
import static org.apache.juneau.httppart.HttpPartSchema.Format.*;
import static org.apache.juneau.httppart.HttpPartSchema.Type.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

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

	// Cache these for faster lookup
	private static final BeanContext BC = BeanContext.DEFAULT;
	private static final ClassMeta<byte[]> CM_ByteArray = BC.getClassMeta(byte[].class);
	private static final ClassMeta<Calendar> CM_Calendar = BC.getClassMeta(Calendar.class);
	private static final ClassMeta<Long> CM_Long = BC.getClassMeta(Long.class);
	private static final ClassMeta<Integer> CM_Integer = BC.getClassMeta(Integer.class);
	private static final ClassMeta<Double> CM_Double = BC.getClassMeta(Double.class);
	private static final ClassMeta<Float> CM_Float = BC.getClassMeta(Float.class);
	private static final ClassMeta<Boolean> CM_Boolean = BC.getClassMeta(Boolean.class);

	private static final HttpPartSchema DEFAULT_SCHEMA = HttpPartSchema.DEFAULT;

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final HttpPartSchema schema;
	final BeanSession bs;

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
		this.bs = createBeanSession();
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

	/**
	 * Convenience method for serializing a part.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part serializers use the schema information.
	 * @param value The value being serialized.
	 * @return The serialized value.
	 * @throws SerializeException If a problem occurred while trying to serialize the input.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public String serialize(HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
		return serialize(null, schema, value);
	}

	@Override /* PartSerializer */
	@SuppressWarnings("rawtypes")
	public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {

		schema = ObjectUtils.firstNonNull(schema, this.schema, DEFAULT_SCHEMA);
		ClassMeta<?> type = getClassMetaForObject(value);
		if (type == null)
			type = object();
		HttpPartSchema.Type t = schema.getType(type);
		HttpPartSchema.Format f = schema.getFormat();
		HttpPartSchema.CollectionFormat cf = schema.getCollectionFormat();

		String out = null;

		schema.validateOutput(value, this);

		if (type.hasTransformTo(schema.getParsedType()) || schema.getParsedType().hasTransformFrom(type)) {
			value = toType(value, schema.getParsedType());
			type = schema.getParsedType();
		}

		if (value != null) {

			if (t == STRING) {

				if (f == BYTE)
					out = base64Encode(toType(value, CM_ByteArray));
				else if (f == BINARY)
					out = toHex(toType(value, CM_ByteArray));
				else if (f == BINARY_SPACED)
					out = toSpacedHex(toType(value, CM_ByteArray));
				else if (f == DATE)
					out = toIsoDate(toType(value, CM_Calendar));
				else if (f == DATE_TIME)
					out = toIsoDateTime(toType(value, CM_Calendar));
				else if (f == HttpPartSchema.Format.UON)
					out = super.serialize(partType, schema, value);
				else
					out = toType(value, string());

			} else if (t == ARRAY) {

				if (cf == HttpPartSchema.CollectionFormat.UON)
					out = super.serialize(partType, null, value);
				else {
					List<String> l = new ArrayList<>();

					HttpPartSchema items = schema.getItems();
					ClassMeta<?> vt = getClassMetaForObject(value);

					if (type.isArray()) {
						for (int i = 0; i < Array.getLength(value); i++)
							l.add(serialize(partType, items, Array.get(value, i)));
					} else if (type.isCollection()) {
						for (Object o : (Collection<?>)value)
							l.add(serialize(partType, items, o));
					} else if (vt.hasTransformTo(String[].class)) {
						l.add(serialize(partType, items, value));
					}

					if (cf == PIPES)
						out = joine(l, '|');
					else if (cf == SSV)
						out = join(l, ' ');
					else if (cf == TSV)
						out = join(l, '\t');
					else
						out = joine(l, ',');
				}

			} else if (t == BOOLEAN) {

				if (f == HttpPartSchema.Format.UON)
					out = super.serialize(partType, null, value);
				else
					out = asString(toType(value, CM_Boolean));

			} else if (t == INTEGER) {

				if (f == HttpPartSchema.Format.UON)
					out = super.serialize(partType, null, value);
				else if (f == INT64)
					out = asString(toType(value, CM_Long));
				else
					out = asString(toType(value, CM_Integer));

			} else if (t == NUMBER) {

				if (f == HttpPartSchema.Format.UON)
					out = super.serialize(partType, null, value);
				else if (f == DOUBLE)
					out = asString(toType(value, CM_Double));
				else
					out = asString(toType(value, CM_Float));

			} else if (t == OBJECT) {

				if (cf == HttpPartSchema.CollectionFormat.UON) {
					out = super.serialize(partType, null, value);
				} else if (schema.hasProperties() && type.isMapOrBean()) {
					ObjectMap m = new ObjectMap();
					if (type.isBean()) {
						for (Map.Entry<String,Object> e : BC.createBeanSession().toBeanMap(value).entrySet())
							m.put(e.getKey(), serialize(partType, schema.getProperty(e.getKey()), e.getValue()));
					} else {
						for (Map.Entry e : (Set<Map.Entry>)((Map)value).entrySet())
							m.put(asString(e.getKey()), serialize(partType, schema.getProperty(asString(e.getKey())), e.getValue()));
					}
					out = super.serialize(m);
				} else {
					out = super.serialize(partType, schema, value);
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

	private <T> T toType(Object in, ClassMeta<T> type) throws SerializeException {
		try {
			return bs.convertToType(in, type);
		} catch (InvalidDataConversionException e) {
			throw new SerializeException(e.getMessage());
		}
	}
}
