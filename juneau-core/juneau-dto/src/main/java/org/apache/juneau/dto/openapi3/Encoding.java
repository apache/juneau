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
public class Encoding extends OpenApiElement{

	private String contentType,
			style;
	private Map<String,HeaderInfo> headers;
	private Boolean explode,
			allowReserved;

	/**
	 * Default constructor.
	 */
	public Encoding() { }

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
		if (copyFrom.headers == null) {
			this.headers = null;
		} else {
			this.headers = new LinkedHashMap<>();
			for (Map.Entry<String,HeaderInfo> e : copyFrom.headers.entrySet())
				this.headers.put(e.getKey(),	e.getValue().copy());
		}
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Encoding copy() {
		return new Encoding(this);
	}

	@Override /* OpenApiElement */
	protected Encoding strict() {
		super.strict();
		return this;
	}

	/**
	 * Bean property getter:  <property>contentType</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getContentType() {
		return contentType;
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
	 * Bean property getter:  <property>style</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getStyle() {
		return style;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public Encoding setStyle(String value) {
		style = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>variables</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String, HeaderInfo> getHeaders() {
		return headers;
	}

	/**
	 * Bean property setter:  <property>variables</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public Encoding setHeaders(Map<String, HeaderInfo> value) {
		headers = copyOf(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>headers</property> property.
	 *
	 * @param key The mapping key.
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public Encoding addHeader(String key, HeaderInfo value) {
		headers = mapBuilder(headers).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExplode() {
		return explode;
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
	 * 	</ul>
	 * @return This object
	 */
	public Encoding setExplode(Boolean value) {
		explode = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getAllowReserved() {
		return allowReserved;
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
	 * 	</ul>
	 * @return This object
	 */
	public Encoding setAllowReserved(Boolean value) {
		allowReserved = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "contentType": return toType(getContentType(), type);
			case "style": return toType(getStyle(), type);
			case "headers": return toType(getHeaders(), type);
			case "explode": return toType(getExplode(), type);
			case "allowReserved": return toType(getAllowReserved(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public Encoding set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "contentType": return setContentType(stringify(value));
			case "style": return setStyle(stringify(value));
			case "headers": return setHeaders(mapBuilder(String.class,HeaderInfo.class).sparse().addAny(value).build());
			case "explode": return setExplode(toBoolean(value));
			case "allowReserved": return setAllowReserved(toBoolean(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
				.addIf(contentType != null, "contentType")
				.addIf(style != null, "style")
				.addIf(headers != null, "headers")
				.addIf(explode != null, "explode")
				.addIf(allowReserved != null, "allowReserved")
				.build();
		return new MultiSet<>(s, super.keySet());
	}
}
