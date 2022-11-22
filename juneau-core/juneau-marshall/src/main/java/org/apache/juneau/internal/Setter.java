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
package org.apache.juneau.internal;

import java.lang.reflect.*;

import org.apache.juneau.*;

/**
 * Encapsulate a bean setter method that may be a method or field.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public interface Setter {

	/**
	 * Call the setter on the specified object.
	 *
	 * @param object The object to call the setter on
	 * @param value The value to set.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	void set(Object object, Object value) throws ExecutableException;

	/**
	 * Field setter
	 */
	static class FieldSetter implements Setter {

		private final Field f;

		public FieldSetter(Field f) {
			this.f = f;
		}

		@Override /* Setter */
		public void set(Object object, Object value) throws ExecutableException {
			try {
				f.set(object, value);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new ExecutableException(e);
			}
		}
	}

	/**
	 * Method setter
	 */
	static class MethodSetter implements Setter {

		private final Method m;

		public MethodSetter(Method m) {
			this.m = m;
		}

		@Override /* Setter */
		public void set(Object object, Object value) throws ExecutableException {
			try {
				m.invoke(object, value);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				throw new ExecutableException(e);
			}
		}
	}
}
