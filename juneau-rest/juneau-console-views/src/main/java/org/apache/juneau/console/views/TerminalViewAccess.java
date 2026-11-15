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
package org.apache.juneau.console.views;

import org.apache.juneau.rest.server.*;

/**
 * The app's answer to "who may read this run's events".
 *
 * <p>
 * Applied by {@link TerminalViewMixin} before the run's source is resolved.  A caller that may not read the run is
 * answered {@code 404}, the same as an unknown id.
 */
@FunctionalInterface
public interface TerminalViewAccess {

	/**
	 * Whether the caller may read the run's events.
	 *
	 * @param req The in-flight request, carrying the caller's principal and request attributes.
	 * @param runId The run id (already checked against the grammar).
	 * @return <jk>true</jk> if the caller may read the run.
	 */
	boolean canView(RestRequest req, String runId);
}
