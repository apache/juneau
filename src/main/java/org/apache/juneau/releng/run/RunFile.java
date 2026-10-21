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
package org.apache.juneau.releng.run;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One {@code <rm.home>/runs/<version>/run.json}, read-only. {@code release.py} is its only writer.
 */
public class RunFile {

	/** The release version ({@code details.version}). */
	public String version;

	/** The release candidate number ({@code details.rc}). */
	public int rc;

	/** {@code rc}, {@code vote}, {@code vote-failed}, {@code promote} or {@code released}. */
	public String phase;

	/** The last completed step. */
	public String last;

	/** The vote deadline, or {@code null}. */
	public String voteDeadline;

	/** The file. */
	public transient Path path;

	/** The attempts recorded in the file, oldest first. */
	public List<Attempt> attempts = new ArrayList<>();
}
