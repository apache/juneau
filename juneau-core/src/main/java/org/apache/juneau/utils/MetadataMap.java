/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/

package org.apache.juneau.utils;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Utility class for quick lookup of class metadata instances.
 * <p>
 * Class instances are created once and then cached.
 * <p>
 * Classes must have a constructor that takes in a single argument.
 *
 * @author james.bognar
 */
public class MetadataMap {

	private Class<?>[] classes = new Class<?>[0];
	private Object[] metadata = new Object[0];


	/**
	 * Constructor.
	 *
	 * @param c The metadata class to create.
	 * @param constructorArg The argument needed to construct the metadata.
	 * @return The cached metadata object.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> c, Object constructorArg) {
		for (int i = 0; i < classes.length; i++)
			if (classes[i] == c)
				return (T)metadata[i];
		synchronized(this) {
			for (int i = 0; i < classes.length; i++)
				if (classes[i] == c)
					return (T)metadata[i];
			Class<?>[] classes2 = new Class<?>[classes.length + 1];
			Object[] metadata2 = new Object[classes.length + 1];
			for (int i = 0; i < classes.length; i++) {
				classes2[i] = classes[i];
				metadata2[i] = metadata[i];
			}
			Object o = null;
			try {
				for (Constructor<?> con : c.getConstructors()) {
					Class<?>[] params = con.getParameterTypes();
					if (params.length == 1 && ClassUtils.isParentClass(params[0], constructorArg.getClass())) {
						o = con.newInstance(constructorArg);
						break;
					}
				}
			} catch (InvocationTargetException e) {
				Throwable t = e.getTargetException();
				if (t instanceof RuntimeException)
					throw (RuntimeException)t;
				throw new RuntimeException(t);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if (o == null)
				throw new BeanRuntimeException(c, "Could not find a constructor on class with a parameter to handle type {0}", constructorArg.getClass());
			classes2[classes.length] = c;
			metadata2[classes.length] = o;
			classes = classes2;
			metadata = metadata2;
			return (T)o;
		}
	}
}
