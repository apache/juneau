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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Describes a single API operation on a path.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Operation <jv>operation</jv> = <jsm>operation</jsm>()
 * 		.tags(<js>"pet"</js>)
 * 		.summary(<js>"Updates a pet in the store with form data"</js>)
 * 		.description(<js>""</js>)
 * 		.operationId(<js>"updatePetWithForm"</js>)
 * 		.consumes(<js>"application/x-www-form-urlencoded"</js>)
 * 		.produces(<js>"application/json"</js>, <js>"application/xml"</js>)
 * 		.parameters(
 * 			<jsm>parameter</jsm>()
 * 				.name(<js>"petId"</js>)
 * 				.in(<js>"path"</js>)
 * 				.description(<js>"ID of pet that needs to be updated"</js>)
 * 				.required(<jk>true</jk>)
 * 				.type(<js>"string"</js>),
 * 			<jsm>parameter</jsm>()
 * 				.name(<js>"name"</js>)
 * 				.in(<js>"formData"</js>)
 * 				.description(<js>"Updated name of the pet"</js>)
 * 				.required(<jk>false</jk>)
 * 				.type(<js>"string"</js>),
 * 			<jsm>parameter</jsm>()
 * 				.name(<js>"status"</js>)
 * 				.in(<js>"formData"</js>)
 * 				.description(<js>"Updated status of the pet"</js>)
 * 				.required(<jk>false</jk>)
 * 				.type(<js>"string"</js>)
 * 		)
 * 		.response(200, <jsm>responseInfo</jsm>(<js>"Pet updated."</js>))
 * 		.response(405, <jsm>responseInfo</jsm>(<js>"Invalid input."</js>))
 * 		.security(<js>"petstore_auth"</js>, <js>"write:pets"</js>, <js>"read:pets"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.toString(<jv>operation</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>operation</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"tags"</js>: [
 * 			<js>"pet"</js>
 * 		],
 * 		<js>"summary"</js>: <js>"Updates a pet in the store with form data"</js>,
 * 		<js>"description"</js>: <js>""</js>,
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="operationId,summary,description,tags,externalDocs,consumes,produces,parameters,responses,schemes,deprecated,security,*")
@FluentSetters
public class Operation extends SwaggerElement {

	private String
		summary,
		description,
		operationId;
	private Boolean deprecated;
	private ExternalDocumentation externalDocs;
	private Set<String>
		tags,
		schemes;
	private Set<MediaType>
		consumes,
		produces;
	private List<ParameterInfo> parameters;
	private List<Map<String,List<String>>> security;
	private Map<String,ResponseInfo> responses;

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

		this.consumes = copyOf(copyFrom.consumes);
		this.deprecated = copyFrom.deprecated;
		this.description = copyFrom.description;
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this.operationId = copyFrom.operationId;
		this.produces = copyOf(copyFrom.produces);
		this.schemes = copyOf(copyFrom.schemes);
		this.summary = copyFrom.summary;
		this.tags = copyOf(copyFrom.tags);

		if (copyFrom.parameters == null) {
			this.parameters = null;
		} else {
			this.parameters = list();
			copyFrom.parameters.forEach(x -> this.parameters.add(x.copy()));
		}

		if (copyFrom.responses == null) {
			this.responses = null;
		} else {
			this.responses = map();
			copyFrom.responses.forEach((k,v) -> this.responses.put(k, v.copy()));
		}

		if (copyFrom.security == null) {
			this.security = null;
		} else {
			this.security = list();
			copyFrom.security.forEach(x -> {
				Map<String,List<String>> m2 = map();
				x.forEach((k,v) -> m2.put(k, copyOf(v)));
				this.security.add(m2);
			});
		}
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Operation copy() {
		return new Operation(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<MediaType> getConsumes() {
		return consumes;
	}

	/**
	 * Bean property setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Values MUST be as described under <a class="doclink" href="https://swagger.io/specification#mimeTypes">Swagger Mime Types</a>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setConsumes(Collection<MediaType> value) {
		consumes = setFrom(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Values MUST be as described under <a class="doclink" href="https://swagger.io/specification#mimeTypes">Swagger Mime Types</a>.
	 * @return This object.
	 */
	public Operation setConsumes(MediaType...value) {
		return setConsumes(Arrays.asList(value));
	}

	/**
	 * Bean property fluent setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Operation addConsumes(MediaType...value) {
		setConsumes(setBuilder(MediaType.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Bean property getter:  <property>deprecated</property>.
	 *
	 * <p>
	 * Declares this operation to be deprecated.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getDeprecated() {
		return deprecated;
	}

	/**
	 * Bean property getter:  <property>deprecated</property>.
	 *
	 * <p>
	 * Declares this operation to be deprecated.
	 *
	 * @return The property value, or <jk>false</jk> if it is not set.
	 */
	public boolean isDeprecated() {
		return deprecated != null && deprecated == true;
	}

	/**
	 * Bean property setter:  <property>deprecated</property>.
	 *
	 * <p>
	 * Declares this operation to be deprecated.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Operation setDeprecated(Boolean value) {
		deprecated = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A verbose explanation of the operation behavior.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * <p>
	 * A verbose explanation of the operation behavior.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation for this operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	/**
	 * Bean property setter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation for this operation.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>operationId</property>.
	 *
	 * <p>
	 * Unique string used to identify the operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getOperationId() {
		return operationId;
	}

	/**
	 * Bean property setter:  <property>operationId</property>.
	 *
	 * <p>
	 * Unique string used to identify the operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The id MUST be unique among all operations described in the API.
	 * 	<br>Tools and libraries MAY use the operationId to uniquely identify an operation, therefore, it is recommended to
	 * 	follow common programming naming conventions.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setOperationId(String value) {
		operationId = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If a parameter is already defined at the <a class="doclink" href="https://swagger.io/specification#pathItemObject">Path Item</a>,
	 * 		the new definition will override it, but can never remove it.
	 * 	<li class='note'>
	 * 		The list MUST NOT include duplicated parameters.
	 * 	<li class='note'>
	 * 		A unique parameter is defined by a combination of a <c>name</c> and <c>location</c>.
	 * 	<li class='note'>
	 * 		The list can use the <a class="doclink" href="https://swagger.io/specification#referenceObject">Swagger Reference Object</a>
	 * 		to link to parameters that are defined at the <a class='doclink' href='https://swagger.io/specification/v2#parameterObject'>Swagger Object's parameters</a>.
	 * 	<li class='note'>
	 * 		There can be one <js>"body"</js> parameter at most.
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<ParameterInfo> getParameters() {
		return parameters;
	}

	/**
	 * Returns the parameter with the specified type and name.
	 *
	 * @param in The parameter in.
	 * @param name The parameter name.  Can be <jk>null</jk> for parameter type <c>body</c>.
	 * @return The matching parameter info, or <jk>null</jk> if not found.
	 */
	public ParameterInfo getParameter(String in, String name) {
		if (parameters != null)
			for (ParameterInfo pi : parameters)
				if (eq(pi.getIn(), in))
					if (eq(pi.getName(), name) || "body".equals(pi.getIn()))
						return pi;
		return null;
	}

	/**
	 * Bean property setter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setParameters(Collection<ParameterInfo> value) {
		parameters = listFrom(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Operation setParameters(ParameterInfo...value) {
		return setParameters(Arrays.asList(value));
	}

	/**
	 * Bean property fluent setter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Operation addParameters(ParameterInfo...value) {
		setParameters(listBuilder(ParameterInfo.class).sparse().add(value).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// produces
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<MediaType> getProduces() {
		return produces;
	}

	/**
	 * Bean property setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Value MUST be as described under <a class="doclink" href="https://swagger.io/specification#mimeTypes">Swagger Mime Types</a>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setProduces(Collection<MediaType> value) {
		produces = setFrom(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Value MUST be as described under <a class="doclink" href="https://swagger.io/specification#mimeTypes">Swagger Mime Types</a>.
	 * @return This object.
	 */
	public Operation setProduces(MediaType...value) {
		return setProduces(Arrays.asList(value));
	}

	/**
	 * Bean property fluent setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Operation addProduces(MediaType...value) {
		setProduces(setBuilder(MediaType.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Bean property getter:  <property>responses</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,ResponseInfo> getResponses() {
		return responses;
	}

	/**
	 * Returns the response info with the given status code.
	 *
	 * @param status The HTTP status code.
	 * @return The response info, or <jk>null</jk> if not found.
	 */
	public ResponseInfo getResponse(String status) {
		if (responses != null)
			return responses.get(status);
		return null;
	}

	/**
	 * Returns the response info with the given status code.
	 *
	 * @param status The HTTP status code.
	 * @return The response info, or <jk>null</jk> if not found.
	 */
	public ResponseInfo getResponse(int status) {
		return getResponse(String.valueOf(status));
	}

	/**
	 * Bean property setter:  <property>responses</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object.
	 */
	public Operation setResponses(Map<String,ResponseInfo> value) {
		responses = copyOf(value);
		return this;
	}

	/**
	 * Adds a single value to the <property>responses</property> property.
	 *
	 * @param statusCode The HTTP status code.
	 * @param response The response description.
	 * @return This object.
	 */
	public Operation addResponse(String statusCode, ResponseInfo response) {
		responses = mapBuilder(responses).add(statusCode, response).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol for the operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<String> getSchemes() {
		return schemes;
	}

	/**
	 * Bean property setter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol for the operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"http"</js>
	 * 		<li><js>"https"</js>
	 * 		<li><js>"ws"</js>
	 * 		<li><js>"wss"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setSchemes(Collection<String> value) {
		schemes = setFrom(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol for the operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>String values can also be JSON arrays.
	 * @return This object.
	 */
	public Operation addSchemes(String...value) {
		setSchemes(setBuilder(String.class).sparse().addJson(value).build());
		return this;
	}

	/**
	 * Bean property getter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for this operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Map<String,List<String>>> getSecurity() {
		return security;
	}

	/**
	 * Bean property setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setSecurity(Collection<Map<String,List<String>>> value) {
		security = listFrom(value);
		return this;
	}

	/**
	 * Same as {@link #addSecurity(String, String...)}.
	 *
	 * @param scheme
	 * 	The scheme name.
	 * @param alternatives
	 * 	The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * 	between the security requirements).
	 * @return This object.
	 */
	public Operation addSecurity(String scheme, String...alternatives) {
		Map<String,List<String>> m = map();
		m.put(scheme, alist(alternatives));
		security = listBuilder(security).add(m).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>summary</property>.
	 *
	 * <p>
	 * A short summary of what the operation does.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * Bean property setter:  <property>summary</property>.
	 *
	 * <p>
	 * A short summary of what the operation does.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setSummary(String value) {
		summary = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags for API documentation control.
	 * <br>Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<String> getTags() {
		return tags;
	}

	/**
	 * Bean property setter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags for API documentation control.
	 * <br>Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Operation setTags(Collection<String> value) {
		tags = setFrom(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags for API documentation control.
	 * <br>Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Operation setTags(String...value) {
		setTags(setBuilder(String.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Bean property fluent adder:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags for API documentation control.
	 * <br>Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * @return This object.
	 */
	public Operation addTags(String...value) {
		setTags(setBuilder(tags).sparse().add(value).build());
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "consumes": return toType(getConsumes(), type);
			case "deprecated": return toType(getDeprecated(), type);
			case "description": return toType(getDescription(), type);
			case "externalDocs": return toType(getExternalDocs(), type);
			case "operationId": return toType(getOperationId(), type);
			case "parameters": return toType(getParameters(), type);
			case "produces": return toType(getProduces(), type);
			case "responses": return toType(getResponses(), type);
			case "schemes": return toType(getSchemes(), type);
			case "security": return toType(getSecurity(), type);
			case "summary": return toType(getSummary(), type);
			case "tags": return toType(getTags(), type);
			default: return super.get(property, type);
		}
	}

	@SuppressWarnings({"unchecked","rawtypes"})
	@Override /* SwaggerElement */
	public Operation set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "consumes": return setConsumes(listBuilder(MediaType.class).sparse().addAny(value).build());
			case "deprecated": return setDeprecated(toBoolean(value));
			case "description": return setDescription(stringify(value));
			case "externalDocs": return setExternalDocs(toType(value, ExternalDocumentation.class));
			case "operationId": return setOperationId(stringify(value));
			case "parameters": return setParameters(listBuilder(ParameterInfo.class).sparse().addAny(value).build());
			case "produces": return setProduces(listBuilder(MediaType.class).sparse().addAny(value).build());
			case "responses": return setResponses(mapBuilder(String.class,ResponseInfo.class).sparse().addAny(value).build());
			case "schemes": return setSchemes(listBuilder(String.class).sparse().addAny(value).build());
			case "security": return setSecurity((List)listBuilder(Map.class,String.class,List.class,String.class).sparse().addAny(value).build());
			case "summary": return setSummary(stringify(value));
			case "tags": return setTags(listBuilder(String.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(consumes != null, "consumes")
			.addIf(deprecated != null, "deprecated")
			.addIf(description != null, "description")
			.addIf(externalDocs != null, "externalDocs")
			.addIf(operationId != null, "operationId")
			.addIf(parameters != null, "parameters")
			.addIf(produces != null, "produces")
			.addIf(responses != null, "responses")
			.addIf(schemes != null, "schemes")
			.addIf(security != null, "security")
			.addIf(summary != null, "summary")
			.addIf(tags != null, "tags")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
