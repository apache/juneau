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

/**
 * Represents a encoder and encoding that matches an HTTP <c>Accept-Encoding</c> header value.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Encoders">Encoders</a>
 * </ul>
 */
public final class EncoderMatch {

	private final String encoding;
	private final Encoder encoder;

	EncoderMatch(String encoding, Encoder encoder) {
		this.encoding = encoding;
		this.encoder = encoder;
	}

	/**
	 * Returns the encoding of the encoder that matched the HTTP <c>Accept-Encoding</c> header value.
	 *
	 * @return The encoding of the match.
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Returns the encoder that matched the HTTP <c>Accept-Encoding</c> header value.
	 *
	 * @return The encoder of the match.
	 */
	public Encoder getEncoder() {
		return encoder;
	}
}
