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
package org.apache.juneau.rest.arg;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.sse.*;

/**
 * Resolves {@link SseSubscription} method parameters.
 */
public class SseSubscriptionArg extends SimpleRestOperationArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new arg, or {@code null} if not applicable.
	 */
	public static SseSubscriptionArg create(ParameterInfo paramInfo) {
		if (paramInfo.isType(SseSubscription.class))
			return new SseSubscriptionArg();
		return null;
	}

	/**
	 * Constructor.
	 */
	@SuppressWarnings({
		"resource" // Subscription is caller-owned and closed by SSE response handling.
	})
	protected SseSubscriptionArg() {
		super(opSession -> {
			var broadcaster = opSession.getBeanStore().getBean(SseBroadcaster.class).orElseGet(() -> opSession.getBeanStore().add(SseBroadcaster.class, SseBroadcaster.create()));
			var id = opt(opSession.getRequest().getHttpServletRequest().getRequestId()).orElse(UUID.randomUUID().toString());
			return broadcaster.subscribe(id);
		});
	}
}
