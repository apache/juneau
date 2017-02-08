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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;

/**
 * Allows the definition of a security scheme that can be used by the operations.
 * <p>
 * Supported schemes are basic authentication, an API key (either as a header or as a query parameter) and OAuth2's common flows (implicit, password, application and access code).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
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
 */
@Bean(properties="type,description,name,in,flow,authorizationUrl,tokenUrl,scopes")
public class SecurityScheme {

	private static final String[] VALID_TYPES = {"basic", "apiKey", "oauth2"};

	private String type;
	private String description;
	private String name;
	private String in;
	private String flow;
	private String authorizationUrl;
	private String tokenUrl;
	private Map<String,String> scopes;
	private boolean strict;

	/**
	 * Convenience method for creating a new SecurityScheme object.
	 *
	 * @param type Required. The type of the security scheme.
	 * 	Valid values are <js>"basic"</js>, <js>"apiKey"</js> or <js>"oauth2"</js>.
	 * @return A new SecurityScheme object.
	 */
	public static SecurityScheme create(String type) {
		return new SecurityScheme().setType(type);
	}

	/**
	 * Same as {@link #create(String)} except methods will throw runtime exceptions if you attempt
	 * to pass in invalid values per the Swagger spec.
	 *
	 * @param type Required. The type of the security scheme.
	 * 	Valid values are <js>"basic"</js>, <js>"apiKey"</js> or <js>"oauth2"</js>.
	 * @return A new SecurityScheme object.
	 */
	public static SecurityScheme createStrict(String type) {
		return new SecurityScheme().setStrict().setType(type);
	}

	private SecurityScheme setStrict() {
		this.strict = true;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 * <p>
	 * Required. The type of the security scheme.
	 * Valid values are <js>"basic"</js>, <js>"apiKey"</js> or <js>"oauth2"</js>.
	 *
	 * @return The value of the <property>type</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 * <p>
	 * Required. The type of the security scheme.
	 * Valid values are <js>"basic"</js>, <js>"apiKey"</js> or <js>"oauth2"</js>.
	 *
	 * @param type The new value for the <property>type</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setType(String type) {
		if (strict && ! ArrayUtils.contains(type, VALID_TYPES))
			throw new RuntimeException("Invalid value passed in to setType(String).  Value='"+type+"', valid values=" + JsonSerializer.DEFAULT_LAX.toString(VALID_TYPES));
		this.type = type;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * A short description for security scheme.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * <p>
	 * A short description for security scheme.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 * <p>
	 * The name of the header or query parameter to be used.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 * <p>
	 * The name of the header or query parameter to be used.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Bean property getter:  <property>in</property>.
	 * <p>
	 * The location of the API key. Valid values are <js>"query"</js> or <js>"header"</js>.
	 *
	 * @return The value of the <property>in</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getIn() {
		return in;
	}

	/**
	 * Bean property setter:  <property>in</property>.
	 * <p>
	 * The location of the API key. Valid values are <js>"query"</js> or <js>"header"</js>.
	 *
	 * @param in The new value for the <property>in</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setIn(String in) {
		this.in = in;
		return this;
	}

	/**
	 * Bean property getter:  <property>flow</property>.
	 * <p>
	 * The flow used by the OAuth2 security scheme.
	 * Valid values are <js>"implicit"</js>, <js>"password"</js>, <js>"application"</js> or <js>"accessCode"</js>.
	 *
	 * @return The value of the <property>flow</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getFlow() {
		return flow;
	}

	/**
	 * Bean property setter:  <property>flow</property>.
	 * <p>
	 * The flow used by the OAuth2 security scheme.
	 * Valid values are <js>"implicit"</js>, <js>"password"</js>, <js>"application"</js> or <js>"accessCode"</js>.
	 *
	 * @param flow The new value for the <property>flow</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setFlow(String flow) {
		this.flow = flow;
		return this;
	}

	/**
	 * Bean property getter:  <property>authorizationUrl</property>.
	 * <p>
	 * The authorization URL to be used for this flow.
	 * This SHOULD be in the form of a URL.
	 *
	 * @return The value of the <property>authorizationUrl</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getAuthorizationUrl() {
		return authorizationUrl;
	}

	/**
	 * Bean property setter:  <property>authorizationUrl</property>.
	 * <p>
	 * The authorization URL to be used for this flow.
	 * This SHOULD be in the form of a URL.
	 *
	 * @param authorizationUrl The new value for the <property>authorizationUrl</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setAuthorizationUrl(String authorizationUrl) {
		this.authorizationUrl = authorizationUrl;
		return this;
	}

	/**
	 * Bean property getter:  <property>tokenUrl</property>.
	 * <p>
	 * The token URL to be used for this flow.
	 * This SHOULD be in the form of a URL.
	 *
	 * @return The value of the <property>tokenUrl</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getTokenUrl() {
		return tokenUrl;
	}

	/**
	 * Bean property setter:  <property>tokenUrl</property>.
	 * <p>
	 * The token URL to be used for this flow.
	 * This SHOULD be in the form of a URL.
	 *
	 * @param tokenUrl The new value for the <property>tokenUrl</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
		return this;
	}

	/**
	 * Bean property getter:  <property>scopes</property>.
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @return The value of the <property>scopes</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,String> getScopes() {
		return scopes;
	}

	/**
	 * Bean property setter:  <property>scopes</property>.
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @param scopes The new value for the <property>scopes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setScopes(Map<String,String> scopes) {
		this.scopes = scopes;
		return this;
	}

	/**
	 * Bean property adder:  <property>scopes</property>.
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @param name The name of the scope.
	 * @param description A short description of the scope.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public SecurityScheme addScope(String name, String description) {
		if (scopes == null)
			scopes = new TreeMap<String,String>();
		scopes.put(name, description);
		return this;
	}
}
