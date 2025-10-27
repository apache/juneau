/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.json.*;
import org.apache.juneau.objecttools.*;

/**
 * This is the root document object for the OpenAPI specification.
 *
 * <p>
 * The OpenAPI Object is the root document that describes an entire API. It contains metadata about the API,
 * available paths and operations, parameters, responses, authentication methods, and other information.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The OpenAPI Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>openapi</c> (string, REQUIRED) - The OpenAPI Specification version (e.g., "3.0.0")
 * 	<li><c>info</c> ({@link Info}, REQUIRED) - Provides metadata about the API
 * 	<li><c>servers</c> (array of {@link Server}) - An array of Server Objects providing connectivity information
 * 	<li><c>paths</c> (map of {@link PathItem}) - The available paths and operations for the API
 * 	<li><c>components</c> ({@link Components}) - An element to hold various schemas for reuse
 * 	<li><c>security</c> (array of {@link SecurityRequirement}) - Security mechanisms applied to all operations
 * 	<li><c>tags</c> (array of {@link Tag}) - A list of tags for API documentation control
 * 	<li><c>externalDocs</c> ({@link ExternalDocumentation}) - Additional external documentation
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an OpenAPI document</jc>
 * 	OpenApi <jv>doc</jv> = <jk>new</jk> OpenApi()
 * 		.setOpenapi(<js>"3.0.0"</js>)
 * 		.setInfo(
 * 			<jk>new</jk> Info()
 * 				.setTitle(<js>"My API"</js>)
 * 				.setVersion(<js>"1.0.0"</js>)
 * 		)
 * 		.setPaths(
 * 			JsonMap.<jsm>of</jsm>(
 * 				<js>"/pets"</js>, <jk>new</jk> PathItem()
 * 					.setGet(<jk>new</jk> Operation()
 * 						.setSummary(<js>"List all pets"</js>)
 * 					)
 * 			)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#openapi-object">OpenAPI Specification &gt; OpenAPI Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/basic-structure/">OpenAPI Basic Structure</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
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
		this.servers = CollectionUtils.copyOf(copyFrom.servers);
		this.paths = CollectionUtils.copyOf(copyFrom.paths);
		this.components = copyFrom.components;
		this.security = CollectionUtils.copyOf(copyFrom.security);
		this.tags = CollectionUtils.copyOf(copyFrom.tags);
		this.externalDocs = copyFrom.externalDocs;
	}

	/**
	 * Adds a path to this OpenAPI document.
	 *
	 * @param path The path string.  Must not be <jk>null</jk>.
	 * @param pathItem The path item.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public OpenApi addPath(String path, PathItem pathItem) {
		assertArgNotNull("path", path);
		assertArgNotNull("pathItem", pathItem);
		if (paths == null)
			paths = new TreeMap<>(PATH_COMPARATOR);
		getPaths().put(path, pathItem);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security mechanisms can be used across the API.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public OpenApi addSecurity(Collection<SecurityRequirement> values) {
		security = CollectionUtils.listb(SecurityRequirement.class).sparse().addAny(security, values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security mechanisms can be used across the API.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public OpenApi addSecurity(SecurityRequirement...values) {
		security = CollectionUtils.listb(SecurityRequirement.class).sparse().addAll(security).addAny((Object)values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>servers</property>.
	 *
	 * <p>
	 * An array of Server Objects, which provide connectivity information to a target server.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public OpenApi addServers(Collection<Server> values) {
		servers = CollectionUtils.listb(Server.class).to(servers).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>servers</property>.
	 *
	 * <p>
	 * An array of Server Objects, which provide connectivity information to a target server.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public OpenApi addServers(Server...values) {
		servers = CollectionUtils.listb(Server.class).to(servers).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property appender:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public OpenApi addTags(Collection<Tag> values) {
		tags = CollectionUtils.listb(Tag.class).to(tags).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property appender:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public OpenApi addTags(Tag...values) {
		tags = CollectionUtils.listb(Tag.class).to(tags).sparse().add(values).build();
		return this;
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
	 * Finds a reference within this OpenAPI document.
	 *
	 * @param ref The reference string (e.g., <js>"#/components/schemas/User"</js>).  Must not be <jk>null</jk> or blank.
	 * @param c The expected class type.  Must not be <jk>null</jk>.
	 * @return The referenced node, or <jk>null</jk> if not found.
	 */
	public <T> T findRef(String ref, Class<T> c) {
		assertArgNotNullOrBlank("ref", ref);
		assertArgNotNull("c", c);
		if (! ref.startsWith("#/"))
			throw new BasicRuntimeException("Unsupported reference:  ''{0}''", ref);
		try {
			return new ObjectRest(this).get(ref.substring(1), c);
		} catch (Exception e) {
			throw new BeanRuntimeException(e, c, "Reference ''{0}'' could not be converted to type ''{1}''.", ref, ClassUtils.className(c));
		}
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "openapi" -> toType(getOpenapi(), type);
			case "info" -> toType(getInfo(), type);
			case "servers" -> toType(getServers(), type);
			case "paths" -> toType(getPaths(), type);
			case "components" -> toType(getComponents(), type);
			case "security" -> toType(getSecurity(), type);
			case "tags" -> toType(getTags(), type);
			case "externalDocs" -> toType(getExternalDocs(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Returns the components object.
	 *
	 * @return The components object.
	 */
	public Components getComponents() { return components; }

	/**
	 * Returns the external documentation.
	 *
	 * @return The external documentation.
	 */
	public ExternalDocumentation getExternalDocs() { return externalDocs; }

	/**
	 * Returns the info object.
	 *
	 * @return The info object.
	 */
	public Info getInfo() { return info; }

	/**
	 * Returns the OpenAPI version.
	 *
	 * @return The OpenAPI version.
	 */
	public String getOpenapi() { return openapi; }

	/**
	 * Returns the paths map.
	 *
	 * @return The paths map.
	 */
	public Map<String,PathItem> getPaths() { return paths; }

	/**
	 * Returns the security requirements list.
	 *
	 * @return The security requirements list.
	 */
	public List<SecurityRequirement> getSecurity() { return security; }

	/**
	 * Returns the servers list.
	 *
	 * @return The servers list.
	 */
	public List<Server> getServers() { return servers; }

	/**
	 * Returns the tags list.
	 *
	 * @return The tags list.
	 */
	public List<Tag> getTags() { return tags; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = CollectionUtils.setb(String.class)
			.addIf(nn(components), "components")
			.addIf(nn(externalDocs), "externalDocs")
			.addIf(nn(info), "info")
			.addIf(nn(openapi), "openapi")
			.addIf(nn(paths), "paths")
			.addIf(nn(security), "security")
			.addIf(nn(servers), "servers")
			.addIf(nn(tags), "tags")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public OpenApi set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "components" -> setComponents(toType(value, Components.class));
			case "externalDocs" -> setExternalDocs(toType(value, ExternalDocumentation.class));
			case "info" -> setInfo(toType(value, Info.class));
			case "openapi" -> setOpenapi(s(value));
			case "paths" -> setPaths(toMap(value, String.class, PathItem.class).sparse().build());
			case "security" -> setSecurity(toList(value, SecurityRequirement.class).sparse().build());
			case "servers" -> setServers(toList(value, Server.class).sparse().build());
			case "tags" -> setTags(toList(value, Tag.class).sparse().build());
			default -> {
				super.set(property, value);
				yield this;
			}
		};
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
	 * Sets the paths map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public OpenApi setPaths(Map<String,PathItem> value) {
		this.paths = toMap(value, String.class, PathItem.class).sparse().sorted(PATH_COMPARATOR).build();
		return this;
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
	 * Bean property setter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public OpenApi setTags(Tag...value) {
		setTags(toList(value, Tag.class).sparse().build());
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public OpenApi strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public OpenApi strict(Object value) {
		super.strict(value);
		return this;
	}

	@Override
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}
}