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

public class ListBuilder_Test {

    @Test
    void build_basic_add_and_addAll() {
        List<String> l = listb(String.class)
            .add("a")
            .add("b", "c")
            .addAll(l("d", "e"))
            .build();
        assertEquals(l("a","b","c","d","e"), l);
    }

    @Test
    void build_sparse_and_unmodifiable() {
        assertNull(listb(String.class).sparse().build());
        List<String> l = listb(String.class).add("a").unmodifiable().build();
        assertThrows(UnsupportedOperationException.class, () -> l.add("b"));
    }

    @Test
    void sorted_natural_and_custom() {
        List<Integer> l1 = listb(Integer.class).add(3,1,2).sorted().build();
        assertEquals(l(1,2,3), l1);

        List<Integer> l2 = listb(Integer.class).add(3,1,2).sorted(Comparator.reverseOrder()).build();
        assertEquals(l(3,2,1), l2);
    }
}


