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

package org.apache.juneau.releng;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.ApacheLdapValidator;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.releng.credential.GithubTokenValidator;
import org.apache.juneau.releng.credential.GpgValidator;
import org.apache.juneau.releng.credential.Validator;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.log.EventBroadcaster;
import org.apache.juneau.releng.log.RunEventsSseServlet;
import org.apache.juneau.releng.milestone.GithubPrSource;
import org.apache.juneau.releng.milestone.MilestoneService;
import org.apache.juneau.releng.release.GitTagReleaseSource;
import org.apache.juneau.releng.release.GithubReleaseSource;
import org.apache.juneau.releng.release.LocalStateReleaseSource;
import org.apache.juneau.releng.release.ReleaseListService;
import org.apache.juneau.releng.rest.CredentialRest;
import org.apache.juneau.releng.rest.HomeRest;
import org.apache.juneau.releng.rest.MilestoneRest;
import org.apache.juneau.releng.rest.SetupRest;
import org.apache.juneau.releng.setup.SetupProbeService;
import org.apache.juneau.releng.rest.ReleaseRest;
import org.apache.juneau.releng.rest.ReleaseRunRest;
import org.apache.juneau.releng.run.CredentialEnv;
import org.apache.juneau.releng.run.ReleaseProcessService;
import org.apache.juneau.releng.run.RunFileWatcher;
import org.apache.juneau.releng.run.RunFiles;
import org.apache.juneau.releng.util.DefaultProcessRunner;
import org.apache.juneau.releng.util.ProcessRunner;
import org.apache.juneau.rest.server.filter.LoopbackBoundary;
import org.apache.juneau.rest.server.filter.LoopbackBoundaryFilter;
import org.apache.juneau.rest.server.filter.SynchronizerToken;
import org.apache.juneau.terminal.region.TerminalProcess;
import org.apache.juneau.secret.macos.keychain.KeychainSecretStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import jakarta.servlet.Servlet;

/**
 * Bean wiring for the Release Manager application: credentials, release engine, REST resources, and servlets.
 */
@Configuration
@SuppressWarnings({
	"java:S6539" // Spring @Configuration legitimately aggregates cohesive bean wiring; splitting would fragment it.
})
public class AppConfiguration {

	/**
	 * The {@link ProcessRunner} used to shell out to external tools.
	 */
	@Bean
	public ProcessRunner processRunner() {
		return new DefaultProcessRunner();
	}

	/**
	 * The process's CSRF secret: minted here, embedded in every page (see the page beans below), and required
	 * back on every write. One per boot, so a restart invalidates whatever any still-open tab is holding and
	 * that tab must reload.
	 *
	 * <p>Deliberately <b>not</b> a double-submit cookie: cookies are scoped by host and ignore the port, so any
	 * page served from any other {@code localhost:*} could plant one this application would read back and
	 * accept. {@link SynchronizerToken} documents that at length.
	 */
	@Bean
	public SynchronizerToken csrfToken() {
		return SynchronizerToken.generate();
	}

	/**
	 * The boundary itself, pinned to the one authority this application is reached at.
	 *
	 * <p>Exactly one spelling is accepted: bound to {@code 127.0.0.1:8790}, the app is <b>not</b> reachable as
	 * {@code localhost:8790}. That is intentional (see {@link LoopbackBoundary}'s canonical-origin section); a
	 * 421 with a {@code Host}-naming message is the diagnostic when someone uses the other spelling.
	 */
	@Bean
	public LoopbackBoundary loopbackBoundary(SynchronizerToken token,
			@Value("${server.address:127.0.0.1}") String address, @Value("${server.port:8790}") int port) {
		return LoopbackBoundary.create().authority(address, port).token(token).build();
	}

	/**
	 * Registers the boundary as a servlet filter at {@code /*} with the highest precedence.
	 *
	 * <p>A filter rather than a Juneau guard or mixin hook because it must be impossible to omit: every REST
	 * resource, the SSE endpoint, the Nexus mock, the static assets and the 404s all pass through it, so an
	 * endpoint added later is covered without anyone remembering to cover it. A guard declared per-operation
	 * would leave each new endpoint unprotected by default, and its absence would be invisible in review.
	 */
	@Bean
	public FilterRegistrationBean<LoopbackBoundaryFilter> loopbackBoundaryFilter(LoopbackBoundary boundary) {
		var reg = new FilterRegistrationBean<>(new LoopbackBoundaryFilter(boundary));
		reg.addUrlPatterns("/*");
		reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return reg;
	}

