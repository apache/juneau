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
import java.lang.reflect.Type;
import java.util.*;

import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.http.exception.*;

/**
 * Represents the parsed form-data parameters in an HTTP request.
 *
 * <p>
 * Similar in functionality to the {@link HttpServletRequest#getParameter(String)} except only looks in the body of the request, not parameters from
 * the URL query string.
 * <br>This can be useful in cases where you're using GET parameters on FORM POSTs, and you don't want the body of the request to be read.
 *
 * <p>
 * Use of this object is incompatible with using any other methods that access the body of the request (since this object will
 * consume the body).
 * <br>Some examples:
 * <ul>
 * 	<li class='jm'>{@link RestRequest#getBody()}
 * 	<li class='jm'>{@link RestRequest#getReader()}
 * 	<li class='jm'>{@link RestRequest#getInputStream()}
 * 	<li class='ja'>{@link FormData}
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmRequestFormData}
 * </ul>
 */
@SuppressWarnings("unchecked")
public class RequestFormData extends LinkedHashMap<String,String[]> {
	private static final long serialVersionUID = 1L;

	private final RestRequest req;
	private final HttpPartParserSession parser;

	RequestFormData(RestRequest req, HttpPartParserSession parser) {
		this.req = req;
		this.parser = parser;
	}

	/**
	 * Adds default entries to these form-data parameters.
	 *
	 * <p>
	 * This includes the default form-data parameters defined on the resource and method levels.
	 *
	 * @param defaultEntries
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestFormData addDefault(Map<String,Object> defaultEntries) {
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
	 * Adds default entries to these form-data parameters.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestFormData addDefault(Collection<NameValuePair> pairs) {
		for (NameValuePair p : pairs) {
			String key = p.getName();
			Object value = p.getValue();
			String[] v = get(key);
			if (v == null || v.length == 0 || StringUtils.isEmpty(v[0]))
				put(key, stringifyAll(value));
		}
		return this;
	}

	/**
	 * Adds a default entries to these form-data parameters.
	 *
	 * <p>
	 * Similar to {@link #put(String, Object)} but doesn't override existing values.
	 *
	 * @param name
	 * 	The form-data parameter name.
	 * @param value
	 * 	The form-data parameter value.
	 * 	<br>Converted to a String using <c>toString()</c>.
	 * 	<br>Ignored if value is <jk>null</jk> or blank.
	 * @return This object (for method chaining).
	 */
	public RequestFormData addDefault(String name, Object value) {
		return addDefault(Collections.singletonMap(name, value));
	}

	/**
	 * Sets a request form-data parameter value.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public void put(String name, Object value) {
		super.put(name, stringifyAll(value));
	}

	/**
	 * Returns a form-data parameter value.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Parameter lookup is case-insensitive (consistent with WAS, but differs from Tomcat).
	 * 	<li>
	 * 		This method returns the raw unparsed value, and differs from calling
	 * 		<code>get(name, String.<jk>class</jk>)</code> which uses the {@link HttpPartParser} for parsing the value.
	 * </ul>
	 *
	 * @param name The form-data parameter name.
	 * @return The parameter value, or <jk>null</jk> if parameter does not exist.
	 */
	public String getString(String name) {
		String[] v = get(name);
		if (v == null || v.length == 0)
			return null;

		// Fix for behavior difference between Tomcat and WAS.
		// getParameter("foo") on "&foo" in Tomcat returns "".
		// getParameter("foo") on "&foo" in WAS returns null.
		if (v.length == 1 && v[0] == null)
			return "";

		return v[0];
	}

