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
 * License information for the exposed API.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	{
 * 		<js>"name"</js>: <js>"Apache 2.0"</js>,
 * 		<js>"url"</js>: <js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>
 * 	}
 * </p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects (org.apache.juneau.dto)</a>
 * 	<ul>
 * 		<li class='sublink'><a class='doclink' href='../../../../../overview-summary.html#DTOs.Swagger'>Swagger</a>
 * 	</ul>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.swagger</a>
 * </ul>
 */
@Bean(properties="name,url")
@SuppressWarnings("hiding")
public class License extends SwaggerElement {

	private String name;
	private String url;

	/**
	 * Bean property getter:  <property>name</property>.
	 * <p>
	 * Required. The license name used for the API.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 * <p>
	 * Required. The license name used for the API.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public License setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Synonym for {@link #setName(String)}.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public License name(String name) {
		return setName(name);
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 * <p>
	 * A URL to the license used for the API. MUST be in the format of a URL.
	 *
	 * @return The value of the <property>url</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Bean property setter:  <property>url</property>.
	 * <p>
	 * A URL to the license used for the API. MUST be in the format of a URL.
	 *
	 * @param url The new value for the <property>url</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public License setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * Synonym for {@link #setUrl(String)}.
	 *
	 * @param url The new value for the <property>url</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public License url(String url) {
		return setUrl(url);
	}
}
