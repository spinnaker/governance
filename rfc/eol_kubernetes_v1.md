# Kubernetes V1 Provider Removal

| | |
|-|-|
| **Status**     | _Proposed, Accepted, **Implemented**, Obsolete_ |
| **RFC #**      | [88](https://github.com/spinnaker/governance/pull/88) |
| **Author(s)**  | Maggie Neterval (`@maggieneterval`) |
| **SIG / WG**   | Kubernetes SIG |

## Overview

Spinnaker currently supports both a V1 ("Legacy") and V2 ("Manifest-based") Kubernetes provider.
We should remove Spinnaker's V1 Kubernetes provider in Spinnaker version 1.21.
Supporting only one provider will improve the user experience for Spinnaker evaluators
and focus the Spinnaker/Kubernetes community on improving the V2 provider.

### Goals

- Describe why we should remove Spinnaker’s Kubernetes V1 provider.
- Propose a plan for how we will remove the provider, including a timeline for communicating the plan to the community, auditing and updating relevant documentation, and pruning V1-only code.

### Non-Goals

- Compare the user experience, implementation details, or scaling characteristics of the V1 and V2 providers.
This RFC makes the assumption that the V2 provider is the Spinnaker community’s current recommendation for deploying to Kubernetes with Spinnaker.


## Motivation and Rationale

- **Improve the user experience for Spinnaker evaluators.**
 As a Spinnaker evaluator, I am less likely to trust the maturity of a V2 solution if a V1 solution continues to be maintained.
 I am also likely to be confused by an apparent mismatch between Spinnaker documentation and what I see in the UI, due to uncertainty around which Kubernetes provider I have actually installed.
- **Focus Spinnaker/Kubernetes community on active areas of development**.
The Spinnaker Kubernetes SIG and Slack channels operate best with a common foundation from which to work, one that reflects the opinions and best practices of our community.
- **The user experience for existing V1 users is poor.**
  Existing V1 users receive almost no support or up-to-date content from the Spinnaker community.
  Additionally, they encounter bugs and performance problems that are unlikely to be resolved.
  We have made significant user experience enhancements and performance improvements in the V2 provider, and will continue to invest solely in V2 for Kubernetes users.
- **Reduce the maintenance burden for Spinnaker developers.**
  Currently, the V1 provider is in maintenance mode: although we are no longer adding or accepting enhancements, we are still responsible for triaging and fixing bugs and upgrading dependencies.
  This is a non-trivial amount of overhead that subtracts from the time we can commit to improving the V2 provider.

## Timeline

Each set of milestones should be complete by the time the corresponding Spinnaker or Halyard version is released.

| Spinnaker Version | Release Window Opens | Milestones |
|-------------------|----------------------|------------|
| 1.18              | 2020-01-06           | Gain consensus on this RFC. Highlight in 1.18 curated changelog that V1 will be removed in 1.21.
| 1.19              | 2020-03-02           | Publish a post to the Spinnaker Community Blog explaining why we are removing V1 and how to migrate to V2. Add a feature flag that must be enabled before deploying to V1 accounts. Add appropriate warnings to Halyard, Clouddriver, and Deck. Highlight in 1.19 curated changelog that V1 will be removed in 1.21.
| 1.20              | 2020-04-27           | Complete audit of community docs and blog posts for V1-related content to update. Highlight in 1.20 curated changelog that V1 will be removed in 1.21.
| 1.21              | 2020-06-22           | Remove V1-only code from all Spinnaker microservices.

| Halyard Version | Supported Spinnaker Versions | Milestones |
|-----------------|------------------------------|------------|
| 1.32            | 1.17, 1.18, 1.19             | Add warning for any configured legacy Kubernetes accounts. Default new and unspecified accounts to standard (V2) provider.
| 1.36            | 1.18, 1.19. 1.20             | Remove legacy Kubernetes distributed installation path. Spinnaker itself must be deployed using a standard (V2) account.
| TBD             | 1.21, 1.22, 1.23             | Stop exposing `--provider-version` command, as no supported release will include support for the legacy (V1) provider.

## Design

- [x] Determine if there are user stories addressed by the V1 provider but not the V2 provider that we should consider adding to V2 before removing V1.
  - [x] Identify and interview existing V1 users in order to learn why they are still using the V1 provider (Thank you to everyone who took the time to speak with us!).
  - [x] Identify and interview people currently evaluating Spinnaker in order to learn if the level of abstraction exposed by V1’s wizards would have value as an onboarding tool in V2
  (Short answer: most users think that manifest composition is now out of scope for Spinnaker because there is now such a rich ecosystem of Kubernetes-native tooling for manifest templating).

