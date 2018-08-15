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

import static org.apache.juneau.internal.ClassUtils.*;

import java.beans.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.annotation.*;

/**
 * Utility methods.
 */
class Utils {

	@SafeVarargs
	static final void assertNoAnnotations(Method m, Class<? extends Annotation> a, Class<? extends Annotation>...c) throws InvalidAnnotationException {
		for (Class<? extends Annotation> cc : c) {
			if (hasAnnotation(cc, m))
				throw new InvalidAnnotationException("@{0} annotation cannot be used in a @{1} bean.  Method=''{2}''", cc.getSimpleName(), a.getSimpleName(), m);
		}
	}

	static void assertNoArgs(Method m, Class<?> a) throws InvalidAnnotationException {
		if (m.getParameterTypes().length != 0)
			throw new InvalidAnnotationException("Method with @{0} annotation cannot have arguments.  Method=''{1}''", a.getSimpleName(), m);
	}

	static void assertReturnNotVoid(Method m, Class<?> a) throws InvalidAnnotationException {
		Class<?> rt = m.getReturnType();
		if (rt == void.class)
			throw new InvalidAnnotationException("Invalid return type for method with annotation @{0}.  Method=''{1}''", a.getSimpleName(), m);
	}

	static void assertReturnType(Method m, Class<? extends Annotation> a, Class<?>...c) throws InvalidAnnotationException {
		Class<?> rt = m.getReturnType();
		for (Class<?> cc : c)
			if (rt == cc)
				return;
		throw new InvalidAnnotationException("Invalid return type for method with annotation @{0}.  Method=''{1}''", a.getSimpleName(), m);
	}

	static void assertArgType(Method m, Class<? extends Annotation> a, Class<?>...c) throws InvalidAnnotationException {
		Class<?>[] ptt = m.getParameterTypes();
		if (ptt.length != 1)
			throw new InvalidAnnotationException("Only one parameter can be passed to method with @{0} annotation.  Method=''{0}''", a.getSimpleName(), m);
		Class<?> rt = ptt[0];
		for (Class<?> cc : c)
			if (rt == cc)
				return;
		throw new InvalidAnnotationException("Invalid return type for method with annotation @{0}.  Method=''{1}''", a.getSimpleName(), m);
	}

	static String getPropertyName(Method m) {
		String n = m.getName();
		if (n.startsWith("get") && n.length() > 3)
			return Introspector.decapitalize(n.substring(3));
		if (n.startsWith("is") && n.length() > 2)
			return Introspector.decapitalize(n.substring(2));
		return n;
	}
}
