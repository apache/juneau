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
package org.apache.juneau;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Same as {@link BeanMeta}, except the list of bean properties are limited by a  {@link Beanp#properties() @Beanp(properties)} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The class type that this metadata applies to.
 */
public final class BeanMetaFiltered<T> extends BeanMeta<T> {

	/**
	 * Wrapper constructor.
	 *
	 * @param innerMeta The untransformed bean meta of the bean property.
	 * @param pNames The list of transformed property names.
	 */
	public BeanMetaFiltered(BeanMeta<T> innerMeta, String[] pNames) {
		super(innerMeta.classMeta, innerMeta.ctx, innerMeta.beanFilter, pNames, null);
	}

	/**
	 * Wrapper constructor.
	 *
	 * @param innerMeta The untransformed bean meta of the bean property.
	 * @param pNames The list of transformed property names.
	 */
	public BeanMetaFiltered(BeanMeta<T> innerMeta, Collection<String> pNames) {
		this(innerMeta, pNames.toArray(new String[pNames.size()]));
	}
}
