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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static java.util.Collections.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;

/**
 * Represents the path parameters in an HTTP request.
 *
 *  <p>
 * 	The {@link RequestPathParams} object is the API for accessing the matched variables
 * 	and remainder on the URL path.
 * </p>
 * <p class='bcode w800'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestPathParams <jv>path</jv>) {...}
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestPost</ja>(..., path=<js>"/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RequestPathParams <jv>path</jv>) {
 * 		<jc>// Example URL:  /123/qux/true/quux</jc>
 *
 * 		<jk>int</jk> <jv>foo</jv> = <jv>path</jv>.get(<js>"foo"</js>).asInteger().orElse(0);  <jc>// =123</jc>
 * 		String <jv>bar</jv> = <jv>path</jv>.get(<js>"bar"</js>).orElse(<jk>null</jk>);  <jc>// =qux</jc>
 * 		<jk>boolean</jk> <jv>baz</jv> = <jv>path</jv>.get(<js>"baz"</js>).asBoolean().orElse(<jk>false</jk>);  <jc>// =true</jc>
 * 		String <jv>remainder</jv> = <jv>path</jv>.getRemainder();  <jc>// =quux</jc>
 * 	}
 * </p>
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestPathParams}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving path parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParams#contains(String...) contains(String...)}
 * 			<li class='jm'>{@link RequestPathParams#containsAny(String...) containsAny(String...)}
 * 			<li class='jm'>{@link RequestPathParams#get(Class) get(Class)}
 * 			<li class='jm'>{@link RequestPathParams#get(String) get(String)}
 * 			<li class='jm'>{@link RequestPathParams#getAll() getAll()}
 * 			<li class='jm'>{@link RequestPathParams#getAll(String) getAll(String)}
 * 			<li class='jm'>{@link RequestPathParams#getFirst(String) getFirst(String)}
 * 			<li class='jm'>{@link RequestPathParams#getLast(String) getLast(String)}
 * 			<li class='jm'>{@link RequestPathParams#getRemainder() getRemainder()}
 * 			<li class='jm'>{@link RequestPathParams#getRemainderUndecoded() getRemainderUndecoded()}
 * 		</ul>
 * 		<li>Methods overridding path parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParams#add(NameValuePair...) add(NameValuePair...)}
 * 			<li class='jm'>{@link RequestPathParams#add(String,Object) add(String,Object)}
 * 			<li class='jm'>{@link RequestPathParams#addDefault(List) addDefault(List)}
 * 			<li class='jm'>{@link RequestPathParams#addDefault(NameValuePair...) addDefault(NameValuePair...)}
 * 			<li class='jm'>{@link RequestPathParams#remove(NameValuePair...) remove(NameValuePair...)}
 * 			<li class='jm'>{@link RequestPathParams#remove(String...) remove(String...)}
 * 			<li class='jm'>{@link RequestPathParams#set(NameValuePair...) set(NameValuePair...)}
 * 			<li class='jm'>{@link RequestPathParams#set(String,Object) set(String,Object)}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParams#copy() copy()}
 * 			<li class='jm'>{@link RequestPathParams#isEmpty() isEmpty()}
 * 		</ul>
 * 	</ul>
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='ja'>{@link org.apache.juneau.http.annotation.Path}
 * </ul>
*/
public class RequestPathParams {

	private final RestSession session;
	private final RestRequest req;
	private final boolean caseSensitive;
	private HttpPartParserSession parser;
	private final VarResolverSession vs;

	private List<RequestPathParam> list = new LinkedList<>();
	private Map<String,List<RequestPathParam>> map = new TreeMap<>();

	RequestPathParams(RestSession session, RestRequest req, boolean caseSensitive) {
		this.session = session;
		this.req = req;
		this.caseSensitive = caseSensitive;
		this.vs = req.getVarResolverSession();

		// Add parameters from parent context if any.
		@SuppressWarnings("unchecked")
		Map<String,String> parentVars = (Map<String,String>)req.getAttribute("juneau.pathVars").orElse(Collections.emptyMap());
		for (Map.Entry<String,String> e : parentVars.entrySet())
			add(e.getKey(), e.getValue());

		UrlPathMatch pm = session.getUrlPathMatch();
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
		session = copyFrom.session;
		req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		list.addAll(copyFrom.list);
		map.putAll(copyFrom.map);
		vs = copyFrom.vs;
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
	 * @return This object.
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
				RequestPathParam x = new RequestPathParam(req, name, vs.resolve(p.getValue()));
				list.add(x);
				map.put(key, AList.of(x));
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
	public RequestPathParams addDefault(NameValuePair...pairs) {
		return addDefault(Arrays.asList(pairs));
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * Returns the path parameter as the specified bean type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link org.apache.juneau.http.annotation.Path} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * @param type The bean type to create.
	 * @return The bean, never <jk>null</jk>.
	 */
	public <T> Optional<T> get(Class<T> type) {
		ClassMeta<T> cm = req.getBeanSession().getClassMeta(type);
		String name = HttpParts.getName(PATH, cm).orElseThrow(()->runtimeException("@Path(name) not found on class {0}", className(type)));
		return get(name).as(type);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
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
