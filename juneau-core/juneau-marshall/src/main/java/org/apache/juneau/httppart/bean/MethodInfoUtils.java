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
package org.apache.juneau.httppart.bean;

import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.annotation.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.reflect.*;

/**
 * Utility methods.
 *
 */
class MethodInfoUtils {

	static void assertArgType(MethodInfo m, Class<? extends Annotation> a, Class<?>...c) throws InvalidAnnotationException {
		var params = m.getParameters();
		if (params.size() != 1)
			throw new InvalidAnnotationException("Only one parameter can be passed to method with @{0} annotation.  Method=''{0}''", cns(a), m);
		var rt = params.get(0).getParameterType().inner();
		for (var cc : c)
			if (rt == cc)
				return;
		throw new InvalidAnnotationException("Invalid return type for method with annotation @{0}.  Method=''{1}''", cns(a), m);
	}

	static void assertNoArgs(MethodInfo m, Class<?> a) throws InvalidAnnotationException {
		if (m.hasParameters())
			throw new InvalidAnnotationException("Method with @{0} annotation cannot have arguments.  Method=''{1}''", cns(a), m);
	}

	static void assertReturnNotVoid(MethodInfo m, Class<?> a) throws InvalidAnnotationException {
		var rt = m.getReturnType();
		if (rt.is(void.class))
			throw new InvalidAnnotationException("Invalid return type for method with annotation @{0}.  Method=''{1}''", cns(a), m);
	}

	static void assertReturnType(MethodInfo m, Class<? extends Annotation> a, Class<?>...c) throws InvalidAnnotationException {
		var rt = m.getReturnType();
		for (var cc : c)
			if (rt.is(cc))
				return;
		throw new InvalidAnnotationException("Invalid return type for method with annotation @{0}.  Method=''{1}''", cns(a), m);
	}
}