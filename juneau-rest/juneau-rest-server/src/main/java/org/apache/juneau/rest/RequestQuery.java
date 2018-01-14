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

import java.lang.reflect.*;
import java.util.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Represents the query parameters in an HTTP request.
 */
@SuppressWarnings("unchecked")
public final class RequestQuery extends LinkedHashMap<String,String[]> {
	private static final long serialVersionUID = 1L;

	private HttpPartParser parser;
	private BeanSession beanSession;

	RequestQuery setParser(HttpPartParser parser) {
		this.parser = parser;
		return this;
	}

	RequestQuery setBeanSession(BeanSession beanSession) {
		this.beanSession = beanSession;
		return this;
	}

	/**
	 * Create a copy of the request query parameters.
	 */
	RequestQuery copy() {
		RequestQuery rq = new RequestQuery();
		rq.putAll(this);
		return rq;
	}

	/**
	 * Adds default entries to these query parameters.
	 * 
	 * <p>
	 * This includes the default queries defined on the servlet and method levels.
	 * 
	 * @param defaultEntries The default entries.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestQuery addDefault(Map<String,Object> defaultEntries) {
		if (defaultEntries != null) {
			for (Map.Entry<String,Object> e : defaultEntries.entrySet()) {
				String key = e.getKey();
				Object value = e.getValue();
				String[] v = get(key);
				if (v == null || v.length == 0 || StringUtils.isEmpty(v[0]))
					put(key, new String[]{StringUtils.toString(value)});
			}
		}
		return this;
	}

	/**
	 * Sets a request query parameter value.
	 * 
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public void put(String name, Object value) {
		put(name, new String[]{StringUtils.toString(value)});
	}

	/**
	 * Returns a query parameter value.
	 * 
	 * <p>
	 * Same as {@link HttpServletRequest#getParameter(String)} except only looks in the URL string, not parameters from
	 * URL-Encoded FORM posts.
	 * 
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying servlet API to load and parse
	 * the request body.
	 * 
	 * <p>
	 * If multiple query parameters have the same name, this returns only the first instance.
	 * 
	 * @param name The URL parameter name.
	 * @return The parameter value, or <jk>null</jk> if parameter not specified or has no value (e.g. <js>"&amp;foo"</js>.
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
	 * Same as {@link #getString(String)} but returns the specified default value if the query parameter was not
	 * specified.
	 * 
	 * @param name The URL parameter name.
	 * @param def The default value.
	 * @return
	 * 	The parameter value, or the default value if parameter not specified or has no value
	 * 	(e.g. <js>"&amp;foo"</js>.
	 */
	public String getString(String name, String def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : s;
	}

	/**
	 * Same as {@link #getString(String)} but converts the value to an integer.
	 * 
	 * @param name The URL parameter name.
	 * @return
	 * 	The parameter value, or <code>0</code> if parameter not specified or has no value
	 * 	(e.g. <js>"&amp;foo"</js>.
	 */
	public int getInt(String name) {
		return getInt(name, 0);
	}

	/**
	 * Same as {@link #getString(String,String)} but converts the value to an integer.
	 * 
	 * @param name The URL parameter name.
	 * @param def The default value.
	 * @return
	 * 	The parameter value, or the default value if parameter not specified or has no value
	 * 	(e.g. <js>"&amp;foo"</js>.
	 */
	public int getInt(String name, int def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Integer.parseInt(s);
	}

	/**
	 * Same as {@link #getString(String)} but converts the value to a boolean.
	 * 
	 * @param name The URL parameter name.
	 * @return
	 * 	The parameter value, or <jk>false</jk> if parameter not specified or has no value
	 * 	(e.g. <js>"&amp;foo"</js>.
	 */
	public boolean getBoolean(String name) {
		return getBoolean(name, false);
	}

	/**
	 * Same as {@link #getString(String,String)} but converts the value to a boolean.
	 * 
	 * @param name The URL parameter name.
	 * @param def The default value.
	 * @return
	 * 	The parameter value, or the default value if parameter not specified or has no value
	 * 	(e.g. <js>"&amp;foo"</js>.
	 */
	public boolean getBoolean(String name, boolean def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Boolean.parseBoolean(s);
	}

