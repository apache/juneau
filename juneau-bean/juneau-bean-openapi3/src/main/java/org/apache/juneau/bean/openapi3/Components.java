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
 * Holds a set of reusable objects for different aspects of the OAS.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.OpenApi">Overview &gt; juneau-rest-server &gt; OpenAPI</a>
 * </ul>
 */
@Bean(properties="schemas,responses,parameters,examples,requestBodies,headers,securitySchemes,links,callbacks,*")
@FluentSetters
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
	 * Returns the schemas map.
	 *
	 * @return The schemas map.
	 */
	public Map<String,SchemaInfo> getSchemas() {
		return schemas;
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
	public Components setResponses(Map<String,Response> value) {
		this.responses = value;
		return this;
	}

	/**
	 * Returns the parameters map.
	 *
	 * @return The parameters map.
	 */
	public Map<String,Parameter> getParameters() {
		return parameters;
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
	 * Returns the examples map.
	 *
	 * @return The examples map.
	 */
	public Map<String,Example> getExamples() {
		return examples;
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
	 * Returns the request bodies map.
	 *
	 * @return The request bodies map.
	 */
	public Map<String,RequestBodyInfo> getRequestBodies() {
		return requestBodies;
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
	 * Returns the headers map.
	 *
	 * @return The headers map.
	 */
	public Map<String,HeaderInfo> getHeaders() {
		return headers;
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
	 * Returns the security schemes map.
	 *
	 * @return The security schemes map.
	 */
	public Map<String,SecuritySchemeInfo> getSecuritySchemes() {
		return securitySchemes;
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

	/**
	 * Returns the links map.
	 *
	 * @return The links map.
	 */
	public Map<String,Link> getLinks() {
		return links;
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
	public Components setCallbacks(Map<String,Callback> value) {
		this.callbacks = value;
		return this;
	}
}
