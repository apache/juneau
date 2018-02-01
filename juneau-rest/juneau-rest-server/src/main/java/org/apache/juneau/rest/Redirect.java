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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.StringUtils.*;

import java.net.*;
import java.text.*;

/**
 * REST methods can return this object as a shortcut for performing <code>HTTP 302</code> redirects.
 * 
 * 
 * <h5 class='section'>Documentation:</h5>
 * <ul>
 * 	<li><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.Redirect">Overview &gt; Redirect</a>
 * </ul>
 */
public final class Redirect {

	private final int httpResponseCode;
	private final URI uri;

	/**
	 * Redirect to the specified URL.
	 * 
	 * <p>
	 * Relative paths are interpreted as relative to the servlet path.
	 * 
	 * @param uri
	 * 	The URL to redirect to.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><code>URL</code>
	 * 		<li><code>URI</code>
	 * 		<li><code>CharSequence</code>
	 * 	</ul>
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public Redirect(Object uri, Object...args) {
		this(0, uri, args);
	}

	/**
	 * Convenience method for redirecting to instance of {@link URL} and {@link URI}.
	 * 
	 * <p>
	 * Same as calling <code>toString()</code> on the object and using the other constructor.
	 * 
	 * @param uri
	 * 	The URL to redirect to.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><code>URL</code>
	 * 		<li><code>URI</code>
	 * 		<li><code>CharSequence</code>
	 * 	</ul>
	 */
	public Redirect(Object uri) {
		this(0, uri, (Object[])null);
	}

	/**
	 * Redirect to the specified URL.
	 * 
	 * <p>
	 * Relative paths are interpreted as relative to the servlet path.
	 * 
	 * @param httpResponseCode The HTTP response code.
	 * @param url
	 * 	The URL to redirect to.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><code>URL</code>
	 * 		<li><code>URI</code>
	 * 		<li><code>CharSequence</code>
	 * 	</ul>
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public Redirect(int httpResponseCode, Object url, Object...args) {
		this.httpResponseCode = httpResponseCode;
		if (url == null)
			url = "";
		this.uri = toURI(format(url.toString(), args));
	}

	/**
	 * Shortcut for redirecting to the servlet root.
	 */
	public Redirect() {
		this(0, null, (Object[])null);
	}

	/**
	 * Returns the response code passed in through the constructor.
	 * 
	 * @return The response code passed in through the constructor, or <code>0</code> if response code wasn't specified.
	 */
	public int getHttpResponseCode() {
		return httpResponseCode;
	}

	/**
	 * Returns the URI to redirect to.
	 * 
	 * @return The URI to redirect to.
	 */
	public URI getURI() {
		return uri;
	}
}
