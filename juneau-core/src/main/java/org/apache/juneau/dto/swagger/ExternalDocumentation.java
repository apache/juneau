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
 * Allows referencing an external resource for extended documentation.
 * <p>
 * Example:
 * <p class='bcode'>
 * 	{
 * 		<js>"description"</js>: <js>"Find more info here"</js>,
 * 		<js>"url"</js>: <js>"https://swagger.io"</js>
 * 	}
 * </p>
 *
 * @author james.bognar
 */
@Bean(properties="description,url")
public class ExternalDocumentation {

	private String description;
	private String url;

	/**
	 * Convenience method for creating a new ExternalDocumentation object.
	 * 
	 * @param url Required.  The URL for the target documentation. Value MUST be in the format of a URL.
	 * @return A new ExternalDocumentation object.
	 */
	public static ExternalDocumentation create(String url) {
		return new ExternalDocumentation().setUrl(url);
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * A short description of the target documentation. GFM syntax can be used for rich text representation.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * <p>
	 * A short description of the target documentation. GFM syntax can be used for rich text representation.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ExternalDocumentation setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 * <p>
	 * Required. The URL for the target documentation. Value MUST be in the format of a URL.
	 *
	 * @return The value of the <property>url</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Bean property setter:  <property>url</property>.
	 * <p>
	 * Required. The URL for the target documentation. Value MUST be in the format of a URL.
	 *
	 * @param url The new value for the <property>url</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ExternalDocumentation setUrl(String url) {
		this.url = url;
		return this;
	}
}
