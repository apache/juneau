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
 * The object provides metadata about the API. The metadata can be used by the clients if needed, and can be presented in the Swagger-UI for convenience.
 *
 * <h6 class='topic'>Example:</h6>
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
 * @author james.bognar
 */
@Bean(properties="title,description,termsOfService,contact,license,version")
public class Info {

	private String title;
	private String description;
	private String termsOfService;
	private Contact contact;
	private License license;
	private String version;

	/**
	 * Convenience method for creating a new Info object.
	 *
	 * @param title Required.  The title of the application.
	 * @param version Required.  Provides the version of the application API (not to be confused with the specification version).
	 * @return A new Info object.
	 */
	public static Info create(String title, String version) {
		return new Info().setTitle(title).setVersion(version);
	}

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
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * A short description of the application. GFM syntax can be used for rich text representation.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
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
	 * Bean property getter:  <property>termsOfService</property>.
	 * <p>
	 * The Terms of Service for the API.
	 *
	 * @return The value of the <property>termsOfService</property> property on this bean, or <jk>null</jk> if it is not set.
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
}
