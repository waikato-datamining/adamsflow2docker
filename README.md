# adamsflow2docker
Library for generating Docker images with an ADAMS workflow running inside.


## Command-line

```commandline
Converts ADAMS workflows into Docker images.


Usage: [--help] [-m MAVEN_HOME] [-u MAVEN_USER_SETTINGS]
       [-j JAVA_HOME] -M MODULES -V VERSION [-d DEPENDENCY...]
       [-D FILE...] [-J JAR_OR_DIR...] [-v JVM...] -i INPUT
       -b DOCKER_BASE_IMAGE [-I DOCKER_INSTRUCTIONS]
       -o OUTPUT_DIR

Options:
-m, --maven_home MAVEN_HOME
	The directory with a local Maven installation to use instead of the
	bundled one.

-u, --maven_user_settings MAVEN_USER_SETTINGS
	The file with the maven user settings to use other than
	$HOME/.m2/settings.xml.

-j, --java_home JAVA_HOME
	The Java home to use for the Maven execution.

-M, --module MODULES
	The comma-separated list of ADAMS modules to use for the application,
	e.g.: adams-weka,adams-groovy,adams-excel

-V, --version VERSION
	The version of ADAMS to use, e.g., '20.1.1' or '20.2.0-SNAPSHOT'.

-d, --dependency DEPENDENCY
	The additional maven dependencies to use for bootstrapping ADAMS
	(group:artifact:version), e.g.: nz.ac.waikato.cms.weka:kfGroovy:1.0.12

-D, --dependency-file FILE
	The file(s) with additional maven dependencies to use for bootstrapping
	ADAMS (group:artifact:version), one dependency per line.

-J, --external-jar JAR_OR_DIR
	The external jar or directory with jar files to also include in the
	application.

-v, --jvm JVM
	The parameters to pass to the JVM to launch the workflow with.

-i, --input INPUT
	The ADAMS workflow to use.

-b, --docker_base_image DOCKER_BASE_IMAGE
	The docker base image to use, e.g. 'openjdk:11-jdk-slim-buster'.

-I, --docker_instructions DOCKER_INSTRUCTIONS
	File with additional docker instructions to use for generating the
	Dockerfile.

-o, --output_dir OUTPUT_DIR
	The directory to output the bootstrapped application, workflow and
	Dockerfile in.
```

## Example

For this example we use the [weka_filter_pipeline.flow](src/main/flows/weka_filter_pipeline.flow)
workflow and the additional [weka_filter_pipeline.dockerfile](src/main/flows/weka_filter_pipeline.dockerfile)
Docker instructions. This workflow polls an input directory for ARFF files to clean with 
the [InterquartileRange](https://weka.sourceforge.io/doc.dev/weka/filters/unsupervised/attribute/InterquartileRange.html)
filter to remove outliers and extreme values. The clean datasets get placed in the 
output directory. The original input file is moved to the output directory as well,
but with the extension `.original` instead of `.arff`.

The command-lines for this example assume this directory structure:

```
/some/where
|
+- data
|  |
|  +- adamsflow2docker   // contains the jar
|  |
|  +- flows
|  |  |
|  |  +- weka_filter_pipeline.flow       // actual flow
|  |  |
|  |  +- weka_filter_pipeline.dockerfile  // additional Dockerfile instructions
|  |
|  +- in    // input directory
|  |
|  +- out   // output directory
|
+- output
|  |
|  +- adamsflow  // will contain all the generated data, including "Dockerfile"
```

For our `Dockerfile`, we use the `openjdk:11-jdk-slim-buster` base image (`-b`), which
contains an OpenJDK 11 installation on top of a [Debian "buster"](https://www.debian.org/releases/buster/)
image. The `weka_filter_pipeline.flow` workflow (`-i`) then gets turned into a
Docker image using the following command-line:

```commandline
java -jar /some/where/data/adamsflow2docker/adamsflow2docker-0.0.2-spring-boot.jar \
  -i /some/where/data/flows/weka_filter_pipeline.flow \ 
  -o /some/where/output/adamsflow \
  -b openjdk:11-jdk-slim-buster \
  -I /some/where/data/flows/weka_filter_pipeline.dockerfile  
```

Now we build the docker image called `adamsflow` from the `Dockerfile`
that has been generated in the output directory `/some/where/output/adamsflow` 
(`-o` option in previous command-line):

```
cd /some/where/output/adamsflow
sudo docker build -t adamsflow .
```

With the image built, we can now push the raw ARFF files through for cleaning.
For this to work, we map the in/out directories from our directory structure
into the Docker container (using the `-v` option) and we supply these input
and output directories via the `INPUT` and `OUTPUT` environment variables (using 
the `-e` option) for the flow to pick them up. In order to see a few more 
messages, we also turn on the debugging output that is part of the workflow, 
using the `VERBOSE` environment variable:

```
sudo docker run -ti \
  -v /some/where/data/in:/data/in \
  -v /some/where/data/out:/data/out \
  -e INPUT=/data/in/ \
  -e OUTPUT=/data/out/ \
  -e VERBOSE=true \
  adamsflow
```


## Releases

* [0.0.2](https://github.com/waikato-datamining/adamsflow2docker/releases/download/adamsflow2docker-0.0.2/adamsflow2docker-0.0.2-spring-boot.jar)
* [0.0.2](https://github.com/waikato-datamining/adamsflow2docker/releases/download/adamsflow2docker-0.0.2/adamsflow2docker-0.0.2-spring-boot.jar)


## Maven

```xml
    <dependency>
      <groupId>nz.ac.waikato.cms.adams</groupId>
      <artifactId>adamsflow2docker</artifactId>
      <version>0.0.2</version>
    </dependency>
```
