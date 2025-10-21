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

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.http.remote.RemoteUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.common.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.common.utils.*;

/**
 * Contains the meta-data about a Java method on a REST proxy class.
 *
 * <p>
 * Captures the information in {@link RemoteOp @RemoteOp} annotations for caching and reuse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxyBasics">REST Proxy Basics</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestClientBasics">juneau-rest-client Basics</a>
 * </ul>
 */
public class RemoteOperationMeta {

	private static class Builder {
		String httpMethod, fullPath, path;
		List<RemoteOperationArg> pathArgs = new LinkedList<>(), queryArgs = new LinkedList<>(), headerArgs = new LinkedList<>(), formDataArgs = new LinkedList<>();
		List<RemoteOperationBeanArg> requestArgs = new LinkedList<>();
		RemoteOperationArg bodyArg;
		RemoteOperationReturn methodReturn;
		Map<String,String> pathDefaults = new LinkedHashMap<>(), queryDefaults = new LinkedHashMap<>(), headerDefaults = new LinkedHashMap<>(), formDataDefaults = new LinkedHashMap<>();
		String contentDefault = null;

		Builder(String parentPath, Method m, String defaultMethod) {

			MethodInfo mi = MethodInfo.of(m);

			AnnotationList al = mi.getAnnotationList(REMOTE_OP_GROUP);
			if (al.isEmpty())
				al = mi.getReturnType().unwrap(Value.class, Optional.class).getAnnotationList(REMOTE_OP_GROUP);

			Value<String> _httpMethod = Value.empty(), _path = Value.empty();
			al.stream().map(x -> x.getName().substring(6).toUpperCase()).filter(x -> ! x.equals("OP")).forEach(x -> _httpMethod.set(x));
			al.forEachValue(String.class, "method", NOT_EMPTY, x -> _httpMethod.set(x.trim().toUpperCase()));
			al.forEachValue(String.class, "path", NOT_EMPTY, x -> _path.set(x.trim()));
			httpMethod = _httpMethod.orElse("").trim();
			path = _path.orElse("").trim();

			Value<String> value = Value.empty();
			al.forEach(RemoteOp.class, x -> isNotEmpty(x.inner().value().trim()), x -> value.set(x.inner().value().trim()));

			if (value.isPresent()) {
				String v = value.get();
				int i = v.indexOf(' ');
				if (i == -1) {
					httpMethod = v;
				} else {
					httpMethod = v.substring(0, i).trim();
					path = v.substring(i).trim();
				}
			} else {
				al.forEach(x -> ! x.isType(RemoteOp.class) && isNotEmpty(x.getValue(String.class, "value", NOT_EMPTY).orElse("").trim()),
					x -> value.set(x.getValue(String.class, "value", NOT_EMPTY).get().trim()));
				if (value.isPresent())
					path = value.get();
			}

			if (path.isEmpty()) {
				path = HttpUtils.detectHttpPath(m, StringUtils.nullIfEmpty(httpMethod));
			}
			if (httpMethod.isEmpty())
				httpMethod = HttpUtils.detectHttpMethod(m, true, defaultMethod);

			path = trimSlashes(path);

			if (! isOneOf(httpMethod, "DELETE", "GET", "POST", "PUT", "OPTIONS", "HEAD", "CONNECT", "TRACE", "PATCH"))
				throw new RemoteMetadataException(m,
					"Invalid value specified for @RemoteOp(httpMethod) annotation: '" + httpMethod + "'.  Valid values are [DELETE,GET,POST,PUT,OPTIONS,HEAD,CONNECT,TRACE,PATCH].");

			methodReturn = new RemoteOperationReturn(mi);

			fullPath = path.indexOf("://") != -1 ? path : (parentPath.isEmpty() ? urlEncodePath(path) : (trimSlashes(parentPath) + '/' + urlEncodePath(path)));

			mi.getParams().forEach(x -> {
				RemoteOperationArg rma = RemoteOperationArg.create(x);
				if (rma != null) {
					HttpPartType pt = rma.getPartType();
					if (pt == HEADER)
						headerArgs.add(rma);
					else if (pt == QUERY)
						queryArgs.add(rma);
					else if (pt == FORMDATA)
						formDataArgs.add(rma);
					else if (pt == PATH)
						pathArgs.add(rma);
					else
						bodyArg = rma;
				}
				RequestBeanMeta rmba = RequestBeanMeta.create(x, AnnotationWorkList.create());
				if (rmba != null) {
					requestArgs.add(new RemoteOperationBeanArg(x.getIndex(), rmba));
				}
			});

			// Process method-level annotations for defaults (9.2.0)
			// Note: We need to handle both individual annotations and repeated annotation arrays
			processHeaderDefaults(mi, headerDefaults);
			processQueryDefaults(mi, queryDefaults);
			processFormDataDefaults(mi, formDataDefaults);
			processPathDefaults(mi, pathDefaults);
			processContentDefaults(mi);
		}

