## Jetty maven plugin
https://jetty.org/docs/jetty/12/programming-guide/maven-jetty/

The Jetty Maven plugin is useful for rapid development and testing. 
It can optionally periodically scan a project for changes and automatically redeploy the webapp if any are found.

This makes the development cycle more productive by eliminating the build and deploy steps: 
use an IDE to make changes to the project, and the running web container automatically picks them up, 
allowing them to be tested straight away.

### Run server
jetty:run -Djetty.http.port=8200