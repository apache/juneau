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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.Enablement.*;

import java.lang.reflect.*;
import java.util.function.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.utils.*;

/**
 * Default implementation of the {@link DebugEnablement} interface.
 *
 * TODO
 */
public class BasicDebugEnablement implements DebugEnablement {

	private final Enablement defaultEnablement;
	private final ReflectionMap<Enablement> enablementMap;
	private final Predicate<HttpServletRequest> conditionalPredicate;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public BasicDebugEnablement(DebugEnablementBuilder builder) {
		this.defaultEnablement = firstNonNull(builder.defaultEnablement, NEVER);
		this.enablementMap = builder.mapBuilder.build();
		this.conditionalPredicate = firstNonNull(builder.conditionalPredicate, x -> "true".equalsIgnoreCase(x.getHeader("Debug")));
	}

	@Override
	public boolean isDebug(RestOpContext context, HttpServletRequest req) {
		Method m = context.getJavaMethod();
		Enablement e = enablementMap.find(m).orElse(enablementMap.find(m.getDeclaringClass()).orElse(defaultEnablement));
		return e == ALWAYS || (e == CONDITIONAL && isConditionallyEnabled(req));
	}

	@Override
	public boolean isDebug(RestContext context, HttpServletRequest req) {
		Class<?> c = context.getResourceClass();
		Enablement e = enablementMap.find(c).orElse(defaultEnablement);
		return e == ALWAYS || (e == CONDITIONAL && isConditionallyEnabled(req));
	}

	/**
	 * Returns <jk>true</jk> if debugging is conditionally enabled on the specified request.
	 *
	 * <p>
	 * This method only gets called when the enablement value resolves to {@link Enablement#CONDITIONAL CONDITIONAL}.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own implementation.
	 * The default implementation is provided by {@link DebugEnablementBuilder#conditionalPredicate(Predicate)}
	 * which has a default predicate of <c><jv>x</jv> -> <js>"true"</js>.equalsIgnoreCase(<jv>x</jv>.getHeader(<js>"Debug"</js>)</c>.
	 *
	 * @param req The incoming HTTP request.
	 * @return <jk>true</jk> if debugging is conditionally enabled on the specified request.
	 */
	protected boolean isConditionallyEnabled(HttpServletRequest req) {
		return conditionalPredicate.test(req);
	}

	/**
	 * Returns the properties defined on this bean as a simple map for debugging purposes.
	 *
	 * <p>
	 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
	 *
	 * @return A new map containing this bean's properties.
	 */
	public OMap toMap() {
		return OMap
			.create()
			.filtered()
			.a("defaultEnablement", defaultEnablement)
			.a("enablementMap", enablementMap)
			.a("conditionalPredicate", conditionalPredicate);
	}

	@Override /* Object */
	public String toString() {
		return toMap().toString();
	}
}
