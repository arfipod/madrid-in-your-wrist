FROM ubuntu:24.04

ARG DEBIAN_FRONTEND=noninteractive
ARG ANDROID_CMDLINE_TOOLS_ZIP=commandlinetools-linux-11076708_latest.zip
ARG ANDROID_CMDLINE_TOOLS_URL=https://dl.google.com/android/repository/${ANDROID_CMDLINE_TOOLS_ZIP}
ARG ANDROID_PLATFORM=android-36
ARG ANDROID_BUILD_TOOLS=35.0.0
ARG GRADLE_VERSION=8.11.1

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:/opt/gradle/bin
ENV GRADLE_USER_HOME=/workspace/.gradle-cache

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    git \
    openjdk-17-jdk \
    unzip \
    zip \
    bash \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools /tmp/android-tools \
    && curl -fsSL ${ANDROID_CMDLINE_TOOLS_URL} -o /tmp/android-tools/${ANDROID_CMDLINE_TOOLS_ZIP} \
    && unzip -q /tmp/android-tools/${ANDROID_CMDLINE_TOOLS_ZIP} -d /tmp/android-tools \
    && mv /tmp/android-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm -rf /tmp/android-tools

RUN yes | sdkmanager --licenses >/dev/null || true \
    && sdkmanager \
        "platform-tools" \
        "platforms;${ANDROID_PLATFORM}" \
        "build-tools;${ANDROID_BUILD_TOOLS}"

RUN curl -fsSL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle \
    && rm /tmp/gradle.zip

WORKDIR /workspace

CMD ["bash"]
