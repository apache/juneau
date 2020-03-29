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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.svl.*;

/**
 * Represents the attributes in an HTTP request.
 *
 * <p>
 * Wraps the request attributes in a {@link Map} interface and provides several convenience methods.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.RequestAttributes}
 * </ul>
 */
public class RequestAttributes extends OMap {

	private static final long serialVersionUID = 1L;

	final RestRequest req;
	final OMap defaultEntries;
	final VarResolverSession vs;

	RequestAttributes(RestRequest req, OMap defaultEntries) {
		super();
		this.req = req;
		this.defaultEntries = defaultEntries;
		this.vs = req.getVarResolverSession();
	}

	@Override /* Map */
	public Object get(Object key) {
		if (key == null)
			return null;
		String k = key.toString();
		Object o = req.getAttribute(k);
		if (o == null)
			 o = req.getSession().getAttribute(k);
		if (o == null)
			o = defaultEntries.get(k);
		return resolve(o);
	}

	@Override /* Map */
	public Object put(String key, Object value) {
		Object o = req.getAttribute(key);
		req.setAttribute(key, value);
		return o;
	}

	Object resolve(Object o) {
		if (o instanceof CharSequence)
			o = vs.resolve(o.toString());
		return o;
	}

	@Override /* Map */
	public Set<java.util.Map.Entry<String,Object>> entrySet() {
		return new AbstractSet<java.util.Map.Entry<String,Object>>() {

			@Override /* Set */
			public Iterator<java.util.Map.Entry<String,Object>> iterator() {

				return new Iterator<java.util.Map.Entry<String,Object>>() {
					Set<String> keys = new LinkedHashSet<>();
					{
						for (String s : iterable(req.getAttributeNames()))
							keys.add(s);
						for (String s : iterable(req.getSession().getAttributeNames()))
							keys.add(s);
					}
					Iterator<String> keyIterator = keys.iterator();
					Iterator<Map.Entry<String,Object>> defaultsIterator = defaultEntries.entrySet().iterator();
					Map.Entry<String,Object> peekNext;

					@Override /* Iterator */
					public boolean hasNext() {
						if (keyIterator.hasNext())
							return true;
						while (defaultsIterator.hasNext() && peekNext == null) {
							peekNext = defaultsIterator.next();
							if (keys.contains(peekNext.getKey()))
								peekNext = null;
						}
						return peekNext != null;
					}

					@Override /* Iterator */
					public java.util.Map.Entry<String,Object> next() {
						if (keyIterator.hasNext()) {
							final String k = keyIterator.next();
							return new java.util.Map.Entry<String,Object>() {

								@Override /* Map.Entry */
								public String getKey() {
									return k;
								}

								@Override /* Map.Entry */
								public Object getValue() {
									return resolve(req.getAttribute(k));
								}

								@Override /* Map.Entry */
								public Object setValue(Object value) {
									Object o = req.getAttribute(k);
									req.setAttribute(k, value);
									return o;
								}
							};
						}
						while (defaultsIterator.hasNext() && peekNext == null) {
							peekNext = defaultsIterator.next();
							if (keys.contains(peekNext.getKey()))
								peekNext = null;
						}
						if (peekNext != null) {
							final java.util.Map.Entry<String,Object> o = peekNext;
							java.util.Map.Entry<String,Object> o2 = new java.util.Map.Entry<String,Object>() {

								@Override /* Map.Entry */
								public String getKey() {
									return o.getKey();
								}

								@Override /* Map.Entry */
								public Object getValue() {
									return resolve(o.getValue());
								}

								@Override /* Map.Entry */
								public Object setValue(Object value) {
									Object o3 = o.getValue();
									req.setAttribute(o.getKey(), value);
									return resolve(o3);
								}
							};
							peekNext = null;
							return o2;
						}
						return null;
					}

				};
			}

			@Override /* Set */
			public int size() {
				int i = defaultEntries.size();
				for (String s : iterable(req.getAttributeNames()))
					if (! defaultEntries.containsKey(s))
						i++;
				return i;
			}
		};
	}
}
