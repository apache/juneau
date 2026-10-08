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
/**
 * Run-view events and the readers that produce them from test-report files.
 *
 * <p>
 * A {@link org.apache.juneau.rest.server.runreport.RunEvent} is one event of a run-view stream (a step, a suite
 * summary, a test, a note, ...).  Each {@link org.apache.juneau.rest.server.runreport.ReportReader} reads one report
 * format (Surefire and JUnit XML, Jest JSON, Playwright JSON) into a
 * {@link org.apache.juneau.rest.server.runreport.ReportResult}, whose {@code toEvents(step)} yields the
 * {@code replace}, {@code test} and {@code note} events a producer appends to a run view when a step ends.  A reader
 * never throws for bad input: a missing, oversize, malformed or truncated file yields the tests that could be
 * recovered plus a warning.
 *
 * <p>
 * Use {@link org.apache.juneau.rest.server.runreport.ReportReaders#forKind(java.lang.String)} to pick a reader by
 * its report kind.
 *
 * <p>
 * This module has no dependency on the REST server or on the view toolkit, so a producer (a build tool, a test
 * runner, a release application) can use it on its own; <c>juneau-rest-server-views</c> depends on it to render and
 * serve the events.
 */
package org.apache.juneau.rest.server.runreport;
