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
package org.apache.juneau.rest.auth.oauth.flow;

import static org.apache.juneau.commons.utils.UriUtils.assertSecureOrLoopback;

import java.io.*;
import java.net.*;
import java.nio.charset.*;

import org.apache.juneau.commons.utils.*;

import com.nimbusds.jose.util.*;

/**
 * HTTP retriever for remote JWK sets.
 *
 * <p>
 * Differs from the default retriever in three ways:
 * <ul>
 * 	<li>Redirects are never followed; a redirect response is an error.
 * 	<li>The URL must use <js>"https"</js> or target a loopback host.
 * 	<li>The response body is capped at {@link #MAX_BYTES} bytes.
 * </ul>
 *
 * @since 10.0.0
 */
public class JwksResourceRetriever implements ResourceRetriever {

	/** Maximum accepted response body size, in bytes. */
	public static final int MAX_BYTES = 64 * 1024;

	private static final int TIMEOUT_MS = 5000;

	@Override /* Overridden from ResourceRetriever */
	public Resource retrieveResource(URL url) throws IOException {
		try {
			assertSecureOrLoopback(url.toURI());
		} catch (URISyntaxException | IllegalArgumentException e) {
			throw new IOException("JWK set URL is not permitted: " + url, e);
		}
		var con = (HttpURLConnection)url.openConnection();
		try {
			con.setInstanceFollowRedirects(false);
			con.setConnectTimeout(TIMEOUT_MS);
			con.setReadTimeout(TIMEOUT_MS);
			var status = con.getResponseCode();
			if (status != 200)
				throw new IOException("JWK set fetch failed with HTTP status " + status);
			var out = new ByteArrayOutputStream();
			try (var in = con.getInputStream()) {
				var buf = new byte[4096];
				int n;
				while ((n = in.read(buf)) >= 0) {
					out.write(buf, 0, n);
					if (out.size() > MAX_BYTES)
						throw new IOException("JWK set response exceeds " + MAX_BYTES + " bytes");
				}
			}
			return new Resource(out.toString(StandardCharsets.UTF_8), con.getContentType());
		} finally {
			con.disconnect();
		}
	}
}
