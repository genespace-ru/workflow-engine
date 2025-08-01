
# To Build

Java 17 should be installed

Build workflow-engine

```sh
mvn package -DskipTests
```

# To Run

Change paths in config file webapps/config.yml

```
repositories:
- workflow-engine/data/src/main/resources/GenericFDC
```

"repositories" list should contain absolute path to folders that will be loaded as repositories


Then launch workflow-engine web edition.

```sh
mvn jetty:run -Djetty.http.port=9998 -Dmaven.javadoc.skip=true -DskipTests=true

```

Use your browser to open it at http://localhost:9998/

