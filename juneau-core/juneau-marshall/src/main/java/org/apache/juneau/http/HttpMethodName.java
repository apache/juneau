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
package org.apache.juneau.http;

/**
 * Represents valid HTTP 1.1 method name static strings per the RFC 2616 spec.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'>
 * 		<a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class HttpMethodName {

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.2" class="doclink">OPTIONS</a> */
	public static final String OPTIONS = "OPTIONS";

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3" class="doclink">GET</a> */
	public static final String GET = "GET";

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.4" class="doclink">HEAD</a> */
	public static final String HEAD = "HEAD";

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5" class="doclink">POST</a> */
	public static final String POST = "POST";

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.6" class="doclink">PUT</a> */
	public static final String PUT = "PUT";

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.7" class="doclink">DELETE</a> */
	public static final String DELETE = "DELETE";

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.8" class="doclink">TRACE</a> */
	public static final String TRACE = "TRACE";

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.9" class="doclink">CONNECT</a> */
	public static final String CONNECT = "CONNECT";

	/** Special case for a REST method that implements a proxy interface. */
	public static final String PROXY = "PROXY";

	/** A non-standard value. */
	public static final String OTHER = "OTHER";
}
