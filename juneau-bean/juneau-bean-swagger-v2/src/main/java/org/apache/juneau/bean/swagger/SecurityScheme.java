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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ArrayUtils.contains;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.CollectionUtils.copyOf;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshaller.*;

/**
 * Allows the definition of a security scheme that can be used by the operations.
 *
 * <p>
 * The Security Scheme Object defines a security scheme that can be used by the operations in Swagger 2.0.
 * Supported schemes are basic authentication, an API key (either as a header or as a query parameter) and OAuth2's
 * common flows (implicit, password, application and access code).
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The Security Scheme Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>type</c> (string, REQUIRED) - The type of the security scheme. Values: <js>"basic"</js>, <js>"apiKey"</js>, <js>"oauth2"</js>
 * 	<li><c>description</c> (string) - A short description for security scheme
 * 	<li><c>name</c> (string) - The name of the header or query parameter to be used (for <js>"apiKey"</js> type)
 * 	<li><c>in</c> (string) - The location of the API key (for <js>"apiKey"</js> type). Values: <js>"query"</js>, <js>"header"</js>
 * 	<li><c>flow</c> (string) - The flow used by the OAuth2 security scheme (for <js>"oauth2"</js> type). Values: <js>"implicit"</js>, <js>"password"</js>, <js>"application"</js>, <js>"accessCode"</js>
 * 	<li><c>authorizationUrl</c> (string) - The authorization URL to be used for this flow (for <js>"oauth2"</js> type)
 * 	<li><c>tokenUrl</c> (string) - The token URL to be used for this flow (for <js>"oauth2"</js> type)
 * 	<li><c>scopes</c> (map of string) - The available scopes for the OAuth2 security scheme (for <js>"oauth2"</js> type)
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjson'>
 * 	<jc>// Basic authentication sample</jc>
 * 	{
 * 		<js>"type"</js>: <js>"basic"</js>
 * 	}
 *
 * 	<jc>// API key sample</jc>
 * 	{
 * 		<js>"type"</js>: <js>"apiKey"</js>,
 * 		<js>"name"</js>: <js>"api_key"</js>,
 * 		<js>"in"</js>: <js>"header"</js>
 * 	}
 *
 * 	<jc>// Implicit OAuth2 sample</jc>
 * 	{
 * 		<js>"type"</js>: <js>"oauth2"</js>,
 * 		<js>"authorizationUrl"</js>: <js>"http://swagger.io/api/oauth/dialog"</js>,
 * 		<js>"flow"</js>: <js>"implicit"</js>,
 * 		<js>"scopes"</js>: {
 * 			<js>"write:pets"</js>: <js>"modify pets in your account"</js>,
 * 			<js>"read:pets"</js>: <js>"read your pets"</js>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#security-scheme-object">Swagger 2.0 Specification &gt; Security Scheme Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/authentication/">Swagger Authentication</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class SecurityScheme extends SwaggerElement {

	private static final String[] VALID_TYPES = { "basic", "apiKey", "oauth2" };

	private String type, description, name, in, flow, authorizationUrl, tokenUrl;
	private Map<String,String> scopes;

	/**
	 * Default constructor.
	 */
	public SecurityScheme() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public SecurityScheme(SecurityScheme copyFrom) {
		super(copyFrom);

		this.authorizationUrl = copyFrom.authorizationUrl;
		this.description = copyFrom.description;
		this.flow = copyFrom.flow;
		this.in = copyFrom.in;
		this.name = copyFrom.name;
		this.scopes = copyOf(copyFrom.scopes);
		this.tokenUrl = copyFrom.tokenUrl;
		this.type = copyFrom.type;
	}

	/**
	 * Bean property appender:  <property>scopes</property>.
	 *
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @param key The scope key.  Must not be <jk>null</jk>.
	 * @param value The scope value.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public SecurityScheme addScope(String key, String value) {
		assertArgNotNull("key", key);
		assertArgNotNull("value", value);
		scopes = mapBuilder(scopes).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public SecurityScheme copy() {
		return new SecurityScheme(this);
	}

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "authorizationUrl" -> toType(getAuthorizationUrl(), type);
			case "description" -> toType(getDescription(), type);
			case "flow" -> toType(getFlow(), type);
			case "in" -> toType(getIn(), type);
			case "name" -> toType(getName(), type);
			case "scopes" -> toType(getScopes(), type);
			case "tokenUrl" -> toType(getTokenUrl(), type);
			case "type" -> toType(getType(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>authorizationUrl</property>.
	 *
	 * <p>
	 * The authorization URL to be used for this flow.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getAuthorizationUrl() { return authorizationUrl; }

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description for security scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>flow</property>.
	 *
	 * <p>
	 * The flow used by the OAuth2 security scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getFlow() { return flow; }

	/**
	 * Bean property getter:  <property>in</property>.
	 *
	 * <p>
	 * The location of the API key.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getIn() { return in; }

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the header or query parameter to be used.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() { return name; }

	/**
	 * Bean property getter:  <property>scopes</property>.
	 *
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,String> getScopes() { return scopes; }

	/**
	 * Bean property getter:  <property>tokenUrl</property>.
	 *
	 * <p>
	 * The token URL to be used for this flow.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTokenUrl() { return tokenUrl; }

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the security scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getType() { return type; }

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setBuilder(String.class)
			.addIf(authorizationUrl != null, "authorizationUrl")
			.addIf(description != null, "description")
			.addIf(flow != null, "flow")
			.addIf(in != null, "in")
			.addIf(name != null, "name")
			.addIf(scopes != null, "scopes")
			.addIf(tokenUrl != null, "tokenUrl")
			.addIf(type != null, "type")
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from SwaggerElement */
	public SecurityScheme set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "authorizationUrl" -> setAuthorizationUrl(Utils.s(value));
			case "description" -> setDescription(Utils.s(value));
			case "flow" -> setFlow(Utils.s(value));
			case "in" -> setIn(Utils.s(value));
			case "name" -> setName(Utils.s(value));
			case "scopes" -> setScopes(mapBuilder(String.class, String.class).sparse().addAny(value).build());
			case "tokenUrl" -> setTokenUrl(Utils.s(value));
			case "type" -> setType(Utils.s(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>authorizationUrl</property>.
	 *
	 * <p>
	 * The authorization URL to be used for this flow.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>This SHOULD be in the form of a URL.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme setAuthorizationUrl(String value) {
		authorizationUrl = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * <p>
	 * A short description for security scheme.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>flow</property>.
	 *
	 * <p>
	 * The flow used by the OAuth2 security scheme.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"implicit"</js>
	 * 		<li><js>"password"</js>
	 * 		<li><js>"application"</js>
	 * 		<li><js>"accessCode"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme setFlow(String value) {
		flow = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>in</property>.
	 *
	 * <p>
	 * The location of the API key.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"query"</js>
	 * 		<li><js>"header"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme setIn(String value) {
		in = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the header or query parameter to be used.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>scopes</property>.
	 *
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme setScopes(Map<String,String> value) {
		scopes = copyOf(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>tokenUrl</property>.
	 *
	 * <p>
	 * The token URL to be used for this flow.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>This SHOULD be in the form of a URL.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme setTokenUrl(String value) {
		tokenUrl = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the security scheme.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"basic"</js>
	 * 		<li><js>"apiKey"</js>
	 * 		<li><js>"oauth2"</js>
	 * 	</ul>
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw new BasicRuntimeException("Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}", value, Json5.of(VALID_TYPES));
		type = value;
		return this;
	}

	@Override /* Overridden from SwaggerElement */
	public SecurityScheme strict() {
		super.strict();
		return this;
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> (interpreted as <jk>false</jk>).
	 * @return This object.
	 */
	@Override
	public SecurityScheme strict(Object value) {
		super.strict(value);
		return this;
	}
}