- [x] Change documentation to encourage new users to adopt only the V2 provider ([spinnaker.github.io/pull/1586](https://github.com/spinnaker/spinnaker.github.io/pull/1586)).
  - [x] Change the docs to explicitly endorse V2 as the recommended provider ([spinnaker.github.io/pull/1586](https://github.com/spinnaker/spinnaker.github.io/pull/1586)).
  - [x] Remove the V1 provider codelab ([spinnaker.github.io/pull/1586](https://github.com/spinnaker/spinnaker.github.io/pull/1586)). Add a redirect from the V1 codelab to the V2 codelab ([spinnaker.github.io/pull/1649](https://github.com/spinnaker/spinnaker.github.io/pull/1649)).
  - [x] Remove inbound links to V1 provider setup ([spinnaker.github.io/pull/1649](https://github.com/spinnaker/spinnaker.github.io/pull/1649)).
  
- [x] Publish a [blog post](https://blog.spinnaker.io/farewell-to-the-kubernetes-v1-provider-79d93861c6e4) on the Spinnaker Community Blog. This post will answer the following questions:
  - [x] Why is Spinnaker removing the V1 provider?
  - [x] When is Spinnaker removing the V1 provider? What is the latest supported release I will be able to use with the V1 provider enabled?
  - [x] How can I successfully execute a zero-downtime migration from the V1 provider to the V2 provider? What are some first-hand lessons from teams who have done this?

- [x] Audit existing Spinnaker content to update.
  - [x] Ensure all guides on spinnaker.io reference V2 only ([spinnaker.github.io/pull/1649](https://github.com/spinnaker/spinnaker.github.io/pull/1649)).
  - [x] Standardize how we refer to the V2 provider (currently, mix of “V2” and “Manifest Based”) and the V1 provider (currently, mix of “V1” and “Legacy”) ([spinnaker.github.io/pull/1814](https://github.com/spinnaker/spinnaker.github.io/pull/1814)).
  - [x] Add documentation to spinnaker.io on why and when the V1 provider will be / was removed ([spinnaker.github.io/pull/1710](https://github.com/spinnaker/spinnaker.github.io/pull/1710)).
  - [x] Add an addendum to any blog posts on the Spinnaker Community Blog that reference the V1 provider highlighting that it is no longer supported ([spinnaker/spinnaker/5463](https://github.com/spinnaker/spinnaker/issues/5463)).
  - [x] Compile a list of Armory documentation and blog posts for Armory to update ([spinnaker/spinnaker/5463](https://github.com/spinnaker/spinnaker/issues/5463)).
  
- [x] Ensure clear and consistent communication of the plan:
  - [x] Highlight the upcoming removal in each release’s curated changelog leading up to the release in which V1 will be officially removed.
  - [x] Post about the RFC in relevant Slack channels before each release: #dev, #kubernetes, #sig-kubernetes.
  - [x] Post about the RFC in any new V1-related issues that are opened on GitHub.
  - [x] Add helpful warnings to Clouddriver and Halyard in the releases leading up to the removal.

- [x] Remove V1-only code.
  - [x] Audit each microservice to ensure no V2 code paths exercise V1 code.
    Note: ezimanyi@ and I have already ensured isolation between V1-only and V2-only code in Clouddriver ([clouddriver/pull/4024](https://github.com/spinnaker/clouddriver/pull/4024), [clouddriver/pull/4028](https://github.com/spinnaker/clouddriver/pull/4028)) and Deck ([deck/pull/7451](https://github.com/spinnaker/deck/pull/7451)).
  - [x] Remove V1 integration tests ([buildtool/pull/112](https://github.com/spinnaker/buildtool/pull/112)).
  - [x] Remove V1-only code from Deck ([deck/pull/8226](https://github.com/spinnaker/deck/pull/8226)).
  - [x] Remove V1-only code from Orca ([orca/pull/3654](https://github.com/spinnaker/orca/pull/3654)).
  - [x] Remove V1-only code from Clouddriver ([clouddriver/pull/4569](https://github.com/spinnaker/clouddriver/pull/4569), [clouddriver/pull/4572](https://github.com/spinnaker/clouddriver/pull/4572)).
  - [x] Since no providers now support multiple versions, and no community members (to the best of our knowledge) ever implemented their own UI "skins," remove "skins" from Deck ([deck/pull/8263](https://github.com/spinnaker/deck/pull/8263)), Gate ([gate/pull/1186](https://github.com/spinnaker/gate/pull/1186)), Clouddriver ([clouddriver/pull/4576](https://github.com/spinnaker/clouddriver/pull/4576)), and the docs ([spinnaker.github.io/pull/1838](https://github.com/spinnaker/spinnaker.github.io/pull/1838)).
  - [x] Remove V1 distributed installation path from Halyard ([halyard/pull/1672](https://github.com/spinnaker/halyard/pull/1672)).
  - [x] When Spinnaker 1.23 is released, no supported release will include the V1 provider.
    Since Kubernetes was the only provider to expose multiple versions, it will then be safe to remove `providerVersion` logic from Halyard and Clouddriver.
    Create a self-assigned issue for this final cleanup ([spinnaker/spinnaker/5749](https://github.com/spinnaker/spinnaker/issues/5749)).

## Drawbacks

- We will be taking away a feature set that some community members currently rely on for critical delivery workflows.
  Migrating from the V1 to the V2 provider will be a significant lift for Spinnaker operators. 

## Prior Art and Alternatives

- The alternative to EOLing the V1 provider is taking no action and keeping it in "maintenance mode."
However, I think it is important for the health of the Spinnaker community that we thoughtfully but explicitly EOL feature sets that no longer represent
a high-quality user experience and reflect our community's opinions and best practices.
We want all Spinnaker/Kubernetes users to benefit from our continued investment in both the code and community of the V2 provider.

## Known Unknowns

- Are there user stories supported by the V1 provider but not the V2 provider that we failed to consider?
- Are there questions existing V1 users will have about migrating to V2 that we will fail to anticipate?

*Note: If you currently use the V1 provider and would like to share thoughts on this RFC,
or you have successfully migrated from the V1 to the V2 provider and would like to be featured
in a Spinnaker community blog post, please feel free to comment here or reach out
to me directly on Slack (@maggieneterval).*
