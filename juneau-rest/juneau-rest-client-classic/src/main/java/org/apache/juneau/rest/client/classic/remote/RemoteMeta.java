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
package org.apache.juneau.rest.client.classic.remote;

import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.http.classic.HttpHeaders.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.http.remote.*;

/**
 * Contains the meta-data about a REST proxy class.
 *
 * <p>
 * Captures the information in {@link Remote @Remote} and {@link RemoteOp @RemoteOp} annotations for
 * caching and reuse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxies">REST Proxy Basics</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestClient">juneau-rest-client Basics</a>
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
	 * @param c The interface class annotated with a {@link Remote @Remote} annotation (optional).
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for remote interface metadata construction dispatch
	})
	public RemoteMeta(Class<?> c) {
		var path = "";

		var ci = ClassInfo.of(c);
		var remotes = rstream(ci.getAnnotations(Remote.class).toList()).map(AnnotationInfo::inner).toList();

		var versionHeader = "Client-Version";
		String clientVersion = null;
		var headers2 = HeaderList.create().resolving();

		for (var r : remotes) {
			if (ine(r.path()))
				path = trimSlashes(resolve(r.path()));
			else if (ine(r.value()))
				path = trimSlashes(resolve(r.value()));
			for (var h : r.headers())
				headers2.append(stringHeader(resolve(h)));
			if (ine(r.version()))
				clientVersion = resolve(r.version());
			if (ine(r.versionHeader()))
				versionHeader = resolve(r.versionHeader());
			if (isNotVoid(r.headerList()) && HeaderList.class.isAssignableFrom(r.headerList())) {
				try {
					headers2.append(((HeaderList) r.headerList().getDeclaredConstructor().newInstance()).getAll());
				} catch (Exception e) {
					throw rex(e, "Could not instantiate HeaderSupplier class");
				}
			}
		}

		if (nn(clientVersion))
			headers2.append(stringHeader(versionHeader, clientVersion));

		Map<Method,RemoteOperationMeta> operations2 = map();
		var path2 = path;
		ci.getPublicMethods().forEach(x -> operations2.put(x.inner(), new RemoteOperationMeta(path2, x.inner(), "GET")));

		this.operations = u(operations2);
		this.headers = headers2;
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