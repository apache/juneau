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
package org.apache.juneau.rest.beans;

import static org.apache.juneau.internal.ObjectUtils.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;

/**
 * Shortcut label for child resources.
 *
 * <p>
 * Typically used in router resources.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>new</jk> ResourceDescription(<js>"httpTool"</js>, <js>"HTTP request test client"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.UtilityBeans">Utility Beans</a>
 * </ul>
 */
@Bean(properties="name,description", findFluentSetters=true)
@Response(schema=@Schema(ignore=true))
public final class ResourceDescription implements Comparable<ResourceDescription> {

	private String name, uri, description;

	/**
	 * Constructor for when the name and uri are the same.
	 *
	 * @param name The name of the child resource.
	 * @param description The description of the child resource.
	 */
	public ResourceDescription(String name, String description) {
		this.name = name;
		this.uri = name;
		this.description = description;
	}

	/**
	 * Constructor for when the name and uri are different.
	 *
	 * @param name The name of the child resource.
	 * @param uri The uri of the child resource.
	 * @param description The description of the child resource.
	 */
	public ResourceDescription(String name, String uri, String description) {
		this.name = name;
		this.uri = uri;
		this.description = description;
	}

	/** No-arg constructor.  Used for JUnit testing of OPTIONS pages. */
	public ResourceDescription() {}

	/**
	 * Returns the name field on this label.
	 *
	 * @return The name.
	 */
	@Html(link="servlet:/{uri}")
	public String getName() {
		return name;
	}

	/**
	 * Returns the uri on this label.
	 *
	 * @return The name.
	 */
	public String getUri() {
		return uri == null ? name : uri;
	}

	/**
	 * Sets the name field on this label to a new value.
	 *
	 * @param name The new name.
	 * @return This object.
	 */
	public ResourceDescription name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Returns the description field on this label.
	 *
	 * @return The description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description field on this label to a new value.
	 *
	 * @param description The new description.
	 * @return This object.
	 */
	public ResourceDescription description(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the uri field on this label to a new value.
	 *
	 * @param uri The new uri.
	 * @return This object.
	 */
	public ResourceDescription uri(String uri) {
		this.uri = uri;
		return this;
	}

	@Override /* Comparable */
	public int compareTo(ResourceDescription o) {
		return getName().toString().compareTo(o.getName().toString());
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof ResourceDescription) && eq(this, (ResourceDescription)o, (x,y)->eq(x.getName(), y.getName()));
	}

	@Override /* Object */
	public int hashCode() {
		return getName().hashCode();
	}
}
