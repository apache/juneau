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
 * JUL-level-driven REST debug logging API.
 *
 * <p>
 * A resource's own {@link java.util.logging.Logger} level is the sole control for request/response debug capture.
 * Verbosity tiers are cumulative: {@code INFO} = basic status line, {@code FINE} = + headers, {@code FINEST} = + bodies.
 * The single public extension point is the {@link org.apache.juneau.rest.server.logging.RestDebugFormatter} interface.
 */
package org.apache.juneau.rest.server.logging;
