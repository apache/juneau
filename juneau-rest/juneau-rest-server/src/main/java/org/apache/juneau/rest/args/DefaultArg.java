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
package org.apache.juneau.rest.args;

import org.apache.juneau.cp.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters on {@link RestOp}-annotated Java methods by retrieving them by type from the REST object bean store.
 *
 * <p>
 * The parameter value is resolved using <c><jv>call</jv>.{@link RestCall#getBeanStore() getBeanStore}().{@link BeanStore#getBean(Class) getBean}(<jv>type</jv>)</c>
 * which resolves the object from the registered bean store (e.g. Spring-injected beans available in the application).
 *
 * <p>
 * This is the default parameter resolver if no other applicable parameter resolvers could be found.
 */
public class DefaultArg implements RestOpArg {

	private final Class<?> type;
	private final ParamInfo paramInfo;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link DefaultArg}, never <jk>null</jk>.
	 */
	public static DefaultArg create(ParamInfo paramInfo) {
		return new DefaultArg(paramInfo);
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 */
	protected DefaultArg(ParamInfo paramInfo) {
		this.type = paramInfo.getParameterType().inner();
		this.paramInfo = paramInfo;
	}

	@Override /* RestOpArg */
	public Object resolve(RestCall call) throws Exception {
		return call.getBeanStore().getBean(type).orElseThrow(()->new ArgException(paramInfo, "Could not resolve bean type {0}", type.getName()));
	}
}
