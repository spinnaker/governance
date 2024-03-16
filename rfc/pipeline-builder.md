# Pipeline Builder Library

|               |                                                                    |
| ------------- |--------------------------------------------------------------------|
| **Status**    | Proposed                                                           |
| **RFC #**     | [350](https://github.com/spinnaker/governance/pull/350)            |
| **Author(s)** | Nicolas Favre-Felix ([`@nicolasff`](https://github.com/nicolasff)) |
| **SIG / WG**  | Platform SIG                                                       |

## Overview

Add a new Java library to Spinnaker that provides a clean API with a fluent/builder style to build and maintain pipelines as testable code instead of storing and editing them in JSON directly.

Repository: https://github.com/nicolasff/pipeline-builder

### Example

A pipeline is built by creating a class that extends the `JsonPipelineBuilder` abstract class and implements two methods:

- `getUniqueName()`: returns a unique name for the pipeline, used to generate a stable pipeline definition ID.
- `buildPipeline()`: returns a `Pipeline` object, which can then be serialized to `String` with `toJson()`.

<details>
<summary>Expand to see a complete pipeline builder class</summary>

```java
public class TutorialPipelineBuilder extends JsonPipelineBuilder {

    @Override
    public String getUniqueName() {
        return "examples.wait-and-evaluate";
    }

    @Override
    protected Pipeline buildPipeline() {
        Stage waitStage = Stage.builder()
            .type(StageTypes.WAIT)
            .name("Wait a moment")
            .contextObject(WaitContext.ofSeconds(5))
            .build();

        Stage evalSumStage = Stage.builder()
            .type(StageTypes.EVALUATE_VARIABLES)
            .name("Evaluate sum")
            .parentStage(waitStage)
            .context(Map.of("variables", List.of(
                Map.of(
                    "key", "sum",
                    "value", "${ #toInt(parameters.a) + #toInt(parameters.b) }"))))
            .build();

        return Pipeline.builder()
            .parameters(List.of(
                PipelineParameter.builder()
                    .name("a")
                    .defaultValue("17")
                    .build(),
                PipelineParameter.builder()
                    .name("b")
                    .defaultValue("25")
                    .build()))
            .name("Tutorial")
            .stages(List.of(waitStage, evalSumStage))
            .build();
    }
}
```

</details>

Once built as a `Pipeline` object, the corresponding JSON can be generated easily:
```java
    Pipeline pipeline = new TutorialPipelineBuilder().build();
    String json = pipeline.toJson();
```

### Goals and Non-Goals

Goals:

- Provide a clean, fluent API to build and maintain Spinnaker pipelines as code
- Leverage Java features and tools to make pipelines more readable and maintainable, such as:
  - Version control
  - Type safety
  - Testability
  - IDE support
  - Code completion
  - Refactoring
- Focus on the developer experience, and offer simpler alternatives when the underlying structure is complex or obscure
- Let users validate their pipelines with code, adding their own tests to the library's built-in safeguards

Non-goals:

- Replace the Deck web interface
- Replace the JSON format
- Upload pipelines to Spinnaker or access the Spinnaker API in any way
- Convert pipelines from JSON to Java

## Motivation and Rationale

Building pipelines using the Deck web interface is convenient for most users, especially those who are just discovering Spinnaker. But as pipelines become more complex and as users become more familiar with Spinnaker, this approach starts to show its limits.

Spinnaker users are often developers or are at least familiar with development tools, but cannot realistically use them to build and maintain their pipelines. Pipelines are stored in a JSON format, which makes them particularly difficult to maintain in a version control system. Editing JSON is highly error-prone, and users who attempt to maintain pipelines this way cannot rely on development tools like IDEs, or language features like type checking for additional safety. JSON pipelines are also difficult to test, and mistakes are often only caught at runtime.

The `pipeline-builder` Java library provides a new way to build and maintain pipelines as code, with a clean API that focuses on readability and developer experience. Users can benefit from the same tools and practices they use to maintain their application code, such as version control, code review, testing, refactoring, and IDE support.
The library defines a number of "builder" classes with a fluent style, where objects are created and configured using method chaining without forcing a specific order of operations.

## Timeline

The Spinnaker team at Apple started working on this library in 2020, and migrated its internal CI pipelines from checked-in JSON files when it was still in its early stages. We have been using and expanding the library since then, and know of multiple teams using it to maintain their own pipelines. Our users typically write a single program capable of generating all the pipelines they need, feeding its JSON output to `curl` to install them in Spinnaker via the API.

We are now (March 2024) proposing to open-source the library and contribute it to the Spinnaker project, under the Apache 2.0 license.

## Design

Each major object type in a pipeline (`Stage`, `Pipeline`, `ExpectedArtifact`) has a dedicated class with a `builder()` static method that returns a `Builder` object. This builder is used to set the object's properties one by one by chaining method calls, with the final `build()` call returning the concrete object.

When a field can accept objects of different types, the library usually provides a class for each concrete implementation, and a common interface or abstract base class that they all implement. For example, a `PipelineTrigger` or `WebhookTrigger` can be used with a field expecting a `Trigger` object.

### Pre-defined helpers

The library comes with a number of pre-defined helper classes, enums, and constants that improve readability and type safety. For example, the `StageTypes` class contains constants for many stage types supported by Spinnaker (e.g. `StageTypes.WAIT` or `StageTypes.Kubernetes.DEPLOY_MANIFEST`).

For some stage types, we've also implemented dedicated context classes that can be used instead of a generic `Map<String, Object>` to configure its inputs in `context`. These classes are usually named after the stage type (e.g. `WaitStageContext`), and also come with a builder. This allows the library to provide additional type safety and validation, and to offer a more natural API; the only difference for the `Stage` builder is the use of `.contextObject()` instead of `.context()`, which takes a generic `Map` value:

```java
Stage healthCheckStage = Stage.builder()
    .name("Call health check endpoint")
    .type(StageTypes.WEBHOOK)
    .contextObject(WebhookContext.builder()
        .method(Method.GET)
        .url("https://my-service/healthcheck")
        .headers(Map.of("Accept", "application/json"))
        .build())
    .build();
```

Only a few stage types currently have dedicated context classes, but more will be added in the future. As long as they implement the empty `ContextObject` interface, library users can also define their own.

### JSON structure vs. developer experience

In some cases, it can be preferable to have a higher-level builder API that does not necessarily correspond one-to-one to all the individual JSON fields.

#### Stage IDs

For example, Spinnaker pipelines contain a list of `Stage` objects, each having a string identifier to represent its unique ID, stored in the `refId` field. These are strings, although they typically contain a number. The first stage might have `refId: "1"`, and the second would have both `refId: "2"` and a parent link with `requisiteStageRefIds: ["1"]`. The library auto-generates similar IDs starting at `"1"`.

For this field, the library uses the more natural names `id`, `parentId`, and `parentIds`. Two additional methods named `parentStage` and `parentStages` are also provided to relate `Stage` objects to each other, without having to use `refId` or `requisiteStageRefIds` directly. A few other fields also come with singular and plural variants, e.g. `inputArtifact(s)` or `trigger(s)` to avoid single-element lists.

Internally the library maps these names to their corresponding JSON fields, so that the value generated for `id` (or provided with `id(String)`) is stored in the `refId` field when the `Stage` object is serialized to JSON. Similarly, the `parentStage` method will automatically set the `requisiteStageRefIds` field.

We think this approach makes the library easier to use, and leads to more readable code. Compare:

```java
// actual API
Stage stage2 = Stage.builder()
    .parentStage(stage1)
    // ...
    .build();

// vs. what a more literal translation of the JSON would look like:
Stage stage2 = Stage.builder()
    .refId("2")
    .requisiteStageRefIds(List.of(stage1.getRefId()))
    // ...
    .build();
```

#### Stage failure booleans

Stages include three booleans that define what should happen to the pipeline execution on failure:

| UI label (shortened)          | `failPipeline` | `completeOtherBranchesThenFail` | `continuePipeline` |
|-------------------------------|----------------|---------------------------------|--------------------|
| Halt entire pipeline          | `true`         | `true`                          | `false`            |
| Halt current branch           | `false`        | `false`                         | `false`            |
| Halt branch and fail pipeline | `false`        | `false`                         | `true`             |
| Ignore failure                | `false`        | `false`                         | `true`             |

While all three booleans are available in the `Stage` builder, a single `onFailure` method can also set all three at once. It takes a `FailureStrategy` enum value, which can be one of `HALT_ENTIRE_PIPELINE`, `HALT_CURRENT_BRANCH`, `HALT_BRANCH_AND_FAIL_PIPELINE`, or `IGNORE_FAILURE` – with very similar names to the UI labels.

```java
Stage stage = Stage.builder()
    .name("Deploy")
    .type(StageTypes.Kubernetes.DEPLOY_MANIFEST)
// ... any other calls needed to build the stage
    .onFailure(FailureStrategy.HALT_BRANCH_AND_FAIL_PIPELINE)
    .build();
```

### Dependencies

The `pipeline-builder` library makes heavy use of two Java libraries to generate pipelines, [Jackson](https://github.com/FasterXML/jackson) and [Lombok](https://projectlombok.org/).
Jackson provides an easy way to transform data classes to JSON, and Lombok uses annotations on these data classes to generate methods at compile time to make the creation of pipeline-related objects as simple as possible.

**note:** In this context, _data classes_ are class definitions that describe objects holding values. They do not perform any sort of computation on this data and their objects are generally immutable once constructed. Most of the classes exported by the library follow this model.

#### Lombok

`pipeline-builder` relies on Lombok to generate the boilerplate code required to create data classes. This allows the library's code to focus on the data itself, and not on the code required to add getters, setters, builders, etc. In particular, the `@Builder` annotation is used with constructors that take all the object's properties as parameters, generating a builder class with one method per constructor parameter – each method having the same name as its corresponding parameter.

## Drawbacks

- The library is not a replacement for Deck, and cannot be used to edit pipelines in Spinnaker.
- It requires some amount of knowledge of the internal format used by Spinnaker to represent pipelines and stages, especially when it comes to their inputs in the `context` map. This is somewhat mitigated by the availability of dedicated context classes for a few stage types, but more will need to be added in the future.
- As Spinnaker evolves, the library will need to be updated to support new features, stage types, and their context classes.
- No dependency exists on the Spinnaker codebase, so the library cannot automatically benefit from changes made to Spinnaker (more about this below).

## Alternatives and Prior Art

Some users already build and maintain their pipelines using other generators, often based on a different source language or on a templating language. Examples include [HCL](https://github.com/hashicorp/hcl) and [Jsonnet](https://jsonnet.org/) with [Sponnet](https://github.com/spinnaker/sponnet).

While these tools help simplify the syntax of the pipeline definitions, they do not provide the same level of type safety, validation (with tests), refactoring support, and IDE compatibility as a library written in the same language as most of the Spinnaker codebase. They usually have some of the same drawbacks as the JSON format, in particular when it comes to poorly-named fields or the complex structures behind some common stage options.

They are also not generally testable with code, unless these tests are limited to validating the generated JSON.

## Evolution with Spinnaker

The very first attempts at creating this library were bringing in some Orca JARs as dependencies, and using the classes defined there for stage contexts. This approach was quickly abandoned, mostly because of the lack of clean "definition-only" modules in Spinnaker. The library would have had to pull in a lot of dependencies, and would have been tightly coupled to the Spinnaker codebase.

Defining "data classes" for the library's objects turned out to be a better approach, one that removed this large dependency on Spinnaker and didn't actually take much effort given the amount of boilerplate code that we avoid by using Lombok.

In the future, if Spinnaker is refactored to extract "API" modules that only define interfaces, data classes, and enums, the library could be updated to use these instead of its own definitions. This would allow it to automatically benefit from changes made to Spinnaker, and would also make it easier to contribute new stage types and context classes.

Alternatively, the `StageDefinitionBuilder` classes in Orca could move away from embedding `StageData` static classes and their associated `Task` classes could use the library's context classes directly. Orca already has an `orca-api` module that currently contains mostly interfaces and other type definitions, as opposed to logic or concrete implementations.

That said, there _are_ a few places where the builder methods do not have exactly the same name as the corresponding JSON fields (as explained above). With readability and maintainability as key goals, we would need to carefully consider any change that would make the library harder to use.

## Further Reading

The `examples` [directory](https://github.com/nicolasff/pipeline-builder/tree/main/examples) in the `pipeline-builder` repository provides additional information about the library:

- Its `src` [directory](https://github.com/nicolasff/pipeline-builder/tree/main/examples/src) features 7 complete pipeline builders, showcasing many of the library's features.
- In addition, the [README file](https://github.com/nicolasff/pipeline-builder/tree/main/examples) next to these examples is structured as both an introduction to the library and a tutorial. It goes over the choices made when building the library, covers the topics discussed in this RFC, and more.
