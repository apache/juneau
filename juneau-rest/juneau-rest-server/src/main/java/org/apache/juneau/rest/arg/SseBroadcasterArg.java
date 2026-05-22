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

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.sse.*;

/**
 * Resolves {@link SseBroadcaster} method parameters.
 */
public class SseBroadcasterArg extends SimpleRestOperationArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new arg, or {@code null} if not applicable.
	 */
	public static SseBroadcasterArg create(ParameterInfo paramInfo) {
		if (paramInfo.isType(SseBroadcaster.class))
			return new SseBroadcasterArg();
		return null;
	}

	/**
	 * Constructor.
	 */
	@SuppressWarnings({
		"resource" // Broadcaster bean is container-managed in BeanStore and intentionally not closed by arg resolver.
	})
	protected SseBroadcasterArg() {
		super(opSession -> opSession.getBeanStore().getBean(SseBroadcaster.class).orElseGet(() -> opSession.getBeanStore().add(SseBroadcaster.class, SseBroadcaster.create())));
	}
}