	/**
	 * The {@link ReleaseListService} that merges git-tag, GitHub, and local-state release sources.
	 */
	@Bean
	public ReleaseListService releaseListService(ProcessRunner runner, @Value("${rm.repo.dir}") String repoDir,
			@Value("${rm.repo.slug}") String repoSlug, RunFiles runFiles) {
		var tags = new GitTagReleaseSource(runner, repoDir);
		var github = new GithubReleaseSource(runner, repoSlug);
		var state = new LocalStateReleaseSource(runFiles);
		return new ReleaseListService(tags::list, github::list, state::list);
	}

	/**
	 * Reads release.py's run files under {@code rm.home}. Liveness of a sidecar-only attempt is asked of the
	 * {@link ReleaseProcessService}.
	 */
	@Bean
	public RunFiles runFiles(@Value("${rm.home}") String home, ObjectProvider<ReleaseProcessService> processes) {
		return new RunFiles(Path.of(home), dir -> {
			var p = processes.getIfAvailable();
			return p != null && p.isLiveDir(dir);
		});
	}

	/**
	 * The {@link RootRest} servlet group registered at {@code /rest/*}.
	 */
	@Bean
	public RootRest rootRest() {
		return new RootRest();
	}

	/**
	 * The {@link HomeRest} resource.
	 */
	@Bean
	public HomeRest homeRest() {
		return new HomeRest();
	}

	/**
	 * The {@link SetupProbeService} that checks the local toolchain and credential prerequisites.
	 */
	@Bean
	public SetupProbeService setupProbeService(ProcessRunner runner, CredentialService credentials,
			@Value("${rm.repo.dir}") String repoDir) {
		return new SetupProbeService(runner, credentials, Path.of(repoDir),
				Path.of(System.getProperty("user.home"), ".m2", "settings.xml"));
	}

	/**
	 * The {@link SetupRest} resource.
	 */
	@Bean
	public SetupRest setupRest(SetupProbeService setup) {
		return new SetupRest(setup);
	}

	/**
	 * The {@link ReleaseRest} resource.
	 */
	@Bean
	public ReleaseRest releaseRest(ReleaseListService svc) {
		return new ReleaseRest(svc);
	}

	/**
	 * The {@link GithubPrSource} used to look up merged PRs for a milestone.
	 */
	@Bean
	public GithubPrSource githubPrSource(ProcessRunner runner, @Value("${rm.repo.slug}") String repoSlug) {
		return new GithubPrSource(runner, repoSlug);
	}

	/**
	 * The {@link MilestoneService} that builds milestone release notes.
	 */
	@Bean
	public MilestoneService milestoneService() {
		return new MilestoneService();
	}

	/**
	 * The {@link MilestoneRest} resource.
	 */
	@Bean
	public MilestoneRest milestoneRest(MilestoneService svc, GithubPrSource prSource, ProcessRunner runner,
			@Value("${rm.repo.dir}") String repoDir) {
		return new MilestoneRest(svc, prSource, runner, repoDir);
	}

	/**
	 * One {@link SecretStore} per credential, namespaced by {@link CredentialSpec#keychainService} so the
	 * keychain service+account coordinates the app has always used are preserved exactly.
	 */
	@Bean
	public Map<CredentialSpec, SecretStore> credentialSecretStores() {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			stores.put(spec, new KeychainSecretStore(spec.keychainService));
		return stores;
	}

	/**
	 * Persists the non-secret {availid, GPG key ID} account identifiers under {@code rm.state.dir}
	 * (see {@link AccountStore}) so they survive a restart alongside the Keychain-held secrets.
	 */
	@Bean
	public AccountStore accountStore(@Value("${rm.state.dir}") String stateDir) {
		return new AccountStore(Path.of(stateDir));
	}

