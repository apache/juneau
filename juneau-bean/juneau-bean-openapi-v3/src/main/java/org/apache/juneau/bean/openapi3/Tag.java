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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * Allows adding metadata to a single tag that is used by the operation object.
 *
 * <p>
 * The Tag Object allows adding metadata to a single tag that is used by the Operation Object. It is not mandatory 
 * to have a Tag Object per tag used there, but it can be useful for providing additional information about tags 
 * such as descriptions and external documentation.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Tag Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>name</c> (string, REQUIRED) - The name of the tag
 * 	<li><c>description</c> (string) - A short description for the tag (CommonMark syntax may be used)
 * 	<li><c>externalDocs</c> ({@link ExternalDocumentation}) - Additional external documentation for this tag
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Tag <jv>x</jv> = <jsm>tag</jsm>()
 * 		.name(<js>"pet"</js>)
 * 		.description(<js>"Pets operations"</js>)
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>x</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String <jv>json</jv> = <jv>x</jv>.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"name"</js>: <js>"pet"</js>,
 * 		<js>"description"</js>: <js>"Pets operations"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#tag-object">OpenAPI Specification &gt; Tag Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/grouping-operations-with-tags/">OpenAPI Grouping Operations with Tags</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
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
	 * 	<br>Can be <jk>null</jk> to unset the property.
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

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "name" -> toType(getName(), type);
			case "description" -> toType(getDescription(), type);
			case "externalDocs" -> toType(getExternalDocs(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* Overridden from OpenApiElement */
	public Tag set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> setDescription(Utils.s(value));
			case "externalDocs" -> setExternalDocs(toType(value, ExternalDocumentation.class));
			case "name" -> setName(Utils.s(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(description != null, "description")
			.addIf(externalDocs != null, "externalDocs")
			.addIf(name != null, "name")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Tag strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Tag strict(Object value) {
		super.strict(value);
		return this;
	}

}