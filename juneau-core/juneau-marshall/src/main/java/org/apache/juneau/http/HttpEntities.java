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
package org.apache.juneau.http;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;

/**
 * Standard predefined HTTP entities.
 */
public class HttpEntities {

	/**
	 * Creates a new {@link StringEntity} object.
	 *
	 * <p>
	 * Assumes {@link ContentType#TEXT_PLAIN TEXT/PLAIN} content type and <js>"UTF-8"</js> encoding.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringEntity} object.
	 */
	public static final StringEntity stringEntity(String content) {
		return StringEntity.of(content);
	}

	/**
	 * Creates a new {@link StringEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or {@link ContentType#TEXT_PLAIN} if not specified.
	 * @param charset The content character encoding, or <js>"UTF-8"</js> if not specified.
	 * @return A new {@link StringEntity} object.
	 */
	public static final StringEntity stringEntity(String content, ContentType contentType, Charset charset) {
		return StringEntity.of(content, contentType, charset);
	}

	/**
	 * Creates a new {@link InputStreamEntity} object.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link InputStreamEntity} object.
	 */
	public static final InputStreamEntity streamEntity(InputStream content) {
		return InputStreamEntity.of(content);
	}

	/**
	 * Creates a new {@link InputStreamEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link InputStreamEntity} object.
	 */
	public static final InputStreamEntity streamEntity(InputStream content, long length, ContentType contentType) {
		return InputStreamEntity.of(content, length, contentType);
	}
}
