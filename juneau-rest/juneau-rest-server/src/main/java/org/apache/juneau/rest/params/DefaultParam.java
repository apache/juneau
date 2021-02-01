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
package org.apache.juneau.rest.params;

import org.apache.juneau.cp.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters on {@link RestOp}-annotated Java methods by retrieving them by type from the REST object bean factory.
 *
 * <p>
 * The parameter value is resolved using <c><jv>call</jv>.{@link RestCall#getBeanFactory() getBeanFactory}().{@link BeanFactory#getBean(Class) getBean}(<jv>type</jv>)</c>
 * which resolves the object from the registered bean factory (e.g. Spring-injected beans available in the application).
 *
 * <p>
 * This is the default parameter resolver if no other applicable parameter resolvers could be found.
 */
public class DefaultParam implements RestOperationParam {

	private final Class<?> type;
	private final ParamInfo paramInfo;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link DefaultParam}, never <jk>null</jk>.
	 */
	public static DefaultParam create(ParamInfo paramInfo) {
		return new DefaultParam(paramInfo);
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 */
	protected DefaultParam(ParamInfo paramInfo) {
		this.type = paramInfo.getParameterType().inner();
		this.paramInfo = paramInfo;
	}

	@Override /* RestOperationParam */
	public Object resolve(RestCall call) throws Exception {
		return call.getBeanFactory().getBean(type).orElseThrow(()->new ParameterException(paramInfo, "Could not resolve bean type {0}", type.getName()));
	}
}
