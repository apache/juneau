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

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * Contact information for the exposed API.
 *
 * <p>
 * The Contact Object provides contact information for the exposed API in Swagger 2.0. This information can be used
 * by clients to get in touch with the API maintainers for support, questions, or other inquiries.
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The Contact Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>name</c> (string) - The identifying name of the contact person/organization
 * 	<li><c>url</c> (string) - The URL pointing to the contact information
 * 	<li><c>email</c> (string) - The email address of the contact person/organization
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Contact <jv>contact</jv> = <jsm>contact</jsm>(<js>"API Support"</js>, <js>"http://www.swagger.io/support"</js>, <js>"support@swagger.io"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>contact</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>contact</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"name"</js>: <js>"API Support"</js>,
 * 		<js>"url"</js>: <js>"http://www.swagger.io/support"</js>,
 * 		<js>"email"</js>: <js>"support@swagger.io"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#contact-object">Swagger 2.0 Specification &gt; Contact Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/api-general-info/">Swagger API General Info</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class Contact extends SwaggerElement {

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

		this.email = copyFrom.email;
		this.name = copyFrom.name;
		this.url = copyFrom.url;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Contact copy() {
		return new Contact(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>email</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getEmail() {
		return email;
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
	 * @return This object.
	 */
	public Contact setEmail(String value) {
		email = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
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
	 * The identifying name of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Contact setName(String value) {
		name = value;
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
	 * The URL pointing to the contact information.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Contact setUrl(URI value) {
		url = value;
		return this;
	}

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "email" -> toType(getEmail(), type);
			case "name" -> toType(getName(), type);
			case "url" -> toType(getUrl(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* Overridden from SwaggerElement */
	public Contact set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "email" -> setEmail(Utils.s(value));
			case "name" -> setName(Utils.s(value));
			case "url" -> setUrl(toURI(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(email != null, "email")
			.addIf(name != null, "name")
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
	public Contact strict() {
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
	public Contact strict(Object value) {
		super.strict(value);
		return this;
	}
}