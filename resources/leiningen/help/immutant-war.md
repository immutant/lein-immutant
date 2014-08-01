Generates a war file suitable for deploying to a WildFly container.

The task's behavior can be configured with the following options under
the `[:immutant :war]` path in `project.clj`, all of which can be
overridden via command line switches.

* `:destination` - The directory where the war file should be placed.
  To ease deployment to WildFly, you can specify the root of your
  WildFly installation and the archive will be placed within the
  `standalone/deployments/` directory under that root instead of at
  the root. Defaults to `:target-path`.

  The destination can be specified on the command line as `-o DIR` or
  `--destination DIR`.

* `:dev?` - Tells the task to create a development war with the
  following characteristics:

  - The application source and resources aren't included in the
    archive, and instead are referenced where they are on disk.
  - The application's dependencies are also not included, and are
    referenced from `~/.m2/`.
  - An nREPL endpoint is started on a random port on localhost,
    and will have any middleware and `:repl-options` from your
    `project.clj` applied.

  The development war allows the `ring.middleware.reload` middleware
  to reload changed namespaces on disk, and doesn't require you to
  regenerate the war file after making source changes. You will still
  need to redeploy the application to see any changes that the reload
  middleware can't load, and you'll need to regenerate the war if you
  change any dependencies in `project.clj`.

  Defaults to `false`, which results in an uberwar containing all of
  the application's code, resources, and dependencies.

  Development mode can be specified on the command line as `-d` or
  `--dev`, and can only be enabled by the switch, not disabled.

* `:name` - Specifies the name of the war file (without the .war
  suffix).  Defaults to `%p%t`, and supports placeholders:

  - `%p` for the project name
  - `%v` for project version
  - `%t` for the type suffix (which will be -dev for dev wars, ;; ""
    for full wars).

  The name can be specified on the command line as `-n NAME` or
  `--name NAME`.

* `:nrepl` - A map that specifies the options for an nREPL endpoint.

  - `:host` - The host to bind to. Defaults to "localhost".
     Can be overridden on the command line via `--nrepl-host HOST`.
  - `:port` - The port to bind to. Defaults to `0`, which means a
    random port. Can be overridden on the command line via
    `--nrepl-port PORT`.
  - `:port-file` - The file where the nREPL port is written for
    tooling to pick up. Can be relative to the app root or absolute,
    and *must* be absolute when used with an uberwar. Defaults to both
    `.nrepl-port` *and* `:target-path/nrepl-port`, which is the same
    as lein's default. Note that when specifying a port file, you can
    only specify a single file. Can be overridden on the command line
    via `--nrepl-port-file FILE`.
  - `:start?` - Controls if an nREPL endpoint is started or not. For
    development wars, this is `true` by default, false otherwise. Can
    be overridden on the command line via `--nrepl-start`, and can
    only be enabled by the switch, not disabled.

* `:resource-paths` - A vector of directories containing resources
  that need to be copied to the top-level of the war file. These
  directories are different than the lein-standard :resource-paths, as
  those will be included in the war automatically. These directories
  are used to override or add configuration to `WEB-INF/` or
  `META-INF/` dirrectories within the war. Can be overridden on the
  command line via `-r PATH1,PATH2` or `--resource-paths PATH1,PATH2`.

### Example

```clojure
:immutant {
  :war {
    ;; the following will generate foo-0.1.0.war, or
    ;; foo-0.1.0-dev.war for dev wars.
    :name "foo-%v%t"

    ;; Destination defaults to :target-path
    :destination "path/for/war/"

    ;; contents placed at the top-level of the jar, useful for
    ;; overriding WEB-INF/web.xml, etc. If, after copying these resources,
    ;; we don't have a web.xml, we'll add our own.
    :resource-paths ["war-resources"]

    ;; override the nREPL settings
    :nrepl {:host "foo"
            :port 1234
            :port-file "path/to/port/file"
            :start? true}}}

:main my-app.core
```

### Notes

When generating an uberwar, we generate an uberjar using the standard
lein uberjar task, so all of the options for the uberjar task are
applied.

For both developer and uber wars, we generate a `WEB-INF/web.xml` that
acts as an entry point in to your application. As a convenience, we
drop a copy of that `web.xml` in to `:target-path` in case you need to
modify it. You'll want to place your copy in a directory in your
application root and point `[:immutant :war :resource-paths]` at it so
it will get picked up.
