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
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.urlencoding.*;

/**
 * Represents the query parameters in an HTTP request.
 */
@SuppressWarnings("unchecked")
public final class RequestQuery extends LinkedHashMap<String,String[]> {
	private static final long serialVersionUID = 1L;

	private UrlEncodingParser parser;
	private BeanSession beanSession;

	RequestQuery setParser(UrlEncodingParser parser) {
		this.parser = parser;
		return this;
	}

	RequestQuery setBeanSession(BeanSession beanSession) {
		this.beanSession = beanSession;
		return this;
	}

	/**
	 * Adds default entries to these query parameters.
	 * <p>
	 * This includes the default queries defined on the servlet and method levels.
	 *
	 * @param defaultEntries The default entries.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestQuery addDefault(Map<String,String> defaultEntries) {
		if (defaultEntries != null) {
			for (Map.Entry<String,String> e : defaultEntries.entrySet()) {
				String key = e.getKey(), value = e.getValue();
				String[] v = get(key);
				if (v == null || v.length == 0 || StringUtils.isEmpty(v[0]))
					put(key, new String[]{value});
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
	 * <p>
	 * Same as {@link HttpServletRequest#getParameter(String)} except only looks in the URL string, not parameters from URL-Encoded FORM posts.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying servlet API to load and parse the request body.
	 *
	 * @param name The URL parameter name.
	 * @return The parameter value, or <jk>null</jk> if parameter not specified or has no value (e.g. <js>"&amp;foo"</js>.
	 */
	public String getFirst(String name) {
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
	 * Same as {@link #getFirst(String)} but returns the specified default value if the query parameter was not specified.
	 *
	 * @param name The URL parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if parameter not specified or has no value (e.g. <js>"&amp;foo"</js>.
	 */
	public String getFirst(String name, String def) {
		String s = getFirst(name);
		return StringUtils.isEmpty(s) ? def : s;
	}

	/**
	 * Returns the specified query parameter value converted to a POJO.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying servlet API to load and parse the request body.
	 * <p>
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
		return get(name, beanSession.getClassMeta(type));
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
		return get(name, def, beanSession.getClassMeta(type));
	}

	/**
	 * Returns the specified query parameter value converted to a POJO.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying servlet API to load and parse the request body.
	 * <p>
	 * Use this method if you want to parse into a parameterized <code>Map</code>/<code>Collection</code> object.
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	Listt&lt;String&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	Listt&lt;List&lt;String&gt;&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(String name, Type type, Type...args) throws ParseException {
		return (T)parse(name, beanSession.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #get(String, Class)} except returns a default value if not found.
	 *
	 * @param name The parameter name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(String name, Object def, Type type, Type...args) throws ParseException {
		return (T)parse(name, def, beanSession.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #get(String, Class)} except for use on multi-part parameters
	 * (e.g. <js>"&amp;key=1&amp;key=2&amp;key=3"</js> instead of <js>"&amp;key=(1,2,3)"</js>).
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
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The query parameter name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The query parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAll(String name, Type type, Type...args) throws ParseException {
		return (T)parseAll(name, beanSession.getClassMeta(type, args));
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

	/* Workhorse method */
	private <T> T parse(String name, T def, ClassMeta<T> cm) throws ParseException {
		String val = getFirst(name);
		if (val == null)
			return def;
		return parseValue(val, cm);
	}

	/* Workhorse method */
	private <T> T parse(String name, ClassMeta<T> cm) throws ParseException {
		String val = getFirst(name);
		if (cm.isPrimitive() && (val == null || val.isEmpty()))
			return cm.getPrimitiveDefault();
		return parseValue(val, cm);
	}

	/* Workhorse method */
	@SuppressWarnings("rawtypes")
	private <T> T parseAll(String name, ClassMeta<T> cm) throws ParseException {
		String[] p = get(name);
		if (p == null)
			return null;
		if (cm.isArray()) {
			List c = new ArrayList();
			for (int i = 0; i < p.length; i++)
				c.add(parseValue(p[i], cm.getElementType()));
			return (T)toArray(c, cm.getElementType().getInnerClass());
		} else if (cm.isCollection()) {
			try {
				Collection c = (Collection)(cm.canCreateNewInstance() ? cm.newInstance() : new ObjectList());
				for (int i = 0; i < p.length; i++)
					c.add(parseValue(p[i], cm.getElementType()));
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

	private <T> T parseValue(String val, ClassMeta<T> c) throws ParseException {
		return parser.parse(PartType.QUERY, val, c);
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

	@Override /* Object */
	public String toString() {
		return toString(false);
	}
}
