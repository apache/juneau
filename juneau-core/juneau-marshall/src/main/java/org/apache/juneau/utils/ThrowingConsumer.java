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
 * A subclass of {@link Consumer} that allows for thrown exceptions.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> the type of the input to the consumer.
 */
@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

	@Override
	default void accept(T t) {
		try {
			acceptThrows(t);
		} catch (Exception e) {
			throw asRuntimeException(e);
		}
	}

	/**
	 * The functional method to implement.
	 *
	 * @param t The type of the input to the consumer.
	 * @throws Exception Any exception.
	 */
	void acceptThrows(T t) throws Exception;
}
