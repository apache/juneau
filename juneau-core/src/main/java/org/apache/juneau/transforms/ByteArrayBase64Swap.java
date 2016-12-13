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

import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Transforms <code><jk>byte</jk>[]</code> arrays to BASE-64 encoded {@link String Strings}.
 */
public class ByteArrayBase64Swap extends PojoSwap<byte[],String> {

	/**
	 * Converts the specified <code><jk>byte</jk>[]</code> to a {@link String}.
	 */
	@Override /* PojoSwap */
	public String swap(byte[] b) throws SerializeException {
		try {
			return StringUtils.base64Encode(b);
		} catch (Exception e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Converts the specified {@link String} to a <code><jk>byte</jk>[]</code>.
	 */
	@Override /* PojoSwap */
	public byte[] unswap(String s) throws ParseException {
		try {
			return StringUtils.base64Decode(s);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
