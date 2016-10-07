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
package org.apache.juneau.dto.swagger;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Describes a single API operation on a path.
 * <p>
 * Example:
 * <p class='bcode'>
 * 	{
 * 		<js>"tags"</js>: [
 * 			<js>"pet"</js>
 * 		],
 * 		<js>"summary"</js>: <js>"Updates a pet in the store with form data"</js>,
 * 		<js>"description"</js>: "",
 * 		<js>"operationId"</js>: <js>"updatePetWithForm"</js>,
 * 		<js>"consumes"</js>: [
 * 			<js>"application/x-www-form-urlencoded"</js>
 * 		],
 * 		<js>"produces"</js>: [
 * 			<js>"application/json"</js>,
 * 			<js>"application/xml"</js>
 * 		],
 * 		<js>"parameters"</js>: [
 * 			{
 * 				<js>"name"</js>: <js>"petId"</js>,
 * 				<js>"in"</js>: <js>"path"</js>,
 * 				<js>"description"</js>: <js>"ID of pet that needs to be updated"</js>,
 * 				<js>"required"</js>: <jk>true</jk>,
 * 				<js>"type"</js>: <js>"string"</js>
 * 			},
 * 			{
 * 				<js>"name"</js>: <js>"name"</js>,
 * 				<js>"in"</js>: <js>"formData"</js>,
 * 				<js>"description"</js>: <js>"Updated name of the pet"</js>,
 * 				<js>"required"</js>: <jk>false</jk>,
 * 				<js>"type"</js>: <js>"string"</js>
 * 			},
 * 			{
 * 				<js>"name"</js>: <js>"status"</js>,
 * 				<js>"in"</js>: <js>"formData"</js>,
 * 				<js>"description"</js>: <js>"Updated status of the pet"</js>,
 * 				<js>"required"</js>: <jk>false</jk>,
 * 				<js>"type"</js>: <js>"string"</js>
 * 			}
 * 		],
 * 		<js>"responses"</js>: {
 * 			<js>"200"</js>: {
 * 				<js>"description"</js>: <js>"Pet updated."</js>
 * 			},
 * 			<js>"405"</js>: {
 * 				<js>"description"</js>: <js>"Invalid input"</js>
 * 			}
 * 		},
 * 		<js>"security"</js>: [
 * 			{
 * 				<js>"petstore_auth"</js>: [
 * 					<js>"write:pets"</js>,
 * 					<js>"read:pets"</js>
 * 				]
 * 			}
 * 		]
 * 	}
 * </p>
 *
 * @author james.bognar
 */
@Bean(properties="operationId,summary,description,tags,externalDocs,consumes,produces,parameters,responses,schemes,deprecated,security")
public class Operation {

	private List<String> tags;
	private String summary;
	private String description;
	private ExternalDocumentation externalDocs;
	private String operationId;
	private List<String> consumes;
	private List<String> produces;
	private List<Parameter> parameters;
	private Map<String,Response> responses;
	private List<String> schemes;
	private Boolean deprecated;
	private List<Map<String,List<String>>> security;

	/**
	 * Convenience method for creating a new Operation object.
	 *
	 * @return A new Operation object.
	 */
	public static Operation create() {
		return new Operation();
	}

