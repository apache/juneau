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

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Allows the definition of a security scheme that can be used by the operations.
 *
 * <p>
 * Supported schemes are basic authentication, an API key (either as a header or as a query parameter) and OAuth2's
 * common flows (implicit, password, application and access code).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
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
 * 	<li class='link'>{@doc DtoSwagger}
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

		this.type = copyFrom.type;
		this.description = copyFrom.description;
		this.name = copyFrom.name;
		this.in = copyFrom.in;
		this.flow = copyFrom.flow;
		this.authorizationUrl = copyFrom.authorizationUrl;
		this.tokenUrl = copyFrom.tokenUrl;

		if (copyFrom.scopes == null)
			this.scopes = null;
		else
			this.scopes = new LinkedHashMap<>(copyFrom.scopes);
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
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw new BasicRuntimeException(
				"Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}",
				value, VALID_TYPES
			);
		type = value;
		return this;
	}

	/**
	 * Same as {@link #setType(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"basic"</js>
	 * 		<li><js>"apiKey"</js>
	 * 		<li><js>"oauth2"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme type(Object value) {
		return setType(stringify(value));
	}

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
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Same as {@link #setDescription(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme description(Object value) {
		return setDescription(stringify(value));
	}

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
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Same as {@link #setName(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme name(Object value) {
		return setName(stringify(value));
	}

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
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setIn(String value) {
		in = value;
		return this;
	}

	/**
	 * Same as {@link #setIn(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"query"</js>
	 * 		<li><js>"header"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme in(Object value) {
		return setIn(stringify(value));
	}

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
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setFlow(String value) {
		flow = value;
		return this;
	}

	/**
	 * Same as {@link #setFlow(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"implicit"</js>
	 * 		<li><js>"password"</js>
	 * 		<li><js>"application"</js>
	 * 		<li><js>"accessCode"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme flow(Object value) {
		return setFlow(stringify(value));
	}

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
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setAuthorizationUrl(String value) {
		authorizationUrl = value;
		return this;
	}

	/**
	 * Same as {@link #setAuthorizationUrl(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>This SHOULD be in the form of a URL.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme authorizationUrl(Object value) {
		return setAuthorizationUrl(stringify(value));
	}

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
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setTokenUrl(String value) {
		tokenUrl = value;
		return this;
	}

	/**
	 * Same as {@link #setTokenUrl(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>This SHOULD be in the form of a URL.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme tokenUrl(Object value) {
		return setTokenUrl(stringify(value));
	}

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
	 * @return This object (for method chaining).
	 */
	public SecurityScheme setScopes(Map<String,String> value) {
		scopes = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>scopes</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme addScopes(Map<String,String> values) {
		scopes = addToMap(scopes, values);
		return this;
	}

	/**
	 * Adds one or more values to the <property>enum</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Map&lt;String,{@link HeaderInfo}|String&gt;</c>
	 * 		<li><c>String</c> - JSON object representation of <c>Map&lt;String,{@link HeaderInfo}&gt;</c>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Example </jc>
	 * 	scopes(<js>"{name:'value'}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SecurityScheme scopes(Object...values) {
		scopes = addToMap(scopes, values, String.class, String.class);
		return this;
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "type": return toType(getType(), type);
			case "description": return toType(getDescription(), type);
			case "name": return toType(getName(), type);
			case "in": return toType(getIn(), type);
			case "flow": return toType(getFlow(), type);
			case "authorizationUrl": return toType(getAuthorizationUrl(), type);
			case "tokenUrl": return toType(getTokenUrl(), type);
			case "scopes": return toType(getScopes(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public SecurityScheme set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "type": return type(value);
			case "description": return description(value);
			case "name": return name(value);
			case "in": return in(value);
			case "flow": return flow(value);
			case "authorizationUrl": return authorizationUrl(value);
			case "tokenUrl": return tokenUrl(value);
			case "scopes": return setScopes(null).scopes(value);
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = ASet.<String>of()
			.appendIf(type != null, "type")
			.appendIf(description != null, "description")
			.appendIf(name != null, "name")
			.appendIf(in != null, "in")
			.appendIf(flow != null, "flow")
			.appendIf(authorizationUrl != null, "authorizationUrl")
			.appendIf(tokenUrl != null, "tokenUrl")
			.appendIf(scopes != null, "scopes");
		return new MultiSet<>(s, super.keySet());
	}
}
