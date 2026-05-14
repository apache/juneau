/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package org.apache.juneau.commons.inject;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Condition that requires a bean to be absent in the current store.
 */
@Documented
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(ConditionalOnMissingBeans.class)
public @interface ConditionalOnMissingBean {

	/**
	 * The bean type to check.
	 *
	 * @return The bean type.
	 */
	Class<?> value() default Object.class;

	/**
	 * Optional bean name to check.
	 *
	 * @return The bean name.
	 */
	String name() default "";
}

