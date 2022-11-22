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
 * Represents a parsed <l>Via</l> HTTP response header.
 *
 * <p>
 * Informs the client of proxies through which the response was sent.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Via: 1.0 fred, 1.1 example.com (Apache/1.1)
 * </p>
 *
 * <p>
 * Informs the client of proxies through which the response was sent.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Via: 1.0 fred, 1.1 example.com (Apache/1.1)
 * </p>
 *
 * <p>
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Via general-header field MUST be used by gateways and proxies to indicate the intermediate protocols and
 * recipients between the user agent and the server on requests, and between the origin server and the client on
 * responses.
 * It is analogous to the "Received" field of RFC 822 and is intended to be used for tracking message forwards,
 * avoiding request loops, and identifying the protocol capabilities of all senders along the request/response chain.
 *
 * <p class='bcode'>
 * 	Via =  "Via" ":" 1#( received-protocol received-by [ comment ] )
 * 	received-protocol = [ protocol-name "/" ] protocol-version
 * 	protocol-name     = token
 * 	protocol-version  = token
 * 	received-by       = ( host [ ":" port ] ) | pseudonym
 * 	pseudonym         = token
 * </p>
 *
 * <p>
 * The received-protocol indicates the protocol version of the message received by the server or client along each
 * segment of the request/response chain.
 * The received-protocol version is appended to the Via field value when the message is forwarded so that information
 * about the protocol capabilities of upstream applications remains visible to all recipients.
 *
 * <p>
 * The protocol-name is optional if and only if it would be "HTTP".
 * The received-by field is normally the host and optional port number of a recipient server or client that subsequently
 * forwarded the message.
 * However, if the real host is considered to be sensitive information, it MAY be replaced by a pseudonym.
 * If the port is not given, it MAY be assumed to be the default port of the received-protocol.
 *
 * <p>
 * Multiple Via field values represents each proxy or gateway that has forwarded the message.
 * Each recipient MUST append its information such that the end result is ordered according to the sequence of
 * forwarding applications.
 *
 * <p>
 * Comments MAY be used in the Via header field to identify the software of the recipient proxy or gateway, analogous
 * to the User-Agent and Server header fields.
 * However, all comments in the Via field are optional and MAY be removed by any recipient prior to forwarding the
 * message.
 *
 * <p>
 * For example, a request message could be sent from an HTTP/1.0 user agent to an internal proxy code-named "fred",
 * which uses HTTP/1.1 to forward the request to a public proxy at nowhere.com, which completes the request by
 * forwarding it to the origin server at www.ics.uci.edu.
 * The request received by www.ics.uci.edu would then have the following Via header field:
 * <p class='bcode'>
 * 	Via: 1.0 fred, 1.1 nowhere.com (Apache/1.1)
 * </p>
 *
 * <p>
 * Proxies and gateways used as a portal through a network firewall SHOULD NOT, by default, forward the names and ports
 * of hosts within the firewall region.
 * This information SHOULD only be propagated if explicitly enabled.
 * If not enabled, the received-by host of any host behind the firewall SHOULD be replaced by an appropriate pseudonym
 * for that host.
 *
 * <p>
 * For organizations that have strong privacy requirements for hiding internal structures, a proxy MAY combine an
 * ordered subsequence of Via header field entries with identical received-protocol values into a single such entry.
 * For example...
 * <p class='bcode'>
 * 	Via: 1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy
 * </p>
 *
 * <p>
 * ...could be collapsed to...
 * <p class='bcode'>
 * 	Via: 1.0 ricky, 1.1 mertz, 1.0 lucy
 * </p>
 *
 * <p>
 * Applications SHOULD NOT combine multiple entries unless they are all under the same organizational control and the
 * hosts have already been replaced by pseudonyms.
 * Applications MUST NOT combine entries which have different received-protocol values.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Via")
public class Via extends BasicCsvHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Via";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Via of(String value) {
		return value == null ? null : new Via(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Via of(String...value) {
		return value == null ? null : new Via(value);
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
	public static Via of(Supplier<String[]> value) {
		return value == null ? null : new Via(value);
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
	public Via(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Via(String...value) {
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
	public Via(Supplier<String[]> value) {
		super(NAME, value);
	}
}
