# CODEX Processes

Business processes for the NUM CODEX project (AP2) as plugins for the [HiGHmed Data Sharing Framework][1].

## Build

Prerequisite: Java 11, Maven >= 3.6

Add the Github Package Registry server to your Maven `.m2/settings.xml`. Instructions on how to generate the `USERNAME`
and `TOKEN` can be found in the HiGHmed DSF Wiki page with the
name [Using the Github Maven Package Registry](https://github.com/highmed/highmed-dsf/wiki/Using-the-Github-Maven-Package-Registry)
. The token need at least the `read:packages` scope.

```xml

<servers>
    <server>
        <id>highmed-dsf</id>
        <username>USERNAME</username>
        <password>TOKEN</password>
    </server>
</servers>
```

Build the project from the root directory of this repository by executing the following command.

```sh
mvn clean package
``` 

## Usage

You can test the processes by following the [README](codex-processes-ap2-docker-test-setup/README.md) in
the `codex-processes-ap2-docker-test-setup` directory.

## Edit
You should edit the *.bpmn files only with the standalone Camunda Modeller, because of different 
formatting of the bpmn tools and plugins.
https://camunda.com/download/modeler/

## License

Copyright 2021 Netzwerk Universitätsmedizin

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "
AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

[1]: <https://github.com/highmed/highmed-dsf>
