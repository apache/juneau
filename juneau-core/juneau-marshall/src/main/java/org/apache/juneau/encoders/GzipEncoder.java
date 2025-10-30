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
package org.apache.juneau.encoders;

import static org.apache.juneau.common.utils.CollectionUtils.*;

import java.io.*;
import java.util.zip.*;

/**
 * Encoder for handling <js>"gzip"</js> encoding and decoding.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerEncoders">Encoders</a>

 * </ul>
 */
public class GzipEncoder extends Encoder {

	private static class FinishableGZIPOutputStream extends GZIPOutputStream implements Finishable {
		FinishableGZIPOutputStream(OutputStream out) throws IOException {
			super(out);
		}
	}

	/**
	 * Returns <code>[<js>"gzip"</js>]</code>.
	 */
	@Override /* Overridden from Encoder */
	public String[] getCodings() { return a("gzip"); }

	@Override /* Overridden from Encoder */
	public InputStream getInputStream(InputStream is) throws IOException {
		return new GZIPInputStream(is);
	}

	@Override /* Overridden from Encoder */
	public OutputStream getOutputStream(OutputStream os) throws IOException {
		return new FinishableGZIPOutputStream(os);
	}
}