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
package org.apache.juneau.commons.reflect;

/**
 * A top-level, package-private interface with no other modifiers.
 *
 * <p>
 * Used by {@link ClassInfo_Coverage_Test} to exercise {@code ClassInfo.findToString()}'s
 * modifier-stripping branch where an interface's {@code Modifier.toString(getModifiers())}
 * consists of exactly {@code "abstract interface"} (no {@code static} bit, since only nested
 * types can carry one), so removing {@code "abstract"} and {@code "interface"} leaves an empty
 * string.
 */
interface PackagePrivateGreeterFixture {

	String greet();
}
