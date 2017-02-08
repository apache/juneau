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

import java.net.*;
import java.text.*;

import org.apache.juneau.*;
import org.apache.juneau.urlencoding.*;

/**
 * REST methods can return this object as a shortcut for performing <code>HTTP 302</code> redirects.
 * <p>
 * The following example shows the difference between handling redirects via the {@link RestRequest}/{@link RestResponse},
 * 	and the simplified approach of using this class.
 * <p class='bcode'>
 * 	<jc>// Redirect to "/contextPath/servletPath/foobar"</jc>
 *
 * 	<jc>// Using RestRequest and RestResponse</jc>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/example1"</js>)
 * 	<jk>public void</jk> example1(RestRequest req, RestResponse res) <jk>throws</jk> IOException {
 * 		res.sendRedirect(req.getServletURI() + <js>"/foobar"</js>);
 * 	}
 *
 * 	<jc>// Using Redirect</jc>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/example2"</js>)
 * 	<jk>public</jk> Redirect example2() {
 * 		<jk>return new</jk> Redirect(<js>"foobar"</js>);
 * 	}
 * </p>
 * <p>
 * The constructor can use a {@link MessageFormat}-style pattern with multiple arguments:
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/example3"</js>)
 * 	<jk>public</jk> Redirect example3() {
 * 		<jk>return new</jk> Redirect(<js>"foo/{0}/bar/{1}"</js>, id1, id2);
 * 	}
 * </p>
 * <p>
 * The arguments are serialized to strings using the servlet's {@link UrlEncodingSerializer},
 * 	so any filters defined on the serializer or REST method/class will be used when present.
 * The arguments will also be automatically URL-encoded.
 * <p>
 * Redirecting to the servlet root can be accomplished by simply using the no-arg constructor.
 * <p class='bcode'>
 * 	<jc>// Simply redirect to the servlet root.
 * 	// Equivalent to res.sendRedirect(req.getServletURI()).</jc>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/example4"</js>)
 * 	<jk>public</jk> Redirect exmaple4() {
 * 		<jk>return new</jk> Redirect();
 * 	}
 * </p>
 * <p>
 * This class is handled by {@link org.apache.juneau.rest.response.RedirectHandler}, a built-in default
 * 	response handler created by {@link RestServlet#createResponseHandlers(ObjectMap)}.
 */
public final class Redirect {

	private int httpResponseCode;
	private String url;
	private Object[] args;

	/**
	 * Redirect to the specified URL.
	 * Relative paths are interpreted as relative to the servlet path.
	 *
	 * @param url The URL to redirect to.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public Redirect(CharSequence url, Object...args) {
		this.url = (url == null ? null : url.toString());
		this.args = args;
	}

	/**
	 * Convenience method for redirecting to instance of {@link URL} and {@link URI}.
	 * Same as calling <code>toString()</code> on the object and using the other constructor.
	 *
	 * @param url The URL to redirect to.
	 */
	public Redirect(Object url) {
		this.url = (url == null ? null : url.toString());
	}

	/**
	 * Redirect to the specified URL.
	 * Relative paths are interpreted as relative to the servlet path.
	 *
	 * @param httpResponseCode The HTTP response code.
	 * @param url The URL to redirect to.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public Redirect(int httpResponseCode, CharSequence url, Object...args) {
		this.httpResponseCode = httpResponseCode;
		this.url = (url == null ? null : url.toString());
		this.args = args;
	}

	/**
	 * Shortcut for redirecting to the servlet root.
	 */
	public Redirect() {
	}

	/**
	 * Calculates the URL to redirect to.
	 *
	 * @param s Use this serializer to encode arguments using the {@link UrlEncodingSerializer#serializeUrlPart(Object)} method.
	 * @return The URL to redirect to.
	 */
	public String toUrl(UrlEncodingSerializer s) {
		if (url != null && args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++)
				args[i] = s.serializeUrlPart(args[i]);
			url = MessageFormat.format(url, args);
		}
		return url;
	}

	/**
	 * Returns the response code passed in through the constructor.
	 *
	 * @return The response code passed in through the constructor, or <code>0</code> if response code wasn't specified.
	 */
	public int getHttpResponseCode() {
		return httpResponseCode;
	}
}
