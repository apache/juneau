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
 * 	Operation x = <jsm>operation</jsm>()
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
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
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
	private List<String>
		tags,
		schemes;
	private List<MediaType>
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

		this.summary = copyFrom.summary;
		this.description = copyFrom.description;
		this.operationId = copyFrom.operationId;
		this.deprecated = copyFrom.deprecated;
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this.tags = newList(copyFrom.tags);
		this.schemes = newList(copyFrom.schemes);
		this.consumes = newList(copyFrom.consumes);
		this.produces = newList(copyFrom.produces);

		if (copyFrom.parameters == null) {
			this.parameters = null;
		} else {
			this.parameters = new ArrayList<>();
			for (ParameterInfo p : copyFrom.parameters)
				this.parameters.add(p.copy());
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

		if (copyFrom.responses == null) {
			this.responses = null;
		} else {
			this.responses = new LinkedHashMap<>();
			for (Map.Entry<String,ResponseInfo> e : copyFrom.responses.entrySet())
				this.responses.put(e.getKey(), e.getValue().copy());
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

	/**
	 * Bean property getter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags for API documentation control.
	 * <br>Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<String> getTags() {
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
	 * @return This object (for method chaining).
	 */
	public Operation setTags(Collection<String> value) {
		tags = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>tags</property> property.
	 *
	 * <p>
	 * A list of tags for API documentation control.
	 * <br>Tags can be used for logical grouping of operations by resources or any other qualifier.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation addTags(Collection<String> value) {
		tags = addToList(tags, value);
		return this;
	}

	/**
	 * Same as {@link #addTags(Collection)}.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Collection&lt;String&gt;</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;String&gt;</c>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	tags(<js>"['foo','bar']"</js>);
	 * 			</p>
	 * 		<li><c>String</c> - Individual values
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	tags(<js>"foo"</js>, <js>"bar"</js>);
	 * 			</p>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public Operation tags(Object...values) {
		tags = addToList(tags, values, String.class);
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
	 * @return This object (for method chaining).
	 */
	public Operation setSummary(String value) {
		summary = value;
		return this;
	}

	/**
	 * Same as {@link #setSummary(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation summary(Object value) {
		return setSummary(stringify(value));
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
	 * 	<br>{@doc ExtGFM} can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Same as {@link #setDescription(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>{@doc ExtGFM} can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation description(Object value) {
		return setDescription(stringify(value));
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
	 * @return This object (for method chaining).
	 */
	public Operation setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
		return this;
	}

	/**
	 * Same as {@link #setExternalDocs(ExternalDocumentation)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link ExternalDocumentation}
	 * 		<li><c>String</c> - JSON object representation of {@link ExternalDocumentation}
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	externalDocs(<js>"{description:'description',url:'url'}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation externalDocs(Object value) {
		return setExternalDocs(toType(value, ExternalDocumentation.class));
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
	 * @return This object (for method chaining).
	 */
	public Operation setOperationId(String value) {
		operationId = value;
		return this;
	}

	/**
	 * Same as {@link #setOperationId(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The id MUST be unique among all operations described in the API.
	 * 	<br>Tools and libraries MAY use the operationId to uniquely identify an operation, therefore, it is recommended to
	 * 	follow common programming naming conventions.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation operationId(Object value) {
		return setOperationId(stringify(value));
	}

	/**
	 * Bean property getter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * <p>
	 * This overrides the <c>consumes</c> definition at the Swagger Object.
	 * <br>An empty value MAY be used to clear the global definition.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<MediaType> getConsumes() {
		return consumes;
	}

	/**
	 * Bean property setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * <p>
	 * This overrides the <c>consumes</c> definition at the Swagger Object.
	 * <br>An empty value MAY be used to clear the global definition.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Values MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation setConsumes(Collection<MediaType> value) {
		consumes = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>consumes</property> property.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 *
	 * <p>
	 * This overrides the <c>consumes</c> definition at the Swagger Object.
	 * <br>An empty value MAY be used to clear the global definition.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Values MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation addConsumes(Collection<MediaType> value) {
		consumes = addToList(consumes, value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>consumes</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link MediaType}
	 * 		<li><c>Collection&lt;{@link MediaType}|String&gt;</c>
	 * 		<li><c>{@link MediaType}[]</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;{@link MediaType}&gt;</c>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	consumes(<js>"['text/json']"</js>);
	 * 			</p>
	 * 		<li><c>String</c> - Individual values
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	consumes(<js>"text/json"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation consumes(Object...values) {
		consumes = addToList(consumes, values, MediaType.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * <p>
	 * This overrides the <c>produces</c> definition at the Swagger Object.
	 * <br>An empty value MAY be used to clear the global definition.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<MediaType> getProduces() {
		return produces;
	}

	/**
	 * Bean property setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the operation can produce.
	 *
	 * <p>
	 * This overrides the <c>produces</c> definition at the Swagger Object.
	 * <br>An empty value MAY be used to clear the global definition.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Value MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation setProduces(Collection<MediaType> value) {
		produces = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>produces</property> property.
	 *
	 * <p>
	 * A list of MIME types the operation can produces.
	 *
	 * <p>
	 * This overrides the <c>produces</c> definition at the Swagger Object.
	 * <br>An empty value MAY be used to clear the global definition.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation addProduces(Collection<MediaType> value) {
		produces = addToList(produces, value);
		return this;
	}

	/**
	 * Same as {@link #addProduces(Collection)}.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link MediaType}
	 * 		<li><c>Collection&lt;{@link MediaType}|String&gt;</c>
	 * 		<li><c>{@link MediaType}[]</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;{@link MediaType}&gt;</c>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	produces(<js>"['text/json']"</js>);
	 * 			</p>
	 * 		<li><c>String</c> - Individual values
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	produces(<js>"text/json"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation produces(Object...values) {
		produces = addToList(produces, values, MediaType.class);
		return this;
	}

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
	 * Bean property setter:  <property>parameters</property>.
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation setParameters(Collection<ParameterInfo> value) {
		parameters = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>parameters</property> property.
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
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation addParameters(Collection<ParameterInfo> value) {
		parameters = addToList(parameters, value);
		return this;
	}

	/**
	 * Same as {@link #addParameters(Collection)}.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link ParameterInfo}
	 * 		<li><c>Collection&lt;{@link ParameterInfo}|String&gt;</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;{@link ParameterInfo}&gt;</c>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	parameters(<js>"[{path:'path',id:'id'}]"</js>);
	 * 			</p>
	 * 		<li><c>String</c> - JSON object representation of {@link ParameterInfo}
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	parameters(<js>"{path:'path',id:'id'}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation parameters(Object...values) {
		parameters = addToList(parameters, values, ParameterInfo.class);
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
	public ResponseInfo getResponse(Object status) {
		if (responses != null)
			return responses.get(String.valueOf(status));
		return null;
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
	 * @return This object (for method chaining).
	 */
	public Operation setResponses(Map<String,ResponseInfo> value) {
		responses = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>responses</property> property.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation addResponses(Map<String,ResponseInfo> values) {
		responses = addToMap(responses, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>responses</property> property.
	 *
	 * @param statusCode The HTTP status code.
	 * @param response The response description.
	 * @return This object (for method chaining).
	 */
	public Operation response(String statusCode, ResponseInfo response) {
		return addResponses(Collections.singletonMap(statusCode, response));
	}

	/**
	 * Same as {@link #addResponses(Map)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Map&lt;Integer,{@link ResponseInfo}|String&gt;</c>
	 * 		<li><c>String</c> - JSON object representation of <c>Map&lt;Integer,{@link ResponseInfo}&gt;</c>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	responses(<js>"{'404':{description:'description',...}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public Operation responses(Object...value) {
		responses = addToMap(responses, value, String.class, ResponseInfo.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol for the operation.
	 * <br>The value overrides the Swagger Object <c>schemes</c> definition.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<String> getSchemes() {
		return schemes;
	}

	/**
	 * Bean property setter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol for the operation.
	 * <br>The value overrides the Swagger Object <c>schemes</c> definition.
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
	 * @return This object (for method chaining).
	 */
	public Operation setSchemes(Collection<String> value) {
		schemes = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>schemes</property> property.
	 *
	 * <p>
	 * The transfer protocol for the operation.
	 * <br>The value overrides the Swagger Object <c>schemes</c> definition.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Operation addSchemes(Collection<String> value) {
		schemes = addToList(schemes, value);
		return this;
	}

	/**
	 * Same as {@link #addSchemes(Collection)}.
	 *
	 * @param values
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Collection&lt;String&gt;</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;String&gt;</c>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	schemes(<js>"['scheme1','scheme2']"</js>);
	 * 			</p>
	 * 		<li><c>String</c> - Individual values
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	schemes(<js>"scheme1</js>, <js>"scheme2"</js>);
	 * 			</p>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public Operation schemes(Object...values) {
		schemes = addToList(schemes, values, String.class);
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
	 * @param value T
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation setDeprecated(Boolean value) {
		deprecated = value;
		return this;
	}

	/**
	 * Same as {@link #setDeprecated(Boolean)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation deprecated(Object value) {
		return setDeprecated(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>security</property>.
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
	 * <br>The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * between the security requirements).
	 *
	 * <p>
	 * This definition overrides any declared top-level security.
	 * <br>To remove a top-level <c>security</c> declaration, an empty array can be used.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Operation setSecurity(Collection<Map<String,List<String>>> value) {
		security = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>security</property> property.
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
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * The new value for this property.
	 * @return This object (for method chaining).
	 */
	public Operation addSecurity(Collection<Map<String,List<String>>> values) {
		security = addToList(security, values);
		return this;
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
		return addSecurity(Collections.singletonList(m));
	}

	/**
	 * Same as {@link #addSecurity(Collection)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Map&lt;String,List&lt;String&gt;&gt;</c>
	 * 		<li><c>String</c> - JSON object representation of a <c>Map&lt;String,List&lt;String&gt;&gt;</c>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	securities(<js>"{key:['val1','val2']}"</js>);
	 * 		</p>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Operation securities(Object...value) {
		security = addToList((List)security, value, Map.class, String.class, List.class, String.class);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if the summary property is not null or empty.
	 *
	 * @return <jk>true</jk> if the summary property is not null or empty.
	 */
	public boolean hasSummary() {
		return isNotEmpty(summary);
	}

	/**
	 * Returns <jk>true</jk> if the description property is not null or empty.
	 *
	 * @return <jk>true</jk> if the description property is not null or empty.
	 */
	public boolean hasDescription() {
		return isNotEmpty(description);
	}

	/**
	 * Returns <jk>true</jk> if this operation has the specified tag associated with it.
	 *
	 * @param name The tag name.
	 * @return <jk>true</jk> if this operation has the specified tag associated with it.
	 */
	public boolean hasTag(String name) {
		return tags != null && tags.contains(name);
	}

	/**
	 * Returns <jk>true</jk> if this operation has no tags associated with it.
	 *
	 * @return <jk>true</jk> if this operation has no tags associated with it.
	 */
	public boolean hasNoTags() {
		return tags == null || tags.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if this operation has parameters associated with it.
	 *
	 * @return <jk>true</jk> if this operation has parameters associated with it.
	 */
	public boolean hasParameters() {
		return parameters != null && ! parameters.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if this operation has responses associated with it.
	 *
	 * @return <jk>true</jk> if this operation has responses associated with it.
	 */
	public boolean hasResponses() {
		return responses != null && ! responses.isEmpty();
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "tags": return toType(getTags(), type);
			case "summary": return toType(getSummary(), type);
			case "description": return toType(getDescription(), type);
			case "externalDocs": return toType(getExternalDocs(), type);
			case "operationId": return toType(getOperationId(), type);
			case "consumes": return toType(getConsumes(), type);
			case "produces": return toType(getProduces(), type);
			case "parameters": return toType(getParameters(), type);
			case "responses": return toType(getResponses(), type);
			case "schemes": return toType(getSchemes(), type);
			case "deprecated": return toType(getDeprecated(), type);
			case "security": return toType(getSecurity(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public Operation set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "tags": return setTags(null).tags(value);
			case "summary": return summary(value);
			case "description": return description(value);
			case "externalDocs": return externalDocs(value);
			case "operationId": return operationId(value);
			case "consumes": return setConsumes(null).consumes(value);
			case "produces": return setProduces(null).produces(value);
			case "parameters": return setParameters(null).parameters(value);
			case "responses": return setResponses(null).responses(value);
			case "schemes": return setSchemes(null).schemes(value);
			case "deprecated": return deprecated(value);
			case "security": return setSecurity(null).securities(value);
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = ASet.<String>of()
			.appendIf(tags != null, "tags")
			.appendIf(summary != null, "summary")
			.appendIf(description != null, "description")
			.appendIf(externalDocs != null, "externalDocs")
			.appendIf(operationId != null, "operationId")
			.appendIf(consumes != null, "consumes")
			.appendIf(produces != null, "produces")
			.appendIf(parameters != null, "parameters")
			.appendIf(responses != null, "responses")
			.appendIf(schemes != null, "schemes")
			.appendIf(deprecated != null, "deprecated")
			.appendIf(security != null, "security");
		return new MultiSet<>(s, super.keySet());
	}
}
