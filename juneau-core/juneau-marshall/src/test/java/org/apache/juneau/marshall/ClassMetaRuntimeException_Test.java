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
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class ClassMetaRuntimeException_Test {

	// getMessage(cause=null, c=null, msg!=null) → msg branch (nn(msg)=true, c==null → no prefix)
	@Test void a01_message_only() {
		var e = new ClassMetaRuntimeException("oops");
		assertEquals("oops", e.getMessage());
	}

	// getMessage(cause=null, c!=null, msg!=null) → class prefix + msg
	@Test void a02_class_and_message() {
		var e = new ClassMetaRuntimeException(String.class, "bad value");
		assertTrue(e.getMessage().contains("bad value"));
		assertTrue(e.getMessage().contains("String"));
	}

	// getMessage(cause!=null, c=null, msg=null) → cause.getMessage() branch (nn(msg)=false, nn(cause)=true, c==null)
	@Test void a03_cause_only() {
		var cause = new RuntimeException("root cause");
		var e = new ClassMetaRuntimeException(cause);
		assertTrue(e.getMessage().contains("root cause"));
	}

	// getMessage(cause!=null, c!=null, msg=null) → class prefix + cause.getMessage()
	@Test void a04_cause_and_class() {
		var cause = new RuntimeException("cause msg");
		var e = new ClassMetaRuntimeException(cause, String.class, null);
		assertTrue(e.getMessage().contains("String"));
		assertTrue(e.getMessage().contains("cause msg"));
	}

	// getMessage(cause=null, c=null, msg=null) → returns null → super passes null
	@Test void a05_null_everything() {
		var e = new ClassMetaRuntimeException((Throwable)null, null, null);
		assertNull(e.getMessage());
	}

	// Varargs constructor with format args
	@Test void a06_message_with_args() {
		var e = new ClassMetaRuntimeException("value={0}", 42);
		assertTrue(e.getMessage().contains("42"));
	}
}
