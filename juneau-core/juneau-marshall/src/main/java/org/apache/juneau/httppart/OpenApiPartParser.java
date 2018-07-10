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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * OpenAPI part parser.
 *
 * <table class='styled'>
 * 	<tr><th>Type</th><th>Format</th><th>Valid parameter types</th></tr>
 * 	<tr>
 * 		<td ><code>string</code></td>
 * 		<td>
 * 			<code>byte</code>
 * 			<br><code>binary</code>
 * 			<br><code>binary-spaced</br>
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li><code><jk>byte</jk>[]</code> (default)
 * 				<li>{@link InputStream} - Returns a {@link ByteArrayInputStream}.
 * 				<li>{@link Reader} - Returns a {@link InputStreamReader} wrapped around a {@link ByteArrayInputStream}.
 * 				<li>{@link String} - Constructed using {@link String#String(byte[])}.
 * 				<li>{@link Object} - Returns the default <code><jk>byte</jk>[]</code>.
 * 				<li>Any POJO transformable from a <code><jk>byte</jk>[]</code> (via constructors, static create methods, or toX() instance methods).
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td ><code>string</code></td>
 * 		<td>
 * 			<code>date</code>
 * 			<code>date-time</code>
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li>{@link Calendar} (default)
 * 				<li>{@link Date}
 * 				<li>{@link GregorianCalendar}
 * 				<li>{@link String} - Converted using {@link Calendar#toString()}.
 * 				<li>{@link Object} - Returns the default {@link Calendar}.
 * 				<li>Any POJO transformable from a {@link Calendar} (via constructors, static create methods, or toX() instance methods).
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td ><code>string</code></td>
 * 		<td><code>uon</code></td>
 * 		<td>
 * 			<ul>
 * 				<li>Any <a href='../../../../overview-summary#juneau-marshall.PojoCategories'>parsable POJO</a>.
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td ><code>string</code></td>
 * 		<td>none specified</td>
 * 		<td>
 * 			<ul>
 * 				<li>{@link String} (default)
 * 				<li>{@link Object} - Returns the default {@link String}.
 * 				<li>Any POJO transformable from a {@link String} (via constructors, static create methods, or toX() instance methods).
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td ><code>boolean</code></td>
 * 		<td>
 * 			&nbsp;
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li>{@link Boolean} (default)
 * 				<li><jk>boolean</code>
 * 				<li>{@link String}
 * 				<li>{@link Object} - Returns the default {@link Boolean}.
 * 				<li>Any POJO transformable from a {@link Boolean} (via constructors, static create methods, or toX() instance methods).
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td ><code>array</code></td>
 * 		<td>
 * 			&nbsp;
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li>Arrays or Collections of anything on this list.
 * 				<li>Any POJO transformable from arrays of the default types (e.g. <code>Integer[]</code>, <code>Boolean[][]</code>, etc...).
 * 			</ul>
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td ><code>object</code></td>
 * 		<td>
 * 			&nbsp;
 * 		</td>
 * 		<td>
 * 			<ul>
 * 				<li>Beans with properties of anything on this list.
 * 				<li>Maps with string keys.
 * 			</ul>
 * 		</td>
 * 	</tr>
 * </table>
 */
public class OpenApiPartParser extends UonPartParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "OpenApiPartParser.";

	/**
	 * Configuration property:  OpenAPI schema description.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"OpenApiPartParser.schema"</js>
	 * 	<li><b>Data type:</b>  <code>HttpPartSchema</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link OpenApiPartParserBuilder#schema(HttpPartSchema)}
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

	/** Reusable instance of {@link OpenApiPartParser}. */
	public static final OpenApiPartParser DEFAULT = new OpenApiPartParser(PropertyStore.DEFAULT);

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

	private final HttpPartSchema schema;
	private final BeanSession bs;

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public OpenApiPartParser(PropertyStore ps) {
		super(
			ps.builder().build()
		);
		this.bs = createBeanSession();
		this.schema = getProperty(OAPI_schema, HttpPartSchema.class, DEFAULT_SCHEMA);
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

	/**
	 * Convenience method for parsing a part.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param type The category of value being parsed.
	 * @return The parsed value.
	 * @throws ParseException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartSchema schema, String in, Class<T> type) throws ParseException, SchemaValidationException {
		return parse(null, schema, in, getClassMeta(type));
	}

	/**
	 * Convenience method for parsing a part to a map or collection.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param type The category of value being parsed.
	 * @param args The type arguments of the map or collection.
	 * @return The parsed value.
	 * @throws ParseException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	@SuppressWarnings("unchecked")
	public <T> T parse(HttpPartSchema schema, String in, java.lang.reflect.Type type, java.lang.reflect.Type...args) throws ParseException, SchemaValidationException {
		return (T)parse(null, schema, in, getClassMeta(type, args));
	}

	@Override /* HttpPartParser */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) throws ParseException, SchemaValidationException {
		schema = ObjectUtils.firstNonNull(schema, this.schema, DEFAULT_SCHEMA);
		T t = parseInner(partType, schema, in, type);
		if (t == null && type.isPrimitive())
			t = type.getPrimitiveDefault();
		schema.validateOutput(t, this);
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
			HttpPartSchema.Format f = schema.getFormat();

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
			return bs.convertToType(in, type);
		} catch (InvalidDataConversionException e) {
			throw new ParseException(e.getMessage());
		}
	}
}
