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

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * License information for the exposed API.
 *
 * <p>
 * The License Object provides license information for the exposed API in Swagger 2.0. This information helps clients
 * understand the terms under which the API can be used, including any restrictions or requirements.
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The License Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>name</c> (string, REQUIRED) - The license name used for the API
 * 	<li><c>url</c> (string) - A URL to the license used for the API
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	License <jv>license</jv> = <jsm>license</jsm>(<js>"Apache 2.0"</js>, <js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>license</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>license</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"name"</js>: <js>"Apache 2.0"</js>,
 * 		<js>"url"</js>: <js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#license-object">Swagger 2.0 Specification &gt; License Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/api-general-info/">Swagger API General Info</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class License extends SwaggerElement {

	private String name;
	private URI url;

	/**
	 * Default constructor.
	 */
	public License() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public License(License copyFrom) {
		super(copyFrom);

		this.name = copyFrom.name;
		this.url = copyFrom.url;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public License copy() {
		return new License(this);
	}
	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "name" -> toType(getName(), type);
			case "url" -> toType(getUrl(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The license name used for the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 *
	 * <p>
	 * A URL to the license used for the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public URI getUrl() {
		return url;
	}

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(name != null, "name")
			.addIf(url != null, "url")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from SwaggerElement */
	public License set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "name" -> setName(Utils.s(value));
			case "url" -> setUrl(StringUtils.toURI(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * The license name used for the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public License setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>url</property>.
	 *
	 * <p>
	 * A URL to the license used for the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>URIs defined by {@link UriResolver} can be used for values.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public License setUrl(URI value) {
		url = value;
		return this;
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @return This object.
	 */
	@Override
	public License strict() {
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
	public License strict(Object value) {
		super.strict(value);
		return this;
	}
}