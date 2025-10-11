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
 * Describes the operations available on a single path.
 *
 * <p>
 * The PathItem Object describes the operations available on a single path. A Path Item may be empty, due to ACL 
 * constraints. The path itself is still exposed to the documentation viewer but they will not know which operations 
 * and parameters are available.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The PathItem Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>summary</c> (string) - An optional, string summary, intended to apply to all operations in this path
 * 	<li><c>description</c> (string) - An optional, string description, intended to apply to all operations in this path
 * 	<li><c>get</c> ({@link Operation}) - A definition of a GET operation on this path
 * 	<li><c>put</c> ({@link Operation}) - A definition of a PUT operation on this path
 * 	<li><c>post</c> ({@link Operation}) - A definition of a POST operation on this path
 * 	<li><c>delete</c> ({@link Operation}) - A definition of a DELETE operation on this path
 * 	<li><c>options</c> ({@link Operation}) - A definition of an OPTIONS operation on this path
 * 	<li><c>head</c> ({@link Operation}) - A definition of a HEAD operation on this path
 * 	<li><c>patch</c> ({@link Operation}) - A definition of a PATCH operation on this path
 * 	<li><c>trace</c> ({@link Operation}) - A definition of a TRACE operation on this path
 * 	<li><c>servers</c> (array of {@link Server}) - An alternative server array to service all operations in this path
 * 	<li><c>parameters</c> (array of {@link Parameter}) - A list of parameters that are applicable for all the operations described under this path
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	PathItem <jv>x</jv> = <jsm>pathItem</jsm>()
 * 		.setSummary(<js>"User management"</js>)
 * 		.setGet(<jsm>operation</jsm>().setSummary(<js>"Get users"</js>))
 * 		.setPost(<jsm>operation</jsm>().setSummary(<js>"Create user"</js>));
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>x</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>x</jv>.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"summary"</js>: <js>"User management"</js>,
 * 		<js>"get"</js>: { <js>"summary"</js>: <js>"Get users"</js> },
 * 		<js>"post"</js>: { <js>"summary"</js>: <js>"Create user"</js> }
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#path-item-object">OpenAPI Specification &gt; Path Item Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/paths-and-operations/">OpenAPI Paths and Operations</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
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

	/**
	 * Creates a copy of this object.
	 *
	 * @return A copy of this object.
	 */
	public PathItem copy() {
		return new PathItem(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "summary" -> toType(getSummary(), type);
			case "description" -> toType(getDescription(), type);
			case "get" -> toType(getGet(), type);
			case "put" -> toType(getPut(), type);
			case "post" -> toType(getPost(), type);
			case "delete" -> toType(getDelete(), type);
			case "options" -> toType(getOptions(), type);
			case "head" -> toType(getHead(), type);
			case "patch" -> toType(getPatch(), type);
			case "trace" -> toType(getTrace(), type);
			case "servers" -> toType(getServers(), type);
			case "parameters" -> toType(getParameters(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* Overridden from OpenApiElement */
	public PathItem set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "delete" -> setDelete(toType(value, Operation.class));
			case "description" -> setDescription(Utils.s(value));
			case "get" -> setGet(toType(value, Operation.class));
			case "head" -> setHead(toType(value, Operation.class));
			case "options" -> setOptions(toType(value, Operation.class));
			case "patch" -> setPatch(toType(value, Operation.class));
			case "parameters" -> setParameters(listBuilder(Parameter.class).sparse().addAny(value).build());
			case "post" -> setPost(toType(value, Operation.class));
			case "put" -> setPut(toType(value, Operation.class));
			case "servers" -> setServers(listBuilder(Server.class).sparse().addAny(value).build());
			case "summary" -> setSummary(Utils.s(value));
			case "trace" -> setTrace(toType(value, Operation.class));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(delete != null, "delete")
			.addIf(description != null, "description")
			.addIf(get != null, "get")
			.addIf(head != null, "head")
			.addIf(options != null, "options")
			.addIf(patch != null, "patch")
			.addIf(parameters != null, "parameters")
			.addIf(post != null, "post")
			.addIf(put != null, "put")
			.addIf(servers != null, "servers")
			.addIf(summary != null, "summary")
			.addIf(trace != null, "trace")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public PathItem strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public PathItem strict(Object value) {
		super.strict(value);
		return this;
	}

}