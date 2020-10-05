# Spinnaker Roadmap for H2 2020

These items were pulled from discussions with the major contributors supporting
Spinnaker. It is not meant to be inclusive of all work going on in the
community, nor will all this necessarily get done in the second half of 2020.
Rather, it is meant to give a general sense of direction and priorities.

## Kubernetes

### Features/usability

- [Improve performance of dynamic accounts](https://github.com/spinnaker/governance/pull/159)
- Make Kubernetes-native install pathway
  ([kleat](https://github.com/spinnaker/kleat)) GA and deprecate Halyard

### Performance and scalability

- [Reduce the number of resources cached and thus the resource footprint needed by clouddriver](https://github.com/spinnaker/spinnaker/issues/5908)
- [Replace shelling to kubectl with the Kubernetes client library](https://github.com/spinnaker/spinnaker/issues/5643)

### Stability and bug fixes

- [Add easily-runnable integration tests on the Kubernetes provider](https://github.com/spinnaker/clouddriver/pull/4827)
- [Give users control over whether deployed manifests are SpEL evaluated](https://github.com/spinnaker/spinnaker/issues/5910)
- [Fix bugs with dynamic target selection](https://github.com/spinnaker/spinnaker/issues/5607)
  when liveManifestCalls is enabled, and
  [enable this flag by default](https://github.com/spinnaker/spinnaker/issues/5906)

### Upcoming

Much of the performance/refactor work being done currently will help enable the
items listed here.

- Surface additional Kubernetes resources in the UI (ex: autoscalers)
- Create sensible extension points for plugin developers
- Improve the story around adding CRD support, including making it easier to:
  - define stability conditions for CRDs
  - map CRDs to Spinnaker resources
