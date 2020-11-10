# Spinnaker OSS Roadmap

This OSS Roadmap outlines high-level deliverables and is broken down into loosely-bound buckets (current, near-term, and incubation), rather than being bucketed by specific release versions.
The deliverables below are a combination of independent contributions from private organizations and a rollup of SIG-specific roadmaps.

In advance of [the 2019 summit](https://www.spinnakersummit.com/), we have also
put together a succinct list of what the major contributors supporting Spinnaker are
[working on in the first half of 2020](h1-2020-roadmap.md).

For Spinnaker Release deliverables, please see [Releases](https://github.com/spinnaker/spinnaker/projects?utf8=%E2%9C%93&query=is%3Aopen+Release) on GitHub.

- [Active Investment](#active-investment)
  - [Increase Adoption](#increase-adoption)
  - [Spinnaker-as-a-Platform](#spinnaker-as-a-platform)
  - [Multi-Location Active-Active](#multi-location-active-active)
  - [Dynamic External Configuration](#dynamic-external-configuration)
  - [Kubernetes](#kubernetes)
  - [Amazon ECS](#amazon-ecs)
  - [Spinnaker-as-Code](#spinnaker-as-code)
  - [Security](#security)
  - [UI/UX](#ui/ux)
- [Future Investment](#future-investment)
  - [Increasing Adoption](#increasing-adoption)
- [Internal Investment](#internal-investment)
  - [Groovy Deprecation](#groovy-deprecation)
  - [Spectator to Micrometer Migration](#spectator-to-micrometer-migration)
  - [SQL as a Common Datastore](#sql-as-a-common-datastore)


## Active Investment

_Themes that are currently under active development or continued iteration._

### Increase Adoption

**Synopsis**: _Reduce on-boarding time and effort to install, configure, and integrate with Spinnaker._

- **Lead**: Armory
- **Beneficiary**: Evaluators, operators, integrators
- **Area of Investment**: Onboarding
- **Deliverables**:
  - Spinnaker Operator for Kubernetes

### Spinnaker-as-a-Platform

**Synopsis**: _Spinnaker was designed with pluggability in mind; indeed, such extensibility has enabled new cloud providers beyond AWS such as Titus and new custom stages._ 
_This process could be made easier by investing in more strongly typed APIs and documentation._

- **Lead**: Netflix
- **Beneficiary**: Spinnaker developers, orgs integrating with Spinnaker
- **Area of Investment**: Development extensibility
- **Deliverables**:
  - Plugin system
  - V3 public API
  - V2 event stream

### Multi-Location Active-Active

**Synoposis**: _Our intent is to run Spinnaker's various services out of multiple datacenters in an active-active fashion so as to provide resilience in the event of a major datacenter outage_.

- **Lead**: Netflix
- **Beneficiary**: Spinnaker operators, Netflix-internal customers
- **Area of Investment**: Infrastructure
- **Deliverables**:
  - Capability of running all services across multiple locations
  - Cross-region data replication & recovery

### Dynamic External Configuration

**Synopsis**: _Today, most configuration changes to Spinnaker requires a redeployment of the affected service(s). We want to enable our services to have more runtime-, externally-defined configurations that can be changed without service deployments._

- **Lead**: Pivotal, Armory
- **Beneficiary**: Spinnaker operators
- **Area of Investment**: Infrastructure
- **Deliverables**:
  - Integration with Spring Config Server
  - Dynamic Kubernetes accounts
  - Configuration UI

### Kubernetes

**Synoposis**: _Continued investment in Kubernetes._

- **Lead**: Kubernetes SIG
- **Beneficiary**: Kubernetes users
- **Area of Investment**: Cloud Providers
- **Deliverables**:
  - Performance enhancements
  - Improved isolation between accounts and startup time
  - Partially apply manifests

### Amazon ECS

**Synopsis**: _Continued investment in Amazon ECS provider._

- **Lead**: AWS SIG
- **Beneficiary**: Amazon ECS users
- **Area of Investment**: Cloud Providers
- **Deliverables**:
  - Add support for configuring capacity provider strategies
  - Contribute end to end testing resources (per [RFC](https://github.com/spinnaker/governance/blob/master/rfc/ecs_e2e_tests.md))

### Spinnaker-as-Code

**Synoposis**: _Continued investment in Managed Pipeline Templates V2 and early development of Managed / Declarative Delivery._

- **Lead**: Spinnaker-as-Code SIG
- **Beneficiary**: Users, integrators
- **Area of Investment**: Overall
- **Deliverables**:
  - MPTv2: Enhanced UX around versioning and template editing in UI
  - MPTv2: Support for complex workflows with SpinCLI and Sponnet
  - Managed Delivery: Support for basic AWS & Titus resources
  - Managed Delivery: Support for constraints & multi-environment promotion

### Security

**Synoposis**: _We are focusing on setting up poilicies and procedures for Security incidents and safeguards, dealing with identified vulnerabilities, and expanding authorization models within Spinnaker._

- **Lead**: Security SIG
- **Beneficiary**: Overall
- **Area of Investment**: Security
- **Deliverables**:
  - Defining a process for incoming security vulnerabilities
  - Setup of automated vulnerability scanning of Spinnaker BOM dependencies
  - Improving Fiat integration tests
  - Expanding RBAC for execution permissions to Pipelines
  - RBAC control for CI systems, inbound Pub/Sub
  - RBAC for MPTv2

### UI/UX

**Synoposis**: _Make it easier for community members to contribute to Deck and improve health of the codebase._

- **Lead**: UI/UX SIG
- **Beneficiary**: Spinnaker developers
- **Area of Investment**: Tools
- **Deliverables**:
  - Development best practices documentation
  - Storybook with interactive examples of reusable components
  - Migration from Angular to React
  - Improvement of package/module/build system
  - Avenue to showcase custom extensions of Deck

## Future Investment

_Themes that have not started development but are slated to be started soon._

### Increasing Adoption

**Synoposis**: _Accelerate adoption by showcasing best-practice pipelines and community usage stats._

- **Lead**: Armory
- **Beneficiary**: Evaluators, operators, users
- **Area of Investment**: Onboarding
- **Deliverables**
  - Community stats and dashboards
  - Pre-installed demo pipelines
  - Improve contextual help messages in UI

## Internal Investment

_Themes that are on-going, internal investments on technical debt, etc._
_These investments are contributed to by all organizations and are lead by the Technical Oversight Committee._

### Groovy Deprecation

**Synopsis**: _Groovy has been deprecated and new Groovy code is no longer accepted. Code is being actively migrated to Java or Kotlin._

### Spectator to Micrometer Migration

**Synopsis**: _Spring Boot 2 has standardized around the Micrometer project for metrics collection._
_This is roughly API-compatible with Spectator, Netflix's metrics collection library, and will be incrementally replaced as the project continues._
