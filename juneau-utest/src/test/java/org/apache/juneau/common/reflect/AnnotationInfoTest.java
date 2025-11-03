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
package org.apache.juneau.common.reflect;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.common.annotation.*;
import org.junit.jupiter.api.*;

class AnnotationInfoTest extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Is in group.
	//-----------------------------------------------------------------------------------------------------------------

	@Target(TYPE)
	@Retention(RUNTIME)
	@AnnotationGroup(D1.class)
	public static @interface D1 {}

	@Target(TYPE)
	@Retention(RUNTIME)
	@AnnotationGroup(D1.class)
	public static @interface D2 {}

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface D3 {}

	@D1 @D2 @D3
	public static class D {}

	@Test void d01_isInGroup() {
		var d = ClassInfo.of(D.class);
		var l = d.getAnnotationList(x -> x.isInGroup(D1.class));
		assertSize(2, l);
	}
}