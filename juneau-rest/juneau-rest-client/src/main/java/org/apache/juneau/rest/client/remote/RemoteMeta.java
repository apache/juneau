/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.client.remote;

import static org.apache.juneau.common.utils.ClassUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.http.HttpHeaders.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Contains the meta-data about a REST proxy class.
 *
 * <p>
 * Captures the information in {@link org.apache.juneau.http.remote.Remote @Remote} and {@link org.apache.juneau.http.remote.RemoteOp @RemoteOp} annotations for
 * caching and reuse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxyBasics">REST Proxy Basics</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestClientBasics">juneau-rest-client Basics</a>
 * </ul>
 */
public class RemoteMeta {

	private static String resolve(String s) {
		return VarResolver.DEFAULT.resolve(s);
	}

	private final Map<Method,RemoteOperationMeta> operations;

	private final HeaderList headers;

	/**
	 * Constructor.
	 *
	 * @param c The interface class annotated with a {@link org.apache.juneau.http.remote.Remote @Remote} annotation (optional).
	 */
	public RemoteMeta(Class<?> c) {
		var path = "";

		var ci = ClassInfo.of(c);
		var remotes = rstream(ci.getAnnotations(Remote.class).toList()).map(AnnotationInfo::inner).toList();

		var versionHeader = "Client-Version";
		var clientVersion = (String)null;
		var headers = HeaderList.create().resolving();

		for (var r : remotes) {
			if (isNotEmpty(r.path()))
				path = trimSlashes(resolve(r.path()));
			for (var h : r.headers())
				headers.append(stringHeader(resolve(h)));
			if (isNotEmpty(r.version()))
				clientVersion = resolve(r.version());
			if (isNotEmpty(r.versionHeader()))
				versionHeader = resolve(r.versionHeader());
			if (isNotVoid(r.headerList())) {
				try {
					headers.append(r.headerList().getDeclaredConstructor().newInstance().getAll());
				} catch (Exception e) {
					throw runtimeException(e, "Could not instantiate HeaderSupplier class");
				}
			}
		}

		if (nn(clientVersion))
			headers.append(stringHeader(versionHeader, clientVersion));

		Map<Method,RemoteOperationMeta> operations = map();
		var path2 = path;
		ci.getPublicMethods().stream().forEach(x -> operations.put(x.inner(), new RemoteOperationMeta(path2, x.inner(), "GET")));

		this.operations = u(operations);
		this.headers = headers;
	}

	/**
	 * Returns the headers to set on all requests.
	 *
	 * @return The headers to set on all requests.
	 */
	public HeaderList getHeaders() { return headers; }

	/**
	 * Returns the metadata about the specified operation on this resource proxy.
	 *
	 * @param m The method to look up.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RemoteOperationMeta getOperationMeta(Method m) {
		return operations.get(m);
	}
}