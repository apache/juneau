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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Describes a single API operation on a path.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.OpenApi">Overview &gt; juneau-rest-server &gt; OpenAPI</a>
 * </ul>
 */
@Bean(properties="tags,summary,description,externalDocs,operationId,parameters,requestBody,responses,callbacks,deprecated,security,servers,*")
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
}
