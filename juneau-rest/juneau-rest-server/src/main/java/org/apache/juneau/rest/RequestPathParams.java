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

import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Collections.*;
import static org.apache.juneau.assertions.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;

/**
 * Represents the path parameters in an HTTP request.
 */
@BeanIgnore
public class RequestPathParams {

	private final RestCall call;
	private final RestRequest req;
	private final boolean caseSensitive;
	private HttpPartParserSession parser;

	private List<RequestPathParam> list = new LinkedList<>();
	private Map<String,List<RequestPathParam>> map = new TreeMap<>();

	RequestPathParams(RestCall call, RestRequest req, boolean caseSensitive) {
		this.call = call;
		this.req = req;
		this.caseSensitive = caseSensitive;

		// Add parameters from parent context if any.
		@SuppressWarnings("unchecked")
		Map<String,String> parentVars = (Map<String,String>)req.getAttribute("juneau.pathVars").orElse(null);
		if (parentVars != null)
			for (Map.Entry<String,String> e : parentVars.entrySet())
				add(e.getKey(), e.getValue());

		UrlPathMatch pm = call.getUrlPathMatch();
		if (pm != null) {
			for (Map.Entry<String,String> e : pm.getVars().entrySet())
				add(e.getKey(), e.getValue());
			String r = pm.getRemainder();
			if (r != null) {
				add("/**", r);
				add("/*", urlDecode(r));
			}
		}
	}

	/**
	 * Copy constructor.
	 */
	private RequestPathParams(RequestPathParams copyFrom) {
		call = copyFrom.call;
		req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		list.addAll(copyFrom.list);
		map.putAll(copyFrom.map);
	}

