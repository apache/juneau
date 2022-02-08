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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Swagger}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(properties="siteName,title,description,version,contact,license,termsOfService,*")
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
	// contact
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
	 */
	public void setContact(Contact value) {
		contact = value;
	}

	/**
	 * Bean property fluent getter:  <property>contact</property>.
	 *
	 * <p>
	 * The contact information for the exposed API.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Contact> contact() {
		return Optional.ofNullable(getContact());
	}

	/**
	 * Bean property fluent setter:  <property>contact</property>.
	 *
	 * <p>
	 * The contact information for the exposed API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info contact(Contact value) {
		setContact(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>contact</property>.
	 *
	 * <p>
	 * The contact information for the exposed API as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	contact(<js>"{name:'name',url:'url',...}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property as JSON.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info contact(String json) {
		setContact(toType(json, Contact.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// description
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	<br>{@doc ext.GFM} can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setDescription(String value) {
		description = value;
	}

	/**
	 * Bean property fluent getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the application.
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
	 * A short description of the application.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>{@doc ext.GFM} can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info description(String value) {
		setDescription(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// license
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setLicense(License value) {
		license = value;
	}

	/**
	 * Bean property fluent getter:  <property>license</property>.
	 *
	 * <p>
	 * The license information for the exposed API.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<License> license() {
		return Optional.ofNullable(getLicense());
	}

	/**
	 * Bean property fluent setter:  <property>license</property>.
	 *
	 * <p>
	 * The license information for the exposed API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info license(License value) {
		setLicense(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>license</property>.
	 *
	 * <p>
	 * The license information for the exposed API as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	license(<js>"{name:'name',url:'url',...}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property as JSON.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info license(String json) {
		setLicense(toType(json, License.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// siteName
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setSiteName(String value) {
		siteName = value;
	}

	/**
	 * Bean property fluent getter:  <property>siteName</property>.
	 *
	 * <p>
	 * The site name of the application.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> siteName() {
		return Optional.ofNullable(getSiteName());
	}

	/**
	 * Bean property fluent setter:  <property>siteName</property>.
	 *
	 * <p>
	 * The site name of the application.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Info siteName(String value) {
		setSiteName(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// termsOfService
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setTermsOfService(String value) {
		termsOfService = value;
	}

	/**
	 * Bean property fluent getter:  <property>termsOfService</property>.
	 *
	 * <p>
	 * The Terms of Service for the API.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> termsOfService() {
		return Optional.ofNullable(getTermsOfService());
	}

	/**
	 * Bean property fluent setter:  <property>termsOfService</property>.
	 *
	 * <p>
	 * The Terms of Service for the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info termsOfService(String value) {
		setTermsOfService(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// title
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setTitle(String value) {
		title = value;
	}

	/**
	 * Bean property fluent getter:  <property>title</property>.
	 *
	 * <p>
	 * The title of the application.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> title() {
		return Optional.ofNullable(getTitle());
	}

	/**
	 * Bean property fluent setter:  <property>title</property>.
	 *
	 * <p>
	 * The title of the application.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info title(String value) {
		setTitle(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// version
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setVersion(String value) {
		version = value;
	}

	/**
	 * Bean property fluent getter:  <property>version</property>.
	 *
	 * <p>
	 * The version of the application API (not to be confused with the specification version).
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> version() {
		return Optional.ofNullable(getVersion());
	}

	/**
	 * Bean property fluent setter:  <property>version</property>.
	 *
	 * <p>
	 * The version of the application API (not to be confused with the specification version).
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Info version(String value) {
		setVersion(value);
		return this;
	}


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
			case "contact": return contact(toType(value, Contact.class));
			case "description": return description(stringify(value));
			case "license": return license(toType(value, License.class));
			case "siteName": return siteName(stringify(value));
			case "termsOfService": return termsOfService(stringify(value));
			case "title": return title(stringify(value));
			case "version": return version(stringify(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = ASet.<String>of()
			.appendIf(contact != null, "contact")
			.appendIf(description != null, "description")
			.appendIf(license != null, "license")
			.appendIf(siteName != null, "siteName")
			.appendIf(termsOfService != null, "termsOfService")
			.appendIf(title != null, "title")
			.appendIf(version != null, "version");
		return new MultiSet<>(s, super.keySet());
	}
}
