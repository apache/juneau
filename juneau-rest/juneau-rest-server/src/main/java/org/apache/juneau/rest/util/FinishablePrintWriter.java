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
package org.apache.juneau.rest.util;

import java.io.*;

import org.apache.juneau.encoders.*;

/**
 * A wrapped {@link PrintWriter} with an added <c>finish()</c> method.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class FinishablePrintWriter extends PrintWriter implements Finishable {

	final Finishable f;

	/**
	 * Constructor.
	 *
	 * @param out The wrapped output stream.
	 * @param characterEncoding The character encoding of the output stream.
	 * @param autoFlush Automatically flush after every println.
	 * @throws IOException Thrown by underlying stream.
	 */
	public FinishablePrintWriter(OutputStream out, String characterEncoding, boolean autoFlush) throws IOException {
		super(new OutputStreamWriter(out, characterEncoding), autoFlush);
		f = (out instanceof Finishable ? (Finishable)out : null);
	}

	/**
	 * Calls {@link Finishable#finish()} on the underlying output stream.
	 *
	 * <p>
	 * A no-op if the underlying output stream does not implement the {@link Finishable} interface.
	 */
	@Override /* Finishable */
	public void finish() throws IOException {
		if (f != null)
			f.finish();
	}
}
