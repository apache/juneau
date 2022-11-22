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
import java.util.*;

import org.apache.http.client.utils.*;
import org.apache.juneau.http.*;

/**
 * Aggregates the HTTP method, URL, and optional body into a single bean.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public class RestOperation {

	/**
	 * A placeholder for a non-existent body.
	 * Used to identify when form-data should be used in a request body.
	 * Note that this is different than a <jk>null</jk> body since a <jk>null</jk> can be a serialized request.
	 */
	public static final Object NO_BODY = "NO_BODY";

	private final Object url;
	private final String method;
	private final Object content;
	private boolean hasContent;

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
		return new RestOperation(method, url, NO_BODY);
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
		this.method = method.toUpperCase(Locale.ENGLISH);
		this.content = body;
		this.hasContent = HttpMethod.hasContent(method);
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 *
	 * @return The value of the <property>url</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Object getUri() {
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
	 * Bean property getter:  <property>content</property>.
	 *
	 * @return
	 * 	The value of the <property>content</property> property on this bean.
	 * 	<br>Returns {@link #NO_BODY} if the request does not have a body set.
	 * 	<br>A <jk>null</jk> value means <jk>null</jk> should be the serialized response.
	 */
	public Object getContent() {
		return content;
	}

	/**
	 * Identifies whether this HTTP method typically has content.
	 *
	 * @return <jk>true</jk> if this HTTP method typically has content.
	 */
	public boolean hasContent() {
		return hasContent;
	}

	/**
	 * Overrides the default value for the {@link #hasContent()} method.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RestOperation hasContent(boolean value) {
		this.hasContent = value;
		return this;
	}
}
