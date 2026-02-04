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
@SuppressWarnings("java:S115")
public class PathItem extends OpenApiElement {

	// Argument name constants for assertArgNotNull
	private static final String ARG_property = "property";

	// Property name constants
	private static final String PROP_delete = "delete";
	private static final String PROP_description = "description";
	private static final String PROP_get = "get";
	private static final String PROP_head = "head";
	private static final String PROP_options = "options";
	private static final String PROP_parameters = "parameters";
	private static final String PROP_patch = "patch";
	private static final String PROP_post = "post";
	private static final String PROP_put = "put";
	private static final String PROP_servers = "servers";
	private static final String PROP_summary = "summary";
	private static final String PROP_trace = "trace";

	private String summary;
	private String description;
	private Operation get;
	private Operation put;
	private Operation post;
	private Operation delete;
	private Operation options;
	private Operation head;
	private Operation patch;
	private Operation trace;
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
	 * Creates a copy of this object.
	 *
	 * @return A copy of this object.
	 */
	public PathItem copy() {
		return new PathItem(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_summary -> toType(getSummary(), type);
			case PROP_description -> toType(getDescription(), type);
			case PROP_get -> toType(getGet(), type);
			case PROP_put -> toType(getPut(), type);
			case PROP_post -> toType(getPost(), type);
			case PROP_delete -> toType(getDelete(), type);
			case PROP_options -> toType(getOptions(), type);
			case PROP_head -> toType(getHead(), type);
			case PROP_patch -> toType(getPatch(), type);
			case PROP_trace -> toType(getTrace(), type);
			case PROP_servers -> toType(getServers(), type);
			case PROP_parameters -> toType(getParameters(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Returns the DELETE operation.
	 *
	 * @return The DELETE operation.
	 */
	public Operation getDelete() { return delete; }

	/**
	 * Returns the description.
	 *
	 * @return The description.
	 */
	public String getDescription() { return description; }

	/**
	 * Returns the GET operation.
	 *
	 * @return The GET operation.
	 */
	public Operation getGet() { return get; }

	/**
	 * Returns the HEAD operation.
	 *
	 * @return The HEAD operation.
	 */
	public Operation getHead() { return head; }

	/**
	 * Returns the OPTIONS operation.
	 *
	 * @return The OPTIONS operation.
	 */
	public Operation getOptions() { return options; }

	/**
	 * Returns the parameters list.
	 *
	 * @return The parameters list.
	 */
	public List<Parameter> getParameters() { return parameters; }

	/**
	 * Returns the PATCH operation.
	 *
	 * @return The PATCH operation.
	 */
	public Operation getPatch() { return patch; }

	/**
	 * Returns the POST operation.
	 *
	 * @return The POST operation.
	 */
	public Operation getPost() { return post; }

	/**
	 * Returns the PUT operation.
	 *
	 * @return The PUT operation.
	 */
	public Operation getPut() { return put; }

	/**
	 * Returns the servers list.
	 *
	 * @return The servers list.
	 */
	public List<Server> getServers() { return servers; }

	/**
	 * Returns the summary.
	 *
	 * @return The summary.
	 */
	public String getSummary() { return summary; }

	/**
	 * Returns the TRACE operation.
	 *
	 * @return The TRACE operation.
	 */
	public Operation getTrace() { return trace; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(delete), PROP_delete)
			.addIf(nn(description), PROP_description)
			.addIf(nn(get), PROP_get)
			.addIf(nn(head), PROP_head)
			.addIf(nn(options), PROP_options)
			.addIf(nn(parameters), PROP_parameters)
			.addIf(nn(patch), PROP_patch)
			.addIf(nn(post), PROP_post)
			.addIf(nn(put), PROP_put)
			.addIf(nn(servers), PROP_servers)
			.addIf(nn(summary), PROP_summary)
			.addIf(nn(trace), PROP_trace)
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public PathItem set(String property, Object value) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_delete -> setDelete(toType(value, Operation.class));
			case PROP_description -> setDescription(s(value));
			case PROP_get -> setGet(toType(value, Operation.class));
			case PROP_head -> setHead(toType(value, Operation.class));
			case PROP_options -> setOptions(toType(value, Operation.class));
			case PROP_patch -> setPatch(toType(value, Operation.class));
			case PROP_parameters -> setParameters(listb(Parameter.class).addAny(value).sparse().build());
			case PROP_post -> setPost(toType(value, Operation.class));
			case PROP_put -> setPut(toType(value, Operation.class));
			case PROP_servers -> setServers(listb(Server.class).addAny(value).sparse().build());
			case PROP_summary -> setSummary(s(value));
			case PROP_trace -> setTrace(toType(value, Operation.class));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Sets the DELETE operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setDelete(Operation value) {
		delete = value;
		return this;
	}

	/**
	 * Sets the description.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Sets the GET operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setGet(Operation value) {
		get = value;
		return this;
	}

	/**
	 * Sets the HEAD operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setHead(Operation value) {
		head = value;
		return this;
	}

	/**
	 * Sets the OPTIONS operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setOptions(Operation value) {
		options = value;
		return this;
	}

	/**
	 * Sets the parameters list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setParameters(List<Parameter> value) {
		parameters = value;
		return this;
	}

	/**
	 * Sets the PATCH operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setPatch(Operation value) {
		patch = value;
		return this;
	}

	/**
	 * Sets the POST operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setPost(Operation value) {
		post = value;
		return this;
	}

	/**
	 * Sets the PUT operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setPut(Operation value) {
		put = value;
		return this;
	}

	/**
	 * Sets the servers list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setServers(List<Server> value) {
		servers = value;
		return this;
	}

	/**
	 * Sets the summary.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setSummary(String value) {
		summary = value;
		return this;
	}

	/**
	 * Sets the TRACE operation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public PathItem setTrace(Operation value) {
		trace = value;
		return this;
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