		// Helper methods to process method-level annotations with defaults (9.2.0)
		// These handle both individual annotations and repeated annotation arrays

		private void processContentDefaults(MethodInfo mi) {
			mi.getAnnotationList().forEach(Content.class, null, x -> {
				Content c = x.inner();
				String def = c.def();
				if (isNotEmpty(def)) {
					contentDefault = def;
				}
			});
			mi.getAnnotationList().forEach(ContentAnnotation.Array.class, null, x -> {
				for (Content c : x.inner().value()) {
					String def = c.def();
					if (isNotEmpty(def)) {
						contentDefault = def;
					}
				}
			});
		}

		private static void processFormDataDefaults(MethodInfo mi, Map<String,String> defaults) {
			mi.getAnnotationList().forEach(FormData.class, null, x -> {
				FormData fd = x.inner();
				String name = firstNonEmpty(fd.name(), fd.value());
				String def = fd.def();
				if (isNotEmpty(name) && isNotEmpty(def)) {
					defaults.put(name, def);
				}
			});
			mi.getAnnotationList().forEach(FormDataAnnotation.Array.class, null, x -> {
				for (FormData fd : x.inner().value()) {
					String name = firstNonEmpty(fd.name(), fd.value());
					String def = fd.def();
					if (isNotEmpty(name) && isNotEmpty(def)) {
						defaults.put(name, def);
					}
				}
			});
		}

		private static void processHeaderDefaults(MethodInfo mi, Map<String,String> defaults) {
			// Check for individual @Header annotations
			mi.getAnnotationList().forEach(Header.class, null, x -> {
				Header h = x.inner();
				String name = firstNonEmpty(h.name(), h.value());
				String def = h.def();
				if (isNotEmpty(name) && isNotEmpty(def)) {
					defaults.put(name, def);
				}
			});
			// Check for @Header.Array (repeated annotations)
			mi.getAnnotationList().forEach(HeaderAnnotation.Array.class, null, x -> {
				for (Header h : x.inner().value()) {
					String name = firstNonEmpty(h.name(), h.value());
					String def = h.def();
					if (isNotEmpty(name) && isNotEmpty(def)) {
						defaults.put(name, def);
					}
				}
			});
		}

		private static void processPathDefaults(MethodInfo mi, Map<String,String> defaults) {
			mi.getAnnotationList().forEach(Path.class, null, x -> {
				Path p = x.inner();
				String name = firstNonEmpty(p.name(), p.value());
				String def = p.def();
				if (isNotEmpty(name) && isNotEmpty(def)) {
					defaults.put(name, def);
				}
			});
			mi.getAnnotationList().forEach(PathAnnotation.Array.class, null, x -> {
				for (Path p : x.inner().value()) {
					String name = firstNonEmpty(p.name(), p.value());
					String def = p.def();
					if (isNotEmpty(name) && isNotEmpty(def)) {
						defaults.put(name, def);
					}
				}
			});
		}

		private static void processQueryDefaults(MethodInfo mi, Map<String,String> defaults) {
			mi.getAnnotationList().forEach(Query.class, null, x -> {
				Query q = x.inner();
				String name = firstNonEmpty(q.name(), q.value());
				String def = q.def();
				if (isNotEmpty(name) && isNotEmpty(def)) {
					defaults.put(name, def);
				}
			});
			mi.getAnnotationList().forEach(QueryAnnotation.Array.class, null, x -> {
				for (Query q : x.inner().value()) {
					String name = firstNonEmpty(q.name(), q.value());
					String def = q.def();
					if (isNotEmpty(name) && isNotEmpty(def)) {
						defaults.put(name, def);
					}
				}
			});
		}
	}

	private final String httpMethod;
	private final String fullPath;
	private final RemoteOperationArg[] pathArgs, queryArgs, headerArgs, formDataArgs;
	private final RemoteOperationBeanArg[] requestArgs;
	private final RemoteOperationArg contentArg;
	private final RemoteOperationReturn methodReturn;

	private final Class<?>[] exceptions;
	// Method-level annotations with defaults (9.2.0)
	private final Map<String,String> pathDefaults, queryDefaults, headerDefaults, formDataDefaults;

