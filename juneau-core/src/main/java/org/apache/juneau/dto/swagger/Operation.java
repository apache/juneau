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
import org.apache.juneau.http.*;

/**
 * Describes a single API operation on a path.
 *
 * <h5 class='section'>Example:</h5>
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
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 		<ul>
 * 			<li class='sublink'>
 * 				<a class='doclink' href='../../../../../overview-summary.html#DTOs.Swagger'>Swagger</a>
 * 		</ul>
 * 	</li>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.swagger</a>
 * 	</li>
 * </ul>
 */
@Bean(properties="operationId,summary,description,tags,externalDocs,consumes,produces,parameters,responses,schemes,deprecated,security")
@SuppressWarnings("hiding")
public class Operation extends SwaggerElement {

	private List<String> tags;
	private String summary;
	private String description;
	private ExternalDocumentation externalDocs;
	private String operationId;
	private List<MediaType> consumes;
	private List<MediaType> produces;
	private List<ParameterInfo> parameters;
	private Map<Integer,ResponseInfo> responses;
	private List<String> schemes;
	private Boolean deprecated;
	private List<Map<String,List<String>>> security;

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
	 * Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation addTags(Collection<String> tags) {
		if (tags != null) {
			if (this.tags == null)
				this.tags = new LinkedList<String>();
			this.tags.addAll(tags);
		}
		return this;
	}

	/**
	 * Synonym for {@link #addTags(String...)}.
	 *
	 * @param tags The new value for the <property>tags</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation tags(String...tags) {
		return addTags(tags);
	}

	/**
	 * Synonym for {@link #addTags(Collection)}.
	 *
	 * @param tags The new value for the <property>tags</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation tags(Collection<String> tags) {
		return addTags(tags);
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
	 * Synonym for {@link #setSummary(String)}.
	 *
	 * @param summary The new value for the <property>summary</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation summary(String summary) {
		return setSummary(summary);
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * A verbose explanation of the operation behavior.
	 * <a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used
	 * for rich text representation.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not
	 * set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * <p>
	 * A verbose explanation of the operation behavior.
	 * <a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used
	 * for rich text representation.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Synonym for {@link #setDescription(String)}.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation description(String description) {
		return setDescription(description);
	}

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 * <p>
	 * Additional external documentation for this operation.
	 *
	 * @return The value of the <property>externalDocs</property> property on this bean, or <jk>null</jk> if it is not
	 * set.
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
	 * Synonym for {@link #setExternalDocs(ExternalDocumentation)}.
	 *
	 * @param externalDocs The new value for the <property>externalDocs</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation externalDocs(ExternalDocumentation externalDocs) {
		return setExternalDocs(externalDocs);
	}

	/**
	 * Bean property getter:  <property>operationId</property>.
	 * <p>
	 * Unique string used to identify the operation. The id MUST be unique among all operations described in the API.
	 * Tools and libraries MAY use the operationId to uniquely identify an operation, therefore, it is recommended to
	 * follow common programming naming conventions.
	 *
	 * @return The value of the <property>operationId</property> property on this bean, or <jk>null</jk> if it is not
	 * set.
	 */
	public String getOperationId() {
		return operationId;
	}

