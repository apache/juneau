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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;

/**
 * Contains the meta-data about a Java method on a remoteable interface.
 *
 * <p>
 * Captures the information in {@link RemoteMethod @RemoteMethod} annotations for caching and reuse.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces</a>
 * </ul>
 */
public class RemoteableMethodMeta {

	private final String httpMethod;
	private final String url;
	private final RemoteMethodArg[] pathArgs, queryArgs, headerArgs, formDataArgs, otherArgs;
	private final RemoteMethodBeanArg[] requestArgs;
	private final RemoteMethodArg bodyArg;
	private final RemoteMethodReturn methodReturn;

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
		this.requestArgs = b.requestArgs.toArray(new RemoteMethodBeanArg[b.requestArgs.size()]);
		this.otherArgs = b.otherArgs.toArray(new RemoteMethodArg[b.otherArgs.size()]);
		this.bodyArg = b.bodyArg;
		this.methodReturn = b.methodReturn;
	}

	private static final class Builder {
		String httpMethod, url;
		List<RemoteMethodArg>
			pathArgs = new LinkedList<>(),
			queryArgs = new LinkedList<>(),
			headerArgs = new LinkedList<>(),
			formDataArgs = new LinkedList<>(),
			otherArgs = new LinkedList<>();
		List<RemoteMethodBeanArg>
			requestArgs = new LinkedList<>();
		RemoteMethodArg bodyArg;
		RemoteMethodReturn methodReturn;

		Builder(String restUrl, Method m) {
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

			ReturnValue rv = m.getReturnType() == void.class ? ReturnValue.NONE : rm == null ? ReturnValue.BODY : rm.returns();

			methodReturn = new RemoteMethodReturn(m, rv);

			url =
				trimSlashes(restUrl)
				+ '/'
				+ (path != null ? trimSlashes(path) : urlEncode("NAME".equals(methodPaths) ? m.getName() : getMethodSignature(m)));

			for (int i = 0; i < m.getParameterTypes().length; i++) {
				RemoteMethodArg rma = RemoteMethodArg.create(m, i);
				boolean annotated = false;
				if (rma != null) {
					annotated = true;
					HttpPartType pt = rma.getPartType();
					if (pt == HEADER)
						headerArgs.add(rma);
					else if (pt == QUERY)
						queryArgs.add(rma);
					else if (pt == FORMDATA)
						formDataArgs.add(rma);
					else if (pt == PATH)
						pathArgs.add(rma);
					else if (pt == BODY)
						bodyArg = rma;
					else
						annotated = false;
				}
				RequestBeanMeta rmba = RequestBeanMeta.create(m, i, PropertyStore.DEFAULT);
				if (rmba != null) {
					annotated = true;
					requestArgs.add(new RemoteMethodBeanArg(i, null, rmba));
				}
				if (! annotated) {
					otherArgs.add(new RemoteMethodArg(i, BODY, null));
				}
			}
		}
	}

	/**
	 * Returns the value of the {@link RemoteMethod#httpMethod() @RemoteMethod.httpMethod()} annotation on this Java method.
	 *
	 * @return The value of the annotation, never <jk>null</jk>.
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
	 * Returns the {@link Request @Request} annotated arguments on this Java method.
	 *
	 * @return A list of zero-indexed argument indices.
	 */
	public RemoteMethodBeanArg[] getRequestArgs() {
		return requestArgs;
	}

	/**
	 * Returns the remaining non-annotated arguments on this Java method.
	 *
	 * @return A list of zero-indexed argument indices.
	 */
	public RemoteMethodArg[] getOtherArgs() {
		return otherArgs;
	}

	/**
	 * Returns the argument annotated with {@link Body @Body}.
	 *
	 * @return A index of the argument with the {@link Body @Body} annotation, or <jk>null</jk> if no argument exists.
	 */
	public RemoteMethodArg getBodyArg() {
		return bodyArg;
	}

	/**
	 * Returns whether the method returns the HTTP response body or status code.
	 *
	 * @return Whether the method returns the HTTP response body or status code.
	 */
	public RemoteMethodReturn getReturns() {
		return methodReturn;
	}
}
