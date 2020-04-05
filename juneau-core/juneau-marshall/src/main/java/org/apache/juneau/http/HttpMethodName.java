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
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
public class HttpMethodName {

	/** {@doc RFC2616.section9#sec9.2 OPTIONS} */
	public static final String OPTIONS = "OPTIONS";

	/** {@doc RFC2616.section9#sec9.3 GET} */
	public static final String GET = "GET";

	/** {@doc RFC2616.section9#sec9.4 HEAD} */
	public static final String HEAD = "HEAD";

	/** {@doc RFC2616.section9#sec9.5 POST} */
	public static final String POST = "POST";

	/** {@doc RFC2616.section9#sec9.6 PUT} */
	public static final String PUT = "PUT";

	/** {@doc RFC2616.section9#sec9.7 DELETE} */
	public static final String DELETE = "DELETE";

	/** {@doc RFC2616.section9#sec9.8 TRACE} */
	public static final String TRACE = "TRACE";

	/** {@doc RFC2616.section9#sec9.9 CONNECT} */
	public static final String CONNECT = "CONNECT";

	/** {@doc https://tools.ietf.org/html/rfc5789 PATCH} */
	public static final String PATCH = "PATCH";

	/** Special case for a REST method that implements a REST-RPC interface. */
	public static final String RRPC = "RRPC";

	/** A non-standard value. */
	public static final String OTHER = "OTHER";

	/** Represents any HTTP method. */
	public static final String ANY = "*";
}
