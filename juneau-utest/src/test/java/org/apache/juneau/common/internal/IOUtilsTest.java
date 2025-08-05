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

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link IOUtils}.
 */
class IOUtilsTest extends SimpleTestBase {

    @Test void testReadPath() throws IOException {
        var p = new Properties();
        p.load(new StringReader(IOUtils.read(Paths.get("src/test/resources/files/Test3.properties"))));
        assertEquals("files/Test3.properties", p.get("file"));
    }
}