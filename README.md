smaller-node-builder
====================

A maven mojo to create a smaller bundle containing a npm-module

    <plugin>
      <groupId>de.matrixweb.smaller</groupId>
      <artifactId>smaller-node-builder-maven-plugin</artifactId>
      <version>0.0.1-SNAPSHOT</version>
      <executions>
        <execution>
          <configuration>
            <!-- browserify is working on js -->
            <type>js</type>
            <!-- npm module to prepare for smaller -->
            <package>browserify@2.33.1</package>
            <!-- the script to call browserify with -->
            <script>
              var fs = require('fs');
              var b = browserify();
              b.add(command.path + '/main.js');
              b.bundle().pipe(fs.createWriteStream('/tmp/min.js'));
              return command.path + '/main.js';
            </script>
          </configuration>
          <goals>
            <goal>smaller-node-builder</goal>
          </goals>
        </execution>
      </executions>
    </plugin>

