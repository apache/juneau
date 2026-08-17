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
package org.apache.juneau.rest.client.apachehttpclient50;

import org.apache.hc.client5.http.impl.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.protocol.*;

/**
 * {@link DefaultRedirectStrategy} that never auto-follows a redirect while
 * {@link ApacheHc5Transport#isPolicyActive()} is set on the calling thread &mdash; the SSRF guardrail requires
 * every hop of a policy-covered {@code @Remote} redirect chain to be re-validated (deny-private pre-check +
 * pin-on-connect) before it is followed, which Apache HttpClient's internal redirect executor cannot do. Instead,
 * {@link org.apache.juneau.rest.client.PolicyEnforcedRedirects} inspects the returned (unfollowed) {@code 3xx}
 * response itself and runs its own Juneau-controlled follow-and-revalidate loop.
 *
 * <p>
 * Ordinary (non-{@code @Remote}) requests on the same transport are completely unaffected: {@link #isRedirected}
 * delegates to the default behavior whenever the guard is not active on the calling thread.
 *
 * @since 10.0.0
 */
final class PolicyAwareRedirectStrategy extends DefaultRedirectStrategy {

	@Override /* DefaultRedirectStrategy */
	public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
		if (ApacheHc5Transport.isPolicyActive())
			return false;
		return super.isRedirected(request, response, context);
	}
}