	/**
	 * Returns a form-data parameter value.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Parameter lookup is case-insensitive (consistent with WAS, but differs from Tomcat).
	 * 	<li>
	 * 		This method returns the raw unparsed value, and differs from calling
	 * 		<code>get(name, String.<jk>class</jk>)</code> which uses the {@link HttpPartParser} for parsing the value.
	 * </ul>
	 *
	 * @param name The form-data parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public String getString(String name, String def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : s;
	}

	/**
	 * Returns a form-data parameter value as an integer.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Parameter lookup is case-insensitive (consistent with WAS, but differs from Tomcat).
	 * 	<li>
	 * 		This method returns the raw unparsed value, and differs from calling
	 * 		<code>get(name, Integer.<jk>class</jk>)</code> which uses the {@link HttpPartParser} for parsing the value.
	 * </ul>
	 *
	 * @param name The form-data parameter name.
	 * @return The parameter value, or <c>0</c> if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public int getInt(String name) {
		return getInt(name, 0);
	}

	/**
	 * Returns a form-data parameter value as an integer.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Parameter lookup is case-insensitive (consistent with WAS, but differs from Tomcat).
	 * 	<li>
	 * 		This method returns the raw unparsed value, and differs from calling
	 * 		<code>get(name, Integer.<jk>class</jk>)</code> which uses the {@link HttpPartParser} for parsing the value.
	 * </ul>
	 *
	 * @param name The form-data parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public int getInt(String name, int def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Integer.parseInt(s);
	}

	/**
	 * Returns a form-data parameter value as a boolean.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Parameter lookup is case-insensitive (consistent with WAS, but differs from Tomcat).
	 * 	<li>
	 * 		This method returns the raw unparsed value, and differs from calling
	 * 		<code>get(name, Boolean.<jk>class</jk>)</code> which uses the {@link HttpPartParser} for parsing the value.
	 * </ul>
	 *
	 * @param name The form-data parameter name.
	 * @return The parameter value, or <jk>false</jk> if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public boolean getBoolean(String name) {
		return getBoolean(name, false);
	}

	/**
	 * Returns a form-data parameter value as a boolean.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Parameter lookup is case-insensitive (consistent with WAS, but differs from Tomcat).
	 * 	<li>
	 * 		This method returns the raw unparsed value, and differs from calling
	 * 		<code>get(name, Boolean.<jk>class</jk>)</code> which uses the {@link HttpPartParser} for parsing the value.
	 * </ul>
	 *
	 * @param name The form-data parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public boolean getBoolean(String name, boolean def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Boolean.parseBoolean(s);
	}

	/**
	 * Returns the specified form-data parameter value converted to a POJO using the {@link HttpPartParser} registered with the resource.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myparam = formData.get(<js>"myparam"</js>, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into an int array.</jc>
	 * 	<jk>int</jk>[] myparam = formData.get(<js>"myparam"</js>, <jk>int</jk>[].<jk>class</jk>);

	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean myparam = formData.get(<js>"myparam"</js>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List myparam = formData.get(<js>"myparam"</js>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map myparam = formData.get(<js>"myparam"</js>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(String name, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(null, null, name, null, getClassMeta(type));
	}

	/**
	 * Returns the specified form-data parameter value converted to a POJO using the {@link HttpPartParser} registered with the resource.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Pipe-delimited list of comma-delimited numbers</jc>
	 * 	HttpPartSchema schema = HttpPartSchema.<jsm>create</jsm>()
	 * 		.items(
	 * 			HttpPartSchema.<jsm>create</jsm>()
	 * 			.collectionFormat(<js>"pipes"</js>)
	 * 			.items(
	 * 				HttpPartSchema.<jsm>create</jsm>()
	 * 				.collectionFormat(<js>"csv"</js>)
	 * 				.type(<js>"integer"</js>)
	 * 				.format(<js>"int64"</js>)
	 * 				.minimum(<js>"0"</js>)
	 * 				.maximum(<js>"100"</js>)
	 * 				.minLength(1)
	 * 				.maxLength=(10)
	 * 			)
	 * 		)
	 * 		.build();
	 *
	 * 	<jc>// Parse into a 2d long array.</jc>
	 * 	<jk>long</jk>[][] myparams = formData.get(schema, <js>"myparam"</js>, <jk>long</jk>[][].<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(HttpPartSchema schema, String name, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(null, schema, name, null, getClassMeta(type));
	}

	/**
	 * Returns the specified form-data parameter value converted to a POJO using the specified {@link HttpPartParser}.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Pipe-delimited list of comma-delimited numbers</jc>
	 * 	HttpPartSchema schema = HttpPartSchema.<jsm>create</jsm>()
	 * 		.items(
	 * 			HttpPartSchema.<jsm>create</jsm>()
	 * 			.collectionFormat(<js>"pipes"</js>)
	 * 			.items(
	 * 				HttpPartSchema.<jsm>create</jsm>()
	 * 				.collectionFormat(<js>"csv"</js>)
	 * 				.type(<js>"integer"</js>)
	 * 				.format(<js>"int64"</js>)
	 * 				.minimum(<js>"0"</js>)
	 * 				.maximum(<js>"100"</js>)
	 * 				.minLength(1)
	 * 				.maxLength=(10)
	 * 			)
	 * 		)
	 * 		.build();
	 *
	 *  HttpPartParserSession parser = OpenApiParser.<jsf>DEFAULT</jsf>.createSession();
	 *
	 * 	<jc>// Parse into a 2d long array.</jc>
	 * 	<jk>long</jk>[][] myparams = formData.get(parser, schema, <js>"myparam"</js>, <jk>long</jk>[][].<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed or fails schema validation.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(HttpPartParserSession parser, HttpPartSchema schema, String name, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(parser, schema, name, null, getClassMeta(type));
	}

	/**
	 * Returns the specified form-data parameter value converted to a POJO using the {@link HttpPartParser} registered with the resource.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myparam = formData.get(<js>"myparam"</js>, -1, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into an int array.</jc>
	 * 	<jk>int</jk>[] myparam = formData.get(<js>"myparam"</js>, <jk>new int</jk>[0], <jk>int</jk>[].<jk>class</jk>);

	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean myparam = formData.get(<js>"myparam"</js>, <jk>new</jk> MyBean(), MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List myparam = formData.get(<js>"myparam"</js>, Collections.<jsm>emptyList</jsm>(), LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map myparam = formData.get(<js>"myparam"</js>, Collections.<jsm>emptyMap</jsm>(), TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param name The parameter name.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(String name, T def, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(null, null, name, def, getClassMeta(type));
	}

	/**
	 * Returns the specified form-data parameter value converted to a POJO using the {@link HttpPartParser} registered with the resource.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Pipe-delimited list of comma-delimited numbers</jc>
	 * 	HttpPartSchema schema = HttpPartSchema.<jsm>create</jsm>()
	 * 		.items(
	 * 			HttpPartSchema.<jsm>create</jsm>()
	 * 			.collectionFormat(<js>"pipes"</js>)
	 * 			.items(
	 * 				HttpPartSchema.<jsm>create</jsm>()
	 * 				.collectionFormat(<js>"csv"</js>)
	 * 				.type(<js>"integer"</js>)
	 * 				.format(<js>"int64"</js>)
	 * 				.minimum(<js>"0"</js>)
	 * 				.maximum(<js>"100"</js>)
	 * 				.minLength(1)
	 * 				.maxLength=(10)
	 * 			)
	 * 		)
	 * 		.build();
	 *
	 * 	<jc>// Parse into a 2d long array.</jc>
	 * 	<jk>long</jk>[][] myparams = formData.get(schema, <js>"myparam"</js>, <jk>new long</jk>[][0], <jk>long</jk>[][].<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The parameter name.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(HttpPartSchema schema, String name, T def, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(null, schema, name, def, getClassMeta(type));
	}

	/**
	 * Returns the specified form-data parameter value converted to a POJO using the specified {@link HttpPartParser}.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Pipe-delimited list of comma-delimited numbers</jc>
	 * 	HttpPartSchema schema = HttpPartSchema.<jsm>create</jsm>()
	 * 		.items(
	 * 			HttpPartSchema.<jsm>create</jsm>()
	 * 			.collectionFormat(<js>"pipes"</js>)
	 * 			.items(
	 * 				HttpPartSchema.<jsm>create</jsm>()
	 * 				.collectionFormat(<js>"csv"</js>)
	 * 				.type(<js>"integer"</js>)
	 * 				.format(<js>"int64"</js>)
	 * 				.minimum(<js>"0"</js>)
	 * 				.maximum(<js>"100"</js>)
	 * 				.minLength(1)
	 * 				.maxLength=(10)
	 * 			)
	 * 		)
	 * 		.build();
	 *
	 *  HttpPartParserSession parser = OpenApiParser.<jsf>DEFAULT</jsf>.createSession();
	 *
	 * 	<jc>// Parse into a 2d long array.</jc>
	 * 	<jk>long</jk>[][] myparams = formData.get(parser, schema, <js>"myparam"</js>, <jk>new long</jk>[][0], <jk>long</jk>[][].<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The parameter name.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed or fails schema validation.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(HttpPartParserSession parser, HttpPartSchema schema, String name, T def, Class<T> type) throws BadRequest, InternalServerError {
		return getInner(parser, schema, name, def, getClassMeta(type));
	}

	/**
	 * Returns the specified form-data parameter value converted to a POJO using the {@link HttpPartParser} registered with the resource.
	 *
	 * <p>
	 * Similar to {@link #get(String,Class)} but allows for complex collections of POJOs to be created.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; myparam = formData.get(<js>"myparam"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; myparam = formData.get(<js>"myparam"</js>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; myparam = formData.getr(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; myparam = formData.get(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		<c>Collections</c> must be followed by zero or one parameter representing the value type.
	 * 	<li>
	 * 		<c>Maps</c> must be followed by zero or two parameters representing the key and value types.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(String name, Type type, Type...args) throws BadRequest, InternalServerError {
		return getInner(null, null, name, null, this.<T>getClassMeta(type, args));
	}

	/**
	 * Same as {@link #get(String, Type, Type...)} but allows you to override the part parser.
	 *
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed or fails schema validation.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(HttpPartParserSession parser, HttpPartSchema schema, String name, Type type, Type...args) throws BadRequest, InternalServerError {
		return getInner(parser, schema, name, null, this.<T>getClassMeta(type, args));
	}

	/**
	 * Same as {@link #get(String, Class)} except returns a default value if not found.
	 *
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(String name, T def, Type type, Type...args) throws BadRequest, InternalServerError {
		return getInner(null, null, name, def, this.<T>getClassMeta(type, args));
	}

	/**
	 * Same as {@link #get(String, Object, Type, Type...)} but allows you to override the part parser.
	 *
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed or fails schema validation.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T get(HttpPartParserSession parser, HttpPartSchema schema, String name, T def, Type type, Type...args) throws BadRequest, InternalServerError {
		return getInner(parser, schema, name, def, this.<T>getClassMeta(type, args));
	}

	/**
	 * Returns the specified form-data parameter values converted POJO using the {@link HttpPartParser} registered with the resource.
	 *
	 * <p>
	 * Meant to be used on multi-part parameters (e.g. <js>"key=1&amp;key=2&amp;key=3"</js> instead of <js>"key=@(1,2,3)"</js>)
	 *
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into multiple integers.</jc>
	 * 	<jk>int</jk>[] myparam = formData.getAll(<js>"myparam"</js>, <jk>int</jk>[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into multiple int arrays.</jc>
	 * 	<jk>int</jk>[][] myparam = formData.getAll(<js>"myparam"</js>, <jk>int</jk>[][].<jk>class</jk>);

	 * 	<jc>// Parse into multiple beans.</jc>
	 * 	MyBean[] myparam = formData.getAll(<js>"myparam"</js>, MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into multiple linked-lists of objects.</jc>
	 * 	List[] myparam = formData.getAll(<js>"myparam"</js>, LinkedList[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into multiple maps of object keys/values.</jc>
	 * 	Map[] myparam = formData.getAll(<js>"myparam"</js>, TreeMap[].<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T getAll(String name, Class<T> type) throws BadRequest, InternalServerError {
		return getAllInner(null, null, name, getClassMeta(type));
	}

	/**
	 * Returns the specified form-data parameter values converted to POJOs using the {@link HttpPartParser} registered with the resource.
	 *
	 * <p>
	 * Meant to be used on multi-part parameters (e.g. <js>"key=1&amp;key=2&amp;key=3"</js> instead of <js>"key=@(1,2,3)"</js>)
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Pipe-delimited list of comma-delimited numbers</jc>
	 * 	HttpPartSchema schema = HttpPartSchema.<jsm>create</jsm>()
	 * 		.items(
	 * 			HttpPartSchema.<jsm>create</jsm>()
	 * 			.collectionFormat(<js>"pipes"</js>)
	 * 			.items(
	 * 				HttpPartSchema.<jsm>create</jsm>()
	 * 				.collectionFormat(<js>"csv"</js>)
	 * 				.type(<js>"integer"</js>)
	 * 				.format(<js>"int64"</js>)
	 * 				.minimum(<js>"0"</js>)
	 * 				.maximum(<js>"100"</js>)
	 * 				.minLength(1)
	 * 				.maxLength=(10)
	 * 			)
	 * 		)
	 * 		.build();
	 *
	 * 	<jc>// Parse into multiple 2d long arrays.</jc>
	 * 	<jk>long</jk>[][][] myparams = formData.getAll(schema, <js>"myparam"</js>, <jk>long</jk>[][][].<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T getAll(HttpPartSchema schema, String name, Class<T> type) throws BadRequest, InternalServerError {
		return getAllInner(null, schema, name, getClassMeta(type));
	}

	/**
	 * Returns the specified form-data parameter values converted to POJOs using the specified {@link HttpPartParser}.
	 *
	 * <p>
	 * Meant to be used on multi-part parameters (e.g. <js>"key=1&amp;key=2&amp;key=3"</js> instead of <js>"key=@(1,2,3)"</js>)
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Pipe-delimited list of comma-delimited numbers</jc>
	 * 	HttpPartSchema schema = HttpPartSchema.<jsm>create</jsm>()
	 * 		.items(
	 * 			HttpPartSchema.<jsm>create</jsm>()
	 * 			.collectionFormat(<js>"pipes"</js>)
	 * 			.items(
	 * 				HttpPartSchema.<jsm>create</jsm>()
	 * 				.collectionFormat(<js>"csv"</js>)
	 * 				.type(<js>"integer"</js>)
	 * 				.format(<js>"int64"</js>)
	 * 				.minimum(<js>"0"</js>)
	 * 				.maximum(<js>"100"</js>)
	 * 				.minLength(1)
	 * 				.maxLength=(10)
	 * 			)
	 * 		)
	 * 		.build();
	 *
	 * 	<jc>// Parse into multiple 2d long arrays.</jc>
	 * 	<jk>long</jk>[][][] myparams = formData.getAll(schema, <js>"myparam"</js>, <jk>long</jk>[][][].<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed or fails schema validation.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T getAll(HttpPartParserSession parser, HttpPartSchema schema, String name, Class<T> type) throws BadRequest, InternalServerError {
		return getAllInner(parser, schema, name, getClassMeta(type));
	}

	/**
	 * Same as {@link #get(String, Type, Type...)} except for use on multi-part parameters
	 * (e.g. <js>"key=1&amp;key=2&amp;key=3"</js> instead of <js>"key=@(1,2,3)"</js>)
	 *
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parameter value converted to the specified class type.
	 * @throws BadRequest Thrown if input could not be parsed.
	 * @throws InternalServerError Thrown if any other exception occurs.
	 */
	public <T> T getAll(String name, Type type, Type...args) throws BadRequest, InternalServerError {
		return getAllInner(null, null, name, this.<T>getClassMeta(type, args));
	}

