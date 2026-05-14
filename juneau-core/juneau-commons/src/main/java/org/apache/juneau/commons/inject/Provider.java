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

/**
 * Lightweight provider interface equivalent to JSR-330 provider contracts.
 *
 * @param <T> The provided type.
 */
@FunctionalInterface
public interface Provider<T> {

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	T get();
}

