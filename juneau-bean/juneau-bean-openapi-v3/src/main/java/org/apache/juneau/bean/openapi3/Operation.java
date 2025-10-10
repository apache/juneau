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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.internal.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

/**
 * Describes a single API operation on a path.
 *
 * <p>
 * The Operation Object describes a single operation (such as GET, POST, PUT, DELETE) that can be performed on a path.
 * Operations are the core of the API specification, defining what actions can be taken, what parameters they accept,
 * and what responses they return.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Operation Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>tags</c> (array of string) - A list of tags for API documentation control
 * 	<li><c>summary</c> (string) - A short summary of what the operation does
 * 	<li><c>description</c> (string) - A verbose explanation of the operation behavior (CommonMark syntax may be used)
 * 	<li><c>externalDocs</c> ({@link ExternalDocumentation}) - Additional external documentation for this operation
 * 	<li><c>operationId</c> (string) - Unique string used to identify the operation
 * 	<li><c>parameters</c> (array of {@link Parameter}) - A list of parameters that are applicable for this operation
 * 	<li><c>requestBody</c> ({@link RequestBodyInfo}) - The request body applicable for this operation
 * 	<li><c>responses</c> (map of {@link Response}, REQUIRED) - The list of possible responses as they are returned from executing this operation
 * 	<li><c>callbacks</c> (map of {@link Callback}) - A map of possible out-of band callbacks related to the parent operation
 * 	<li><c>deprecated</c> (boolean) - Declares this operation to be deprecated
 * 	<li><c>security</c> (array of {@link SecurityRequirement}) - A declaration of which security mechanisms can be used for this operation
 * 	<li><c>servers</c> (array of {@link Server}) - An alternative server array to service this operation
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an Operation object for a GET endpoint</jc>
 * 	Operation <jv>operation</jv> = <jk>new</jk> Operation()
 * 		.setTags(<js>"pet"</js>)
 * 		.setSummary(<js>"Find pets by status"</js>)
 * 		.setDescription(<js>"Multiple status values can be provided with comma separated strings"</js>)
 * 		.setOperationId(<js>"findPetsByStatus"</js>)
 * 		.setParameters(
 * 			<jk>new</jk> Parameter()
 * 				.setName(<js>"status"</js>)
 * 				.setIn(<js>"query"</js>)
 * 				.setDescription(<js>"Status values that need to be considered for filter"</js>)
 * 				.setRequired(<jk>true</jk>)
 * 				.setSchema(
 * 					<jk>new</jk> SchemaInfo().setType(<js>"string"</js>)
 * 				)
 * 		)
 * 		.setResponses(
 * 			JsonMap.<jsm>of</jsm>(
 * 				<js>"200"</js>, <jk>new</jk> Response()
 * 					.setDescription(<js>"successful operation"</js>)
 * 					.setContent(
 * 						JsonMap.<jsm>of</jsm>(
 * 							<js>"application/json"</js>, <jk>new</jk> MediaType()
 * 								.setSchema(<jk>new</jk> SchemaInfo().<jsm>setType</jsm>(<js>"array"</js>))
 * 						)
 * 					)
 * 			)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#operation-object">OpenAPI Specification &gt; Operation Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/paths-and-operations/">OpenAPI Paths and Operations</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
@FluentSetters
public class Operation extends OpenApiElement {

	private List<String> tags;
	private String summary, description, operationId;
	private ExternalDocumentation externalDocs;
	private List<Parameter> parameters;
	private RequestBodyInfo requestBody;
	private Map<String,Response> responses;
	private Map<String,Callback> callbacks;
	private Boolean deprecated;
	private List<SecurityRequirement> security;
	private List<Server> servers;

	/**
	 * Default constructor.
	 */
	public Operation() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Operation(Operation copyFrom) {
		super(copyFrom);
		this.tags = copyOf(copyFrom.tags);
		this.summary = copyFrom.summary;
		this.description = copyFrom.description;
		this.operationId = copyFrom.operationId;
		this.externalDocs = copyFrom.externalDocs;
		this.parameters = copyOf(copyFrom.parameters);
		this.requestBody = copyFrom.requestBody;
		this.responses = copyOf(copyFrom.responses);
		this.callbacks = copyOf(copyFrom.callbacks);
		this.deprecated = copyFrom.deprecated;
		this.security = copyOf(copyFrom.security);
		this.servers = copyOf(copyFrom.servers);
	}

	/**
	 * Returns the tags list.
	 *
	 * @return The tags list.
	 */
	public List<String> getTags() {
		return tags;
	}

	/**
	 * Sets the tags list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setTags(List<String> value) {
		this.tags = value;
		return this;
	}

	/**
	 * Sets the tags list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setTags(String...value) {
		setTags(listBuilder(String.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Bean property appender:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags for API documentation control.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addTags(String...values) {
		tags = listBuilder(tags).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property appender:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags for API documentation control.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addTags(Collection<String> values) {
		tags = listBuilder(tags).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addParameters(Parameter...values) {
		parameters = listBuilder(parameters).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addParameters(Collection<Parameter> values) {
		parameters = listBuilder(parameters).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>responses</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @param statusCode
	 * 	The status code for the response.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param response
	 * 	The response object.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addResponse(String statusCode, Response response) {
		assertArgNotNull("statusCode", statusCode);
		assertArgNotNull("response", response);
		responses = mapBuilder(responses).sparse().add(statusCode, response).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>callbacks</property>.
	 *
	 * <p>
	 * A map of possible out-of band callbacks related to the parent operation.
	 *
	 * @param name
	 * 	The name of the callback.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param callback
	 * 	The callback object.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addCallback(String name, Callback callback) {
		assertArgNotNull("name", name);
		assertArgNotNull("callback", callback);
		callbacks = mapBuilder(callbacks).sparse().add(name, callback).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security mechanisms can be used for this operation.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addSecurity(SecurityRequirement...values) {
		security = listBuilder(security).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security mechanisms can be used for this operation.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addSecurity(Collection<SecurityRequirement> values) {
		security = listBuilder(security).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>servers</property>.
	 *
	 * <p>
	 * An alternative server array to service this operation.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addServers(Server...values) {
		servers = listBuilder(servers).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>servers</property>.
	 *
	 * <p>
	 * An alternative server array to service this operation.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Operation addServers(Collection<Server> values) {
		servers = listBuilder(servers).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Returns the summary.
	 *
	 * @return The summary.
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * Sets the summary.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setSummary(String value) {
		this.summary = value;
		return this;
	}

	/**
	 * Returns the description.
	 *
	 * @return The description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setDescription(String value) {
		this.description = value;
		return this;
	}

	/**
	 * Returns the operation ID.
	 *
	 * @return The operation ID.
	 */
	public String getOperationId() {
		return operationId;
	}

	/**
	 * Sets the operation ID.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setOperationId(String value) {
		this.operationId = value;
		return this;
	}

	/**
	 * Returns the external documentation.
	 *
	 * @return The external documentation.
	 */
	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	/**
	 * Sets the external documentation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setExternalDocs(ExternalDocumentation value) {
		this.externalDocs = value;
		return this;
	}

	/**
	 * Returns the parameters list.
	 *
	 * @return The parameters list.
	 */
	public List<Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Returns the parameter with the specified type and name.
	 *
	 * @param in The parameter in.  Must not be <jk>null</jk>.
	 * @param name The parameter name.  Must not be <jk>null</jk>.
	 * @return The matching parameter, or <jk>null</jk> if not found.
	 */
	public Parameter getParameter(String in, String name) {
		assertArgNotNull("in", in);
		assertArgNotNull("name", name);
		if (parameters != null)
			for (var p : parameters)
				if (eq(p.getIn(), in) && eq(p.getName(), name))
					return p;
		return null;
	}

	/**
	 * Sets the parameters list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setParameters(List<Parameter> value) {
		this.parameters = value;
		return this;
	}

	/**
	 * Sets the parameters list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setParameters(Parameter...value) {
		setParameters(listBuilder(Parameter.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Returns the request body.
	 *
	 * @return The request body.
	 */
	public RequestBodyInfo getRequestBody() {
		return requestBody;
	}

	/**
	 * Sets the request body.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setRequestBody(RequestBodyInfo value) {
		this.requestBody = value;
		return this;
	}

	/**
	 * Returns the responses map.
	 *
	 * @return The responses map.
	 */
	public Map<String,Response> getResponses() {
		return responses;
	}

	/**
	 * Returns the response with the given status code.
	 *
	 * @param status The HTTP status code.  Must not be <jk>null</jk>.
	 * @return The response, or <jk>null</jk> if not found.
	 */
	public Response getResponse(String status) {
		assertArgNotNull("status", status);
		return responses == null ? null : responses.get(status);
	}

	/**
	 * Returns the response with the given status code.
	 *
	 * @param status The HTTP status code.
	 * @return The response, or <jk>null</jk> if not found.
	 */
	public Response getResponse(int status) {
		return getResponse(String.valueOf(status));
	}

	/**
	 * Sets the responses map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setResponses(Map<String,Response> value) {
		this.responses = value;
		return this;
	}

	/**
	 * Returns the callbacks map.
	 *
	 * @return The callbacks map.
	 */
	public Map<String,Callback> getCallbacks() {
		return callbacks;
	}

	/**
	 * Sets the callbacks map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setCallbacks(Map<String,Callback> value) {
		this.callbacks = value;
		return this;
	}

	/**
	 * Returns the deprecated flag.
	 *
	 * @return The deprecated flag.
	 */
	public Boolean getDeprecated() {
		return deprecated;
	}

	/**
	 * Sets the deprecated flag.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setDeprecated(Boolean value) {
		this.deprecated = value;
		return this;
	}

	/**
	 * Returns the security requirements list.
	 *
	 * @return The security requirements list.
	 */
	public List<SecurityRequirement> getSecurity() {
		return security;
	}

	/**
	 * Sets the security requirements list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setSecurity(List<SecurityRequirement> value) {
		this.security = value;
		return this;
	}

	/**
	 * Sets the security requirements list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setSecurity(SecurityRequirement...value) {
		setSecurity(listBuilder(SecurityRequirement.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Returns the servers list.
	 *
	 * @return The servers list.
	 */
	public List<Server> getServers() {
		return servers;
	}

	/**
	 * Sets the servers list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setServers(List<Server> value) {
		this.servers = value;
		return this;
	}

	/**
	 * Sets the servers list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setServers(Server...value) {
		setServers(listBuilder(Server.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Creates a copy of this object.
	 *
	 * @return A copy of this object.
	 */
	public Operation copy() {
		return new Operation(this);
	}

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "tags" -> toType(getTags(), type);
			case "summary" -> toType(getSummary(), type);
			case "description" -> toType(getDescription(), type);
			case "operationId" -> toType(getOperationId(), type);
			case "externalDocs" -> toType(getExternalDocs(), type);
			case "parameters" -> toType(getParameters(), type);
			case "requestBody" -> toType(getRequestBody(), type);
			case "responses" -> toType(getResponses(), type);
			case "callbacks" -> toType(getCallbacks(), type);
			case "deprecated" -> toType(getDeprecated(), type);
			case "security" -> toType(getSecurity(), type);
			case "servers" -> toType(getServers(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* OpenApiElement */
	public Operation set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "callbacks" -> setCallbacks(mapBuilder(String.class, Callback.class).sparse().addAny(value).build());
			case "deprecated" -> setDeprecated(toType(value, Boolean.class));
			case "description" -> setDescription(Utils.s(value));
			case "externalDocs" -> setExternalDocs(toType(value, ExternalDocumentation.class));
			case "operationId" -> setOperationId(Utils.s(value));
			case "parameters" -> setParameters(listBuilder(Parameter.class).sparse().addAny(value).build());
			case "requestBody" -> setRequestBody(toType(value, RequestBodyInfo.class));
			case "responses" -> setResponses(mapBuilder(String.class, Response.class).sparse().addAny(value).build());
			case "security" -> setSecurity(listBuilder(SecurityRequirement.class).sparse().addAny(value).build());
			case "servers" -> setServers(listBuilder(Server.class).sparse().addAny(value).build());
			case "summary" -> setSummary(Utils.s(value));
			case "tags" -> setTags(listBuilder(String.class).sparse().addAny(value).build());
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(callbacks != null, "callbacks")
			.addIf(deprecated != null, "deprecated")
			.addIf(description != null, "description")
			.addIf(externalDocs != null, "externalDocs")
			.addIf(operationId != null, "operationId")
			.addIf(parameters != null, "parameters")
			.addIf(requestBody != null, "requestBody")
			.addIf(responses != null, "responses")
			.addIf(security != null, "security")
			.addIf(servers != null, "servers")
			.addIf(summary != null, "summary")
			.addIf(tags != null, "tags")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	// <FluentSetters>

	@Override /* GENERATED - do not modify */
	public Operation strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - do not modify */
	public Operation strict(Object value) {
		super.strict(value);
		return this;
	}

	// </FluentSetters>
}
