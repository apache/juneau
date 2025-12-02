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

import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.stats.*;

/**
 * Resolves method parameters on {@link RestOp}-annotated Java methods of types found on the {@link RestContext} object.
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link BeanContext}
 * 	<li class='jc'>{@link Config}
 * 	<li class='jc'>{@link DebugEnablement}
 * 	<li class='jc'>{@link EncoderSet}
 * 	<li class='jc'>{@link FileFinder}
 * 	<li class='jc'>{@link Logger}
 * 	<li class='jc'>{@link MethodExecStore}
 * 	<li class='jc'>{@link RestChildren}
 * 	<li class='jc'>{@link RestContext}
 * 	<li class='jc'>{@link RestContextStats}
 * 	<li class='jc'>{@link CallLogger}
 * 	<li class='jc'>{@link RestOperations}
 * 	<li class='jc'>{@link StaticFiles}
 * 	<li class='jc'>{@link ThrownStore}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class RestContextArgs extends SimpleRestOperationArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new arg, or <jk>null</jk> if the parameter type is not one of the supported types.
	 */
	public static RestContextArgs create(ParameterInfo paramInfo) {
		if (paramInfo.isType(BeanContext.class))
			return new RestContextArgs(RestContext::getBeanContext);
		if (paramInfo.isType(Config.class))
			return new RestContextArgs(RestContext::getConfig);
		if (paramInfo.isType(DebugEnablement.class))
			return new RestContextArgs(RestContext::getDebugEnablement);
		if (paramInfo.isType(EncoderSet.class))
			return new RestContextArgs(RestContext::getEncoders);
		if (paramInfo.isType(Logger.class))
			return new RestContextArgs(RestContext::getLogger);
		if (paramInfo.isType(MethodExecStore.class))
			return new RestContextArgs(RestContext::getMethodExecStore);
		if (paramInfo.isType(RestChildren.class))
			return new RestContextArgs(RestContext::getRestChildren);
		if (paramInfo.isType(RestContext.class))
			return new RestContextArgs(x -> x);
		if (paramInfo.isType(RestContextStats.class))
			return new RestContextArgs(RestContext::getStats);
		if (paramInfo.isType(CallLogger.class))
			return new RestContextArgs(RestContext::getCallLogger);
		if (paramInfo.isType(RestOperations.class))
			return new RestContextArgs(RestContext::getRestOperations);
		if (paramInfo.isType(StaticFiles.class))
			return new RestContextArgs(RestContext::getStaticFiles);
		if (paramInfo.isType(ThrownStore.class))
			return new RestContextArgs(RestContext::getThrownStore);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param <T> The function return type.
	 * @param function The function for finding the arg.
	 */
	protected <T> RestContextArgs(ThrowingFunction<RestContext,T> function) {
		super(session -> function.apply(session.getRestContext()));
	}
}