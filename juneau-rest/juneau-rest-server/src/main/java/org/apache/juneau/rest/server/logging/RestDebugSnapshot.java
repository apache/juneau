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
package org.apache.juneau.rest.server.logging;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.commons.logging.*;

/**
 * Immutable holder of the resolution-time state a completed request needs to render its single debug record.
 *
 * <p>
 * For an asynchronous response the record is rendered on the response-completion thread, after the request thread has
 * already released the {@code localSession} thread-local. This snapshot is resolved on the request thread at the
 * async-dispatch handoff and published before the completion callback is registered, so the completion thread never has
 * to re-resolve the logger/formatter/tier from foreign-thread state.
 *
 * <p>
 * The captured {@code tier} is the resolved <b>detail tier</b> ({@code INFO}/{@code FINE}/{@code FINEST}) that selects
 * how much of the message is rendered. It is deliberately separate from the emitted record level, which is always
 * {@link Level#INFO}.
 *
 * <p>
 * The {@code context} component carries the request thread's {@link org.apache.juneau.commons.logging.LogContext}
 * snapshot taken at the async-dispatch handoff. For an async response the completion thread's live {@code LogContext} is
 * empty (v1 is thread-confined; there is no general async propagation), so {@code RestDebugPipeline.emit(...)} pre-seeds
 * the completion-thread record from this carried map &mdash; the only way the async debug record retains its structured
 * {@code requestId} (design §8.2). Empty (the shared empty-map singleton) when no request context was active.
 *
 * @param logger The resolved logger the record is emitted through.
 * @param formatter The resolved (non-<jk>null</jk>) formatter used to render the message.
 * @param tier The resolved detail tier ({@code INFO}/{@code FINE}/{@code FINEST}); never <jk>null</jk> (a null tier
 * 	means access logging is off, in which case no snapshot is created).
 * @param context The request thread's {@code LogContext} snapshot to re-establish on the completion thread. Never
 * 	<jk>null</jk> (empty singleton when no context was active).
 */
record RestDebugSnapshot(RichLogger logger, RestDebugFormatter formatter, Level tier, Map<String,Object> context) {}
