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
 * Allows adding meta data to a single tag that is used by the <a class="doclink" href="http://swagger.io/specification/#operationObject">Operation Object</a>.
 * <p>
 * It is not mandatory to have a Tag Object per tag used there.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	{
 * 		<js>"name"</js>: <js>"pet"</js>,
 * 		<js>"description"</js>: <js>"Pets operations"</js>
 * 	}
 * </p>
 * <p>
 * Refer to <a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.swagger</a> for usage information.
 */
@Bean(properties="name,description,externalDocs")
@SuppressWarnings("hiding")
public class Tag extends SwaggerElement {

	private String name;
	private String description;
	private ExternalDocumentation externalDocs;

	/**
	 * Bean property getter:  <property>name</property>.
	 * <p>
	 * Required. The name of the tag.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 * <p>
	 * Required. The name of the tag.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Tag setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Synonym for {@link #setName(String)}.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Tag name(String name) {
		return setName(name);
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * A short description for the tag.
	 * <a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * <p>
	 * A short description for the tag.
	 * <a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Tag setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Synonym for {@link #setDescription(String)}.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Tag description(String description) {
		return setDescription(description);
	}

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 * <p>
	 * Additional external documentation for this tag.
	 *
	 * @return The value of the <property>externalDocs</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	/**
	 * Bean property setter:  <property>externalDocs</property>.
	 * <p>
	 * Additional external documentation for this tag.
	 *
	 * @param externalDocs The new value for the <property>externalDocs</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Tag setExternalDocs(ExternalDocumentation externalDocs) {
		this.externalDocs = externalDocs;
		return this;
	}

	/**
	 * Synonym for {@link #setExternalDocs(ExternalDocumentation)}.
	 *
	 * @param externalDocs The new value for the <property>externalDocs</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Tag externalDocs(ExternalDocumentation externalDocs) {
		return setExternalDocs(externalDocs);
	}
}
