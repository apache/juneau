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

import org.apache.juneau.commons.inject.*;

/**
 * The env-driven {@code RestContext.*} default properties for a resource, resolved as a single bound object.
 *
 * <p>
 * Each property supplies the environment-level default for the corresponding {@link Rest @Rest} attribute (for
 * example {@link #getClientVersionHeader()} backs {@code @Rest(clientVersionHeader)}). A resource's
 * {@code @Rest} annotation values, when present, take precedence over these defaults; these properties only
 * establish the starting value before the annotation chain is applied.
 *
 * <p>
 * Values are resolved under the {@code RestContext} prefix from the standard {@link org.apache.juneau.commons.settings.Settings Settings}
 * source chain (thread-local override, global override, registered property sources, system properties, system
 * environment) as well as any per-resource {@link org.apache.juneau.commons.settings.PropertySource PropertySource}
 * beans registered in the resource's bean store &mdash; so a resource's {@code @Rest(config=...)} file can set a
 * {@code RestContext.*} key and have it take effect. Relaxed key matching is enabled, so a key may be spelled in
 * camelCase, dotted, or {@code SCREAMING_SNAKE_CASE} form.
 *
 * <p>
 * A key absent from every source leaves the corresponding property at its default value shown below. The bound
 * instance for a resource is retrievable via {@link RestContext#getRestContextProperties()} or from the resource
 * bean store via {@code beanStore().getBean(RestContextProperties.class)}.
 *
 * <p>
 * The {@code RestContext.uriAuthority} and {@code RestContext.uriContext} settings are intentionally not part of
 * this bean: they carry a null-versus-empty distinction that is represented separately on {@link RestContext}. The
 * {@code juneau.restLogger.level} setting is likewise separate, as it is owned by the call-logger subsystem rather
 * than {@code RestContext}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ConfigProperties">@ConfigProperties</a>
 * </ul>
 *
 * @since 10.0.0
 */
@ConfigProperties(prefix = "RestContext")
public class RestContextProperties {

	private static final String FALSE = "false";

	private String debugDefault = "";
	private String allowedHeaderParams = "Accept,Content-Type";
	private String allowedMethodHeaders = "";
	private String allowedMethodParams = "HEAD,OPTIONS";
	private String disableContentParam = FALSE;
	private String renderResponseStackTraces = FALSE;
	private String problemDetails = FALSE;
	private String virtualThreads = FALSE;
	private String responseTraceparent = "true";
	private String mdcAsyncPropagation = "true";
	private String eagerInit = FALSE;
	private String lazyChildren = FALSE;
	private String clientVersionHeader = "Client-Version";
	private String uriRelativity = "";
	private String uriResolution = "";

	/**
	 * The default {@code @Rest(debugDefault)} debug enablement value ({@code "ALWAYS"} / {@code "NEVER"} /
	 * {@code "CONDITIONAL"}); blank means unset.
	 *
	 * @return The value. Never <jk>null</jk>.
	 */
	public String getDebugDefault() {
		return debugDefault;
	}

	/**
	 * The default set of header names that may be passed via URL query parameter ({@code @Rest(allowedHeaderParams)}).
	 *
	 * @return The comma-delimited value. Never <jk>null</jk>.
	 */
	public String getAllowedHeaderParams() {
		return allowedHeaderParams;
	}

	/**
	 * The default set of HTTP method names that may be specified via a request header ({@code @Rest(allowedMethodHeaders)}).
	 *
	 * @return The comma-delimited value. Never <jk>null</jk>.
	 */
	public String getAllowedMethodHeaders() {
		return allowedMethodHeaders;
	}

	/**
	 * The default set of HTTP method names that may be specified via URL query parameter ({@code @Rest(allowedMethodParams)}).
	 *
	 * @return The comma-delimited value. Never <jk>null</jk>.
	 */
	public String getAllowedMethodParams() {
		return allowedMethodParams;
	}

