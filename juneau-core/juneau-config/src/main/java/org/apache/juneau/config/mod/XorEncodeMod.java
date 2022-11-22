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
package org.apache.juneau.config.mod;

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;

/**
 * Simply XOR+Base64 encoder for obscuring passwords and other sensitive data in INI config files.
 *
 * <p>
 * This is not intended to be used as strong encryption.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jc.ModdedEntries">Overview &gt; juneau-config &gt; Modded/Encoded Entries</a>
 * </ul>
 */
public class XorEncodeMod extends Mod {

	/** Reusable XOR-ConfigEncoder instance. */
	public static final XorEncodeMod INSTANCE = new XorEncodeMod();

	private static final String KEY = System.getProperty("org.apache.juneau.config.XorEncoder.key",
		"nuy7og796Vh6G9O6bG230SHK0cc8QYkH");	// The super-duper-secret key

	/**
	 * Constructor.
	 */
	public XorEncodeMod() {
		super('*', null, null, null);
	}

	@Override
	public String apply(String value) {
		byte[] b = value.getBytes(UTF8);
		for (int i = 0; i < b.length; i++) {
				int j = i % KEY.length();
			b[i] = (byte)(b[i] ^ KEY.charAt(j));
		}
		return "{" + base64Encode(b) + "}";
	}

	@Override
	public String remove(String value) {
		value = value.trim();
		value = value.substring(1, value.length()-1);
		byte[] b = base64Decode(value);
		for (int i = 0; i < b.length; i++) {
			int j = i % KEY.length();
			b[i] = (byte)(b[i] ^ KEY.charAt(j));
		}
		return new String(b, UTF8);
	}

	@Override
	public boolean isApplied(String value) {
		return startsWith(value, '{') && endsWith(value, '}');
	}
}
