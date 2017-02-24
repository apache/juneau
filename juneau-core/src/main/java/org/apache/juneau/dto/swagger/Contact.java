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
 * Contact information for the exposed API.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	{
 * 		<js>"name"</js>: <js>"API Support"</js>,
 * 		<js>"url"</js>: <js>"http://www.swagger.io/support"</js>,
 * 		<js>"email"</js>: <js>"support@swagger.io"</js>
 * 	}
 * </p>
 * <p>
 * Refer to <a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.swagger</a> for usage information.
 */
@Bean(properties="name,url,email")
@SuppressWarnings("hiding")
public class Contact extends SwaggerElement {

	private String name;
	private String url;
	private String email;

	/**
	 * Bean property getter:  <property>name</property>.
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Contact setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Synonym for {@link #setName(String)}.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Contact name(String name) {
		return setName(name);
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 * <p>
	 * The URL pointing to the contact information. MUST be in the format of a URL.
	 *
	 * @return The value of the <property>url</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Bean property setter:  <property>url</property>.
	 * <p>
	 * The URL pointing to the contact information. MUST be in the format of a URL.
	 *
	 * @param url The new value for the <property>url</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Contact setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * Synonym for {@link #setUrl(String)}.
	 *
	 * @param url The new value for the <property>url</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Contact url(String url) {
		return setName(url);
	}

	/**
	 * Bean property getter:  <property>email</property>.
	 * <p>
	 * The email address of the contact person/organization. MUST be in the format of an email address.
	 *
	 * @return The value of the <property>email</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Bean property setter:  <property>email</property>.
	 * <p>
	 * The email address of the contact person/organization. MUST be in the format of an email address.
	 *
	 * @param email The new value for the <property>email</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Contact setEmail(String email) {
		this.email = email;
		return this;
	}

	/**
	 * Synonym for {@link #setEmail(String)}.
	 *
	 * @param email The new value for the <property>email</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Contact email(String email) {
		return setEmail(email);
	}
}
