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
import static org.apache.juneau.internal.CollectionBuilders.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * A single encoding definition applied to a single schema property.
 *
 * <p>
 * The Encoding Object is a single encoding definition applied to a single schema property. It allows you to define
 * how a property should be serialized when it's part of a request or response body with a specific media type.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Encoding Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>contentType</c> (string) - The Content-Type for encoding a specific property. Default value depends on the property type
 * 	<li><c>headers</c> (map of {@link HeaderInfo}) - A map allowing additional information to be provided as headers
 * 	<li><c>style</c> (string) - Describes how a specific property value will be serialized depending on its type
 * 	<li><c>explode</c> (boolean) - When this is true, property values of type array or object generate separate parameters for each value
 * 	<li><c>allowReserved</c> (boolean) - Determines whether the parameter value should allow reserved characters
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Encoding <jv>x</jv> = <jsm>encoding</jsm>()
 * 		.setContentType(<js>"application/x-www-form-urlencoded"</js>)
 * 		.setStyle(<js>"form"</js>)
 * 		.setExplode(<jk>true</jk>);
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
 * 		<js>"contentType"</js>: <js>"application/x-www-form-urlencoded"</js>,
 * 		<js>"style"</js>: <js>"form"</js>,
 * 		<js>"explode"</js>: <jk>true</jk>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#encoding-object">OpenAPI Specification &gt; Encoding Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/describing-request-body/">OpenAPI Describing Request Body</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class Encoding extends OpenApiElement {

	private String contentType, style;
	private Map<String,HeaderInfo> headers;
	private Boolean explode, allowReserved;

	/**
	 * Default constructor.
	 */
	public Encoding() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Encoding(Encoding copyFrom) {
		super(copyFrom);

		this.contentType = copyFrom.contentType;
		this.style = copyFrom.style;
		this.explode = copyFrom.explode;
		this.allowReserved = copyFrom.allowReserved;
		this.headers = CollectionUtils.copyOf(copyFrom.headers, HeaderInfo::copy);
	}

	/**
	 * Adds one or more values to the <property>headers</property> property.
	 *
	 * @param key The mapping key.  Must not be <jk>null</jk>.
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return This object
	 */
	public Encoding addHeader(String key, HeaderInfo value) {
		assertArgNotNull("key", key);
		assertArgNotNull("value", value);
		headers = mapBuilder(headers).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Encoding copy() {
		return new Encoding(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "contentType" -> toType(getContentType(), type);
			case "style" -> toType(getStyle(), type);
			case "headers" -> toType(getHeaders(), type);
			case "explode" -> toType(getExplode(), type);
			case "allowReserved" -> toType(getAllowReserved(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getAllowReserved() { return allowReserved; }

	/**
	 * Bean property getter:  <property>contentType</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getContentType() { return contentType; }

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExplode() { return explode; }

	/**
	 * Bean property getter:  <property>variables</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,HeaderInfo> getHeaders() { return headers; }

	/**
	 * Bean property getter:  <property>style</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getStyle() { return style; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(allowReserved != null, "allowReserved")
			.addIf(contentType != null, "contentType")
			.addIf(explode != null, "explode")
			.addIf(headers != null, "headers")
			.addIf(style != null, "style")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Encoding set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "allowReserved" -> setAllowReserved(toBoolean(value));
			case "contentType" -> setContentType(Utils.s(value));
			case "explode" -> setExplode(toBoolean(value));
			case "headers" -> setHeaders(mapBuilder(String.class, HeaderInfo.class).sparse().addAny(value).build());
			case "style" -> setStyle(Utils.s(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
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
	public Encoding setAllowReserved(Boolean value) {
		allowReserved = value;
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
	public Encoding setContentType(String value) {
		contentType = value;
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
	public Encoding setExplode(Boolean value) {
		explode = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>variables</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Encoding setHeaders(Map<String,HeaderInfo> value) {
		headers = CollectionUtils.copyOf(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Encoding setStyle(String value) {
		style = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Encoding strict(Object value) {
		super.strict(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	protected Encoding strict() {
		super.strict();
		return this;
	}
}