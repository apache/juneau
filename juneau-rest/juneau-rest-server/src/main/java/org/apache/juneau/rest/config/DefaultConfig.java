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
package org.apache.juneau.rest.config;

import org.apache.juneau.annotation.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.oapi.*;

/**
 * Predefined REST configuration that defines common default values for all configurations.
 */
@Rest(
	// Configuration file.
	config="$S{j.configFile,$E{J_CONFIG_FILE,SYSTEM_DEFAULT}}",

	// Standard fields.
	path="",
	roleGuard="",
	rolesDeclared="",

	// Configuration beans.
	converters={},
	encoders={IdentityEncoder.class},
	guards={},
	parsers={},
	partParser=OpenApiParser.class,
	partSerializer=OpenApiSerializer.class,
	responseProcessors={
		ReaderProcessor.class,
		InputStreamProcessor.class,
		ThrowableProcessor.class,
		HttpResponseProcessor.class,
		HttpResourceProcessor.class,
		HttpEntityProcessor.class,
		ResponseBeanProcessor.class,
		PlainTextPojoProcessor.class,
		SerializedPojoProcessor.class
	},
	restOpArgs={
		AttributeArg.class,
		ContentArg.class,
		FormDataArg.class,
		HasFormDataArg.class,
		HasQueryArg.class,
		HeaderArg.class,
		HttpServletRequestArgs.class,
		HttpServletResponseArgs.class,
		HttpSessionArgs.class,
		InputStreamParserArg.class,
		MethodArg.class,
		ParserArg.class,
		PathArg.class,
		QueryArg.class,
		ReaderParserArg.class,
		RequestBeanArg.class,
		ResponseBeanArg.class,
		ResponseHeaderArg.class,
		ResponseCodeArg.class,
		RestContextArgs.class,
		RestSessionArgs.class,
		RestOpContextArgs.class,
		RestOpSessionArgs.class,
		RestRequestArgs.class,
		RestResponseArgs.class,
		DefaultArg.class
	},
	serializers={},

	// Configurable settings.
	allowedHeaderParams="$S{j.allowedHeaderParams,$E{J_ALLOWED_HEADER_PARAMS,Accept,Content-Type}}",
	allowedMethodHeaders="$S{j.allowedMethodHeaders,$E{J_ALLOWED_METHOD_HEADERS,}}",
	allowedMethodParams="$S{j.allowedMethodParams,$E{J_ALLOWED_METHOD_PARAMS,HEAD,OPTIONS}}",
	clientVersionHeader="$S{j.clientVersionHeader,$E{J_CLIENT_VERSION_HEADER,Client-Version}}",
	debug="$S{j.debug,$E{J_DEBUG,}}",
	debugOn="$S{j.debugOn,$E{J_DEBUG_ON,}}",
	defaultAccept="$S{j.defaultAccept,$E{J_DEFAULT_ACCEPT,}}",
	defaultCharset="$S{j.defaultCharset,$E{J_DEFAULT_CHARSET,UTF-8}}",
	defaultContentType="$S{j.defaultContentType,$E{J_DEFAULT_CONTENT_TYPE,}}",
	defaultRequestAttributes="$S{j.defaultRequestAttributes,$E{J_DEFAULT_REQUEST_ATTRIBUTES,}}",
	defaultRequestHeaders="$S{j.defaultRequestHeaders,$E{J_DEFAULT_REQUEST_HEADERS,}}",
	defaultResponseHeaders="$S{j.defaultResponseHeaders,$E{J_DEFAULT_RESPONSE_HEADERS,}}",
	disableContentParam="$S{j.disableContentParam,$E{J_DISABLE_CONTENT_PARAM,false}}",
	maxInput="$S{j.maxInput,$E{J_MAX_INPUT,1000000}}",
	messages="$S{j.messages,$E{J_MESSAGES,}}",
	renderResponseStackTraces="$S{j.renderResponseStackTraces,$E{J_RENDER_RESPONSE_STACK_TRACES,false}}",
	uriAuthority="$S{j.uriAuthority,$E{J_URI_AUTHORITY,}}",
	uriContext="$S{j.uriContext,$E{J_URI_CONTEXT,}}",
	uriRelativity="$S{j.uriRelativity,$E{J_URI_RELATIVITY,}}",
	uriResolution="$S{j.uriResolution,$E{J_URI_RESOLUTION,}}",

	// Metadata settings.
	consumes={},
	description="",
	produces={},
	siteName="$S{j.siteName,$E{J_SITE_NAME,}}",
	swagger=@Swagger,
	title="$S{j.title,$E{J_TITLE,}}",

	// Injectable/overridable beans.
	beanStore=BeanStore.Void.class,  // Defaults to BeanStore.
	callLogger=CallLogger.Void.class,  // Defaults to BasicCallLogger.
	debugEnablement=DebugEnablement.Void.class,  // Defaults to BasicDefaultEnablement.
	staticFiles=StaticFiles.Void.class,  // Defaults to BasicStaticFiles.
	swaggerProvider=SwaggerProvider.Void.class,  // Defaults to BasicSwaggerProvider.

	// Overridable context classes.
	restChildrenClass=RestChildren.class,
	restOperationsClass=RestOperations.class
)
@BeanConfig(
	// When parsing generated beans, ignore unknown properties that may only exist as getters and not setters.
	ignoreUnknownBeanProperties="true",
	ignoreUnknownEnumValues="true"
)
@SerializerConfig(
	// Enable automatic resolution of URI objects to root-relative values.
	uriResolution="ROOT_RELATIVE"
)
public interface DefaultConfig {}
