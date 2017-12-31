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

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;

/**
 * Represents the parsed form data parameters in an HTTP request.
 */
@SuppressWarnings("unchecked")
public class RequestFormData extends LinkedHashMap<String,String[]> {
	private static final long serialVersionUID = 1L;

	private HttpPartParser parser;
	private BeanSession beanSession;

	RequestFormData setParser(HttpPartParser parser) {
		this.parser = parser;
		return this;
	}

	RequestFormData setBeanSession(BeanSession beanSession) {
		this.beanSession = beanSession;
		return this;
	}

	/**
	 * Adds default entries to these form-data parameters.
	 *
	 * <p>
	 * This includes the default form-data parameters defined on the servlet and method levels.
	 *
	 * @param defaultEntries The default entries.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestFormData addDefault(Map<String,String> defaultEntries) {
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
	 * Sets a request form data parameter value.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public void put(String name, Object value) {
		super.put(name, new String[]{StringUtils.toString(value)});
	}

	/**
	 * Returns a form data parameter value.
	 *
	 * <p>
	 * Parameter lookup is case-insensitive (consistent with WAS, but differs from Tomcat).
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by the
	 * 		underlying servlet API.
	 * 	<li>This method returns the raw unparsed value, and differs from calling
	 * 		<code>getFormDataParameter(name, String.<jk>class</js>)</code> which will convert the value from UON
	 * 		notation:
	 * 		<ul>
	 * 			<li><js>"null"</js> =&gt; <jk>null</jk>
	 * 			<li><js>"'null'"</js> =&gt; <js>"null"</js>
	 * 			<li><js>"'foo bar'"</js> =&gt; <js>"foo bar"</js>
	 * 			<li><js>"foo~~bar"</js> =&gt; <js>"foo~bar"</js>
	 * 		</ul>
	 * </ul>
	 *
	 * @param name The form data parameter name.
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
	 * Same as {@link #getString(String)} except returns a default value if <jk>null</jk> or empty.
	 *
	 * @param name The form data parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public String getString(String name, String def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : s;
	}

	/**
	 * Same as {@link #getString(String)} but converts the value to an integer.
	 *
	 * @param name The form data parameter name.
	 * @return The parameter value, or <code>0</code> if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public int getInt(String name) {
		return getInt(name, 0);
	}

	/**
	 * Same as {@link #getString(String,String)} but converts the value to an integer.
	 *
	 * @param name The form data parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public int getInt(String name, int def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Integer.parseInt(s);
	}

	/**
	 * Same as {@link #getString(String)} but converts the value to a boolean.
	 *
	 * @param name The form data parameter name.
	 * @return The parameter value, or <jk>false</jk> if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public boolean getBoolean(String name) {
		return getBoolean(name, false);
	}

	/**
	 * Same as {@link #getString(String,String)} but converts the value to a boolean.
	 *
	 * @param name The form data parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if parameter does not exist or is <jk>null</jk> or empty.
	 */
	public boolean getBoolean(String name, boolean def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Boolean.parseBoolean(s);
	}

	/**
	 * Returns the specified form data parameter value converted to a POJO using the {@link HttpPartParser}
	 * registered with this servlet.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myparam = req.getFormDataParameter(<js>"myparam"</js>, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into an int array.</jc>
	 * 	<jk>int</jk>[] myparam = req.getFormDataParameter(<js>"myparam"</js>, <jk>int</jk>[].<jk>class</jk>);

	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean myparam = req.getFormDataParameter(<js>"myparam"</js>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List myparam = req.getFormDataParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map myparam = req.getFormDataParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by the
	 * 		underlying servlet API.
	 * </ul>
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
	 * Same as {@link #get(String, Object, Class)} but allows you to override the part parser.
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
		return parse(parser, name, getClassMeta(type));
	}

	/**
	 * Same as {@link #get(String, Class)} except returns a default value if not specified.
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
		return parse(parser, name, def, getClassMeta(type));
	}

	/**
	 * Same as {@link #get(String, Class)} except for use on multi-part parameters
	 * (e.g. <js>"key=1&amp;key=2&amp;key=3"</js> instead of <js>"key=(1,2,3)"</js>)
	 *
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAll(String name, Class<T> type) throws ParseException {
		return getAll(null, name, type);
	}

	/**
	 * Same as {@link #getAll(String, Class)} but allows you to override the part parser.
	 *
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param parser
	 * 	The parser to use for parsing the string value.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the servlet/method. 
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAll(HttpPartParser parser, String name, Class<T> type) throws ParseException {
		return parseAll(parser, name, getClassMeta(type));
	}

	/**
	 * Returns the specified form data parameter value converted to a POJO using the {@link HttpPartParser}
	 * registered with this servlet.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by the
	 * 		underlying servlet API.
	 * 	<li>Use this method if you want to parse into a parameterized <code>Map</code>/<code>Collection</code> object.
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; myparam = req.getFormDataParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; myparam = req.getFormDataParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; myparam = req.getFormDataParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; myparam = req.getFormDataParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
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
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(String name, Type type, Type...args) throws ParseException {
		return get(null, name, type, args);
	}

	/**
	 * Same as {@link #get(String, Type, Type...)} but allows you to override the part parser.
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
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T get(HttpPartParser parser, String name, Type type, Type...args) throws ParseException {
		return (T)parse(parser, name, getClassMeta(type, args));
	}

	/**
	 * Same as {@link #get(String, Type, Type...)} except for use on multi-part parameters
	 * (e.g. <js>"key=1&amp;key=2&amp;key=3"</js> instead of <js>"key=(1,2,3)"</js>)
	 *
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
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
	 * @return The parameter value converted to the specified class type.
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
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAll(HttpPartParser parser, String name, Type type, Type...args) throws ParseException {
		return (T)parseAll(parser, name, getClassMeta(type, args));
	}
	

	/* Workhorse method */
	<T> T parse(HttpPartParser parser, String name, T def, ClassMeta<T> cm) throws ParseException {
		String val = getString(name);
		if (val == null)
			return def;
		return parseValue(parser, val, cm);
	}

	/* Workhorse method */
	<T> T parse(HttpPartParser parser, String name, ClassMeta<T> cm) throws ParseException {
		String val = getString(name);
		if (cm.isPrimitive() && (val == null || val.isEmpty()))
			return cm.getPrimitiveDefault();
		return parseValue(parser, val, cm);
	}

	/* Workhorse method */
	@SuppressWarnings("rawtypes")
	<T> T parseAll(HttpPartParser parser, String name, ClassMeta<T> cm) throws ParseException {
		String[] p = get(name);
		if (p == null)
			return null;
		if (parser == null)
			parser = this.parser;
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
		throw new ParseException("Invalid call to getParameters(String, ClassMeta).  Class type must be a Collection or array.");
	}

	private <T> T parseValue(HttpPartParser parser, String val, ClassMeta<T> c) throws ParseException {
		try {
			if (parser == null)
				parser = this.parser;
			return parser.parse(HttpPartType.FORM_DATA, val, c);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Converts the form data parameters to a readable string.
	 *
	 * @param sorted Sort the form data parameters by name.
	 * @return A JSON string containing the contents of the form data parameters.
	 */
	public String toString(boolean sorted) {
		Map<String,Object> m = (sorted ? new TreeMap<String,Object>() : new LinkedHashMap<String,Object>());
		for (Map.Entry<String,String[]> e : this.entrySet()) {
			String[] v = e.getValue();
			m.put(e.getKey(), v.length == 1 ? v[0] : v);
		}
		return JsonSerializer.DEFAULT_LAX.toString(m);
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
