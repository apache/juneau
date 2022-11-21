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
package org.apache.juneau.dto.openapi3;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import org.apache.juneau.annotation.Bean;
import org.apache.juneau.internal.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * information for Link object.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Contact x = <jsm>contact</jsm>(<js>"API Support"</js>, <js>"http://www.swagger.io/support"</js>, <js>"support@swagger.io"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"name"</js>: <js>"API Support"</js>,
 * 		<js>"url"</js>: <js>"http://www.swagger.io/support"</js>,
 * 		<js>"email"</js>: <js>"support@swagger.io"</js>
 * 	}
 * </p>
 */
@Bean(properties="authorizationUrl,tokenUrl,refreshUrl,scopes,*")
@FluentSetters
public class OAuthFlow extends OpenApiElement {

	private String authorizationUrl;
	private String tokenUrl;
	private String refreshUrl;
	private Map<String,String> scopes;


	/**
	 * Default constructor.
	 */
	public OAuthFlow() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public OAuthFlow(OAuthFlow copyFrom) {
		super(copyFrom);

		this.authorizationUrl = copyFrom.authorizationUrl;
		this.tokenUrl = copyFrom.tokenUrl;
		this.refreshUrl = copyFrom.refreshUrl;

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
	public OAuthFlow copy() {
		return new OAuthFlow(this);
	}

	/**
	 * Bean property getter:  <property>operationRef</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getAuthorizationUrl() {
		return authorizationUrl;
	}

	/**
	 * Bean property setter:  <property>operationRef</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public OAuthFlow setAuthorizationUrl(String value) {
		authorizationUrl = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTokenUrl() {
		return tokenUrl;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public OAuthFlow setTokenUrl(String value) {
		tokenUrl = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>externalValue</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getRefreshUrl() {
		return refreshUrl;
	}

	/**
	 * Bean property setter:  <property>externalValue</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>MUST be in the format of an email address.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public OAuthFlow setRefreshUrl(String value) {
		refreshUrl = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * An example of the response message.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,String> getScopes() {
		return scopes;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * <p>
	 * An example of the response message.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Keys must be MIME-type strings.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public OAuthFlow setScopes(Map<String,String> value) {
		scopes = copyOf(value);
		return this;
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param name The mime-type string.
	 * @param description The example.
	 * @return This object
	 */
	public OAuthFlow addScope(String name, String description) {
		scopes = mapBuilder(scopes).sparse().add(name, description).build();
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "refreshUrl": return toType(getRefreshUrl(), type);
			case "tokenUrl": return toType(getTokenUrl(), type);
			case "authorizationUrl": return toType(getAuthorizationUrl(), type);
			case "scopes": return toType(getScopes(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public OAuthFlow set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "authorizationUrl": return setAuthorizationUrl(stringify(value));
			case "tokenUrl": return setTokenUrl(stringify(value));
			case "refreshUrl": return setRefreshUrl(stringify(value));
			case "scopes": return setScopes(mapBuilder(String.class,String.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(authorizationUrl != null, "authorizationUrl")
			.addIf(tokenUrl != null, "tokenUrl")
			.addIf(refreshUrl != null, "refreshUrl")
			.addIf(scopes != null, "scopes")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
