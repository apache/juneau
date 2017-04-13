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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Contains the meta-data about a remoteable interface.
 * <p>
 * Captures the information in {@link Remoteable @Remoteable} and {@link RemoteMethod @RemoteMethod}
 * annotations for caching and reuse.
 */
public class RemoteableMeta {

	private final Map<Method,RemoteableMethodMeta> methods;

	/**
	 * Constructor.
	 *
	 * @param c The interface class annotated with a {@link Remoteable @Remoteable} annotation (optional).
	 * @param restUrl The absolute URL of the remote REST interface that implements this proxy interface.
	 */
	public RemoteableMeta(Class<?> c, String restUrl) {
		Remoteable r = ReflectionUtils.getAnnotation(Remoteable.class, c);

		String expose = r == null ? "DECLARED" : r.expose();
		if (! isOneOf(expose, "ALL", "DECLARED", "ANNOTATED"))
			throw new RemoteableMetadataException(c, "Invalid value specified for ''expose'' annotation.  Valid values are [ALL,ANNOTATED,DECLARED].");

		Map<Method,RemoteableMethodMeta> _methods = new LinkedHashMap<Method,RemoteableMethodMeta>();
		for (Method m : expose.equals("DECLARED") ? c.getDeclaredMethods() : c.getMethods()) {
			if (isPublic(m)) {
				RemoteMethod rm = c.getAnnotation(RemoteMethod.class);
				if (rm != null || ! expose.equals("ANNOTATED"))
					_methods.put(m, new RemoteableMethodMeta(restUrl, m));
			}
		}

		this.methods = Collections.unmodifiableMap(_methods);
	}

	/**
	 * Returns the metadata about the specified method on this interface proxy.
	 *
	 * @param m The method to look up.
	 * @return Metadata about the method, or <jk>null</jk> if no metadata was found.
	 */
	public RemoteableMethodMeta getMethodMeta(Method m) {
		return methods.get(m);
	}
}
