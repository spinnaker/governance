# Plugins Proposal

| | |
|-|-|
| **Status** | **Proposed**, Accepted, Implemented, Obsolete |
| **RFC  Num** | [#43](https://github.com/spinnaker/community/pull/43) |
| **Author(s)** | Rob Zienert (`@robzienert`), Cameron Motevasselani (`@link108`) |
| **SIG / WG** | Platform SIG |
| **Obsoletes** | [spinnaker/spinnaker#4181](https://github.com/spinnaker/spinnaker/issues/4181) |

## Overview
This proposal builds atop prior proposals and proof of concepts for a plugin system within Spinnaker. 
This proposal is for an experimental body of work spanning kork and multiple services including Deck, and eventually, standardization of some development lifecycles of Spinnaker itself.

### Goals and Non-Goals
* Goals
	* Support plugins on both the frontend & backend services
	* Support plugins spanning multiple services
	* Support in-process and RPC-backed plugins for backend services
	* Identify up/downgrade processes for plugins
	* Provide tooling for developers and operators
* Non-goals
	* Support for alternative runtimes (e.g. Lambda, Docker; to be addressed later)
	* Identification of all extension points within Spinnaker
	* Event-based plugins
	* Finalization of extension point acceptance processes
	* Finalization of plugin development and publishing workflow
	* Finalization of plugin operator workflow

It should be clear from these lists that this proposal is focusing on the service-side of plugins and to be a launching point for discovery and subsequent narrowly-focused proposals of ecosystem work.

Initial plugin targets will be:

* Pipeline Stages
* Pipeline SpEL functions
* Micrometer backends
* Gate proxy endpoint configuration

## Motivation and Rationale
Extending Spinnaker today falls into two categories, 1) writing Java code that directly extends internal APIs, or 2) usage of web hook stages or preconfigured jobs.

In the first case, a native Spring extension model, extensions need to be written on the JVM and released in lock-step with service releases. 
Further, it usually requires running the Spinnaker ecosystem to validate correct behavior - a technical hurdle that many possible contributors do not have the time or patience for. 
In the second case, extensions can be written and tested outside of the Spinnaker release cycle, but can only affect functionality of Spinnaker in the narrow scope of a pipeline stage.

In an effort to lower the bar for Spinnaker development, a native plugin model is being proposed that can be used for federated development of Spinnaker. 
This native plugin system will need to support clearly defined extension points within Spinnaker at both service- and product-level abstractions, from either an in-process or remote runtime model.

By creating this plugin model, Spinnaker will be able to iterate functionality faster as changes will be more easily developed, distributed and installed without adding bloat to the core product offering. 
This work aims to be a quality of life improvement for Spinnaker developers, operators, and users alike.

Early stakeholders for this effort are:

* Netflix Delivery Engineering
* Netflix Cloud Database Engineering
* Armory

We have explicitly not called out the Spinnaker OSS community, as the early deliverables of this effort are experimental and won’t be subject to backwards compatibility constraints.
Community members are welcome to be involved early nonetheless, provided acceptance of instability.

## Timeline
Proof of concepts were developed both by Armory and Netflix independently in 2019Q3. 
In Q4, we plan to deliver an alpha-quality plugin implementation that exhibits capability of extending multiple services as well as Deck, and include a basic distribution story for both in-process and out-of-process extensions.

Development will continue into and through 2020. 
If the plugin proposal is successful, plugin development and thereby the core plugin framework will continue iteration for the foreseeable future.

## Design
### Taxonomies
| Term | Definition |
| - | - |
| **Extension Point** | A strongly typed service- or library-defined point of extension |
| **Plugin** | An implementation of an Extension Point |
| **Installed Plugin** | A plugin known to Spinnaker, regardless of enabled state |
| **Enabled Plugin** | A plugin that is installed and actively participating in the application lifecycle |
| **Disabled Plugin** | A plugin that is installed, but not participating in the application lifecycle |

### Multiple Plugin Runtimes
There’s a place and a time for different plugin runtimes. 
In-process extensibility offers a lot of power and efficiency at the cost of higher coupling to core service rollouts, whereas a remote plugin runtime trades runtime efficiency and reliability for development, release independence, and choice of technologies (language, frameworks).

The goal of this proposal is to offer the best of both worlds by allowing extension point developers (Core Spinnaker developers) to decide what runtime(s) are the best for the extension.

Extension points will be defined and exposed via PF4J.

