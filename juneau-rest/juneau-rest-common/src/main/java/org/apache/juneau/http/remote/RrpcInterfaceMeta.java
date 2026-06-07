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
package org.apache.juneau.http.remote;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.rest.common.utils.*;

/**
 * Holds resolved metadata for an interface annotated with {@link org.apache.juneau.http.remote.Remote @Remote}.
 *
 * <p>
 * Built once per interface class and cached for efficient proxy invocation.
 * Use {@link #of(Class)} to obtain instances.
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
public final class RrpcInterfaceMeta {

	private static final Map<Class<?>, RrpcInterfaceMeta> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	private final Class<?> iface;
	private final String basePath;
	private final Map<Method, RrpcInterfaceMethodMeta> methodMetas;
	private final Map<String, RrpcInterfaceMethodMeta> methodMetasByPath;

	private RrpcInterfaceMeta(Class<?> iface) {
		this(iface, false);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Provides backwards compatibility with the classic {@code RrpcInterfaceMeta} signature; the {@code uri}
	 * argument is accepted for API parity and is currently ignored by this implementation.
	 *
	 * <p>
	 * Unlike {@link #of(Class)}, this constructor includes <i>all</i> public interface methods (matching the
	 * classic behavior), not just those annotated with the {@code @Remote*} annotations.
	 *
	 * @param iface The interface class. Must not be <jk>null</jk>.
	 * @param uri Reserved for compatibility with the classic constructor. May be <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1172" // 'uri' is part of the public constructor signature retained for API parity with the classic RrpcInterfaceMeta; intentionally ignored
	})
	public RrpcInterfaceMeta(Class<?> iface, String uri) {
		this(iface, true);
	}

	private RrpcInterfaceMeta(Class<?> iface, boolean includeUnannotated) {
		if (!iface.isInterface())
			throw illegalArg("Class {0} is not an interface", iface.getName());
		var remote = iface.getAnnotation(Remote.class);

		this.iface = iface;
		this.basePath = buildBasePath(remote);

		var metas = new LinkedHashMap<Method, RrpcInterfaceMethodMeta>();
		for (var m : iface.getMethods()) {
			var opt = buildMethodMeta(m);
			if (opt.isPresent()) {
				metas.put(m, opt.get());
			} else if (includeUnannotated) {
				metas.put(m, new RrpcInterfaceMethodMeta(m, "POST", buildSignaturePath(m), RemoteReturn.BODY));
			}
		}
		this.methodMetas = Collections.unmodifiableMap(metas);

		var byPath = new LinkedHashMap<String, RrpcInterfaceMethodMeta>();
		for (var v : metas.values())
			byPath.put(v.getPath(), v);
		this.methodMetasByPath = Collections.unmodifiableMap(byPath);
	}

	private static String buildBasePath(Remote remote) {
		if (remote == null)
			return "";
		return remote.path().isEmpty() ? remote.value() : remote.path();
	}

	private static String buildSignaturePath(Method m) {
		var sb = new StringBuilder(128);
		sb.append(m.getName()).append('/');
		Class<?>[] pt = m.getParameterTypes();
		if (pt.length == 0)
			return sb.toString();
		sb.append('(');
		for (var i = 0; i < pt.length; i++) {
			if (i > 0)
				sb.append(',');
			appendTypeName(sb, pt[i]);
		}
		sb.append(')');
		return sb.toString();
	}

	private static void appendTypeName(StringBuilder sb, Class<?> c) {
		if (c.isArray()) {
			appendTypeName(sb, c.getComponentType());
			sb.append("[]");
		} else {
			sb.append(c.getName());
		}
	}

	/**
	 * Returns the {@link RrpcInterfaceMeta} for the given interface, creating and caching it if necessary.
	 *
	 * @param iface The interface class. Must be annotated with {@link org.apache.juneau.http.remote.Remote}. Must not be <jk>null</jk>.
	 * @return The metadata. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the class is not an interface or not annotated with {@link org.apache.juneau.http.remote.Remote}.
	 */
	public static RrpcInterfaceMeta of(Class<?> iface) {
		assertArgNotNull("iface", iface);
		if (!iface.isInterface())
			throw illegalArg("Class {0} is not an interface", iface.getName());
		if (iface.getAnnotation(Remote.class) == null)
			throw illegalArg("Class {0} is not annotated with @Remote", iface.getName());
		return CACHE.computeIfAbsent(iface, RrpcInterfaceMeta::new);
	}

	/**
	 * Returns the interface class.
	 *
	 * @return The interface. Never <jk>null</jk>.
	 */
	public Class<?> getInterface() {
		return iface;
	}

	/**
	 * Returns the Java class of this interface.
	 *
	 * <p>
	 * Alias for {@link #getInterface()} provided for backwards compatibility with the classic API.
	 *
	 * @return The Java class. Never <jk>null</jk>.
	 */
	public Class<?> getJavaClass() {
		return getInterface();
	}

	/**
	 * Returns the method metadata keyed by the resolved method path.
	 *
	 * @return An unmodifiable map from path to {@link RrpcInterfaceMethodMeta}. Never <jk>null</jk>.
	 */
	public Map<String, RrpcInterfaceMethodMeta> getMethodsByPath() {
		return methodMetasByPath;
	}

	/**
	 * Returns the method metadata for the given path.
	 *
	 * @param path The path. May be <jk>null</jk>.
	 * @return The matching method metadata, or <jk>null</jk> if no match was found.
	 */
	public RrpcInterfaceMethodMeta getMethodMetaByPath(String path) {
		return path == null ? null : methodMetasByPath.get(path);
	}

	/**
	 * Returns the base path from the {@link org.apache.juneau.http.remote.Remote#path() Remote} annotation.
	 *
	 * @return The base path. Never <jk>null</jk>, but may be empty.
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Returns the method metadata for all annotated methods.
	 *
	 * @return An unmodifiable map from {@link Method} to {@link RrpcInterfaceMethodMeta}. Never <jk>null</jk>.
	 */
	public Map<Method, RrpcInterfaceMethodMeta> getMethodMetas() {
		return methodMetas;
	}

	/**
	 * Returns the metadata for the given method, or {@code null} if the method has no remote annotation.
	 *
	 * @param method The method. Must not be <jk>null</jk>.
	 * @return The metadata, or <jk>null</jk>.
	 */
	public RrpcInterfaceMethodMeta getMethodMeta(Method method) {
		return methodMetas.get(method);
	}

	private static Optional<RrpcInterfaceMethodMeta> buildMethodMeta(Method m) {
		if (m.isAnnotationPresent(RemoteGet.class)) {
			var a = m.getAnnotation(RemoteGet.class);
			return opt(simpleMeta(m, "GET", a.path(), a.value(), a.returns()));
		}
		if (m.isAnnotationPresent(RemotePost.class)) {
			var a = m.getAnnotation(RemotePost.class);
			return opt(simpleMeta(m, "POST", a.path(), a.value(), a.returns()));
		}
		if (m.isAnnotationPresent(RemotePut.class)) {
			var a = m.getAnnotation(RemotePut.class);
			return opt(simpleMeta(m, "PUT", a.path(), a.value(), a.returns()));
		}
		if (m.isAnnotationPresent(RemotePatch.class)) {
			var a = m.getAnnotation(RemotePatch.class);
			return opt(simpleMeta(m, "PATCH", a.path(), a.value(), a.returns()));
		}
		if (m.isAnnotationPresent(RemoteDelete.class)) {
			var a = m.getAnnotation(RemoteDelete.class);
			return opt(simpleMeta(m, "DELETE", a.path(), a.value(), a.returns()));
		}
		if (m.isAnnotationPresent(RemoteOp.class))
			return opt(buildRemoteOpMeta(m, m.getAnnotation(RemoteOp.class)));

		return opte();
	}

	private static RrpcInterfaceMethodMeta simpleMeta(Method m, String httpMethod, String pathAttr, String valueAttr, RemoteReturn returnType) {
		var path = pathAttr.isEmpty() ? valueAttr : pathAttr;
		return new RrpcInterfaceMethodMeta(m, httpMethod, path, returnType);
	}

	private static RrpcInterfaceMethodMeta buildRemoteOpMeta(Method m, RemoteOp a) {
		var method = a.method().trim();
		var path = a.path().trim();
		var returnType = a.returns();
		var v = a.value().trim();
		if (!v.isEmpty()) {
			var i = v.indexOf(' ');
			if (i == -1) {
				method = v.toUpperCase();
			} else {
				method = v.substring(0, i).trim().toUpperCase();
				path = v.substring(i).trim();
			}
		}
		if (path.isEmpty())
			path = HttpUtils.detectHttpPath(m, isEmpty(method) ? null : method);
		if (method.isEmpty())
			method = HttpUtils.detectHttpMethod(m, true, "GET");
		if (!isOneOf(method, "DELETE", "GET", "POST", "PUT", "OPTIONS", "HEAD", "CONNECT", "TRACE", "PATCH"))
			throw illegalArg("Invalid @RemoteOp method ''{0}'' on {1}.{2}", method, m.getDeclaringClass().getName(), m.getName());
		return new RrpcInterfaceMethodMeta(m, method, trimSlashes(path), returnType);
	}

	@Override /* Object */
	public String toString() {
		return "@Remote " + iface.getSimpleName() + " (basePath=" + basePath + ", methods=" + methodMetas.size() + ")";
	}
}
