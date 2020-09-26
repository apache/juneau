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
package org.apache.juneau.http.remote;

import static org.apache.juneau.internal.StringUtils.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.remote.*;

/**
 * Contains the meta-data about a remote proxy REST interface.
 *
 * <p>
 * Captures the information in {@link Remote @Remote} annotations for caching and reuse.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestRpc}
 * </ul>
 */
public class RrpcInterfaceMeta {

	private final Map<Method,RrpcInterfaceMethodMeta> methods;
	private final Map<String,RrpcInterfaceMethodMeta> methodsByPath;
	private final String path;
	private final Class<?> c;

	/**
	 * Constructor.
	 *
	 * @param c
	 * 	The interface class annotated with a {@link Remote @Remote} annotation.
	 * 	<br>Note that the annotations are optional.
	 * @param uri
	 * 	The absolute URL of the remote REST interface that implements this proxy interface.
	 * 	<br>This is only used on the client side.
	 */
	@SuppressWarnings("deprecation")
	public RrpcInterfaceMeta(Class<?> c, String uri) {
		this.c = c;
		String path = "";
		ClassInfo ci = ClassInfo.of(c);

		for (RemoteInterface r : ci.getAnnotations(RemoteInterface.class))
			if (! r.path().isEmpty())
				path = trimSlashes(r.path());

		for (Remote r : ci.getAnnotations(Remote.class))
			if (! r.path().isEmpty())
				path = trimSlashes(r.path());

		AMap<Method,RrpcInterfaceMethodMeta> methods = AMap.of();
		for (MethodInfo m : ci.getPublicMethods())
			methods.put(m.inner(), new RrpcInterfaceMethodMeta(uri, m.inner()));

		AMap<String,RrpcInterfaceMethodMeta> methodsByPath = AMap.of();
		for (RrpcInterfaceMethodMeta rmm : methods.values())
			methodsByPath.put(rmm.getPath(), rmm);

		this.methods = methods.unmodifiable();
		this.methodsByPath = methodsByPath.unmodifiable();
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
	public Map<String,RrpcInterfaceMethodMeta> getMethodsByPath() {
		return methodsByPath;
	}

	/**
	 * Returns the metadata about the specified method on this interface proxy.
	 *
	 * @param m The method to look up.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RrpcInterfaceMethodMeta getMethodMeta(Method m) {
		return methods.get(m);
	}

	/**
	 * Returns the metadata about the specified method on this interface proxy by the path defined on the method.
	 *
	 * @param p The HTTP path to look for.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RrpcInterfaceMethodMeta getMethodMetaByPath(String p) {
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
