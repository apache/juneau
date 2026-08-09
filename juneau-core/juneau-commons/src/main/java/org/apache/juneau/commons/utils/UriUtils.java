/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;

/**
 * Various {@link URI}-related utility methods.
 *
 * @since 10.0.0
 */
public class UriUtils {

	/**
	 * Returns <jk>true</jk> if the specified URI either uses the <js>"https"</js> scheme or targets a loopback
	 * host.
	 *
	 * <p>
	 * A loopback host is one of <js>"localhost"</js> (case-insensitive), <js>"127.0.0.1"</js>, <js>"::1"</js>, or
	 * <js>"[::1]"</js>.  A plaintext (<js>"http"</js>) URI aimed at any other host returns <jk>false</jk>, as does
	 * a <jk>null</jk> or scheme-less (malformed) URI.  This is the check to make before transmitting anything
	 * sensitive to a caller-configured endpoint so that a plaintext transport is only tolerated for local
	 * development.
	 *
	 * @param uri The URI to check.  Can be <jk>null</jk> (returns <jk>false</jk>).
	 * @return <jk>true</jk> if the URI is <js>"https"</js> or targets a loopback host.
	 */
	public static boolean isSecureOrLoopback(URI uri) {
		if (uri == null)
			return false;
		if ("https".equalsIgnoreCase(uri.getScheme()))
			return true;
		var host = uri.getHost();
		return host != null && (host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost") || host.equals("[::1]") || host.equals("::1"));
	}

	/**
	 * Throwing variant of {@link #isSecureOrLoopback(URI)}.
	 *
	 * @param uri The URI to validate.  Can be <jk>null</jk> (rejected).
	 * @return The validated URI (for fluent use).
	 * @throws IllegalArgumentException If the URI is not <js>"https"</js> and does not target a loopback host.
	 */
	public static URI assertSecureOrLoopback(URI uri) {
		if (! isSecureOrLoopback(uri))
			throw iaex("URI must use https or target a loopback host: %s", uri);
		return uri;
	}

	/**
	 * Constructor.
	 */
	protected UriUtils() {}
}
