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

	/**
	 * Constructor.
	 *
	 * @param supplier The supplier of the bean.
	 */
	public ResourceSupplier(Supplier<?> supplier) {
		this.supplier = supplier;
	}

	@Override
	public Object get() {
		return supplier.get();
	}
}
