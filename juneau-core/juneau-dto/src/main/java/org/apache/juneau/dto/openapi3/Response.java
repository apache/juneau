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
package org.apache.juneau.dto.openapi3;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import org.apache.juneau.UriResolver;
import org.apache.juneau.annotation.Bean;
import org.apache.juneau.internal.*;

import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * TODO
 */
@Bean(properties="contentType,style,explode,headers,allowReserved,*")
@FluentSetters
public class Response extends OpenApiElement{

	private String description;
	private Map<String,HeaderInfo> headers;
	private Map<String,MediaType> content;
	private Map<String,Link> links;

	/**
	 * Default constructor.
	 */
	public Response() { }

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Response(Response copyFrom) {
		super(copyFrom);

		this.description = copyFrom.description;
		if (copyFrom.headers == null) {
			this.headers = null;
		} else {
			this.headers = new LinkedHashMap<>();
			for (Map.Entry<String,HeaderInfo> e : copyFrom.headers.entrySet())
				this.headers.put(e.getKey(),	e.getValue().copy());
		}

		if (copyFrom.content == null) {
			this.content = null;
		} else {
			this.content = new LinkedHashMap<>();
			for (Map.Entry<String,MediaType> e : copyFrom.content.entrySet())
				this.content.put(e.getKey(),	e.getValue().copy());
		}

		if (copyFrom.links == null) {
			this.links = null;
		} else {
			this.links = new LinkedHashMap<>();
			for (Map.Entry<String,Link> e : copyFrom.links.entrySet())
				this.links.put(e.getKey(),	e.getValue().copy());
		}
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Response copy() {
		return new Response(this);
	}

	@Override /* OpenApiElement */
	protected Response strict() {
		super.strict();
		return this;
	}

	/**
	 * Bean property getter:  <property>Description</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
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
	 * Bean property getter:  <property>headers</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String, HeaderInfo> getHeaders() {
		return headers;
	}

	/**
	 * Bean property setter:  <property>headers</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public Response setHeaders(Map<String, HeaderInfo> value) {
		headers = copyOf(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>variables</property> property.
	 *
	 * @param key The mapping key.
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public Response addHeader(String key, HeaderInfo value) {
		headers = mapBuilder(headers).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>headers</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String, MediaType> getContent() {
		return content;
	}

	/**
	 * Bean property setter:  <property>content</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public Response setContent(Map<String, MediaType> value) {
		content = copyOf(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>variables</property> property.
	 *
	 * @param key The mapping key.
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public Response addContent(String key, MediaType value) {
		content = mapBuilder(content).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>link</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String, Link> getLinks() {
		return links;
	}

	/**
	 * Bean property setter:  <property>Link</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public Response setLinks(Map<String, Link> value) {
		links = copyOf(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>variables</property> property.
	 *
	 * @param key The mapping key.
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public Response addLink(String key, Link value) {
		links = mapBuilder(links).sparse().add(key, value).build();
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "description": return toType(getDescription(), type);
			case "content": return toType(getContent(), type);
			case "headers": return toType(getHeaders(), type);
			case "links": return toType(getLinks(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public Response set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "description": return setDescription(stringify(value));
			case "headers": return setHeaders(mapBuilder(String.class,HeaderInfo.class).sparse().addAny(value).build());
			case "content": return setContent(mapBuilder(String.class,MediaType.class).sparse().addAny(value).build());
			case "links": return setLinks(mapBuilder(String.class,Link.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
				.addIf(description != null, "description")
				.addIf(headers != null, "headers")
				.addIf(content != null, "content")
				.addIf(links != null, "links")
				.build();
		return new MultiSet<>(s, super.keySet());
	}
}
