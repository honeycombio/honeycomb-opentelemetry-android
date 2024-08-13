# This is necessary for --exclude.
# syntax=docker/dockerfile:1.7-labs

FROM cimg/android:2024.08

WORKDIR /usr/src/work

# For CI, it would be fine to copy the whole directory.
# But for local runs, we want to copy the stuff that's not in .gitignore.
COPY --chown=circleci:circleci core ./core
COPY --chown=circleci:circleci smoke-tests ./smoke-tests
COPY --chown=circleci:circleci gradle ./gradle
COPY --chown=circleci:circleci build.gradle.kts ./
COPY --chown=circleci:circleci gradle.properties ./
COPY --chown=circleci:circleci gradlew ./
COPY --chown=circleci:circleci gradlew.bat ./
COPY --chown=circleci:circleci settings.gradle.kts ./

# EXPOSE 1234
CMD [ "./gradlew", "test" ]