	/**
	 * Bean property getter:  <property>tags</property>.
	 * <p>
	 * A list of tags for API documentation control.
	 * Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @return The value of the <property>tags</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getTags() {
		return tags;
	}

	/**
	 * Bean property setter:  <property>tags</property>.
	 * <p>
	 * A list of tags for API documentation control.
	 * Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @param tags The new value for the <property>tags</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setTags(List<String> tags) {
		this.tags = tags;
		return this;
	}

	/**
	 * Bean property adder:  <property>tags</property>.
	 * <p>
	 * A list of tags for API documentation control.
	 * Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @param tags The values to add for the <property>tags</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Operation addTags(String...tags) {
		return addTags(Arrays.asList(tags));
	}

	/**
	 * Bean property adder:  <property>tags</property>.
	 * <p>
	 * A list of tags for API documentation control.
	 * Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @param tags The values to add for the <property>tags</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Operation addTags(Collection<String> tags) {
		if (this.tags == null)
			this.tags = new LinkedList<String>();
		this.tags.addAll(tags);
		return this;
	}

	/**
	 * Bean property getter:  <property>summary</property>.
	 * <p>
	 * A short summary of what the operation does.
	 * For maximum readability in the swagger-ui, this field SHOULD be less than 120 characters.
	 *
	 * @return The value of the <property>summary</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * Bean property setter:  <property>summary</property>.
	 * <p>
	 * A short summary of what the operation does.
	 * For maximum readability in the swagger-ui, this field SHOULD be less than 120 characters.
	 *
	 * @param summary The new value for the <property>summary</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setSummary(String summary) {
		this.summary = summary;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * A verbose explanation of the operation behavior.
	 * <a href='https://help.github.com/articles/github-flavored-markdown'>GFM syntax</a> can be used for rich text representation.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * <p>
	 * A verbose explanation of the operation behavior.
	 * <a href='https://help.github.com/articles/github-flavored-markdown'>GFM syntax</a> can be used for rich text representation.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 * <p>
	 * Additional external documentation for this operation.
	 *
	 * @return The value of the <property>externalDocs</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	/**
	 * Bean property setter:  <property>externalDocs</property>.
	 * <p>
	 * Additional external documentation for this operation.
	 *
	 * @param externalDocs The new value for the <property>externalDocs</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setExternalDocs(ExternalDocumentation externalDocs) {
		this.externalDocs = externalDocs;
		return this;
	}

	/**
	 * Bean property getter:  <property>operationId</property>.
	 * <p>
	 * Unique string used to identify the operation. The id MUST be unique among all operations described in the API.
	 * Tools and libraries MAY use the operationId to uniquely identify an operation, therefore, it is recommended to follow common programming naming conventions.
	 *
	 * @return The value of the <property>operationId</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getOperationId() {
		return operationId;
	}

	/**
	 * Bean property setter:  <property>operationId</property>.
	 * <p>
	 * Unique string used to identify the operation. The id MUST be unique among all operations described in the API.
	 * Tools and libraries MAY use the operationId to uniquely identify an operation, therefore, it is recommended to follow common programming naming conventions.
	 *
	 * @param operationId The new value for the <property>operationId</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setOperationId(String operationId) {
		this.operationId = operationId;
		return this;
	}

	/**
	 * Bean property getter:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the operation can consume.
	 * This overrides the <code>consumes</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @return The value of the <property>consumes</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getConsumes() {
		return consumes;
	}

	/**
	 * Bean property setter:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the operation can consume.
	 * This overrides the <code>consumes</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param consumes The new value for the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setConsumes(List<String> consumes) {
		this.consumes = consumes;
		return this;
	}

	/**
	 * Bean property adder:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the operation can consume.
	 * This overrides the <code>consumes</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param consumes The new values to add to the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Operation addConsumes(String...consumes) {
		return addConsumes(Arrays.asList(consumes));
	}

	/**
	 * Bean property adder:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the operation can consume.
	 * This overrides the <code>consumes</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param consumes The new values to add to the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Operation addConsumes(Collection<String> consumes) {
		if (this.consumes == null)
			this.consumes = new LinkedList<String>();
		this.consumes.addAll(consumes);
		return this;
	}

	/**
	 * Bean property getter:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the operation can produce.
	 * This overrides the <code>produces</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @return The value of the <property>produces</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getProduces() {
		return produces;
	}

	/**
	 * Bean property setter:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the operation can produce.
	 * This overrides the <code>produces</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param produces The new value for the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setProduces(List<String> produces) {
		this.produces = produces;
		return this;
	}

	/**
	 * Bean property adder:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the operation can produce.
	 * This overrides the <code>produces</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param produces The new value for the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Operation addProduces(String...produces) {
		if (this.produces == null)
			this.produces = new LinkedList<String>();
		this.produces.addAll(Arrays.asList(produces));
		return this;
	}

	/**
	 * Bean property getter:  <property>parameters</property>.
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 * If a parameter is already defined at the <a href='http://swagger.io/specification/#pathItemParameters'>Path Item</a>, the new definition will override it, but can never remove it.
	 * The list MUST NOT include duplicated parameters.
	 * A unique parameter is defined by a combination of a <code>name</code> and <code>location</code>.
	 * The list can use the <a href='http://swagger.io/specification/#referenceObject'>Reference Object</a> to link to parameters that are defined at the <a href='http://swagger.io/specification/#swaggerParameters'>Swagger Object's parameters</a>.
	 * There can be one <js>"body"</js> parameter at most.
	 *
	 * @return The value of the <property>parameters</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Bean property setter:  <property>parameters</property>.
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 * If a parameter is already defined at the <a href='http://swagger.io/specification/#pathItemParameters'>Path Item</a>, the new definition will override it, but can never remove it.
	 * The list MUST NOT include duplicated parameters.
	 * A unique parameter is defined by a combination of a <code>name</code> and <code>location</code>.
	 * The list can use the <a href='http://swagger.io/specification/#referenceObject'>Reference Object</a> to link to parameters that are defined at the <a href='http://swagger.io/specification/#swaggerParameters'>Swagger Object's parameters</a>.
	 * There can be one <js>"body"</js> parameter at most.
	 *
	 * @param parameters The new value for the <property>parameters</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	/**
	 * Bean property adder:  <property>parameters</property>.
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 * If a parameter is already defined at the <a href='http://swagger.io/specification/#pathItemParameters'>Path Item</a>, the new definition will override it, but can never remove it.
	 * The list MUST NOT include duplicated parameters.
	 * A unique parameter is defined by a combination of a <code>name</code> and <code>location</code>.
	 * The list can use the <a href='http://swagger.io/specification/#referenceObject'>Reference Object</a> to link to parameters that are defined at the <a href='http://swagger.io/specification/#swaggerParameters'>Swagger Object's parameters</a>.
	 * There can be one <js>"body"</js> parameter at most.
	 *
	 * @param parameter The new value to add to the <property>parameters</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation addParameter(Parameter parameter) {
		if (parameters == null)
			parameters = new LinkedList<Parameter>();
		parameters.add(parameter);
		return this;
	}

	/**
	 * Bean property getter:  <property>responses</property>.
	 * <p>
	 * Required. The list of possible responses as they are returned from executing this operation.
	 *
	 * @return The value of the <property>responses</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Response> getResponses() {
		return responses;
	}

	/**
	 * Bean property setter:  <property>responses</property>.
	 * <p>
	 * Required. The list of possible responses as they are returned from executing this operation.
	 *
	 * @param responses The new value for the <property>responses</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setResponses(Map<String,Response> responses) {
		this.responses = responses;
		return this;
	}

	/**
	 * Bean property adder:  <property>responses</property>.
	 * <p>
	 * Required. The list of possible responses as they are returned from executing this operation.
	 *
	 * @param statusCode The HTTP status code.
	 * @param response The response description.
	 * @return This object (for method chaining).
	 */
	public Operation addResponse(String statusCode, Response response) {
		if (responses == null)
			responses = new TreeMap<String,Response>();
		responses.put(statusCode, response);
		return this;
	}

