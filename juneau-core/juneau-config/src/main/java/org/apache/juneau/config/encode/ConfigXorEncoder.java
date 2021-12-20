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
package org.apache.juneau.config.encode;

import static org.apache.juneau.internal.StringUtils.*;

import static org.apache.juneau.internal.IOUtils.*;

/**
 * Simply XOR+Base64 encoder for obscuring passwords and other sensitive data in INI config files.
 *
 * <p>
 * This is not intended to be used as strong encryption.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jc.EncodedEntries}
 * </ul>
 */
public final class ConfigXorEncoder implements ConfigEncoder {

	/** Reusable XOR-ConfigEncoder instance. */
	public static final ConfigXorEncoder INSTANCE = new ConfigXorEncoder();

	private static final String key = System.getProperty("org.apache.juneau.config.XorEncoder.key",
		"nuy7og796Vh6G9O6bG230SHK0cc8QYkH");	// The super-duper-secret key

	@Override /* ConfigEncoder */
	public String encode(String fieldName, String in) {
		byte[] b = in.getBytes(UTF8);
		for (int i = 0; i < b.length; i++) {
				int j = i % key.length();
			b[i] = (byte)(b[i] ^ key.charAt(j));
		}
		return '{' + base64Encode(b) + '}';
	}

	@Override /* ConfigEncoder */
	public String decode(String fieldName, String in) {
		if (! isEncoded(in))
			return in;
		in = in.substring(1, in.length()-1);
		byte[] b = base64Decode(in);
		for (int i = 0; i < b.length; i++) {
			int j = i % key.length();
			b[i] = (byte)(b[i] ^ key.charAt(j));
	}
		return new String(b, UTF8);
	}

	@Override /* ConfigEncoder */
	public boolean isEncoded(String in) {
		return in != null && in.length() > 1 && in.charAt(0) == '{' && in.charAt(in.length()-1) == '}';
	}
}
