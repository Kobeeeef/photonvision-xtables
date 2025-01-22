# Base image
FROM ubuntu:22.04

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV NODE_VERSION=18
ENV PYTHON_VERSION=3.11
ENV JAVA_VERSION=17

# Install common tools and dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    git \
    sudo \
    unzip \
    zip \
    software-properties-common \
    build-essential \
    libcholmod3 \
    liblapack3 \
    libsuitesparseconfig5 \
    graphviz \
    gcc-aarch64-linux-gnu \
    g++-aarch64-linux-gnu \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js
RUN curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | bash - \
    && apt-get install -y nodejs

# Install Python and pip
RUN add-apt-repository ppa:deadsnakes/ppa -y \
    && apt-get update \
    && apt-get install -y python${PYTHON_VERSION} python3-pip \
    && ln -sf /usr/bin/python${PYTHON_VERSION} /usr/bin/python \
    && python -m pip install --upgrade pip

# Install Gradle
RUN curl -s "https://get.sdkman.io" | bash \
    && bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install gradle"

# Install Java
RUN apt-get update && apt-get install -y openjdk-${JAVA_VERSION}-jdk

# Set Java environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

# Set working directory
WORKDIR /workspace

# Copy project files
COPY . /workspace

# Ensure Gradlew is executable
RUN chmod +x /workspace/gradlew

# Install Node.js dependencies
RUN cd photon-client && npm ci

# Install Python dependencies
RUN cd docs && pip install sphinx sphinx_rtd_theme sphinx-tabs sphinxext-opengraph doc8 && \
    [ -f requirements.txt ] && pip install -r requirements.txt || true

# Build the client
RUN cd photon-client && npm run build

# Build the docs
RUN cd docs && make html

# Install Arm64 toolchain
RUN cd /workspace && ./gradlew installArm64Toolchain

# Build LinuxArm64 JAR
RUN cd /workspace && ./gradlew photon-server:shadowJar -PArchOverride=linuxarm64

# Set up a volume for the output JAR files
VOLUME ["/output"]

# Copy the JAR to the output directory
RUN mkdir -p /output && cp photon-server/build/libs/*.jar /output/

# Set the default command to an interactive shell
CMD ["/bin/bash"]
