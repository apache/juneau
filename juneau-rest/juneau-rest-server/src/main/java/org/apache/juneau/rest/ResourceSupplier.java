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
package org.apache.juneau.rest;

import java.util.function.*;

/**
 * A supplier of a REST resource bean.
 *
 * <p>
 * Pretty much just a normal supplier, but wrapped in a concrete class so that it can be retrieved by class name.
 */
public class ResourceSupplier implements Supplier<Object> {

	private final Supplier<?> supplier;
	private final Class<?> resourceClass;

	/**
	 * Constructor.
	 *
	 * @param resourceClass The resource class.
	 * 	<br>May or may not be the same as the object returned by the supplier (e.g. supplier returns a proxy).
	 * @param supplier The supplier of the bean.
	 */
	public ResourceSupplier(Class<?> resourceClass, Supplier<?> supplier) {
		this.resourceClass = resourceClass;
		this.supplier = supplier;
	}

	/**
	 * Returns the resource class.
	 *
	 * <p>
	 * May or may not be the same as the object returned by the supplier (e.g. supplier returns a proxy).
	 *
	 * @return The resource class.
	 */
	public Class<?> getResourceClass() {
		return resourceClass;
	}

	@Override
	public Object get() {
		return supplier.get();
	}
}
