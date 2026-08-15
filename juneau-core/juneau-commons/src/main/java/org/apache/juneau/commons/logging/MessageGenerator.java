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
package org.apache.juneau.commons.logging;

/**
 * Strategy interface for rendering log message patterns.
 */
@FunctionalInterface
public interface MessageGenerator {

	/**
	 * Printf-style message generator backed by {@link #format(String, Object...)}.
	 */
	MessageGenerator PRINTF = (pattern, args) -> org.apache.juneau.commons.utils.StringUtils.format(pattern, args);

	/**
	 * MessageFormat-style message generator backed by {@link org.apache.juneau.commons.utils.StringUtils#mformat(String, Object...)}.
	 */
	MessageGenerator MESSAGE_FORMAT = (pattern, args) -> org.apache.juneau.commons.utils.StringUtils.mformat(pattern, args);

	/**
	 * Renders a message pattern with arguments.
	 *
	 * @param pattern The message pattern.
	 * @param args The message arguments.
	 * @return The rendered message.
	 */
	String format(String pattern, Object...args);
}
