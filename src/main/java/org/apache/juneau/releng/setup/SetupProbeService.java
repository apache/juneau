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

package org.apache.juneau.releng.setup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.releng.nexus.MavenSettingsCredentials;
import org.apache.juneau.releng.util.ProcessRunner;

/**
 * Setup-tab probe inventory, eager verdicts, and loopback package-manager install.
 *
 * <p>Pills are prereqs first, then credentials. PATH tools may spawn {@code brew} or {@code apt-get};
 * checkout and {@code settings.xml} stay instructions. Credential live Validate stays a click on
 * {@code CredentialRest}.
 */
public class SetupProbeService {

	public static final String ID_CHECKOUT = "checkout";
	public static final String ID_MVN = "mvn";
	public static final String ID_GIT = "git";
	public static final String ID_GH = "gh";
	public static final String ID_GPG = "gpg";
	public static final String ID_SETTINGS = "settings-xml";
	public static final String ID_APACHE = "apache";
	public static final String ID_GPG_KEY = "gpg-key";
	public static final String ID_GITHUB = "github";

	static final String KIND_PREREQ = "prereq";
	static final String KIND_CREDENTIAL = "credential";
	static final String STATUS_PASS = "pass";
	static final String STATUS_FAIL = "fail";
	static final String STATUS_WARN = "warn";
	static final String STATUS_PENDING = "pending";
	static final String FRESH_EAGER = "eager";
	static final String PM_BREW = "brew";
	static final String PM_APT = "apt";
	static final String SERVER_ID = "apache.releases.https";

	private static final Duration INSTALL_TIMEOUT = Duration.ofMinutes(10);
	private static final Set<String> INSTALLABLE = Set.of(ID_MVN, ID_GIT, ID_GH, ID_GPG);
	private static final Map<String, String> BREW_FORMULA = Map.of(ID_MVN, "maven", ID_GIT, "git", ID_GH, "gh", ID_GPG,
			"gnupg");
	private static final Map<String, String> APT_PKG = Map.of(ID_MVN, "maven", ID_GIT, "git", ID_GH, "gh", ID_GPG,
			"gnupg");

	private final ProcessRunner runner;
	private final CredentialService credentials;
	private final Path repoDir;
	private final Path settingsXml;

	public SetupProbeService(ProcessRunner runner, CredentialService credentials, Path repoDir, Path settingsXml) {
		this.runner = runner;
		this.credentials = credentials;
		this.repoDir = repoDir;
		this.settingsXml = settingsXml;
	}

	/** Pill shells for first paint: labels only, nothing evaluated. */
	public List<Probe> inventory() {
		var out = new ArrayList<Probe>();
		for (var spec : specs())
			out.add(spec.pending());
		return out;
	}

	/** Eager verdicts for every probe plus the detected package manager. */
	public SetupData data() {
		var d = new SetupData();
		d.packageManager = detectPackageManager();
		d.probes = new ArrayList<>();
		for (var spec : specs())
			d.probes.add(evaluate(spec, d.packageManager));
		return d;
	}

	/**
	 * Spawns brew or apt-get for a PATH-tool probe. Re-probes afterwards. Unknown ids 404.
	 * Checkout / settings.xml are not installable.
	 */
	public InstallResult install(String probeId) {
		if (!INSTALLABLE.contains(probeId))
			throw new NotFound("Not an installable probe: %s", probeId);
		var pm = detectPackageManager();
		if (pm == null) {
			var r = new InstallResult();
			r.ok = false;
			r.output = "No package manager on PATH (need brew or apt-get).";
			r.probe = evaluate(spec(probeId), null);
			return r;
		}
		List<String> cmd;
		Map<String, String> env = null;
		if (PM_BREW.equals(pm)) {
			cmd = List.of("brew", "install", BREW_FORMULA.get(probeId));
		} else {
			cmd = List.of("apt-get", "install", "-y", APT_PKG.get(probeId));
			env = Map.of("DEBIAN_FRONTEND", "noninteractive");
		}
		var proc = runner.run(cmd, null, env, INSTALL_TIMEOUT);
		var r = new InstallResult();
		r.ok = proc.ok();
		r.output = proc.output() == null ? "" : proc.output();
		if (!proc.ok() && PM_APT.equals(pm) && looksLikeNeedsRoot(r.output))
			r.output = r.output + "\napt-get needs root/sudo; this app will not prompt for a password. "
					+ "Run the copyable command in your own terminal.";
		r.probe = evaluate(spec(probeId), pm);
		return r;
	}

	private static boolean looksLikeNeedsRoot(String output) {
		if (output == null)
			return false;
		var lower = output.toLowerCase();
		return lower.contains("permission denied") || lower.contains("are you root") || lower.contains("superuser")
				|| lower.contains("sudo");
	}

	String detectPackageManager() {
		if (onPath("brew"))
			return PM_BREW;
		if (onPath("apt-get"))
			return PM_APT;
		return null;
	}

	private boolean onPath(String tool) {
		return runner.run(List.of("which", tool), null, Map.of()).ok();
	}

	private Probe evaluate(Spec spec, String packageManager) {
		return switch (spec.id) {
			case ID_CHECKOUT -> checkout();
			case ID_MVN, ID_GIT, ID_GH, ID_GPG -> pathTool(spec, packageManager);
			case ID_SETTINGS -> settings();
			case ID_APACHE, ID_GPG_KEY, ID_GITHUB -> credential(spec);
			default -> spec.pending();
		};
	}

