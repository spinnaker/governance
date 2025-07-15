# Distributed Tracing

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | [#345](https://github.com/spinnaker/governance/pull/345) |
| **Author(s)**  | @mattgogerly (Matt Gogerly) |
| **SIG / WG**   | Ops |

## Overview

Introduce distributed tracing to the Spinnaker project for enhanced observability
and monitoring.

### Goals and Non-Goals

#### Goals

- Standardise Spinnaker service logging configuration in Kork.
- Add a distributed tracing framework as a Kork dependency.
- All Spinnaker service logs include trace information.
- Configuration options to send to a trace aggregator (e.g. Zipkin).

#### Non-Goals

- Bundling a trace aggregator with Spinnaker.

## Motivation and Rationale

Spinnaker is a complicated system to operate. Consisting of approximately 10 services,
it can be extremely difficult to follow the lifecycle of a request without intimate knowledge
of the interdependencies between each service. This makes troubleshooting issues painful,
as it often requires jumping between multiple log streams to follow a request as it moves
through the system.

Distributed tracing is a method for following the lifecycle of a request as it moves through
various connected systems. Each unique request is "tagged" with an ID where it originates, and this
ID is then propagated alongside any downstream requests.

When combined with a log aggregator of your choice, including these trace IDs in logs enables
querying for a single ID to identify all logs across all services involved in the request.

Furthermore, traces can be pushed to an aggregator such as Zipkin to enable visualisation
of the request lifecycle.

## Timeline

I propose that this work be undertaken in three distinct stages:

### Stage One

Introduce a standard `logback-config.xml` in Kork for all services to consume. Currently, each service
defines it's on Logback configuration in the `{service}-web` module. These are all nearly identical, with
minute, inconsequential differences. By introducing a standard logging configuration it becomes easier to
make broad improvements to logging.

This will be achieved by removing the `logback-defaults.xml` file from each service and including the Kork
equivalent in each service's source set, as described [here](https://stackoverflow.com/a/43686346).

### Stage Two

Introduce a set of libraries as dependencies in Kork, and wire them up to OkHttp and logging. This RFC proposes
using [Spring Sleuth](https://spring.io/projects/spring-cloud-sleuth). [Micrometer Tracing](https://github.com/micrometer-metrics/tracing) is the successor to Spring Sleuth but does not support versions of Spring Boot < 3 or Kotlin versions < 1.7.

`org.springframework.cloud:spring-cloud-starter-zipkin` will be added as an `api` dependency to `kork-web`. Each `{service}-web` will need to declare it as an `implementation` dependency. I envisage this will be much easier once the monorepo is in place. As Spring Sleuth is a wrapper for Brave and brings it in transitively, we can remove the existing dependency on `io.zipkin.brave:brave-bom`.

Alternatively, we can exclude `org.springframework.cloud:spring-cloud-sleuth-brave` and opt to add a dependency on `org.springframework.cloud:spring-cloud-sleuth-otel-autoconfigure` to use OpenTelemetry instead. We would also need to replace the [Brave OkHttp instrumentation](https://github.com/openzipkin/brave/blob/master/instrumentation/okhttp3) with the equivalent [OpenTelemetry instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/okhttp/okhttp-3.0/library).

I leave the choice between Brave and OpenTelemetry open to discussion in this RFC.

Additionally, we can add instrumentation to the HTTP client(s) in Deck, to include the initial call from Deck -> Gate as part
of each trace.

Finally, we update the `logback-config.xml` in Kork to include the trace data in each log. At this point, assuming a
Spinnaker operator is forwarding logs to an aggregator like Splunk or Elasticsearch, it will be possible to query
by trace ID and retrieve all logs relating to a single request.

### Stage Three

Add a way to export traces (or samples of them) to a backend. This RFC proposes adding support for Zipkin as a backend.

Spinnaker operators will be able to enable sending traces to Zipkin, providing visualisation tooling, using standard Spring Sleuth configuration options in their `spinnaker-local.yml` profile as described [here](https://cloud.spring.io/spring-cloud-sleuth/reference/html/#sending-spans-to-zipkin).

## Design

### Dependencies

`org.springframework.cloud:spring-cloud-starter-zipkin` will be added as a dependency, replacing `io.zipkin.brave:brave-bom`.

If OpenTelemetry is chosen over Brave `io.zipkin.brave:brave-instrumentation-okhttp3` will be replaced by
`io.opentelemetry.instrumentation:opentelemetry-okhttp-3.0` in `kork-retrofit`, `kork-retrofit2` and `kork-web` in
stage two.

## Drawbacks

We are introducing more libraries that will need to be kept up to date.

## Prior Art and Alternatives

Netflix introduced the [Brave tracing library](https://github.com/spinnaker/kork/pull/774) to Kork back in 2020.
This was wired up to Kork's default `OkHttpClient` as a starting point. Services that use this client (all except Gate
at time of writing) log traces as a separate log message after each request. Whilst a good starting point, this
implementation has a few key limitations:

* Only outbound HTTP requests have traces logged. Logs related to handling the request in the downstream system do not include the trace ID.
* Traces are logged separately to the log of the request itself making it difficult, if not impossible, to identify which trace belongs to which request in a busy system.
* There is no configuration available to send the traces to an aggregator for visualisation.

## Known Unknowns

We need to choose between OpenTelemetry and Brave.

## Security, Privacy, and Compliance

It's inevitable that any dependencies added as part of this effort could have CVEs reported at some point. These
should be triaged and patched in the same way that we could handle a CVE report for any other dependency.
