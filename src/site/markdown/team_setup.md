# TEAM Engine Setup Instructions

These instructions are for system administrators and test users who want
to deploy their own instance of TEAM Engine on their own infrastructure,
and use this ETS. There are a few extra steps required to use this ETS
compared to other standard test suites provided by OGC.

## Software Environment

This guide assumes you are using this specific set of hosting software.
Some minor version changes may work, although they are not guaranteed.

* OpenJDK JRE 8 or Oracle JRE 8
* Tomcat 7
* TEAM Engine v5.3
* ETS Security Client 1.0 (latest release)
* Git
* Maven 3

This guide will be using [Debian Stretch][debian] as the operating system. 
It should work with minor changes for other Linux distributions or for 
BSD/MacOS systems. For Windows, I recommend following the 
[official TEAM Engine guide][official] and then using this guide for 
deploying the ETS.

[debian]: https://www.debian.org/releases/
[official]: https://github.com/opengeospatial/teamengine/blob/master/doc/en/index.rst

## 1. Install Java

OpenJDK is available in most Linux package managers; Oracle JDK must be
manually downloaded from Oracle and installed using their instructions.

For OpenJDK:

```sh
$ sudo apt update
$ sudo apt install openjdk-8-jdk-headless
$ java -version
openjdk version "1.8.0_181"
OpenJDK Runtime Environment (build 1.8.0_181-8u181-b13-1~deb9u1-b13)
OpenJDK 64-Bit Server VM (build 25.181-b13, mixed mode)
$ javac -version
javac 1.8.0_181
```

## 2. Configure JAVA_HOME

It is important to have the location of the Java home directory configured
for various apps, including Tomcat.

By default on Debian, the java directory is located at 
`/usr/lib/jvm/java-8-openjdk-amd64`. This may vary on your OS.

To set the environment variable, add the following line to the end of
`/etc/profile`:

```sh
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"
```

After reloading your shell session, you should be able to check the
environment variable:

```sh
$ echo $JAVA_HOME
/usr/lib/jvm/java-8-openjdk-amd64
```

## 3. Install Maven

Maven 3 is available in the Debian apt repository.

```sh
$ sudo apt install maven
$ mvn -version
Apache Maven 3.3.9
Maven home: /usr/share/maven
Java version: 1.8.0_181, vendor: Oracle Corporation
Java home: /usr/lib/jvm/java-8-openjdk-amd64/jre
Default locale: en_US, platform encoding: ANSI_X3.4-1968
OS name: "linux", version: "4.9.93-linuxkit-aufs", arch: "amd64", family: "unix"
```

## 4. Install Git

Git is available in the Debian apt repository.

```sh
$ sudo apt install git
$ git --version
git version 2.11.0
```

## 5. Install Tomcat 7

