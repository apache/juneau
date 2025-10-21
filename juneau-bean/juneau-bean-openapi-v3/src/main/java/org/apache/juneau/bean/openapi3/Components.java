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

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.utils.*;

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
		this.schemas = CollectionUtils.copyOf(copyFrom.schemas);
		this.responses = CollectionUtils.copyOf(copyFrom.responses);
		this.parameters = CollectionUtils.copyOf(copyFrom.parameters);
		this.examples = CollectionUtils.copyOf(copyFrom.examples);
		this.requestBodies = CollectionUtils.copyOf(copyFrom.requestBodies);
		this.headers = CollectionUtils.copyOf(copyFrom.headers);
		this.securitySchemes = CollectionUtils.copyOf(copyFrom.securitySchemes);
		this.links = CollectionUtils.copyOf(copyFrom.links);
		this.callbacks = CollectionUtils.copyOf(copyFrom.callbacks);
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
			case "schemas" -> toType(getSchemas(), type);
			case "responses" -> toType(getResponses(), type);
			case "parameters" -> toType(getParameters(), type);
			case "examples" -> toType(getExamples(), type);
			case "requestBodies" -> toType(getRequestBodies(), type);
			case "headers" -> toType(getHeaders(), type);
			case "securitySchemes" -> toType(getSecuritySchemes(), type);
			case "links" -> toType(getLinks(), type);
			case "callbacks" -> toType(getCallbacks(), type);
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
		var s = CollectionUtils.setb(String.class)
			.addIf(callbacks != null, "callbacks")
			.addIf(examples != null, "examples")
			.addIf(headers != null, "headers")
			.addIf(links != null, "links")
			.addIf(parameters != null, "parameters")
			.addIf(requestBodies != null, "requestBodies")
			.addIf(responses != null, "responses")
			.addIf(schemas != null, "schemas")
			.addIf(securitySchemes != null, "securitySchemes")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Components set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "callbacks" -> setCallbacks(toMap(value, String.class, Callback.class).sparse().build());
			case "examples" -> setExamples(toMap(value, String.class, Example.class).sparse().build());
			case "headers" -> setHeaders(toMap(value, String.class, HeaderInfo.class).sparse().build());
			case "links" -> setLinks(toMap(value, String.class, Link.class).sparse().build());
			case "parameters" -> setParameters(toMap(value, String.class, Parameter.class).sparse().build());
			case "requestBodies" -> setRequestBodies(toMap(value, String.class, RequestBodyInfo.class).sparse().build());
			case "responses" -> setResponses(toMap(value, String.class, Response.class).sparse().build());
			case "schemas" -> setSchemas(toMap(value, String.class, SchemaInfo.class).sparse().build());
			case "securitySchemes" -> setSecuritySchemes(toMap(value, String.class, SecuritySchemeInfo.class).sparse().build());
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
		this.callbacks = value;
		return this;
	}

	/**
	 * Sets the examples map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setExamples(Map<String,Example> value) {
		this.examples = value;
		return this;
	}

	/**
	 * Sets the headers map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setHeaders(Map<String,HeaderInfo> value) {
		this.headers = value;
		return this;
	}

	/**
	 * Sets the links map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setLinks(Map<String,Link> value) {
		this.links = value;
		return this;
	}

	/**
	 * Sets the parameters map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setParameters(Map<String,Parameter> value) {
		this.parameters = value;
		return this;
	}

	/**
	 * Sets the request bodies map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setRequestBodies(Map<String,RequestBodyInfo> value) {
		this.requestBodies = value;
		return this;
	}

	/**
	 * Sets the responses map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setResponses(Map<String,Response> value) {
		this.responses = value;
		return this;
	}

	/**
	 * Sets the schemas map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setSchemas(Map<String,SchemaInfo> value) {
		this.schemas = value;
		return this;
	}

	/**
	 * Sets the security schemes map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Components setSecuritySchemes(Map<String,SecuritySchemeInfo> value) {
		this.securitySchemes = value;
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