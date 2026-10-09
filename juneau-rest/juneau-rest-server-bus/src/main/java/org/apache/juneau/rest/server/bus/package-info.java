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
/**
 * Server half of the console message bus (spec §11).
 *
 * <p>
 * A {@link ServerBus} holds retained topic values and the sessions that bridge them
 * to pages; a {@link org.apache.juneau.rest.server.bus.BusPolicy} says what may flow, in which direction, to whom
 * (everything else is denied); {@link BusEventsMixin} exposes the CSRF-guarded session
 * POST and the capability-gated SSE stream on a REST resource.  The WebSocket transport is the separate
 * {@code juneau-rest-server-bus-websocket} module.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <ja>@Rest</ja>(path=<js>"/ops/jobs"</js>)
 *   <jk>public class</jk> JobsRest <jk>extends</jk> BasicRestServlet <jk>implements</jk> BusEventsMixin {
 *     <jk>static final</jk> ServerBus <jsf>BUS</jsf> = ServerBus.<jsm>create</jsm>(BusPolicy.<jsm>create</jsm>()
 *       .downstream(<js>"ops.jobs"</js>, <jk>true</jk>).build());
 *     <ja>@Override</ja> <jk>public</jk> ServerBus serverBus() { <jk>return</jk> <jsf>BUS</jsf>; }
 *   }
 *
 *   <jc>// Anywhere in the application:</jc>
 *   JobsRest.<jsf>BUS</jsf>.publish(<js>"ops.jobs"</js>, JsonMap.<jsm>of</jsm>(<js>"schemaVersion"</js>, 1, <js>"running"</js>, <jv>running</jv>));
 * </p>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.bus;
