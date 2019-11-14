# Spinnaker Roadmap for H1 2020

These items were pulled from discussions with the major contributors supporting
Spinnaker. It is not meant to be inclusive of all work going on in the
community, nor will all this necessarily get done in the first half of 2020.
Rather, it is meant to give an general sense of direction and priorities.

## [Managed Delivery](https://blog.spinnaker.io/managed-delivery-evolving-continuous-delivery-at-netflix-eb74877fb33c) (Netflix)

*   Supports EC2 and Titus (Netflix's container management platform) as target
    provider
*   Manages artifacts and infrastructure (Spinnaker Clusters, Server Groups, AWS
    LBs, AWS Security Groups)
*   New UI focused on artifact flow through environments
*   Support for a set of environment constraints that enable defining a complete
    delivery workflow from CI to production

## Simplified Kubernetes developer experience (Google)

*   Renaming UI elements in Spinnaker to avoid Kubernetes clashes and VM-based
    terminology
*   Visualizing (more) Kubernetes resources in the Spinnaker UI
*   Simpler workflow for first deployment to Kubernetes with a container, Helm
    chart, or folder of YAML
*   Better surfacing of underlying errors when reconciliation fails

## Simplified/cloud-native Spinnaker ops experience

*   Official Kubernetes operator that supports a config-as-code approach to
    managing Spinnaker with Terraform and/or Kubernetes (Armory)
*   Official production-grade Helm chart to install Spinnaker (Pivotal)
*   Prometheus dashboard installed out of the box with Spinnaker (Armory/Google)
*   Dynamic credentials management with Spring Cloud Config Server (Pivotal)

## Spinnaker modernization & cloud-nativization

*   Kayenta as a Kubernetes controller with a Tekton task to call it
    (Google/Netflix)
*   Tekton task to call a Spinnaker pipeline (with artifacts) (Armory)
*   Investigate/proof-of-concept gRPC APIs (Google/Netflix)
*   Agreed-upon plan for move to Java 11 (Google)
*   Support for pluggable stages (Armory)
*   Support for pluggable cloud providers (Netflix)
*   Continued SQL consolidation & service migration (Netflix)
*   Auto-generated documentation for Spinnaker configuration, providing a
    comprehensive single source for all available configuration parameters
    (Netflix)
*   Metrics & Alerting revamp (Micrometer migration) (Netflix)

## Community enablement

*   Run integration tests in a cloud CI environment (Google)
*   Framework for making Spinnaker community stats available (Google/Armory)
*   Further static code analysis / testing tools / testing standardization
    (Netflix)

## Spinnaker support for runtimes

*   Pivotal CF: Enhanced support for networking and security (Pivotal)
*   Pivotal Spring: Additional coordination for blue/green of message driven and
    bidirectional RPC workloads (Pivotal)
*   AWS: Support for services with multiple load balancer target groups (Amazon)
*   AWS: Performance enhancements for accounts with large number of resources
    (Amazon)
*   Google Cloud: Documented approach to using Spinnaker with Cloud Run (Google)
