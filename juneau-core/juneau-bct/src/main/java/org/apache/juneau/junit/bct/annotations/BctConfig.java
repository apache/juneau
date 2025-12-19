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
package org.apache.juneau.junit.bct.annotations;

import java.lang.annotation.*;

import org.apache.juneau.commons.lang.*;
import org.apache.juneau.junit.bct.*;
import org.junit.jupiter.api.extension.*;

/**
 * Annotation for configuring BCT settings in JUnit 5 tests.
 *
 * <p>
 * This annotation can be applied to test classes or test methods to automatically configure
 * BCT settings before tests run and clear them after tests complete.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@BctConfig</ja>(sortMaps=TriState.<jsf>TRUE</jsf>)
 * 	<jk>class</jk> MyTest {
 * 		<ja>@Test</ja>
 * 		<ja>@BctConfig</ja>(sortCollections=TriState.<jsf>TRUE</jsf>)
 * 		<jk>void</jk> testFoo() {
 * 			<jc>// sortMaps and sortCollections are both enabled</jc>
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * Method-level annotations override class-level annotations.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(BctConfigExtension.class)
public @interface BctConfig {

	/**
	 * Enable sorting of maps in BCT assertions.
	 *
	 * @return {@link TriState#TRUE} to enable map sorting, {@link TriState#FALSE} to disable,
	 * 	or {@link TriState#UNSET} to inherit from class-level annotation.
	 */
	TriState sortMaps() default TriState.UNSET;

	/**
	 * Enable sorting of collections in BCT assertions.
	 *
	 * @return {@link TriState#TRUE} to enable collection sorting, {@link TriState#FALSE} to disable,
	 * 	or {@link TriState#UNSET} to inherit from class-level annotation.
	 */
	TriState sortCollections() default TriState.UNSET;

	/**
	 * Custom bean converter class to use for BCT assertions.
	 *
	 * <p>
	 * If specified (not the default {@link BeanConverter}), the class will be instantiated using
	 * its no-arg constructor and set as the converter for the current thread.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@BctConfiguration</ja>(beanConverter=MyCustomConverter.<jk>class</jk>)
	 * 	<jk>class</jk> MyTest {
	 * 		<ja>@Test</ja>
	 * 		<jk>void</jk> testFoo() {
	 * 			<jc>// Uses MyCustomConverter for all assertions</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return The bean converter class to instantiate and use, or {@link BeanConverter} to use the default.
	 */
	Class<? extends BeanConverter> beanConverter() default BeanConverter.class;
}

