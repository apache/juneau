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
 * Utility class for creating {@link FileWriter} objects.
 */
public final class FileWriterBuilder {

	private File file;
	private Charset cs = Charset.defaultCharset();
	private boolean append, buffered;

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static FileWriterBuilder create() {
		return new FileWriterBuilder();
	}

	/**
	 * Creates a new builder initialized with the specified file.
	 *
	 * @param file The file being written to.
	 * @return A new builder.
	 */
	public static FileWriterBuilder create(File file) {
		return new FileWriterBuilder().file(file);
	}

	/**
	 * Creates a new builder initialized with the specified file path.
	 *
	 * @param path The file path being written to.
	 * @return A new builder.
	 */
	public static FileWriterBuilder create(String path) {
		return new FileWriterBuilder().file(path);
	}

	/**
	 * Sets the file being written to.
	 *
	 * @param file The file being written to.
	 * @return This object.
	 */
	public FileWriterBuilder file(File file) {
		this.file = file;
		return this;
	}

	/**
	 * Sets the path of the file being written to.
	 *
	 * @param path The path of the file being written to.
	 * @return This object.
	 */
	public FileWriterBuilder file(String path) {
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
	public FileWriterBuilder charset(Charset cs) {
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
	public FileWriterBuilder charset(String cs) {
		this.cs = Charset.forName(cs);
		return this;
	}

	/**
	 * Sets the append mode on the writer to <jk>true</jk>.
	 *
	 * @return This object.
	 */
	public FileWriterBuilder append() {
		this.append = true;
		return this;
	}

	/**
	 * Sets the buffer mode on the writer to <jk>true</jk>.
	 *
	 * @return This object.
	 */
	public FileWriterBuilder buffered() {
		this.buffered = true;
		return this;
	}

	/**
	 * Creates a new File writer.
	 *
	 * @return A new File writer.
	 * @throws FileNotFoundException If file could not be found.
	 */
	public Writer build() throws FileNotFoundException {
		OutputStream os = new FileOutputStream(file, append);
		if (buffered)
			os = new BufferedOutputStream(os);
		return new OutputStreamWriter(os, cs);
	}
}