	/**
	 * The {@link CredentialService} backed by the per-credential secret stores and validators.
	 */
	@Bean
	public CredentialService credentialService(Map<CredentialSpec, SecretStore> stores, ProcessRunner runner,
			AccountStore accounts) {
		var validators = new EnumMap<CredentialSpec, Validator>(CredentialSpec.class);
		validators.put(CredentialSpec.APACHE_LDAP, new ApacheLdapValidator());
		validators.put(CredentialSpec.GPG, new GpgValidator(runner));
		validators.put(CredentialSpec.GITHUB, new GithubTokenValidator(runner));
		return new CredentialService(stores, validators, accounts);
	}

	/**
	 * The {@link CredentialRest} resource.
	 */
	@Bean
	public CredentialRest credentialRest(CredentialService svc) {
		return new CredentialRest(svc);
	}

	/**
	 * Registers {@link RootRest} as a servlet at {@code /rest/*}.
	 */
	@Bean
	public ServletRegistrationBean<Servlet> rootRestRegistration(RootRest rest) {
		return new ServletRegistrationBean<>(rest, "/rest/*");
	}

	/**
	 * The shared console-ui chrome stylesheet + themeable logo/page-background assets, mounted independently of
	 * {@code /rest/*} so the site-absolute {@code /juneau-console/*} URLs every tab's {@code base.ftlh} links
	 * against resolve the same way regardless of which tab rendered the page.
	 */
	@Bean
	public ServletRegistrationBean<Servlet> consoleAssetsRegistration() {
		return new ServletRegistrationBean<>(new ConsoleAssetsRest(), "/juneau-console/*");
	}

	// ==========================================================================================
	// Release tab wiring.
	// ==========================================================================================

	/**
	 * The release target's centralized endpoints. Canonical Apache Juneau production values, including
	 * real {@code repository.apache.org} for Nexus.
	 */
	@Bean
	public TargetProfile targetProfile() {
		return TargetProfile.prodDefault();
	}

	/**
	 * The stored secrets as the environment variables release.py reads.
	 */
	@Bean
	public CredentialEnv credentialEnv(Map<CredentialSpec, SecretStore> stores, AccountStore accounts) {
		return new CredentialEnv(stores, accounts);
	}

	/**
	 * The Release page's event bus: run-file changes and process start/end.
	 */
	@Bean
	public EventBroadcaster runEvents() {
		return new EventBroadcaster();
	}

	/**
	 * Runs {@code scripts/release.py}, one process at a time. On shutdown, a live process's attempt is marked
	 * {@code jrm-stopped} before it is closed, so History can say why it ended.
	 */
	@Bean(destroyMethod = "shutdown")
	public ReleaseProcessService releaseProcessService(@Value("${rm.home}") String home,
			@Value("${rm.scripts.dir}") String scriptsDir, @Value("${rm.juneau.terminal.script}") String runScript,
			CredentialEnv credentials, EventBroadcaster runEvents) {
		System.setProperty(TerminalProcess.SCRIPT_PROPERTY, runScript);
		return new ReleaseProcessService(Path.of(home), Path.of(scriptsDir), credentials::environment, runEvents);
	}

	/**
	 * Publishes a {@code run} event when any {@code run.json} changes, CLI runs included.
	 */
	@SuppressWarnings("resource") // Spring owns the bean and closes it via destroyMethod.
	@Bean(initMethod = "start", destroyMethod = "close")
	public RunFileWatcher runFileWatcher(@Value("${rm.home}") String home, EventBroadcaster runEvents) {
		return new RunFileWatcher(Path.of(home), runEvents);
	}

	/**
	 * The {@link ReleaseRunRest} resource.
	 */
	@Bean
	public ReleaseRunRest releaseRunRest(RunFiles runFiles, ReleaseProcessService processes) {
		return new ReleaseRunRest(runFiles, processes);
	}

	/**
	 * The SSE servlet: a plain HttpServlet registered alongside RootRest, serving the Release page's one channel at
	 * {@code /events/runs}.
	 */
	@Bean
	public ServletRegistrationBean<Servlet> runEventsRegistration(EventBroadcaster runEvents) {
		return new ServletRegistrationBean<>(new RunEventsSseServlet(runEvents), "/events/*");
	}
}
