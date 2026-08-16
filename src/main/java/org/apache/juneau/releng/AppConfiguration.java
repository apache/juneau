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
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.ApacheLdapValidator;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.releng.credential.GithubTokenValidator;
import org.apache.juneau.releng.credential.GpgValidator;
import org.apache.juneau.releng.credential.Validator;
import org.apache.juneau.releng.email.EmailService;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.engine.BranchResolver;
import org.apache.juneau.releng.engine.DropRcService;
import org.apache.juneau.releng.engine.ExecutionMode;
import org.apache.juneau.releng.engine.ReleaseEngine;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.StepRegistry;
import org.apache.juneau.releng.log.LogBroadcaster;
import org.apache.juneau.releng.log.SseLogServlet;
import org.apache.juneau.releng.milestone.GithubPrSource;
import org.apache.juneau.releng.milestone.MilestoneService;
import org.apache.juneau.releng.nexus.NexusMockRest;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.release.GitTagReleaseSource;
import org.apache.juneau.releng.release.GithubReleaseSource;
import org.apache.juneau.releng.release.LocalStateReleaseSource;
import org.apache.juneau.releng.release.ReleaseListService;
import org.apache.juneau.releng.rest.CredentialRest;
import org.apache.juneau.releng.rest.HomeRest;
import org.apache.juneau.releng.rest.MilestoneRest;
import org.apache.juneau.releng.rest.ReleaseRest;
import org.apache.juneau.releng.rest.ReleaseRunRest;
import org.apache.juneau.releng.util.ProcessRunner;
import org.apache.juneau.secret.keychain.KeychainSecretStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Servlet;

@Configuration
@SuppressWarnings({ "java:S6539" // Spring @Configuration legitimately aggregates cohesive bean wiring; splitting would fragment it.
})
public class AppConfiguration {

	@Bean
	public ProcessRunner processRunner() {
		return new ProcessRunner.Default();
	}

