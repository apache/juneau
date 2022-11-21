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

import static org.apache.juneau.internal.ConverterUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.Bean;
import org.apache.juneau.internal.*;

import java.util.*;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ArrayUtils.contains;
import static org.apache.juneau.internal.CollectionUtils.*;

/**
 * Describes a single operation parameter.
 *
 * <p>
 * A unique parameter is defined by a combination of a name and location.
 *
 * <p>
 * There are five possible parameter types.
 * <ul class='spaced-list'>
 * 	<li><js>"path"</js> - Used together with Path Templating, where the parameter value is actually part of the
 * 		operation's URL.
 * 		This does not include the host or base path of the API.
 * 		For example, in <code>/items/{itemId}</code>, the path parameter is <code>itemId</code>.
 * 	<li><js>"query"</js> - Parameters that are appended to the URL.
 * 		For example, in <code>/items?id=###</code>, the query parameter is <code>id</code>.
 * 	<li><js>"header"</js> - Custom headers that are expected as part of the request.
 * 	<li><js>"body"</js> - The payload that's appended to the HTTP request.
 * 		Since there can only be one payload, there can only be one body parameter.
 * 		The name of the body parameter has no effect on the parameter itself and is used for documentation purposes
 * 		only.
 * 		Since Form parameters are also in the payload, body and form parameters cannot exist together for the same
 * 		operation.
 * 	<li><js>"formData"</js> - Used to describe the payload of an HTTP request when either
 * 		<code>application/x-www-form-urlencoded</code>, <code>multipart/form-data</code> or both are used as the
 * 		content type of the request (in Swagger's definition, the consumes property of an operation).
 * 		This is the only parameter type that can be used to send files, thus supporting the file type.
 * 		Since form parameters are sent in the payload, they cannot be declared together with a body parameter for the
 * 		same operation.
 * 		Form parameters have a different format based on the content-type used (for further details, consult
 * 		<code>http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4</code>):
 * 		<ul>
 * 			<li><js>"application/x-www-form-urlencoded"</js> - Similar to the format of Query parameters but as a
 * 				payload.
 * 				For example, <code>foo=1&amp;bar=swagger</code> - both <code>foo</code> and <code>bar</code> are form
 * 				parameters.
 * 				This is normally used for simple parameters that are being transferred.
 * 			<li><js>"multipart/form-data"</js> - each parameter takes a section in the payload with an internal header.
 * 				For example, for the header <code>Content-Disposition: form-data; name="submit-name"</code> the name of
 * 				the parameter is <code>submit-name</code>.
 * 				This type of form parameters is more commonly used for file transfers.
 * 		</ul>
 * 	</li>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	ParameterInfo x = <jsm>parameterInfo</jsm>(<js>"query"</js>, <js>"foo"</js>);
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
 * 		<js>"in"</js>: <js>"query"</js>,
 * 		<js>"name"</js>: <js>"foo"</js>
 * 	}
 * </p>
 */
@Bean(properties="in,name,type,description,scheme,bearerFormat,flows,*")
@FluentSetters
public class SecuritySchemeInfo extends OpenApiElement {

	private static final String[] VALID_IN = {"query", "header", "cookie"};
	private static final String[] VALID_TYPES = {"apiKey", "http", "oauth2", "openIdConnect"};

	private String
		type,
		description,
		name,
		in,
		scheme,
		bearerFormat,
		openIdConnectUrl;

	private OAuthFlow flows;

