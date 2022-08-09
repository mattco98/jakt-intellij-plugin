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
        - Function parameters resolve to their usages in the function body (and _not_ the aforementioned argument labels)
        - Shorthand enum names in match cases and `is` expressions
    - <details>
      <summary>Type declaration from identifiers when ctrl-clicking</summary>
      <img src="https://i.imgur.com/AoIVqWF.gif" />
      </details>
- Rename refactoring
    - Works for any identifier that supports resolution (declaration, local variable, imports, etc)
- Run Configurations
    - Gutter icons for `main` function allow easily running scripts with custom arguments 

## Contributing

There is no special setup needed for the project; simply open it in IntelliJ, and run the `Test` task for tests, and
the `Run IDE for UI Tests` to launch a clean version of IntelliJ with the plugin installed. 

The `buildPlugin` task will build the plugin to `build/libs`, which can be installed manually in the plugins tab in
settings.

### TODO (in order of importance)

- GitHub Actions: Automatically run tests for commits and PRs
- Grammar
    - Improve parsing errors
    - Implement error recovery (currently highlighting breaks for invalid files)
- Complex refactoring (move/delete)
- Most of the features mentioned in the IntelliJ [Custom Language Support tutorial](https://plugins.jetbrains.com/docs/intellij/additional-minor-features.html)
