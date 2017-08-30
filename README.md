[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

### TeamCity plugin to upload artifacts to external locations
----------------------------
[![Build Status](https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:bt402)/statusIcon)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=bt402)

This plugin adds basic deployment functions
to TeamCity continuous integration server
http://www.jetbrains.com/teamcity/

#### Installation

Copy zip archive to `%TeamCity_data_dir%/plugins`
and restart the server. Agents will be upgraded automatically

#### Usage

In build configuration settings, new runners will be available for build steps.
New runners include:
 * SMB Deployer   - upload artifacts via SMB (Windows shares)
 * FTP Deployer   - upload artifacts via FTP
 * SSH Deployer   - upload artifacts via SSH (using SCP or SFTP protocols)
 * SSH Exec       - execute arbitrary commands using SSH
 * Cargo Deployer - deploy WAR application archives to different servlet containers (based on Cargo library: http://cargo.codehaus.org)

 When configuring artifacts for upload, same patterns can be used as in "Artifacts Path" section of "General Settings"
 page. Including packaging artifacts to zip/tgz archives

#### Build

You need two JDK-s to build the plugin - maven must be run under jdk8, but agent modules must be compiled using jdk6.
You have to define path to the jdk6 using command line property:

    mvn package -Djava_16="C:\Program Files\Java\jdk1.6.0_45"
or

    mvn package -Djava_16=$(/usr/libexec/java_home -v 1.6)
on OS X

#### License

Apache, version 2.0
http://www.apache.org/licenses/LICENSE-2.0
