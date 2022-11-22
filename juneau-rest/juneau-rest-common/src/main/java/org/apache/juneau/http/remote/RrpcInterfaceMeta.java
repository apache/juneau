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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;

/**
 * Contains the meta-data about a remote proxy REST interface.
 *
 * <p>
 * Captures the information in {@link Remote @Remote} annotations for caching and reuse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestRpc">REST/RPC</a>
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
	public RrpcInterfaceMeta(Class<?> c, String uri) {
		this.c = c;
		Value<String> path = Value.of("");
		ClassInfo ci = ClassInfo.of(c);

		ci.forEachAnnotation(Remote.class, x -> isNotEmpty(x.path()), x -> path.set(trimSlashes(x.path())));

		Map<Method,RrpcInterfaceMethodMeta> methods = map();
		ci.forEachPublicMethod(
			x -> true,
			x -> methods.put(x.inner(), new RrpcInterfaceMethodMeta(uri, x.inner()))
		);

		Map<String,RrpcInterfaceMethodMeta> methodsByPath = map();
		methods.values().forEach(x -> methodsByPath.put(x.getPath(), x));

		this.methods = unmodifiable(methods);
		this.methodsByPath = unmodifiable(methodsByPath);
		this.path = path.get();
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
