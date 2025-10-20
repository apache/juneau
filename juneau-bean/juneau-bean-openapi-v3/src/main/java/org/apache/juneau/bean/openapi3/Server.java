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

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * An object representing a Server.
 *
 * <p>
 * The Server Object represents a server that provides connectivity information to a target server. This can be used
 * to specify different servers for different environments (e.g., development, staging, production) or to provide
 * server-specific configuration such as variables for templating.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Server Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>url</c> (string, REQUIRED) - A URL to the target host. This URL supports Server Variables and may be relative
 * 	<li><c>description</c> (string) - An optional string describing the host designated by the URL (CommonMark syntax may be used)
 * 	<li><c>variables</c> (map of {@link ServerVariable}) - A map between a variable name and its value
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a server with variables</jc>
 * 	Server <jv>server</jv> = <jk>new</jk> Server()
 * 		.setUrl(<js>"https://{username}.gigantic-server.com:{port}/{basePath}"</js>)
 * 		.setDescription(<js>"The production API server"</js>)
 * 		.setVariables(
 * 			JsonMap.<jsm>of</jsm>(
 * 				<js>"username"</js>, <jk>new</jk> ServerVariable()
 * 					.setDefault(<js>"demo"</js>)
 * 					.setDescription(<js>"this value is assigned by the service provider"</js>),
 * 				<js>"port"</js>, <jk>new</jk> ServerVariable()
 * 					.setDefault(<js>"8443"</js>)
 * 					.setEnum(<js>"8443"</js>, <js>"443"</js>),
 * 				<js>"basePath"</js>, <jk>new</jk> ServerVariable()
 * 					.setDefault(<js>"v2"</js>)
 * 			)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#server-object">OpenAPI Specification &gt; Server Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/api-host-and-base-path/">OpenAPI API Host and Base Path</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class Server extends OpenApiElement {
	private URI url;
	private String description;
	private Map<String,ServerVariable> variables;

	/**
	 * Default constructor.
	 */
	public Server() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Server(Server copyFrom) {
		super(copyFrom);

		this.url = copyFrom.url;
		this.description = copyFrom.description;
		this.variables = CollectionUtils.copyOf(copyFrom.variables, ServerVariable::copy);
	}

	/**
	 * Adds one or more values to the <property>variables</property> property.
	 *
	 * @param key The mapping key.  Must not be <jk>null</jk>.
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public Server addVariable(String key, ServerVariable value) {
		assertArgNotNull("key", key);
		assertArgNotNull("value", value);
		variables = CollectionUtils.mapb(String.class, ServerVariable.class).to(variables).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Server copy() {
		return new Server(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "url" -> toType(getUrl(), type);
			case "description" -> toType(getDescription(), type);
			case "variables" -> toType(getVariables(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>url</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public URI getUrl() { return url; }

	/**
	 * Bean property getter:  <property>variables</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,ServerVariable> getVariables() { return variables; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = CollectionUtils.setb(String.class)
			.addIf(description != null, "description")
			.addIf(url != null, "url")
			.addIf(variables != null, "variables")
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Server set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> setDescription(Utils.s(value));
			case "url" -> setUrl(toURI(value));
			case "variables" -> setVariables(toMap(value, String.class, ServerVariable.class).sparse().build());
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Server setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>url</property>.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * <br>Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Server setUrl(URI value) {
		url = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>variables</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Server setVariables(Map<String,ServerVariable> value) {
		variables = CollectionUtils.copyOf(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Server strict(Object value) {
		super.strict(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	protected Server strict() {
		super.strict();
		return this;
	}
}