# Kubernetes operator for Spinnaker

| | |
|-|-|
| **Status**     | **Proposed** |
| **RFC #**      | https://github.com/spinnaker/community/pull/41 |
| **Author(s)**  | Nicolas Cohen (`@ncknt`) |
| **SIG / WG**   | Kubernetes SIG |

## Overview

Halyard is great at offering a simple way to configure Spinnaker services but is not easy to operationalize. We've come up with an MVP of a Kubernetes operator for Spinnaker that allows administrators to manage Spinnaker with kubectl (or whatever Kubernetes tool).

We're proposing to move the operator to Spinnaker's GitHub: https://docs.google.com/document/d/1Fs-xK7sOZ4YLSw1ae_cQ_rsB0kMNon9AfYli2a5Udhs/edit?usp=sharing

### Goals and Non-Goals

The goal of the Spinnaker Operator is to enable users to manage Spinnaker via Kubernetes native tools (`kubectl`, ...). Short term we want feature parity with Halyard deployments. Longer term, we'd like extensive preflight checks and active monitoring of Spinnaker.

## Motivation and Rationale

Deploying Spinnaker in Kubernetes with Halyard comes with a few challenges:

- Users often start using Halyard from their laptop and struggle to find a good process to share the configuration.
- How should Halyard itself be managed? Locally spun up at CI time or as a long-time running service?
- How do you template the configuration if you have multiple running instances?
- How do you pass credentials to Halyard, like kubeconfig files or AWS credentials?
- Users are left with a lot of activities that Halyard doesn’t handle such as exposing Spinnaker and cleaning up old secrets.

Finally, there’s the impression that Spinnaker is not “Kubernetes native” because it cannot be installed and managed with the usual administrator toolchain (kubectl). Talking to Spinnaker administrators, we’ve realized this is a pretty widely shared feeling. They’re already proficient at using kubectl and are reluctant to learn and configure yet another new tool.

## Timeline

Current MVP can deploy and expose Spinnaker. Preflight checks will be added in the coming weeks. Moving the operator to Spinnaker's GitHub account involves publishing new images to gcr and cleaning the build.

## Design

See https://docs.google.com/document/d/1Fs-xK7sOZ4YLSw1ae_cQ_rsB0kMNon9AfYli2a5Udhs/edit#heading=h.7vexvfs9sl5e

### Dependencies

No new dependencies to existing projects. Some (relatively small) changes are necessary in Halyard to:

1) Generate manifests from a dynamic config (not in `~/.hal/`) at runtime
2) Generate CR and `configMap` from an existing config to facilitate the migration to an operator based deployment.


## Drawbacks

It's another project. The operator is also a new deployment running in the user's cluster. If the operator is strictly a tool to configure (vs monitor), that may seem overkill.

## Prior Art and Alternatives

We considered that [operator](https://operatorhub.io/operator/spinnaker-operator) but couldn't find good documentation, the code, or users using it. It also seems to be running a Halyard job in the cluster. Feel free to correct me.

## Known Unknowns

The `spec` of the [CRD](https://github.com/armory/spinnaker-operator/blob/master/deploy/crds/spinnaker_v1alpha1_spinnakerservice_crd.yaml) is still in flux. As we add more features, it will need to evolve before being stable. As an example, new fields are needed to track validation options. The existing `status` is also a first stab.

We'd love to get feedback on the user workflows that this enables (or that are lacking). We already heard about support for Kubernetes secret as a common requirement.


## Security, Privacy, and Compliance

Security of the operator is controlled via a `ClusterRole`. The current definition may need to be tightened.

## Operations

After installing the Operator in a target cluster, the user can manage `SpinnakerService` using `kubectl` and manifests. How do you get these manifests?

1) Users can maintain the `configMap` by hand. We’ll develop documentation to make it easier. We also expect (and have seen) users scripting the Halyard configuration with helm or kustomize.
2) We added an endpoint to Halyard to generate the `configMap` and `SpinnakerService` from an existing Halyard configuration. This can be used as a one time migration to using configMaps or as a `hal deploy manifests | kubectl apply -f -`
3) We can add an `Operator` deployment type to Halyard that applies a generated `configMap` and `SpinnakerService` - similar to how `Distributed` type manifests are applied. In this scenario, the user can keep a local Halyard but deploy with the Operator under the hood.

There's currently no telemetry baked in.

## Risks

The implementation relies on Halyard's deployer being able to generate manifests instead of applying them.

## Future Possibilities

- More preflight and scheduled checks for connectivity, authorization: we'd for instance like to move some of the cloud provider account check to preflight check to speed up Clouddriver startup. The health of Spinnaker could be visible via `kubectl describe` and events.
- Support native secrets: allow users to reference Kubernetes secrets stored out of band.
- Enable a default metrics store (Prometheus) to allow administrators to more easily monitor their Spinnaker installations like we do with Redis.
- Make the Spinnaker CRD a good place to add Kubernetes configuration for each service. For instance, number of replicas, security contexts, etc. are added to the Halyard configuration. Halyard could be kept focused on producing the right configuration for each service and not manage any runtime/environment settings.
- Properly document Halyard configuration to let users skip Halyard CLI altogether.
- Generate an Application Custom Resource (CR) for Spinnaker: https://github.com/kubernetes-sigs/application.

