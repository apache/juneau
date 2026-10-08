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
package org.apache.juneau.rest.server.views;

/** Test-only row bean for {@code ribbon-corpus.json}.  Not public API. */
public class CorpusRow {
	private int id;
	private String status, stream, phase, isNew;

	/** @return the id. */ public int getId() { return id; }
	/** @param v the id. @return this. */ public CorpusRow setId(int v) { id = v; return this; }
	/** @return the status. */ public String getStatus() { return status; }
	/** @param v the status. @return this. */ public CorpusRow setStatus(String v) { status = v; return this; }
	/** @return the stream. */ public String getStream() { return stream; }
	/** @param v the stream. @return this. */ public CorpusRow setStream(String v) { stream = v; return this; }
	/** @return the phase. */ public String getPhase() { return phase; }
	/** @param v the phase. @return this. */ public CorpusRow setPhase(String v) { phase = v; return this; }
	/** @return "yes" or "no". */ public String getIsNew() { return isNew; }
	/** @param v "yes" or "no". @return this. */ public CorpusRow setIsNew(String v) { isNew = v; return this; }
}
