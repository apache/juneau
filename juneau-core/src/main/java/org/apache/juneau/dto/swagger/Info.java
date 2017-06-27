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

import org.apache.juneau.annotation.*;

/**
 * The object provides metadata about the API. The metadata can be used by the clients if needed, and can be presented
 * in the Swagger-UI for convenience.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
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
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 		<ul>
 * 			<li class='sublink'>
 * 				<a class='doclink' href='../../../../../overview-summary.html#DTOs.Swagger'>Swagger</a>
 * 		</ul>
 * 	</li>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.swagger</a>
 * 	</li>
 * </ul>
 */
@Bean(properties="title,description,termsOfService,contact,license,version")
@SuppressWarnings("hiding")
public class Info extends SwaggerElement {

	private String title;
	private String description;
	private String termsOfService;
	private Contact contact;
	private License license;
	private String version;

	/**
	 * Bean property getter:  <property>title</property>.
	 * <p>
	 * Required.  The title of the application.
	 *
	 * @return The value of the <property>title</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Bean property setter:  <property>title</property>.
	 * <p>
	 * Required.  The title of the application.
	 *
	 * @param title The new value for the <property>title</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Synonym for {@link #setTitle(String)}.
	 *
	 * @param title The new value for the <property>title</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info title(String title) {
		return setTitle(title);
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * A short description of the application. GFM syntax can be used for rich text representation.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not
	 * set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * <p>
	 * A short description of the application. GFM syntax can be used for rich text representation.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Synonym for {@link #setDescription(String)}.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info description(String description) {
		return setDescription(description);
	}

	/**
	 * Bean property getter:  <property>termsOfService</property>.
	 * <p>
	 * The Terms of Service for the API.
	 *
	 * @return The value of the <property>termsOfService</property> property on this bean, or <jk>null</jk> if it is not
	 * set.
	 */
	public String getTermsOfService() {
		return termsOfService;
	}

	/**
	 * Bean property setter:  <property>termsOfService</property>.
	 * <p>
	 * The Terms of Service for the API.
	 *
	 * @param termsOfService The new value for the <property>termsOfService</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info setTermsOfService(String termsOfService) {
		this.termsOfService = termsOfService;
		return this;
	}

	/**
	 * Synonym for {@link #setTermsOfService(String)}.
	 *
	 * @param termsOfService The new value for the <property>termsOfService</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info termsOfService(String termsOfService) {
		return setTermsOfService(termsOfService);
	}

	/**
	 * Bean property getter:  <property>contact</property>.
	 * <p>
	 * The contact information for the exposed API.
	 *
	 * @return The value of the <property>contact</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Contact getContact() {
		return contact;
	}

	/**
	 * Bean property setter:  <property>contact</property>.
	 * <p>
	 * The contact information for the exposed API.
	 *
	 * @param contact The new value for the <property>contact</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info setContact(Contact contact) {
		this.contact = contact;
		return this;
	}

	/**
	 * Synonym for {@link #setContact(Contact)}.
	 *
	 * @param contact The new value for the <property>contact</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info contact(Contact contact) {
		return setContact(contact);
	}

	/**
	 * Bean property getter:  <property>license</property>.
	 * <p>
	 * The license information for the exposed API.
	 *
	 * @return The value of the <property>license</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public License getLicense() {
		return license;
	}

	/**
	 * Bean property setter:  <property>license</property>.
	 * <p>
	 * The license information for the exposed API.
	 *
	 * @param license The new value for the <property>license</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info setLicense(License license) {
		this.license = license;
		return this;
	}

	/**
	 * Synonym for {@link #setLicense(License)}.
	 *
	 * @param license The new value for the <property>license</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info license(License license) {
		return setLicense(license);
	}

	/**
	 * Bean property getter:  <property>version</property>.
	 * <p>
	 * Required.  Provides the version of the application API (not to be confused with the specification version).
	 *
	 * @return The value of the <property>version</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Bean property setter:  <property>version</property>.
	 * <p>
	 * Required.  Provides the version of the application API (not to be confused with the specification version).
	 *
	 * @param version The new value for the <property>version</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info setVersion(String version) {
		this.version = version;
		return this;
	}

	/**
	 * Synonym for {@link #setVersion(String)}.
	 *
	 * @param version The new value for the <property>version</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Info version(String version) {
		return setVersion(version);
	}
}
