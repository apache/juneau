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

import static org.apache.juneau.common.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;

/**
 * Transforms <code><jk>byte</jk>[]</code> arrays to Strings.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 */
public abstract class ByteArraySwap extends StringSwap<byte[]> {

	/**
	 * Converts byte arrays to BASE-64 encoding.
	 */
	public static class Base64 extends ByteArraySwap {
		/**
		 * Converts the specified <code><jk>byte</jk>[]</code> to a {@link String}.
		 */
		@Override /* ObjectSwap */
		public String swap(BeanSession session, byte[] b) throws Exception {
			return base64Encode(b);
		}

		/**
		 * Converts the specified {@link String} to a <code><jk>byte</jk>[]</code>.
		 */
		@Override /* ObjectSwap */
		public byte[] unswap(BeanSession session, String s, ClassMeta<?> hint) throws Exception {
			return base64Decode(s);
		}
	}

	/**
	 * Converts byte arrays to hex encoding.
	 */
	public static class Hex extends ByteArraySwap {
		/**
		 * Converts the specified <code><jk>byte</jk>[]</code> to a {@link String}.
		 */
		@Override /* ObjectSwap */
		public String swap(BeanSession session, byte[] b) throws Exception {
			return toHex(b);
		}

		/**
		 * Converts the specified {@link String} to a <code><jk>byte</jk>[]</code>.
		 */
		@Override /* ObjectSwap */
		public byte[] unswap(BeanSession session, String s, ClassMeta<?> hint) throws Exception {
			return fromHex(s);
		}
	}

	/**
	 * Converts byte arrays to spaced-hex encoding.
	 */
	public static class SpacedHex extends ByteArraySwap {
		/**
		 * Converts the specified <code><jk>byte</jk>[]</code> to a {@link String}.
		 */
		@Override /* ObjectSwap */
		public String swap(BeanSession session, byte[] b) throws Exception {
			return toSpacedHex(b);
		}

		/**
		 * Converts the specified {@link String} to a <code><jk>byte</jk>[]</code>.
		 */
		@Override /* ObjectSwap */
		public byte[] unswap(BeanSession session, String s, ClassMeta<?> hint) throws Exception {
			return fromSpacedHex(s);
		}
	}
}
