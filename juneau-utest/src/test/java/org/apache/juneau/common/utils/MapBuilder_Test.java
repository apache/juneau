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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

public class MapBuilder_Test {

    @Test
    void build_basic_add_and_addAll() {
        Map<String,Integer> m = mapb(String.class, Integer.class)
            .add("a",1)
            .add("b",2)
            .addAll(map("c",3))
            .build();
        LinkedHashMap<String,Integer> expected = map();
        expected.put("a",1);
        expected.put("b",2);
        expected.put("c",3);
        assertEquals(expected, m);
    }

    @Test
    void build_sparse_and_unmodifiable() {
        assertNull(mapb(String.class, Integer.class).sparse().build());
        Map<String,Integer> m = mapb(String.class, Integer.class).add("a",1).unmodifiable().build();
        assertThrows(UnsupportedOperationException.class, () -> m.put("b",2));
    }

    @Test
    void sorted_natural_and_custom() {
        Map<String,Integer> m1 = mapb(String.class, Integer.class)
            .add("b",2).add("c",3).add("a",1)
            .sorted()
            .build();
        assertEquals(new TreeMap<>(map("a",1,"b",2,"c",3)), m1);

        Map<String,Integer> m2 = mapb(String.class, Integer.class)
            .add("b",2).add("c",3).add("a",1)
            .sorted(Comparator.reverseOrder())
            .build();
        TreeMap<String,Integer> expected = new TreeMap<>(Comparator.reverseOrder());
        expected.put("c",3);
        expected.put("b",2);
        expected.put("a",1);
        assertEquals(expected, m2);
    }
}