	/**
	 * Bean property setter:  <property>operationId</property>.
	 * <p>
	 * Unique string used to identify the operation. The id MUST be unique among all operations described in the API.
	 * Tools and libraries MAY use the operationId to uniquely identify an operation, therefore, it is recommended to
	 * follow common programming naming conventions.
	 *
	 * @param operationId The new value for the <property>operationId</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setOperationId(String operationId) {
		this.operationId = operationId;
		return this;
	}

	/**
	 * Synonym for {@link #setOperationId(String)}.
	 *
	 * @param operationId The new value for the <property>operationId</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation operationId(String operationId) {
		return setOperationId(operationId);
	}

	/**
	 * Bean property getter:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the operation can consume.
	 * This overrides the <code>consumes</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a class="doclink"
	 * href="http://swagger.io/specification/#mimeTypes">Mime Types</a>.
	 *
	 * @return The value of the <property>consumes</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<MediaType> getConsumes() {
		return consumes;
	}

	/**
	 * Bean property setter:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the operation can consume.
	 * This overrides the <code>consumes</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a class="doclink"
	 * href="http://swagger.io/specification/#mimeTypes">Mime Types</a>.
	 *
	 * @param consumes The new value for the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setConsumes(List<MediaType> consumes) {
		this.consumes = consumes;
		return this;
	}

	/**
	 * Bean property adder:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the operation can consume.
	 * This overrides the <code>consumes</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a class="doclink"
	 * href="http://swagger.io/specification/#mimeTypes">Mime Types</a>.
	 *
	 * @param consumes The new values to add to the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation addConsumes(MediaType...consumes) {
		return addConsumes(Arrays.asList(consumes));
	}

	/**
	 * Bean property adder:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the operation can consume.
	 * This overrides the <code>consumes</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a class="doclink"
	 * href="http://swagger.io/specification/#mimeTypes">Mime Types</a>.
	 *
	 * @param consumes The new values to add to the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation addConsumes(Collection<MediaType> consumes) {
		if (consumes != null) {
			if (this.consumes == null)
				this.consumes = new LinkedList<MediaType>();
			this.consumes.addAll(consumes);
		}
		return this;
	}

	/**
	 * Synonym for {@link #addConsumes(MediaType...)}.
	 *
	 * @param consumes The new values to add to the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation consumes(MediaType...consumes) {
		return addConsumes(consumes);
	}

	/**
	 * Synonym for {@link #addConsumes(Collection)}.
	 *
	 * @param consumes The new values to add to the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation consumes(Collection<MediaType> consumes) {
		return addConsumes(consumes);
	}

	/**
	 * Bean property getter:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the operation can produce.
	 * This overrides the <code>produces</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a class="doclink"
	 * href="http://swagger.io/specification/#mimeTypes">Mime Types</a>.
	 *
	 * @return The value of the <property>produces</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<MediaType> getProduces() {
		return produces;
	}

	/**
	 * Bean property setter:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the operation can produce.
	 * This overrides the <code>produces</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a class="doclink"
	 * href="http://swagger.io/specification/#mimeTypes">Mime Types</a>.
	 *
	 * @param produces The new value for the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setProduces(List<MediaType> produces) {
		this.produces = produces;
		return this;
	}

	/**
	 * Bean property adder:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the operation can produce.
	 * This overrides the <code>produces</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a class="doclink"
	 * href="http://swagger.io/specification/#mimeTypes">Mime Types</a>.
	 *
	 * @param produces The new value for the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation addProduces(MediaType...produces) {
		if (produces != null) {
			if (this.produces == null)
				this.produces = new LinkedList<MediaType>();
			this.produces.addAll(Arrays.asList(produces));
		}
		return this;
	}

	/**
	 * Bean property adder:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the operation can produces.
	 * This overrides the <code>produces</code> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under <a class="doclink"
	 * href="http://swagger.io/specification/#mimeTypes">Mime Types</a>.
	 *
	 * @param produces The new values to add to the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation addProduces(Collection<MediaType> produces) {
		if (produces != null) {
			if (this.produces == null)
				this.produces = new LinkedList<MediaType>();
			this.produces.addAll(produces);
		}
		return this;
	}

	/**
	 * Synonym for {@link #addProduces(MediaType...)}.
	 *
	 * @param produces The new value for the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation produces(MediaType...produces) {
		return addProduces(produces);
	}

	/**
	 * Synonym for {@link #addProduces(Collection)}.
	 *
	 * @param produces The new value for the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation produces(Collection<MediaType> produces) {
		return addProduces(produces);
	}

	/**
	 * Bean property getter:  <property>parameters</property>.
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 * If a parameter is already defined at the <a class="doclink"
	 * href="http://swagger.io/specification/#pathItemParameters">Path Item</a>, the new definition will override it,
	 * but can never remove it.
	 * The list MUST NOT include duplicated parameters.
	 * A unique parameter is defined by a combination of a <code>name</code> and <code>location</code>.
	 * The list can use the <a class="doclink"
	 * href="http://swagger.io/specification/#referenceObject">Reference Object</a> to link to parameters that are
	 * defined at the <a class="doclink" href="http://swagger.io/specification/#swaggerParameters">Swagger Object's
	 * parameters</a>.
	 * There can be one <js>"body"</js> parameter at most.
	 *
	 * @return The value of the <property>parameters</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<ParameterInfo> getParameters() {
		return parameters;
	}

	/**
	 * Bean property setter:  <property>parameters</property>.
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 * If a parameter is already defined at the <a class="doclink"
	 * href="http://swagger.io/specification/#pathItemParameters">Path Item</a>, the new definition will override it,
	 * but can never remove it.
	 * The list MUST NOT include duplicated parameters.
	 * A unique parameter is defined by a combination of a <code>name</code> and <code>location</code>.
	 * The list can use the <a class="doclink"
	 * href="http://swagger.io/specification/#referenceObject">Reference Object</a> to link to parameters that are
	 * defined at the <a class="doclink"
	 * href="http://swagger.io/specification/#swaggerParameters">Swagger Object's parameters</a>.
	 * There can be one <js>"body"</js> parameter at most.
	 *
	 * @param parameters The new value for the <property>parameters</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation setParameters(List<ParameterInfo> parameters) {
		this.parameters = parameters;
		return this;
	}

	/**
	 * Bean property adder:  <property>parameters</property>.
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 * If a parameter is already defined at the <a class="doclink"
	 * href="http://swagger.io/specification/#pathItemParameters">Path Item</a>, the new definition will override it,
	 * but can never remove it.
	 * The list MUST NOT include duplicated parameters.
	 * A unique parameter is defined by a combination of a <code>name</code> and <code>location</code>.
	 * The list can use the <a class="doclink"
	 * href="http://swagger.io/specification/#referenceObject">Reference Object</a> to link to parameters that are
	 * defined at the <a class="doclink"
	 * href="http://swagger.io/specification/#swaggerParameters">Swagger Object's parameters</a>.
	 * There can be one <js>"body"</js> parameter at most.
	 *
	 * @param parameters The new value to add to the <property>parameters</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation addParameters(ParameterInfo...parameters) {
		if (this.parameters == null)
			this.parameters = new LinkedList<ParameterInfo>();
		this.parameters.addAll(Arrays.asList(parameters));
		return this;
	}

	/**
	 * Synonym for {@link #addParameters(ParameterInfo...)}.
	 *
	 * @param parameters The new value to add to the <property>parameters</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation parameters(ParameterInfo...parameters) {
		return addParameters(parameters);
	}

	/**
	 * Synonym for {@link #setParameters(List)}.
	 *
	 * @param parameters The new value to add to the <property>parameters</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation parameters(List<ParameterInfo> parameters) {
		return setParameters(parameters);
	}

	/**
	 * Bean property getter:  <property>responses</property>.
	 * <p>
	 * Required. The list of possible responses as they are returned from executing this operation.
	 *
	 * @return The value of the <property>responses</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<Integer,ResponseInfo> getResponses() {
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
	public Operation setResponses(Map<Integer,ResponseInfo> responses) {
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
	public Operation addResponse(Integer statusCode, ResponseInfo response) {
		if (responses == null)
			responses = new TreeMap<Integer,ResponseInfo>();
		responses.put(statusCode, response);
		return this;
	}

	/**
	 * Synonym for {@link #addResponse(Integer,ResponseInfo)}.
	 *
	 * @param statusCode The HTTP status code.
	 * @param response The response description.
	 * @return This object (for method chaining).
	 */
	public Operation response(Integer statusCode, ResponseInfo response) {
		return addResponse(statusCode, response);
	}

