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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.Date;

/**
 * Represents the headers in an HTTP request.
 *
 * <p>
 * Entries are stored in a case-insensitive map.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmRequestHeaders}
 * </ul>
 */
public class RequestHeaders extends TreeMap<String,String[]> {
	private static final long serialVersionUID = 1L;

	private final RestRequest req;
	private HttpPartParserSession parser;
	private RequestQuery queryParams;
	private Set<String> allowedQueryParams;

	RequestHeaders(RestRequest req) {
		super(String.CASE_INSENSITIVE_ORDER);
		this.req = req;
	}

	RequestHeaders parser(HttpPartParserSession parser) {
		this.parser = parser;
		return this;
	}

	RequestHeaders queryParams(RequestQuery queryParams, Set<String> allowedQueryParams) {
		this.queryParams = queryParams;
		this.allowedQueryParams = allowedQueryParams;
		return this;
	}

	/**
	 * Adds default entries to these headers.
	 *
	 * <p>
	 * Similar to {@link #put(String, Object)} but doesn't override existing values.
	 *
	 * @param defaultEntries
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders addDefault(Map<String,Object> defaultEntries) {
		if (defaultEntries != null) {
			for (Map.Entry<String,Object> e : defaultEntries.entrySet()) {
				String key = e.getKey();
				Object value = e.getValue();
				String[] v = get(key);
				if (v == null || v.length == 0 || StringUtils.isEmpty(v[0]))
					put(key, stringifyAll(value));
			}
		}
		return this;
	}

	/**
	 * Adds default entries to these headers.
	 *
	 * <p>
	 * Similar to {@link #put(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders addDefault(List<Header> pairs) {
		for (Header p : pairs) {
			String key = p.getName();
			Object value = p.getValue();
			String[] v = get(key);
			if (v == null || v.length == 0 || StringUtils.isEmpty(v[0]))
				put(key, stringifyAll(value));
		}
		return this;
	}

	/**
	 * Adds a default header value on this request.
	 *
	 * <p>
	 * Similar to {@link #put(String, Object)} but doesn't override existing values.
	 *
	 * @param name
	 * 	The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Converted to a String using <c>toString()</c>.
	 * 	<br>Ignored if value is <jk>null</jk> or blank.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders addDefault(String name, Object value) {
		return addDefault(Collections.singletonMap(name, value));
	}

	/**
	 * Adds a set of header values to this object.
	 *
	 * @param name The header name.
	 * @param values The header values.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders put(String name, Enumeration<String> values) {
		// Optimized for enumerations of one entry, the most-common case.
		if (values.hasMoreElements()) {
			String v = values.nextElement();
			String[] s = new String[]{v};
			while (values.hasMoreElements())
				s = append(s, values.nextElement());
			put(name, s);
		}
		return this;
	}

	/**
	 * Returns the specified header last value as a string.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The header name.
	 * @return The header value, or <jk>null</jk> if it doesn't exist.
	 */
	public String getString(String name) {
		String[] v = null;
		if (queryParams != null)
			if (allowedQueryParams.contains("*") || allowedQueryParams.contains(name))
				v = queryParams.get(name, true);
		if (v == null || v.length == 0)
			v = get(name);
		if (v == null || v.length == 0)
			return null;
		return v[v.length-1];
	}

	/**
	 * Returns the specified header value as a string.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param def The default value to return if the header value isn't found.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public String getString(String name, String def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : s;
	}

	/**
	 * Returns the specified header value as an integer.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @return The header value, or <code>0</code> value if the header isn't present.
	 */
	public int getInt(String name) {
		return getInt(name, 0);
	}

	/**
	 * Returns the specified header value as an integer.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param def The default value to return if the header value isn't found.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public int getInt(String name, int def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Integer.parseInt(s);
	}

	/**
	 * Returns the specified header value as a boolean.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public boolean getBoolean(String name) {
		return getBoolean(name, false);
	}

	/**
	 * Returns the specified header value as a boolean.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param def The default value to return if the header value isn't found.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public boolean getBoolean(String name, boolean def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Boolean.parseBoolean(s);
	}

	/**
	 * Sets a request header value.
	 *
	 * <p>
	 * This overwrites any previous value.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 */
	public void put(String name, Object value) {
		super.put(name, stringifyAll(value));
	}

