# Osmparser

## Description
This repo holds a system composed of a self-developed declarative
DSL that allow users, in a simple way, to describe requests to the
Overpass API, which will recover *features* (name of each geolocated
element) fulfilling several filters and store the obtained data in any 
relational database that you specify.

## Requirements
- Antlr4.
- Java 11 (tested with AdoptOpenJDK 11).
- Maven 3.8+.
- PostgreSQL.

## Installation and execution
Make sure you are on the ```main``` branch of the project. If you are not,
use this command: ```git checkout main```

```
mvn clean compile install (only first time to compile grammar)
mvn exec:java -Dexec.args='path_to_file'
```
- If you want to skip tests while running `mvn clean compile install`, execute: 
`mvn clean compile install -DskipTests=true`.
- The way to specify the file in the second command depends on the operating system
  that you are using.

## Running tests
```
mvn test
```

## Working example
If you want to see a working example of the DSL and how to write correct queries,
please take a look at this [file](src/main/resources/bdExample.grammar.OSMGrammar).
