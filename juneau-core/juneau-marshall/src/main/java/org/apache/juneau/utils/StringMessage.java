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
package org.apache.juneau.utils;

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.text.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;

/**
 * An encapsulated MessageFormat-style string and arguments.
 *
 * <p>
 * Useful for delayed serialization of arguments for logging.
 * Message string will not be constructed until the <code>toString()</code> method is called.
 */
public class StringMessage implements CharSequence, Writable {

	private final String pattern;
	private final Object[] args;
	private String results;

	/**
	 * Constructor.
	 *
	 * @param pattern {@link MessageFormat}-style pattern.
	 * @param args Message arguments.
	 */
	public StringMessage(String pattern, Object...args) {
		this.pattern = pattern;
		this.args = args;
	}

	@Override /* Writable */
	public void writeTo(Writer w) throws IOException {
		w.write(toString());

	}

	@Override /* Writable */
	public MediaType getMediaType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override /* CharSequence */
	public char charAt(int index) {
		return toString().charAt(index);
	}

	@Override /* CharSequence */
	public int length() {
		return toString().length();
	}

	@Override /* CharSequence */
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	@Override /* Object */
	public String toString() {
		if (results == null)
			results = format(pattern, args);
		return results;
	}
}
