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
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.server.arg.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.rest.server.openapi.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.swagger.*;

/**
 * Shared abstract base for the {@link RestBuilder} flavor builders ({@code RestServlet.Builder},
 * {@code RestResource.Builder}, {@code RestMixin.Builder}).
 *
 * <p>
 * Each fluent {@code @Rest}-member setter is implemented <b>once</b> here and forwards into a backing
 * {@code RestAnnotation.Builder} &mdash; the override bag.  At {@link RestContext} construction time the bag is
 * turned into a synthetic, highest-priority {@code @Rest} annotation that is prepended to the resource's
 * {@code @Rest} chain, so builder-supplied values <b>take precedence</b> over the class's own
 * {@link Rest @Rest} annotation values.
 *
 * <h5 class='section'>Self type (CRTP):</h5>
 *
 * <p>
 * {@code SELF} is the concrete builder type, left open through the flavor builders and their user subclasses so
 * that bespoke setters on a subclass chain with true covariant returns.  Setters return {@link #self()}.
 *
 * @param <R> The resource type produced by {@link #build()}.
 * @param <SELF> The concrete builder type (self type).
 * @since 10.0.0
 */
@SuppressWarnings({
	"unchecked", // CRTP self-type cast in self() is safe by construction.
	"java:S1452", // Wildcard return on getResourceType() is intentional.
	"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
})
public abstract class AbstractRestBuilder<R,SELF extends AbstractRestBuilder<R,SELF>> implements RestBuilder<SELF> {

	private final Class<R> resourceType;
	private final RestAnnotation.Builder anno = RestAnnotation.create();
	private final Map<String,Object> extras = m();
	private Boolean mdcAsyncPropagation;

	/**
	 * Constructor.
	 *
	 * @param resourceType The resource type produced by {@link #build()}.  Must not be <jk>null</jk>.
	 */
	protected AbstractRestBuilder(Class<R> resourceType) {
		this.resourceType = assertArgNotNull("resourceType", resourceType);
	}

	/**
	 * Returns this builder cast to the self type.
	 *
	 * @return This object.
	 */
	protected final SELF self() {
		return (SELF)this;
	}

	/**
	 * Builds the configured resource instance.
	 *
	 * @return A new resource instance configured by this builder.
	 */
	public abstract R build();

