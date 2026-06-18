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
package org.apache.juneau.microservice.management;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Child-resource flavor of the {@code /info} endpoint.
 *
 * <p>
 * Mounts as a routed child via {@link Rest#children() @Rest(children=InfoResource.class)} under the subtree
 * {@code /info} and delegates to a shared {@link InfoManager} worker &mdash; the same logic the
 * {@link InfoMixin mixin} flavor uses, so the two forms cannot drift.  Extends {@link BasicRestResource} so
 * the returned bean serializes via the {@code BasicUniversalConfig} set (mirrors {@code HealthResource}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link InfoMixin}
 * 	<li class='jc'>{@link InfoManager}
 * </ul>
 *
 * @since 10.0.0
 */
@Rest(path="/info")
public class InfoResource extends BasicRestResource {

	private final InfoManager manager = new InfoManager();

	/**
	 * [GET /] - Application build/version/git metadata from the manifest.
	 *
	 * @param req The HTTP request.
	 * @return The manifest main attributes as a sorted map (empty when no manifest is registered).
	 */
	@RestGet(
		path="/*",
		summary="Application info",
		description="Renders the running application's manifest main attributes (build/version/git metadata)."
	)
	public JsonMap getInfo(RestRequest req) {
		return manager.getInfo(manager.resolveManifest(req.getContext()));
	}
}
