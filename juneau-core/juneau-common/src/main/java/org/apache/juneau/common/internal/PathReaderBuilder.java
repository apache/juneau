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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for creating {@link Path}-based {@link Reader} objects.
 *
 * @since 9.1.0
 */
public final class PathReaderBuilder {

    /**
     * Creates a new builder.
     *
     * @return A new builder.
     */
    public static PathReaderBuilder create() {
        return new PathReaderBuilder();
    }

    /**
     * Creates a new builder initialized with the specified path.
     *
     * @param path The path being written to.
     * @return A new builder.
     */
    public static PathReaderBuilder create(final Path path) {
        return new PathReaderBuilder().path(path);
    }

    private Path path;

    private Charset charset = Charset.defaultCharset();

    private boolean allowNoFile;

    /**
     * If called and the path is <jk>null</jk> or non-existent, then the {@link #build()} command will return an empty reader instead of a {@link IOException}.
     *
     * @return This object.
     */
    public PathReaderBuilder allowNoFile() {
        this.allowNoFile = true;
        return this;
    }

    /**
     * Creates a new File reader.
     *
     * @return A new File reader.
     * @throws IOException if an I/O error occurs opening the path
     */
    public Reader build() throws IOException {
        if (!allowNoFile && path == null) {
            throw new IllegalStateException("No path");
        }
        if (!allowNoFile && !Files.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        return allowNoFile ? new StringReader("") : Files.newBufferedReader(path, charset != null ? charset : Charset.defaultCharset());
    }

    /**
     * Sets the character encoding of the path.
     *
     * @param charset The character encoding. The default is {@link Charset#defaultCharset()}. Null resets to the default.
     * @return This object.
     */
    public PathReaderBuilder charset(final Charset charset) {
        this.charset = charset;
        return this;
    }

    /**
     * Sets the character encoding of the path.
     *
     * @param charset The character encoding. The default is {@link Charset#defaultCharset()}. Null resets to the default.
     * @return This object.
     */
    public PathReaderBuilder charset(final String charset) {
        this.charset = charset != null ? Charset.forName(charset) : null;
        return this;
    }

    /**
     * Sets the path being written from.
     *
     * @param path The path being written from.
     * @return This object.
     */
    public PathReaderBuilder path(final Path path) {
        this.path = path;
        return this;
    }

    /**
     * Sets the path of the path being written from.
     *
     * @param path The path of the path being written from.
     * @return This object.
     */
    public PathReaderBuilder path(final String path) {
        this.path = Paths.get(path);
        return this;
    }
}