	private final String contentDefault;

	/**
	 * Constructor.
	 *
	 * @param parentPath The absolute URI of the REST interface backing the interface proxy.
	 * @param m The Java method.
	 * @param defaultMethod The default HTTP method if not specified through annotation.
	 */
	public RemoteOperationMeta(final String parentPath, Method m, String defaultMethod) {
		Builder b = new Builder(parentPath, m, defaultMethod);
		this.httpMethod = b.httpMethod;
		this.fullPath = b.fullPath;
		this.pathArgs = b.pathArgs.toArray(new RemoteOperationArg[b.pathArgs.size()]);
		this.queryArgs = b.queryArgs.toArray(new RemoteOperationArg[b.queryArgs.size()]);
		this.formDataArgs = b.formDataArgs.toArray(new RemoteOperationArg[b.formDataArgs.size()]);
		this.headerArgs = b.headerArgs.toArray(new RemoteOperationArg[b.headerArgs.size()]);
		this.requestArgs = b.requestArgs.toArray(new RemoteOperationBeanArg[b.requestArgs.size()]);
		this.contentArg = b.bodyArg;
		this.methodReturn = b.methodReturn;
		this.exceptions = m.getExceptionTypes();
		this.pathDefaults = Collections.unmodifiableMap(b.pathDefaults);
		this.queryDefaults = Collections.unmodifiableMap(b.queryDefaults);
		this.headerDefaults = Collections.unmodifiableMap(b.headerDefaults);
		this.formDataDefaults = Collections.unmodifiableMap(b.formDataDefaults);
		this.contentDefault = b.contentDefault;
	}

	/**
	 * Performs an action on the exceptions thrown by this method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachException(Consumer<Class<?>> action) {
		for (Class<?> e : exceptions)
			action.accept(e);
		return this;
	}

	/**
	 * Performs an action on the {@link FormData @FormData} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachFormDataArg(Consumer<RemoteOperationArg> action) {
		for (RemoteOperationArg a : formDataArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Performs an action on the {@link Header @Header} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachHeaderArg(Consumer<RemoteOperationArg> action) {
		for (RemoteOperationArg a : headerArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Performs an action on the {@link Path @Path} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachPathArg(Consumer<RemoteOperationArg> action) {
		for (RemoteOperationArg a : pathArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Performs an action on the {@link Query @Query} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachQueryArg(Consumer<RemoteOperationArg> action) {
		for (RemoteOperationArg a : queryArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Performs an action on the {@link Request @Request} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachRequestArg(Consumer<RemoteOperationBeanArg> action) {
		for (RemoteOperationBeanArg a : requestArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Returns the argument annotated with {@link Content @Content}.
	 *
	 * @return A index of the argument with the {@link Content @Content} annotation, or <jk>null</jk> if no argument exists.
	 */
	public RemoteOperationArg getContentArg() { return contentArg; }

	/**
	 * Returns the default value for a {@link Content @Content} annotation on the method.
	 *
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getContentDefault() { return contentDefault; }

	/**
	 * Returns the default value for a {@link FormData @FormData} annotation on the method.
	 *
	 * @param name The form data parameter name.
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getFormDataDefault(String name) {
		return formDataDefaults.get(name);
	}

	/**
	 * Returns the absolute URI of the REST interface invoked by this Java method.
	 *
	 * @return The absolute URI of the REST interface, never <jk>null</jk>.
	 */
	public String getFullPath() { return fullPath; }

	/**
	 * Returns the default value for a {@link Header @Header} annotation on the method.
	 *
	 * @param name The header name.
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getHeaderDefault(String name) {
		return headerDefaults.get(name);
	}

	/**
	 * Returns the value of the {@link RemoteOp#method() @RemoteOp(method)} annotation on this Java method.
	 *
	 * @return The value of the annotation, never <jk>null</jk>.
	 */
	public String getHttpMethod() { return httpMethod; }

	/**
	 * Returns the default value for a {@link Path @Path} annotation on the method.
	 *
	 * @param name The path parameter name.
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getPathDefault(String name) {
		return pathDefaults.get(name);
	}

	/**
	 * Returns the default value for a {@link Query @Query} annotation on the method.
	 *
	 * @param name The query parameter name.
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getQueryDefault(String name) {
		return queryDefaults.get(name);
	}

	/**
	 * Returns whether the method returns the HTTP response body or status code.
	 *
	 * @return Whether the method returns the HTTP response body or status code.
	 */
	public RemoteOperationReturn getReturns() { return methodReturn; }
}