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
package org.apache.juneau.common.internal;

import java.io.*;
import java.nio.charset.*;

/**
 * Utility class for creating {@link FileReader} objects.
 */
public final class FileReaderBuilder {

	private File file;
	private Charset cs = Charset.defaultCharset();
	private boolean allowNoFile;

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static FileReaderBuilder create() {
		return new FileReaderBuilder();
	}

	/**
	 * Creates a new builder initialized with the specified file.
	 *
	 * @param file The file being written to.
	 * @return A new builder.
	 */
	public static FileReaderBuilder create(File file) {
		return new FileReaderBuilder().file(file);
	}

	/**
	 * Sets the file being written from.
	 *
	 * @param file The file being written from.
	 * @return This object.
	 */
	public FileReaderBuilder file(File file) {
		this.file = file;
		return this;
	}

	/**
	 * Sets the path of the file being written from.
	 *
	 * @param path The path of the file being written from.
	 * @return This object.
	 */
	public FileReaderBuilder file(String path) {
		this.file = new File(path);
		return this;
	}

	/**
	 * Sets the character encoding of the file.
	 *
	 * @param cs
	 * 	The character encoding.
	 * 	The default is {@link Charset#defaultCharset()}.
	 * @return This object.
	 */
	public FileReaderBuilder charset(Charset cs) {
		this.cs = cs;
		return this;
	}

	/**
	 * Sets the character encoding of the file.
	 *
	 * @param cs
	 * 	The character encoding.
	 * 	The default is {@link Charset#defaultCharset()}.
	 * @return This object.
	 */
	public FileReaderBuilder charset(String cs) {
		this.cs = Charset.forName(cs);
		return this;
	}

	/**
	 * If called and the file is <jk>null</jk> or non-existent, then the {@link #build()} command will return an empty
	 * reader instead of a {@link FileNotFoundException}.
	 *
	 * @return This object.
	 */
	public FileReaderBuilder allowNoFile() {
		this.allowNoFile = true;
		return this;
	}

	/**
	 * Creates a new File reader.
	 *
	 * @return A new File reader.
	 * @throws FileNotFoundException If file could not be found.
	 */
	public Reader build() throws FileNotFoundException {
		if (allowNoFile && (file == null || ! file.exists()))
			return new StringReader("");
		return new InputStreamReader(new FileInputStream(file), cs);
	}
}
