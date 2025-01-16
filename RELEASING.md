# Releasing

- Update the version number in the README (ie. the installation instructions)
- Ensure `CHANGELOG.md` is updated with the changes since the last release, and then change the `Unreleased` heading to the next version number and add a new empty `Unreleased` section.
- Commit changes, push, and open a release preparation pull request for review.
- Once the pull request is merged, fetch the updated `main` branch.
- Apply a tag for the new version on the merged commit (e.g. `git tag -a v1.2.3 -m "v1.2.3"`). The tag **must** start with a `v` in order for the release tooling to pick it up.
- Push the tag upstream (this will kick off the release pipeline in CI) e.g. `git push origin v1.2.3`
- Copy `CHANGELOG.md` entries for newest version into draft GitHub release created as part of CI publish steps.
  - Make sure to "generate release notes" in GitHub for full changelog notes and any new contributors
- Publish the GitHub release.