Tomcat 7 is not available in the apt repository (only version 8), so we
will download and install from source instead. Find the latest `tar.gz`
files from the [Tomcat Download page](https://tomcat.apache.org/download-70.cgi).

```
$ sudo apt install wget
$ cd /tmp
$ wget http://muug.ca/mirror/apache-dist/tomcat/tomcat-7/v7.0.91/bin/apache-tomcat-7.0.91.tar.gz
$ wget https://www.apache.org/dist/tomcat/tomcat-7/v7.0.91/bin/apache-tomcat-7.0.91.tar.gz.sha512
$ sha512sum -c apache-tomcat-7.0.91.tar.gz.sha512
apache-tomcat-7.0.91.tar.gz: OK
```

Note: That last line MUST print OK; if it does not, then your Tomcat 
download may be invalid or has been tampered with.

Also install Tomcat native, for additional OS support:

```sh
$ sudo apt install libtcnative-1
```

Next is installing Tomcat to `/opt`.

```sh
$ tar xzvf apache-tomcat-7.0.91.tar.gz
$ sudo cp -r apache-tomcat-7.0.91 /opt/tomcat7
```

Install an init script to auto-start and stop the server, using
`src/site/resources/tomcat.sh` as the source, to `/etc/init.d/tomcat`.
It contains modifications for including the TEAM Engine base directory
as a JVM option.

The init script should be executable:

```sh
$ sudo chmod +x /etc/init.d/tomcat
```

Create a user/group for Tomcat:

```sh
$ adduser tomcat --home /opt/tomcat7 --no-create-home --system --group
```

And modify the permissions of the Tomcat directory for the Tomcat user:

```sh
$ sudo chown -R tomcat:tomcat /opt/tomcat7
```

You can try running Tomcat and see if it works in your browser:

```
$ sudo service tomcat start
```

It should be available at http://host:8080/, where `host` is the IP of
the machine running Debian.

## 6. Download TEAM Engine Source

The source for [TEAM Engine is available on GitHub][github]. We will
use Git to get version 5.3. We will only clone enough code to get that
release tag.

```sh
$ cd $HOME
$ git clone --branch 5.3 --depth 1 https://github.com/opengeospatial/teamengine.git
```

Next build the source to generate the packages for Tomcat.

```sh
$ cd $HOME/teamengine
$ mvn install
```

[github]: https://github.com/opengeospatial/teamengine

## 7. Setup of TEAM Engine Base Directory

A base directory must be set up for TEAM Engine to contain the setup
scripts for the test suites.

```sh
$ sudo apt install unzip
$ sudo mkdir /opt/te_base
$ sudo unzip teamengine-console/target/teamengine-console-5.3-base.zip -d /opt/te_base
$ sudo chown -R tomcat:tomcat /opt/te_base
```

## 8. Deploy TEAM Engine to Tomcat

First install the libraries for Tomcat:

```sh
$ sudo unzip teamengine-web/target/teamengine-common-libs.zip -d /opt/tomcat7/lib
```

Then we install the WAR file to Tomcat's webapps:

```sh
$ sudo cp teamengine-web/target/teamengine.war /opt/tomcat7/webapps/.
```

Tomcat, if running, will automatically extract the WAR file. If Tomcat is
not running, start it with the service command:

```sh
$ sudo service tomcat start
```

## 9. Download ETS Source

Use git to download the latest source for the test suite:

```sh
$ cd $HOME
$ git clone https://github.com/opengeospatial/ets-security-client10
```

And compile:

```sh
$ cd ets-security-client10
$ mvn install
```

## 10. Deploy ETS to TEAM Engine

The control scripts and sample files need to be installed to the base
directory of TEAM Engine.

```sh
$ cd $HOME/ets-security-client10
$ sudo unzip target/ets-security-client10-0.2-SNAPSHOT-ctl.zip -d /opt/te_base/scripts
```

Additionally, copy the sample Java Keystore to the TE Base so we can
reference it in test sessions. If you have your own keystore, then skip
this step.

```sh
$ sudo cp src/main/resources/security.jks /opt/te_base/ets-security-client10.jks
$ sudo chown -R tomcat:tomcat /opt/te_base
```

Next, install the dependent libraries.

```sh
$ sudo unzip target/ets-security-client10-0.2-SNAPSHOT-deps.zip -d /opt/tomcat7/webapps/teamengine/WEB-INF/lib
$ sudo chown -R tomcat:tomcat /opt/tomcat7/webapps/teamengine/WEB-INF/lib
```

Finally, the system administrator **must** customize the CTL script for
their installation. It is located at 
`/opt/te_base/scripts/security-client10/1.0/ctl/security-client10-suite.ctl`.
In this file, you will have to scroll down to the `<properties>` element,
and edit the `host`, `port`, `jks_path`, and `jks_password` parameters.

You will probably want the `host` parameter to be `0.0.0.0` or a specific
IP if you want this test server to be accessible outside of the current
machine.

The `port` should not conflict with any other services running on the 
machine.

The `jks_path` should point to either your own Java Keystore, or the 
sample keystore from this ETS.

The `jks_password` is required for the test server to unlock the keystore.

These settings will apply to all test sessions ran by this TEAM Engine
instance. While a test user may normally edit these values when running
the ETS as a JAR or inside an IDE, a test user should not have access to
these parameters inside TEAM Engine; being able to edit the values remotely
could pose a security risk.

## 11. Run ETS

The ETS should now be installed in TEAM Engine, and you should be able to
create a new test session for Security Client 1.0.
