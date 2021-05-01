# Third-party dependencies

## Add a new dependency

Contributors who want to add a new dependency need to

1. Add the artifact id in `ARTIFACTS =` constant in `deps.bzl`.
2. Run
   ```sh
   bazel run @unpinned_maven//:pin
   ```
