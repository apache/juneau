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

/**
 * Static literals for {@code juneau-rest-server}: annotation attribute name constants used for
 * allowlist inheritance checks and other module-internal logic.
 *
 * <p>
 * HTTP wire names shared with clients belong on {@link RestSharedConstants} instead.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RestSharedConstants}
 * </ul>
 */
@SuppressWarnings({
	"java:S115", // PROPERTY_ + camelCase property name mirrors annotation attribute name; not strict UPPER_SNAKE_CASE
})
public final class RestServerConstants {

	private RestServerConstants() {}

	/**
	 * The {@code "allowedParserOptions"} annotation attribute name — used in {@code noInherit} matching.
	 *
	 * @see org.apache.juneau.rest.annotation.Rest#allowedParserOptions()
	 */
	public static final String PROPERTY_allowedParserOptions = "allowedParserOptions";

	/**
	 * The {@code "allowedSerializerOptions"} annotation attribute name — used in {@code noInherit} matching.
	 *
	 * @see org.apache.juneau.rest.annotation.Rest#allowedSerializerOptions()
	 */
	public static final String PROPERTY_allowedSerializerOptions = "allowedSerializerOptions";

	/** The {@code "allowedHeaderParams"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_allowedHeaderParams = "allowedHeaderParams";

	/** The {@code "allowedMethodHeaders"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_allowedMethodHeaders = "allowedMethodHeaders";

	/** The {@code "allowedMethodParams"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_allowedMethodParams = "allowedMethodParams";

	/** The {@code "disableContentParam"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_disableContentParam = "disableContentParam";

	/** The {@code "renderResponseStackTraces"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_renderResponseStackTraces = "renderResponseStackTraces";

	/** The {@code "clientVersionHeader"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_clientVersionHeader = "clientVersionHeader";

	/** The {@code "uriAuthority"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_uriAuthority = "uriAuthority";

	/** The {@code "uriContext"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_uriContext = "uriContext";

	/** The {@code "uriRelativity"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_uriRelativity = "uriRelativity";

	/** The {@code "uriResolution"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_uriResolution = "uriResolution";

	/** The {@code "noInherit"} annotation attribute name. */
	public static final String PROPERTY_noInherit = "noInherit";

	/** The {@code "defaultCharset"} annotation attribute name — used in {@code noInherit} matching on both {@code @RestOp} / verb annotations (op-level {@code noInherit} blocks the {@code @Rest(defaultCharset)} fallback) and {@code @Rest} annotations (class-level {@code noInherit} cuts off the resource-class hierarchy walk). */
	public static final String PROPERTY_defaultCharset = "defaultCharset";

	/** The {@code "maxInput"} annotation attribute name — used in {@code noInherit} matching on both {@code @RestOp} / verb annotations (op-level {@code noInherit} blocks the {@code @Rest(maxInput)} fallback) and {@code @Rest} annotations (class-level {@code noInherit} cuts off the resource-class hierarchy walk). */
	public static final String PROPERTY_maxInput = "maxInput";

	/** The {@code "messages"} annotation attribute name — used in {@code noInherit} matching on {@code @Rest} annotations to cut off the resource-class hierarchy walk when resolving message bundle locations. */
	public static final String PROPERTY_messages = "messages";

	/** The {@code "callLogger"} annotation attribute name — used in {@code noInherit} matching on {@code @Rest} annotations to cut off the resource-class hierarchy walk when resolving the call logger. */
	public static final String PROPERTY_callLogger = "callLogger";

	/** The {@code "debugEnablement"} annotation attribute name — used in {@code noInherit} matching on {@code @Rest} annotations to cut off the resource-class hierarchy walk when resolving the debug enablement bean. */
	public static final String PROPERTY_debugEnablement = "debugEnablement";

	/** Property name for the {@code debugDefault} setting. */
	public static final String PROPERTY_debugDefault = "debugDefault";

	/** The {@code "staticFiles"} annotation attribute name — used in {@code noInherit} matching on {@code @Rest} annotations to cut off the resource-class hierarchy walk when resolving the static files bean. */
	public static final String PROPERTY_staticFiles = "staticFiles";

	/** The {@code "swaggerProvider"} annotation attribute name — used in {@code noInherit} matching on {@code @Rest} annotations to cut off the resource-class hierarchy walk when resolving the swagger provider bean. */
	public static final String PROPERTY_swaggerProvider = "swaggerProvider";

