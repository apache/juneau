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
package org.apache.juneau;

import org.junit.jupiter.api.*;

/**
 * Base test class that provides common test configuration for all test classes in the junit package.
 *
 * <p>This class configures test method ordering to use method names, ensuring consistent and predictable
 * test execution order across all test classes in the package.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestClassOrder(ClassOrderer.ClassName.class)
public abstract class TestBase {
	// Base class for common test configuration
}
