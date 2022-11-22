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
import org.apache.juneau.annotation.*;
import org.apache.juneau.swap.*;

/**
 * Built-in object swap implementation class for the {@link Beanp#format() @Beanp(format)} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 */
public class StringFormatSwap extends StringSwap<Object> {

	private final String format;

	/**
	 * Constructor.
	 *
	 * @param format The string format string.
	 */
	public StringFormatSwap(String format) {
		this.format = format;
	}

	@Override /* ObjectSwap */
	public String swap(BeanSession session, Object o) throws Exception {
		return String.format(format, o);
	}

	@Override /* ObjectSwap */
	public Object unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
		return session.convertToType(f, hint);
	}
}
