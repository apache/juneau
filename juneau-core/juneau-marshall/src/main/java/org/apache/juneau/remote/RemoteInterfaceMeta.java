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
package org.apache.juneau.remote;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Contains the meta-data about a remote proxy REST interface.
 *
 * <p>
 * Captures the information in {@link RemoteInterface @RemoteInterface} annotations for caching and reuse.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-rest-server.rRPC}
 * </ul>
 */
public class RemoteInterfaceMeta {

	private final Map<Method,RemoteInterfaceMethod> methods;
	private final Map<String,RemoteInterfaceMethod> methodsByPath;
	private final String path;
	private final Class<?> c;

	/**
	 * Constructor.
	 *
	 * @param c
	 * 	The interface class annotated with a {@link RemoteInterface @RemoteInterface} annotation.
	 * 	<br>Note that the annotations are optional.
	 * @param uri
	 * 	The absolute URL of the remote REST interface that implements this proxy interface.
	 * 	<br>This is only used on the client side.
	 */
	public RemoteInterfaceMeta(Class<?> c, String uri) {
		this.c = c;
		String path = "";
		List<RemoteInterface> rr = getAnnotationsParentFirst(RemoteInterface.class, c);
		for (RemoteInterface r : rr)
			if (! r.path().isEmpty())
				path = trimSlashes(r.path());

		Map<Method,RemoteInterfaceMethod> methods = new LinkedHashMap<>();
		for (Method m : c.getMethods())
			if (isPublic(m))
				methods.put(m, new RemoteInterfaceMethod(uri, m));

		Map<String,RemoteInterfaceMethod> methodsByPath = new LinkedHashMap<>();
		for (RemoteInterfaceMethod rmm : methods.values())
			methodsByPath.put(rmm.getPath(), rmm);

		this.methods = unmodifiableMap(methods);
		this.methodsByPath = unmodifiableMap(methodsByPath);
		this.path = path;
	}

	/**
	 * Returns a map of all methods on this interface proxy keyed by HTTP path.
	 *
	 * @return
	 * 	A map of all methods on this remote interface keyed by HTTP path.
	 * 	<br>The keys never have leading slashes.
	 * 	<br>The map is never <jk>null</jk>.
	 */
	public Map<String,RemoteInterfaceMethod> getMethodsByPath() {
		return methodsByPath;
	}

	/**
	 * Returns the metadata about the specified method on this interface proxy.
	 *
	 * @param m The method to look up.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RemoteInterfaceMethod getMethodMeta(Method m) {
		return methods.get(m);
	}

	/**
	 * Returns the metadata about the specified method on this interface proxy by the path defined on the method.
	 *
	 * @param p The HTTP path to look for.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RemoteInterfaceMethod getMethodMetaByPath(String p) {
		return methodsByPath.get(p);
	}

	/**
	 * Returns the Java class of this interface.
	 *
	 * @return
	 * 	The Java class of this interface.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Class<?> getJavaClass() {
		return c;
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