	/**
	 * Returns the specified header value converted to a POJO using the {@link HttpPartParser} registered with the resource.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myheader = req.getHeaders().get(<js>"My-Header"</js>, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse a UUID.</jc>
	 * 	UUID myheader = req.getHeaders().get(<js>"My-Header"</js>, UUID.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(String name, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(null, null, name, null, getClassMeta(type));
	}

	/**
	 * Returns all headers with the specified name converted to a POJO using the {@link HttpPartParser} registered with the resource.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk>[] myheaders = req.getHeaders().getAll(<js>"My-Header"</js>, <jk>int</jk>[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse a UUID.</jc>
	 * 	UUID[] myheaders = req.getHeaders().getAll(<js>"My-Header"</js>, UUID[].<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * 	<li>
	 * 		The class must be an array or collection type.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T getAll(String name, Class<T> type) throws BadRequest, InternalServerError {
		return getAllInner(null, null, name, getClassMeta(type));
	}

	/**
	 * Returns the specified header value converted to a POJO using the specified part parser.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myheader = req.getHeaders().get(<js>"My-Header"</js>, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse a UUID.</jc>
	 * 	UUID myheader = req.getHeaders().get(<js>"My-Header"</js>, UUID.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param parser
	 * 	The parser to use for parsing the string header.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The HTTP header name.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed or fails schema validation.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(HttpPartParserSession parser, HttpPartSchema schema, String name, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(parser, schema, name, null, getClassMeta(type));
	}

	/**
	 * Same as {@link #get(String, Class)} but returns a default value if not found.
	 *
	 * @param name The HTTP header name.
	 * @param def The default value if the header was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(String name, T def, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(null, null, name, def, getClassMeta(type));
	}

	/**
	 * Same as {@link #get(String, Object, Class)} but allows you to override the part parser used.
	 *
	 * @param parser
	 * 	The parser to use for parsing the string header.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The HTTP header name.
	 * @param def The default value if the header was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed or fails schema validation.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(HttpPartParserSession parser, HttpPartSchema schema, String name, T def, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(parser, schema, name, def, getClassMeta(type));
	}

	/**
	 * Returns the specified header value converted to a POJO using the {@link HttpPartParser} registered with the resource.
	 *
	 * <p>
	 * Similar to {@link #get(String,Class)} but allows for complex collections of POJOs to be created.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; myheader = req.getHeader(<js>"My-Header"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		<c>Collections</c> must be followed by zero or one parameter representing the value type.
	 * 	<li>
	 * 		<c>Maps</c> must be followed by zero or two parameters representing the key and value types.
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(String name, Type type, Type...args) throws BadRequest, InternalServerError {
		return getInner(null, null, name, null, this.<T>getClassMeta(type, args));
	}

	/**
	 * Same as {@link #get(String, Type, Type...)} but allows you to override the part parser used.
	 *
	 * @param parser
	 * 	The parser to use for parsing the string header.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name
	 * 	The HTTP header name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed or fails schema validation.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(HttpPartParserSession parser, HttpPartSchema schema, String name, Type type, Type...args) throws BadRequest, InternalServerError {
		return getInner(parser, schema, name, null, this.<T>getClassMeta(type, args));
	}

	/**
	 * Converts all the headers with the specified name to the specified type.
	 *
	 * @param parser
	 * 	The parser to use for parsing the string header.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name
	 * 	The HTTP header name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed or fails schema validation.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T getAll(HttpPartParserSession parser, HttpPartSchema schema, String name, Type type, Type...args) throws BadRequest, InternalServerError {
		return getAllInner(parser, schema, name, this.<T>getClassMeta(type, args));
	}

	/* Workhorse method */
	private <T> T getInner(HttpPartParserSession parser, HttpPartSchema schema, String name, T def, ClassMeta<T> cm) throws BadRequest, InternalServerError {
		if (parser == null)
			parser = req.getPartParserSession();
		try {
			if (cm.isMapOrBean() && isOneOf(name, "*", "")) {
				OMap m = new OMap();
				for (Map.Entry<String,String[]> e : this.entrySet()) {
					String k = e.getKey();
					HttpPartSchema pschema = schema == null ? null : schema.getProperty(k);
					ClassMeta<?> cm2 = cm.getValueType();
					m.put(k, getInner(parser, pschema, k, null, cm2));
				}
				return req.getBeanSession().convertToType(m, cm);
			}
			T t = parse(parser, schema, getString(name), cm);
			return (t == null ? def : t);
		} catch (SchemaValidationException e) {
			throw new BadRequest(e, "Validation failed on header ''{0}''. ", name);
		} catch (ParseException e) {
			throw new BadRequest(e, "Could not parse header ''{0}''.", name) ;
		} catch (Exception e) {
			throw new InternalServerError(e, "Could not parse header ''{0}''.", name);
		}
	}

