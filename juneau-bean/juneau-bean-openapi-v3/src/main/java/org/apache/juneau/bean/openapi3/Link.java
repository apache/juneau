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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * The Link object represents a possible design-time link for a response.
 *
 * <p>
 * The Link Object represents a possible design-time link for a response. The presence of a link does not guarantee
 * the caller's ability to successfully invoke it, rather it provides a known relationship and traversal mechanism
 * between responses and other operations.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Link Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>operationRef</c> (string) - A relative or absolute reference to an OAS operation (mutually exclusive with <c>operationId</c>)
 * 	<li><c>operationId</c> (string) - The name of an existing, resolvable OAS operation (mutually exclusive with <c>operationRef</c>)
 * 	<li><c>parameters</c> (map of any) - A map representing parameters to pass to an operation as specified with <c>operationId</c> or identified via <c>operationRef</c>
 * 	<li><c>requestBody</c> (any) - A literal value or expression to use as a request body when calling the target operation
 * 	<li><c>description</c> (string) - A description of the link (CommonMark syntax may be used)
 * 	<li><c>server</c> ({@link Server}) - A server object to be used by the target operation
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a link to another operation</jc>
 * 	Link <jv>link</jv> = <jk>new</jk> Link()
 * 		.setOperationId(<js>"getUserById"</js>)
 * 		.setParameters(
 * 			JsonMap.<jsm>of</jsm>(<js>"userId"</js>, <js>"$response.body#/id"</js>)
 * 		)
 * 		.setDescription(<js>"The id value returned in the response can be used as userId parameter in GET /users/{userId}"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#link-object">OpenAPI Specification &gt; Link Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/links/">OpenAPI Links</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class Link extends OpenApiElement {

	private String operationRef;
	private String operationId;
	private String description;
	private Object requestBody;
	private Server server;
	private Map<String,Object> parameters;

	/**
	 * Default constructor.
	 */
	public Link() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Link(Link copyFrom) {
		super(copyFrom);

		this.operationRef = copyFrom.operationRef;
		this.description = copyFrom.description;
		this.operationId = copyFrom.operationId;
		this.requestBody = copyFrom.requestBody;
		this.server = copyFrom.server == null ? null : copyFrom.server.copy();
		this.parameters = copyOf(copyFrom.parameters);
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param mimeType The mime-type string.  Must not be <jk>null</jk>.
	 * @param parameter The example.  Must not be <jk>null</jk>.
	 * @return This object
	 */
	public Link addParameter(String mimeType, Object parameter) {
		assertArgNotNull("mimeType", mimeType);
		assertArgNotNull("parameter", parameter);
		parameters = mapBuilder(parameters).sparse().add(mimeType, parameter).build();
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Link copy() {
		return new Link(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> toType(getDescription(), type);
			case "operationRef" -> toType(getOperationRef(), type);
			case "operationId" -> toType(getOperationId(), type);
			case "requestBody" -> toType(getRequestBody(), type);
			case "parameters" -> toType(getParameters(), type);
			case "server" -> toType(getServer(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>externalValue</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getOperationId() { return operationId; }

	/**
	 * Bean property getter:  <property>operationRef</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getOperationRef() { return operationRef; }

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * An example of the response message.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Object> getParameters() { return parameters; }

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js>
	 * to control the number of results per page might default to 100 if not supplied by the client in the request.
	 *
	 * (Note: <js>"value"</js> has no meaning for required parameters.)
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getRequestBody() { return requestBody; }

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Server getServer() { return server; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(description != null, "description")
			.addIf(operationId != null, "operationId")
			.addIf(operationRef != null, "operationRef")
			.addIf(parameters != null, "parameters")
			.addIf(requestBody != null, "requestBody")
			.addIf(server != null, "server")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Link set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> setDescription(Utils.s(value));
			case "operationId" -> setOperationId(Utils.s(value));
			case "operationRef" -> setOperationRef(Utils.s(value));
			case "parameters" -> setParameters(mapBuilder(String.class, Object.class).sparse().addAny(value).build());
			case "requestBody" -> setRequestBody(value);
			case "server" -> setServer(toType(value, Server.class));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>externalValue</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>MUST be in the format of an email address.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setOperationId(String value) {
		operationId = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>operationRef</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setOperationRef(String value) {
		operationRef = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * <p>
	 * An example of the response message.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Keys must be MIME-type strings.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setParameters(Map<String,Object> value) {
		parameters = copyOf(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>value</property>.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js>
	 * to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * (Note: <js>"default"</js> has no meaning for required parameters.)
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 *
	 * @param val The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setRequestBody(Object val) {
		requestBody = val;
		return this;
	}

	/**
	 * Bean property setter:  <property>additionalProperties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setServer(Server value) {
		server = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Link strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Link strict(Object value) {
		super.strict(value);
		return this;
	}
}