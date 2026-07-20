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
package org.apache.juneau.rest.server.debug;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.rest.server.RestOpAnnotation.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.server.*;

/**
 * Default implementation of the {@link DebugEnablement} interface.
 *
 * <p>
 * Enables debug mode based on the following annotations:
 * <ul>
 * 	<li class='ja'>{@link Rest#debug()}
 * 	<li class='ja'>{@link RestOp#debug()}
 * 	<li class='jm'>{@link Debug#on()}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 */
public class BasicDebugEnablement extends DebugEnablement {

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this enablement.  Must not be <jk>null</jk>.
	 */
	public BasicDebugEnablement(BeanStore beanStore) {
		super(beanStore);
	}

	@Override
	protected Builder init(BeanStore beanStore) {
		var b = super.init(beanStore);

		var resource = beanStore.getBean(ResourceSupplier.class).orElseThrow(() -> new IllegalStateException("ResourceSupplier not found"));
		var varResolver = beanStore.getBean(VarResolver.class).orElseThrow(() -> new IllegalStateException("VarResolver not found"));
		var ap = AP;

		// Default debug enablement when no resource-class or operation-method debug setting is in effect.
		// RestContext.findDebugEnablement() unconditionally pre-publishes an Enablement bean derived from
		// @Rest(debugDefault=...) → pre-registered Enablement bean → @Rest(debug) boolean flag. Subclasses
		// only need to read it back here. (Pre-10.0 this method also resolved RestContext.Builder out of the
		// bean store to read isDebug(); that legacy protocol has been removed.)
		var debugDefault = beanStore.getBean(Enablement.class).orElse(Enablement.NEVER);
		b.defaultEnable(debugDefault);

		var ci = ClassInfo.ofProxy(resource.get());

		// Gather @Rest(debug) settings.
		// @formatter:off
		rstream(ap.find(Rest.class, ci)).map(AnnotationInfo::inner).forEach(x -> {
			var x2 = varResolver.resolve(x.debug().value());
			if (! x2.isEmpty())
				b.enable(Enablement.fromString(x2), ci.getNameFull());
		});
		// @formatter:on

		// Gather @RestOp(debug) settings.
		// @formatter:off
		ci.getPublicMethods()
			.forEach(x ->
				rstream(ap.find(x))
					.filter(REST_OP_GROUP)
					.flatMap(ai -> ai.getValue(String.class, "debug").stream())
					.filter(Shorts::ine)
					.map(varResolver::resolve)
					.map(Enablement::fromString)
					.filter(Objects::nonNull)
					.forEach(e -> b.enable(e, x.getNameFull()))
			);
		// @formatter:on

		// Gather @Rest(debugOn) settings.
		// @formatter:off
		rstream(ap.find(Rest.class, ci)).map(AnnotationInfo::inner).forEach(x -> {
			var x2 = varResolver.resolve(x.debug().on());
			for (var e : splitMap(x2, true).entrySet()) {
				var k = e.getKey();
				var v = e.getValue();
				if (v.isEmpty())
					v = "ALWAYS";
				if (! k.isEmpty())
					o(Enablement.fromString(v)).ifPresent(en -> b.enable(en, k));
			}
		});
		// @formatter:on

		return b;
	}
}