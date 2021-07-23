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
package org.apache.juneau.testutils;

import java.io.*;

/**
 * Various stream convenience methods.
 */
public class StreamUtils {

	/**
	 * Creates an input stream from the specified string.
	 *
	 * @param in The contents of the reader.
	 * @return A new input stream.
	 */
	public static final ByteArrayInputStream inputStream(String in) {
		return new ByteArrayInputStream(in.getBytes());
	}

	/**
	 * Creates a reader from the specified string.
	 *
	 * @param in The contents of the reader.
	 * @return A new reader.
	 */
	public static final StringReader reader(String in) {
		return new StringReader(in);
	}

	/**
	 * Returns a capturing print stream.
	 *
	 * <p>
	 * Print stream contents can be retrieved by calling {@link Object#toString()} on the object.
	 *
	 * @return A new print stream.
	 */
	public static final PrintStream printStream() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		return new PrintStream(baos, true) {
			@Override
			public String toString() {
				return baos.toString();
			}
		};
	}
}
