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
package org.apache.juneau.http.header;

import java.util.function.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Host</l> HTTP request header.
 *
 * <p>
 * The domain name of the server (for virtual hosting), and the TCP port number on which the server is listening.
 * The port number may be omitted if the port is the standard port for the service requested.
 * Mandatory since HTTP/1.1.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Host: en.wikipedia.org:8080
 * 	Host: en.wikipedia.org
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Host request-header field specifies the Internet host and port number of the resource being requested, as
 * obtained from the original URI given by the user or referring resource (generally an HTTP URL, as described in
 * section 3.2.2).
 * The Host field value MUST represent the naming authority of the origin server or gateway given by the original URL.
 * This allows the origin server or gateway to differentiate between internally-ambiguous URLs, such as the root "/" URL
 * of a server for multiple host names on a single IP address.
 *
 * <p class='bcode'>
 * 	Host = "Host" ":" host [ ":" port ] ; Section 3.2.2
 * </p>
 *
 * <p>
 * A "host" without any trailing port information implies the default port for the service requested (e.g., "80" for an
 * HTTP URL).
 * For example, a request on the origin server for &lt;http://www.w3.org/pub/WWW/&gt; would properly include:
 * <p class='bcode'>
 * 	GET /pub/WWW/ HTTP/1.1
 * 	Host: www.w3.org
 * </p>
 *
 * <p>
 * A client MUST include a Host header field in all HTTP/1.1 request messages.
 * If the requested URI does not include an Internet host name for the service being requested, then the Host header
 * field MUST be given with an empty value.
 * An HTTP/1.1 proxy MUST ensure that any request message it forwards does contain an appropriate Host header field that
 * identifies the service being requested by the proxy.
 * All Internet-based HTTP/1.1 servers MUST respond with a 400 (Bad Request) status code to any HTTP/1.1 request
 * message which lacks a Host header field.
 *
 * <p>
 * See sections 5.2 and 19.6.1.1 for other requirements relating to Host.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Host")
public class Host extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Host";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Host of(String value) {
		return value == null ? null : new Host(value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Host of(Supplier<String> value) {
		return value == null ? null : new Host(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Host(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Host(Supplier<String> value) {
		super(NAME, value);
	}
}