	/**
	 * Reflectively instantiates the resource type, preferring a {@code (RestBuilder<?>)} constructor (constructor
	 * injection) and falling back to the no-arg constructor.
	 *
	 * <p>
	 * Flavor builder {@code build()} implementations call this then stash {@code this} on the returned instance.
	 *
	 * @return A new, uninitialized resource instance.
	 */
	protected R createResource() {
		var ci = ClassInfo.of(resourceType);
		var ctor = ci.getDeclaredConstructor(x -> x.hasParameterTypes(RestBuilder.class)).orElse(null);
		if (ctor != null)
			return ctor.accessible().newInstance(this);
		var noArg = ci.getNoArgConstructor(Visibility.PRIVATE)
			.orElseThrow(() -> isex("Resource class %s has no no-arg or RestBuilder<?> constructor.", resourceType.getName()));
		return noArg.accessible().newInstance();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Override-bag accessors (consumed by RestContext during construction)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the resource type produced by this builder.
	 *
	 * @return The resource type.  Never <jk>null</jk>.
	 */
	public Class<?> getResourceType() {
		return resourceType;
	}

	/**
	 * Returns the synthetic {@code @Rest} annotation carrying all builder-set members, for use as the
	 * highest-priority contributor of the {@code @Rest} resolution chain.
	 *
	 * @return The synthetic annotation.  Never <jk>null</jk>.
	 */
	public Rest toRestAnnotation() {
		return anno.build();
	}

	/**
	 * Returns the programmatic MDC async-propagation override, or <jk>null</jk> if not set.
	 *
	 * @return The override, or <jk>null</jk> if not set.
	 */
	public Boolean getMdcAsyncPropagation() {
		return mdcAsyncPropagation;
	}

	/**
	 * Returns the forward-compat extras set via {@link #set(String, Object)}.
	 *
	 * @return An unmodifiable view of the extras map.  Never <jk>null</jk>.
	 */
	public Map<String,Object> getExtras() {
		return u(extras);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Identity & mounting
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RestBuilder<?> */
	public SELF path(String value) { anno.path(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF paths(String... value) { anno.paths(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF children(Class<?>... value) { anno.children(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF mixins(Class<?>... value) { anno.mixins(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF mixinDefs(Mixin... value) { anno.mixinDefs(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF uriAuthority(String value) { anno.uriAuthority(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF uriContext(String value) { anno.uriContext(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF uriRelativity(String value) { anno.uriRelativity(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF uriResolution(String value) { anno.uriResolution(value); return self(); }

	//-----------------------------------------------------------------------------------------------------------------
	// Marshalling
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RestBuilder<?> */
	public SELF serializers(Class<? extends Serializer>... value) { anno.serializers(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF parsers(Class<?>... value) { anno.parsers(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF encoders(Class<? extends Encoder>... value) { anno.encoders(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF partSerializer(Class<? extends HttpPartSerializer> value) { anno.partSerializer(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF partParser(Class<? extends HttpPartParser> value) { anno.partParser(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF consumes(String... value) { anno.consumes(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF produces(String... value) { anno.produces(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF responseProcessors(Class<? extends ResponseProcessor>... value) { anno.responseProcessors(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF allowedSerializerOptions(String... value) { anno.allowedSerializerOptions(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF allowedParserOptions(String... value) { anno.allowedParserOptions(value); return self(); }

	//-----------------------------------------------------------------------------------------------------------------
	// Request behavior
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RestBuilder<?> */
	public SELF allowedHeaderParams(String value) { anno.allowedHeaderParams(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF allowedMethodHeaders(String value) { anno.allowedMethodHeaders(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF allowedMethodParams(String value) { anno.allowedMethodParams(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF clientVersionHeader(String value) { anno.clientVersionHeader(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF defaultAccept(String value) { anno.defaultAccept(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF defaultContentType(String value) { anno.defaultContentType(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF defaultCharset(String value) { anno.defaultCharset(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF defaultRequestAttributes(String... value) { anno.defaultRequestAttributes(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF defaultRequestHeaders(String... value) { anno.defaultRequestHeaders(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF defaultResponseHeaders(String... value) { anno.defaultResponseHeaders(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF disableContentParam(String value) { anno.disableContentParam(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF maxInput(String value) { anno.maxInput(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF restOpArgs(Class<? extends RestOpArg>... value) { anno.restOpArgs(value); return self(); }

	//-----------------------------------------------------------------------------------------------------------------
	// Security
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RestBuilder<?> */
	public SELF guards(Class<? extends RestGuard>... value) { anno.guards(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF roleGuard(String value) { anno.roleGuard(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF rolesDeclared(String value) { anno.rolesDeclared(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF converters(Class<? extends RestConverter>... value) { anno.converters(value); return self(); }

	//-----------------------------------------------------------------------------------------------------------------
	// Lifecycle / perf
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RestBuilder<?> */
	public SELF eagerInit(String value) { anno.eagerInit(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF lazyChildren(String value) { anno.lazyChildren(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF virtualThreads(String value) { anno.virtualThreads(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF asyncTimeoutMillis(String value) { anno.asyncTimeoutMillis(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF asyncCompletionExecutor(String value) { anno.asyncCompletionExecutor(value); return self(); }

	//-----------------------------------------------------------------------------------------------------------------
	// Observability / logging / errors
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RestBuilder<?> */
	public SELF callLogger(Class<? extends CallLogger> value) { anno.callLogger(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF debug(String value) { anno.debug(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF observability(String value) { anno.observability(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF renderResponseStackTraces(String value) { anno.renderResponseStackTraces(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF problemDetails(String value) { anno.problemDetails(value); return self(); }

	//-----------------------------------------------------------------------------------------------------------------
	// Docs / metadata / i18n / static files
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RestBuilder<?> */
	public SELF title(String... value) { anno.title(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF description(String... value) { anno.description(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF siteName(String value) { anno.siteName(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF swaggerProvider(Class<? extends SwaggerProvider> value) { anno.swaggerProvider(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF openApiProvider(Class<? extends OpenApiProvider> value) { anno.openApiProvider(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF messages(String value) { anno.messages(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF config(String value) { anno.config(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF staticFiles(Class<? extends StaticFiles> value) { anno.staticFiles(value); return self(); }

	@Override /* RestBuilder<?> */
	public SELF noInherit(String... value) { anno.noInherit(value); return self(); }

	//-----------------------------------------------------------------------------------------------------------------
	// Programmatic-only knob & escape hatch
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RestBuilder<?> */
	public SELF mdcAsyncPropagation(boolean value) { mdcAsyncPropagation = value; return self(); }

	@Override /* RestBuilder<?> */
	public SELF set(String key, Object value) { extras.put(assertArgNotNull("key", key), value); return self(); }
}
