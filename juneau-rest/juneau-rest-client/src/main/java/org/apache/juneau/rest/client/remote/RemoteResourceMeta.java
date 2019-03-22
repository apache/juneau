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
package org.apache.juneau.rest.client.remote;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.reflection.*;

/**
 * Contains the meta-data about a REST proxy class.
 *
 * <p>
 * Captures the information in {@link RemoteResource @RemoteResource} and {@link RemoteMethod @RemoteMethod} annotations for
 * caching and reuse.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-rest-client.RestProxies}
 * </ul>
 */
public class RemoteResourceMeta {

	private final Map<Method,RemoteMethodMeta> methods;
	private final String path;

	/**
	 * Constructor.
	 *
	 * @param c The interface class annotated with a {@link RemoteResource @RemoteResource} annotation (optional).
	 */
	public RemoteResourceMeta(Class<?> c) {
		String path = "";

		ClassInfo ci = ClassInfo.lookup(c);
		for (RemoteResource r : ci.getAnnotations(RemoteResource.class, true))
			if (! r.path().isEmpty())
				path = trimSlashes(r.path());

		Map<Method,RemoteMethodMeta> methods = new LinkedHashMap<>();
		for (MethodInfo m : ci.getPublicMethods())
			if (m.isPublic())
				methods.put(m.getInner(), new RemoteMethodMeta(path, m.getInner(), false, "GET"));

		this.methods = unmodifiableMap(methods);
		this.path = path;
	}

	/**
	 * Returns the metadata about the specified method on this resource proxy.
	 *
	 * @param m The method to look up.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RemoteMethodMeta getMethodMeta(Method m) {
		return methods.get(m);
	}

	/**
	 * Returns the HTTP path of this interface.
	 *
	 * @return
	 * 	The HTTP path of this interface.
	 * 	<br>Never <jk>null</jk>.
	 * 	<br>Never has leading or trailing slashes.
	 */
	public String getPath() {
		return path;
	}
}
