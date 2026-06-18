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

import java.util.*;

import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.*;

/**
 * Shared worker for the {@code /info} management endpoint:  renders the running application's
 * {@link ManifestFile} main attributes (build/version/git metadata) as a map.
 *
 * <p>
 * Both the {@link InfoMixin mixin} and {@link InfoResource resource} flavors delegate here, so the two
 * forms cannot drift &mdash; the same shared-worker pattern the health and metrics surfaces use.
 *
 * <p>
 * The manifest is resolved from the host bean store.  A standalone microservice registers its
 * {@link ManifestFile} bean during {@code Microservice.init()}, so the {@code Main-*} attributes (and any
 * build-side stamped {@code Build-*}/{@code Git-*} fields) are reachable here.  When no manifest bean is
 * present the endpoint degrades cleanly to an empty map rather than failing.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ManagementSurface">Management Surface</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class InfoManager {

	/**
	 * Resolves the application's {@link ManifestFile} from the host context's bean store.
	 *
	 * @param context The REST context whose bean store is searched.  May be <jk>null</jk>.
	 * @return The manifest, or <jk>null</jk> if none is registered (or the context is <jk>null</jk>).
	 */
	@SuppressWarnings({
		"resource" // The bean store is owned by the RestContext; this adapter only borrows beans and must not close it.
	})
	public ManifestFile resolveManifest(RestContext context) {
		if (context == null)
			return null;
		return context.getBeanStore().getBean(ManifestFile.class).orElse(null);
	}

	/**
	 * Renders the manifest's main attributes (build/version/git metadata) as a sorted map.
	 *
	 * @param manifest The resolved manifest, or <jk>null</jk>.
	 * @return A sorted map of manifest attribute &rarr; value (empty when {@code manifest} is <jk>null</jk>); never <jk>null</jk>.
	 */
	public JsonMap getInfo(ManifestFile manifest) {
		var out = new JsonMap();
		if (manifest != null)
			out.putAll(new TreeMap<>(manifest.asMap()));
		return out;
	}
}
