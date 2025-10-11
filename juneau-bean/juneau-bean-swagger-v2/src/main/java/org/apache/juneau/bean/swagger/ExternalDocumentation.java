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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * Allows referencing an external resource for extended documentation.
 *
 * <p>
 * The External Documentation Object allows referencing an external resource for extended documentation in Swagger 2.0. 
 * This can be used to provide additional documentation that is not part of the main Swagger specification, such as 
 * detailed guides, tutorials, or API documentation hosted elsewhere.
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The External Documentation Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>description</c> (string) - A short description of the target documentation
 * 	<li><c>url</c> (string, REQUIRED) - The URL for the target documentation
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	ExternalDocumentation <jv>extDoc</jv> = <jsm>externalDocumentation</jsm>(<js>"https://swagger.io"</js>, <js>"Find more info here"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>extDoc</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>extDoc</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"description"</js>: <js>"Find more info here"</js>,
 * 		<js>"url"</js>: <js>"https://swagger.io"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#external-documentation-object">Swagger 2.0 Specification &gt; External Documentation Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/external-documentation/">Swagger External Documentation</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class ExternalDocumentation extends SwaggerElement {

	private String description;
	private URI url;

	/**
	 * Default constructor.
	 */
	public ExternalDocumentation() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public ExternalDocumentation(ExternalDocumentation copyFrom) {
		super(copyFrom);

		this.description = copyFrom.description;
		this.url = copyFrom.url;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public ExternalDocumentation copy() {
		return new ExternalDocumentation(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the target documentation.
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
	 * A short description of the target documentation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ExternalDocumentation setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 *
	 * <p>
	 * The URL for the target documentation.
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
	 * The URL for the target documentation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>URIs defined by {@link UriResolver} can be used for values.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ExternalDocumentation setUrl(URI value) {
		url = value;
		return this;
	}

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> toType(getDescription(), type);
			case "url" -> toType(getUrl(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* Overridden from SwaggerElement */
	public ExternalDocumentation set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> setDescription(Utils.s(value));
			case "url" -> setUrl(StringUtils.toURI(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(description != null, "description")
			.addIf(url != null, "url")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @return This object.
	 */
	@Override
	public ExternalDocumentation strict() {
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
	public ExternalDocumentation strict(Object value) {
		super.strict(value);
		return this;
	}

}