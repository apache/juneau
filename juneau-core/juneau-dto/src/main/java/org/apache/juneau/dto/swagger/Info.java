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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * The object provides metadata about the API.
 *
 * <p>
 * The metadata can be used by the clients if needed, and can be presented
 * in the Swagger-UI for convenience.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Info <jv>info</jv> = <jsm>info</jsm>(<js>"Swagger Sample App"</js>, <js>"1.0.1"</js>)
 * 		.description(<js>"This is a sample server Petstore server."</js>)
 * 		.termsOfService(<js>"http://swagger.io/terms/"</js>)
 * 		.contact(
 * 			<jsm>contact</jsm>(<js>"API Support"</js>, <js>"http://www.swagger.io/support"</js>, <js>"support@swagger.io"</js>)
 * 		)
 * 		.license(
 * 			<jsm>license</jsm>(<js>"Apache 2.0"</js>, <js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>)
 * 		);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.toString(<jv>info</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>info</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"title"</js>: <js>"Swagger Sample App"</js>,
 * 		<js>"description"</js>: <js>"This is a sample server Petstore server."</js>,
 * 		<js>"termsOfService"</js>: <js>"http://swagger.io/terms/"</js>,
 * 		<js>"contact"</js>: {
 * 			<js>"name"</js>: <js>"API Support"</js>,
 * 			<js>"url"</js>: <js>"http://www.swagger.io/support"</js>,
 * 			<js>"email"</js>: <js>"support@swagger.io"</js>
 * 		},
 * 		<js>"license"</js>: {
 * 			<js>"name"</js>: <js>"Apache 2.0"</js>,
 * 			<js>"url"</js>: <js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>
 * 		},
 * 		<js>"version"</js>: <js>"1.0.1"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="siteName,title,description,version,contact,license,termsOfService,*")
@FluentSetters
public class Info extends SwaggerElement {

	private String
		siteName,
		title,
		description,
		termsOfService,
		version;
	private Contact contact;
	private License license;

	/**
	 * Default constructor.
	 */
	public Info() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Info(Info copyFrom) {
		super(copyFrom);

		this.contact = copyFrom.contact == null ? null : copyFrom.contact.copy();
		this.description = copyFrom.description;
		this.license = copyFrom.license == null ? null : copyFrom.license.copy();
		this.siteName = copyFrom.siteName;
		this.termsOfService = copyFrom.termsOfService;
		this.title = copyFrom.title;
		this.version = copyFrom.version;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Info copy() {
		return new Info(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>contact</property>.
	 *
	 * <p>
	 * The contact information for the exposed API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Contact getContact() {
		return contact;
	}

	/**
	 * Bean property setter:  <property>contact</property>.
	 *
	 * <p>
	 * The contact information for the exposed API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info setContact(Contact value) {
		contact = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the application.
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
	 * A short description of the application.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>license</property>.
	 *
	 * <p>
	 * The license information for the exposed API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public License getLicense() {
		return license;
	}

	/**
	 * Bean property setter:  <property>license</property>.
	 *
	 * <p>
	 * The license information for the exposed API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info setLicense(License value) {
		license = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>siteName</property>.
	 *
	 * <p>
	 * The site name of the application.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getSiteName() {
		return siteName;
	}

	/**
	 * Bean property setter:  <property>siteName</property>.
	 *
	 * <p>
	 * The site name of the application.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Info setSiteName(String value) {
		siteName = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>termsOfService</property>.
	 *
	 * <p>
	 * The Terms of Service for the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTermsOfService() {
		return termsOfService;
	}

	/**
	 * Bean property setter:  <property>termsOfService</property>.
	 *
	 * <p>
	 * The Terms of Service for the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info setTermsOfService(String value) {
		termsOfService = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * <p>
	 * The title of the application.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Bean property setter:  <property>title</property>.
	 *
	 * <p>
	 * The title of the application.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Info setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>version</property>.
	 *
	 * <p>
	 * The version of the application API (not to be confused with the specification version).
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Bean property setter:  <property>version</property>.
	 *
	 * <p>
	 * The version of the application API (not to be confused with the specification version).
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object.
	 */
	public Info setVersion(String value) {
		version = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "contact": return toType(getContact(), type);
			case "description": return toType(getDescription(), type);
			case "license": return toType(getLicense(), type);
			case "siteName": return toType(getSiteName(), type);
			case "termsOfService": return toType(getTermsOfService(), type);
			case "title": return toType(getTitle(), type);
			case "version": return toType(getVersion(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public Info set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "contact": return setContact(toType(value, Contact.class));
			case "description": return setDescription(stringify(value));
			case "license": return setLicense(toType(value, License.class));
			case "siteName": return setSiteName(stringify(value));
			case "termsOfService": return setTermsOfService(stringify(value));
			case "title": return setTitle(stringify(value));
			case "version": return setVersion(stringify(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(contact != null, "contact")
			.addIf(description != null, "description")
			.addIf(license != null, "license")
			.addIf(siteName != null, "siteName")
			.addIf(termsOfService != null, "termsOfService")
			.addIf(title != null, "title")
			.addIf(version != null, "version")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
