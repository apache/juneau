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
package org.apache.juneau.rest.server.widgets;

/**
 * The body-slot contract for a {@link Card}: the single, heterogeneous content a card hosts.
 *
 * <p>
 * This marker is the extension point future body types implement.  The emitter (in the views module,
 * {@code CardGridTable}) dispatches over a <b>closed</b> set of known implementations and <b>fails closed</b> on
 * any unknown {@code CardBody}; a new body type is added by implementing this interface, patching the emitter's
 * closed dispatch set, and bringing its <b>own</b> server-render and (if stateful) its <b>own</b> data path
 * &mdash; it does not share {@link CardFieldList}'s refresh envelope.
 *
 * @since 10.0.0
 */
public interface CardBody {

	/**
	 * Fail-closed bean validation.
	 *
	 * @throws IllegalArgumentException If this body is not well-formed.
	 */
	void validate();
}