	/**
	 * Default constructor.
	 */
	public SecuritySchemeInfo() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public SecuritySchemeInfo(SecuritySchemeInfo copyFrom) {
		super(copyFrom);

		this.name = copyFrom.name;
		this.in = copyFrom.in;
		this.description = copyFrom.description;
		this.type = copyFrom.type;
		this.scheme = copyFrom.scheme;
		this.bearerFormat = copyFrom.bearerFormat;
		this.openIdConnectUrl = copyFrom.openIdConnectUrl;
		this.flows = copyFrom.flows;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public SecuritySchemeInfo copy() {
		return new SecuritySchemeInfo(this);
	}

	@Override /* SwaggerElement */
	protected SecuritySchemeInfo strict() {
		super.strict();
		return this;
	}


	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the parameter.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Parameter names are case sensitive.
	 * 	<li>
	 * 		If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment
	 * 		from the <code>path</code> field in the paths object.
	 * 	<li>
	 * 		For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 * </ul>
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
	 * The name of the parameter.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Parameter names are case sensitive.
	 * 	<li>
	 * 		If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment
	 * 		from the <code>path</code> field in the paths object.
	 * 	<li>
	 * 		For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object
	 */
	public SecuritySchemeInfo setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>in</property>.
	 *
	 * <p>
	 * The location of the parameter.
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
	 * The location of the parameter.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"query"</js>
	 * 		<li><js>"header"</js>
	 * 		<li><js>"path"</js>
	 * 		<li><js>"formData"</js>
	 * 		<li><js>"body"</js>
	 * 	</ul>
	 * 	<br>Property value is required.
	 * @return This object
	 */
	public SecuritySchemeInfo setIn(String value) {
		if (isStrict() && ! contains(value, VALID_IN))
			throw new BasicRuntimeException(
				"Invalid value passed in to setIn(String).  Value=''{0}'', valid values={1}",
				value, VALID_IN
			);
		in = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A brief description of the parameter.
	 * <br>This could contain examples of use.
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
	 * A brief description of the parameter.
	 * <br>This could contain examples of use.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 *
	 * <p>
	 * The schema defining the type used for the body parameter.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 *
	 * <p>
	 * The schema defining the type used for the body parameter.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object
	 */
	public SecuritySchemeInfo setScheme(String value) {
		scheme = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the parameter.
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
	 * The type of the parameter.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 		<li><js>"file"</js>
	 * 	</ul>
	 * 	<br>If type is <js>"file"</js>, the <code>consumes</code> MUST be either <js>"multipart/form-data"</js>, <js>"application/x-www-form-urlencoded"</js>
	 * 		or both and the parameter MUST be <code>in</code> <js>"formData"</js>.
	 * 	<br>Property value is required.
	 * @return This object
	 */
	public SecuritySchemeInfo setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw new BasicRuntimeException(
				"Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}",
				value, VALID_TYPES
			);
		type = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned type.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getBearerFormat() {
		return bearerFormat;
	}

	/**
	 * Bean property setter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned type.
	 *
	 * @param value The new value for this property.
	 * @return This object
	 */
	public SecuritySchemeInfo setBearerFormat(String value) {
		bearerFormat = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public OAuthFlow getFlows() {
		return flows;
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required if <code>type</code> is <js>"array"</js>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SecuritySchemeInfo setFlows(OAuthFlow value) {
		flows = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>collectionFormat</property>.
	 *
	 * <p>
	 * Determines the format of the array if type array is used.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getOpenIdConnectUrl() {
		return openIdConnectUrl;
	}

	/**
	 * Bean property setter:  <property>collectionFormat</property>.
	 *
	 * <p>
	 * Determines the format of the array if type array is used.
	 *
	 * @param value The new value for this property.
	 * @return This object
	 */
	public SecuritySchemeInfo setOpenIdConnectUrl(String value) {
		openIdConnectUrl = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "name": return toType(getName(), type);
			case "in": return toType(getIn(), type);
			case "description": return toType(getDescription(), type);
			case "scheme": return toType(getScheme(), type);
			case "flows": return toType(getFlows(), type);
			case "bearerFormat": return toType(getBearerFormat(), type);
			case "openIdConnectUrl": return toType(getOpenIdConnectUrl(), type);
			case "type": return toType(getType(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public SecuritySchemeInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "name": return setName(stringify(value));
			case "in": return setIn(stringify(value));
			case "description": return setDescription(stringify(value));
			case "scheme": return setScheme(stringify(value));
			case "bearerFormat": return setBearerFormat(stringify(value));
			case "type": return setType(stringify(value));
			case "flows": return setFlows(toType(value, OAuthFlow.class));
			case "openIdConnectUrl": return setOpenIdConnectUrl(stringify(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(name != null, "name")
			.addIf(in != null, "in")
			.addIf(description != null, "description")
			.addIf(scheme != null, "scheme")
			.addIf(bearerFormat != null, "bearerFormat")
			.addIf(type != null, "type")
			.addIf(flows != null, "flows")
			.addIf(openIdConnectUrl != null, "openIdConnectUrl")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

}
