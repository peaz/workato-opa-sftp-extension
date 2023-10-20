# Workato SFTP Extension

Current tested to work with SFTP server using App Password credentials.

## Building extension

Steps to build an extension:

1. Install the latest Java 17 SDK
2. Use `./gradlew jar` command to bootstrap Gradle and build the project.
3. The output is in `build/libs`.

## Installing the extension to OPA

1. Add a new directory called `ext` under Workato agent install directory.
2. Copy the extension JAR file to `ext` directory. Pre-build jar: [workato-sftp-connector-0.2.jar](build/libs/workato-opa-sftp-extension-0.2.jar)
3. Also include the [jsch-0.1.55.jar](https://repo.mavenlibs.com/maven/com/jcraft/jsch/0.1.55/jsch-0.1.55.jar) dependency to  the `ext` directory.
4. Update the `config/config.yml` to add the `ext` file to class path.

```yml
server:
   classpath: /opt/opa/workato-agent/ext
```

4. Update the `conf/config.yml` to configure the new extension.

```yml
extensions:
   sftp:
      controllerClass: com.knyc.opa.SFTPExtension

```

## Custom SDK for the extension

The corresponding custom SDK can be found here in this repo as well.

Link: [opa-sftp-connector.rb](custom-sdk/opa-sftp-connector.rb)

Create a new Custom SDK in your Workato workspace and use it with the OPA extension.