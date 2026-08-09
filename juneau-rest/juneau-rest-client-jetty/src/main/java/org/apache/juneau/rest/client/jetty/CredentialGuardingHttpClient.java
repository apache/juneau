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
package org.apache.juneau.rest.client.jetty;

import java.net.*;

import org.apache.juneau.rest.client.*;
import org.eclipse.jetty.client.*;

/**
 * {@link HttpClient} that removes caller-set credential headers when a request is copied to follow a
 * {@code 3xx} redirect to a different origin.
 *
 * <p>
 * Jetty follows redirects by copying the original request &mdash; including {@code Authorization} and
 * {@code Cookie} headers &mdash; onto a new request aimed at the {@code Location} target.  When that target
 * is a different scheme, host, or port (or an {@code https}&rarr;{@code http} downgrade), forwarding those
 * headers would disclose credentials to an unrelated origin.  This subclass hooks the copy step and strips
 * the credential headers whenever the target origin differs from the request being copied.
 *
 * <p>
 * The forward/strip decision and the header set are delegated to {@link RedirectSecurity} so that all
 * transports share one policy.
 */
final class CredentialGuardingHttpClient extends HttpClient {

	@Override /* HttpClient */
	protected Request copyRequest(Request oldRequest, URI newURI) {
		var newRequest = super.copyRequest(oldRequest, newURI);
		if (shouldStrip(originOf(oldRequest), newURI))
			for (var name : RedirectSecurity.stripOnCrossOrigin())
				newRequest.headers(fields -> fields.remove(name));
		return newRequest;
	}

	private static URI originOf(Request request) {
		try {
			return new URI(request.getScheme(), null, request.getHost(), request.getPort(), null, null, null);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	private static boolean shouldStrip(URI from, URI to) {
		try {
			return RedirectSecurity.shouldStripCredentials(from, to);
		} catch (RuntimeException e) {
			// If the origins cannot be compared, err on the side of not forwarding credentials.
			return true;
		}
	}
}