	/* Workhorse method */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	<T> T getAllInner(HttpPartParserSession parser, HttpPartSchema schema, String name, ClassMeta<T> cm) throws BadRequest, InternalServerError {
		String[] p = get(name);
		if (p == null)
			p = new String[0];
		if (schema == null)
			schema = HttpPartSchema.DEFAULT;
		try {
			if (cm.isArray()) {
				List c = new ArrayList();
				for (int i = 0; i < p.length; i++)
					c.add(parse(parser, schema.getItems(), p[i], cm.getElementType()));
				return (T)toArray(c, cm.getElementType().getInnerClass());
			} else if (cm.isCollection()) {
				Collection c = (Collection)(cm.canCreateNewInstance() ? cm.newInstance() : new OList());
				for (int i = 0; i < p.length; i++)
					c.add(parse(parser, schema.getItems(), p[i], cm.getElementType()));
				return (T)c;
			}
		} catch (SchemaValidationException e) {
			throw new BadRequest(e, "Validation failed on header ''{0}''. ", name);
		} catch (ParseException e) {
			throw new BadRequest(e, "Could not parse header ''{0}''.", name) ;
		} catch (Exception e) {
			throw new InternalServerError(e, "Could not parse header ''{0}''.", name);
		}
		throw new InternalServerError("Invalid call to getAll(String, ClassMeta).  Class type must be a Collection or array.");
	}

	/* Workhorse method */
	private <T> T parse(HttpPartParserSession parser, HttpPartSchema schema, String val, ClassMeta<T> cm) throws SchemaValidationException, ParseException {
		if (parser == null)
			parser = this.parser;
		return parser.parse(HttpPartType.HEADER, schema, val, cm);
	}

	/**
	 * Returns a copy of this object but only with the specified header names copied.
	 *
	 * @param headers The headers to include in the copy.
	 * @return A new headers object.
	 */
	public RequestHeaders subset(String...headers) {
		RequestHeaders rh2 = new RequestHeaders(req).parser(parser).queryParams(queryParams, allowedQueryParams);
		for (String h : headers)
			if (containsKey(h))
				rh2.put(h, get(h));
		return rh2;
	}

	/**
	 * Same as {@link #subset(String...)} but allows you to specify header names as a comma-delimited list.
	 *
	 * @param headers The headers to include in the copy.
	 * @return A new headers object.
	 */
	public RequestHeaders subset(String headers) {
		return subset(split(headers));
	}

	/**
	 * Returns the <c>Accept</c> header on the request.
	 *
	 * <p>
	 * Content-Types that are acceptable for the response.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Accept: text/plain
	 * </p>
	 *
	 * @return The parsed <c>Accept</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Accept getAccept() {
		return Accept.of(getString("Accept"));
	}

	/**
	 * Returns the <c>Accept-Charset</c> header on the request.
	 *
	 * <p>
	 * Character sets that are acceptable.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Accept-Charset: utf-8
	 * </p>
	 *
	 * @return The parsed <c>Accept-Charset</c> header on the request, or <jk>null</jk> if not found.
	 */
	public AcceptCharset getAcceptCharset() {
		return AcceptCharset.of(getString("Accept-Charset"));
	}

