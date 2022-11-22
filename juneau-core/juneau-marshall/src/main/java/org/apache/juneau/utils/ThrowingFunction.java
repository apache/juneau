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
package org.apache.juneau.utils;

import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.util.function.*;

/**
 * A subclass of {@link Function} that allows for thrown exceptions.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> the type of the input to the function.
 * @param <R> the type of the result of the function.
 */
@FunctionalInterface
public interface ThrowingFunction<T,R> extends Function<T,R> {

	@Override
	default R apply(T t) {
		try {
			return applyThrows(t);
		} catch (Exception e) {
			throw asRuntimeException(e);
		}
	}

	/**
	 * The functional method to implement.
	 *
	 * @param t The type of the input to the function.
	 * @return The type of the result of the function.
	 * @throws Exception Any exception.
	 */
	R applyThrows(T t) throws Exception;
}
