smaller-node-builder
====================

A maven mojo to create a smaller bundle containing a npm-module

    <plugin>
      <groupId>de.matrixweb.smaller</groupId>
      <artifactId>smaller-node-builder-maven-plugin</artifactId>
      <version>1.0.0</version>
      <executions>
        <execution>
          <configuration>
            <!-- browserify is working on js -->
            <type>js</type>
            <name>browserify-2.34.0</name>
            <!-- npm module to prepare for smaller -->
            <packages>
              <package>browserify@2.34.0</package>
              <package>through@2.3.4</package>
            </packages>
            <!-- the bridge script between smaller and browserify -->
            <script>
              var browserify = require('browserify');
              var fs = require('fs');
              var through = require('through');
              var min = '';
              browserify()
                .add(command.path + '/' + command.in)
                .bundle().pipe(through(function write(data) {
                    min += data;
                  }, function end() {
                    fs.writeFileSync(command.out + '/output.js', min);
                    done();
                  }));
            </script>
          </configuration>
          <goals>
            <goal>smaller-node-builder</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
