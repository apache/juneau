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

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;

/**
 * Describes a single request body.
 *
 * <p>
 * The Request Body Object describes a single request body that can be sent to an API operation. It includes
 * a description, whether the request body is required, and the content (media types) that the request body can contain.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Request Body Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>description</c> (string) - A brief description of the request body (CommonMark syntax may be used)
 * 	<li><c>content</c> (map of {@link MediaType}, REQUIRED) - The content of the request body (keys are media types)
 * 	<li><c>required</c> (boolean) - Determines if the request body is required in the request (default is <jk>false</jk>)
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a request body for JSON content</jc>
 * 	RequestBodyInfo <jv>requestBody</jv> = <jk>new</jk> RequestBodyInfo()
 * 		.setDescription(<js>"Pet object that needs to be added to the store"</js>)
 * 		.setRequired(<jk>true</jk>)
 * 		.setContent(
 * 			JsonMap.<jsm>of</jsm>(
 * 				<js>"application/json"</js>, <jk>new</jk> MediaType()
 * 					.setSchema(
 * 						<jk>new</jk> SchemaInfo().setRef(<js>"#/components/schemas/Pet"</js>)
 * 					)
 * 			)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#request-body-object">OpenAPI Specification &gt; Request Body Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/describing-request-body/">OpenAPI Describing Request Body</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class RequestBodyInfo extends OpenApiElement {

	private String description;
	private Map<String,MediaType> content;
	private Boolean required;

	/**
	 * Default constructor.
	 */
	public RequestBodyInfo() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public RequestBodyInfo(RequestBodyInfo copyFrom) {
		super(copyFrom);

		this.description = copyFrom.description;
		this.required = copyFrom.required;
		this.content = copyOf(copyFrom.content, MediaType::copy);
	}

	/**
	 * Adds one or more values to the <property>content</property> property.
	 *
	 * @param key The mapping key.  Must not be <jk>null</jk>.
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public RequestBodyInfo addContent(String key, MediaType value) {
		assertArgNotNull("key", key);
		assertArgNotNull("value", value);
		content = mapb(String.class, MediaType.class).to(content).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public RequestBodyInfo copy() {
		return new RequestBodyInfo(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> toType(getDescription(), type);
			case "content" -> toType(getContent(), type);
			case "required" -> toType(getRequired(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>content</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,MediaType> getContent() { return content; }

	/**
	 * Bean property getter:  <property>contentType</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getRequired() { return required; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(content), "content")
			.addIf(nn(description), "description")
			.addIf(nn(required), "required")
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public RequestBodyInfo set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "content" -> setContent(toMapBuilder(value, String.class, MediaType.class).sparse().build());
			case "description" -> setDescription(s(value));
			case "required" -> setRequired(toBoolean(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>content</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public RequestBodyInfo setContent(Map<String,MediaType> value) {
		content = copyOf(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>url</property>.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * <br>Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public RequestBodyInfo setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>explode</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public RequestBodyInfo setRequired(Boolean value) {
		required = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public RequestBodyInfo strict(Object value) {
		super.strict(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	protected RequestBodyInfo strict() {
		super.strict();
		return this;
	}
}