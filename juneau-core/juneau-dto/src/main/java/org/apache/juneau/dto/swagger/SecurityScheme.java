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

import static org.apache.juneau.internal.ArrayUtils.contains;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Allows the definition of a security scheme that can be used by the operations.
 *
 * <p>
 * Supported schemes are basic authentication, an API key (either as a header or as a query parameter) and OAuth2's
 * common flows (implicit, password, application and access code).
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Swagger}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(properties="type,description,name,in,flow,authorizationUrl,tokenUrl,scopes,*")
public class SecurityScheme extends SwaggerElement {

	private static final String[] VALID_TYPES = {"basic", "apiKey", "oauth2"};

	private String
		type,
		description,
		name,
		in,
		flow,
		authorizationUrl,
		tokenUrl;
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
		this.scopes = copyFrom.scopes == null ? null : new LinkedHashMap<>(copyFrom.scopes);
		this.tokenUrl = copyFrom.tokenUrl;
		this.type = copyFrom.type;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public SecurityScheme copy() {
		return new SecurityScheme(this);
	}


	@Override /* SwaggerElement */
	protected SecurityScheme strict() {
		super.strict();
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// authorizationUrl
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>authorizationUrl</property>.
	 *
	 * <p>
	 * The authorization URL to be used for this flow.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getAuthorizationUrl() {
		return authorizationUrl;
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
	 */
	public void setAuthorizationUrl(String value) {
		authorizationUrl = value;
	}

	/**
	 * Bean property fluent getter:  <property>authorizationUrl</property>.
	 *
	 * <p>
	 * The authorization URL to be used for this flow.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> authorizationUrl() {
		return optional(getAuthorizationUrl());
	}

	/**
	 * Bean property fluent setter:  <property>authorizationUrl</property>.
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
	public SecurityScheme authorizationUrl(String value) {
		setAuthorizationUrl(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// description
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description for security scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
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
	 */
	public void setDescription(String value) {
		description = value;
	}

	/**
	 * Bean property fluent getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description for security scheme.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> description() {
		return optional(getDescription());
	}

	/**
	 * Bean property fluent setter:  <property>description</property>.
	 *
	 * <p>
	 * A short description for security scheme.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme description(String value) {
		setDescription(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// flow
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>flow</property>.
	 *
	 * <p>
	 * The flow used by the OAuth2 security scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getFlow() {
		return flow;
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
	 */
	public void setFlow(String value) {
		flow = value;
	}

	/**
	 * Bean property fluent getter:  <property>flow</property>.
	 *
	 * <p>
	 * The flow used by the OAuth2 security scheme.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> flow() {
		return optional(getFlow());
	}

	/**
	 * Bean property fluent setter:  <property>flow</property>.
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
	public SecurityScheme flow(String value) {
		setFlow(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// in
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>in</property>.
	 *
	 * <p>
	 * The location of the API key.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getIn() {
		return in;
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
	 */
	public void setIn(String value) {
		in = value;
	}

	/**
	 * Bean property fluent getter:  <property>in</property>.
	 *
	 * <p>
	 * The location of the API key.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> in() {
		return optional(getIn());
	}

	/**
	 * Bean property fluent setter:  <property>in</property>.
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
	public SecurityScheme in(String value) {
		setIn(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// name
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the header or query parameter to be used.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
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
	 */
	public void setName(String value) {
		name = value;
	}

	/**
	 * Bean property fluent getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the header or query parameter to be used.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> name() {
		return optional(getName());
	}

	/**
	 * Bean property fluent setter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the header or query parameter to be used.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme name(String value) {
		setName(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// scopes
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>scopes</property>.
	 *
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,String> getScopes() {
		return scopes;
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
	 */
	public void setScopes(Map<String,String> value) {
		scopes = copyOf(value);
	}

	/**
	 * Bean property appender:  <property>scopes</property>.
	 *
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public SecurityScheme addScopes(Map<String,String> values) {
		scopes = mapBuilder(scopes).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>scopes</property>.
	 *
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,String>> scopes() {
		return optional(getScopes());
	}

	/**
	 * Bean property fluent setter:  <property>scopes</property>.
	 *
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @param value
	 * 	The values to set on this property.
	 * @return This object.
	 */
	public SecurityScheme scopes(Map<String,String> value) {
		setScopes(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>scopes</property>.
	 *
	 * <p>
	 * The available scopes for the OAuth2 security scheme.
	 *
	 * @param json
	 * 	The new value for this property as JSON.
	 * @return This object.
	 */
	public SecurityScheme scopes(String json) {
		setScopes(mapBuilder(String.class,String.class).sparse().addJson(json).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// tokenUrl
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>tokenUrl</property>.
	 *
	 * <p>
	 * The token URL to be used for this flow.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTokenUrl() {
		return tokenUrl;
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
	 */
	public void setTokenUrl(String value) {
		tokenUrl = value;
	}

	/**
	 * Bean property fluent getter:  <property>tokenUrl</property>.
	 *
	 * <p>
	 * The token URL to be used for this flow.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> tokenUrl() {
		return optional(getTokenUrl());
	}

	/**
	 * Bean property fluent setter:  <property>tokenUrl</property>.
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
	public SecurityScheme tokenUrl(String value) {
		setTokenUrl(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the security scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getType() {
		return type;
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
	 */
	public void setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw runtimeException(
				"Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}",
				value, json(VALID_TYPES)
			);
		type = value;
	}

	/**
	 * Bean property fluent getter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the security scheme.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> type() {
		return optional(getType());
	}

	/**
	 * Bean property fluent setter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the security scheme.
	 *
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"basic"</js>
	 * 		<li><js>"apiKey"</js>
	 * 		<li><js>"oauth2"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SecurityScheme type(String value) {
		setType(value);
		return this;
	}


	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "authorizationUrl": return toType(getAuthorizationUrl(), type);
			case "description": return toType(getDescription(), type);
			case "flow": return toType(getFlow(), type);
			case "in": return toType(getIn(), type);
			case "name": return toType(getName(), type);
			case "scopes": return toType(getScopes(), type);
			case "tokenUrl": return toType(getTokenUrl(), type);
			case "type": return toType(getType(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public SecurityScheme set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "authorizationUrl": return authorizationUrl(stringify(value));
			case "description": return description(stringify(value));
			case "flow": return flow(stringify(value));
			case "in": return in(stringify(value));
			case "name": return name(stringify(value));
			case "scopes": return scopes(mapBuilder(String.class,String.class).sparse().addAny(value).build());
			case "tokenUrl": return tokenUrl(stringify(value));
			case "type": return type(stringify(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(authorizationUrl != null, "authorizationUrl")
			.addIf(description != null, "description")
			.addIf(flow != null, "flow")
			.addIf(in != null, "in")
			.addIf(name != null, "name")
			.addIf(scopes != null, "scopes")
			.addIf(tokenUrl != null, "tokenUrl")
			.addIf(type != null, "type")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
