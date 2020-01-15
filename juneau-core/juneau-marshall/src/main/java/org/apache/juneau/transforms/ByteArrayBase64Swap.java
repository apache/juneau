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

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.transform.*;

/**
 * Transforms <code><jk>byte</jk>[]</code> arrays to BASE-64 encoded {@link String Strings}.
 *
 * <div class='warn'>
 * 	<b>Deprecated</b> - Use {@link ByteArraySwap}
 * </div>
 */
@Deprecated
public class ByteArrayBase64Swap extends StringSwap<byte[]> {

	/**
	 * Converts the specified <code><jk>byte</jk>[]</code> to a {@link String}.
	 */
	@Override /* PojoSwap */
	public String swap(BeanSession session, byte[] b) throws Exception {
		return base64Encode(b);
	}

	/**
	 * Converts the specified {@link String} to a <code><jk>byte</jk>[]</code>.
	 */
	@Override /* PojoSwap */
	public byte[] unswap(BeanSession session, String s, ClassMeta<?> hint) throws Exception {
		return base64Decode(s);
	}
}
