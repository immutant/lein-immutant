# lein-immutant

A [Leiningen](http://leiningen.org/) plugin for using Immutant 2.x
applications with a [WildFly](http://wildfly.org/) application server.

**Note:** as of Immutant 2.x, you *only* need this plugin if you are
deploying your applications to a WildFly container. See [somewhere]
for more info.

**Note deux:** this plugin requires Leinigen 2.4.0 or greater.

## Installation

The current version is a snapshot:

    [lein-immutant "2.0.0-SNAPSHOT"]

## Usage

The plugin currently provides two subtasks:

* `lein immutant war` - This generates a `.war` file suitable for
  deploying to WildFly. See
  [the help file for the war task](docs/war.md) for details.

* `lein immutant test` - This deploys the application to WildFly, and
  runs the tests. See [the help file for the test task](docs/test.md)
  for details.

## License

Copyright (C) 2011-2014 Red Hat, Inc.

Licensed under the Eclipse Public License v1.0
