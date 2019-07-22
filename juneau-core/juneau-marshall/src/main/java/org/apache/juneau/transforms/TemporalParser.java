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
package org.apache.juneau.transforms;

import java.lang.reflect.*;
import java.time.format.*;
import java.time.temporal.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;

/**
 * A helper class for converting strings to {@link Temporal} objects.
 *
 * <p>
 * Looks for static <cb>from()</cb> or <cb>parse()</cb> methods to use on the temporal class to instantiate instances.
 *
 * @param <T> The temporal class type.
 */
class TemporalParser<T extends Temporal> {

	private final Method fromMethod;
	private final Method parseMethod;
	private final ClassInfo ci;

	/**
	 * Constructor.
	 *
	 * @param c The temporal class type.
	 */
	public TemporalParser(Class<T> c) {
		this.ci = ClassInfo.of(c);
		this.fromMethod = ci.getStaticPublicMethodInner("from", c, TemporalAccessor.class);
		this.parseMethod = ci.getStaticPublicMethodInner("parse", c, String.class, DateTimeFormatter.class);
	}

	/**
	 * Parses the specified input using the specified formatter.
	 *
	 * @param input The input string.
	 * @param formatter The formatter.
	 * @return The parsed string.
	 * @throws ExecutableException If input could not be parsed using the specified formatter.
	 */
	@SuppressWarnings("unchecked")
	public T parse(String input, DateTimeFormatter formatter) throws ExecutableException {
		try {
			if (parseMethod != null) {
				return (T)parseMethod.invoke(null, input, formatter);
			}
			if (fromMethod != null) {
				TemporalAccessor ta = formatter.parse(input);
				return (T)fromMethod.invoke(null, ta);
			}
			throw new ExecutableException("From or Parse methods not found on temporal class ''{0}''", ci.getSimpleName());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new ExecutableException(e);
		}
	}
}
