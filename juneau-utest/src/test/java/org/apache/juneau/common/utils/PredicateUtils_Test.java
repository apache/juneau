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

import static org.apache.juneau.common.utils.PredicateUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.junit.jupiter.api.*;

public class PredicateUtils_Test {

    @Test
    void and_allNull_returnsTrue() {
        Predicate<String> p = and(null, null);
        assertTrue(p.test("anything"));
        assertTrue(p.test(null));
    }

    @Test
    void and_empty_returnsTrue() {
        Predicate<String> p = and();
        assertTrue(p.test("x"));
    }

    @Test
    void and_single_predicate() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Predicate<Integer> p = and(isEven);
        assertTrue(p.test(2));
        assertFalse(p.test(3));
        assertFalse(p.test(null));
    }

    @Test
    void and_multiple_predicates() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Predicate<Integer> gt10 = x -> x != null && x > 10;
        Predicate<Integer> p = and(isEven, gt10);
        assertTrue(p.test(12));
        assertFalse(p.test(11));
        assertFalse(p.test(10));
        assertFalse(p.test(9));
        assertFalse(p.test(null));
    }

    @Test
    void and_ignoresNullEntries() {
        Predicate<String> startsA = s -> s != null && s.startsWith("A");
        Predicate<String> p = and(null, startsA, null);
        assertTrue(p.test("Alpha"));
        assertFalse(p.test("Beta"));
    }

    @Test
    void or_allNull_returnsFalse() {
        Predicate<String> p = or(null, null);
        assertFalse(p.test("anything"));
        assertFalse(p.test(null));
    }

    @Test
    void or_empty_returnsFalse() {
        Predicate<String> p = or();
        assertFalse(p.test("x"));
    }

    @Test
    void or_single_predicate() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Predicate<Integer> p = or(isEven);
        assertTrue(p.test(2));
        assertFalse(p.test(3));
        assertFalse(p.test(null));
    }

    @Test
    void or_multiple_predicates() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Predicate<Integer> gt10 = x -> x != null && x > 10;
        Predicate<Integer> p = or(isEven, gt10);
        assertTrue(p.test(12));
        assertTrue(p.test(11));
        assertTrue(p.test(10));
        assertFalse(p.test(9));
        assertFalse(p.test(null));
    }

    @Test
    void or_ignoresNullEntries() {
        Predicate<String> startsA = s -> s != null && s.startsWith("A");
        Predicate<String> p = or(null, startsA, null);
        assertTrue(p.test("Alpha"));
        assertFalse(p.test("Beta"));
    }
}


