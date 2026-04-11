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
package org.apache.juneau.ng.rest.client;

/**
 * Pluggable logging interface for REST calls made by {@link NgRestClient}.
 *
 * <p>
 * Called in the {@code finally} block of every {@link NgRestRequest#run()} invocation, regardless of success or failure.
 * The {@link RestLogEntry} carries the request, response (or {@code null} on transport error), elapsed time, and error.
 *
 * <p>
 * This is a {@link FunctionalInterface} — lambda expressions and method references are valid implementations:
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	NgRestClient <jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.logger(<jv>entry</jv> -&gt; System.<jf>out</jf>.println(<jv>entry</jv>.format()))
 * 		.build();
 * </p>
 *
 * <p>
 * Multiple loggers can be combined using lambda composition:
 * <p class='bjava'>
 * 	RestLogger <jv>combined</jv> = <jv>entry</jv> -&gt; { <jv>errorLogger</jv>.log(<jv>entry</jv>); <jv>perfLogger</jv>.log(<jv>entry</jv>); };
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@FunctionalInterface
public interface RestLogger {

	/**
	 * Receives a log entry for a completed (or failed) REST call.
	 *
	 * @param entry The log entry. Never <jk>null</jk>.
	 */
	void log(RestLogEntry entry);
}
