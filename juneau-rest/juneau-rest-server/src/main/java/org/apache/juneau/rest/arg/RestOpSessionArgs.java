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
package org.apache.juneau.rest.arg;

import org.apache.juneau.cp.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Resolves method parameters on {@link RestOp}-annotated Java methods of types found on the {@link RestOpSession} object.
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link BeanStore}
 * 	<li class='jc'>{@link RestOpSession}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class RestOpSessionArgs extends SimpleRestOperationArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new arg, or <jk>null</jk> if the parameter type is not one of the supported types.
	 */
	public static RestOpSessionArgs create(ParamInfo paramInfo) {
		if (paramInfo.isType(BeanStore.class))
			return new RestOpSessionArgs(x->x.getBeanStore());
		if (paramInfo.isType(RestOpSession.class))
			return new RestOpSessionArgs(x->x);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param <T> The function return type.
	 * @param function The function for finding the arg.
	 */
	protected <T> RestOpSessionArgs(ThrowingFunction<RestOpSession,T> function) {
		super((session)->function.apply(session));
	}
}
