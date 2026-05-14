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

/**
 * Lightweight bean lookup, contribution, and injection support for Juneau.
 *
 * <p>
 * This package provides a compact, store-centric model intended for framework-owned wiring.
 * It is not a general-purpose CDI/Spring replacement.
 *
 * <h5 class='section'>Supported annotation interop:</h5>
 * <ul>
 * 	<li>JSR-330-style injection markers (matched by FQN, no hard dependency): {@code jakarta.inject.*}, {@code javax.inject.*}
 * 	<li>JSR-250 lifecycle markers (matched by FQN, no hard dependency): {@code jakarta.annotation.*}, {@code javax.annotation.*}
 * 	<li>Spring {@code @Autowired} support (matched by FQN)
 * </ul>
 *
 * <h5 class='section'>Non-goals:</h5>
 * <ul>
 * 	<li>Classpath component scanning
 * 	<li>AOP, transactions, scheduling, caching abstractions
 * 	<li>General graph solving/autowire containers
 * </ul>
 */
package org.apache.juneau.commons.inject;

