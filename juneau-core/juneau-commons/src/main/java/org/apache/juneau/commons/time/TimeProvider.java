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
package org.apache.juneau.commons.time;

import static org.apache.juneau.commons.utils.Utils.*;

import java.time.*;
import java.util.function.*;

import org.apache.juneau.commons.function.*;

public class TimeProvider {

	public static final TimeProvider INSTANCE = new TimeProvider();

	public ZoneId getSystemDefaultZoneId() {
		return ZoneId.systemDefault();
	}

	public ZonedDateTime now() {
		return ZonedDateTime.now();
	}

	public ZonedDateTime now(ZoneId zoneId) {
		return ZonedDateTime.now(zoneId);
	}
}
