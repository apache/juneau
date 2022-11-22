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

import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.http.remote.RemoteUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.remote.RemoteOp;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Contains the meta-data about a Java method on a REST proxy class.
 *
 * <p>
 * Captures the information in {@link RemoteOp @RemoteOp} annotations for caching and reuse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#jrc.Proxies">REST Proxies</a>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public class RemoteOperationMeta {

	private final String httpMethod;
	private final String fullPath;
	private final RemoteOperationArg[] pathArgs, queryArgs, headerArgs, formDataArgs;
	private final RemoteOperationBeanArg[] requestArgs;
	private final RemoteOperationArg contentArg;
	private final RemoteOperationReturn methodReturn;
	private final Class<?>[] exceptions;

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
	}

	private static final class Builder {
		String httpMethod, fullPath, path;
		List<RemoteOperationArg>
			pathArgs = new LinkedList<>(),
			queryArgs = new LinkedList<>(),
			headerArgs = new LinkedList<>(),
			formDataArgs = new LinkedList<>();
		List<RemoteOperationBeanArg>
			requestArgs = new LinkedList<>();
		RemoteOperationArg bodyArg;
		RemoteOperationReturn methodReturn;

		Builder(String parentPath, Method m, String defaultMethod) {

			MethodInfo mi = MethodInfo.of(m);

			AnnotationList al = mi.getAnnotationList(REMOTE_OP_GROUP);
			if (al.isEmpty())
				al = mi.getReturnType().unwrap(Value.class,Optional.class).getAnnotationList(REMOTE_OP_GROUP);

			Value<String> _httpMethod = Value.empty(), _path = Value.empty();
			al.stream().map(x -> x.getName().substring(6).toUpperCase()).filter(x -> ! x.equals("OP")).forEach(x -> _httpMethod.set(x));
			al.forEachValue(String.class, "method", NOT_EMPTY, x -> _httpMethod.set(x.trim().toUpperCase()));
			al.forEachValue(String.class, "path", NOT_EMPTY, x-> _path.set(x.trim()));
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
				al.forEach(x -> ! x.isType(RemoteOp.class) && isNotEmpty(x.getValue(String.class, "value", NOT_EMPTY).orElse("").trim()),x -> value.set(x.getValue(String.class, "value", NOT_EMPTY).get().trim()));
				if (value.isPresent())
					path = value.get();
			}

			if (path.isEmpty()) {
				path = HttpUtils.detectHttpPath(m, nullIfEmpty(httpMethod));
			}
			if (httpMethod.isEmpty())
				httpMethod = HttpUtils.detectHttpMethod(m, true, defaultMethod);

			path = trimSlashes(path);

			if (! isOneOf(httpMethod, "DELETE", "GET", "POST", "PUT", "OPTIONS", "HEAD", "CONNECT", "TRACE", "PATCH"))
				throw new RemoteMetadataException(m,
					"Invalid value specified for @RemoteOp(httpMethod) annotation: '"+httpMethod+"'.  Valid values are [DELETE,GET,POST,PUT,OPTIONS,HEAD,CONNECT,TRACE,PATCH].");

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
		}
	}

	/**
	 * Returns the value of the {@link RemoteOp#method() @RemoteOp(method)} annotation on this Java method.
	 *
	 * @return The value of the annotation, never <jk>null</jk>.
	 */
	public String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the absolute URI of the REST interface invoked by this Java method.
	 *
	 * @return The absolute URI of the REST interface, never <jk>null</jk>.
	 */
	public String getFullPath() {
		return fullPath;
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
	public RemoteOperationArg getContentArg() {
		return contentArg;
	}

	/**
	 * Returns whether the method returns the HTTP response body or status code.
	 *
	 * @return Whether the method returns the HTTP response body or status code.
	 */
	public RemoteOperationReturn getReturns() {
		return methodReturn;
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
}
