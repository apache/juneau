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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Used to aid in serialization, deserialization, and validation.
 */
@Bean(properties="propertyName,mapping,*")
@FluentSetters
public class Discriminator extends OpenApiElement {

	private String propertyName;
	private Map<String,String> mapping;

	/**
	 * Default constructor.
	 */
	public Discriminator() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Discriminator(Discriminator copyFrom) {
		super(copyFrom);

		this.propertyName = copyFrom.propertyName;
		if (copyFrom.mapping == null)
			this.mapping = null;
		else
			this.mapping = new LinkedHashMap<>(copyFrom.mapping);
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Discriminator copy() {
		return new Discriminator(this);
	}

	/**
	 * Bean property getter:  <property>propertyName</property>.
	 *
	 * <p>
	 * A short description of the target documentation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Bean property setter:  <property>propertyName</property>.
	 *
	 * <p>
	 * A short description of the target documentation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Discriminator setPropertyName(String value) {
		propertyName = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>mapping</property>.
	 *
	 * <p>
	 * The URL for the target documentation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,String> getMapping() {
		return mapping;
	}

	/**
	 * Bean property setter:  <property>mapping</property>.
	 *
	 * <p>
	 * The URL for the target documentation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>URIs defined by {@link UriResolver} can be used for values.
	 * @return This object
	 */
	public Discriminator setMapping(Map<String,String> value) {
		mapping = copyOf(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>mapping</property> property.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return This object
	 */
	public Discriminator addMapping(String key, String value) {
		mapping = mapBuilder(mapping).sparse().add(key, value).build();
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "propertyName": return toType(getPropertyName(), type);
			case "mapping": return toType(getMapping(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public Discriminator set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "propertyName": return setPropertyName(stringify(value));
			case "mapping": return setMapping(mapBuilder(String.class,String.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(propertyName != null, "propertyName")
			.addIf(mapping != null, "mapping")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
