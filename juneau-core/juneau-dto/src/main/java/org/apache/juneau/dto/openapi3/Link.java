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
@Bean(properties="operationRef,operationId,description,requestBody,server,parameters,*")
@FluentSetters
public class Link extends OpenApiElement {

	private String operationRef;
	private String operationId;
	private String description;
	private Object requestBody;
	private Server server;
	private Map<String,Object> parameters;


	/**
	 * Default constructor.
	 */
	public Link() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Link(Link copyFrom) {
		super(copyFrom);

		this.operationRef = copyFrom.operationRef;
		this.description = copyFrom.description;
		this.operationId = copyFrom.operationId;
		this.requestBody = copyFrom.requestBody;
		this.server = copyFrom.server == null ? null : copyFrom.server.copy();

		if (copyFrom.parameters == null)
			this.parameters = null;
		else
			this.parameters = new LinkedHashMap<>(copyFrom.parameters);
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Link copy() {
		return new Link(this);
	}

	/**
	 * Bean property getter:  <property>operationRef</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getOperationRef() {
		return operationRef;
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
	public Link setOperationRef(String value) {
		operationRef = value;
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
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setDescription(String value) {
		description = value;
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
	public String getOperationId() {
		return operationId;
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
	public Link setOperationId(String value) {
		operationId = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js>
	 * to control the number of results per page might default to 100 if not supplied by the client in the request.
	 *
	 * (Note: <js>"value"</js> has no meaning for required parameters.)
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getRequestBody() {
		return requestBody;
	}

	/**
	 * Bean property setter:  <property>value</property>.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js>
	 * to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * (Note: <js>"default"</js> has no meaning for required parameters.)
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 *
	 * @param val The new value for this property.
	 * @return This object
	 */
	public Link setRequestBody(Object val) {
		requestBody = val;
		return this;
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Server getServer() {
		return server;
	}

	/**
	 * Bean property setter:  <property>additionalProperties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setServer(Server value) {
		server = value;
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
	public Map<String,Object> getParameters() {
		return parameters;
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
	public Link setParameters(Map<String,Object> value) {
		parameters = copyOf(value);
		return this;
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param mimeType The mime-type string.
	 * @param parameter The example.
	 * @return This object
	 */
	public Link addParameter(String mimeType, Object parameter) {
		parameters = mapBuilder(parameters).sparse().add(mimeType, parameters).build();
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "description": return toType(getDescription(), type);
			case "operationRef": return toType(getOperationRef(), type);
			case "operationId": return toType(getOperationId(), type);
			case "requestBody": return toType(getRequestBody(), type);
			case "parameters": return toType(getParameters(), type);
			case "server": return toType(getServer(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public Link set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "description": return setDescription(stringify(value));
			case "operationId": return setOperationId(stringify(value));
			case "operationRef": return setOperationRef(stringify(value));
			case "requestBody": return setRequestBody(value);
			case "server": return setServer(toType(value, Server.class));
			case "parameters": return setParameters(mapBuilder(String.class,Object.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(description != null, "description")
			.addIf(operationId != null, "operationId")
			.addIf(operationRef != null, "operationRef")
			.addIf(requestBody != null, "requestBody")
			.addIf(parameters != null, "parameters")
			.addIf(server != null, "server")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
