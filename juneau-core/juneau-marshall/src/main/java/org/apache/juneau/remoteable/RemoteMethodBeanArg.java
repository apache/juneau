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
package org.apache.juneau.remoteable;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;

/**
 * Represents the metadata about an {@link RequestBean}-annotated argument of a method on a remote proxy interface.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces</a>
 * </ul>
 */
public final class RemoteMethodBeanArg {

	private final int index;
	private final HttpPartSerializer serializer;
	private final Map<String,RemoteMethodArg> properties;

	private RemoteMethodBeanArg(int index, Class<? extends HttpPartSerializer> serializer, Map<String,RemoteMethodArg> properties) {
		this.index = index;
		this.serializer = newInstance(HttpPartSerializer.class, serializer);
		this.properties = properties;
	}

	/**
	 * Returns the index of the parameter in the method that is a request bean.
	 *
	 * @return The index of the parameter in the method that is a request bean.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the serializer to use for serializing parts on the request bean.
	 *
	 * @return The serializer to use for serializing parts on the request bean, or <jk>null</jk> if not defined.
	 */
	public HttpPartSerializer getSerializer() {
		return serializer;
	}

	/**
	 * Returns metadata on the specified property of the request bean.
	 *
	 * @param name The bean property name.
	 * @return Metadata about the bean property, or <jk>null</jk> if not found.
	 */
	public RemoteMethodArg getProperty(String name) {
		return properties.get(name);
	}

	static RemoteMethodBeanArg create(Method m, int i) {
		Map<String,RemoteMethodArg> map = new LinkedHashMap<>();
		if (hasAnnotation(RequestBean.class, m, i)) {
			RequestBean rb = getAnnotation(RequestBean.class, m, i);
			BeanMeta<?> bm = BeanContext.DEFAULT.getBeanMeta(m.getParameterTypes()[i]);
			for (BeanPropertyMeta bp : bm.getPropertyMetas()) {
				String n = bp.getName();
				Annotation a = bp.getAnnotation(Path.class);
				if (a != null) {
					HttpPartSchema s = HttpPartSchema.create().apply(a).build();
					map.put(n, new RemoteMethodArg(PATH, s, n));
				}
				a = bp.getAnnotation(Header.class);
				if (a != null) {
					HttpPartSchema s = HttpPartSchema.create().apply(a).build();
					map.put(n, new RemoteMethodArg(HEADER, s, n));
				}
				a = bp.getAnnotation(Query.class);
				if (a != null) {
					HttpPartSchema s = HttpPartSchema.create().apply(a).build();
					map.put(n, new RemoteMethodArg(QUERY, s, n));
				}
				a = bp.getAnnotation(FormData.class);
				if (a != null) {
					HttpPartSchema s = HttpPartSchema.create().apply(a).build();
					map.put(n, new RemoteMethodArg(FORMDATA, s, n));
				}
				a = bp.getAnnotation(Body.class);
				if (a != null) {
					HttpPartSchema s = HttpPartSchema.create().apply(a).build();
					map.put(n, new RemoteMethodArg(BODY, s, n));
				}
			}
			return new RemoteMethodBeanArg(i, rb.serializer(), map);
		}
		return null;
	}
}
