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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;

/**
 * Describes a single API operation on a path.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
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
 * <p class='bcode w800'>
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoSwagger}
 * </ul>
 */
@Bean(properties="operationId,summary,description,tags,externalDocs,consumes,produces,parameters,responses,schemes,deprecated,security,*")
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

		this.consumes = newSet(copyFrom.consumes);
		this.deprecated = copyFrom.deprecated;
		this.description = copyFrom.description;
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this.operationId = copyFrom.operationId;
		this.produces = newSet(copyFrom.produces);
		this.schemes = newSet(copyFrom.schemes);
		this.summary = copyFrom.summary;
		this.tags = newSet(copyFrom.tags);

		if (copyFrom.parameters == null) {
			this.parameters = null;
		} else {
			this.parameters = new ArrayList<>();
			for (ParameterInfo p : copyFrom.parameters)
				this.parameters.add(p.copy());
		}

		if (copyFrom.responses == null) {
			this.responses = null;
		} else {
			this.responses = new LinkedHashMap<>();
			for (Map.Entry<String,ResponseInfo> e : copyFrom.responses.entrySet())
				this.responses.put(e.getKey(), e.getValue().copy());
		}

		if (copyFrom.security == null) {
			this.security = null;
		} else {
			this.security = new ArrayList<>();
			for (Map<String,List<String>> m : copyFrom.security) {
				Map<String,List<String>> m2 = new LinkedHashMap<>();
				for (Map.Entry<String,List<String>> e : m.entrySet())
					m2.put(e.getKey(), newList(e.getValue()));
				this.security.add(m2);
			}
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
	// consumes
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
	 * 	<br>Values MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setConsumes(Collection<MediaType> value) {
		consumes = newSet(value);
	}

	/**
	 * Bean property adder:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Values MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void addConsumes(Collection<MediaType> value) {
		consumes = addToSet(consumes, value);
	}

	/**
	 * Bean property fluent getter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<MediaType>> consumes() {
		return Optional.ofNullable(getConsumes());
	}

	/**
	 * Bean property fluent setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation consumes(MediaType...value) {
		setConsumes(toSet(value, MediaType.class));
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation consumes(Collection<MediaType> value) {
		setConsumes(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * @param value
	 * 	The new value for this property.
	 * <br>Values can also be JSON arrays.
	 * @return This object (for method chaining).
	 */
	public Operation consumes(String...value) {
		setConsumes(toSet(value, MediaType.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// deprecated
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setDeprecated(Boolean value) {
		deprecated = value;
	}

	/**
	 * Bean property fluent getter:  <property>deprecated</property>.
	 *
	 * <p>
	 * Declares this operation to be deprecated.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Boolean> deprecated() {
		return Optional.ofNullable(getDeprecated());
	}

	/**
	 * Bean property fluent setter:  <property>deprecated</property>.
	 *
	 * <p>
	 * Declares this operation to be deprecated.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation deprecated(Boolean value) {
		setDeprecated(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>deprecated</property>.
	 *
	 * <p>
	 * Declares this operation to be deprecated.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation deprecated(String value) {
		setDeprecated(toBoolean(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// description
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	<br>{@doc ExtGFM} can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setDescription(String value) {
		description = value;
	}

	/**
	 * Bean property fluent getter:  <property>description</property>.
	 *
	 * <p>
	 * A verbose explanation of the operation behavior.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> description() {
		return Optional.ofNullable(getDescription());
	}

	/**
	 * Bean property fluent setter:  <property>description</property>.
	 *
	 * <p>
	 * A verbose explanation of the operation behavior.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>{@doc ExtGFM} can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation description(String value) {
		setDescription(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// externalDocs
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
	}

	/**
	 * Bean property fluent getter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation for this operation.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<ExternalDocumentation> externalDocs() {
		return Optional.ofNullable(getExternalDocs());
	}

	/**
	 * Bean property fluent setter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation for this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation externalDocs(ExternalDocumentation value) {
		setExternalDocs(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation for this operation as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	externalDocs(<js>"{description:'description',url:'url'}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation externalDocs(String json) {
		setExternalDocs(toType(json, ExternalDocumentation.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// operationId
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setOperationId(String value) {
		operationId = value;
	}

	/**
	 * Bean property fluent getter:  <property>operationId</property>.
	 *
	 * <p>
	 * Unique string used to identify the operation.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> operationId() {
		return Optional.ofNullable(getOperationId());
	}

	/**
	 * Bean property fluent setter:  <property>operationId</property>.
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
	 * @return This object (for method chaining).
	 */
	public Operation operationId(String value) {
		setOperationId(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parameters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If a parameter is already defined at the {@doc ExtSwaggerPathItemObject Path Item},
	 * 		the new definition will override it, but can never remove it.
	 * 	<li>
	 * 		The list MUST NOT include duplicated parameters.
	 * 	<li>
	 * 		A unique parameter is defined by a combination of a <c>name</c> and <c>location</c>.
	 * 	<li>
	 * 		The list can use the {@doc ExtSwaggerReferenceObject}
	 * 		to link to parameters that are defined at the {@doc ExtSwaggerParameterObject Swagger Object's parameters}.
	 * 	<li>
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
				if (StringUtils.isEquals(pi.getIn(), in))
					if (StringUtils.isEquals(pi.getName(), name) || "body".equals(pi.getIn()))
						return pi;
		return null;
	}

	/**
	 * Returns the parameter with the specified type and name.
	 *
	 * @param in The parameter in.
	 * @param name The parameter name.  Can be <jk>null</jk> for parameter type <c>body</c>.
	 * @return The matching parameter info, or <jk>null</jk> if not found.
	 */
	public Optional<ParameterInfo> parameter(String in, String name) {
		return Optional.ofNullable(getParameter(in, name));
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
	 */
	public void setParameters(Collection<ParameterInfo> value) {
		parameters = newList(value);
	}

	/**
	 * Adds one or more values to the <property>parameters</property> property.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 */
	public void addParameters(Collection<ParameterInfo> value) {
		parameters = addToList(parameters, value);
	}

	/**
	 * Bean property fluent getter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<List<ParameterInfo>> parameters() {
		return Optional.ofNullable(getParameters());
	}

	/**
	 * Bean property fluent setter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation parameters(ParameterInfo...value) {
		setParameters(toList(value, ParameterInfo.class));
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation parameters(Collection<ParameterInfo> value) {
		setParameters(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>parameters</property>.
	 *
	 * <p>
	 * A list of parameters that are applicable for this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<bf>Strings can be JSON arrays of objects.
	 * @return This object (for method chaining).
	 */
	public Operation parameters(String...value) {
		setParameters(toList(value, ParameterInfo.class));
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
	 * 	<br>Value MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setProduces(Collection<MediaType> value) {
		produces = newSet(value);
	}

	/**
	 * Bean property adder:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Value MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 */
	public void addProduces(Collection<MediaType> value) {
		produces = addToSet(produces, value);
	}

	/**
	 * Bean property fluent getter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<MediaType>> produces() {
		return Optional.ofNullable(getProduces());
	}

	/**
	 * Bean property fluent setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation produces(MediaType...value) {
		setProduces(toSet(value, MediaType.class));
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation produces(Collection<MediaType> value) {
		setProduces(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Strings can also be JSON arrays.
	 * @return This object (for method chaining).
	 */
	public Operation produces(String...value) {
		setProduces(toSet(value, MediaType.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// responses
	//-----------------------------------------------------------------------------------------------------------------

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
	 * Returns the response info with the given status code.
	 *
	 * @param status The HTTP status code.
	 * @return The response info as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<ResponseInfo> response(String status) {
		return Optional.ofNullable(getResponse(status));
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
	 */
	public void setResponses(Map<String,ResponseInfo> value) {
		responses = newMap(value);
	}

	/**
	 * Bean property appender:  <property>responses</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 */
	public void addResponses(Map<String,ResponseInfo> values) {
		responses = addToMap(responses, values);
	}

	/**
	 * Adds a single value to the <property>responses</property> property.
	 *
	 * @param statusCode The HTTP status code.
	 * @param response The response description.
	 * @return This object (for method chaining).
	 */
	public Operation response(String statusCode, ResponseInfo response) {
		addResponses(Collections.singletonMap(statusCode, response));
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>responses</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,ResponseInfo>> responses() {
		return Optional.ofNullable(getResponses());
	}

	/**
	 * Bean property fluent setter:  <property>responses</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation responses(Map<String,ResponseInfo> value) {
		setResponses(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>responses</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	responses(<js>"{'404':{description:'description',...}}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation responses(String json) {
		setResponses(toMap(json, String.class, ResponseInfo.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// schemes
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setSchemes(Collection<String> value) {
		schemes = newSet(value);
	}

	/**
	 * Bean property appender:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol for the operation.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 */
	public void addSchemes(Collection<String> value) {
		schemes = addToSet(schemes, value);
	}

	/**
	 * Bean property fluent getter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol for the operation.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<String>> schemes() {
		return Optional.ofNullable(getSchemes());
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
	 * @return This object (for method chaining).
	 */
	public Operation schemes(String...value) {
		setSchemes(toSet(value, String.class));
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol for the operation.
	 * <br>The value overrides the Swagger Object <c>schemes</c> definition.
	 *
	 * @param values
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation schemes(Collection<String> values) {
		setSchemes(values);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// security
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setSecurity(Collection<Map<String,List<String>>> value) {
		security = newList(value);
	}

	/**
	 * Adds one or more values to the <property>security</property> property.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for this operation.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * The new value for this property.
	 */
	public void addSecurity(Collection<Map<String,List<String>>> values) {
		security = addToList(security, values);
	}

	/**
	 * Same as {@link #addSecurity(Collection)}.
	 *
	 * @param scheme
	 * 	The scheme name.
	 * @param alternatives
	 * 	The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * 	between the security requirements).
	 * @return This object (for method chaining).
	 */
	public Operation security(String scheme, String...alternatives) {
		Map<String,List<String>> m = new LinkedHashMap<>();
		m.put(scheme, Arrays.asList(alternatives));
		addSecurity(Collections.singletonList(m));
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>security</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<List<Map<String,List<String>>>> security() {
		return Optional.ofNullable(getSecurity());
	}

	/**
	 * Bean property fluent setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for this operation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation security(List<Map<String,List<String>>> value) {
		setSecurity(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for this operation.
	 * <br>The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * between the security requirements).
	 *
	 * <p>
	 * This definition overrides any declared top-level security.
	 * <br>To remove a top-level <c>security</c> declaration, an empty array can be used.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	security(<js>"[{key:['val1','val2']}]"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Operation security(String value) {
		setSecurity((Collection)toList(value, Map.class, String.class, List.class, String.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// summary
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setSummary(String value) {
		summary = value;
	}

	/**
	 * Bean property fluent getter:  <property>summary</property>.
	 *
	 * <p>
	 * A short summary of what the operation does.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> summary() {
		return Optional.ofNullable(getSummary());
	}

	/**
	 * Bean property fluent setter:  <property>summary</property>.
	 *
	 * <p>
	 * A short summary of what the operation does.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation summary(String value) {
		setSummary(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// tags
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setTags(Collection<String> value) {
		tags = newSet(value);
	}

	/**
	 * Bean property adder:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags for API documentation control.
	 * <br>Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @param value
	 * 	The values to add to this property.
	 */
	public void addTags(Collection<String> value) {
		tags = addToSet(tags, value);
	}

	/**
	 * Bean property fluent getter:  <property>tags</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<String>> tags() {
		return Optional.ofNullable(getTags());
	}

	/**
	 * Convenience method for checking whether the tags property contains the specified tag.
	 *
	 * @param name The tag name to check for.
	 * @return <jk>true</jk> if tag exists in the tags property.
	 */
	public boolean hasTag(String name) {
		return getTags().contains(name);
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
	 * @return This object (for method chaining).
	 */
	public Operation tags(Collection<String> value) {
		setTags(value);
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
	 *  <br>Strings can also be JSON arrays.
	 * @return This object (for method chaining).
	 */
	public Operation tags(String...value) {
		setTags(toSet(value, String.class));
		return this;
	}


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
			case "consumes": return consumes(toList(value, MediaType.class));
			case "deprecated": return deprecated(toBoolean(value));
			case "description": return description(stringify(value));
			case "externalDocs": return externalDocs(toType(value, ExternalDocumentation.class));
			case "operationId": return operationId(stringify(value));
			case "parameters": return parameters(toList(value, ParameterInfo.class));
			case "produces": return produces(toList(value, MediaType.class));
			case "responses": return responses(toMap(value, String.class, ResponseInfo.class));
			case "schemes": return schemes(toList(value, String.class));
			case "security": return security((List)toList(value, Map.class, String.class, List.class, String.class));
			case "summary": return summary(stringify(value));
			case "tags": return tags(toList(value, String.class));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = ASet.<String>of()
			.appendIf(consumes != null, "consumes")
			.appendIf(deprecated != null, "deprecated")
			.appendIf(description != null, "description")
			.appendIf(externalDocs != null, "externalDocs")
			.appendIf(operationId != null, "operationId")
			.appendIf(parameters != null, "parameters")
			.appendIf(produces != null, "produces")
			.appendIf(responses != null, "responses")
			.appendIf(schemes != null, "schemes")
			.appendIf(security != null, "security")
			.appendIf(summary != null, "summary")
			.appendIf(tags != null, "tags");
		return new MultiSet<>(s, super.keySet());
	}
}