### PF4J as a Foundation
[PF4J](https://github.com/pf4j/pf4j) is to be used as the foundation for all plugin functionality. 
While designed for in-process extensibility, we’ll be using PF4J to define both the contract for in-process plugins, as well as remote plugins. 
Alternative (co-process, remote) runtimes would also be available but will be covered later.

PF4J has a lot to offer out of the box that we would need to build ourselves, regardless of final implementation. 
It doesn’t go as far as OSGI, but provides basic (yet extensible) ClassLoader isolation, a simple extension contract that we can use broadly, and a standardized manifest format. 
PF4J was originally built for creating in-process plugins only, but its own extension points make it feasible to enable remote plugins using the same contract. 
What runtime a particular plugin uses would be determined by a combination of: 1) The extension point developer allowing remote invocation, and 2) the plugin developer writing the plugin for remote invocation.

In the case of a remote plugin, the plugin developer will have the freedom to choose language, framework, and runtime. 
For any remote plugin, an in-process counterpart PF4J plugin will be required to enable communication between a service and the plugin. 
For example, a remote plugin that exposes its integration through a gRPC server would also need a gRPC transport plugin on the service so that communication between the two can be configured correctly. 
Other transport plugins, such as Docker co-process, Functions, HTTP, and so-on could also be developed.

An extension point looks like the following:

```kotlin
// Definition of the PluginStage extension point.
// The annotation defined here also informs the Spinnaker
// plugin framwework which runtimes are acceptable. This
// annotation would additionally be used for other metadata
// Spinnaker needs to know atop whatever PF4J offers out of
// the box.
@SpinnakerExtension(allowRemote = true)
class PipelineStageExtension : ExtensionPoint {
  // ...
}
```

It is highly recommended to read the documentation of PF4J to fully understand the scope of this proposal: This proposal will not go in depth on PF4J internals or taxonomy.

### Front50 as the Source of Truth
Operators will need the ability to understand, at a glance, what plugins are installed and enabled or disabled, their versions, as well as any other metadata about a plugin. 
Whether plugins are implemented as in-process or not, Front50 should be used to track and expose a single source of truth of what plugins are installed in any particular service, as well as which ones are enabled or disabled.

The Front50 plugin registry will also be exposed through Gate, and can be consumed by Deck to load the enabled plugins with UI components on application startup.

Plugins can also be enabled or disabled at runtime without redeploying a service. 
This flag is sourced from Front50, which all services will be listening for change events from. Whether this change event is published or polled for initially is unknown.

Halyard configs can be used to inform Spinnaker of desired state, but whatever state is defined in Front50 will be used as the canonical source for plugins. 
When a service starts up, it will ask Front50 what plugins need to be installed and which of those need to be enabled or disabled. Halyard’s role will only go as far as updating Front50’s repository of plugin metadata.

Using Front50 as a source of truth will require extension of PF4J.

```proto
syntax = "proto3";

// PluginManifest is used during registration of a plugin
message PluginManifest {
	// The name of the plugin, which is used as a fully 
	// qualified identifier, including a namespace and 
	// plugin name, ex: "netflix/fast-properties"
	string name = 1;

	// A short description of the plugin
	// ex: "Provides rollout safety of runtime application 
	// config via pipelines"
	string description = 2;

	// The name of the plugin author, ex: "Netflix"
	string provider_name = 3;

	// The semver plugin version, ex: "1.0.0"
	string version = 4;

	// A list of plugin dependencies. Initially this will be
	// informational only. ex: "netflix/somedep>=1.x"
	repeated string dependencies = 5;

	// required_versions is a repeated string of service version	// requirements. e.g. "clouddriver >= 5.33.0"
	repeated string required_versions = 6;

	// A list of extension points that the plugin implements.
	// e.g. "pipelineStage"
	repeated string provided_capabilities = 7;

	// Whether or not the plugin is enabled. An unset value 
	// should be considered as true.
	boolean enabled = 8;

	// If the plugin is in-process and this flag is set to true,
	// the plugin will be embedded into the process without
	// isolation, allowing full modification of the application
	// lifecycle. This is inherently unsafe and caution must be
	// used setting this field.
	boolean inprocess_unsafe = 9;
}
```

#### Sending Plugin Configuration to Deck
While Front50 will be the source of truth for which plugins are installed and enabled,  we can pass that data via `settings.js` today and transition to using Front50 when it is ready. 
Using `settings.js` as an intermediate step allows us to collect feedback from selected users in order to improve the user experience in the future.

We plan to prototype alternative options for Deck, such as altering webpack to support dynamic resolution, or introducing a standard container that can build Deck on startup with the configured plugins from Front50.

### Protobuf as the Remote Contract
As a departure from PF4J, plugin contracts will be defined as Protobuf messages (not services), so long as they are _remote-capable_. 
If a plugin is in-process only, and there’s no expectation that it becomes available for remote plugins, it does not need to define its contract as protobuf.

However, for any remotely-capable extension, its contract must be defined as protobufs, rather than exposed as simple POJOs or interfaces. 
A key distinction is that we’re not specifying gRPC as the transport.  
A remote transport may be Amazon SQS or Redis PubSub: not necessarily gRPC.

### Technology Compatibility Kits
_alias: TCKs_

