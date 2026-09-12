FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

# NOTE: Google's command-line-tools filename embeds a build number that
# changes over time. If this URL 404s, grab the current one from
# https://developer.android.com/studio#command-line-tools-only and update it.
ARG CMDLINE_TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip

RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" \
    && cd /tmp \
    && curl -fsSL -o cmdline-tools.zip "${CMDLINE_TOOLS_URL}" \
    && unzip -q cmdline-tools.zip -d "${ANDROID_HOME}/cmdline-tools" \
    && mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest" \
    && rm cmdline-tools.zip

# Match compileSdk/targetSdk and build-tools used by app/build.gradle.kts.
RUN yes | sdkmanager --licenses >/dev/null \
    && sdkmanager --install "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null
