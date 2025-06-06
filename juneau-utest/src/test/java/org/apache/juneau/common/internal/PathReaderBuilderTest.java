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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.Test;

/**
 * Tests {@link PathReaderBuilder}.
 */
public class PathReaderBuilderTest {

    private static final Path PATH = Paths.get("src/test/resources/files/Test3.properties");

    @Test
    public void testAllowNoFile() throws IOException {
        final var p = new Properties();
        try (Reader r = PathReaderBuilder.create().allowNoFile().build()) {
            p.load(new StringReader(IOUtils.read(r, Files.size(PATH))));
        }
        assertNull(p.get("file"));
        p.clear();
        try (Reader r = PathReaderBuilder.create().allowNoFile().path("this file does not exist, at all.").build()) {
            p.load(new StringReader(IOUtils.read(r, Files.size(PATH))));
        }
        assertNull(p.get("file"));
    }

    @Test
    public void testAllowNoFileException() throws IOException {
        final var p = new Properties();
        assertThrows(IllegalStateException.class, () -> PathReaderBuilder.create().build());
        assertThrows(NoSuchFileException.class, () -> PathReaderBuilder.create().path("this file does not exist, at all.").build());
    }

    @Test
    public void testCharsetCharset() throws IOException {
        final var p = new Properties();
        try (Reader r = PathReaderBuilder.create().path(PATH).charset(StandardCharsets.UTF_8).build()) {
            p.load(new StringReader(IOUtils.read(r, Files.size(PATH))));
        }
        assertEquals("files/Test3.properties", p.get("file"));
        p.clear();
        try (Reader r = PathReaderBuilder.create().path(PATH).charset((Charset) null).build()) {
            p.load(new StringReader(IOUtils.read(r, Files.size(PATH))));
        }
        assertEquals("files/Test3.properties", p.get("file"));
    }

    @Test
    public void testCharsetString() throws IOException {
        final var p = new Properties();
        try (Reader r = PathReaderBuilder.create().path(PATH).charset(StandardCharsets.UTF_8.name()).build()) {
            p.load(new StringReader(IOUtils.read(r, Files.size(PATH))));
        }
        assertEquals("files/Test3.properties", p.get("file"));
        p.clear();
        try (Reader r = PathReaderBuilder.create().path(PATH).charset((String) null).build()) {
            p.load(new StringReader(IOUtils.read(r, Files.size(PATH))));
        }
        assertEquals("files/Test3.properties", p.get("file"));
    }

    @Test
    public void testCreate() throws IOException {
        final var p = new Properties();
        try (Reader r = PathReaderBuilder.create(PATH).build()) {
            p.load(new StringReader(IOUtils.read(r, Files.size(PATH))));
        }
        assertEquals("files/Test3.properties", p.get("file"));
    }

    @Test
    public void testPathPath() throws IOException {
        final var p = new Properties();
        try (Reader r = PathReaderBuilder.create().path(PATH).build()) {
            p.load(new StringReader(IOUtils.read(r, Files.size(PATH))));
        }
        assertEquals("files/Test3.properties", p.get("file"));
    }

    @Test
    public void testPathString() throws IOException {
        final var p = new Properties();
        try (Reader r = PathReaderBuilder.create().path(PATH.toString()).build()) {
            p.load(new StringReader(IOUtils.read(r, Files.size(PATH))));
        }
        assertEquals("files/Test3.properties", p.get("file"));
    }

}
