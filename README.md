# Feasibility Processes

Business processes for the MII feasibility project as plugins for the [Data Sharing Framework][1].

## Usage

For documentation of deployment and configuration of the feasibility processes see the [wiki][2].

## Development

### Build

Prerequisite: Java 17, Maven >= 3.6

Build the project from the root directory of this repository by executing the following command.

```sh
mvn clean package
```

### Testing

You can test the processes by following the [README](mii-process-feasibility-docker-test-setup/README.md) in
the `mii-process-feasibility-docker-test-setup` directory.

### Edit
You should edit the *.bpmn files only with the standalone Camunda Modeller, because of different
formatting of the bpmn tools and plugins.
https://camunda.com/download/modeler/

## License

Copyright 2025 Medizininformatik-Initiative

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "
AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

[1]: <https://github.com/datasharingframework/dsf>
[2]: <https://github.com/medizininformatik-initiative/mii-process-feasibility/wiki>
