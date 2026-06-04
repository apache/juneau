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

import java.lang.reflect.*;

import org.apache.juneau.commons.settings.*;

/**
 * Runtime context for evaluating {@link Condition} instances.
 *
 * @param beanStore The bean store.
 * @param settings The settings facade.
 * @param classLoader The classloader used for class-presence checks.
 * @param annotatedElement The annotated class/method/field under evaluation.
 */
public record ConditionContext(BeanStore beanStore, Settings settings, ClassLoader classLoader, AnnotatedElement annotatedElement) {}

