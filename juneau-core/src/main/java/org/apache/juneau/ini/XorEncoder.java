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
package org.apache.juneau.ini;

import org.apache.juneau.internal.*;

/**
 * Simply XOR+Base64 encoder for obscuring passwords and other sensitive data in INI config files.
 * <p>
 * This is not intended to be used as strong encryption.
 */
public final class XorEncoder implements Encoder {

	/** Reusable XOR-Encoder instance. */
	public static final XorEncoder INSTANCE = new XorEncoder();

	private static final String key = System.getProperty("org.apache.juneau.ini.XorEncoder.key", "nuy7og796Vh6G9O6bG230SHK0cc8QYkH");	// The super-duper-secret key

	@Override /* Encoder */
	public String encode(String fieldName, String in) {
		byte[] b = in.getBytes(IOUtils.UTF8);
		for (int i = 0; i < b.length; i++) {
				int j = i % key.length();
			b[i] = (byte)(b[i] ^ key.charAt(j));
		}
		return StringUtils.base64Encode(b);
	}

	@Override /* Encoder */
	public String decode(String fieldName, String in) {
		byte[] b = StringUtils.base64Decode(in);
		for (int i = 0; i < b.length; i++) {
			int j = i % key.length();
			b[i] = (byte)(b[i] ^ key.charAt(j));
	}
		return new String(b, IOUtils.UTF8);
	}
}
