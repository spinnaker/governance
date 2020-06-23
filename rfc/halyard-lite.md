# Replacement of Halyard

|               |                                                       |
| ------------- | ----------------------------------------------------- |
| **Status**    | _Proposed, **Accepted**, Implemented, Obsolete_       |
| **RFC #**     | [89](https://github.com/spinnaker/governance/pull/89) |
| **Author(s)** | Eric Zimanyi (@ezimanyi)                              |
| **SIG / WG**  | Kubernetes SIG                                        |

## Overview

The current recommended way to deploy Spinnaker is by using Halyard. While
Halyard has greatly simplified some aspects of installing and configuring
Spinnaker, it has a number of shortcomings that are particularly apparent when
deploying to Kubernetes. This document proposes replacing Halyard with a smaller
tool called _kleat_ that integrates better into the Kubernetes ecosystem.

### Goals and Non-Goals

Goals:

- Improve the install process for users deploying to Kubernetes by providing an
  install path that is Kubernetes-native and integrates well with the Kubernetes
  ecosystem
- Reduce the maintenance burden of Halyard, in particular by reducing the toil
  required to support new configuration parameters

Non-goals:

- This document focuses primarily on the motivation behind replacing Halyard
  with a smaller tool as well as the broad design and surface of of this tool. A
  more detailed technical design of this tool is not in scope for this document.
- While this document focuses on making the experience of deploying Spinnaker to
  Kubernetes easier, it does not intend to deprecate or eliminate the ability to
  deploy Spinnaker to other platforms.

## Motivation and Rationale

The primary purpose of Halyard is to simplify the process of configuring,
updating, and operating Spinnaker.

Without Halyard, installing Spinnaker involves:

- Determining the version of each microservice you’d like to deploy. In general,
  this would involve picking a top-level Spinnaker version (ex: 1.16.1) and
  looking up the version of each microservice tied to that top-level version.
- Writing a `service.yml` config file for each microservice containing the
  configuration you want for your Spinnaker installation, and copy it to the
  correct directory.
  - The configuration parameters accepted by each microservice are in general
    not well documented, so writing these config files would require a fairly
    detailed understanding of each microservice.

Installing to a Kubernetes cluster has the additional complexity of:

- Adding each service’s config file as a `ConfigMap` to your cluster
- Adding any files referenced in the service configs as `ConfigMap`s or
  `Secret`s to the cluster.
- Writing a workload (`Deployment`/`ReplicaSet`) manifest for each microservice,
  making sure to mount the appropriate configuration into each workload.
- Writing a `Service` for any workloads (deck, gate) that should be exposed
  outside of the cluster.

With Halyard, this process is greatly simplified. In particular, a user can
install Spinnaker to Kubernetes via:

- Configuring Halyard with the top-level Spinnaker version to install, without
  needing to know about the individual microservice versions
- Adding all configuration parameters to a single config file, either by editing
  the YAML directly or by running `hal config` commands
- Running `hal deploy apply` to deploy Spinnaker

While Halyard has greatly simplified the install process, it has a number of
shortcomings.

### Not Kubernetes Native

As Spinnaker positions itself as a Kubernetes-native deployment tool, operators
are expecting to deploy and operate Spinnaker using the same Kubernetes-native
tools they use for other software. In particular, the workflow for updating
Spinnaker using Halyard is to SSH to a VM (or a pod) and run a series of
imperative commands. Halyard's client/server nature does not lend itself well to
GitOps or Infrastructure as Code workflows; while some users have worked around
this, the process is error prone and difficult to automate.

The desire for a Kubernetes-native path to install Spinnaker has led to a number
of alternate install paths built on top of Halyard:

