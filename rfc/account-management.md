# Spinnaker Account Management

| | |
|-|-|
| **Status**    | _**Proposed**, Accepted, Implemented, Obsolete_       |
| **RFC #**      | https://github.com/spinnaker/governance/pull/159 |
| **Author(s)**  | Nicolas Cohen (@ncknt), Nima Kaviani (@nimakaviani) |
| **SIG / WG**   | Platform |

## Overview

Spinnaker reads account definitions (cloud providers, CI, metric stores, etc.) from Spring properties. This works well in a world with a handful of accounts that rarely change but causes operational pain when provisioning accounts dynamically or with account information stored externally.

This document proposes to add a thin account management layer in each Spinnaker service to support dynamic account management from non Spring data sources.


### Goals and Non-Goals

Goals:
- Provide an interface that each Spinnaker service can optionally implement
- Support Spring properties (as today)
- Adding account dynamically without having to restart the service
- Add stable interfaces that can pull accounts from a custom source
- Prepare Clouddriver for multiple credential repositories

Non Goals:
- Storing account information in Spinnaker
- Migrate all services and all providers


## Motivation and Rationale

Spinnaker reads account definitions (cloud providers, CI, metric stores, etc.) from Spring properties. This forces Spinnaker operators to restart services when an account is added, modified, or removed. The community is currently using [Spring Cloud Config](https://spinnaker.io/setup/configuration/) but it is complicated to set up and has significant performance issues as the number of accounts grow.

We're proposing to define interfaces to let each Spinnaker service support a variety of sources of account definitions as well as dynamically reload them.  We'll work in each service to add support - one provider at a time.

With this proposal implemented, operators of Spinnaker could easily:
- reload accounts dynamically across all types of accounts
- add a new account source from a specific provider
- extend the behavior of the `AccountRepository` across all types of accounts.
- add a new account source and get the account information from its source of truth.

We have evidence from our customers that some form of dynamic account management is needed.

## Timeline

Spinnaker 1.23 (1.22?):
- Add interfaces to kork 
- Start splitting AccountCredentialsRepository by provider (we'd now be able to use multiple accounts with the same name in different providers)
- AWS, Kubernetes support in Clouddriver.

Future releases:
- More services/providers as the community sees the need. We expect the change to be non invasive.

We have other providers in mind and are ready to contribute for these as well.


## Design

Here's a simple implementation of the interfaces (interfaces should be in kork): https://github.com/ncknt/clouddriver/compare/master...ncknt:feat/kork-accounts?expand=1

An `Account` contains the information necessary to access an account. It can be parsed and enriched into a `Credentials`. Spinnaker already has a lot of these: e.g. `com.netflix.spinnaker.clouddriver.security.AccountCredentials`, `com.netflix.spinnaker.clouddriver.artifacts.config.ArtifactCredentials`, `com.netflix.spinnaker.igor.service.BuildService`, etc.

An `AccountParser` translates an `Account` into an instance of `Credentials`. A `CredentialsRepository` contains one type of `Credentials`. Most of the time, we only need access to a single type of credentials (e.g. in clouddriver's providers). When access to different types of credentials is needed (e.g. in clouddriver's artifact), we can use a `CompositeCredentialsRepository`.

Each account repository is wired with an `AccountSource`. The source is queried once when the repository is created but can also be queried again if it is a `ReloadableAccountRepository` which the default implementation is.

If `accounts.dynamic.enabled`, then every `accounts.dynamic.[type of account].reloadFrequencyMs` the source will be queried, parsed, and merged into the repository.

A `CredentialsLifecycleHandler` handles enabling/disabling credentials (e.g. scheduling agents for a new provider account).

Out of the box, nothing should change for users. However, we could now write a plugin that for instance get Kubernetes accounts from a remote endpoint, or polls Jenkins master definitions from a database.

### Clouddriver Providers
Clouddriver currently uses one repository to store all accounts. Part of the proposal pushes each provider to use a `CredentialsRepository` defined above. It also uses the `AccountCredentialsProvider` abstraction.

For each provider, the migration looks like:
- Move from `AccountCredentialsProvider` to `CredentialsProvider<T extends AccountCredentials>`
- `CredentialsProvider` has the same methods except it is using generics
- Create the default `CredentialsRepository` bean
- Create the default `CredentialsProvider` bean

`clouddriver-elasticsearch` uses credentials across credentials and can be changed to use a `CompositeCredentialsRepository` that federates `CredentialsRepository` with non migrated provider accounts.

When all providers are migrated, we can delete `AccountCredentialsProvider`.

### Clouddriver Artifacts
Artifacts can be migrated by extending `CompositeAccountRepository` in `ArtifactCredentialsRepository` and registering all non migrated account.

### Reloading

The proposal would support pulling `AccountSource` on a given frequency via `accounts.reload.[type of account].frequencyReloadMs`. See `com.netflix.spinnaker.accounts.dynamic`

### Alternate Account Source

Because reusing the same format as Spring properties is so common, we also include an alternate account source (see `com.netflix.spinnaker.accounts.external`) to load that information from an endpoint - which could be a local file.

### Dependencies

No new dependencies. Each service for which this is implemented would need to add `kork-accounts` as a dependency.

## Drawbacks

In the short term, it adds a bit of complexity to credentials. Also if there are other alternatives, we're not aware of.

## Prior Art and Alternatives

- Loading Spring configuration via Spring Cloud Config has been noted above. We found it to not perform well at scale despite recent improvements and be hard to extend/operate. Synchronizers still need to be written to apply the change of properties to a running instance of the service.
- We considered keeping a global repository for all credentials in Clouddriver. Loading from separate source was becoming challenging. Querying unknown accounts (which is another goal not captured explicitly but that is enabled by this change) was also impossible if accounts have multiple sources. Finally, we couldn't see any obvious reason for not being able to define two accounts with the same name in different providers.
- `dynamicConfig.files` is an alternative that was added to bypass Spring Cloud Config. It still relies on Spring properties. It can reload any Spring property but as far as we know it's only used in the wild to reload accounts. It'd probably be acceptable to remove after deprecation.

We are not trying to deprecate anything but hope to provide a simple interface for which various plugins could be written and shared in the community.

## Known Unknowns

We'd like to validate the approach with the community. The first implementation will (re)load some of the cloud providers via an external source. We have immediate plans for a plugin to do that.


## Security, Privacy, and Compliance

This is a consideration for plugin authors.

## Operations

No extra work is expected.

## Risks

The main risk is that we missed some particular usage pattern or that there are existing plugins/tooling built on top of the account repository pattern.

## Future Possibilities

A natural extension would be to manage accounts in a Spinnaker service (e.g. Front50). It was actually considered for this proposal to provide a push system but turned out to be really more complex.

It's also possible to discover accounts via a cloud provider APIs.
