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
 * Jakarta WebSocket 2.2 transport for the console message bus.
 *
 * <p>
 * The server half of the bus ({@code org.apache.juneau.rest.server.bus}) carries frames over SSE with no extra
 * dependency.  This package adds the WebSocket transport, which also carries upstream (page &rarr; server) frames.
 * The servlet container supplies the implementation (for example Jetty's {@code jetty-ee11-websocket-jakarta-server}
 * or Tomcat's {@code tomcat-embed-websocket}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jk>public void</jk> contextInitialized(ServletContextEvent <jv>e</jv>) {
 *     BusWebSockets.<jsm>register</jsm>(<jv>e</jv>.getServletContext(), JobsRest.<jsf>BUS</jsf>);   <jc>// "/juneau-bus/ws/{sessionId}"</jc>
 *   }
 * </p>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.bus.websocket;