	/**
	 * Bean property getter:  <property>schemes</property>.
	 * <p>
	 * The transfer protocol for the operation.
	 * Values MUST be from the list: <js>"http"</js>, <js>"https"</js>, <js>"ws"</js>, <js>"wss"</js>.
	 * The value overrides the Swagger Object <code>schemes</code> definition.
	 *
	 * @return The value of the <property>schemes</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getSchemes() {
		return schemes;
	}

	/**
	 * Bean property setter:  <property>schemes</property>.
	 * <p>
	 * The transfer protocol for the operation.
	 * Values MUST be from the list: <js>"http"</js>, <js>"https"</js>, <js>"ws"</js>, <js>"wss"</js>.
	 * The value overrides the Swagger Object <code>schemes</code> definition.
	 *
	 * @param schemes The new value for the <property>schemes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setSchemes(List<String> schemes) {
		this.schemes = schemes;
		return this;
	}

	/**
	 * Bean property adder:  <property>schemes</property>.
	 * <p>
	 * The transfer protocol for the operation.
	 * Values MUST be from the list: <js>"http"</js>, <js>"https"</js>, <js>"ws"</js>, <js>"wss"</js>.
	 * The value overrides the Swagger Object <code>schemes</code> definition.
	 *
	 * @param schemes The new values to add to the <property>schemes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Operation addSchemes(String...schemes) {
		return addSchemes(Arrays.asList(schemes));
	}

	/**
	 * Bean property adder:  <property>schemes</property>.
	 * <p>
	 * The transfer protocol for the operation.
	 * Values MUST be from the list: <js>"http"</js>, <js>"https"</js>, <js>"ws"</js>, <js>"wss"</js>.
	 * The value overrides the Swagger Object <code>schemes</code> definition.
	 *
	 * @param schemes The new values to add to the <property>schemes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Operation addSchemes(Collection<String> schemes) {
		if (this.schemes == null)
			this.schemes = new LinkedList<String>();
		this.schemes.addAll(schemes);
		return this;
	}

	/**
	 * Bean property getter:  <property>deprecated</property>.
	 * <p>
	 * Declares this operation to be deprecated.
	 * Usage of the declared operation should be refrained.
	 * Default value is <jk>false</jk>.
	 *
	 * @return The value of the <property>deprecated</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getDeprecated() {
		return deprecated;
	}

	/**
	 * Bean property setter:  <property>deprecated</property>.
	 * <p>
	 * Declares this operation to be deprecated.
	 * Usage of the declared operation should be refrained.
	 * Default value is <jk>false</jk>.
	 *
	 * @param deprecated The new value for the <property>deprecated</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setDeprecated(Boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	/**
	 * Bean property getter:  <property>security</property>.
	 * <p>
	 * A declaration of which security schemes are applied for this operation.
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * This definition overrides any declared top-level security.
	 * To remove a top-level <code>security</code> declaration, an empty array can be used.
	 *
	 * @return The value of the <property>security</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Map<String,List<String>>> getSecurity() {
		return security;
	}

	/**
	 * Bean property setter:  <property>security</property>.
	 * <p>
	 * A declaration of which security schemes are applied for this operation.
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * This definition overrides any declared top-level security.
	 * To remove a top-level <code>security</code> declaration, an empty array can be used.
	 *
	 * @param security The new value for the <property>security</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setSecurity(List<Map<String,List<String>>> security) {
		this.security = security;
		return this;
	}

	/**
	 * Bean property adder:  <property>security</property>.
	 * <p>
	 * A declaration of which security schemes are applied for this operation.
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * This definition overrides any declared top-level security.
	 * To remove a top-level <code>security</code> declaration, an empty array can be used.
	 *
	 * @param security The new value to add to the <property>security</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Operation addSecurity(Map<String,List<String>> security) {
		if (this.security == null)
			this.security = new LinkedList<Map<String,List<String>>>();
		this.security.add(security);
		return this;
	}
}
