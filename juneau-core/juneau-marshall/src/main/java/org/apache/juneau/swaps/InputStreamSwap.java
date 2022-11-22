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

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;

/**
 * Transforms <code>InputStreams</code> to {@link String Strings}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 */
public abstract class InputStreamSwap extends StringSwap<InputStream> {

	/**
	 * Converts {@link InputStream InputStreams} to BASE-64 encoding.
	 */
	public static class Base64 extends InputStreamSwap {
		/**
		 * Converts the specified {@link InputStream} to a {@link String}.
		 */
		@Override /* ObjectSwap */
		public String swap(BeanSession session, InputStream is) throws Exception {
			return base64Encode(toBytes(is));
		}

		/**
		 * Converts the specified {@link String} to an {@link InputStream}.
		 */
		@Override /* ObjectSwap */
		public InputStream unswap(BeanSession session, String s, ClassMeta<?> hint) throws Exception {
			return toStream(base64Decode(s), hint);
		}
	}

	/**
	 * Converts {@link InputStream InputStreams} to hex encoding.
	 */
	public static class Hex extends InputStreamSwap {
		/**
		 * Converts the specified {@link InputStream} to a {@link String}.
		 */
		@Override /* ObjectSwap */
		public String swap(BeanSession session, InputStream is) throws Exception {
			return toHex(toBytes(is));
		}

		/**
		 * Converts the specified {@link String} to an {@link InputStream}.
		 */
		@Override /* ObjectSwap */
		public InputStream unswap(BeanSession session, String s, ClassMeta<?> hint) throws Exception {
			return toStream(fromHex(s), hint);
		}
	}

	/**
	 * Converts {@link InputStream InputStreams} to spaced-hex encoding.
	 */
	public static class SpacedHex extends InputStreamSwap {
		/**
		 * Converts the specified {@link InputStream} to a {@link String}.
		 */
		@Override /* ObjectSwap */
		public String swap(BeanSession session, InputStream is) throws Exception {
			return toSpacedHex(toBytes(is));
		}

		/**
		 * Converts the specified {@link String} to an {@link InputStream}.
		 */
		@Override /* ObjectSwap */
		public InputStream unswap(BeanSession session, String s, ClassMeta<?> hint) throws Exception {
			return toStream(fromSpacedHex(s), hint);
		}
	}

	/**
	 * Convert the specified input stream to a byte array.
	 *
	 * @param is
	 * 	The input stream to convert to bytes.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>The stream is automatically closed.
	 * @return The byte array.
	 * @throws IOException Thrown by input stream.
	 */
	protected byte[] toBytes(InputStream is) throws IOException {
		return readBytes(is);
	}


	/**
	 * Convert the specified byte array into an input stream.
	 *
	 * @param b The byte array.
	 * @param hint Contains a hint about what subtype is being requested.
	 * @return The byte array.
	 */
	protected InputStream toStream(byte[] b, ClassMeta<?> hint) {
		Class<?> c = hint == null ? InputStream.class : hint.getInnerClass();
		if (c == InputStream.class || c == ByteArrayInputStream.class)
			return new ByteArrayInputStream(b);
		return null;
	}
}
