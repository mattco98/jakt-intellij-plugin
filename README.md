# Jakt IntelliJ Plugin

A fully-featured plugin for the [Jakt](https://github.com/SerenityOS/jakt) programming language.

## Features

- Highlighting
    - Fully-configurable via a Color Scheme page
    - Material-like defaults for Darcula
    - Semantic-based. Examples:
        - Static method calls vs instance method calls
        - Mutable vs immutable local variables
- Validation
    - Flexible BNF-based language description detects parsing errors early
    - Displays type-checking and advanced parsing errors from the Jakt compiler
- Completions
    - Completes after plain identifiers, field access expressions, and namespace qualifiers
    - Intelligent completions (example: not suggesting static methods for a field access expression)
    - <details>
      <summary>Function template completion</summary>
      <img src="https://i.imgur.com/ruRKiDR.gif" />
      </details>
    - Completes prelude types/functions
- Intentions
    - Basic intention support (currently only has one very simple intention)
- Resolution
    - Two-way resolution (ref -> decl, decl -> all refs)
    - Sees through import statements
    - Complex resolution support. Example:
        - Function argument labels resolve to the respective parameter
        - Function parameters resolve to their usages in the function body (and _not_ the aformentioned argument lables)
        - Shorthand enum names in match cases and `is` expressions
    - <details>
      <summary>Type declaration from identifiers when ctrl-clicking</summary>
      <img src="https://i.imgur.com/AoIVqWF.gif" />
      </details>
- Rename refactoring
    - Works for any identifier that supports resolution (declaration, local variable, imports, etc)

## Contributing

The plugin uses [JNA](https://github.com/java-native-access/jna) to communicate directly with the Jakt compiler. This requires custom Rust bindings. To compile the bindings:

1. Enter the `jakt-jna-binding` directory
2. Execute `cargo update`
    - This has to be done every time the upstream Jakt repository changes to update the bindings
3. Run `build_bindings.sh`
    - This compiles the bindings and the Jakt compiler into a `libjakt.so`, which is copied to `src/main/resources/` and used by the JNA interface.

After compiling the bindings, it is as simple as running the `Run IDE for UI Tests` launch configuration.

### TODO (in order of importance)

- [`DocumentationProvider`](https://plugins.jetbrains.com/docs/intellij/documentation.html)
- Github Actions: Automatically run tests for commits and PRs
- Grammar
    - Improve parsing errors
    - Implement error recovery (currently highlighting breaks for invalid files)
- [Nav bar](https://plugins.jetbrains.com/docs/intellij/navbar.html)
- Inlay hints? Perhaps for local variable whose type isn't obvious (i.e. not `let a = Foo()`)
- Move left/right handler
- More intentions
    - Function arrow body to block body
    - Match arrow body to block body
    - etc...
- Complex refactoring (move/delete)
- More complex tests (completion/intentions)
- Most of the features mentioned in the IntelliJ [Custom Language Support tutorial](https://plugins.jetbrains.com/docs/intellij/additional-minor-features.html)
- Anything required for publishing the plugin to the JetBrains Marketplace (icons, metadata, CLion compat, etc)