	/**
	 * Synonym for {@link #setResponses(Map)}.
	 *
	 * @param responses The new value for the <property>responses</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation responses(Map<Integer,ResponseInfo> responses) {
		return setResponses(responses);
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
	public Operation addSchemes(Collection<String> schemes) {
		if (this.schemes == null)
			this.schemes = new LinkedList<String>();
		this.schemes.addAll(schemes);
		return this;
	}

	/**
	 * Synonym for {@link #addSchemes(String...)}.
	 *
	 * @param schemes The new values to add to the <property>schemes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation schemes(String...schemes) {
		return addSchemes(schemes);
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
	 * Synonym for {@link #setDeprecated(Boolean)}.
	 *
	 * @param deprecated The new value for the <property>deprecated</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation deprecated(Boolean deprecated) {
		return setDeprecated(deprecated);
	}

	/**
	 * Bean property getter:  <property>security</property>.
	 * <p>
	 * A declaration of which security schemes are applied for this operation.
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * between the security requirements).
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
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * between the security requirements).
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
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * between the security requirements).
	 * This definition overrides any declared top-level security.
	 * To remove a top-level <code>security</code> declaration, an empty array can be used.
	 *
	 * @param security The new value to add to the <property>security</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Operation addSecurity(Map<String,List<String>> security) {
		if (this.security == null)
			this.security = new LinkedList<Map<String,List<String>>>();
		this.security.add(security);
		return this;
	}

	/**
	 * Synonym for {@link #addSecurity(Map)}.
	 *
	 * @param scheme The security scheme that applies to this operation
	 * @param alternatives The list of values describes alternative security schemes that can be used (that is, there is
	 * a logical OR between the security requirements).
	 * @return This object (for method chaining).
	 */
	public Operation security(String scheme, String...alternatives) {
		Map<String,List<String>> m = new LinkedHashMap<String,List<String>>();
		m.put(scheme, Arrays.asList(alternatives));
		return addSecurity(m);
	}
}