	/**
	 * The raw (unresolved) {@code @Rest(disableContentParam)} default &mdash; whether the {@code &content=} URL
	 * parameter is disabled.
	 *
	 * <p>
	 * The value is stored verbatim &mdash; without SVL variable resolution or boolean conversion &mdash; because SVL
	 * resolution requires the resource's var resolver, which this bean does not carry. {@link RestContext} reads this
	 * raw value, resolves any SVL variables, and parses it to a boolean.
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getDisableContentParamRaw() {
		return disableContentParam;
	}

	/**
	 * The raw (unresolved) {@code @Rest(renderResponseStackTraces)} default &mdash; whether stack traces are rendered
	 * in error responses.
	 *
	 * <p>
	 * Stored verbatim; {@link RestContext} owns SVL resolution and boolean parsing (see {@link #getDisableContentParamRaw()}).
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getRenderResponseStackTracesRaw() {
		return renderResponseStackTraces;
	}

	/**
	 * The raw (unresolved) {@code @Rest(problemDetails)} default &mdash; whether RFC 7807
	 * {@code application/problem+json} responses are emitted.
	 *
	 * <p>
	 * Stored verbatim; {@link RestContext} owns SVL resolution and boolean parsing (see {@link #getDisableContentParamRaw()}).
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getProblemDetailsRaw() {
		return problemDetails;
	}

	/**
	 * The raw (unresolved) {@code @Rest(virtualThreads)} default &mdash; whether per-request virtual-thread dispatch is
	 * opted into on Java 21+.
	 *
	 * <p>
	 * Stored verbatim; {@link RestContext} owns SVL resolution and boolean parsing (see {@link #getDisableContentParamRaw()}).
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getVirtualThreadsRaw() {
		return virtualThreads;
	}

	/**
	 * The raw (unresolved) default controlling whether the server writes W3C {@code traceparent} / {@code tracestate}
	 * response headers when a tracer is active. Defaults to {@code "true"}.
	 *
	 * <p>
	 * Stored verbatim; {@link RestContext} owns SVL resolution and boolean parsing (see {@link #getDisableContentParamRaw()}).
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getResponseTraceparentRaw() {
		return responseTraceparent;
	}

	/**
	 * The raw (unresolved) default controlling whether the request thread's SLF4J MDC map is propagated to
	 * asynchronous completion threads. Defaults to {@code "true"}.
	 *
	 * <p>
	 * Stored verbatim; {@link RestContext} owns SVL resolution and boolean parsing (see {@link #getDisableContentParamRaw()}).
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getMdcAsyncPropagationRaw() {
		return mdcAsyncPropagation;
	}

	/**
	 * The raw (unresolved) {@code @Rest(eagerInit)} default &mdash; whether framework beans are force-initialized
	 * during construction.
	 *
	 * <p>
	 * Stored verbatim; {@link RestContext} owns SVL resolution and boolean parsing (see {@link #getDisableContentParamRaw()}).
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getEagerInitRaw() {
		return eagerInit;
	}

	/**
	 * The raw (unresolved) {@code @Rest(lazyChildren)} default &mdash; whether child-context construction is deferred.
	 *
	 * <p>
	 * Stored verbatim; {@link RestContext} owns SVL resolution and boolean parsing (see {@link #getDisableContentParamRaw()}).
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getLazyChildrenRaw() {
		return lazyChildren;
	}

	/**
	 * The default request header used for client-version matching ({@code @Rest(clientVersionHeader)}).
	 *
	 * @return The value. Never <jk>null</jk>.
	 */
	public String getClientVersionHeader() {
		return clientVersionHeader;
	}

	/**
	 * The raw (unresolved) {@code @Rest(uriRelativity)} default value. Blank means unset.
	 *
	 * <p>
	 * The value is stored verbatim &mdash; without SVL variable resolution or enum conversion &mdash; because SVL
	 * resolution requires the resource's var resolver, which this bean does not carry. {@link RestContext} reads
	 * this raw value, resolves any SVL variables, and applies lenient enum parsing.
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getUriRelativityRaw() {
		return uriRelativity;
	}

	/**
	 * The raw (unresolved) {@code @Rest(uriResolution)} default value. Blank means unset.
	 *
	 * <p>
	 * The value is stored verbatim &mdash; without SVL variable resolution or enum conversion &mdash; because SVL
	 * resolution requires the resource's var resolver, which this bean does not carry. {@link RestContext} reads
	 * this raw value, resolves any SVL variables, and applies lenient enum parsing.
	 *
	 * @return The raw value. Never <jk>null</jk>.
	 */
	public String getUriResolutionRaw() {
		return uriResolution;
	}
}
