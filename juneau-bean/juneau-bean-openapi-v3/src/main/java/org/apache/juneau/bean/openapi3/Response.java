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

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.utils.*;

/**
 * Describes a single response from an API operation.
 *
 * <p>
 * The Response Object describes a single response from an API operation, including a description, headers, content, and links.
 * Responses are returned based on the HTTP status code, with the most common being success responses (2xx), redirects (3xx),
 * client errors (4xx), and server errors (5xx).
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Response Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>description</c> (string, REQUIRED) - A short description of the response (CommonMark syntax may be used)
 * 	<li><c>headers</c> (map of {@link HeaderInfo}) - Maps a header name to its definition
 * 	<li><c>content</c> (map of {@link MediaType}) - A map containing descriptions of potential response payloads (keys are media types)
 * 	<li><c>links</c> (map of {@link Link}) - A map of operations links that can be followed from the response
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a successful response with JSON content</jc>
 * 	Response <jv>response</jv> = <jk>new</jk> Response()
 * 		.setDescription(<js>"A list of pets"</js>)
 * 		.setContent(
 * 			JsonMap.<jsm>of</jsm>(
 * 				<js>"application/json"</js>, <jk>new</jk> MediaType()
 * 					.setSchema(
 * 						<jk>new</jk> SchemaInfo()
 * 							.setType(<js>"array"</js>)
 * 							.setItems(<jk>new</jk> Items().setRef(<js>"#/components/schemas/Pet"</js>))
 * 					)
 * 			)
 * 		)
 * 		.setHeaders(
 * 			JsonMap.<jsm>of</jsm>(
 * 				<js>"X-Rate-Limit"</js>, <jk>new</jk> HeaderInfo()
 * 					.setDescription(<js>"Requests per hour allowed by the user"</js>)
 * 					.setSchema(<jk>new</jk> SchemaInfo().setType(<js>"integer"</js>))
 * 			)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#response-object">OpenAPI Specification &gt; Response Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/describing-responses/">OpenAPI Describing Responses</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class Response extends OpenApiElement {

	private String description;
	private Map<String,HeaderInfo> headers;
	private Map<String,MediaType> content;
	private Map<String,Link> links;

	/**
	 * Default constructor.
	 */
	public Response() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Response(Response copyFrom) {
		super(copyFrom);

		this.description = copyFrom.description;
		this.headers = CollectionUtils.copyOf(copyFrom.headers, HeaderInfo::copy);
		this.content = CollectionUtils.copyOf(copyFrom.content, MediaType::copy);
		this.links = CollectionUtils.copyOf(copyFrom.links, Link::copy);
	}

	/**
	 * Adds one or more values to the <property>content</property> property.
	 *
	 * @param key The mapping key.  Must not be <jk>null</jk>.
	 * @param value The values to add to this property.  Must not be <jk>null</jk>.
	 * @return This object
	 */
	public Response addContent(String key, MediaType value) {
		assertArgNotNull("key", key);
		assertArgNotNull("value", value);
		content = CollectionUtils.mapb(String.class, MediaType.class).to(content).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Adds one or more values to the <property>headers</property> property.
	 *
	 * @param key The mapping key.  Must not be <jk>null</jk>.
	 * @param value The values to add to this property.  Must not be <jk>null</jk>.
	 * @return This object
	 */
	public Response addHeader(String key, HeaderInfo value) {
		assertArgNotNull("key", key);
		assertArgNotNull("value", value);
		headers = CollectionUtils.mapb(String.class, HeaderInfo.class).to(headers).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Adds one or more values to the <property>links</property> property.
	 *
	 * @param key The mapping key.  Must not be <jk>null</jk>.
	 * @param value The values to add to this property.  Must not be <jk>null</jk>.
	 * @return This object
	 */
	public Response addLink(String key, Link value) {
		assertArgNotNull("key", key);
		assertArgNotNull("value", value);
		links = CollectionUtils.mapb(String.class, Link.class).to(links).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Response copy() {
		return new Response(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> toType(getDescription(), type);
			case "content" -> toType(getContent(), type);
			case "headers" -> toType(getHeaders(), type);
			case "links" -> toType(getLinks(), type);
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
	 * Returns the content with the specified media type.
	 *
	 * @param mediaType The media type.  Must not be <jk>null</jk>.
	 * @return The media type info, or <jk>null</jk> if not found.
	 */
	public MediaType getContent(String mediaType) {
		assertArgNotNull("mediaType", mediaType);
		return content == null ? null : content.get(mediaType);
	}

	/**
	 * Bean property getter:  <property>Description</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Returns the header with the specified name.
	 *
	 * @param name The header name.  Must not be <jk>null</jk>.
	 * @return The header info, or <jk>null</jk> if not found.
	 */
	public HeaderInfo getHeader(String name) {
		assertArgNotNull("name", name);
		return headers == null ? null : headers.get(name);
	}

	/**
	 * Bean property getter:  <property>headers</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,HeaderInfo> getHeaders() { return headers; }

	/**
	 * Returns the link with the specified name.
	 *
	 * @param name The link name.  Must not be <jk>null</jk>.
	 * @return The link info, or <jk>null</jk> if not found.
	 */
	public Link getLink(String name) {
		assertArgNotNull("name", name);
		return links == null ? null : links.get(name);
	}

	/**
	 * Bean property getter:  <property>links</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Link> getLinks() { return links; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = CollectionUtils.setb(String.class)
			.addIf(nn(content), "content")
			.addIf(nn(description), "description")
			.addIf(nn(headers), "headers")
			.addIf(nn(links), "links")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Response set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "content" -> setContent(toMap(value, String.class, MediaType.class).sparse().build());
			case "description" -> setDescription(s(value));
			case "headers" -> setHeaders(toMap(value, String.class, HeaderInfo.class).sparse().build());
			case "links" -> setLinks(toMap(value, String.class, Link.class).sparse().build());
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
	public Response setContent(Map<String,MediaType> value) {
		content = CollectionUtils.copyOf(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>Description</property>.
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
	public Response setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>headers</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Response setHeaders(Map<String,HeaderInfo> value) {
		headers = CollectionUtils.copyOf(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>links</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Response setLinks(Map<String,Link> value) {
		links = CollectionUtils.copyOf(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Response strict(Object value) {
		super.strict(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	protected Response strict() {
		super.strict();
		return this;
	}
}