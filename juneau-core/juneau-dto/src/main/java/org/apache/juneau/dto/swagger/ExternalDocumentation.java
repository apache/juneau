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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

/**
 * Allows referencing an external resource for extended documentation.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	ExternalDocumentation <jv>extDoc</jv> = <jsm>externalDocumentation</jsm>(<js>"https://swagger.io"</js>, <js>"Find more info here"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.toString(<jv>extDoc</jv>);
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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="description,url,*")
@FluentSetters
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
	 * @return This object.
	 */
	public ExternalDocumentation setUrl(URI value) {
		url = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "description": return toType(getDescription(), type);
			case "url": return toType(getUrl(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public ExternalDocumentation set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "description": return setDescription(stringify(value));
			case "url": return setUrl(StringUtils.toURI(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(description != null, "description")
			.addIf(url != null, "url")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
