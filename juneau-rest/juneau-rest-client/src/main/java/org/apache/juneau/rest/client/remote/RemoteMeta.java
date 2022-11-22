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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Contains the meta-data about a REST proxy class.
 *
 * <p>
 * Captures the information in {@link org.apache.juneau.http.remote.Remote @Remote} and {@link org.apache.juneau.http.remote.RemoteOp @RemoteOp} annotations for
 * caching and reuse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#jrc.Proxies">REST Proxies</a>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public class RemoteMeta {

	private final Map<Method,RemoteOperationMeta> operations;
	private final HeaderList headers;

	/**
	 * Constructor.
	 *
	 * @param c The interface class annotated with a {@link org.apache.juneau.http.remote.Remote @Remote} annotation (optional).
	 */
	public RemoteMeta(Class<?> c) {
		String path = "";

		ClassInfo ci = ClassInfo.of(c);
		List<Remote> remotes = ci.getAnnotations(Remote.class);

		String versionHeader = "Client-Version", clientVersion = null;
		HeaderList headers = HeaderList.create().resolving();

		for (Remote r : remotes) {
			if (isNotEmpty(r.path()))
				path = trimSlashes(resolve(r.path()));
			for (String h : r.headers())
				headers.append(stringHeader(resolve(h)));
			if (isNotEmpty(r.version()))
				clientVersion = resolve(r.version());
			if (isNotEmpty(r.versionHeader()))
				versionHeader = resolve(r.versionHeader());
			if (isNotVoid(r.headerList())) {
				try {
					headers.append(r.headerList().getDeclaredConstructor().newInstance().getAll());
				} catch (Exception e) {
					throw new BasicRuntimeException(e, "Could not instantiate HeaderSupplier class");
				}
			}
		}

		if (clientVersion != null)
			headers.append(stringHeader(versionHeader, clientVersion));

		Map<Method,RemoteOperationMeta> operations = map();
		String path2 = path;
		ci.forEachPublicMethod(
			x -> true,
			x -> operations.put(x.inner(), new RemoteOperationMeta(path2, x.inner(), "GET"))
		);

		this.operations = unmodifiable(operations);
		this.headers = headers;
	}

	/**
	 * Returns the metadata about the specified operation on this resource proxy.
	 *
	 * @param m The method to look up.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RemoteOperationMeta getOperationMeta(Method m) {
		return operations.get(m);
	}

	/**
	 * Returns the headers to set on all requests.
	 *
	 * @return The headers to set on all requests.
	 */
	public HeaderList getHeaders() {
		return headers;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static String resolve(String s) {
		return VarResolver.DEFAULT.resolve(s);
	}
}
