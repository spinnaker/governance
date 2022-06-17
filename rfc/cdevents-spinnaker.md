# Implementing CDEvents to Spinnaker using Java SDK


| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | _Update after PR has been made_ |
| **Author(s)**  | _Jalander Ramagiri (`@rjalander`)_|
| **SIG / WG**   | CDF-CDEvents, CDF-SIGEvents |

## Overview

Many organisations use more than one CI/CD tools as part of their pipelines and most of the times the organisations implement their own glue code in order to make the tools work with each other.

Implementing [CDEvents](https://cdevents.dev/) to Spinnaker enables Spinnaker to communicate with other CI/CD tools with in the ecosystem. [CDEvents](https://cdevents.dev/) is a common specification for Continuous Delivery events based on CloudEvents.
Spinnaker can produce and consume [CDEvents](https://cdevents.dev/) using [Java SDK](https://github.com/cdevents/sdk-java) which is developed and maintained by the [CDEvents community](https://cdevents.dev/community/).

### Goals and Non-Goals
Goals:
- Event-Driven Standardization with CI/CD tools like Keptn, Tekton, Jenkins, Spinnaker, etc..
- Create an `events-broker` that is able to produce and consume `CDEvents`
- One needs to subscribe to `events-broker` with the type of event that needs to consume
- API end points will be created to produce and consume `CDEvents`

## Motivation and Rationale

- Currently there is no common specification for CI/CD tools to communicate with in the CI/CD ecosystem. 

- In a complex and fast-moving CI/CD world with a lot of different tools and platforms that need to communicate with each other.

- Multiple pipelines can be run between CI/CD tools by sharing the artifacts and its metadata.

- Increase in traceability with in the ecosystem from commit to deployment of an artifact.

- Operators can deploy Spinnaker with `CDEvents` to run multiple pipelines using different CI/CD tools in ecosystem.

## Timeline
Proof of concept with Spinnaker, Keptn and Tekton CI/CD tools developed to communicate with each other about occurrences and running pipelines from building an artifact to deploy.

The final implementation aligning with `CDEvents` vocabulary specification will take from 4-6 weeks.

## Design
Proposing a design based on the PoC outcome.

A `CDEventsController` will be created with the API endpoints to produce and consume `CDEvents` from `events-broker` with request mappings,

`@RequestMapping(value = "cdevents/consume", method = RequestMethod.POST)`

`@RequestMapping(value = "cdevents/produce", method = RequestMethod.POST)`


An `events-broker` will be created to send and receive events by knative-eventing and the sample URL can be configured as below in the cluster.

`events-broker : http://broker-ingress.knative-eventing.svc.cluster.local/default/events-broker`

#### Consume CDEvents

Spinnaker API endpoint `cdevents/consume` will be subscribed to `events-broker` to listen to the various types of events and Spinnaker Pipelines need to be configured with the `CDEvents` type on which Spinnaker is interested on.
Example: A sample event `dev.cdevents.artifact.published` subscribed to `events-broker`
``` 
kubectl create -f - <<EOF
apiVersion: eventing.knative.dev/v1
kind: Trigger
metadata:
  name: cd-artifact-published-to-spinnaker
spec:
  broker: events-broker
  filter:
    attributes:
      type: dev.cdevents.artifact.published
  subscriber:
    uri: http://<spin-service>:<port>/cdevents/consume
EOF 
```
Once the event is received by Spinnaker API endpoint `cdevents/consume`, further can be handled to run pipelines based on the configured event data in Spinnaker.

#### Produce CDEvents
Spinnaker API endpoint `cdevents/produce` will be invoked with the type of event that needs to be sent to `events-broker`.
`CDEvents` can be created using [Java SDK](https://github.com/cdevents/sdk-java) that is available in `CDEvents` [GitHub Repositories](https://github.com/cdevents).
`Java SDK` used to create variuos types of events alligning with `CDEvents` vocabulary specification using CloudEventBuilder. 

`CDEvents` can be published to `events-broker` once created from Spinnaker application using CloudEvents HTTP libraries.

Example: A sample serivce-deployed event published to `events-broker`
```
curl -v -d '{"id": "1234", "subject": "event"}' -X POST -H "Ce-Id: HelloSpinnaker" -H "Ce-Specversion: 1.0" -H "Ce-Type: dev.cdevents.service.deployed" -H "Ce-Source: not-sendoff" -H "Content-Type: application/json" "http://<events-broker-url>/default/events-broker"
```

### Dependencies

The CloudEvent dependencies(io.cloudevents) are required to implement produce/consume events 


## Prior Art and Alternatives
The existing approach is integration oriented and could sometimes result in outages due to change in implementation of integrated components. `CDEvents` allows users to take a more standardised approach, ensuring efficiency and sustainability while creating and maintaining CI/CD pipelines.

## Known Unknowns

The approach will be demonstrated with the Spinnaker Community using `CDEvents` PoC.

## Operations
Admin needs to configure Pipelines that should run on consuming the different event types from `events-broker`

## Future Possibilities

`CDEvents` open the doors to scalable and decoupled interoperability within the software supply chain and create the potential for greater visibility and measurability of cloud native continuous delivery workflows.
