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
package org.apache.juneau.rest.httppart;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.util.*;

import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Represents the parsed form-data parameters in an HTTP request.
 *
 * <p>
 * 	The {@link RequestFormParams} object is the API for accessing the HTTP request body as form data.
 * 	It can be accessed by passing it as a parameter on your REST Java method:
 * </p>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestFormParams <jv>formData</jv>) {...}
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestFormParams <jv>formData</jv>) {
 *
 * 		<jc>// Get query parameters converted to various types.</jc>
 * 		<jk>int</jk> <jv>p1</jv> = <jv>formData</jv>.get(<js>"p1"</js>).asInteger().orElse(0);
 * 		String <jv>p2</jv> = <jv>formData</jv>.get(<js>"p2"</js>).orElse(<jk>null</jk>);
 * 		UUID <jv>p3</jv> = <jv>formData</jv>.get(<js>"p3"</js>).as(UUID.<jk>class</jk>).orElse(<jk>null</jk>);
 * 	 }
 * </p>
 *
 * <p>
 * 	Note that this object does NOT take GET parameters into account and only returns values found in the body of the request.
 * </p>
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestFormParams}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving form data parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestFormParams#contains(String...) contains(String...)}
 * 			<li class='jm'>{@link RequestFormParams#containsAny(String...) containsAny(String...)}
 * 			<li class='jm'>{@link RequestFormParams#get(Class) get(Class)}
 * 			<li class='jm'>{@link RequestFormParams#get(String) get(String)}
 * 			<li class='jm'>{@link RequestFormParams#getAll() getAll()}
 * 			<li class='jm'>{@link RequestFormParams#getAll(String) getAll(String)}
 * 			<li class='jm'>{@link RequestFormParams#getFirst(String) getFirst(String)}
 * 			<li class='jm'>{@link RequestFormParams#getLast(String) getLast(String)}
 * 			<li class='jm'>{@link RequestFormParams#getSearchArgs() getSearchArgs()}
 * 		</ul>
 * 		<li>Methods overridding form data parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestFormParams#add(NameValuePair...) add(NameValuePair...)}
 * 			<li class='jm'>{@link RequestFormParams#add(Part) add(Part)}
 * 			<li class='jm'>{@link RequestFormParams#add(String,Object) add(String,Object)}
 * 			<li class='jm'>{@link RequestFormParams#addDefault(List) addDefault(List)}
 * 			<li class='jm'>{@link RequestFormParams#addDefault(NameValuePair...) addDefault(NameValuePair...)}
 * 			<li class='jm'>{@link RequestFormParams#addDefault(String,String) addDefault(String,String)}
 * 			<li class='jm'>{@link RequestFormParams#remove(NameValuePair...) remove(NameValuePair...)}
 * 			<li class='jm'>{@link RequestFormParams#remove(String...) remove(String...)}
 * 			<li class='jm'>{@link RequestFormParams#set(NameValuePair...) set(NameValuePair...)}
 * 			<li class='jm'>{@link RequestFormParams#set(String,Object) set(String,Object)}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestFormParams#asQueryString() asQueryString()}
 * 			<li class='jm'>{@link RequestFormParams#copy() copy()}
 * 			<li class='jm'>{@link RequestFormParams#isEmpty() isEmpty()}
 * 		</ul>
 * 	</ul>
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='jc'>{@link RequestFormParam}
 * 	<li class='ja'>{@link org.apache.juneau.http.annotation.FormData}
 * 	<li class='ja'>{@link org.apache.juneau.http.annotation.HasFormData}
 * 	<li class='link'>{@doc jrs.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class RequestFormParams {

	private final RestRequest req;
	private final boolean caseSensitive;
	private HttpPartParserSession parser;
	private final VarResolverSession vs ;

	private List<RequestFormParam> list = new LinkedList<>();
	private Map<String,List<RequestFormParam>> map = new TreeMap<>();

	/**
	 * Constructor.
	 *
	 * @param req The request creating this bean.
	 * @param caseSensitive Whether case-sensitive name matching is enabled.
	 * @throws Exception Any exception can be thrown.
	 */
	public RequestFormParams(RestRequest req, boolean caseSensitive) throws Exception {
		this.req = req;
		this.caseSensitive = caseSensitive;
		this.vs = req.getVarResolverSession();

		Map<String,String[]> m = null;
		Collection<Part> c = null;

		RequestBody body = req.getBody();
		if (body.isLoaded() || ! req.getHeader(ContentType.class).orElse(ContentType.NULL).equalsIgnoreCase("multipart/form-data"))
			m = RestUtils.parseQuery(body.getReader());
		else {
			c = req.getHttpServletRequest().getParts();
			if (c == null || c.isEmpty())
				m = req.getHttpServletRequest().getParameterMap();
		}

		if (m != null) {
			for (Map.Entry<String,String[]> e : m.entrySet()) {
				String name = e.getKey();
				String key = key(name);
				List<RequestFormParam> l = list();

				String[] values = e.getValue();
				if (values == null)
					values = new String[0];

				// Fix for behavior difference between Tomcat and WAS.
				// getParameter("foo") on "&foo" in Tomcat returns "".
				// getParameter("foo") on "&foo" in WAS returns null.
				if (values.length == 1 && values[0] == null)
					values[0] = "";

				for (String value : values) {
					RequestFormParam p = new RequestFormParam(req, name, value);
					list.add(p);
					l.add(p);
				}
				map.put(key, l);
			}
		} else if (c != null) {
			c.stream().forEach(x->add(x));
		}
	}


	/**
	 * Copy constructor.
	 */
	private RequestFormParams(RequestFormParams copyFrom) {
		req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		list.addAll(copyFrom.list);
		map.putAll(copyFrom.map);
		vs = copyFrom.vs;
	}

	/**
	 * Sets the parser to use for part values.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public RequestFormParams parser(HttpPartParserSession value) {
		this.parser = value;
		for (RequestFormParam p : list)
			p.parser(parser);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic operations.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds default entries to these parameters.
	 *
	 * <p>
	 * Similar to {@link #set(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestFormParams addDefault(List<? extends NameValuePair> pairs) {
		for (NameValuePair p : pairs) {
			String name = p.getName();
			String key = key(name);
			List<RequestFormParam> l = map.get(key);
			boolean hasAllBlanks = l != null && l.stream().allMatch(x -> StringUtils.isEmpty(x.getValue()));
			if (l == null || hasAllBlanks) {
				if (hasAllBlanks)
					list.removeAll(l);
				RequestFormParam x = new RequestFormParam(req, name, vs.resolve(p.getValue()));
				list.add(x);
				map.put(key, list(x));
			}
		}
		return this;
	}

	/**
	 * Adds default entries to these parameters.
	 *
	 * <p>
	 * Similar to {@link #set(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestFormParams addDefault(NameValuePair...pairs) {
		return addDefault(Arrays.asList(pairs));
	}

	/**
	 * Adds a default entry to the form data parameters.
	 *
	 * @param name The name.
	 * @param value The value.
	 * @return This object.
	 */
	public RequestFormParams addDefault(String name, String value) {
		return addDefault(BasicStringPart.of(name, value));
	}

	/**
	 * Returns all the parameters with the specified name.
	 *
	 * @param name The parameter name.
	 * @return The list of all parameters with the specified name, or an empty list if none are found.
	 */
	public List<RequestFormParam> getAll(String name) {
		assertArgNotNull("name", name);
		List<RequestFormParam> l = map.get(key(name));
		return l == null ? emptyList() : unmodifiable(l);
	}

	/**
	 * Returns all the parameters on this request.
	 *
	 * @return All the parameters on this request.
	 */
	public List<RequestFormParam> getAll() {
		return unmodifiable(list);
	}

	/**
	 * Returns <jk>true</jk> if the parameters with the specified names are present.
	 *
	 * @param names The parameter names.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the parameters with the specified names are present.
	 */
	public boolean contains(String...names) {
		assertArgNotNull("names", names);
		for (String n : names)
			if (! map.containsKey(key(n)))
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if the parameter with any of the specified names are present.
	 *
	 * @param names The parameter names.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the parameter with any of the specified names are present.
	 */
	public boolean containsAny(String...names) {
		assertArgNotNull("names", names);
		for (String n : names)
			if (map.containsKey(key(n)))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if these parameters are empty.
	 *
	 * @return <jk>true</jk> if these parameters are empty.
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * Adds a parameter value.
	 *
	 * <p>
	 * Parameter is added to the end.
	 * <br>Existing parameter with the same name are not changed.
	 *
	 * @param name The parameter name.  Must not be <jk>null</jk>.
	 * @param value The parameter value.
	 * @return This object.
	 */
	public RequestFormParams add(String name, Object value) {
		assertArgNotNull("name", name);
		String key = key(name);
		RequestFormParam h = new RequestFormParam(req, name, stringify(value)).parser(parser);
		if (map.containsKey(key))
			map.get(key).add(h);
		else
			map.put(key, list(h));
		list.add(h);
		return this;
	}

	/**
	 * Adds a parameter value.
	 *
	 * <p>
	 * Parameter is added to the end.
	 * <br>Existing parameter with the same name are not changed.
	 *
	 * @param part The parameter part.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestFormParams add(Part part) {
		assertArgNotNull("part", part);
		String key = key(part.getName());
		RequestFormParam h = new RequestFormParam(req, part).parser(parser);
		if (map.containsKey(key))
			map.get(key).add(h);
		else
			map.put(key, list(h));
		list.add(h);
		return this;
	}

	/**
	 * Adds request parameter values.
	 *
	 * <p>
	 * Parameters are added to the end.
	 * <br>Existing parameters with the same name are not changed.
	 *
	 * @param parameters The parameter objects.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestFormParams add(NameValuePair...parameters) {
		assertArgNotNull("parameters", parameters);
		for (NameValuePair p : parameters) {
			if (p != null)
				add(p.getName(), p.getValue());
		}
		return this;
	}

	/**
	 * Sets a parameter value.
	 *
	 * <p>
	 * Parameter is added to the end.
	 * <br>Any previous parameters with the same name are removed.
	 *
	 * @param name The parameter name.  Must not be <jk>null</jk>.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Converted to a string using {@link Object#toString()}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestFormParams set(String name, Object value) {
		assertArgNotNull("name", name);
		String key = key(name);
		RequestFormParam p = new RequestFormParam(req, name, stringify(value)).parser(parser);
		if (map.containsKey(key))
			list.removeIf(x->caseSensitive?x.getName().equals(name):x.getName().equalsIgnoreCase(name));
		list.add(p);
		map.put(key, list(p));
		return this;
	}


	/**
	 * Sets request header values.
	 *
	 * <p>
	 * Parameters are added to the end of the headers.
	 * <br>Any previous parameters with the same name are removed.
	 *
	 * @param parameters The parameters to set.  Must not be <jk>null</jk> or contain <jk>null</jk>.
	 * @return This object.
	 */
	public RequestFormParams set(NameValuePair...parameters) {
		assertArgNotNull("headers", parameters);
		for (NameValuePair p : parameters)
			remove(p);
		for (NameValuePair p : parameters)
			add(p);
		return this;
	}

	/**
	 * Remove parameters.
	 *
	 * @param name The parameter names.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestFormParams remove(String...name) {
		assertArgNotNull("name", name);
		for (String n : name) {
			String key = key(n);
			if (map.containsKey(key))
				list.removeAll(map.get(key));
			map.remove(key);
		}
		return this;
	}

	/**
	 * Remove parameters.
	 *
	 * @param parameters The parameters to remove.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestFormParams remove(NameValuePair...parameters) {
		for (NameValuePair p : parameters)
			remove(p.getName());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience getters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the first parameter with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestFormParam#isPresent()} can be used
	 * to test for the existence of the parameter.
	 *
	 * @param name The parameter name.
	 * @return The parameter.  Never <jk>null</jk>.
	 */
	public RequestFormParam getFirst(String name) {
		assertArgNotNull("name", name);
		List<RequestFormParam> l = map.get(key(name));
		return (l == null || l.isEmpty() ? new RequestFormParam(req, name, null).parser(parser) : l.get(0));
	}

	/**
	 * Returns the last parameter with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestFormParam#isPresent()} can be used
	 * to test for the existence of the parameter.
	 *
	 * @param name The parameter name.
	 * @return The parameter.  Never <jk>null</jk>.
	 */
	public RequestFormParam getLast(String name) {
		assertArgNotNull("name", name);
		List<RequestFormParam> l = map.get(key(name));
		return (l == null || l.isEmpty() ? new RequestFormParam(req, name, null).parser(parser) : l.get(l.size()-1));
	}

	/**
	 * Returns the last parameter with the specified name.
	 *
	 * <p>
	 * This is equivalent to {@link #getLast(String)}.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public RequestFormParam get(String name) {
		return getLast(name);
	}

	/**
	 * Returns the form data parameter as the specified bean type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link org.apache.juneau.http.annotation.FormData} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * @param <T> The bean type to create.
	 * @param type The bean type to create.
	 * @return The bean, never <jk>null</jk>.
	 */
	public <T> Optional<T> get(Class<T> type) {
		ClassMeta<T> cm = req.getBeanSession().getClassMeta(type);
		String name = HttpParts.getName(FORMDATA, cm).orElseThrow(()->runtimeException("@FormData(name) not found on class {0}", className(type)));
		return get(name).as(type);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

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
		for (RequestFormParam e : list) {
			if (sb.length() > 0)
				sb.append("&");
			sb.append(urlEncode(e.getName())).append('=').append(urlEncode(e.getValue()));
		}
		return sb.toString();
	}

	/**
	 * Makes a copy of these parameters.
	 *
	 * @return A new parameters object.
	 */
	public RequestFormParams copy() {
		return new RequestFormParams(this);
	}

	/**
	 * Locates the special search query arguments in the query and returns them as a {@link SearchArgs} object.
	 *
	 * <p>
	 * The query arguments are as follows:
	 * <ul class='spaced-list'>
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
	 * 		<br><c>0</c> implies return all rows.
	 * 		<br>Example: <js>"&amp;l=100"</js>
	 * 	<li>
	 * 		<js>"&amp;i="</js> - The case-insensitive search flag.
	 * 		<br>Example: <js>"&amp;i=true"</js>
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Whitespace is trimmed in the parameters.
	 * </ul>
	 *
	 * @return
	 * 	A new {@link SearchArgs} object initialized with the special search query arguments.
	 * 	<br>Returns <jk>null</jk> if no search arguments were found.
	 */
	public SearchArgs getSearchArgs() {
		if (contains("s","v","o","p","l","i")) {
			return new SearchArgs.Builder()
				.search(get("s").asString().orElse(null))
				.view(get("v").asString().orElse(null))
				.sort(get("o").asString().orElse(null))
				.position(get("p").asInteger().orElse(null))
				.limit(get("l").asInteger().orElse(null))
				.ignoreCase(get("i").asBoolean().orElse(null))
				.build();
		}
		return null;
	}

	/**
	 * Converts the parameters to a readable string.
	 *
	 * @param sorted Sort the parameters by name.
	 * @return A JSON string containing the contents of the parameters.
	 */
	public String toString(boolean sorted) {
		JsonMap m = JsonMap.create();
		if (sorted) {
			for (List<RequestFormParam> p1 : map.values())
				for (RequestFormParam p2 : p1)
					m.append(p2.getName(), p2.getValue());
		} else {
			for (RequestFormParam p : list)
				m.append(p.getName(), p.getValue());
		}
		return m.toString();
	}

	private String key(String name) {
		return caseSensitive ? name : name.toLowerCase();
	}

	@Override /* Object */
	public String toString() {
		return toString(false);
	}
}
