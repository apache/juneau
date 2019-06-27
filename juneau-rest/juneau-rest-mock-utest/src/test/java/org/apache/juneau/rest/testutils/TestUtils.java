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
package org.apache.juneau.rest.testutils;

import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.zip.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock2.*;

@SuppressWarnings({"javadoc"})
public class TestUtils extends org.apache.juneau.testutils.TestUtils {

	/**
	 * Converts string into a GZipped input stream.
	 *
	 * @param contents The contents to compress.
	 * @return The input stream converted to GZip.
	 * @throws Exception
	 */
	public static final byte[] compress(String contents) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(contents.length()>>1);
		try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
			gos.write(contents.getBytes());
			gos.finish();
			gos.flush();
		}
		return baos.toByteArray();
	}

	/**
	 * Converts a GZipped input stream into a string.
	 *
	 * @param is The contents to decompress.
	 * @return The string.
	 * @throws Exception
	 */
	public static final String decompress(byte[] is) throws Exception {
		return read(new GZIPInputStream(new ByteArrayInputStream(is)));
	}

	/**
	 * Gets the swagger for the specified @Resource-annotated object.
	 * @param c
	 * @return
	 */
	public static Swagger getSwagger(Class<?> c) {
		try {
			RestContext rc = RestContext.create(c.newInstance()).build();
			RestRequest req = rc.getCallHandler().createRequest(new MockServletRequest());
			RestInfoProvider ip = rc.getInfoProvider();
			return ip.getSwagger(req);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static final void dumpResponse(String r, String msg, Object...args) {
		System.err.println("*** Failure ****************************************************************************************"); // NOT DEBUG
		System.err.println(format(msg, args));  // NOT DEBUG
		System.err.println("*** Response-Start *********************************************************************************"); // NOT DEBUG
		System.err.println(r); // NOT DEBUG
		System.err.println("*** Response-End ***********************************************************************************"); // NOT DEBUG
	}
}
