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
@Bean(properties="url,description,variables,*")
@FluentSetters
public class Server extends OpenApiElement{
	private URI url;
	private String description;
	private Map<String,ServerVariable> variables;

	/**
	 * Default constructor.
	 */
	public Server() { }

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Server(Server copyFrom) {
		super(copyFrom);

		this.url = copyFrom.url;
		this.description = copyFrom.description;
		if (copyFrom.variables == null) {
			this.variables = null;
		} else {
			this.variables = new LinkedHashMap<>();
			for (Map.Entry<String,ServerVariable> e : copyFrom.variables.entrySet())
				this.variables.put(e.getKey(),	e.getValue().copy());
		}
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Server copy() {
		return new Server(this);
	}

	@Override /* OpenApiElement */
	protected Server strict() {
		super.strict();
		return this;
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public URI getUrl() {
		return url;
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
	public Server setUrl(URI value) {
		url = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public Server setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>variables</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String, ServerVariable> getVariables() {
		return variables;
	}

	/**
	 * Bean property setter:  <property>variables</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public Server setVariables(Map<String, ServerVariable> value) {
		variables = copyOf(value);
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
	public Server addVariable(String key, ServerVariable value) {
		variables = mapBuilder(variables).sparse().add(key, value).build();
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "url": return toType(getUrl(), type);
			case "description": return toType(getDescription(), type);
			case "variables": return toType(getVariables(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public Server set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "url": return setUrl(toURI(value));
			case "description": return setDescription(stringify(value));
			case "variables": return setVariables(mapBuilder(String.class,ServerVariable.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
				.addIf(url != null, "url")
				.addIf(description != null, "description")
				.addIf(variables != null, "variables")
				.build();
		return new MultiSet<>(s, super.keySet());
	}
}
