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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.TreeMap;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.objecttools.*;

/**
 * This is the root document object for the OpenAPI specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.OpenApi">Overview &gt; juneau-rest-server &gt; OpenAPI</a>
 * </ul>
 */
@Bean(properties="openapi,info,servers,paths,components,security,tags,externalDocs,*")
@FluentSetters
public class OpenApi extends OpenApiElement {

	/** Represents a null OpenAPI document */
	public static final OpenApi NULL = new OpenApi();

	private static final Comparator<String> PATH_COMPARATOR = (o1, o2) -> o1.replace('{', '@').compareTo(o2.replace('{', '@'));

	private String openapi = "3.0.0";
	private Info info;
	private List<Server> servers;
	private Map<String,PathItem> paths;
	private Components components;
	private List<SecurityRequirement> security;
	private List<Tag> tags;
	private ExternalDocumentation externalDocs;

	/**
	 * Default constructor.
	 */
	public OpenApi() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public OpenApi(OpenApi copyFrom) {
		super(copyFrom);
		this.openapi = copyFrom.openapi;
		this.info = copyFrom.info;
		this.servers = copyOf(copyFrom.servers);
		this.paths = copyOf(copyFrom.paths);
		this.components = copyFrom.components;
		this.security = copyOf(copyFrom.security);
		this.tags = copyOf(copyFrom.tags);
		this.externalDocs = copyFrom.externalDocs;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public OpenApi copy() {
		return new OpenApi(this);
	}

	/**
	 * Returns the OpenAPI version.
	 *
	 * @return The OpenAPI version.
	 */
	public String getOpenapi() {
		return openapi;
	}

	/**
	 * Sets the OpenAPI version.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public OpenApi setOpenapi(String value) {
		this.openapi = value;
		return this;
	}

	/**
	 * Returns the info object.
	 *
	 * @return The info object.
	 */
	public Info getInfo() {
		return info;
	}

	/**
	 * Sets the info object.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public OpenApi setInfo(Info value) {
		this.info = value;
		return this;
	}

	/**
	 * Returns the servers list.
	 *
	 * @return The servers list.
	 */
	public List<Server> getServers() {
		return servers;
	}

	/**
	 * Sets the servers list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public OpenApi setServers(List<Server> value) {
		this.servers = value;
		return this;
	}

	/**
	 * Returns the paths map.
	 *
	 * @return The paths map.
	 */
	public Map<String,PathItem> getPaths() {
		return paths;
	}

	/**
	 * Sets the paths map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public OpenApi setPaths(Map<String,PathItem> value) {
		this.paths = mapBuilder(String.class,PathItem.class).sparse().sorted(PATH_COMPARATOR).addAll(value).build();
		return this;
	}

	/**
	 * Adds a path to this OpenAPI document.
	 *
	 * @param path The path string.
	 * @param pathItem The path item.
	 * @return This object.
	 */
	public OpenApi addPath(String path, PathItem pathItem) {
		if (paths == null)
			paths = new TreeMap<>(PATH_COMPARATOR);
		getPaths().put(path, pathItem);
		return this;
	}

	/**
	 * Returns the components object.
	 *
	 * @return The components object.
	 */
	public Components getComponents() {
		return components;
	}

	/**
	 * Sets the components object.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public OpenApi setComponents(Components value) {
		this.components = value;
		return this;
	}

	/**
	 * Returns the security requirements list.
	 *
	 * @return The security requirements list.
	 */
	public List<SecurityRequirement> getSecurity() {
		return security;
	}

	/**
	 * Sets the security requirements list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public OpenApi setSecurity(List<SecurityRequirement> value) {
		this.security = value;
		return this;
	}

	/**
	 * Returns the tags list.
	 *
	 * @return The tags list.
	 */
	public List<Tag> getTags() {
		return tags;
	}

	/**
	 * Sets the tags list.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public OpenApi setTags(List<Tag> value) {
		this.tags = value;
		return this;
	}

	/**
	 * Returns the external documentation.
	 *
	 * @return The external documentation.
	 */
	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	/**
	 * Sets the external documentation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public OpenApi setExternalDocs(ExternalDocumentation value) {
		this.externalDocs = value;
		return this;
	}

	/**
	 * Finds a reference within this OpenAPI document.
	 *
	 * @param ref The reference string (e.g., <js>"#/components/schemas/User"</js>).
	 * @param c The expected class type.
	 * @return The referenced node, or <jk>null</jk> if the ref was <jk>null</jk> or empty or not found.
	 */
	public <T> T findRef(String ref, Class<T> c) {
		if (Utils.isEmpty(ref))
			return null;
		if (! ref.startsWith("#/"))
			throw new BasicRuntimeException("Unsupported reference:  ''{0}''", ref);
		try {
			return new ObjectRest(this).get(ref.substring(1), c);
		} catch (Exception e) {
			throw new BeanRuntimeException(e, c, "Reference ''{0}'' could not be converted to type ''{1}''.", ref, className(c));
		}
	}

	@Override
	public String toString() {
		return JsonSerializer.DEFAULT_READABLE.toString(this);
	}
}
