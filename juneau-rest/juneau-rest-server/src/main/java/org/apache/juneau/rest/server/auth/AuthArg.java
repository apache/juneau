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
package org.apache.juneau.rest.server.auth;

import java.security.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.arg.*;

/**
 * Resolves {@code @RestOp}-method parameters that represent the authenticated {@link Principal}.
 *
 * <p>
 * Matches any of:
 * <ul>
 * 	<li>Parameter annotated with {@link Auth} (any type).
 * 	<li>Parameter typed as {@link Principal} or any subtype (including {@link ClaimsPrincipal}).
 * </ul>
 *
 * <p>
 * Resolution pulls the {@link Principal} stashed by an upstream AuthN guard
 * ({@link BearerTokenGuard} / {@link ApiKeyGuard}) on the request attributes under
 * {@link RestServerConstants#PRINCIPAL_ATTR}. If the stashed value is missing or not assignable to
 * the requested parameter type, <jk>null</jk> is injected &mdash; the guard chain is the contract
 * that guarantees a non-null value.
 *
 * <p>
 * Type-driven resolution lets a bare {@code Principal} parameter be filled without an annotation,
 * matching the Spring / Servlet convention; the {@link Auth} annotation is for clarity and for
 * disambiguating against another arg resolver that might otherwise claim the parameter.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Auth}
 * 	<li class='jc'>{@link BearerTokenGuard}
 * 	<li class='jc'>{@link ApiKeyGuard}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class AuthArg implements RestOpArg {

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link AuthArg}, or <jk>null</jk> if the parameter is not eligible.
	 */
	public static AuthArg create(ParameterInfo paramInfo) {
		var hasAuth = AP.has(Auth.class, paramInfo);
		var isPrincipal = paramInfo.getParameterType().isAssignableTo(Principal.class);
		if (hasAuth || isPrincipal)
			return new AuthArg(paramInfo);
		return null;
	}

	private final Class<?> type;

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 */
	protected AuthArg(ParameterInfo paramInfo) {
		this.type = paramInfo.getParameterType().inner();
	}

	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) {
		var v = opSession.getRequest().getAttribute(RestServerConstants.PRINCIPAL_ATTR).getValue();
		if (v == null || ! type.isInstance(v))
			return null;
		return v;
	}
}
