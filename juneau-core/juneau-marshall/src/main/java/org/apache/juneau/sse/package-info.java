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
 * Server-Sent Events (SSE) marshalling support — {@code text/event-stream}.
 *
 * <p>
 * Provides {@link org.apache.juneau.sse.SseSerializer} and {@link org.apache.juneau.sse.SseParser}
 * for the
 * <a class="doclink" href="https://html.spec.whatwg.org/multipage/server-sent-events.html">WHATWG Server-Sent Events</a>
 * wire format. The serializer accepts a single {@link org.apache.juneau.sse.SseEvent}, an
 * {@link java.lang.Iterable Iterable}, a {@link java.util.stream.Stream Stream}, or an
 * {@code SseEvent[]} array and emits one event per element with a {@link java.io.Writer#flush()
 * flush()} after each one — that is what makes server push actually work behind
 * {@code SerializedPojoProcessor}.
 *
 * <p>
 * SSE streams are typically long-lived. For true line-driven streaming over a {@link java.io.Reader}
 * that the caller owns, use the {@link org.apache.juneau.sse.SseEventReader} iterator.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://html.spec.whatwg.org/multipage/server-sent-events.html">WHATWG HTML §9.2 — Server-sent events</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SseBasics">SSE Basics</a>
 * </ul>
 */
package org.apache.juneau.sse;
