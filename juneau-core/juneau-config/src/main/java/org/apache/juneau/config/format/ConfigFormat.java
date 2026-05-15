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
package org.apache.juneau.config.format;

import java.io.*;

import org.apache.juneau.config.internal.*;

/**
 * Format strategy for config map persistence.
 */
public interface ConfigFormat {

	/**
	 * Format identifier.
	 *
	 * @return Format identifier.
	 */
	String id();

	/**
	 * Normalizes source format text into INI-style content consumed by {@link ConfigMap}.
	 *
	 * @param contents The source contents.
	 * @return INI-style contents.
	 * @throws IOException Thrown by underlying stream.
	 */
	String toInternal(String contents) throws IOException;

	/**
	 * Writes a map to this format.
	 *
	 * @param map The map to write.
	 * @return Serialized contents in this format.
	 * @throws IOException Thrown by underlying stream.
	 */
	String fromInternal(ConfigMap map) throws IOException;
}
