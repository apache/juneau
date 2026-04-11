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
package org.apache.juneau.ng.http.remote;

import java.lang.reflect.*;

import org.apache.juneau.http.remote.RemoteReturn;

/**
 * Holds resolved metadata for a single method on an interface annotated with
 * {@link org.apache.juneau.http.remote.Remote}.
 *
 * <p>
 * Extracted at interface-discovery time (see {@link RrpcInterfaceMeta}) and cached for
 * efficient proxy invocation.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class RrpcInterfaceMethodMeta {

	private final Method method;
	private final String httpMethod;
	private final String path;
	private final RemoteReturn returnType;

	RrpcInterfaceMethodMeta(Method method, String httpMethod, String path, RemoteReturn returnType) {
		this.method = method;
		this.httpMethod = httpMethod;
		this.path = path;
		this.returnType = returnType;
	}

	/**
	 * Returns the Java method this metadata is for.
	 *
	 * @return The method. Never <jk>null</jk>.
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * Returns the HTTP method (e.g. {@code "GET"}, {@code "POST"}).
	 *
	 * @return The HTTP method. Never <jk>null</jk>.
	 */
	public String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the resolved path for this operation (relative to the interface base path).
	 *
	 * @return The path. Never <jk>null</jk>, but may be empty.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns what the proxy method should return.
	 *
	 * @return The return type. Never <jk>null</jk>.
	 */
	public RemoteReturn getReturnType() {
		return returnType;
	}

	@Override /* Object */
	public String toString() {
		return httpMethod + " " + path + " → " + method.getName() + "()";
	}
}
