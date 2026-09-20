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
package org.apache.juneau.rest.server.view.freemarker;

import org.apache.juneau.rest.server.*;

/**
 * Request-scoped handle so FreeMarker {@code TemplateDirectiveModel}s can resolve per-request
 * asset URLs during {@code template.process}.
 *
 * <p>
 * {@link FreemarkerViewRenderer} opens the scope around its {@code template.process(...)} call and
 * always closes it in a {@code finally}, so directives such as {@code <@page toolkit=...>} can reach
 * the in-flight {@link RestRequest} (e.g. to call {@code ViewsMixin.viewAssetUrl}) without threading
 * it through every template model.
 *
 * @since 10.0.0
 */
public final class FreemarkerRenderScope {

	private static final ThreadLocal<RestRequest> REQ = new ThreadLocal<>();

	private FreemarkerRenderScope() {}

	static void open(RestRequest req) {
		REQ.set(req);
	}

	static void close() {
		REQ.remove();
	}

	/**
	 * Returns the request for the in-flight FreeMarker render.
	 *
	 * @return The {@link RestRequest} for the in-flight FreeMarker render, or {@code null} if none.
	 */
	public static RestRequest request() {
		return REQ.get();
	}
}
