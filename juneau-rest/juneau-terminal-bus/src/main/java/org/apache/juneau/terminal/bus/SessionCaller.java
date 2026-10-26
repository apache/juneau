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
package org.apache.juneau.terminal.bus;

import org.apache.juneau.console.bus.*;
import org.apache.juneau.terminal.region.*;

/** A transport-neutral view of a bus session, so no {@link BusSession} reaches app code. */
final class SessionCaller implements TerminalCaller {
	private final BusSession session;

	SessionCaller(BusSession session) {
		this.session = session;
	}

	@Override public String principalName() { return session.principalName(); }
	@Override public Object attribute(String name) { return session.attribute(name); }
}
