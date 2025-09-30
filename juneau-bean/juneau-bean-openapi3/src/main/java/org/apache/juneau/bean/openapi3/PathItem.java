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
 * Describes the operations available on a single path.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.OpenApi">Overview &gt; juneau-rest-server &gt; OpenAPI</a>
 * </ul>
 */
@Bean(properties="summary,description,get,put,post,delete,options,head,patch,trace,servers,parameters,*")
@FluentSetters
public class PathItem extends OpenApiElement {

	private String summary, description;
	private Operation get, put, post, delete, options, head, patch, trace;
	private List<Server> servers;
	private List<Parameter> parameters;

	/**
	 * Default constructor.
	 */
	public PathItem() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public PathItem(PathItem copyFrom) {
		super(copyFrom);
		this.summary = copyFrom.summary;
		this.description = copyFrom.description;
		this.get = copyFrom.get;
		this.put = copyFrom.put;
		this.post = copyFrom.post;
		this.delete = copyFrom.delete;
		this.options = copyFrom.options;
		this.head = copyFrom.head;
		this.patch = copyFrom.patch;
		this.trace = copyFrom.trace;
		this.servers = copyOf(copyFrom.servers);
		this.parameters = copyOf(copyFrom.parameters);
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
	public PathItem setSummary(String value) {
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
	public PathItem setDescription(String value) {
		this.description = value;
		return this;
	}

	/**
	 * Returns the GET operation.
	 *
	 * @return The GET operation.
	 */
	public Operation getGet() {
		return get;
	}

	/**
	 * Sets the GET operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setGet(Operation value) {
		this.get = value;
		return this;
	}

	/**
	 * Returns the PUT operation.
	 *
	 * @return The PUT operation.
	 */
	public Operation getPut() {
		return put;
	}

	/**
	 * Sets the PUT operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setPut(Operation value) {
		this.put = value;
		return this;
	}

	/**
	 * Returns the POST operation.
	 *
	 * @return The POST operation.
	 */
	public Operation getPost() {
		return post;
	}

	/**
	 * Sets the POST operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setPost(Operation value) {
		this.post = value;
		return this;
	}

	/**
	 * Returns the DELETE operation.
	 *
	 * @return The DELETE operation.
	 */
	public Operation getDelete() {
		return delete;
	}

	/**
	 * Sets the DELETE operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setDelete(Operation value) {
		this.delete = value;
		return this;
	}

	/**
	 * Returns the OPTIONS operation.
	 *
	 * @return The OPTIONS operation.
	 */
	public Operation getOptions() {
		return options;
	}

	/**
	 * Sets the OPTIONS operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setOptions(Operation value) {
		this.options = value;
		return this;
	}

	/**
	 * Returns the HEAD operation.
	 *
	 * @return The HEAD operation.
	 */
	public Operation getHead() {
		return head;
	}

	/**
	 * Sets the HEAD operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setHead(Operation value) {
		this.head = value;
		return this;
	}

	/**
	 * Returns the PATCH operation.
	 *
	 * @return The PATCH operation.
	 */
	public Operation getPatch() {
		return patch;
	}

	/**
	 * Sets the PATCH operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setPatch(Operation value) {
		this.patch = value;
		return this;
	}

	/**
	 * Returns the TRACE operation.
	 *
	 * @return The TRACE operation.
	 */
	public Operation getTrace() {
		return trace;
	}

	/**
	 * Sets the TRACE operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setTrace(Operation value) {
		this.trace = value;
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
	public PathItem setServers(List<Server> value) {
		this.servers = value;
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
	public PathItem setParameters(List<Parameter> value) {
		this.parameters = value;
		return this;
	}
}
