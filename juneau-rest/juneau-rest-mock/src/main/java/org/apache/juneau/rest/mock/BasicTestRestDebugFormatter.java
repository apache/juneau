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
package org.apache.juneau.rest.mock;

import org.apache.juneau.rest.server.logging.*;

/**
 * Default {@link RestDebugFormatter} registered by {@link MockRestClient} / {@code mock.classic.MockRestClient} for
 * mocked resources.
 *
 * <p>
 * Identical to {@link BasicRestDebugFormatter} today &mdash; the shipped defaults (credential-bearing header
 * redaction, an 8&nbsp;KB body capture cap) are already test-safe. Kept as a distinct type, rather than registering
 * {@link BasicRestDebugFormatter} directly, so mock-specific rendering can be layered in independently of the
 * server's shipped default without affecting non-mock resources.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class BasicTestRestDebugFormatter extends BasicRestDebugFormatter {}
