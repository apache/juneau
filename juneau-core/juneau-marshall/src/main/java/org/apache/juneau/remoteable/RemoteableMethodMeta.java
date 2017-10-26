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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Contains the meta-data about a Java method on a remoteable interface.
 *
 * <p>
 * Captures the information in {@link RemoteMethod @RemoteMethod} annotations for caching and reuse.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../overview-summary.html#Remoteable.3rdParty'>Interface proxies against 3rd-party REST interfaces</a>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.remoteable</a>
 * </ul>
 */
public class RemoteableMethodMeta {

	private final String httpMethod;
	private final String url;
	private final RemoteMethodArg[] pathArgs, queryArgs, headerArgs, formDataArgs, requestBeanArgs;
	private final Integer[] otherArgs;
	private final Integer bodyArg;
	private final ReturnValue returnValue;

	/**
	 * Constructor.
	 *
	 * @param restUrl The absolute URL of the REST interface backing the interface proxy.
	 * @param m The Java method.
	 */
	public RemoteableMethodMeta(final String restUrl, Method m) {
		Builder b = new Builder(restUrl, m);
		this.httpMethod = b.httpMethod;
		this.url = b.url;
		this.pathArgs = b.pathArgs.toArray(new RemoteMethodArg[b.pathArgs.size()]);
		this.queryArgs = b.queryArgs.toArray(new RemoteMethodArg[b.queryArgs.size()]);
		this.formDataArgs = b.formDataArgs.toArray(new RemoteMethodArg[b.formDataArgs.size()]);
		this.headerArgs = b.headerArgs.toArray(new RemoteMethodArg[b.headerArgs.size()]);
		this.requestBeanArgs = b.requestBeanArgs.toArray(new RemoteMethodArg[b.requestBeanArgs.size()]);
		this.otherArgs = b.otherArgs.toArray(new Integer[b.otherArgs.size()]);
		this.bodyArg = b.bodyArg;
		this.returnValue = b.returnValue;
	}

	private static class Builder {
		private String httpMethod, url;
		private List<RemoteMethodArg>
			pathArgs = new LinkedList<>(),
			queryArgs = new LinkedList<>(),
			headerArgs = new LinkedList<>(),
			formDataArgs = new LinkedList<>(),
			requestBeanArgs = new LinkedList<>();
		private List<Integer>
			otherArgs = new LinkedList<>();
		private Integer bodyArg;
		private ReturnValue returnValue;

		private Builder(String restUrl, Method m) {
			Remoteable r = m.getDeclaringClass().getAnnotation(Remoteable.class);
			RemoteMethod rm = m.getAnnotation(RemoteMethod.class);

			httpMethod = rm == null ? "POST" : rm.httpMethod();
			if (! isOneOf(httpMethod, "DELETE", "GET", "POST", "PUT"))
				throw new RemoteableMetadataException(m,
					"Invalid value specified for @RemoteMethod.httpMethod() annotation.  Valid values are [DELTE,GET,POST,PUT].");

			String path = rm == null || rm.path().isEmpty() ? null : rm.path();
			String methodPaths = r == null ? "NAME" : r.methodPaths();

			if (! isOneOf(methodPaths, "NAME", "SIGNATURE"))
				throw new RemoteableMetadataException(m,
					"Invalid value specified for @Remoteable.methodPaths() annotation.  Valid values are [NAME,SIGNATURE].");

			returnValue = rm == null ? ReturnValue.BODY : rm.returns();

			url =
				trimSlashes(restUrl)
				+ '/'
				+ (path != null ? trimSlashes(path) : urlEncode("NAME".equals(methodPaths) ? m.getName() : getMethodSignature(m)));

			int index = 0;
			for (Annotation[] aa : m.getParameterAnnotations()) {
				boolean annotated = false;
				for (Annotation a : aa) {
					Class<?> ca = a.annotationType();
					if (ca == Path.class) {
						Path p = (Path)a;
						annotated = pathArgs.add(new RemoteMethodArg(p.name(), p.value(), index, false, p.serializer()));
					} else if (ca == Query.class) {
						Query q = (Query)a;
						annotated = queryArgs.add(new RemoteMethodArg(q.name(), q.value(), index, q.skipIfEmpty(), q.serializer()));
					} else if (ca == QueryIfNE.class) {
						QueryIfNE q = (QueryIfNE)a;
						annotated = queryArgs.add(new RemoteMethodArg(q.name(), q.value(), index, true, q.serializer()));
					} else if (ca == FormData.class) {
						FormData f = (FormData)a;
						annotated = formDataArgs.add(new RemoteMethodArg(f.name(), f.value(), index, f.skipIfEmpty(), f.serializer()));
					} else if (ca == FormDataIfNE.class) {
						FormDataIfNE f = (FormDataIfNE)a;
						annotated = formDataArgs.add(new RemoteMethodArg(f.name(), f.value(), index, true, f.serializer()));
					} else if (ca == Header.class) {
						Header h = (Header)a;
						annotated = headerArgs.add(new RemoteMethodArg(h.name(), h.value(), index, h.skipIfEmpty(), h.serializer()));
					} else if (ca == HeaderIfNE.class) {
						HeaderIfNE h = (HeaderIfNE)a;
						annotated = headerArgs.add(new RemoteMethodArg(h.name(), h.value(), index, true, h.serializer()));
					} else if (ca == RequestBean.class) {
						RequestBean rb = (RequestBean)a;
						annotated = requestBeanArgs.add(new RemoteMethodArg("", "", index, false, rb.serializer()));
					} else if (ca == Body.class) {
						annotated = true;
						if (bodyArg == null)
							bodyArg = index;
						else
							throw new RemoteableMetadataException(m,
								"Multiple @Body parameters found.  Only one can be specified per Java method.");
					}
				}
				if (! annotated)
					otherArgs.add(index);
				index++;
			}

			if (bodyArg != null && otherArgs.size() > 0)
				throw new RemoteableMetadataException(m,
					"@Body and non-annotated parameters found together.  Non-annotated parameters cannot be used when @Body is used.");
		}
	}

