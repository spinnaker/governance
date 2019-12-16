
# Plugins UI Design Doc
| | |
|-|-|
| **Status** | **Proposed**, Accepted, Implemented, Obsolete |
| **RFC  Num** | [#74](https://github.com/spinnaker/community/pull/74) |
| **Author(s)** | Brandon Powell (`@bpowell`), Cameron Motevasselani (`@link108`), Clay Mccoy (`@claymccoy`) | Chris Thielen (`@christopherthielen`)
| **SIG / WG** | Platform SIG |
| **Obsoletes** | [Google Doc](https://docs.google.com/document/d/16WmRSziTJsSBZ1kuKUfVleLAYMIxmfvz/edit) |

## Motivation
Spinnaker is used by many companies, each having wildly different needs.  Many enterprise companies have heavily customized Spinnaker improve the experience based on their needs.  Some examples of extensions seen in the real world are: 

1. Custom stages
2. Modifying header and footer - show screenshot
3. Custom search filters - show screenshot
4. Custom routes - show screenshot
5. Custom details panels - show screenshot
6. Adding labels or other information to sections in the infrastructure tab
 
However, the process of customization necessitates a custom build of the spinnaker services and UI.  Adding a build step greatly increases the operational costs and complexity of running spinnaker.  Smaller companies with fewer resources likely lack the engineering and ops bandwidth to do this.

This proposal explores a world where the the UI (Deck) is extensible _without a build step_.  
  
This design doc references the [Plugins RFC](./plugins.md).

This doc focuses on:
* [Loading Plugin code into Deck](#Loading-Plugin-code-into-Deck)
* [Bootstrapping Deck](#Bootstraping-Deck)
* [Extension points](#Extension-Points)
* [Plugin development](#Plugin-development)

## Loading Plugin code into Deck

### Gathering plugin metadata
In order for Deck to know which plugins it can load, it must have access to some configuration information, including the name of the plugins that are enabled and where to download the plugin resources (JavaScript bundles, CSS, images, etc).  This Plugin Configuration will be fetched from Front50.

Please see the current [Plugins RFC](./plugins.md) for details around Front50 as the source of truth for plugin metadata.  Until the Front50 work is completed, a stop-gap solution using `settings.js` to locate the plugin resources should suffice.

### Loading plugin resources
The JavaScript Plugin resources will be fetched and loaded by Deck using native Javascript `import`.  Plugin resources must be served from a server that is configured to be [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) aware.

Some potential servers may be:
- An artifact store such as Artifactory
- Deck's standard Docker image.  Plugin resources would have to be located on-instance [1]
- A proxy service that serves Deck resources as well as resources from a storage service such as S3 (this is the case at Netflix)
- Front50 itself, via Gate.  This seems like a promising option.

[1] Halyard could configure an init container which would download plugin resources into the container.  The resources would be served by served by Apache in the same way that the standard resources are served.  Note: if such an init container were created, it could potentially serve the same purpose to front-load plugin assets for backend services.

## Bootstrapping Deck
Plugins will be able to use shared library code which is provided by Deck itself (see [Plugin Structure](#plugin-structure) below).  These shared libraries should be loaded before the plugins themselves.  Because of this, the bootstrapping process must change.  The order of operations likely should be:

- Load Deck assets
- Load plugin manifest
- Load plugin assets
- Deck Bootstrap
- Plugin Initialization
- Start Deck

####  Deck Bootstrap
Today, Deck is bootstrapped via AngularJS as soon as all the declared AngularJS modules dependencies and available.  We have to delay starting AngularJS while the plugins load. In order to delay the starting of AngularJS, we have to manually bootstrap the application after all the plugins are loaded.  A small snippet such as the this pseudocode can be added to `index.html`.

```
// loadPluginsPromises is an array of Promise
Promise.all(loadPluginsPromises).then(loadedPlugins => {
  // Tell AngularJS to start
  bootstrap(document.documentElement, ['netflix.spinnaker']);
}).catch(error => {
  // Handle plugin loading errors
});
```

### Plugin Initialization

During Deck's bootstrap, it should call all enabled plugins' `initialize()` method passing in a Plugin API object.  This object will expose a set of hand-picked functions that are hand picked for Spinnaker Core and register.

When each plugin initializes, it should register Extensions via the Plugin API object.  
Pseudocode:
```
export function initialize(registries: ISpinnakerRegistries) {
  registries.stageRegistry.register(myCustomStage);
}
```

## Extension Points

Deck should provides a set of well defined extension points and expose them via the Plugin API object.  A number of extension points exist today.  Examples include:

- Stage Registry
- Cloud provider Registry
- Data Source Registry
- Application tabs/routes
- Component Overrides

## Plugin Development

Plugin developers should have a development experience that closely resembles development of Deck itself.  Spinnaker should provide a plugin quick-start, such as a github repository that developers can clone. This quick start should provide:

- A standard build system
- Templates for creating specific extension points, i.e., an example stage
- A curated set of transitive dependencies (3rd party libraries) included in core Deck
- A set of reusable UI components that spinnaker itself is built upon
- A set of linter rules which match the code style in core Deck.

The build system will ideally build a plugin as ES6 module(s).  Deck will import these ES6 modules using native browser `import()`.  The build system should ideally support code splitting to enable lazy loading of portions of plugin code.  The build system should not include the transitive dependencies that are already loaded by core Deck. Instead, it should share those curated dependencies at runtime from the existing code loaded into core deck.  

## Known Unknowns
* Where are resources downloaded from? How do we ensure end users can download plugin resources?
* Failure handling.  
  * What should deck do if it fails to reach Front50 or to load plugins?
    * Spinnaker alert/banner at top of page should inform the user that plugins were not able to be loaded
      * Only show this warning if plugins are enabled
      * Give guidance on how to resolve error or look up more info
  * What should deck do if it fails to download a plugin resource? 
* Plugin manifests will contain locations where assets are, but another place may serve them (eg secure artifact store), can halyard help with this?

## Alternatives to Loading

### Build step
We may be able to modify Webpack to download plugins and compile Deck at deploy time. This eliminates the need for users to reach out to an artifact store and leads to a smoother UX. It would increase deploy times by a lot though, so that is something to consider. Going this path would also mean that Deck would have to be redeployed any time a new plugin has been added.

### RequireJS
RequireJS could be used to load plugins. When doing initial prototyping with RequireJS, it was complaining at compile time about not being able to find plugins because they are supposed to be added at runtime, not compile time. 
