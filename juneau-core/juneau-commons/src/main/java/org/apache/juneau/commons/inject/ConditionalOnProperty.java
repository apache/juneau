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
 * Condition that checks a setting value.
 */
@Documented
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(ConditionalOnProperties.class)
public @interface ConditionalOnProperty {

	/**
	 * The property name to look up.
	 *
	 * @return The property name.
	 */
	String name();

	/**
	 * Required property value.
	 *
	 * <p>
	 * When blank, any present value matches.
	 *
	 * @return The required value.
	 */
	String havingValue() default "";

	/**
	 * Whether to match when the property is missing.
	 *
	 * @return <jk>true</jk> to match missing properties.
	 */
	boolean matchIfMissing() default false;
}

