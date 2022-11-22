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
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Connection</l> HTTP request header.
 *
 * <p>
 * Control options for the current connection and list of hop-by-hop request fields.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Connection: keep-alive
 * 	Connection: Upgrade
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Connection general-header field allows the sender to specify options that are desired for that particular
 * connection and MUST NOT be communicated by proxies over further connections.
 *
 * <p>
 * The Connection header has the following grammar:
 * <p class='bcode'>
 * 	Connection = "Connection" ":" 1#(connection-token)
 * 	connection-token  = token
 * </p>
 *
 * <p>
 * HTTP/1.1 proxies MUST parse the Connection header field before a message is forwarded and, for each connection-token
 * in this field, remove any header field(s) from the message with the same name as the connection-token.
 * Connection options are signaled by the presence of a connection-token in the Connection header field, not by any
 * corresponding additional header field(s), since the additional header field may not be sent if there are no
 * parameters associated with that connection option.
 *
 * <p>
 * Message headers listed in the Connection header MUST NOT include end-to-end headers, such as Cache-Control.
 *
 * <p>
 * HTTP/1.1 defines the "close" connection option for the sender to signal that the connection will be closed after
 * completion of the response.
 * For example...
 * <p class='bcode'>
 * 	Connection: close
 * </p>
 * <p>
 * ...in either the request or the response header fields indicates that the connection SHOULD NOT be considered
 * `persistent' (section 8.1) after the current request/response is complete.
 *
 * <p>
 * HTTP/1.1 applications that do not support persistent connections MUST include the "close" connection option in
 * every message.
 *
 * <p>
 * A system receiving an HTTP/1.0 (or lower-version) message that includes a Connection header MUST, for each
 * connection-token in this field, remove and ignore any header field(s) from the message with the same name as the
 * connection-token.
 * This protects against mistaken forwarding of such header fields by pre-HTTP/1.1 proxies.
 * See section 19.6.2.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Connection")
public class Connection extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Connection";

	private static final Cache<String,Connection> CACHE = Cache.of(String.class, Connection.class).build();

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Connection of(String value) {
		return value == null ? null : CACHE.get(value, ()->new Connection(value));
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
	public static Connection of(Supplier<String> value) {
		return value == null ? null : new Connection(value);
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
	public Connection(String value) {
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
	public Connection(Supplier<String> value) {
		super(NAME, value);
	}

	/**
	 * Returns <jk>true</jk> if the header value is <c>close</c>.
	 *
	 * @return <jk>true</jk> if the header value is <c>close</c>.
	 */
	public boolean isClose() {
		return equalsIgnoreCase("close");
	}

	/**
	 * Returns <jk>true</jk> if the header value is <c>keep-alive</c>.
	 *
	 * @return <jk>true</jk> if the header value is <c>keep-alive</c>.
	 */
	public boolean isKeepAlive() {
		return equalsIgnoreCase("keep-alive");
	}

	/**
	 * Returns <jk>true</jk> if the header value is <c>upgrade</c>.
	 *
	 * @return <jk>true</jk> if the header value is <c>upgrade</c>.
	 */
	public boolean isUpgrade() {
		return equalsIgnoreCase("upgrade");
	}
}