	/**
	 * Returns the specified query parameter value converted to a POJO.
	 * 
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying servlet API to load and parse
	 * the request body.
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myparam = req.getQueryParameter(<js>"myparam"</js>, <jk>int</jk>.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into an int array.</jc>
	 * 	<jk>int</jk>[] myparam = req.getQueryParameter(<js>"myparam"</js>, <jk>int</jk>[].<jk>class</jk>);

	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean myparam = req.getQueryParameter(<js>"myparam"</js>, MyBean.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List myparam = req.getQueryParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map myparam = req.getQueryParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>);
	 * </p>
	 * 
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(String name, Class<T> type) throws ParseException {
		return get(null, name, type);
	}

	/**
	 * Same as {@link #get(String, Class)} but allows you to override the part parser.
	 * 
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the servlet/method. 
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(HttpPartParser parser, String name, Class<T> type) throws ParseException {
		return get(parser, name, getClassMeta(type));
	}

	/**
	 * Same as {@link #get(String, Class)} except returns a default value if not found.
	 * 
	 * @param name The parameter name.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(String name, T def, Class<T> type) throws ParseException {
		return get(null, name, def, type);
	}

	/**
	 * Same as {@link #get(String, Object, Class)} but allows you to override the part parser.
	 * 
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the servlet/method. 
	 * @param name The parameter name.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(HttpPartParser parser, String name, T def, Class<T> type) throws ParseException {
		return get(parser, name, def, getClassMeta(type));
	}

	/**
	 * Returns the specified query parameter value converted to a POJO.
	 * 
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying servlet API to load and parse
	 * the request body.
	 * 
	 * <p>
	 * Use this method if you want to parse into a parameterized <code>Map</code>/<code>Collection</code> object.
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 * 
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(String name, Type type, Type...args) throws ParseException {
		return get((HttpPartParser)null, name, type, args);
	}

	/**
	 * Same as {@link #get(String, Type, Type...)} but allows you to override the part parser.
	 * 
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the servlet/method. 
	 * 
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(HttpPartParser parser, String name, Type type, Type...args) throws ParseException {
		return (T)parse(parser, name, getClassMeta(type, args));
	}

	/**
	 * Same as {@link #get(String, Class)} except returns a default value if not found.
	 * 
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(String name, Object def, Type type, Type...args) throws ParseException {
		return get(null, name, def, type, args);
	}

	/**
	 * Same as {@link #get(String, Object, Type, Type...)} but allows you to override the part parser.
	 * 
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the servlet/method. 
	 * @param name The parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(HttpPartParser parser, String name, Object def, Type type, Type...args) throws ParseException {
		return (T)parse(parser, name, def, getClassMeta(type, args));
	}

	/**
	 * Same as {@link #get(String, Class)} except for use on multi-part parameters
	 * (e.g. <js>"&amp;key=1&amp;key=2&amp;key=3"</js> instead of <js>"&amp;key=(1,2,3)"</js>).
	 * 
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 * 
	 * @param name The query parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The query parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAll(String name, Class<T> c) throws ParseException {
		return getAll(name, beanSession.getClassMeta(c));
	}

	/**
	 * Same as {@link #get(String, Type, Type...)} except for use on multi-part parameters
	 * (e.g. <js>"&amp;key=1&amp;key=2&amp;key=3"</js> instead of <js>"&amp;key=(1,2,3)"</js>).
	 * 
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 * 
	 * @param name The query parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The query parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAll(String name, Type type, Type...args) throws ParseException {
		return getAll(null, name, type, args);
	}

	/**
	 * Same as {@link #getAll(String, Type, Type...)} but allows you to override the part parser.
	 * 
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the servlet/method. 
	 * @param name The query parameter name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The query parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAll(HttpPartParser parser, String name, Type type, Type...args) throws ParseException {
		return (T)parseAll(parser, name, getClassMeta(type, args));
	}

	/**
	 * Returns <jk>true</jk> if the request contains any of the specified query parameters.
	 * 
	 * @param params The list of parameters to check for.
	 * @return <jk>true</jk> if the request contains any of the specified query parameters.
	 */
	public boolean containsAnyKeys(String...params) {
		for (String p : params)
			if (containsKey(p))
				return true;
		return false;
	}

	/**
	 * Locates the special search query arguments in the query and returns them as a {@link SearchArgs} object.
	 * 
	 * <p>
	 * The query arguments are as follows:
	 * <ul>
	 * 	<li>
	 * 		<js>"&amp;s="</js> - A comma-delimited list of column-name/search-token pairs.
	 * 		<br>Example: <js>"&amp;s=column1=foo*,column2=*bar"</js>
	 * 	<li>
	 * 		<js>"&amp;v="</js> - A comma-delimited list column names to view.
	 * 		<br>Example: <js>"&amp;v=column1,column2"</js>
	 * 	<li>
	 * 		<js>"&amp;o="</js> - A comma-delimited list column names to sort by.
	 * 		<br>Column names can be suffixed with <js>'-'</js> to indicate descending order.
	 * 		<br>Example: <js>"&amp;o=column1,column2-"</js>
	 * 	<li>
	 * 		<js>"&amp;p="</js> - The zero-index row number of the first row to display.
	 * 		<br>Example: <js>"&amp;p=100"</js>
	 * 	<li>
	 * 		<js>"&amp;l="</js> - The number of rows to return.
	 * 		<br><code>0</code> implies return all rows.
	 * 		<br>Example: <js>"&amp;l=100"</js>
	 * 	<li>
	 * 		<js>"&amp;i="</js> - The case-insensitive search flag.
	 * 		<br>Example: <js>"&amp;i=true"</js>
	 * </ul>
	 * 
	 * <p>
	 * Whitespace is trimmed in the parameters.
	 * 
	 * @return
	 * 	A new {@link SearchArgs} object initialized with the special search query arguments.
	 * 	<jk>null</jk> if no search arguments were found.
	 */
	public SearchArgs getSearchArgs() {
		if (hasAny("s","v","o","p","l","i")) {
			return new SearchArgs.Builder()
				.search(getString("s"))
				.view(getString("v"))
				.sort(getString("o"))
				.position(getInt("p"))
				.limit(getInt("l"))
				.ignoreCase(getBoolean("i"))
				.build();
		}
		return null;
	}

