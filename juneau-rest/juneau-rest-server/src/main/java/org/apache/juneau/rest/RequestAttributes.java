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

import static org.apache.juneau.assertions.Assertions.*;

import java.util.*;

import javax.servlet.http.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.svl.*;

/**
 * Represents the attributes in an HTTP request.
 *
 * <p>
 * Wraps the request attributes in a {@link Map} interface and provides several convenience methods.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmRequestAttributes}
 * </ul>
 */
public class RequestAttributes {

	final RestRequest req;
	final HttpServletRequest sreq;
	final VarResolverSession vs;

	RequestAttributes(RestRequest req) {
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
	 * @return This object (for method chaining).
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
	 * @return This object (for method chaining).
	 */
	public RequestAttributes addDefault(NamedAttributeList pairs) {
		for (NamedAttribute p : pairs.entries)
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
	 * @return This object (for method chaining).
	 */
	public RequestAttributes addDefault(NamedAttribute...pairs) {
		return addDefault(Arrays.asList(pairs));
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
		ArrayList<RequestAttribute> l = new ArrayList<>();
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
	 * @return This object (for method chaining).
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
	 * @return This object (for method chaining).
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
	 * @return This object (for method chaining).
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
	 * @return This object (for method chaining).
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
		OMap m = new OMap();
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
