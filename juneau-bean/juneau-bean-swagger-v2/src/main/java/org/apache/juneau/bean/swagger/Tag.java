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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.utils.*;

/**
 * Allows adding metadata to a single tag that is used by the Operation Object.
 *
 * <p>
 * The Tag Object allows adding metadata to a single tag that is used by the Operation Object in Swagger 2.0.
 * It is not mandatory to have a Tag Object per tag used there, but it can be useful for providing additional
 * information about tags such as descriptions and external documentation.
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The Tag Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>name</c> (string, REQUIRED) - The name of the tag
 * 	<li><c>description</c> (string) - A short description for the tag
 * 	<li><c>externalDocs</c> ({@link ExternalDocumentation}) - Additional external documentation for this tag
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Tag <jv>tag</jv> = <jsm>tag</jsm>()
 * 		.name(<js>"pet"</js>)
 * 		.description(<js>"Pets operations"</js>)
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>tag</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>tag</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"name"</js>: <js>"pet"</js>,
 * 		<js>"description"</js>: <js>"Pets operations"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#tag-object">Swagger 2.0 Specification &gt; Tag Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/grouping-operations-with-tags/">Swagger Grouping Operations with Tags</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class Tag extends SwaggerElement {

	private String name, description;
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

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> toType(getDescription(), type);
			case "externalDocs" -> toType(getExternalDocs(), type);
			case "name" -> toType(getName(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description for the tag.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation for this tag.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public ExternalDocumentation getExternalDocs() { return externalDocs; }

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the tag.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() { return name; }

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = CollectionUtils.setb(String.class)
			.addIf(nn(description), "description")
			.addIf(nn(externalDocs), "externalDocs")
			.addIf(nn(name), "name")
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from SwaggerElement */
	public Tag set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> setDescription(s(value));
			case "externalDocs" -> setExternalDocs(toType(value, ExternalDocumentation.class));
			case "name" -> setName(s(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * <p>
	 * A short description for the tag.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Tag setDescription(String value) {
		description = value;
		return this;
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
	 * @return This object.
	 */
	public Tag setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
		return this;
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
	 * @return This object.
	 */
	public Tag setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @return This object.
	 */
	@Override
	public Tag strict() {
		super.strict();
		return this;
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> (interpreted as <jk>false</jk>).
	 * @return This object.
	 */
	@Override
	public Tag strict(Object value) {
		super.strict(value);
		return this;
	}
}