	/**
	 * Returns the <c>Accept-Encoding</c> header on the request.
	 *
	 * <p>
	 * List of acceptable encodings.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Accept-Encoding: gzip, deflate
	 * </p>
	 *
	 * @return The parsed <c>Accept-Encoding</c> header on the request, or <jk>null</jk> if not found.
	 */
	public AcceptEncoding getAcceptEncoding() {
		return AcceptEncoding.of(getString("Accept-Encoding"));
	}

	/**
	 * Returns the <c>Accept-Language</c> header on the request.
	 *
	 * <p>
	 * List of acceptable human languages for response.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Accept-Language: en-US
	 * </p>
	 *
	 * @return The parsed <c>Accept-Language</c> header on the request, or <jk>null</jk> if not found.
	 */
	public AcceptLanguage getAcceptLanguage() {
		return AcceptLanguage.of(getString("Accept-Language"));
	}

	/**
	 * Returns the <c>Authorization</c> header on the request.
	 *
	 * <p>
	 * Authentication credentials for HTTP authentication.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
	 * </p>
	 *
	 * @return The parsed <c>Authorization</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Authorization getAuthorization() {
		return Authorization.of(getString("Authorization"));
	}

	/**
	 * Returns the <c>Cache-Control</c> header on the request.
	 *
	 * <p>
	 * Used to specify directives that must be obeyed by all caching mechanisms along the request-response chain.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Cache-Control: no-cache
	 * </p>
	 *
	 * @return The parsed <c>Cache-Control</c> header on the request, or <jk>null</jk> if not found.
	 */
	public CacheControl getCacheControl() {
		return CacheControl.of(getString("Cache-Control"));
	}

	/**
	 * Returns the <c>Connection</c> header on the request.
	 *
	 * <p>
	 * Control options for the current connection and list of hop-by-hop request fields.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Connection: keep-alive
	 * 	Connection: Upgrade
	 * </p>
	 *
	 * @return The parsed <code></code> header on the request, or <jk>null</jk> if not found.
	 */
	public Connection getConnection() {
		return Connection.of(getString("Connection"));
	}

	/**
	 * Returns the <c>Content-Length</c> header on the request.
	 *
	 * <p>
	 * The length of the request body in octets (8-bit bytes).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Content-Length: 348
	 * </p>
	 *
	 * @return The parsed <c>Content-Length</c> header on the request, or <jk>null</jk> if not found.
	 */
	public ContentLength getContentLength() {
		return ContentLength.of(getString("Content-Length"));
	}

	/**
	 * Returns the <c>Content-Type</c> header on the request.
	 *
	 * <p>
	 * The MIME type of the body of the request (used with POST and PUT requests).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Content-Type: application/x-www-form-urlencoded
	 * </p>
	 *
	 * @return The parsed <c>Content-Type</c> header on the request, or <jk>null</jk> if not found.
	 */
	public ContentType getContentType() {
		return ContentType.of(getString("Content-Type"));
	}

	/**
	 * Returns the <c>Date</c> header on the request.
	 *
	 * <p>
	 * The date and time that the message was originated (in "HTTP-date" format as defined by RFC 7231 Date/Time Formats).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Date: Tue, 15 Nov 1994 08:12:31 GMT
	 * </p>
	 *
	 * @return The parsed <c>Date</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Date getDate() {
		return Date.of(getString("Date"));
	}

	/**
	 * Returns the <c>Expect</c> header on the request.
	 *
	 * <p>
	 * Indicates that particular server behaviors are required by the client.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Expect: 100-continue
	 * </p>
	 *
	 * @return The parsed <c>Expect</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Expect getExpect() {
		return Expect.of(getString("Expect"));
	}

	/**
	 * Returns the <c>From</c> header on the request.
	 *
	 * <p>
	 * The email address of the user making the request.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	From: user@example.com
	 * </p>
	 *
	 * @return The parsed <c>From</c> header on the request, or <jk>null</jk> if not found.
	 */
	public From getFrom() {
		return From.of(getString("From"));
	}

