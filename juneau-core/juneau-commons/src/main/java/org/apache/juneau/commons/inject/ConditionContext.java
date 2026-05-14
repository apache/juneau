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
 */
public final class ConditionContext {

	private final BeanStore beanStore;
	private final Settings settings;
	private final ClassLoader classLoader;
	private final AnnotatedElement annotatedElement;

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store.
	 * @param settings The settings facade.
	 * @param classLoader The classloader used for class-presence checks.
	 * @param annotatedElement The annotated class/method/field under evaluation.
	 */
	public ConditionContext(BeanStore beanStore, Settings settings, ClassLoader classLoader, AnnotatedElement annotatedElement) {
		this.beanStore = beanStore;
		this.settings = settings;
		this.classLoader = classLoader;
		this.annotatedElement = annotatedElement;
	}

	/**
	 * The bean store.
	 *
	 * @return The bean store.
	 */
	public BeanStore beanStore() {
		return beanStore;
	}

	/**
	 * The settings facade.
	 *
	 * @return The settings facade.
	 */
	public Settings settings() {
		return settings;
	}

	/**
	 * The class loader.
	 *
	 * @return The class loader.
	 */
	public ClassLoader classLoader() {
		return classLoader;
	}

	/**
	 * The element being evaluated.
	 *
	 * @return The annotated element.
	 */
	public AnnotatedElement annotatedElement() {
		return annotatedElement;
	}
}

