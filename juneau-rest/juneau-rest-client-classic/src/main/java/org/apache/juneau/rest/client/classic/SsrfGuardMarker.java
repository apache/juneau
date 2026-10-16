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
package org.apache.juneau.rest.client.classic;

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.protocol.*;
import org.apache.juneau.rest.client.classic.remote.*;

/**
 * The per-request marker of an SSRF-guard-active {@code @Remote} call.
 *
 * <p>
 * A guarded {@link RestRequest} sets the {@link #ATTRIBUTE} attribute on the {@link HttpContext} of every request it
 * sends (each redirect hop included).  The client's connection manager, request interceptor, request executor and
 * redirect strategy read it from there, so the guard holds on whichever thread Apache HttpClient executes the request.
 * The calling thread's {@link RemoteUrlPolicyState} is still honoured too, so a request made on that thread while a
 * guarded call is running stays guarded.
 *
 * <p>
 * It fails closed: a request created by a guarded {@link RestRequest} whose context lacks the marker (one a custom
 * call handler sent with a different context) is refused, not sent unguarded.
 *
 * @since 10.0.0
 */
final class SsrfGuardMarker {

	/** The {@link HttpContext} attribute that marks a request as SSRF-guard-active; its value is {@link Boolean#TRUE}. */
	static final String ATTRIBUTE = "org.apache.juneau.rest.client.classic.ssrfGuard";

	private SsrfGuardMarker() {}

	/**
	 * Marks the given context as that of an SSRF-guard-active request.
	 *
	 * @param context The context.
	 * @return The previous value of the attribute, so the caller can restore it.
	 */
	static Object mark(HttpContext context) {
		var previous = context.getAttribute(ATTRIBUTE);
		context.setAttribute(ATTRIBUTE, Boolean.TRUE);
		return previous;
	}

	/**
	 * Returns whether the request is SSRF-guard-active: its context carries the marker, or the calling thread is
	 * running a guarded call.
	 *
	 * @param request The request, or <jk>null</jk> to use the one on the context.
	 * @param context The context, or <jk>null</jk>.
	 * @return <jk>true</jk> if the request is guarded.
	 * @throws SsrfGuardConnectionManager.GuardRefusedException If a guarded {@link RestRequest} created the request but
	 * 	the context lacks the marker.
	 */
	static boolean isGuarded(HttpRequest request, HttpContext context) {
		if (nn(context) && Boolean.TRUE.equals(context.getAttribute(ATTRIBUTE)))
			return true;
		var r = nn(request) ? request : nn(context) ? (HttpRequest)context.getAttribute(HttpCoreContext.HTTP_REQUEST) : null;
		while (r instanceof HttpRequestWrapper w)
			r = w.getOriginal();
		if (r instanceof RestRequestCreated c && nn(c.getRestRequest()) && c.getRestRequest().isSsrfGuarded())
			throw new SsrfGuardConnectionManager.GuardRefusedException("Refusing to send a policy-covered @Remote request "
				+ "whose HttpContext lacks the SSRF-guard marker: it was sent with a context other than the one its RestRequest "
				+ "passed on (for example by a custom RestCallHandler), so the SSRF guardrail's connect-time contract "
				+ "(pin-on-connect + redirect revalidation) cannot be confirmed.  Pass the context through unchanged, or set "
				+ "allowPrivateUrls(true) if this is an intentional local-dev/intranet target.");
		return RemoteUrlPolicyState.isActive();
	}
}
