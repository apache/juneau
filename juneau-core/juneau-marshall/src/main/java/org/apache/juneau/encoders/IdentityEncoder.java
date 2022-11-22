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
package org.apache.juneau.encoders;

import java.io.*;

/**
 * Encoder for handling <js>"identity"</js> encoding and decoding.
 *
 * <p>
 * Identity encoding is just another name for no encoding at all.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Encoders">Encoders</a>
 * </ul>
 */
public class IdentityEncoder extends Encoder {

	/** Singleton */
	public static final IdentityEncoder INSTANCE = new IdentityEncoder();

	/** Constructor. */
	public IdentityEncoder() {}

	@Override /* Encoder */
	public InputStream getInputStream(InputStream is) throws IOException {
		return is;
	}

	@Override /* Encoder */
	public OutputStream getOutputStream(OutputStream os) throws IOException {
		return os;
	}

	/**
	 * Returns <code>[<js>"identity"</js>]</code>.
	 */
	@Override /* Encoder */
	public String[] getCodings() {
		return new String[]{"identity"};
	}
}