	/**
	 * Same as {@link #getAll(String, Type, Type...)} but allows you to override the part parser.
	 *
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the resource/method.
	 * @param schema
	 * 	The schema object that defines the format of the input.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if parser is schema-aware (e.g. {@link OpenApiParser}).
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
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
			parser = req.getPartParser();
		try {
			if (cm.isMapOrBean() && isOneOf(name, "*", "")) {
				OMap m = new OMap();
				for (Map.Entry<String,String[]> e : this.entrySet()) {
					String k = e.getKey();
					HttpPartSchema pschema = schema == null ? null : schema.getProperty(k);
					ClassMeta<?> cm2 = cm.getValueType();
					if (cm.getValueType().isCollectionOrArray())
						m.put(k, getAllInner(parser, pschema, k, cm2));
					else
						m.put(k, getInner(parser, pschema, k, null, cm2));
				}
				return req.getBeanSession().convertToType(m, cm);
			}
			T t = parse(parser, schema, getString(name), cm);
			return (t == null ? def : t);
		} catch (SchemaValidationException e) {
			throw new BadRequest(e, "Validation failed on form-data parameter ''{0}''. ", name);
		} catch (ParseException e) {
			throw new BadRequest(e, "Could not parse form-data parameter ''{0}''.", name) ;
		} catch (Exception e) {
			throw new InternalServerError(e, "Could not parse form-data parameter ''{0}''.", name) ;
		}
	}

	/* Workhorse method */
	@SuppressWarnings("rawtypes")
	<T> T getAllInner(HttpPartParserSession parser, HttpPartSchema schema, String name, ClassMeta<T> cm) throws BadRequest, InternalServerError {
		String[] p = get(name);
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
			throw new BadRequest(e, "Validation failed on form-data parameter ''{0}''. ", name);
		} catch (ParseException e) {
			throw new BadRequest(e, "Could not parse form-data parameter ''{0}''.", name) ;
		} catch (Exception e) {
			throw new InternalServerError(e, "Could not parse form-data parameter ''{0}''.", name) ;
		}
		throw new InternalServerError("Invalid call to getAll(String, ClassMeta).  Class type must be a Collection or array.");
	}

	private <T> T parse(HttpPartParserSession parser, HttpPartSchema schema, String val, ClassMeta<T> c) throws SchemaValidationException, ParseException {
		if (parser == null)
			parser = this.parser;
		return parser.parse(HttpPartType.FORMDATA, schema, val, c);
	}

	/**
	 * Converts the form-data parameters to a readable string.
	 *
	 * @param sorted Sort the form-data parameters by name.
	 * @return A JSON string containing the contents of the form-data parameters.
	 */
	public String toString(boolean sorted) {
		Map<String,Object> m = null;
		if (sorted)
			m = new TreeMap<>();
		else
			m = new LinkedHashMap<>();
		for (Map.Entry<String,String[]> e : this.entrySet()) {
			String[] v = e.getValue();
			if (v != null)
				m.put(e.getKey(), v.length == 1 ? v[0] : v);
		}
		return SimpleJsonSerializer.DEFAULT.toString(m);
	}

	/**
	 * Converts this object to a query string.
	 *
	 * <p>
	 * Returned query string does not start with <js>'?'</js>.
	 *
	 * @return A new query string, or an empty string if this object is empty.
	 */
	public String asQueryString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String,String[]> e : this.entrySet()) {
			for (int i = 0; i < e.getValue().length; i++) {
				if (sb.length() > 0)
					sb.append("&");
				sb.append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()[i]));
			}
		}
		return sb.toString();
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
