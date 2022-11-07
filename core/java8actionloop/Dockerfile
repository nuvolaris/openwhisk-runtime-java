#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# build go proxy from source
FROM golang:1.16 AS builder_source
ARG GO_PROXY_GITHUB_USER=apache
ARG GO_PROXY_GITHUB_BRANCH=master
RUN git clone --branch ${GO_PROXY_GITHUB_BRANCH} \
   https://github.com/${GO_PROXY_GITHUB_USER}/openwhisk-runtime-go /src ;\
   cd /src ; env GO111MODULE=on CGO_ENABLED=0 go build main/proxy.go && \
   mv proxy /bin/proxy

# or build it from a release
FROM golang:1.16 AS builder_release
ARG GO_PROXY_RELEASE_VERSION=1.16@1.18.0
RUN curl -sL \
  https://github.com/apache/openwhisk-runtime-go/archive/{$GO_PROXY_RELEASE_VERSION}.tar.gz\
  | tar xzf -\
  && cd openwhisk-runtime-go-*/main\
  && GO111MODULE=on go build -o /bin/proxy

FROM ubuntu:20.04
LABEL maintainer="Michele Sciabarra michele@nuvolaris.io"
# configure dpkg && timezone

# add docker and java (amazon corretto) repos
RUN rm -rf /var/lib/apt/lists/* &&\
    echo 'debconf debconf/frontend select Noninteractive' | debconf-set-selections &&\
    apt-get clean &&\
    apt-get update &&\
    apt-get -y install gpg apt-utils &&\
    apt-get -y --no-install-recommends upgrade &&\
    apt-get -y install --no-install-recommends\
    python locales wget vim\
    software-properties-common\
    lsb-release ca-certificates\
    curl unzip telnet &&\
    rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/London \
    LANG="en_US.UTF-8" \
	LANGUAGE="en_US:en" \
	LC_ALL="en_US.UTF-8" 

RUN locale-gen en_US.UTF-8 &&\
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime ;\
    echo $TZ > /etc/timezone ;\
    useradd -m -d /home nuvolaris ;\
    chown nuvolaris:root /home 

# install java (amazon corretto)
RUN wget -O- https://apt.corretto.aws/corretto.key | apt-key add - ;\
    add-apt-repository 'deb https://apt.corretto.aws stable main' ;\
    apt-get update && apt-get -y install java-1.8.0-amazon-corretto-jdk

# select the builder to use
ARG GO_PROXY_BUILD_FROM=release

RUN mkdir -p /usr/java/src /usr/java/lib 
COPY --from=builder_source /bin/proxy /bin/proxy_source
COPY --from=builder_release /bin/proxy /bin/proxy_release
RUN mv /bin/proxy_${GO_PROXY_BUILD_FROM} /bin/proxy

ADD https://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.8.5/gson-2.8.5.jar /usr/java/lib/gson-2.8.5.jar
ADD https://jdbc.postgresql.org/download/postgresql-42.5.0.jar /usr/java/lib/postgresql-42.5.0.jar

ADD lib/src/Launcher.java /usr/java/src/Launcher.java
RUN cd /usr/java/src ;\
    javac -cp /usr/java/lib/gson-2.8.5.jar Launcher.java ;\
    jar cvf /usr/java/lib/launcher.jar *.class ;\
    chown -R nuvolaris:root /home /usr/java/lib ;\
    chmod -R 0770 /home /usr/java/lib

ADD bin/compile /bin/compile
ENV OW_COMPILER=/bin/compile
ENV OW_SAVE_JAR=exec.jar

USER nuvolaris
WORKDIR /home
ENTRYPOINT /bin/proxy
