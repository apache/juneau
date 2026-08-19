/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.utils;

import java.util.zip.*;

/**
 * Content-checksum utility methods.
 */
public class ChecksumUtils {

	/** Prevents instantiation. */
	private ChecksumUtils() {}

	/**
	 * Formats a CRC32 checksum of the specified bytes as a zero-padded, lower-case, 8-hex-char content hash.
	 *
	 * <p>
	 * Suitable as a compact, content-sensitive cache-buster for classpath-shipped assets (JS/CSS/images, etc.):
	 * the hash changes the instant the underlying bytes do, without relying on a build/version string alone
	 * (which stays stable across {@code -SNAPSHOT} dev rebuilds and so cannot bust a browser's cache on its own).
	 *
	 * @param bytes The bytes to checksum.  Must not be <jk>null</jk>.
	 * @return The zero-padded 8-hex-char CRC32 checksum (e.g. {@code "1a2b3c4d"}).
	 */
	public static String hash8(byte[] bytes) {
		var crc = new CRC32();
		crc.update(bytes);
		return String.format("%08x", crc.getValue());
	}
}
