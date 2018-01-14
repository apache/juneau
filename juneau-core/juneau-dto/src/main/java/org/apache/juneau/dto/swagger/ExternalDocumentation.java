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

import java.net.*;
import java.net.URI;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Allows referencing an external resource for extended documentation.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	{
 * 		<js>"description"</js>: <js>"Find more info here"</js>,
 * 		<js>"url"</js>: <js>"https://swagger.io"</js>
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
@Bean(properties="description,url")
public class ExternalDocumentation extends SwaggerElement {

	private String description;
	private URI url;

	/**
	 * Bean property getter:  <property>description</property>.
	 * 
	 * <p>
	 * A short description of the target documentation. GFM syntax can be used for rich text representation.
	 * 
	 * @return
	 * 	The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * 
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
	 * Synonym for {@link #setDescription(String)}.
	 * 
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ExternalDocumentation description(String description) {
		return setDescription(description);
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 * 
	 * <p>
	 * Required. The URL for the target documentation.
	 * 
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 * 
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 * 
	 * @return The value of the <property>url</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public URI getUrl() {
		return url;
	}

	/**
	 * Bean property setter:  <property>url</property>.
	 * 
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 * 
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 * 
	 * @param url The new value for the <property>url</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ExternalDocumentation setUrl(Object url) {
		this.url = toURI(url);
		return this;
	}

	/**
	 * Synonym for {@link #setUrl(Object)}.
	 * 
	 * @param url The new value for the <property>url</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ExternalDocumentation url(Object url) {
		return setUrl(url);
	}
}
