Configtools is a set of tools to handle configuration parameters from various sources - environment variables, .properties files, etc - in various contexts - standalone, web, Spring, etc - in a uniform way.

## Building

The library is build using maven through github actions and is published in maven central.
You probably never need to build it yourself. But if you want to, you can find the exact maven commands that were used to build the artifacts in the files in .github/workflows/

## Using

Add one (or all) of the following maven dependencies to your project.

```
<dependency>
	<groupId>be.vito.rma.configtools</groupId>
	<artifactId>configtools-common</artifactId>
	<version>3.0.0</version>
</dependency>
```
```
<dependency>
	<groupId>be.vito.rma.configtools</groupId>
	<artifactId>configtools-spring</artifactId>
	<version>3.0.0</version>
</dependency>
```

## Version history

see Changelog.txt