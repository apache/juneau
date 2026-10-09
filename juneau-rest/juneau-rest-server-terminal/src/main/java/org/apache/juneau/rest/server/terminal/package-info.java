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
 * Raw command output in the browser: a byte-offset endpoint over a {@link org.apache.juneau.rest.server.terminal.TerminalSource}
 * and an xterm.js panel that renders it the way a terminal would.
 *
 * <p>
 * {@link org.apache.juneau.rest.server.terminal.TerminalMixin} serves {@code /juneau-terminal/{id}/bytes} and
 * {@code /raw}, plus this module's {@code juneau-terminal.js} and {@code .css}.  Sources:
 * {@link org.apache.juneau.rest.server.terminal.FileTerminalSource} tails a log with a {@code .size} sidecar;
 * {@link org.apache.juneau.rest.server.terminal.MemoryTerminalSource} is a ring buffer for in-process producers.
 * {@link org.apache.juneau.rest.server.terminal.TerminalProcess} runs a tool under a pseudo-terminal through
 * {@code juneau_run.py --pty} and exposes both its bytes and its run-view step events.
 *
 * <p>
 * On a console page, {@code <@card type="terminal">} (see {@link org.apache.juneau.rest.server.terminal.TerminalDef})
 * mounts the panel; the {@code terminal} toolkit pack loads xterm.js from the {@code org.webjars.npm:xterm__xterm} WebJar.
 */
package org.apache.juneau.rest.server.terminal;
