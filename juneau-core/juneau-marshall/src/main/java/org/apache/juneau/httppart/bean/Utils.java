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
package org.apache.juneau.httppart.bean;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;

/**
 * Utility methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
class Utils {

	static void assertNoArgs(MethodInfo m, Class<?> a) throws InvalidAnnotationException {
		if (m.hasParams())
			throw new InvalidAnnotationException("Method with @{0} annotation cannot have arguments.  Method=''{1}''", a.getSimpleName(), m);
	}

	static void assertReturnNotVoid(MethodInfo m, Class<?> a) throws InvalidAnnotationException {
		ClassInfo rt = m.getReturnType();
		if (rt.is(void.class))
			throw new InvalidAnnotationException("Invalid return type for method with annotation @{0}.  Method=''{1}''", a.getSimpleName(), m);
	}

	static void assertReturnType(MethodInfo m, Class<? extends Annotation> a, Class<?>...c) throws InvalidAnnotationException {
		ClassInfo rt = m.getReturnType();
		for (Class<?> cc : c)
			if (rt.is(cc))
				return;
		throw new InvalidAnnotationException("Invalid return type for method with annotation @{0}.  Method=''{1}''", a.getSimpleName(), m);
	}

	static void assertArgType(MethodInfo m, Class<? extends Annotation> a, Class<?>...c) throws InvalidAnnotationException {
		List<Class<?>> ptt = m.getRawParamTypes();
		if (ptt.size() != 1)
			throw new InvalidAnnotationException("Only one parameter can be passed to method with @{0} annotation.  Method=''{0}''", a.getSimpleName(), m);
		Class<?> rt = ptt.get(0);
		for (Class<?> cc : c)
			if (rt == cc)
				return;
		throw new InvalidAnnotationException("Invalid return type for method with annotation @{0}.  Method=''{1}''", a.getSimpleName(), m);
	}
}
