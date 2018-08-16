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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Contains the meta-data about a remoteable interface.
 *
 * <p>
 * Captures the information in {@link Remoteable @Remoteable} and {@link RemoteMethod @RemoteMethod} annotations for
 * caching and reuse.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces</a>
 * </ul>
 */
public class RemoteableMeta {

	private final Map<Method,RemoteableMethodMeta> methods;
	private final Map<String,RemoteableMethodMeta> methodsByPath;
	private final String path;
	private final Class<?> c;

	/**
	 * Constructor.
	 *
	 * @param c The interface class annotated with a {@link Remoteable @Remoteable} annotation (optional).
	 * @param restUrl The absolute URL of the remote REST interface that implements this proxy interface.
	 */
	public RemoteableMeta(Class<?> c, String restUrl) {
		this.c = c;
		String expose = "DECLARED";
		boolean useSigs = false;
		String path = "";
		List<Remoteable> rr = getAnnotationsParentFirst(Remoteable.class, c);
		if (rr.isEmpty()) {
			useSigs = true;
		} else for (Remoteable r : rr) {
			if (! r.expose().isEmpty())
				expose = r.expose();
			if (! r.path().isEmpty())
				path = trimSlashes(r.path());
			useSigs |= r.useMethodSignatures();
		}

		if (! isOneOf(expose, "ALL", "DECLARED", "ANNOTATED"))
			throw new RemoteableMetadataException(c, "Invalid value specified for ''expose'' annotation.  Valid values are [ALL,ANNOTATED,DECLARED].");

		Map<Method,RemoteableMethodMeta> methods = new LinkedHashMap<>();
		Map<String,RemoteableMethodMeta> methodsByPath = new LinkedHashMap<>();
		for (Method m : expose.equals("DECLARED") ? c.getDeclaredMethods() : c.getMethods()) {
			if (isPublic(m)) {
				RemoteMethod rm = c.getAnnotation(RemoteMethod.class);
				if (rm != null || ! expose.equals("ANNOTATED")) {
					RemoteableMethodMeta rmm = new RemoteableMethodMeta(restUrl, m, useSigs);
					methods.put(m, rmm);
					methodsByPath.put(rmm.getPath(), rmm);
				}
			}
		}

		this.methods = unmodifiableMap(methods);
		this.methodsByPath = unmodifiableMap(methodsByPath);
		this.path = path;
	}

	/**
	 * Returns a map of all methods on this interface proxy keyed by HTTP path.
	 *
	 * @return
	 * 	A map of all methods on this remoteable interface keyed by HTTP path.
	 * 	<br>The keys never have leading slashes.
	 * 	<br>The map is never <jk>null</jk>.
	 */
	public Map<String,RemoteableMethodMeta> getMethodsByPath() {
		return methodsByPath;
	}

	/**
	 * Returns the metadata about the specified method on this interface proxy.
	 *
	 * @param m The method to look up.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RemoteableMethodMeta getMethodMeta(Method m) {
		return methods.get(m);
	}

	/**
	 * Returns the metadata about the specified method on this interface proxy by the path defined on the method.
	 *
	 * @param p The HTTP path to look for.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RemoteableMethodMeta getMethodMetaByPath(String p) {
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
