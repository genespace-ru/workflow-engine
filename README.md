
# To Build

Java 21 should be installed

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

# To Run command line tool

You will need java 21

To convert WDL to Nextflow:

```sh
java -jar WDL2Nextflow.jar <PATH_TO_WDL> 
```
Where <PATH_TO_WDL> is path to the WDL file. It will create file with the same name as <PATH_TO_WDL> with extension .nf

To generate visual diagram:
```sh
java -jar WDL2Nextflow.jar <PATH_TO_WDL> -i
```
Where <PATH_TO_WDL> is path to the WDL file. It will create image file with the same name as <PATH_TO_WDL> with extension .png