	RequestPathParams parser(HttpPartParserSession parser) {
		this.parser = parser;
		for (RequestPathParam p : list)
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
	 * @return This object (for method chaining).
	 */
	public RequestPathParams addDefault(List<NameValuePair> pairs) {
		for (NameValuePair p : pairs) {
			String name = p.getName();
			String key = key(name);
			List<RequestPathParam> l = map.get(key);
			boolean hasAllBlanks = l != null && l.stream().allMatch(x -> StringUtils.isEmpty(x.getValue()));
			if (l == null || hasAllBlanks) {
				if (hasAllBlanks)
					list.removeAll(l);
				RequestPathParam x = new RequestPathParam(req, name, p.getValue());
				list.add(x);
				map.put(key, AList.of(x));
			}
		}
		return this;
	}

	/**
	 * Returns all the parameters with the specified name.
	 *
	 * @param name The parameter name.
	 * @return The list of all parameters with the specified name, or an empty list if none are found.
	 */
	public List<RequestPathParam> getAll(String name) {
		assertArgNotNull("name", name);
		List<RequestPathParam> l = map.get(key(name));
		return unmodifiableList(l == null ? emptyList() : l);
	}

	/**
	 * Returns all the parameters on this request.
	 *
	 * @return All the parameters on this request.
	 */
	public List<RequestPathParam> getAll() {
		return unmodifiableList(list);
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
	 * @return This object (for method chaining).
	 */
	public RequestPathParams add(String name, Object value) {
		assertArgNotNull("name", name);
		String key = key(name);
		RequestPathParam h = new RequestPathParam(req, name, stringify(value)).parser(parser);
		if (map.containsKey(key))
			map.get(key).add(h);
		else
			map.put(key, AList.of(h));
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
	 * @return This object (for method chaining).
	 */
	public RequestPathParams add(NameValuePair...parameters) {
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
	 * @return This object (for method chaining).
	 */
	public RequestPathParams set(String name, Object value) {
		assertArgNotNull("name", name);
		String key = key(name);
		RequestPathParam p = new RequestPathParam(req, name, stringify(value)).parser(parser);
		if (map.containsKey(key))
			list.removeIf(x->caseSensitive?x.getName().equals(name):x.getName().equalsIgnoreCase(name));
		list.add(p);
		map.put(key, AList.of(p));
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
	 * @return This object (for method chaining).
	 */
	public RequestPathParams set(NameValuePair...parameters) {
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
	 * @return This object (for method chaining).
	 */
	public RequestPathParams remove(String...name) {
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
	 * @return This object (for method chaining).
	 */
	public RequestPathParams remove(NameValuePair...parameters) {
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
	 * Note that this method never returns <jk>null</jk> and that {@link RequestPathParam#isPresent()} can be used
	 * to test for the existence of the parameter.
	 *
	 * @param name The parameter name.
	 * @return The parameter.  Never <jk>null</jk>.
	 */
	public RequestPathParam getFirst(String name) {
		assertArgNotNull("name", name);
		List<RequestPathParam> l = map.get(key(name));
		return (l == null || l.isEmpty() ? new RequestPathParam(req, name, null).parser(parser) : l.get(0));
	}

	/**
	 * Returns the last parameter with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestPathParam#isPresent()} can be used
	 * to test for the existence of the parameter.
	 *
	 * @param name The parameter name.
	 * @return The parameter.  Never <jk>null</jk>.
	 */
	public RequestPathParam getLast(String name) {
		assertArgNotNull("name", name);
		List<RequestPathParam> l = map.get(key(name));
		return (l == null || l.isEmpty() ? new RequestPathParam(req, name, null).parser(parser) : l.get(l.size()-1));
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
	public RequestPathParam get(String name) {
		return getLast(name);
	}

	/**
	 * Returns the last parameter with the specified name as a string.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<String> getString(String name) {
		return getLast(name).asString();
	}

	/**
	 * Returns the last parameter with the specified name as an integer.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<Integer> getInteger(String name) {
		return getLast(name).asInteger();
	}

	/**
	 * Returns the last parameter with the specified name as a boolean.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<Boolean> getBoolean(String name) {
		return getLast(name).asBoolean();
	}

	/**
	 * Returns the last parameter with the specified name as a list from a comma-delimited string.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<List<String>> getCsvArray(String name) {
		return getLast(name).asCsvArray();
	}

	/**
	 * Returns the last parameter with the specified name as a long.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<Long> getLong(String name) {
		return getLast(name).asLong();
	}

	/**
	 * Returns the last parameter with the specified name as a boolean.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<ZonedDateTime> getDate(String name) {
		return getLast(name).asDate();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Makes a copy of these parameters.
	 *
	 * @return A new parameters object.
	 */
	public RequestPathParams copy() {
		return new RequestPathParams(this);
	}

	/**
	 * Returns the decoded remainder of the URL following any path pattern matches.
	 *
	 * <p>
	 * The behavior of path remainder is shown below given the path pattern "/foo/*":
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th>URL</th>
	 * 		<th>Path Remainder</th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo</c></td>
	 * 		<td><jk>null</jk></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo/</c></td>
	 * 		<td><js>""</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo//</c></td>
	 * 		<td><js>"/"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo///</c></td>
	 * 		<td><js>"//"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo/a/b</c></td>
	 * 		<td><js>"a/b"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo//a/b/</c></td>
	 * 		<td><js>"/a/b/"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo/a%2Fb</c></td>
	 * 		<td><js>"a/b"</js></td>
	 * 	</tr>
	 * </table>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// REST method</jc>
	 * 	<ja>@RestGet</ja>(<js>"/foo/{bar}/*"</js>)
	 * 	<jk>public</jk> String doGetById(RequestPathParams <jv>path</jv>, <jk>int</jk> <jv>bar</jv>) {
	 * 		<jk>return</jk> <jv>path</jv>.remainder().orElse(<jk>null</jk>);
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The remainder can also be retrieved by calling <code>get(<js>"/**"</js>)</code>.
	 *
	 * @return The path remainder string.
	 */
	public RequestPathParam getRemainder() {
		return get("/*");

	}

	/**
	 * Same as {@link #getRemainder()} but doesn't decode characters.
	 *
	 * <p>
	 * The undecoded remainder can also be retrieved by calling <code>get(<js>"/*"</js>)</code>.
	 *
	 * @return The un-decoded path remainder.
	 */
	public RequestPathParam getRemainderUndecoded() {
		return get("/**");
	}

	/**
	 * Converts the parameters to a readable string.
	 *
	 * @param sorted Sort the parameters by name.
	 * @return A JSON string containing the contents of the parameters.
	 */
	public String toString(boolean sorted) {
		OMap m = OMap.create();
		if (sorted) {
			for (List<RequestPathParam> p1 : map.values())
				for (RequestPathParam p2 : p1)
					m.append(p2.getName(), p2.getValue());
		} else {
			for (RequestPathParam p : list)
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