	/** The {@code "debug"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_debug = "debug";

	/** The {@code "consumes"} annotation attribute name — used in {@code noInherit} matching to block class-level {@code @Rest(consumes)} from contributing to an op, and as the property name for the {@code @Rest(consumes)} chain walk in {@link RestContext#getConsumes()}. */
	public static final String PROPERTY_consumes = "consumes";

	/** The {@code "produces"} annotation attribute name — used in {@code noInherit} matching to block class-level {@code @Rest(produces)} from contributing to an op, and as the property name for the {@code @Rest(produces)} chain walk in {@link RestContext#getProduces()}. */
	public static final String PROPERTY_produces = "produces";

	/** The {@code "responseProcessors"} annotation attribute name — used in {@code noInherit} matching on {@code @Rest} annotations to cut off the resource-class hierarchy walk when resolving the response-processor list. */
	public static final String PROPERTY_responseProcessors = "responseProcessors";

	/** The {@code "restOpArgs"} annotation attribute name — used in {@code noInherit} matching on {@code @Rest} annotations to cut off the resource-class hierarchy walk when resolving the per-operation argument resolver list. */
	public static final String PROPERTY_restOpArgs = "restOpArgs";

	/** The {@code "httpMethod"} / {@code "method"} surface — reserved for {@code noInherit} matching; op HTTP method has no {@link RestContext} scalar fallback. */
	public static final String PROPERTY_httpMethod = "httpMethod";

	/** The {@code "converters"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations to block class-level {@code @Rest(converters)} from contributing to an op. */
	public static final String PROPERTY_converters = "converters";

	/** The {@code "guards"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations to block class-level {@code @Rest(guards)} (and {@code roleGuard} / {@code rolesDeclared}) from contributing to an op. */
	public static final String PROPERTY_guards = "guards";

	/** The {@code "matchers"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_matchers = "matchers";

	/** The {@code "clientVersion"} annotation attribute name — folded into {@link #PROPERTY_matchers} for op-level matchers (drives {@code ClientVersionMatcher} injection). */
	public static final String PROPERTY_clientVersion = "clientVersion";

	/** The {@code "roleGuard"} annotation attribute name — folded into {@link #PROPERTY_guards} for op-level guards. */
	public static final String PROPERTY_roleGuard = "roleGuard";

	/** The {@code "rolesDeclared"} annotation attribute name — folded into {@link #PROPERTY_guards} for op-level guards (drives {@code RoleBasedRestGuard} role-name set). */
	public static final String PROPERTY_rolesDeclared = "rolesDeclared";

	/** The {@code "serializers"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_serializers = "serializers";

	/** The {@code "parsers"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_parsers = "parsers";

	/** The {@code "encoders"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_encoders = "encoders";

	/** The {@code "defaultRequestHeaders"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_defaultRequestHeaders = "defaultRequestHeaders";

	/** The {@code "defaultResponseHeaders"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_defaultResponseHeaders = "defaultResponseHeaders";

	/** The {@code "defaultRequestAttributes"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_defaultRequestAttributes = "defaultRequestAttributes";

	/** The {@code "defaultRequestQueryData"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_defaultRequestQueryData = "defaultRequestQueryData";

	/** The {@code "defaultRequestFormData"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_defaultRequestFormData = "defaultRequestFormData";

	/** The {@code "defaultAccept"} annotation attribute name — folded into {@link #PROPERTY_defaultRequestHeaders}. */
	public static final String PROPERTY_defaultAccept = "defaultAccept";

	/** The {@code "defaultContentType"} annotation attribute name — folded into {@link #PROPERTY_defaultRequestHeaders}. */
	public static final String PROPERTY_defaultContentType = "defaultContentType";

	/** The {@code "partSerializer"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_partSerializer = "partSerializer";

	/** The {@code "partParser"} annotation attribute name — used in {@code noInherit} matching. */
	public static final String PROPERTY_partParser = "partParser";

	/** The {@code "path"} annotation attribute name — used in {@code noInherit} matching on {@code @RestOp} / verb annotations. */
	public static final String PROPERTY_path = "path";

	/** The {@code "value"} annotation attribute name — used by {@code @RestOp}/verb annotations to hold the (optional method-prefixed) path; folded into {@link #PROPERTY_path}. */
	public static final String PROPERTY_value = "value";
}
