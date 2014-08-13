Runs a project's tests inside WildFly.

Runs WildFly, deploys the project to it, runs all tests found beneath
the test/ directory, undeploys the app, and then shuts down
WildFly. All tests specified in the `:test-paths` from project.clj will
be executed.

The task's behavior can be configured with the following options under
the `[:immutant :test]` path in `project.clj`, all of which can be
overridden via command line switches.

* `:cluster?` - If true, the application will be deployed to the
  default cluster defined in the WildFly domain configuration (which
  defaults to two nodes), with the tests being run on one of the nodes.
  Defaults to `false`.

  Cluster mode can be specified on the command line as `-c` or
  `--cluster`.

* `:jboss-home` - Specifies the path to the WildFly install to be
  used.

  The path can be specified on the command line as `-j PATH` or
  `--jboss-home PATH`, or via the `$JBOSS_HOME` environment
  variable. `$JBOSS_HOME` will only be checked if the path isn't
  provided in `project.clj` or via the cli option.

* `:offset` - The test WildFly instance(s) is started with offset
  ports so it won't collide with a dev WildFly you may be running on
  the standard ports. By default, the ports are offset by 67.

  The offset can be specified on the command line as `-o OFFSET` or
  `--offset OFFSET`.

If you only want to run a subset of your tests inside or outside of
the container, you can separate the tests in to different
directories, and use profiles to activate the correct tests
in-container. Given the following project.clj snippet:

    :test-paths [\"outside-tests\"]
    :profiles {:inside {:test-paths ^:replace [\"inside-tests\"]}}

`lein test` will only run the outside tests, while
`lein with-profile inside immutant test` will only run the inside
tests within WildFly.

If you want to run all of the tests within WildFly, remove the
`:replace` metadata to have `inside-tests` added to the existing
`:test-paths`.