	/**
	 * Returns the <c>Host</c> header on the request.
	 *
	 * <p>
	 * The domain name of the server (for virtual hosting), and the TCP port number on which the server is listening.
	 * The port number may be omitted if the port is the standard port for the service requested.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Host: en.wikipedia.org:8080
	 * 	Host: en.wikipedia.org
	 * </p>
	 *
	 * @return The parsed <c>Host</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Host getHost() {
		return Host.of(getString("Host"));
	}

	/**
	 * Returns the <c>If-Match</c> header on the request.
	 *
	 * <p>
	 * Only perform the action if the client supplied entity matches the same entity on the server.
	 * This is mainly for methods like PUT to only update a resource if it has not been modified since the user last
	 * updated it.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-Match: "737060cd8c284d8af7ad3082f209582d"
	 * </p>
	 *
	 * @return The parsed <c>If-Match</c> header on the request, or <jk>null</jk> if not found.
	 */
	public IfMatch getIfMatch() {
		return IfMatch.of(getString("If-Match"));
	}

	/**
	 * Returns the <c>If-Modified-Since</c> header on the request.
	 *
	 * <p>
	 * Allows a 304 Not Modified to be returned if content is unchanged.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
	 * </p>
	 *
	 * @return The parsed <c>If-Modified-Since</c> header on the request, or <jk>null</jk> if not found.
	 */
	public IfModifiedSince getIfModifiedSince() {
		return IfModifiedSince.of(getString("If-Modified-Since"));
	}

	/**
	 * Returns the <c>If-None-Match</c> header on the request.
	 *
	 * <p>
	 * Allows a 304 Not Modified to be returned if content is unchanged, see HTTP ETag.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-None-Match: "737060cd8c284d8af7ad3082f209582d"
	 * </p>
	 *
	 * @return The parsed <c>If-None-Match</c> header on the request, or <jk>null</jk> if not found.
	 */
	public IfNoneMatch getIfNoneMatch() {
		return IfNoneMatch.of(getString("If-None-Match"));
	}

	/**
	 * Returns the <c>If-Range</c> header on the request.
	 *
	 * <p>
	 * If the entity is unchanged, send me the part(s) that I am missing; otherwise, send me the entire new entity.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-Range: "737060cd8c284d8af7ad3082f209582d"
	 * </p>
	 *
	 * @return The parsed <c>If-Range</c> header on the request, or <jk>null</jk> if not found.
	 */
	public IfRange getIfRange() {
		return IfRange.of(getString("If-Range"));
	}

	/**
	 * Returns the <c>If-Unmodified-Since</c> header on the request.
	 *
	 * <p>
	 * Only send the response if the entity has not been modified since a specific time.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-Unmodified-Since: Sat, 29 Oct 1994 19:43:31 GMT
	 * </p>
	 *
	 * @return The parsed <c>If-Unmodified-Since</c> header on the request, or <jk>null</jk> if not found.
	 */
	public IfUnmodifiedSince getIfUnmodifiedSince() {
		return IfUnmodifiedSince.of(getString("If-Unmodified-Since"));
	}

	/**
	 * Returns the <c>Max-Forwards</c> header on the request.
	 *
	 * <p>
	 * Limit the number of times the message can be forwarded through proxies or gateways.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Max-Forwards: 10
	 * </p>
	 *
	 * @return The parsed <c>Max-Forwards</c> header on the request, or <jk>null</jk> if not found.
	 */
	public MaxForwards getMaxForwards() {
		return MaxForwards.of(getString("Max-Forwards"));
	}

	/**
	 * Returns the <c>Pragma</c> header on the request.
	 *
	 * <p>
	 * Implementation-specific fields that may have various effects anywhere along the request-response chain.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Pragma: no-cache
	 * </p>
	 *
	 * @return The parsed <c>Pragma</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Pragma getPragma() {
		return Pragma.of(getString("Pragma"));
	}

	/**
	 * Returns the <c>Proxy-Authorization</c> header on the request.
	 *
	 * <p>
	 * Authorization credentials for connecting to a proxy.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Proxy-Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
	 * </p>
	 *
	 * @return The parsed <c>Proxy-Authorization</c> header on the request, or <jk>null</jk> if not found.
	 */
	public ProxyAuthorization getProxyAuthorization() {
		return ProxyAuthorization.of(getString("Proxy-Authorization"));
	}

