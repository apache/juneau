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
package org.apache.juneau.swaps;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;

/**
 * Transforms beans into {@link String Strings} by simply calling the {@link Object#toString()} method.
 *
 * <p>
 * Allows you to specify classes that should just be converted to {@code Strings} instead of potentially
 * being turned into Maps by the {@link BeanContext} (or worse, throwing
 * {@link BeanRuntimeException BeanRuntimeExceptions}).
 *
 * <p>
 * This is usually a one-way transform.
 * Beans serialized as strings cannot be reconstituted using a parser unless it is a
 * <a class="doclink" href="../../../../index.html#jm.PojoCategories">parsable POJO</a>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 *
 * @param <T> The class type of the bean.
 */
public class BeanStringSwap<T> extends StringSwap<T> {

	/**
	 * Converts the specified bean to a {@link String}.
	 */
	@Override /* ObjectSwap */
	public String swap(BeanSession session, T o) {
		return o.toString();
	}
}
