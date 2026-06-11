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
package org.apache.juneau.rest.client;

/**
 * Lifecycle interceptor for REST calls made by {@link RestClient}.
 *
 * <p>
 * Registered on the client builder via {@link RestClient.Builder#interceptors(RestCallInterceptor...)}.
 * All callbacks have default no-op implementations so implementations only need to override the hooks they care about.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.interceptors(new RestCallInterceptor() {
 * 			&#64;Override
 * 			<jk>public void</jk> onConnect(RestRequest req, RestResponse res) {
 * 				System.<jf>out</jf>.println(<js>"Response: "</js> + res.getStatusCode());
 * 			}
 * 		})
 * 		.build();
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S112" // Interceptor SPI callbacks intentionally declare 'throws Exception' so implementations may abort a call with any exception type.
})
public interface RestCallInterceptor {

	/**
	 * Called before the transport request is built and sent.
	 *
	 * <p>
	 * Use this hook to add headers, modify query parameters, or short-circuit the call.
	 *
	 * @param req The request being built. Never <jk>null</jk>.
	 * @throws Exception If the call should be aborted.
	 */
	default void onInit(RestRequest req) throws Exception {}

	/**
	 * Called after a response has been received from the transport.
	 *
	 * <p>
	 * Use this hook to inspect status codes, validate headers, or modify how the response is interpreted.
	 *
	 * @param req The request that was sent. Never <jk>null</jk>.
	 * @param res The response received. Never <jk>null</jk>.
	 * @throws Exception If a post-connect error should be raised.
	 */
	default void onConnect(RestRequest req, RestResponse res) throws Exception {}

	/**
	 * Called when the request is being closed (in the {@code finally} block of {@link RestRequest#run()}).
	 *
	 * <p>
	 * This is called regardless of whether the call succeeded or failed, and is always invoked before
	 * the logger receives its entry. Use it for cleanup, metrics recording, or span/trace finalization.
	 *
	 * @param req The request. Never <jk>null</jk>.
	 * @param res The response, or <jk>null</jk> if the transport failed before a response was received.
	 * @throws Exception If an error occurs during close handling.
	 */
	default void onClose(RestRequest req, RestResponse res) throws Exception {}
}