	private Probe checkout() {
		var p = spec(ID_CHECKOUT).pending();
		p.copyCommand = "git clone " + TargetProfile.prodDefault().cloneUrl();
		p.nextStep = "Clone apache/juneau and point rm.repo.dir at that checkout (default ~/git/apache/juneau).";
		if (!Files.isDirectory(repoDir)) {
			fail(p, "No directory at " + repoDir);
			return p;
		}
		if (!Files.isDirectory(repoDir.resolve(".git"))) {
			fail(p, "Not a git checkout: " + repoDir);
			return p;
		}
		if (!Files.isRegularFile(repoDir.resolve("pom.xml"))) {
			fail(p, "Missing pom.xml in " + repoDir);
			return p;
		}
		pass(p, "Found checkout at " + repoDir);
		return p;
	}

	private Probe pathTool(Spec spec, String packageManager) {
		var p = spec.pending();
		p.installable = packageManager != null;
		p.copyCommand = copyCommand(spec.id, packageManager);
		p.nextStep = p.installable ? "Install via the button, or paste the command in a terminal."
				: "Install this tool and ensure it is on PATH.";
		if (onPath(spec.id))
			pass(p, spec.id + " is on PATH");
		else
			fail(p, spec.id + " is not on PATH");
		return p;
	}

	private Probe settings() {
		var p = spec(ID_SETTINGS).pending();
		p.copyCommand = """
				<server>
				  <id>apache.releases.https</id>
				  <username>YOUR_AVAILID</username>
				  <password>YOUR_PASSWORD</password>
				</server>""";
		p.nextStep = "Add a <server id=\"" + SERVER_ID + "\"> block to " + settingsXml + ".";
		try {
			MavenSettingsCredentials.resolve(SERVER_ID, settingsXml);
			pass(p, "Found <server id=\"" + SERVER_ID + "\"> in " + settingsXml);
		} catch (Exception e) {
			fail(p, e.getMessage() == null ? "Missing " + SERVER_ID + " in " + settingsXml : e.getMessage());
		}
		return p;
	}

	private Probe credential(Spec spec) {
		var p = spec.pending();
		p.credentialName = spec.credentialName;
		p.nextStep = "Save the secret in Details, then Validate.";
		for (var st : credentials.status()) {
			if (!st.name.equals(spec.credentialName))
				continue;
			p.message = st.lastMessage == null ? "" : st.lastMessage;
			if (!st.present) {
				fail(p, spec.label + " is not set");
				return p;
			}
			if (st.lastValid == null) {
				warn(p, "Stored — not tested");
				return p;
			}
			if (Boolean.TRUE.equals(st.lastValid))
				pass(p, spec.label + " is valid");
			else
				fail(p, spec.label + " failed validation");
			return p;
		}
		fail(p, spec.label + " is not set");
		return p;
	}

	private String copyCommand(String toolId, String packageManager) {
		var formula = BREW_FORMULA.get(toolId);
		var pkg = APT_PKG.get(toolId);
		if (PM_BREW.equals(packageManager))
			return "brew install " + formula;
		if (PM_APT.equals(packageManager))
			return "sudo apt-get install -y " + pkg;
		return "brew install " + formula + "\n# or\nsudo apt-get install -y " + pkg;
	}

	private static void pass(Probe p, String message) {
		p.status = STATUS_PASS;
		p.message = message;
	}

	private static void fail(Probe p, String message) {
		p.status = STATUS_FAIL;
		p.message = message;
	}

	private static void warn(Probe p, String message) {
		p.status = STATUS_WARN;
		p.message = message;
	}

	private static Spec spec(String id) {
		for (var s : specs())
			if (s.id.equals(id))
				return s;
		throw new NotFound("Unknown probe: %s", id);
	}

	private static List<Spec> specs() {
		return List.of(
			new Spec(ID_CHECKOUT, KIND_PREREQ, "Juneau checkout", null),
			new Spec(ID_MVN, KIND_PREREQ, "mvn", null),
			new Spec(ID_GIT, KIND_PREREQ, "git", null),
			new Spec(ID_GH, KIND_PREREQ, "gh", null),
			new Spec(ID_GPG, KIND_PREREQ, "gpg", null),
			new Spec(ID_SETTINGS, KIND_PREREQ, "settings.xml", null),
			new Spec(ID_APACHE, KIND_CREDENTIAL, CredentialSpec.APACHE_LDAP.label, CredentialSpec.APACHE_LDAP.id),
			new Spec(ID_GPG_KEY, KIND_CREDENTIAL, CredentialSpec.GPG.label, CredentialSpec.GPG.id),
			new Spec(ID_GITHUB, KIND_CREDENTIAL, CredentialSpec.GITHUB.label, CredentialSpec.GITHUB.id));
	}

	private record Spec(String id, String kind, String label, String credentialName) {
		Probe pending() {
			var p = new Probe();
			p.id = id;
			p.kind = kind;
			p.label = label;
			p.status = STATUS_PENDING;
			p.freshness = FRESH_EAGER;
			p.credentialName = credentialName;
			return p;
		}
	}

	/** One Setup pill / Details payload. Public fields for JSON + FreeMarker. */
	public static class Probe {
		public String id;
		public String kind;
		public String label;
		public String status;
		public String message;
		public String nextStep;
		public String copyCommand;
		public boolean installable;
		public String freshness;
		public String credentialName;
	}

	/** GET /data body. */
	public static class SetupData {
		public String packageManager;
		public List<Probe> probes;
	}

	/** POST /install/{probeId} body. */
	public static class InstallResult {
		public boolean ok;
		public String output;
		public Probe probe;
	}
}