- A [Helm chart](https://github.com/helm/charts/tree/master/stable/spinnaker)
  that installs Halyard as a stateful set
  - While this has provided a Kubernetes-native way of installing Halyard, the
    process of running and maintaining Spinnaker itself still involves SSH’ing
    to the Halyard pod and running imperative commands
- An operator built by
  [OpsMx](https://operatorhub.io/operator/opsmx-spinnaker-operator)
  - This operator appears to have many of the same drawbacks as the Helm chart,
    in that it will initially configure a Spinnaker installation but many
    customizations still involve SSH’ing to a Halyard pod and running imperative
    commands.
- An operator built by [Armory](https://docs.armory.io/spinnaker/operator/)
  - This operator removes the need for users to run imperative commands to
    manage Spinnaker. It allows users to configure Spinnaker via CRUD on a
    SpinnakerService CRD that contains the full Halyard configuration, with the
    operator handling any redeployments from those changes.
  - While much of the surface area of Halyard is hidden from users of this
    operator, it is still built on top of Halyard.

### Duplication of Kubernetes-native tools

Users often have requirements for customizing the Kubernetes objects that
comprise a Spinnaker installation. For example, a common use case is to add a
toleration to some/all of the workloads so that they can run on specific node
pools.

As Halyard is responsible for both generating and deploying the YAML for all the
Kubernetes objects that comprise a Spinnaker installation, Halyard needs to
handle any customization that users want to apply. Maintaining all of these
possible customizations in Halyard adds a maintenance burden, as users
frequently open pull requests to add additional fields they would like on their
deployments/services. Changes to the templates that generate the YAML Halyard
deploys are among the most frequent causes of bugs, as there is no test coverage
on these files and the templating language (Jinja) is often not familiar to
contributors.

The problem of taking a set of YAML files that constitute a software
installation, and customizing them with various overrides is one that has
numerous solutions in the Kubernetes community, the most notable being
[Helm](https://helm.sh/) and
[Kustomize](https://github.com/kubernetes-sigs/kustomize). Ideally, Halyard
would not duplicate functionality that has existing solutions with much higher
adoption; this would reduce both the maintenance burden on Halyard and the
barrier to entry to Spinnaker for users familiar with these tools.

### Dynamic configuration

Recently, some Spinnaker microservices have been updated to enable the use of
dynamic configuration sources, leveraging Spring’s Cloud Config project. In
order to use this feature, users need the raw YAML config for the individual
service in a config source supported by Cloud Config. The current paradigm of
using Halyard to both generate and deploy config does not fit well with this
workflow, where users need to separately store their service configuration.

### Incompatibility with Kubernetes-native tools

There is currently no easy way to install/manage Spinnaker using either Helm or
Kustomize (excluding the above-mentioned Helm chart that effectively just
installs Halyard). The root cause is the duplication of functionality in the
prior section; as Halyard generates and applies YAML, there is no intermediate
representation that users can customize using these tools.

While the Armory operator provides a viable Kubernetes-native way to install
Spinnaker, there is still no Kubernetes-native install path for users who won’t
want or need an operator. Ideally it would be reasonably easy to write a Helm
chart and/or Kustomization that installs Spinnaker’s microservices and is
managed using idiomatic patterns.

### Complexity of implementation

Halyard is the third-largest of Spinnaker’s microservices by lines of code,
comprising 55435 lines of Java code. There is minimal test code, totalling 3094
lines of Java and Groovy code. The poor test coverage, combined with heavy use
of reflection and unchecked casts make refactoring difficult.

Halyard also consumes many of the other microservices as libraries. In general,
engineers modifying the other microservices don’t expect that they are modifying
a library consumed by other microservices. This has led to cases where changes
to clouddriver or front50 led to
[breaking Halyard](https://github.com/spinnaker/halyard/pull/1062) when trying
to bump the dependency.

As a representative example of the amount of code change required to add a new
config parameter to Halyard, consider the PR to add
[Google Pubsub config](https://github.com/spinnaker/halyard/pull/750). This is a
fairly representative example of a config parameter; it supports
add/edit/delete/get/list on a list of Google Pubsub subscriptions, and required
2244 lines of code to implement (none of which is test code).

The amount of code necessary to add simple config parameters is a burden both on
contributors and on the maintainers of Halyard who have to review this code.
This has led to Halyard’s config often lagging behind that of the individual
microservices. Ideally, adding a config parameter would be a reasonably simple
first contribution for someone looking to get involved with the project, but
that is currently very far from the case.

## Timeline

- Spinnaker 1.21 (June 2020)
  - An alpha version of kleat is released along with documentation, and is able
    to generate service configurations from the Halyard config.
  - A simple Kustomize kustomization is released that can deploy Spinnaker to
    Kubernetes in simple cases.
- Spinnaker 1.22 (August 2020)
  - Feedback from early users of kleat and the kustomization are addressed, and
    the kustomization now supports more advanced use cases
  - The Armory operator is moved to depend on kleat
- Spinnaker 1.23 (October 2020)
  - Kleat, as well as the kustomize install path are GA and the recommended
    install path for new users of Spinnaker.
  - Documentation (such as install documentation) referencing old Halyard
    commands is updated to reference kleat.
  - Based on adoption of kleat, a plan is formulated around how and when to
    deprecate and cease support of Halyard. The details of this plan will be a
    separate RFC.

## Design

### Removed and replaced features

#### Commands to edit the Halyard config

There are currently two ways to update the Halyard configuration:

- Edit the YAML directly
- Run `hal …` commands to update the config

YAML is ubiquitous in the Kubernetes world, and Kubernetes-native tools
generally assume that users are comfortable writing YAML configuration files. As
such, the value in providing a CLI that hides YAML from users is questionable,
and may even contribute to the perception that Spinnaker (or at least its
installation) is not Kubernetes-native. The CLI also encourages an imperative
workflow whereby configuration is achieved via a series hal commands.
Furthermore, a significant amount of the complexity and verbosity of Halyard’s
codebase comes from the commands to edit the Halyard configuration.

Based on these considerations, the CLI commands to edit the configuration will
be removed. As the
[hal command reference](https://www.spinnaker.io/reference/halyard/commands/) is
the best existing documentation on the available config parameters for
Spinnaker, it will be replaced by extensive documentation of a well-typed
Halyard configuration so that users can be confident directly editing this
config.

#### Generation and application of Kubernetes YAML

As discussed above, Halyard’s functionality to generate and apply Kubernetes
YAML duplicates functionality in existing tools and makes it incompatible with
these tools. It also makes it impossible for users to customize their
deployments without making code changes to Halyard to account for these
customizations.

For these reasons, Halyard will no longer handle generating and deploying
Kubernetes YAML. Instead, Halyard will focus on generating the service configs
from the Halyard config. We’ll provide a Helm chart and/or Kustomization that
consumes these service configs and generates/applies YAML to the cluster.

#### Input validation

Halyard currently does fairly strict validation of input when using the CLI; in
addition to validating the well-formedness of parameters, in some cases it also
uses the code from the other services to attempt to perform operations using the
parameters. (For example, when configuring a Google Compute account, Halyard
will try to make a test request to list instances during its validation step.)

The value of this validation is not clear when Halyard is running on a different
machine than the microservice in question. In many cases, network policies or
firewalls prevent the Halyard instance from communicating with external services
that individual microservices do have access to.

Halyard will reduce the scope of its validation efforts. It will focus on
validating the well-formedness of the Halyard config, but will no longer act as
the other microservices to try to actually build instances of credentials and
make requests using them. Validating the well-formedness of the config will
involve both:

- Validating the structure of the config
- Performing simple validation on the values supplied in the config, such as
  ensuring that a value is an integer or a valid URL

#### Secret management

Spinnaker now supports two ways of handling secrets in config files:

- Secrets encrypted using the Spinnaker-specific format supported by
  `kork-secrets`. Halyard is able to decrypt these secrets, and in some cases
  (ex: [deck](https://github.com/spinnaker/halyard/pull/1379)) writes
  unencrypted values to services.
- Support in the services themselves (via Spring Cloud Config) to decrypt
  externally-stored secrets

Halyard will no longer support decrypting secrets; users can continue to use
either secret encryption method with two exceptions:

- Halyard will no longer write unencrypted secrets to deck's config. Given that
  these are effectively not secrets once Halyard has written the unencrypted
  value to disk, users can store the unencrypted values directly in the Halyard
  config until deck supports decryption of encrypted secrets.
- Halyard will no longer decrypt secrets for the purpose of validating the
  config; a reduction of the scope of Halyard's validation is discussed in more
  detail in the previous section.

#### Config backup/restore

Halyard has functionality to backup the entire configuration to an archive file,
and to restore from such an archive file. This is useful either for backup
purposes, or to copy configuration from one Halyard installation to another.

In general, the canonical solution for backing up and tracking changes to
configuration in a Kubernetes-native world is to store this configuration in a
git repository. To avoid duplicating existing solutions, Halyard will no longer
have built-in support for backing up and restoring the configuration; it will
instead presume that the operator is using some external tool to version and
back up the configuration.

#### Version publishing/deprecating

Halyard handles publishing and deprecating Spinnaker versions; this
functionality is only used by maintainers of the project and requires having a
GCP service account with write permissions to the GCS bucket that stores
Spinnaker version info. It is possible to use this functionality to publish
custom BOMs to a private GCS bucket, so users who publish and consume custom
BOMs may also be using these commands.

To simplify the surface area for end users, this functionality will be removed
from Halyard and migrated to a separate admin-specific tool to handle these
changes. Any users that are using the `hal admin` commands to publish custom
BOMs will need to migrate to the new tool; given that users publishing a custom
BOM are in general advanced users, this should not be a difficult migration.
(More research will also be done at that stage to determine if there are even
any end-users relying on these commands as part of their custom BOM workflow.)

## Retained core competency

Having outlined the features to be removed from Halyard above, this section will
outline the remaining core competency of Halyard.

Halyard will focus on its core competency of translating a Halyard config file
into the config files for the individual microservices. Halyard will expose a
single command that takes as input a Halyard config and outputs to a specified
directory the configurations for each microservice, as well as files referenced
by these config files.

This command will roughly map onto the current `hal config generate` command.

### API contract

#### Input

The input to kleat will be a Halyard config file, expressed as YAML. In order to
promote backwards compatibility, the format of this config file will change only
minimally from the Halyard config.

Notably, the changes that will be made are:

- A Halyard-generated halconfig contains two top-level fields:
  deploymentConfigurations (a list of HalConfigs) and currentDeployment, (the
  name of the deploymentConfiguration to manage). Kleat will accept a file with
  a single config as input, so any halconfigs with multiple
  deploymentConfigurations need to be separated into different files. Feedback
  from users is that having multiple configs in a single file was rarely, if
  ever, used.
- Fields where minor changes would greatly simplify `kleat` and reduce its
  dependence on microservice-specific details will be changed along with clear
  upgrade documentation on the changes required. A list of fields found to date
  can be found [here](https://github.com/spinnaker/kleat#required-changes). Note
  that most users will only have configured a small subset of these fields, so
  the burden on upgrade should be small.

In addition, there will be some fields that are no longer relevant in kleat;
these fields will be documented as being ignored by kleat. Depending on feedback
from initial adopters, kleat may emit a non-fatal warning if these fields are
set. (An example of such a field is `deploymentEnvironment.tolerations`, which
does not affect the generation of service configurations but only adds fields to
the output deployment YAML.)

As the Halyard config is not currently well-documented (with the documentation
instead living on the hal command reference), we will add extensive
documentation on this config file. In order to ensure the documentation stays up
to date, the technical implementation will keep the documentation alongside the
code and auto-generate
[user-facing documentation](https://github.com/spinnaker/kleat/blob/master/docs/docs.md#hal)
from this documented code.

#### Referenced files

In many cases, the Halyard config directly references files that are on disk on
the machine running Halyard. For example, the hal config might point to a file
containing a service account key that needs to be present in the container
running clouddriver.

Halyard has allowed the hal config to reference files anywhere on disk on the
machine running Halyard; as part of its config generation, it:

- copies these files to a staging directory
- updates all the config files to point to this staging directory
- mounts all these files in the deployed containers using the path of the
  staging directory

In order to make the handling of external files (which are generally sensitive)
more flexible, `kleat` will handle cases where the config points to files as
follows:

- The hal config should reference the location of the file _as it will be
  mounted in the container_.
- The default `kustomize` install pathway will mount a Kubernetes secret into
  `/var/secrets` in each container. Users can add any files to this secret in
  their overlay, and the files will then be mounted under `/var/secrets` in the
  pods for each service.
  - Users using this workflow would, for example, reference
    `/var/secrets/my-token.json` in their hal config. Then in their overlay,
    they would add `my-token.json` to the secret in the base kustomization,
    which would cause it to be mounted at that path in every container.
- Users who wish to handle secret deployment out-of-band can do so; they would
  reference the mounted path of the secret file in their hal config and would be
  responsible for mounting that secret in all containers (such as with JSON
  patches in their overlay).

It is worth noting that this approach to handling files referenced in the config
is one area where we are most interested in feedback from early adopters, and
are definitely open to improvements in this workflow if users find it
cumbersome.

#### Output

The output of kleat will be a directory containing the service YAML of each
microservice. As a baseline, `hal config generate` currently outputs this data
to a staging directory and we will use the same format for the output from
kleat.

### Install Pathways

With kleat, there will be a number of install pathways available to users.

#### Operator

The operator being built by Armory will replace its dependency on the existing
Halyard with kleat. As the operator already abstracts away the parts of Halyard
that are being removed, this change should be transparent to the end user.

#### Kustomize

We’ll provide a [Kustomization](https://github.com/spinnaker/kustomization-base)
that contains the necessary Kubernetes YAML for deploying Spinnaker. In order to
simplify the configuration in users' overlays, this base kustomization will
create empty `Secret`s for the config and referenced files and will mount them
in the appropriate places in the containers. Users will then just need to add
files to these `Secret`s in their overlay.

As a way to quickly get started, we'll provide a
[skeleton repo](https://github.com/spinnaker/spinnaker-config) that users can
clone and add their config to.

An overview of how to deploy one service this way is provided
[here](https://github.com/spinnaker/kustomization-base#installing-spinnaker),
but the quick overview is that users will need to:

- Run `kleat` on their hal config, to generate their service configs.
- Add those service configs to the relevant config maps in their
  `kustomization.yaml`. (This is already done in the skeleton repo.)
- Add any referenced files to the `spinnaker-secrets` secret in their
  `kustomization.yaml`
- For some users, this will be all and they run `kustomize build` and deploy the
  result.
- Other users may want to:
  - Add some `-local.yml` config files
  - Configure a custom redis endpoint
  - Add custom patches to the deployed services

In terms of customizing the deployment, some users may want to apply JSON
patches to the base config in their overlay, and others may want to fork the
base config and directly modify the YAML. At this point we don't have a strong
opinion about which path users should take and intend to support both.

Users can select a version of Spinnaker to deploy by supplying container tag
overrides in their `kustomization.yaml` file. The release process will be
updated so that published container images will be tagged with their top-level
version (ex: `spinnaker-1.20.1`). This avoids the need to fetch a BOM and parse
it to find the the specific tag for each microservice.

#### Helm

The solution here is less clear, as there is not a great way to pass config
files to Helm; to have a native Helm installation, it would be necessary to
reproduce the entire Halyard config in the values file, and re-implement kleat
in Helm’s templating language. A better solution would be to have Helm install
the operator, which is similar to how the chart currently just installs Halyard
but should provide a more Kubernetes-native experience.

The initial implementation is focusing on the Kustomize install path, and there
are no immediate plans to add a Helm install pathway.

#### Manual

Users with enough Kubernetes experience, or with enough special use cases may
want to manually write the YAML for their Spinnaker deployment. In this case,
they’d use kleat to generate the required service configuration files, and would
then feed these into their process for generating the YAML required to deploy
Spinnaker.

### Non-Kubernetes

Users who are not using Kubernetes will still be able to use kleat to generate
their config files and stage any dependent files. As is the case for Kubernetes,
Halyard will no longer handle actually deploying the services to VMs; users will
be responsible for putting the generated configs in the expected location and
fetching/starting the services. As Halyard currently only supports deploying all
services to the same machine where Halyard is running, it's not clear that this
was commonly used for actual production setups; the fact that kleat will have an
explicit contract to output the required config files should make it easier to
build downstream tooling for deploying to VMs for users that want this
functionality.

### Technical Overview

#### Refactor or replace

Given the above discussion about the shortcomings of the current implementation
of Halyard, and the fact that we are proposing to only support a small core of
the current functionality, we will re-implement this core part instead of
refactoring Halyard.

This has the advantage of allowing the existing Halyard to be maintained while
the new kleat is being implemented and adopted by end users; it also insulates
the existing Halyard from quick iteration on kleat. One disadvantage of this
approach is that there will be a period of time where we need to maintain both
the old and new Halyard.

#### CLI vs. Daemon

Halyard is currently implemented as a CLI and a daemon, greatly adding to its
complexity. This decision was made to allow support for clients other than the
CLI, but to date no such implementations have been written and none are planned.
The new tool will replace this with a CLI only, with no background daemon
process.

#### Language

kleat will be written in Go, which has the following advantages:

- The primary initial consumer of kleat will be the Armory Operator, which is
  also written in Go and will be able to consume parts of kleat as a library
- Operators deploying to Kubernetes are usually much more familiar with Go than
  with Java, so this will likely lead to an easier path to contribution from the
  community
- Go is well-suited for writing simple command-line tools

## Drawbacks

This change will require users to change their workflow for deploying Spinnaker
in order to adopt kleat. While the format of the Halyard config itself will not
change, users will need to change the way they actually deploy Spinnaker from
running `hal deploy apply` to using one of the above-described install pathways.

## Prior Art and Alternatives

One alternative to the current approach of writing a new tool to replace Halyard
would be to refactor the existing Halyard to accomplish the same goals. That
being said, given the assessment above that the majority of Halyard's
functionality is either low-value or exists in other popular tools, the amount
of code reuse that would be possible by an in-place refactor is minimal. An
in-place refactor would also add the risk of breaking existing users during the
implementation of this RFC.

Overall, one of the primary motivations of this RFC is to remove functionality
from Halyard that duplicates existing tools with larger market share, and
instead build a tool that integrates well with these existing tools. This has
the benefit of allowing users to install Spinnaker using tools that are familiar
to them, and removes the maintenance burden of Halyard's custom implementation
of duplicative tools.

## Known Unknowns

At this point, there are still a few details that are unknown and will be
clarified during the implementation and testing phases:

- The exact format of the output from the CLI tool. It will need to output the
  service configs as well as (possibly) some metadata about the Spinnaker
  deployment (ex: the versions of containers to pull) but the exact format will
  be determined in concert with early consumers.
- The exact format of the Helm chart and Kustomize kustomization that will be
  provided to support installation to Spinnaker remains unspecified.

## Security, Privacy, and Compliance

The Halyard configuration often contains secrets that are eventually included in
the microservice configuration files (ex: tokens/passwords for communicating
with external services) as discussed in the above section on secret management.

By removing the ability for Halyard to decrypt these secrets and forcing the the
services themselves to handle decryption, we are ensuring end-to-end encryption
and eliminating one potential vulnerability point.

## Operations

Migration to kleat will involve some one-time work to update any scripts or
procedures that organizations have developed to deploy Spinnaker. In particular,
some configuration that existed in the Halyard configuration will now be moved
to the Helm or Kustomize layer, and will require users to update their
configuration accordingly.

While this will be a one-time migration effort, it will move users to tools that
have wide adoption in the Kubernetes community and which are likely familiar to
operators. This should simplify ongoing maintenance and updates to their
Spinnaker deployment by reducing the amount that users need to learn about
Spinnaker-specific tooling and instead leveraging their knowledge of more
general Kubernetes tools.

## Risks

The primary risk here is that users do not want to migrate to kleat, either
because there is an important user story that it does not support, or because it
does not provide enough of an improvement as incentive for the one-time
migration costs. As the existing Halyard will continue to be supported during
the transition, the primary risk here is wasted effort if kleat does not get
enough adoption to warrant continued maintenance. A more likely outcome is that
feedback from initial testers results in updates to this plan to better support
their use cases, which might delay the full GA availability of kleat.

## Future Possibilities

Once Kubernetes-specific configuration for a Spinnaker deployment has been moved
out of Halyard and into Kubernetes-specific tools such as Helm and Kustomize,
there is a lot of value that could be derived from adding features to these Helm
charts and Kustomize kustomizations. For example, one could imagine users
sharing (and/or contributing upstream) useful HA or fault-tolerant deployments
of Spinnaker. Given that these configurations would be in tools/languages
familiar to operators deploying to Kubernetes instead of in a Spinnaker-specific
tool, it is likely that there would be more community participation than there
currently is in Halyard.
