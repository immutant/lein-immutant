# lein-immutant

A [Leiningen](http://leiningen.org/) plugin for deploying Immutant 2.x
applications to a [WildFly](http://wildfly.org/) application server.

## Usage

**Note:** as of Immutant 2.x, you /only/ need this plugin if you are
deploying your applications to a WildFly container. See [somewhere]
for more info.

### Installation

The current version is a snapshot:

    [lein-immutant "2.0.0-SNAPSHOT"]

### Usage

The plugin currently provides one subtask: `lein immutant war`. This
generates a `.war` file suitable for deploying to WildFly.

See
[the help file for the war task](docs/war.md)
for details.

## License

Copyright (C) 2011-2014 Red Hat, Inc.

Licensed under the Eclipse Public License v1.0
