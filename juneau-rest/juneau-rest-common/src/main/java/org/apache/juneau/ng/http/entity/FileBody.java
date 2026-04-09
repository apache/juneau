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
package org.apache.juneau.ng.http.entity;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.ng.http.*;

/**
 * A repeatable {@link HttpBody} that streams content from a {@link File}.
 *
 * <p>
 * {@link #getContentLength()} returns the file size, allowing transports to set {@code Content-Length}.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * @since 9.2.1
 */
public final class FileBody implements HttpBody {

	private final File file;
	private final String contentType;

	private FileBody(File file, String contentType) {
		this.file = assertArgNotNull("file", file);
		this.contentType = contentType;
	}

	/**
	 * Creates a {@link FileBody} with {@code application/octet-stream} content type.
	 *
	 * @param file The file. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static FileBody of(File file) {
		return new FileBody(file, "application/octet-stream");
	}

	/**
	 * Creates a {@link FileBody} with the given content type.
	 *
	 * @param file The file. Must not be <jk>null</jk>.
	 * @param contentType The MIME content type. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static FileBody of(File file, String contentType) {
		return new FileBody(file, contentType);
	}

	/**
	 * Returns the underlying file.
	 *
	 * @return The file. Never <jk>null</jk>.
	 */
	public File getFile() {
		return file;
	}

	@Override /* HttpBody */
	public String getContentType() {
		return contentType;
	}

	@Override /* HttpBody */
	public long getContentLength() {
		return file.length();
	}

	@Override /* HttpBody */
	public void writeTo(OutputStream out) throws IOException {
		Files.copy(file.toPath(), out);
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return true;
	}
}
