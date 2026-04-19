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
package org.apache.juneau.rest.rrpc;

import java.lang.reflect.*;

import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;

import jakarta.servlet.*;

/**
 * A specialized {@link RestOpContext} for handling <js>"RRPC"</js> HTTP methods.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestRpc">REST/RPC</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S2160" // equals() inherited from parent context; RrpcRestOpContext adds only runtime-state fields not relevant to identity
})
public class RrpcRestOpContext extends RestOpContext {

	private final RrpcInterfaceMeta meta;

	/**
	 * 2-arg positional context constructor.
	 *
	 * <p>
	 * Constructs an {@code RrpcRestOpContext} directly from a Java method and its owning {@link RestContext}, mirroring
	 * {@link RestOpContext#RestOpContext(Method, RestContext) RestOpContext}'s 2-arg ctor and replacing the legacy
	 * {@code RestOpContext.create(method, context).beanStore(context.getRootBeanStore()).type(RrpcRestOpContext.class).build()}
	 * fluent chain that {@link org.apache.juneau.rest.RestContext.Builder#createRestOperations(org.apache.juneau.rest.beanstore.BasicBeanStore, java.util.function.Supplier, RestContext) createRestOperations}
	 * used to call. Per TODO-16 Phase C-3 Route B, the public {@code RestOpContext.create(...)} factory was deleted; this
	 * ctor is the only supported entry point for instantiating an {@code RrpcRestOpContext}.
	 *
	 * <p>
	 * Internally builds a {@link RestOpContext.Builder} backed by {@link RestContext#getRootBeanStore()} (rather than the
	 * resource-scoped {@link RestContext#getBeanStore()} used by {@code RestOpContext}'s 2-arg ctor). The root bean store
	 * is preserved verbatim from the legacy fluent chain to avoid behavioral drift in RRPC builder-time bean creation.
	 *
	 * @param method The Java method this context represents. Must not be <jk>null</jk>.
	 * @param context The owning {@link RestContext}. Must not be <jk>null</jk>.
	 * @throws ServletException If context could not be created.
	 * @since 9.5.0
	 */
	public RrpcRestOpContext(Method method, RestContext context) throws ServletException {
		this(new RestOpContext.Builder(method, context).beanStore(context.getRootBeanStore()));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this method context.
	 * @throws ServletException Problem with metadata was detected.
	 */
	protected RrpcRestOpContext(RestOpContext.Builder builder) throws ServletException {
		super(builder);

		var interfaceClass = getBeanContext().getClassMeta(getJavaMethod().getGenericReturnType());
		meta = new RrpcInterfaceMeta(interfaceClass.inner(), null);
		if (meta.getMethodsByPath().isEmpty())
			throw new InternalServerError("Method {0} returns an interface {1} that doesn't define any remote methods.", getJavaMethod().getName(), interfaceClass.getNameFull());

	}

	@Override
	public RrpcRestOpSession.Builder createSession(RestSession session) {
		return RrpcRestOpSession.create(this, session);
	}

	/**
	 * Returns the metadata about the RRPC Java method.
	 *
	 * @return The metadata about the RRPC Java method.
	 */
	protected RrpcInterfaceMeta getMeta() { return meta; }
}