	/**
	 * Returns the <c>Range</c> header on the request.
	 *
	 * <p>
	 * Request only part of an entity. Bytes are numbered from 0.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Range: bytes=500-999
	 * </p>
	 *
	 * @return The parsed <c>Range</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Range getRange() {
		return Range.of(getString("Range"));
	}

	/**
	 * Returns the <c>Referer</c> header on the request.
	 *
	 * <p>
	 * This is the address of the previous web page from which a link to the currently requested page was followed.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Referer: http://en.wikipedia.org/wiki/Main_Page
	 * </p>
	 *
	 * @return The parsed <c>Referer</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Referer getReferer() {
		return Referer.of(getString("Referer"));
	}

	/**
	 * Returns the <c>TE</c> header on the request.
	 *
	 * <p>
	 * The transfer encodings the user agent is willing to accept: the same values as for the response header field
	 * Transfer-Encoding can be used, plus the "trailers" value (related to the "chunked" transfer method) to notify the
	 * server it expects to receive additional fields in the trailer after the last, zero-sized, chunk.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	TE: trailers, deflate
	 * </p>
	 *
	 * @return The parsed <c>TE</c> header on the request, or <jk>null</jk> if not found.
	 */
	public TE getTE() {
		return TE.of(getString("TE"));
	}

	/**
	 * Returns the <c>Time-Zone</c> header value on the request if there is one.
	 *
	 * <p>
	 * Example: <js>"GMT"</js>.
	 *
	 * @return The <c>Time-Zone</c> header value on the request, or <jk>null</jk> if not present.
	 */
	public TimeZone getTimeZone() {
		String tz = getString("Time-Zone");
		if (tz != null)
			return TimeZone.getTimeZone(tz);
		return null;
	}

	/**
	 * Returns the <c>User-Agent</c> header on the request.
	 *
	 * <p>
	 * The user agent string of the user agent.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0
	 * </p>
	 *
	 * @return The parsed <c>User-Agent</c> header on the request, or <jk>null</jk> if not found.
	 */
	public UserAgent getUserAgent() {
		return UserAgent.of(getString("User-Agent"));
	}

	/**
	 * Returns the <c>Upgrade</c> header on the request.
	 *
	 * <p>
	 * Ask the server to upgrade to another protocol.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Upgrade: HTTP/2.0, HTTPS/1.3, IRC/6.9, RTA/x11, websocket
	 * </p>
	 *
	 * @return The parsed <c>Upgrade</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Upgrade getUpgrade() {
		return Upgrade.of(getString("Upgrade"));
	}

	/**
	 * Returns the <c>Via</c> header on the request.
	 *
	 * <p>
	 * Informs the server of proxies through which the request was sent.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Via: 1.0 fred, 1.1 example.com (Apache/1.1)
	 * </p>
	 *
	 * @return The parsed <c>Via</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Via getVia() {
		return Via.of(getString("Via"));
	}

	/**
	 * Returns the <c>Warning</c> header on the request.
	 *
	 * <p>
	 * A general warning about possible problems with the entity body.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Warning: 199 Miscellaneous warning
	 * </p>
	 *
	 * @return The parsed <c>Warning</c> header on the request, or <jk>null</jk> if not found.
	 */
	public Warning getWarning() {
		return Warning.of(getString("Warning"));
	}

	/**
	 * Converts the headers to a readable string.
	 *
	 * @param sorted Sort the headers by name.
	 * @return A JSON string containing the contents of the headers.
	 */
	public String toString(boolean sorted) {
		Map<String,Object> m = null;
		if (sorted)
			m = new TreeMap<>();
		else
			m = new LinkedHashMap<>();
		for (Map.Entry<String,String[]> e : this.entrySet()) {
			String[] v = e.getValue();
			m.put(e.getKey(), v.length == 1 ? v[0] : v);
		}
		return SimpleJsonSerializer.DEFAULT.toString(m);
	}

	@Override /* Object */
	public String toString() {
		return toString(false);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private <T> ClassMeta<T> getClassMeta(Type type, Type...args) {
		return req.getBeanSession().getClassMeta(type, args);
	}

	private <T> ClassMeta<T> getClassMeta(Class<T> type) {
		return req.getBeanSession().getClassMeta(type);
	}
}
