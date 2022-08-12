# Make operator the default install method

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | _Update after PR has been made_ |
| **Author(s)**  | Maria Ashby (@mashby2022)_, Fernando Freire (@dogonthehorizon)_ |
| **SIG / WG**   | _Applicable SIG(s) or Working Group(s)_ |
| **Obsoletes**  | _<RFC-#s>, if any, or remove header_ |

## Overview


The current recommended way to deploy Spinnaker is by using Kleat. While Kleat has improved some aspects of installing and configuring Spinnaker, it is currently not being maintained since Google has left the project. This document proposes that Operator should replace Kleat as the default install method. 

### Goals and Non-Goals

Goals:
Improve the install process for users by introducing Operator as the default installation method.
Deprecate Halyard, which is no longer maintained
Provide Kleat as an alternative to the Operator for users that need more control in their installation process, but don’t make it the default
Non-goals:
While this document focuses on making the experience of installing Spinnaker easier, it does not intend to deprecate or eliminate the current installation tool, Kleat 


## Motivation and Rationale

From the Kubernetes Operator pattern docs: “Operators are software extensions to Kubernetes that make use of custom resources to manage applications and their components.” In other words, an Operator is a Kubernetes controller that manages a specific application using a custom resource. The Spinnaker Operator for Kubernetes is a custom Kubernetes Operator that you can install in your cluster to manage Spinnaker installations.

We propose migrating to the Spinnaker Operator as the default installation method for the community for the following reasons:
Leveraging existing Kubernetes tooling and patterns to install Spinnaker
Simplifying day 0 operations for new users in the community by encapsulating best practices and defaults in th operator

Kleat doesn’t go away in our proposed architecture, but we instead integrate with it. For users that require more control or sophistication in their deployment methodology, they will still be able to leverage Kleat for their needs.

## Timeline

(October 2022)
A generally available version of Operator is released along with documentation, and is able to deploy and upgrade Spinnaker.
Plans are communicated in Slack and additional channels about the proposed plan
(December 2022)
Feedback from early users of the Operator are addressed and released
Halyard is removed from the Operator in favor of Kleat integration
(February  2023)
Halyard is removed from the docs site and Halyard repos are archived.
General communication goes out


## Design

Removed and replaced features
Commands to edit the Halyard config
Refer to previous Halyard lite RFC 
Input validation
Existing Halyard validations will be  migrated to their Operator equivalents
https://docs.armory.io/armory-enterprise/installation/armory-operator/op-config-manifest/#specvalidation
spec.validation
Currently these configurations are experimental. By default, the Operator always validates Kubernetes accounts when applying a SpinnakerService manifest.
Validation options that apply to all validations that Operator performs:
spec.validation.failOnError: Boolean. Defaults to true. If false, the validation runs and the results are logged, but the service is always considered valid.
spec.validation.failFast: Boolean. Defaults to false. If true, validation stops at the first error.
spec.validation.frequencySeconds: Optional. Integer. Define a grace period before a validation runs again. For example, if you specify a value of 120 and edit the SpinnakerService without changing an account within a 120 second window, the validation on that account does not run again.
Additionally, the following settings are specific to Kubernetes, Docker, AWS, AWS S3, CI tools, metric stores, persistent storage, or notification systems:
spec.validation.providers.kubernetes
spec.validation.providers.docker
spec.validation.providers.aws
spec.validation.providers.s3
spec.validation.providers.ci
spec.validation.providers.metricStores
spec.validation.providers.persistentStorage
spec.validation.providers.notifications
Supported settings are enabled (set to false to turn off validations), failOnError, and frequencySeconds.
The following example disables all Kubernetes account validations:
spec:
  validation:
    providers:
      kubernetes:
        enabled: false
spec.accounts
Support for SpinnakerAccount CRD (Experimental):
spec.accounts.enabled: Boolean. Defaults to false. If true, the SpinnakerService uses all SpinnakerAccount objects enabled.
spec.accounts.dynamic (experimental): Boolean. Defaults to false. If true, SpinnakerAccount objects are available to Armory Enterprise as the account is applied (without redeploying any service).
Secret management
Spinnaker now supports two ways of handling secrets in config files:
Secrets encrypted using the Spinnaker-specific format supported by kork-secrets. Halyard is able to decrypt these secrets, and in some cases (ex: deck) writes unencrypted values to services.
Support in the services themselves (via Spring Cloud Config) to decrypt externally-stored secrets
Operator will additionally support sourcing secrets from Kubernetes Secret objects
Version publishing/deprecating
Refer to Halyard Lite RFC 
Retained core competency
API contract
Input
Users will now define a SpinnakerService CRD
Link to this but don’t embed in the doc:
https://docs.armory.io/armory-enterprise/installation/armory-operator/op-manifest-reference/
Largely compatible with the halyard config, with additional parent config options
There’s no planned automated migration between Halyard and Operator
Output
The operator will handle Spinnaker setup within the desired Kubernetes cluster
Install Pathways
Operator
The operator being built by Armory will replace its dependency on the existing Halyard with kleat. As the operator already abstracts away the parts of Halyard that are being removed, this change should be transparent to the end user.
Kustomize / Kleat
Refer to the Halyard Lite RFC
Helm
Refer to the Halyard Lite RFC
Manual
Refer to the Halyard Lite RFC
Technical Overview

### Dependencies

_What existing internal and external systems does this proposal depend on?_
_Are any new dependencies (libraries or systems) being introduced?_

## Drawbacks


This change will require manual intervention to migrate users over to Operator. The Operator install method will be a very different install/configure experience to the current accepted installation UX (Halyard). Operator makes assumptions about how to configure resources inside Kubernetes. For example, in cases where you need more control over how resources are created, provide your own “frontend” to Kleat. 




## Prior Art and Alternatives

Refer to Halyard Lite RFC


## Known Unknowns

There are no known unknowns at this time.


## Security, Privacy, and Compliance

Operator will maintain existing opt-out telemetry as documented here: https://spinnaker.io/docs/community/stay-informed/stats/


## Operations


Migration to Operator will involve some one-time work to update any scripts or procedures that organizations have developed to deploy Spinnaker.
While this will be a one-time migration effort, it will move users to tools that have wide adoption in the Kubernetes community and which are likely familiar to operators. This should simplify ongoing maintenance and updates to their Spinnaker deployment by reducing the amount that users need to learn about Spinnaker-specific tooling and instead leveraging their knowledge of more general Kubernetes tools.


## Risks

The primary risk here is the manual migration of users to Operator. We currently do not have an automated tool to migrate from the previous Halyard configuration format to Spinnaker. Additionally, there is not a lot of community incentive to migrate over to Operator. 


## Future Possibilities

In the future, we  could do more to automatically infer Kubernetes cluster state and automatically configure Spinnaker features to take advantage of them. Philosophically, operators are meant to capture human operational intelligence about a product. Manual/repeatable stuff people are doing to install/configure Spinnaker are good candidates for automation via the Operator.
 
