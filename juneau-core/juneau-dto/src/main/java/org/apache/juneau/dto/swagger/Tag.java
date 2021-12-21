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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Allows adding meta data to a single tag that is used by the {@doc ext.SwaggerOperationObject Operation Object}.
 *
 * <p>
 * It is not mandatory to have a Tag Object per tag used there.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Tag <jv>tag</jv> = <jsm>tag</jsm>()
 * 		.name(<js>"pet"</js>)
 * 		.description(<js>"Pets operations"</js>)
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.toString(<jv>tag</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>tag</jv>.toString();
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"name"</js>: <js>"pet"</js>,
 * 		<js>"description"</js>: <js>"Pets operations"</js>
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Swagger}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(properties="name,description,externalDocs,*")
public class Tag extends SwaggerElement {

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

		this.description = copyFrom.description;
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this.name = copyFrom.name;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Tag copy() {
		return new Tag(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// description
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	<br>{@doc ext.GFM} can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setDescription(String value) {
		description = value;
	}

	/**
	 * Bean property fluent getter:  <property>description</property>.
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
	 * A short description for the tag.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Tag description(String value) {
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
	 */
	public void setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
	}

	/**
	 * Bean property fluent getter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation for this tag.
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
	 * Additional external documentation for this tag.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Tag externalDocs(ExternalDocumentation value) {
		setExternalDocs(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation for this tag as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	externalDocs(<js>"{description:'description',url:'url'}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property as JSON.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Tag externalDocs(String json) {
		setExternalDocs(toType(json, ExternalDocumentation.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// name
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setName(String value) {
		name = value;
	}

	/**
	 * Bean property fluent getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the tag.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> name() {
		return Optional.ofNullable(getName());
	}

	/**
	 * Bean property fluent setter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the tag.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Tag name(String value) {
		setName(value);
		return this;
	}


	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "description": return toType(getDescription(), type);
			case "externalDocs": return toType(getExternalDocs(), type);
			case "name": return toType(getName(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public Tag set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "description": return description(stringify(value));
			case "externalDocs": return externalDocs(toType(value, ExternalDocumentation.class));
			case "name": return name(stringify(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = ASet.<String>of()
			.appendIf(description != null, "description")
			.appendIf(externalDocs != null, "externalDocs")
			.appendIf(name != null, "name");
		return new MultiSet<>(s, super.keySet());
	}
}
