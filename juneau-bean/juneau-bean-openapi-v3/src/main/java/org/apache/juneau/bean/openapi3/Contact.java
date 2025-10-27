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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.utils.*;

/**
 * Contact information for the exposed API.
 *
 * <p>
 * The Contact Object provides contact information for the exposed API. This information can be used by clients
 * to get in touch with the API maintainers for support, questions, or other inquiries.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Contact Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>name</c> (string) - The identifying name of the contact person/organization
 * 	<li><c>url</c> (string) - The URL pointing to the contact information
 * 	<li><c>email</c> (string) - The email address of the contact person/organization
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Contact <jv>x</jv> = <jsm>contact</jsm>(<js>"API Support"</js>, <js>"http://www.swagger.io/support"</js>, <js>"support@swagger.io"</js>);
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
 * 		<js>"name"</js>: <js>"API Support"</js>,
 * 		<js>"url"</js>: <js>"http://www.swagger.io/support"</js>,
 * 		<js>"email"</js>: <js>"support@swagger.io"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#contact-object">OpenAPI Specification &gt; Contact Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/api-general-info/">OpenAPI API General Info</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class Contact extends OpenApiElement {

	private String name;
	private URI url;
	private String email;

	/**
	 * Default constructor.
	 */
	public Contact() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Contact(Contact copyFrom) {
		super(copyFrom);

		this.name = copyFrom.name;
		this.url = copyFrom.url;
		this.email = copyFrom.email;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Contact copy() {
		return new Contact(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "name" -> toType(getName(), type);
			case "url" -> toType(getUrl(), type);
			case "email" -> toType(getEmail(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>email</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getEmail() { return email; }

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() { return name; }

	/**
	 * Bean property getter:  <property>url</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public URI getUrl() { return url; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = CollectionUtils.setb(String.class)
			.addIf(nn(email), "email")
			.addIf(nn(name), "name")
			.addIf(nn(url), "url")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Contact set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "email" -> setEmail(s(value));
			case "name" -> setName(s(value));
			case "url" -> setUrl(toURI(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>email</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>MUST be in the format of an email address.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Contact setEmail(String value) {
		email = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Contact setName(String value) {
		name = value;
		return this;
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
	public Contact setUrl(URI value) {
		url = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Contact strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Contact strict(Object value) {
		super.strict(value);
		return this;
	}
}