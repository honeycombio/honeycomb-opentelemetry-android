# Releasing

- Update `CHANGELOG.md` with the changes since the last release.
  - One quick way to do this is to run:
    ```shell
    git log --pretty='%C(green)%d%Creset- %s | %an'
    ```
  - Copy the latest commits and paste them into `CHANGELOG.md`
  - Update the author names to GitHub handles
- Commit changes, push, and open a release preparation pull request for review.
- Once the pull request is merged, fetch the updated `main` branch.
- Apply a tag for the new version on the merged commit (e.g. `git tag -a v1.2.3 -m "v1.2.3"`). The tag **must** start with a `v` in order for the release tooling to pick it up.
- Push the tag upstream (this will kick off the release pipeline in CI) e.g. `git push origin v1.2.3`
- Copy `CHANGELOG.md` entries for newest version into draft GitHub release created as part of CI publish steps.
  - Make sure to "generate release notes" in GitHub for full changelog notes and any new contributors
- Publish the GitHub release.
