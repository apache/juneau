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
package org.apache.juneau.rest.server.servlet;

import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.convention.*;
import org.apache.juneau.rest.server.docs.*;
import org.apache.juneau.rest.server.ops.*;

import jakarta.servlet.http.*;

/**
 * Identical to {@link BasicRestServlet} but doesn't extend from {@link HttpServlet}.
 *
 * <p>
 * Meant as a base class for child REST resources in servlet containers and Spring Boot environments.
 *
 * <p>
 * Provides support for JSON, XML, HTML, URL-Encoding, UON, XML, OpenAPI, and MessagePack.  See {@link BasicUniversalConfig}
 * for details.
 *
 * <p>
 * The residual cross-cutting endpoints are supplied by single-responsibility op-mixins ({@link ErrorMixin},
 * {@link HtdocMixin}, {@link StatsMixin}, and {@link FaviconMixin}), and the api-docs surface by the
 * api-docs mixin pack ({@link SwaggerUiMixin} and {@link RedocMixin}, which transitively pull in
 * {@link SwaggerMixin} and {@link OpenApiMixin}). Resulting endpoints:
 * {@code /api}, {@code /swagger}, {@code /openapi}, {@code /openapi.json}, {@code /openapi.yaml},
 * {@code /redoc}, {@code /htdocs/*}, {@code /favicon.ico}, {@code /stats}, {@code /error}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
// @formatter:off
@Rest(mixins={SwaggerUiMixin.class, RedocMixin.class, ErrorMixin.class, HtdocMixin.class, StatsMixin.class, FaviconMixin.class})
@HtmlDocConfig(
	// Basic page navigation links.
	navlinks={
		"up: request:/..",
		"api: servlet:/api",
		"stats: servlet:/stats"
	}
)
@JsonSchemaConfig(
	// Add descriptions to the following types when not specified:
	addDescriptionsTo="bean,collection,array,map,enum",
	// Add example to the following types:
	addExamplesTo="bean,collection,array,map",
	// Don't generate schema information on the Swagger / OpenApi beans themselves or HTML beans.
	ignoreTypes="Swagger,OpenApi,org.apache.juneau.bean.html5.*",
	// Use $ref references for bean definitions to reduce duplication in generated specs.
	useBeanDefs="true"
)
// @formatter:on
public abstract class BasicRestResource extends RestResource implements BasicUniversalConfig {}