A convention of writing TCKs for each Extension Point will be established. 
The concept of a TCK is borrowed from the JSR world, where compatibility tests are written which assert correct behavior of various features.  
Each implementation must them pass those compatibility tests.  
For Spinnaker plugins, TCKs can be written for two different consumers: Extension Point providers and Plugin developers.

TCKs assert correct behavior of an extension point and its associated plugin(s). 
For any extension point, a TCK will be written that asserts even pathological plugins behave in an expected way while integrating with the extension point. 
For a plugin, a TCK will be provided that a plugin developer can use to assert correctness without necessarily needing to run other Spinnaker services.

This goes further than the convention of using Protobuf to define the interfaces of a particular plugin, where Protobufs only define the interface, whereas a TCK will assert behavior that lies beneath the interface. 
Combining a TCK and adherence to the interface, contracts will be fully asserted between a plugin and its extension point(s).

### Up/Downgrading Plugins
This section applies to federated development scenarios, where the operator is not necessarily the same as the plugin developers; where the deploy cadence of a plugin does not adhere to the deploy cadence of the core Spinnaker services.

The upgrading (or downgrading, from now on broadly upgrading) plugin story is different based on the particular plugin runtime.

#### In-process
All in-process plugins should be downloadable. 
When a service starts up, it will ask Front50 for a manifest of plugins, downloading all installed plugins that are not already present on the filesystem (in the case of baked-in plugins) to a locally-accessible directory. 
At this point, PF4J will scan the directory, installing them into the process and enabling them based on the information pulled from Front50. 
Should Front50 be unavailable, the service will fail to start.

When a new plugin version is released and Front50 is updated with a new desired version, it is the responsibility of the operator to deploy this new version.
_**TBD**: How will an operator know a new version exists?_ 
This new version will need to be released in lock-step with the services that it is associated with. 