	/**
	 * Returns <jk>true</jk> if the query parameters contains any of the specified names.
	 * 
	 * @param paramNames The parameter names to check for.
	 * @return <jk>true</jk> if the query parameters contains any of the specified names.
	 */
	public boolean hasAny(String...paramNames) {
		for (String p : paramNames)
			if (containsKey(p))
				return true;
		return false;
	}

	/* Workhorse method */
	private <T> T parse(HttpPartParser parser, String name, Object def, ClassMeta<T> cm) throws ParseException {
		String val = getString(name);
		if (val == null)
			return (T)def;
		return parseValue(parser, val, cm);
	}

	/* Workhorse method */
	private <T> T parse(HttpPartParser parser, String name, ClassMeta<T> cm) throws ParseException {
		String val = getString(name);
		if (cm.isPrimitive() && (val == null || val.isEmpty()))
			return cm.getPrimitiveDefault();
		return parseValue(parser, val, cm);
	}

	/* Workhorse method */
	@SuppressWarnings("rawtypes")
	private <T> T parseAll(HttpPartParser parser, String name, ClassMeta<T> cm) throws ParseException {
		String[] p = get(name);
		if (p == null)
			return null;
		if (cm.isArray()) {
			List c = new ArrayList();
			for (int i = 0; i < p.length; i++)
				c.add(parseValue(parser, p[i], cm.getElementType()));
			return (T)toArray(c, cm.getElementType().getInnerClass());
		} else if (cm.isCollection()) {
			try {
				Collection c = (Collection)(cm.canCreateNewInstance() ? cm.newInstance() : new ObjectList());
				for (int i = 0; i < p.length; i++)
					c.add(parseValue(parser, p[i], cm.getElementType()));
				return (T)c;
			} catch (ParseException e) {
				throw e;
			} catch (Exception e) {
				// Typically an instantiation exception.
				throw new ParseException(e);
			}
		}
		throw new ParseException("Invalid call to getQueryParameters(String, ClassMeta).  Class type must be a Collection or array.");
	}

	private <T> T parseValue(HttpPartParser parser, String val, ClassMeta<T> c) throws ParseException {
		if (parser == null)
			parser = this.parser;
		return parser.parse(HttpPartType.QUERY, val, c);
	}

	/**
	 * Converts the query parameters to a readable string.
	 * 
	 * @param sorted Sort the query parameters by name.
	 * @return A JSON string containing the contents of the query parameters.
	 */
	public String toString(boolean sorted) {
		Map<String,Object> m = (sorted ? new TreeMap<String,Object>() : new LinkedHashMap<String,Object>());
		for (Map.Entry<String,String[]> e : this.entrySet()) {
			String[] v = e.getValue();
			m.put(e.getKey(), v.length == 1 ? v[0] : v);
		}
		return JsonSerializer.DEFAULT_LAX.toString(m);
	}

	/**
	 * Converts this object to a query string.
	 * 
	 * <p>
	 * Returned query string does not start with <js>'?'</js>.
	 * 
	 * @return A new query string, or an empty string if this object is empty.
	 */
	public String toQueryString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String,String[]> e : this.entrySet()) {
			for (int i = 0; i < e.getValue().length; i++) {
				if (sb.length() > 0)
					sb.append("&");
				sb.append(XmlUtils.urlEncode(e.getKey())).append('=').append(XmlUtils.urlEncode(e.getValue()[i]));
			}
		}
		return sb.toString();
	}

	private ClassMeta<?> getClassMeta(Type type, Type...args) {
		return beanSession.getClassMeta(type, args);
	}

	private <T> ClassMeta<T> getClassMeta(Class<T> type) {
		return beanSession.getClassMeta(type);
	}

	@Override /* Object */
	public String toString() {
		return toString(false);
	}
}
