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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import javax.servlet.http.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Represents the attributes in an HTTP request.
 *
 * <p>
 * 	The {@link RequestAttributes} object is the API for accessing the standard servlet attributes on an HTTP request
 * 	(i.e. {@link javax.servlet.ServletRequest#getAttribute(String)}).
 * </p>
 *
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestAttributes <jv>attributes</jv>) {...}
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestAttributes <jv>attributes</jv>) {
 *
 * 		<jc>// Add a default value.</jc>
 * 		<jv>attributes</jv>.addDefault(<js>"Foo"</js>, 123);
 *
 * 		<jc>// Get an attribute value as a POJO.</jc>
 * 		UUID <jv>etag</jv> = <jv>attributes</jv>.get(<js>"ETag"</js>).as(UUID.<jk>class</jk>).orElse(<jk>null</jk>);
 * 	}
 * </p>
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestHeaders}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving request attributes:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestAttributes#contains(String...) contains(String...)}
 * 			<li class='jm'>{@link RequestAttributes#containsAny(String...) containsAny(String...)}
 * 			<li class='jm'>{@link RequestAttributes#get(String) get(String)}
 * 			<li class='jm'>{@link RequestAttributes#getAll() getAll()}
 * 		</ul>
 * 		<li>Methods for overriding request attributes:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestAttributes#addDefault(List) addDefault(List)}
 * 			<li class='jm'>{@link RequestAttributes#addDefault(NamedAttribute...) addDefault(NamedAttribute...)}
 * 			<li class='jm'>{@link RequestAttributes#addDefault(NamedAttributeMap) addDefault(NamedAttributeMap)}
 * 			<li class='jm'>{@link RequestAttributes#addDefault(String,Object) addDefault(String,Object)}
 * 			<li class='jm'>{@link RequestAttributes#remove(NamedAttribute...) remove(NamedAttribute...)}
 * 			<li class='jm'>{@link RequestAttributes#remove(String...) remove(String...)}
 * 			<li class='jm'>{@link RequestAttributes#set(NamedAttribute...) set(NamedAttribute...)}
 * 			<li class='jm'>{@link RequestAttributes#set(String,Object) set(String,Object)}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestAttributes#asMap() asMap()}
 * 		</ul>
 * 	</ul>
 * </ul>
 *
 * <p>
 * 	Modifications made to request attributes through the <c>RequestAttributes</c> bean are automatically reflected in
 * 	the underlying servlet request attributes making it possible to mix the usage of both APIs.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
public class RequestAttributes {

	final RestRequest req;
	final HttpServletRequest sreq;
	final VarResolverSession vs;

	/**
	 * Constructor.
	 *
	 * @param req The request creating this bean.
	 */
	public RequestAttributes(RestRequest req) {
		super();
		this.req = req;
		this.sreq = req.getHttpServletRequest();
		this.vs = req.getVarResolverSession();
	}

	/**
	 * Adds default entries to the request attributes.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestAttributes addDefault(List<NamedAttribute> pairs) {
		for (NamedAttribute p : pairs)
			if (sreq.getAttribute(p.getName()) == null) {
				Object o = p.getValue();
				sreq.setAttribute(p.getName(), o instanceof String ? vs.resolve(o) : o);
			}
		return this;
	}

	/**
	 * Adds default entries to the request attributes.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestAttributes addDefault(NamedAttributeMap pairs) {
		for (NamedAttribute p : pairs.values())
			if (sreq.getAttribute(p.getName()) == null) {
				Object o = p.getValue();
				sreq.setAttribute(p.getName(), o instanceof String ? vs.resolve(o) : o);
			}
		return this;
	}

	/**
	 * Adds default entries to the request attributes.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestAttributes addDefault(NamedAttribute...pairs) {
		return addDefault(alist(pairs));
	}

	/**
	 * Adds a default entry to the request attributes.
	 *
	 * @param name The name.
	 * @param value The value.
	 * @return This object.
	 */
	public RequestAttributes addDefault(String name, Object value) {
		return addDefault(BasicNamedAttribute.of(name, value));
	}

	/**
	 * Returns the request attribute with the specified name.
	 *
	 * @param name The attribute name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public RequestAttribute get(String name) {
		return new RequestAttribute(req, name, sreq.getAttribute(name));
	}

	/**
	 * Returns all the attribute on this request.
	 *
	 * @return All the attribute on this request.
	 */
	public List<RequestAttribute> getAll() {
		List<RequestAttribute> l = list();
		Enumeration<String> e = sreq.getAttributeNames();
		while (e.hasMoreElements()) {
			String n = e.nextElement();
			l.add(new RequestAttribute(req, n, sreq.getAttribute(n)));
		}
		return l;
	}

	/**
	 * Returns <jk>true</jk> if the attributes with the specified names are present.
	 *
	 * @param names The attribute names.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the parameters with the specified names are present.
	 */
	public boolean contains(String...names) {
		assertArgNotNull("names", names);
		for (String n : names)
			if (sreq.getAttribute(n) == null)
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if the attribute with any of the specified names are present.
	 *
	 * @param names The attribute names.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the attribute with any of the specified names are present.
	 */
	public boolean containsAny(String...names) {
		assertArgNotNull("names", names);
		for (String n : names)
			if (sreq.getAttribute(n) != null)
				return true;
		return false;
	}

	/**
	 * Sets a request attribute.
	 *
	 * @param name The attribute name.  Must not be <jk>null</jk>.
	 * @param value
	 * 	The attribute value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestAttributes set(String name, Object value) {
		assertArgNotNull("name", name);
		sreq.setAttribute(name, value);
		return this;
	}

	/**
	 * Sets request attributes.
	 *
	 * @param attributes The parameters to set.  Must not be <jk>null</jk> or contain <jk>null</jk>.
	 * @return This object.
	 */
	public RequestAttributes set(NamedAttribute...attributes) {
		assertArgNotNull("attributes", attributes);
		for (NamedAttribute p : attributes)
			set(p);
		return this;
	}

	/**
	 * Remove request attributes.
	 *
	 * @param name The attribute names.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestAttributes remove(String...name) {
		assertArgNotNull("name", name);
		for (String n : name) {
			sreq.removeAttribute(n);
		}
		return this;
	}

	/**
	 * Remove request attributes.
	 *
	 * @param attributes The attributes to remove.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestAttributes remove(NamedAttribute...attributes) {
		for (NamedAttribute p : attributes)
			remove(p.getName());
		return this;
	}

	/**
	 * Returns the request attributes as a map.
	 *
	 * @return The request attributes as a map.  Never <jk>null</jk>.
	 */
	public Map<String,Object> asMap() {
		JsonMap m = new JsonMap();
		Enumeration<String> e = sreq.getAttributeNames();
		while (e.hasMoreElements()) {
			String n = e.nextElement();
			m.put(n, sreq.getAttribute(n));
		}
		return m;
	}

	@Override /* Object */
	public String toString() {
		return asMap().toString();
	}
}
