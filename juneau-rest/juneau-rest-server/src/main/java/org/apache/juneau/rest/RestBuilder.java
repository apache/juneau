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
package org.apache.juneau.rest;

import org.apache.juneau.encoders.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.openapi.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.serializer.*;

/**
 * Public, fluent, self-typed configuration surface for programmatically configuring a {@code @Rest} resource,
 * child resource, or mixin instead of (or in addition to) the {@link Rest @Rest} annotation.
 *
 * <p>
 * This is the user-facing builder surface introduced for programmatic resource/mixin configuration (e.g.
 * instantiating a configured resource bean in a Spring {@code @Bean} method).  Builder-supplied values take
 * <b>precedence</b> over {@link Rest @Rest} annotation values &mdash; they slot in as the highest-priority
 * (rung&nbsp;1) contributor of the runtime-override resolution chain documented on
 * {@link RestContext#getPaths()}, generalized to every {@code @Rest} member.
 *
 * <p class='bjava'>
 * 	<jc>// Configure a resource programmatically; builder values win over the class's @Rest annotation.</jc>
 * 	MyRest <jv>r</jv> = MyRest.<jsm>builder</jsm>().path(<js>"/foo"</js>).allowedHeaderParams(<js>"foo"</js>).build();
 * </p>
 *
 * <h5 class='section'>Self type (CRTP):</h5>
 *
 * <p>
 * The {@code SELF} type parameter is the concrete builder type, so every fluent setter returns the most-derived
 * builder type and bespoke setters added by a builder subclass chain with true covariant returns.  This mirrors
 * the project-wide {@code SELF}/{@code self()} self-type convention.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AbstractRestBuilder}
 * 	<li class='jc'>{@link org.apache.juneau.rest.servlet.RestServlet.Builder}
 * 	<li class='jc'>{@link org.apache.juneau.rest.servlet.RestResource.Builder}
 * 	<li class='jc'>{@link org.apache.juneau.rest.servlet.RestMixin.Builder}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 *
 * @param <SELF> The concrete builder type (self type).
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
})
public interface RestBuilder<SELF extends RestBuilder<SELF>> {

	//-----------------------------------------------------------------------------------------------------------------
	// Identity & mounting
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the {@link Rest#path() path} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF path(String value);

	/**
	 * Specifies the {@link Rest#paths() top-level mount paths} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF paths(String... value);

	/**
	 * Specifies the {@link Rest#children() child resources} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF children(Class<?>... value);

	/**
	 * Specifies the {@link Rest#mixins() mixins} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF mixins(Class<?>... value);

	/**
	 * Specifies the {@link Rest#uriAuthority() uriAuthority} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF uriAuthority(String value);

	/**
	 * Specifies the {@link Rest#uriContext() uriContext} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF uriContext(String value);

	/**
	 * Specifies the {@link Rest#uriRelativity() uriRelativity} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF uriRelativity(String value);

	/**
	 * Specifies the {@link Rest#uriResolution() uriResolution} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF uriResolution(String value);

	//-----------------------------------------------------------------------------------------------------------------
	// Marshalling
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the {@link Rest#serializers() serializers} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked" // Generic varargs parameter; no heap pollution from this declaration.
	})
	SELF serializers(Class<? extends Serializer>... value);

	/**
	 * Specifies the {@link Rest#parsers() parsers} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF parsers(Class<?>... value);

	/**
	 * Specifies the {@link Rest#encoders() encoders} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked" // Generic varargs parameter; no heap pollution from this declaration.
	})
	SELF encoders(Class<? extends Encoder>... value);

	/**
	 * Specifies the {@link Rest#partSerializer() partSerializer} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF partSerializer(Class<? extends HttpPartSerializer> value);

	/**
	 * Specifies the {@link Rest#partParser() partParser} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF partParser(Class<? extends HttpPartParser> value);

	/**
	 * Specifies the {@link Rest#consumes() consumes} media types for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF consumes(String... value);

	/**
	 * Specifies the {@link Rest#produces() produces} media types for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF produces(String... value);

	/**
	 * Specifies the {@link Rest#responseProcessors() responseProcessors} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked" // Generic varargs parameter; no heap pollution from this declaration.
	})
	SELF responseProcessors(Class<? extends ResponseProcessor>... value);

	/**
	 * Specifies the {@link Rest#allowedSerializerOptions() allowedSerializerOptions} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF allowedSerializerOptions(String... value);

	/**
	 * Specifies the {@link Rest#allowedParserOptions() allowedParserOptions} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF allowedParserOptions(String... value);

	//-----------------------------------------------------------------------------------------------------------------
	// Request behavior / params
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the {@link Rest#allowedHeaderParams() allowedHeaderParams} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF allowedHeaderParams(String value);

	/**
	 * Specifies the {@link Rest#allowedMethodHeaders() allowedMethodHeaders} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF allowedMethodHeaders(String value);

	/**
	 * Specifies the {@link Rest#allowedMethodParams() allowedMethodParams} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF allowedMethodParams(String value);

	/**
	 * Specifies the {@link Rest#clientVersionHeader() clientVersionHeader} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF clientVersionHeader(String value);

	/**
	 * Specifies the {@link Rest#defaultAccept() defaultAccept} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF defaultAccept(String value);

	/**
	 * Specifies the {@link Rest#defaultContentType() defaultContentType} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF defaultContentType(String value);

	/**
	 * Specifies the {@link Rest#defaultCharset() defaultCharset} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF defaultCharset(String value);

	/**
	 * Specifies the {@link Rest#defaultRequestAttributes() defaultRequestAttributes} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF defaultRequestAttributes(String... value);

	/**
	 * Specifies the {@link Rest#defaultRequestHeaders() defaultRequestHeaders} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF defaultRequestHeaders(String... value);

	/**
	 * Specifies the {@link Rest#defaultResponseHeaders() defaultResponseHeaders} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF defaultResponseHeaders(String... value);

	/**
	 * Specifies the {@link Rest#disableContentParam() disableContentParam} flag for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF disableContentParam(String value);

	/**
	 * Specifies the {@link Rest#maxInput() maxInput} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF maxInput(String value);

	/**
	 * Specifies the {@link Rest#restOpArgs() restOpArgs} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked" // Generic varargs parameter; no heap pollution from this declaration.
	})
	SELF restOpArgs(Class<? extends RestOpArg>... value);

	//-----------------------------------------------------------------------------------------------------------------
	// Security
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the {@link Rest#guards() guards} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked" // Generic varargs parameter; no heap pollution from this declaration.
	})
	SELF guards(Class<? extends RestGuard>... value);

	/**
	 * Specifies the {@link Rest#roleGuard() roleGuard} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF roleGuard(String value);

	/**
	 * Specifies the {@link Rest#rolesDeclared() rolesDeclared} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF rolesDeclared(String value);

	/**
	 * Specifies the {@link Rest#converters() converters} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked" // Generic varargs parameter; no heap pollution from this declaration.
	})
	SELF converters(Class<? extends RestConverter>... value);

	//-----------------------------------------------------------------------------------------------------------------
	// Lifecycle / perf
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the {@link Rest#eagerInit() eagerInit} flag for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF eagerInit(String value);

	/**
	 * Specifies the {@link Rest#lazyChildren() lazyChildren} flag for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF lazyChildren(String value);

	/**
	 * Specifies the {@link Rest#virtualThreads() virtualThreads} flag for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF virtualThreads(String value);

	/**
	 * Specifies the {@link Rest#asyncTimeoutMillis() asyncTimeoutMillis} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF asyncTimeoutMillis(String value);

	/**
	 * Specifies the {@link Rest#asyncCompletionExecutor() asyncCompletionExecutor} bean name for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF asyncCompletionExecutor(String value);

	//-----------------------------------------------------------------------------------------------------------------
	// Observability / logging / errors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the {@link Rest#callLogger() callLogger} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF callLogger(Class<? extends CallLogger> value);

	/**
	 * Specifies the {@link Rest#debug() debug} mode for this resource.
	 *
	 * @param value The new value for this property (e.g. {@code "true"}, {@code "conditional"}).
	 * @return This object.
	 */
	SELF debug(String value);

	/**
	 * Specifies the {@link Rest#observability() observability} flag for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF observability(String value);

	/**
	 * Specifies the {@link Rest#renderResponseStackTraces() renderResponseStackTraces} flag for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF renderResponseStackTraces(String value);

	/**
	 * Specifies the {@link Rest#problemDetails() problemDetails} flag for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF problemDetails(String value);

	//-----------------------------------------------------------------------------------------------------------------
	// Docs / metadata / i18n / static files
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the {@link Rest#title() title} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF title(String... value);

	/**
	 * Specifies the {@link Rest#description() description} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF description(String... value);

	/**
	 * Specifies the {@link Rest#siteName() siteName} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF siteName(String value);

	/**
	 * Specifies the {@link Rest#swaggerProvider() swaggerProvider} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF swaggerProvider(Class<? extends SwaggerProvider> value);

	/**
	 * Specifies the {@link Rest#openApiProvider() openApiProvider} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF openApiProvider(Class<? extends OpenApiProvider> value);

	/**
	 * Specifies the {@link Rest#messages() messages} bundle location for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF messages(String value);

	/**
	 * Specifies the {@link Rest#config() config} location for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF config(String value);

	/**
	 * Specifies the {@link Rest#staticFiles() staticFiles} for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF staticFiles(Class<? extends StaticFiles> value);

	/**
	 * Specifies the {@link Rest#noInherit() noInherit} property names for this resource.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF noInherit(String... value);

	//-----------------------------------------------------------------------------------------------------------------
	// Programmatic-only knob (no @Rest member)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies whether MDC async propagation is enabled for this resource.
	 *
	 * <p>
	 * This is a <b>programmatic-only</b> knob &mdash; it has no {@link Rest @Rest} annotation member (its default is
	 * env-driven).  Setting it here overrides the {@code RestContext.mdcAsyncPropagation} env-driven default.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	SELF mdcAsyncPropagation(boolean value);

	//-----------------------------------------------------------------------------------------------------------------
	// Forward-compat escape hatch
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Generic forward-compatible override setter, keyed by the {@code @Rest} property name (e.g.
	 * {@code "allowContentParam"}, {@code "defaultRequestHeaders"}).
	 *
	 * <p>
	 * Use this escape hatch for members not yet exposed as a dedicated fluent method.  Keys reuse the existing
	 * {@code @Rest} member names.
	 *
	 * @param key The {@code @Rest} property name.
	 * @param value The override value.
	 * @return This object.
	 */
	SELF set(String key, Object value);
}