	/**
	 * FreeMarker engine configuration picked up by the Juneau view bridge (via BeanStore lookup).
	 *
	 * <p>Mirrors the bridge-default (classpath loader rooted at {@code /templates}, UTF-8, HTML output,
	 * cached templates) but enables {@code exposeFields} so the public-field view beans
	 * ({@code Release}, {@code CredentialStatus}) are readable from templates — the default object
	 * wrapper only exposes bean getters, which these beans intentionally don't have.
	 *
	 * <p>FreeMarker's {@code Configuration} is referenced by fully-qualified name because its simple name
	 * collides with Spring's {@link Configuration @Configuration} annotation used on this class.
	 */
	@Bean
	public freemarker.template.Configuration freemarkerConfiguration() {
		var cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_34);
		cfg.setClassLoaderForTemplateLoading(AppConfiguration.class.getClassLoader(), "templates");
		cfg.setDefaultEncoding("UTF-8");
		cfg.setOutputFormat(freemarker.core.HTMLOutputFormat.INSTANCE);
		cfg.setTemplateUpdateDelayMilliseconds(Long.MAX_VALUE);
		var owb = new freemarker.template.DefaultObjectWrapperBuilder(freemarker.template.Configuration.VERSION_2_3_34);
		owb.setExposeFields(true);
		cfg.setObjectWrapper(owb.build());
		return cfg;
	}

	@Bean
	public ReleaseListService releaseListService(ProcessRunner runner, @Value("${rm.repo.dir}") String repoDir,
			@Value("${rm.repo.slug}") String repoSlug, @Value("${rm.state.dir}") String stateDir) {
		var tags = new GitTagReleaseSource(runner, repoDir);
		var github = new GithubReleaseSource(runner, repoSlug);
		var state = new LocalStateReleaseSource(Path.of(stateDir));
		return new ReleaseListService(tags::list, github::list, state::list);
	}

	@Bean
	public RootRest rootRest() {
		return new RootRest();
	}

	@Bean
	public HomeRest homeRest() {
		return new HomeRest();
	}

	@Bean
	public ReleaseRest releaseRest(ReleaseListService svc) {
		return new ReleaseRest(svc);
	}

	@Bean
	public GithubPrSource githubPrSource(ProcessRunner runner, @Value("${rm.repo.slug}") String repoSlug) {
		return new GithubPrSource(runner, repoSlug);
	}

	@Bean
	public MilestoneService milestoneService() {
		return new MilestoneService();
	}

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

	@Bean
	public CredentialService credentialService(Map<CredentialSpec, SecretStore> stores, ProcessRunner runner,
			AccountStore accounts) {
		var validators = new EnumMap<CredentialSpec, Validator>(CredentialSpec.class);
		validators.put(CredentialSpec.APACHE_LDAP, new ApacheLdapValidator());
		validators.put(CredentialSpec.GPG, new GpgValidator(runner));
		validators.put(CredentialSpec.GITHUB, new GithubTokenValidator(runner));
		return new CredentialService(stores, validators, accounts);
	}

	@Bean
	public CredentialRest credentialRest(CredentialService svc) {
		return new CredentialRest(svc);
	}

	@Bean
	public ServletRegistrationBean<Servlet> rootRestRegistration(RootRest rest) {
		return new ServletRegistrationBean<>(rest, "/rest/*");
	}

	// ==========================================================================================
	// Release-orchestration engine and New Release tab wiring.
	// ==========================================================================================

	/**
	 * The box-wide execution mode. Defaults to SAFE; only {@code rm.mode=live} enables real mutation (and
	 * even then, per-run arming is still required at the guard chokepoint).
	 */
	@Bean
	public ExecutionMode executionMode(@Value("${rm.mode:safe}") String mode) {
		return "live".equalsIgnoreCase(mode) ? ExecutionMode.LIVE : ExecutionMode.SAFE;
	}

	/**
	 * The release target's centralized endpoints. Everything is the canonical Apache Juneau production value
	 * except the Nexus base, which is <b>mode-derived</b>: the in-app {@code /mock/nexus} loopback under SAFE,
	 * real {@code repository.apache.org} under LIVE. This is what makes the 3 Tier-A Nexus staging callouts
	 * hit the mock in SAFE and real Nexus in LIVE without any per-step branching.
	 */
	@Bean
	public TargetProfile targetProfile(ExecutionMode mode, @Value("${server.address:127.0.0.1}") String address,
			@Value("${server.port:8790}") int port) {
		var base = mode == ExecutionMode.LIVE ? TargetProfile.prodDefault().nexusBaseUrl()
				: "http://" + address + ":" + port + "/mock/nexus";
		return TargetProfile.prodDefault().withNexusBaseUrl(base);
	}

	@Bean
	public RunStateStore runStateStore(@Value("${rm.state.dir}") String stateDir) {
		return new RunStateStore(Path.of(stateDir));
	}

	@Bean
	public BranchResolver branchResolver(ProcessRunner runner, @Value("${rm.repo.dir}") String repoDir) {
		return new BranchResolver(runner, repoDir);
	}

	@Bean
	public EmailService emailService(ProcessRunner runner, @Value("${rm.state.dir}") String stateDir) {
		return new EmailService(Path.of(stateDir), runner);
	}

	/** Resolves live secrets from CredentialService's stores per mutating action. */
	@Bean
	public ReleaseEngine.SecretResolver secretResolver(Map<CredentialSpec, SecretStore> stores, AccountStore accounts,
			ExecutionMode mode, TargetProfile target) {
		return new ReleaseEngine.SecretResolver() {
			private String read(CredentialSpec spec, String account) {
				return stores.get(spec).find(account).map(String::new).orElse("");
			}

			// Precedence: the rm.availid/rm.gpg.keyid system property (headless/testing override) wins when
			// set, else the AccountStore-persisted value (survives restart), else "".
			@Override
			public String availid() {
				var sys = System.getProperty("rm.availid", "");
				return !sys.isBlank() ? sys : accounts.get(CredentialSpec.APACHE_LDAP).orElse("");
			}

			@Override
			public String ldapPassword() {
				return read(CredentialSpec.APACHE_LDAP, availid());
			}

			@Override
			public String gpgKeyId() {
				var sys = System.getProperty("rm.gpg.keyid", "");
				return !sys.isBlank() ? sys : accounts.get(CredentialSpec.GPG).orElse("");
			}

			@Override
			public String gpgPassphrase() {
				return read(CredentialSpec.GPG, gpgKeyId());
			}

			@Override
			public String githubToken() {
				return read(CredentialSpec.GITHUB, "token");
			}

			// Nexus credentials are the committer's Apache LDAP identity — the same Keychain-backed
			// SecretStore entry as ldapPassword() above, because an encrypted <password> in settings.xml
			// cannot be used directly. Falls back to ~/.m2/settings.xml only when that Keychain entry
			// hasn't been stored yet. Under SAFE the base is the loopback mock and the credential is a
			// throwaway placeholder — the real Keychain secret is never read or sent (OQ-C).
			@Override
			public NexusStagingClient nexus() {
				if (mode == ExecutionMode.SAFE)
					return NexusStagingClient.create(target.nexusBaseUrl(), target.nexusProfileId(), "safe-placeholder",
							"safe-placeholder");
				return nexusClient(target, availid(), ldapPassword(), () -> NexusStagingClient
						.create(target.nexusBaseUrl(), target.nexusProfileId(), "apache.releases.https"));
			}
		};
	}

	/**
	 * Builds the LIVE Nexus client from Keychain-backed credentials (via the {@link SecretStore} SPI, per
	 * {@code availid}/{@code ldapPassword} above) when both are present, else defers to
	 * {@code settingsXmlFallback}. The base URL + profile id come from the (mode-derived) {@link TargetProfile}.
	 * Package-private for {@code AppConfigurationTest}.
	 */
	static NexusStagingClient nexusClient(TargetProfile target, String availid, String ldapPassword,
			Supplier<NexusStagingClient> settingsXmlFallback) {
		if (availid.isBlank() || ldapPassword.isBlank())
			return settingsXmlFallback.get();
		return NexusStagingClient.create(target.nexusBaseUrl(), target.nexusProfileId(), availid, ldapPassword);
	}

	@Bean
	public StepRegistry stepRegistry(BranchResolver branches) {
		return StepRegistry.standard(branches);
	}

	@Bean
	@SuppressWarnings({ "java:S107" // Constructor-injected collaborators; a parameter object would obscure the wiring.
	})
	public ReleaseEngine releaseEngine(RunStateStore store, StepRegistry registry, ProcessRunner runner,
			BranchResolver branches, EmailService email, MilestoneService milestone,
			ReleaseEngine.SecretResolver secrets, ExecutionMode mode, TargetProfile target,
			@Value("${rm.state.dir}") String stateDir, @Value("${rm.staging.dir}") String stagingDir,
			@Value("${rm.repo.dir}") String repoDir, @Value("${rm.git.committer.email}") String committerEmail) {
		var engine = new ReleaseEngine(store, registry, runner, branches, Path.of(stateDir), Path.of(stagingDir),
				repoDir, committerEmail, email, milestone, secrets, mode, target);
		engine.recoverOnBoot(); // Demote runs left mid-flight by a previous process on restart.
		return engine;
	}

	@Bean
	public DropRcService dropRcService(RunStateStore store, StepRegistry registry, ProcessRunner runner,
			ReleaseEngine.SecretResolver secrets, ReleaseEngine engine, ExecutionMode mode, TargetProfile target,
			@Value("${rm.staging.dir}") String stagingDir, @Value("${rm.state.dir}") String stateDir) {
		return new DropRcService(store, registry, runner, Path.of(stagingDir).resolve("git/juneau"), Path.of(stateDir),
				secrets.nexus(), mode, engine::isArmed, target, engine::broadcaster);
	}

	@Bean
	public ReleaseRunRest releaseRunRest(ReleaseEngine engine, DropRcService dropRc) {
		return new ReleaseRunRest(engine, dropRc);
	}

	/**
	 * The SAFE-only Nexus loopback mock (OQ-D): a dedicated Juneau REST resource that lets the real
	 * {@link NexusStagingClient} run its full staging flow against an in-memory model with zero canonical
	 * side effects. Present only when {@code rm.mode=safe} (default), so it can never serve a LIVE run.
	 */
	@Bean
	@ConditionalOnProperty(name = "rm.mode", havingValue = "safe", matchIfMissing = true)
	public NexusMockRest nexusMockRest(TargetProfile target) {
		return new NexusMockRest(target.nexusProfileId());
	}

	@Bean
	@ConditionalOnProperty(name = "rm.mode", havingValue = "safe", matchIfMissing = true)
	public ServletRegistrationBean<Servlet> nexusMockRegistration(NexusMockRest mock, ReleaseEngine engine) {
		// Each new run resets the mock so the lazily-synthesized staging repo starts from a clean slate.
		engine.setRunStartHook(() -> mock.model().reset());
		return new ServletRegistrationBean<>(mock, "/mock/nexus/*");
	}

	/**
	 * The SSE servlet — a plain HttpServlet, registered alongside RootRest. Keyed by
	 * {@code (version, stepId)} rather than {@code version} alone, and resolves each step's log path from
	 * its {@code StepState.logRef} rather than a run-level {@code logFile}.
	 */
	@Bean
	public ServletRegistrationBean<Servlet> sseLogRegistration(ReleaseEngine engine, RunStateStore store) {
		var servlet = new SseLogServlet(logPathForStep(store), broadcasterForStep(engine, store));
		return new ServletRegistrationBean<>(servlet, "/events/*");
	}

	/**
	 * That step's on-disk log path, or empty when there's no persisted run for {@code version} or that run
	 * has no such {@code stepId}. Package-private for {@code AppConfigurationTest}.
	 */
	static BiFunction<String, String, Optional<Path>> logPathForStep(RunStateStore store) {
		return (version, stepId) -> store.load(version).map(rs -> rs.step(stepId))
				.filter(ss -> ss != null && ss.logRef != null).map(ss -> store.stateDir().resolve(ss.logRef));
	}

	/**
	 * That step's live {@link LogBroadcaster}, or empty when there's no persisted run for {@code version}
	 * or that run has no such {@code stepId}. Returning empty (rather than always minting a broadcaster via
	 * {@code engine.broadcaster()}'s {@code computeIfAbsent}) lets {@code SseLogServlet} take its immediate
	 * "(no active run/step …)" branch for a bogus key instead of blocking a worker thread for the full
	 * heartbeat interval. Package-private for {@code AppConfigurationTest}.
	 */
	static BiFunction<String, String, Optional<LogBroadcaster>> broadcasterForStep(ReleaseEngine engine,
			RunStateStore store) {
		return (version, stepId) -> store.load(version).filter(rs -> rs.step(stepId) != null)
				.map(rs -> engine.broadcaster(version, stepId));
	}
}
