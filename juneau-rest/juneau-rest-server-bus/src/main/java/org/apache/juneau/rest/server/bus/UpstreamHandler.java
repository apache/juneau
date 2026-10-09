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
package org.apache.juneau.rest.server.bus;

import org.apache.juneau.marshall.collections.*;

/**
 * Handles one upstream topic (page → server, WebSocket bridges only).  Runs on the container's WebSocket thread;
 * keep it short or hand off.  An exception is logged and answered with a generic {@code bus:upstream-failed} frame.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   UpstreamHandler <jv>cancelAll</jv> = (<jv>session</jv>, <jv>topic</jv>, <jv>payload</jv>) -&gt; {
 *     <jv>jobs</jv>.running().forEach(AsyncJob::cancel);
 *   };
 *   BusPolicy.<jsm>create</jsm>().upstream(<js>"ops.cancel-all"</js>, <jv>cancelAll</jv>);
 * </p>
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface UpstreamHandler {

	/**
	 * Handles one upstream publish.
	 *
	 * @param session The session that sent it.  Never <jk>null</jk>.
	 * @param topic The topic, already checked against the session's upstream grant.
	 * @param payload The JSON-object payload.  Never <jk>null</jk>.
	 * @throws Exception Any failure; it is logged and never crosses the wire.
	 */
	void handle(BusSession session, String topic, JsonMap payload) throws Exception;
}
