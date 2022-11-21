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

import org.apache.juneau.annotation.Bean;
import org.apache.juneau.dto.swagger.ExternalDocumentation;
import org.apache.juneau.internal.*;

import java.util.Set;

/**
 * Allows adding meta data to a single tag that is used by the operation object.
 *
 * <p>
 * It is not mandatory to have a Tag Object per tag used there.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Tag x = <jsm>tag</jsm>()
 * 		.name(<js>"pet"</js>)
 * 		.description(<js>"Pets operations"</js>)
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"name"</js>: <js>"pet"</js>,
 * 		<js>"description"</js>: <js>"Pets operations"</js>
 * 	}
 * </p>
 */
@Bean(properties="name,description,externalDocs,*")
@FluentSetters
public class Tag extends OpenApiElement {

	private String
		name,
		description;
	private ExternalDocumentation externalDocs;

	/**
	 * Default constructor.
	 */
	public Tag() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Tag(Tag copyFrom) {
		super(copyFrom);

		this.name = copyFrom.name;
		this.description = copyFrom.description;
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Tag copy() {
		return new Tag(this);
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the tag.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the tag.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object
	 */
	public Tag setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description for the tag.
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
	 * A short description for the tag.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Tag setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation for this tag.
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
	 * Additional external documentation for this tag.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Tag setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "name": return toType(getName(), type);
			case "description": return toType(getDescription(), type);
			case "externalDocs": return toType(getExternalDocs(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public Tag set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "name": return setName(stringify(value));
			case "description": return setDescription(stringify(value));
			case "externalDocs": return setExternalDocs(toType(value, ExternalDocumentation.class));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(name != null, "name")
			.addIf(description != null, "description")
			.addIf(externalDocs != null, "externalDocs")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
