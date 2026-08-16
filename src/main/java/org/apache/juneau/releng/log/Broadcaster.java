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

package org.apache.juneau.releng.log;

import java.util.function.Consumer;

/**
 * Common subscribe shape shared by {@link LogBroadcaster} (console log lines) and
 * {@link RunStateBroadcaster} (run/step status snapshots) so {@link SseLogServlet}'s tail loop can serve
 * either channel without duplicating it.
 */
public interface Broadcaster {

	/** Subscribes {@code sink} to every payload published from now on; closing the return unsubscribes it. */
	AutoCloseable subscribe(Consumer<String> sink);
}
