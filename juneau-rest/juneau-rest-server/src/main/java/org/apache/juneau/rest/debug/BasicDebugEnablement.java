// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.debug;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.rest.annotation.RestOpAnnotation.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;

/**
 * Default implementation of the {@link DebugEnablement} interface.
 * 
 * <p>
 * Enables debug mode based on the following annotations:
 * <ul>
 * 	<li class='ja'>{@link Rest#debug()}
 * 	<li class='ja'>{@link RestOp#debug()}
 * 	<li class='ja'>{@link Rest#debugOn()}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.LoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 */
public class BasicDebugEnablement extends DebugEnablement {

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this enablement.
	 */
	public BasicDebugEnablement(BeanStore beanStore) {
		super(beanStore);
	}

	@Override
	protected Builder init(BeanStore beanStore) {
		Builder b = super.init(beanStore);

		DefaultSettingsMap defaultSettings = beanStore.getBean(DefaultSettingsMap.class).get();
		RestContext.Builder builder = beanStore.getBean(RestContext.Builder.class).get();
		ResourceSupplier resource = beanStore.getBean(ResourceSupplier.class).get();
		VarResolver varResolver = beanStore.getBean(VarResolver.class).get();

		// Default debug enablement if not overridden at class/method level.
		Enablement debugDefault = defaultSettings.get(Enablement.class, "RestContext.debugDefault").orElse(builder.isDebug() ? Enablement.ALWAYS : Enablement.NEVER);
		b.defaultEnable(debugDefault);

		ClassInfo ci = ClassInfo.ofProxy(resource.get());

		// Gather @Rest(debug) settings.
		ci.forEachAnnotation(
			Rest.class,
			x -> true,
			x -> {
				String x2 = varResolver.resolve(x.debug());
				if (! x2.isEmpty())
					b.enable(Enablement.fromString(x2), ci.getFullName());
			}
		);

		// Gather @RestOp(debug) settings.
		ci.forEachPublicMethod(
			x -> true,
			x -> {
				x.getAnnotationList(REST_OP_GROUP).forEachValue(
					String.class,
					"debug",
					y -> true,
					y -> {
						String y2 = varResolver.resolve(y);
						if (! y2.isEmpty())
							b.enable(Enablement.fromString(y2), x.getFullName());
					}
				);
			}
		);

		// Gather @Rest(debugOn) settings.
		ci.forEachAnnotation(
			Rest.class,
			x -> true,
			x -> {
				String x2 = varResolver.resolve(x.debugOn());
				for (Map.Entry<String,String> e : splitMap(x2, true).entrySet()) {
					String k = e.getKey(), v = e.getValue();
					if (v.isEmpty())
						v = "ALWAYS";
					if (! k.isEmpty())
						b.enable(Enablement.fromString(v), k);
				}
			}
		);

		return b;
	}
}
