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
package org.apache.juneau.rest.client;

import java.net.*;

import org.apache.http.client.utils.*;

/**
 * Aggregates the HTTP method, URL, and optional body into a single bean.
 */
public class RestOperation {

	private final Object url;
	private final String method;
	private final Object body;

	/**
	 * Creator.
	 *
	 * @param method The HTTP method.
	 * @param url
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return A new {@link RestOperation} object.
	 */
	public static RestOperation of(String method, Object url) {
		return new RestOperation(method, url, null);
	}

	/**
	 * Creator.
	 *
	 * @param method The HTTP method.
	 * @param url
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body The HTTP body.
	 * @return A new {@link RestOperation} object.
	 */
	public static RestOperation of(String method, Object url, Object body) {
		return new RestOperation(method, url, body);
	}

	/**
	 * Constructor.
	 *
	 * @param method The HTTP method.
	 * @param url
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body The HTTP body.
	 */
	public RestOperation(String method, Object url, Object body) {
		this.url = url;
		this.method = method;
		this.body = body;
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 *
	 * @return The value of the <property>url</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Object getUrl() {
		return url;
	}

	/**
	 * Bean property getter:  <property>method</property>.
	 *
	 * @return The value of the <property>method</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Bean property getter:  <property>body</property>.
	 *
	 * @return The value of the <property>body</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Object getBody() {
		return body;
	}

	/**
	 * Identifies whether this HTTP method typically has a body.
	 *
	 * @return <jk>true</jk> if this HTTP method typically has a body.
	 */
	public boolean hasBody() {
		return ! (method.equalsIgnoreCase("get") || method.equalsIgnoreCase("delete"));
	}
}