	/**
	 * Returns the value of the {@link RemoteMethod#httpMethod()} annotation on this Java method.
	 *
	 * @return The value of the {@link RemoteMethod#httpMethod()} annotation, never <jk>null</jk>.
	 */
	public String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the absolute URL of the REST interface invoked by this Java method.
	 *
	 * @return The absolute URL of the REST interface, never <jk>null</jk>.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Returns the {@link Path @Path} annotated arguments on this Java method.
	 *
	 * @return A map of {@link Path#value() @Path.value()} names to zero-indexed argument indices.
	 */
	public RemoteMethodArg[] getPathArgs() {
		return pathArgs;
	}

	/**
	 * Returns the {@link Query @Query} annotated arguments on this Java method.
	 *
	 * @return A map of {@link Query#value() @Query.value()} names to zero-indexed argument indices.
	 */
	public RemoteMethodArg[] getQueryArgs() {
		return queryArgs;
	}

	/**
	 * Returns the {@link FormData @FormData} annotated arguments on this Java method.
	 *
	 * @return A map of {@link FormData#value() @FormData.value()} names to zero-indexed argument indices.
	 */
	public RemoteMethodArg[] getFormDataArgs() {
		return formDataArgs;
	}

	/**
	 * Returns the {@link Header @Header} annotated arguments on this Java method.
	 *
	 * @return A map of {@link Header#value() @Header.value()} names to zero-indexed argument indices.
	 */
	public RemoteMethodArg[] getHeaderArgs() {
		return headerArgs;
	}

	/**
	 * Returns the {@link RequestBean @RequestBean} annotated arguments on this Java method.
	 *
	 * @return A list of zero-indexed argument indices.
	 */
	public RemoteMethodArg[] getRequestBeanArgs() {
		return requestBeanArgs;
	}

	/**
	 * Returns the remaining non-annotated arguments on this Java method.
	 *
	 * @return A list of zero-indexed argument indices.
	 */
	public Integer[] getOtherArgs() {
		return otherArgs;
	}

	/**
	 * Returns the argument annotated with {@link Body @Body}.
	 *
	 * @return A index of the argument with the {@link Body @Body} annotation, or <jk>null</jk> if no argument exists.
	 */
	public Integer getBodyArg() {
		return bodyArg;
	}

	/**
	 * Returns whether the method returns the HTTP response body or status code.
	 *
	 * @return Whether the method returns the HTTP response body or status code.
	 */
	public ReturnValue getReturns() {
		return returnValue;
	}
}