Actual plugin artifacts can be stored at any HTTP-accessible endpoint. 
Artifact repositories will support optional authentication, whereas checksums will always be required.
There is no hard rule on what technology should be used for the artifact repository, but initial development will use basic HTTP endpoints.
The download mechanism will be built into the PF4J integration within kork, either using [pf4j/pf4j-update](https://github.com/pf4j/pf4j-update) or a similar system that fits our needs.

A tightly-coupled service and plugin deployment is a poor developer and operator story, but an acceptable shortcoming for an initial release.  There are no current plans for supporting a world where in-process plugins can be upgraded at runtime. There is a lot of complexity and a broad spectrum of potential bugs by hot-reloading plugins, so we aim to avoid this issue entirely by not supporting this use case. If true decoupling of service and plugin is desired, the plugin must be developed and deployed as a remote plugin.

#### Remote
Remote plugin upgrade process is not much different than iterating a remote service, since that’s what it is.

When a remote plugin is released, its internal functionality will change immediately, just as it would be expected for a remote service. 
When the remote plugin starts, it must send a registration event to Spinnaker. 
This registration event would contain, at least:

* Plugin ID
* Other plugin metadata (author, description, version, etc).
* What extension points it integrates with

Upon receiving this registration event, Spinnaker would then update the Front50 registry with the new information and downstream services would re-bootstrap the plugin, adding or removing it from extension points as necessary.

### Deployment of Remote Plugins
Remote plugins can be deployed using whatever means the plugin developer desires.
In the case of a remote plugin being developed by a third-party outside of the organization, the operator may need to deploy and maintain the plugin themselves.
Halyard can be used for registering remote plugins with Front50.
In the absence of Halyard, registering remote plugins with Front50 would be done through the API, just as Halyard would do.

### Deployment of In-Process Plugins
Downloading plugin resources is necessary for in-process plugins.
Services need to have plugin resources available locally in order to load them into their process.
Plugin resources are hosted on a remote server and must be downloaded in order to use them.
The existing method of downloading plugin resources involves the service itself downloading plugins via a given URI.

#### Kubernetes Environment
Halyard can be used for deploying plugins to Kubernetes environments.
To deploy plugins, Halyard will follow the same pattern as Kubernetes regarding downloading resources prior to runtime.
Downloading of resources will be done through init containers and volume mounts that are then shared with their respective service.
By using init containers to download resources, we can standardize on one method of deploying plugins to spinnaker services.
Plugin resources will be placed in the following locations:

- Java services: `/opt/spinnaker/plugins/resources`
- Deck: `/opt/deck/html/plugins`

Plugins are placed in a different location for Deck so that it can serve plugin resources.

#### VM Based Environment
Plugins and their configurations can be downloaded or baked into the image.

### Plugin Configuration

#### Remote Plugins
Halyard or the operator, depending on preference, will be tasked with providing plugin configurations and plugin overrides to the remote plugins.

#### In-Process Plugins
PF4J plugins are configured with properties files and deployments are configured via `manifest.yml` file.
Properties files configure the application and `manifest.yml` lets halyard know where plugin resources are located.
`manifest.yml` can also override properties from properties file.
PF4J will handle loading plugins into a Spinnaker service, while `manifest.yml` will inform which resources should be deployed to each service.

### Dependencies
A series of new kork modules will be provided for libraries and services alike to consume to expose new extension points. 
Under the covers, this introduces PF4J as a new global service dependency.

In-process plugins, by default, will have their own ClassLoader that is isolated from other Plugin ClassLoaders. 
These Plugin ClassLoaders will be children of the root ClassLoader, meaning they cannot have dependencies that conflict with the root ClassLoader. 
Future work may be performed to allow for different levels of isolation if necessary.

**TBD**: We would like to offer different plugin runtimes besides in-process as plugins themselves. 
These plugin runtimes would then come with their own dependencies that would be exposed to the parent service, such as gRPC or Lambda function invocation.

## Drawbacks
There are a handful major issues with the proposed direction:

1. In-process plugins have a poor developer and operator experience which can negatively affect end-users, especially in rollback scenarios. 
In-process plugins also require developers to have more knowledge of the Spinnaker toolchain and potentially the service architecture. 
Exposing logging, telemetry and crash reports to plugin developers for in-process plugins will be difficult or impossible dependent on a Spinnaker installation’s preferences.
2. Supporting alternative runtimes adds complexity to the final implementation and will likely require good judgment on an extension point developer to write the correct abstractions.

## Prior Art and Alternatives
The existing plugins code that has come out from [#4181](https://github.com/spinnaker/spinnaker/issues/4181)  is a successful implementation of plugins, but only supports an in-process runtime model, and is implementing a bespoke plugin system. 
It would be preferential to use an existing library for plugins, where possible, that we don’t need to solely maintain.

Furthermore, the existing system does not afford alternate plugin runtimes. 
In-process plugins have the benefit of being capable of a wide variety of changes, but come at the cost of a poor operator story in that service and plugin deploy cadences must be in lock-step. 
This model is fine for Spinnaker installations where the operator is also the plugin developer, but offers a poor story for environments where the plugin developer and Spinnaker service operators are not the same group and do not want to coordinate and be coupled to each other’s deploy cadences.

The proposed plugin system will aim for feature parity of the existing plugin system, but will not guarantee backwards compatibility of developer or operator experience. 
The priority is in satisfying broad-strokes business requirements, as opposed to satisfying existing technical capabilities.

## Known Unknowns
There are a lot of known unknowns. 
It’s the intention that separate proposals will be made for many of these unknowns, such as development and publishing lifecycle, as well as operator experiences.

* In-process plugins
	* How will plugin authors get metrics, logs, or crash reports from their plugins?
	* How will operators be notified that new plugin versions exist and can be released?
	* Do we want to support updating backend plugins at runtime?
* Deck plugins
	* We want to experiment with some Webpack magic to achieve plugin loading, although we’re unsure if this will be possible yet.
* Service interaction
	* How do we handle behavior of faulty plugins?
		* If a plugin brings a service down entirely, should there be some mechanism for tracking which plugin is at fault? How would this be exposed?

## Security, Privacy, and Compliance
In-process plugins present an interesting security issue. 
Plugins can introduce new dependencies which expose CVEs, they can access internal APIs that may break core—or other plugins—behavior. 
Further, a story around metrics, logs and error reporting for plugin developers becomes harder as there’s greater possibility for leaking potentially sensitive information to parties that do not necessarily have organizational approval for such data.

Initially speaking, things like logs, metrics and crash reports for in-process plugins will go unaddressed, putting the sole burden of security judgment on the Operator.

In-process plugins are not fully isolated, so close scrutiny of a plugin’s behavior is mandatory for any Operator prior to installation or enabling.

## Operations
This proposal alters how Spinnaker development will be performed, as well as how Spinnaker functionality is released and delivered to various environments. 
We do not yet know the exact ways that this will change, however the goal is to make development of Spinnaker—for both core and extensions—easier.

## Risks
* There is an existing plugin system that has made assumptions on plugin loading. 
There is risk that switching its internal functionality to PF4J may be in some ways backwards incompatible; such as how plugins are detected, or how config is loaded for them.
	* This will likely cause breakages in existing plugins that will need to be migrated.
* Service rollbacks when in-process plugins are being used will also rollback plugin versions, which may be undesired behavior.
* Having Front50 as the source of truth for what plugins are installed across the system, and which ones are enabled or disabled introduces it as a single point of failure for a yet unknown, wide-sweeping segment of functionality for Spinnaker.
	* Should Front50 be unavailable, services should use the shipped / baked config as defaults.
	* Gate should store an in-memory cache of the desired plugin state such that Deck may have less dependence on a healthy Front50 deployment.
