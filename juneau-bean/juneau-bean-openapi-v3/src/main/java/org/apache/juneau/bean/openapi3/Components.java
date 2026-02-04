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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.commons.collections.*;

/**
 * Holds a set of reusable objects for different aspects of the OpenAPI Specification.
 *
 * <p>
 * The Components Object holds a set of reusable objects that can be referenced from other parts of the API specification.
 * This promotes reusability and reduces duplication by allowing common schemas, responses, parameters, and other objects
 * to be defined once and referenced multiple times using the <c>$ref</c> syntax.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Components Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>schemas</c> (map of {@link SchemaInfo}) - Reusable schema definitions
 * 	<li><c>responses</c> (map of {@link Response}) - Reusable response definitions
 * 	<li><c>parameters</c> (map of {@link Parameter}) - Reusable parameter definitions
 * 	<li><c>examples</c> (map of {@link Example}) - Reusable example definitions
 * 	<li><c>requestBodies</c> (map of {@link RequestBodyInfo}) - Reusable request body definitions
 * 	<li><c>headers</c> (map of {@link HeaderInfo}) - Reusable header definitions
 * 	<li><c>securitySchemes</c> (map of {@link SecuritySchemeInfo}) - Reusable security scheme definitions
 * 	<li><c>links</c> (map of {@link Link}) - Reusable link definitions
 * 	<li><c>callbacks</c> (map of {@link Callback}) - Reusable callback definitions
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a Components object with reusable schemas</jc>
 * 	Components <jv>components</jv> = <jk>new</jk> Components()
 * 		.setSchemas(
 * 			JsonMap.<jsm>of</jsm>(
 * 				<js>"Pet"</js>, <jk>new</jk> SchemaInfo()
 * 					.setType(<js>"object"</js>)
 * 					.setRequired(<js>"id"</js>, <js>"name"</js>)
 * 					.setProperties(
 * 						JsonMap.<jsm>of</jsm>(
 * 							<js>"id"</js>, <jk>new</jk> SchemaInfo().setType(<js>"integer"</js>),
 * 							<js>"name"</js>, <jk>new</jk> SchemaInfo().setType(<js>"string"</js>)
 * 						)
 * 					),
 * 				<js>"Error"</js>, <jk>new</jk> SchemaInfo()
 * 					.setType(<js>"object"</js>)
 * 					.setProperties(
 * 						JsonMap.<jsm>of</jsm>(
 * 							<js>"code"</js>, <jk>new</jk> SchemaInfo().setType(<js>"integer"</js>),
 * 							<js>"message"</js>, <jk>new</jk> SchemaInfo().setType(<js>"string"</js>)
 * 						)
 * 					)
 * 			)
 * 		);
 * 	<jc>// These schemas can then be referenced: "#/components/schemas/Pet"</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#components-object">OpenAPI Specification &gt; Components Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/components/">OpenAPI Components</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class Components extends OpenApiElement {

	// Property name constants
	private static final String PROP_CALLBACKS = "callbacks";
	private static final String PROP_EXAMPLES = "examples";
	private static final String PROP_HEADERS = "headers";
	private static final String PROP_LINKS = "links";
	private static final String PROP_PARAMETERS = "parameters";
	private static final String PROP_REQUEST_BODIES = "requestBodies";
	private static final String PROP_RESPONSES = "responses";
	private static final String PROP_SCHEMAS = "schemas";
	private static final String PROP_SECURITY_SCHEMES = "securitySchemes";

	private Map<String,SchemaInfo> schemas;
	private Map<String,Response> responses;
	private Map<String,Parameter> parameters;
	private Map<String,Example> examples;
	private Map<String,RequestBodyInfo> requestBodies;
	private Map<String,HeaderInfo> headers;
	private Map<String,SecuritySchemeInfo> securitySchemes;
	private Map<String,Link> links;
	private Map<String,Callback> callbacks;

	/**
	 * Default constructor.
	 */
	public Components() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Components(Components copyFrom) {
		super(copyFrom);
		this.schemas = copyOf(copyFrom.schemas);
		this.responses = copyOf(copyFrom.responses);
		this.parameters = copyOf(copyFrom.parameters);
		this.examples = copyOf(copyFrom.examples);
		this.requestBodies = copyOf(copyFrom.requestBodies);
		this.headers = copyOf(copyFrom.headers);
		this.securitySchemes = copyOf(copyFrom.securitySchemes);
		this.links = copyOf(copyFrom.links);
		this.callbacks = copyOf(copyFrom.callbacks);
	}

	/**
	 * Creates a copy of this object.
	 *
	 * @return A copy of this object.
	 */
	public Components copy() {
		return new Components(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case PROP_SCHEMAS -> toType(getSchemas(), type);
			case PROP_RESPONSES -> toType(getResponses(), type);
			case PROP_PARAMETERS -> toType(getParameters(), type);
			case PROP_EXAMPLES -> toType(getExamples(), type);
			case PROP_REQUEST_BODIES -> toType(getRequestBodies(), type);
			case PROP_HEADERS -> toType(getHeaders(), type);
			case PROP_SECURITY_SCHEMES -> toType(getSecuritySchemes(), type);
			case PROP_LINKS -> toType(getLinks(), type);
			case PROP_CALLBACKS -> toType(getCallbacks(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Returns the callbacks map.
	 *
	 * @return The callbacks map.
	 */
	public Map<String,Callback> getCallbacks() { return callbacks; }

	/**
	 * Returns the examples map.
	 *
	 * @return The examples map.
	 */
	public Map<String,Example> getExamples() { return examples; }

	/**
	 * Returns the headers map.
	 *
	 * @return The headers map.
	 */
	public Map<String,HeaderInfo> getHeaders() { return headers; }

	/**
	 * Returns the links map.
	 *
	 * @return The links map.
	 */
	public Map<String,Link> getLinks() { return links; }

	/**
	 * Returns the parameters map.
	 *
	 * @return The parameters map.
	 */
	public Map<String,Parameter> getParameters() { return parameters; }

	/**
	 * Returns the request bodies map.
	 *
	 * @return The request bodies map.
	 */
	public Map<String,RequestBodyInfo> getRequestBodies() { return requestBodies; }

	/**
	 * Returns the responses map.
	 *
	 * @return The responses map.
	 */
	public Map<String,Response> getResponses() { return responses; }

	/**
	 * Returns the schemas map.
	 *
	 * @return The schemas map.
	 */
	public Map<String,SchemaInfo> getSchemas() { return schemas; }

	/**
	 * Returns the security schemes map.
	 *
	 * @return The security schemes map.
	 */
	public Map<String,SecuritySchemeInfo> getSecuritySchemes() { return securitySchemes; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(callbacks), PROP_CALLBACKS)
			.addIf(nn(examples), PROP_EXAMPLES)
			.addIf(nn(headers), PROP_HEADERS)
			.addIf(nn(links), PROP_LINKS)
			.addIf(nn(parameters), PROP_PARAMETERS)
			.addIf(nn(requestBodies), PROP_REQUEST_BODIES)
			.addIf(nn(responses), PROP_RESPONSES)
			.addIf(nn(schemas), PROP_SCHEMAS)
			.addIf(nn(securitySchemes), PROP_SECURITY_SCHEMES)
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Components set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case PROP_CALLBACKS -> setCallbacks(toMapBuilder(value, String.class, Callback.class).sparse().build());
			case PROP_EXAMPLES -> setExamples(toMapBuilder(value, String.class, Example.class).sparse().build());
			case PROP_HEADERS -> setHeaders(toMapBuilder(value, String.class, HeaderInfo.class).sparse().build());
			case PROP_LINKS -> setLinks(toMapBuilder(value, String.class, Link.class).sparse().build());
			case PROP_PARAMETERS -> setParameters(toMapBuilder(value, String.class, Parameter.class).sparse().build());
			case PROP_REQUEST_BODIES -> setRequestBodies(toMapBuilder(value, String.class, RequestBodyInfo.class).sparse().build());
			case PROP_RESPONSES -> setResponses(toMapBuilder(value, String.class, Response.class).sparse().build());
			case PROP_SCHEMAS -> setSchemas(toMapBuilder(value, String.class, SchemaInfo.class).sparse().build());
			case PROP_SECURITY_SCHEMES -> setSecuritySchemes(toMapBuilder(value, String.class, SecuritySchemeInfo.class).sparse().build());
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Sets the callbacks map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setCallbacks(Map<String,Callback> value) {
		callbacks = value;
		return this;
	}

	/**
	 * Sets the examples map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setExamples(Map<String,Example> value) {
		examples = value;
		return this;
	}

	/**
	 * Sets the headers map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setHeaders(Map<String,HeaderInfo> value) {
		headers = value;
		return this;
	}

	/**
	 * Sets the links map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setLinks(Map<String,Link> value) {
		links = value;
		return this;
	}

	/**
	 * Sets the parameters map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setParameters(Map<String,Parameter> value) {
		parameters = value;
		return this;
	}

	/**
	 * Sets the request bodies map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setRequestBodies(Map<String,RequestBodyInfo> value) {
		requestBodies = value;
		return this;
	}

	/**
	 * Sets the responses map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setResponses(Map<String,Response> value) {
		responses = value;
		return this;
	}

	/**
	 * Sets the schemas map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setSchemas(Map<String,SchemaInfo> value) {
		schemas = value;
		return this;
	}

	/**
	 * Sets the security schemes map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setSecuritySchemes(Map<String,SecuritySchemeInfo> value) {
		securitySchemes = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Components strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Components strict(Object value) {
		super.strict(value);
		return this;
	}
}