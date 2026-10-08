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
package org.apache.juneau.rest.server.views.report;

import org.apache.juneau.rest.server.views.RunEvent.*;

/**
 * One test recovered from a report.
 *
 * <p>
 * A reader cannot build a {@code RunEvent} directly because a test event needs a step id, which the caller supplies
 * later through {@link ReportResult#toEvents(String)}.
 *
 * @param suite The suite name (a class name or a file path).
 * @param name The test name.
 * @param status The test status.
 * @param ms The duration in milliseconds, or <jk>null</jk>.
 * @param msg The failure message, or <jk>null</jk>.
 * @param trace The stack trace, or <jk>null</jk>.
 */
public record ReportTest(String suite, String name, TestStatus status, Long ms, String msg, String trace) {}
