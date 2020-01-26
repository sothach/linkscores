# LinkScore

![Build Status](https://travis-ci.org/sothach/linkscores.svg?branch=master)
![Coverage Status](https://coveralls.io/repos/github/sothach/linkscores/badge.svg?branch=master)
![Codacy Badge](https://api.codacy.com/project/badge/Grade/b4025f317a524af9966e4ef063580ad8)

## Command line application to record and report on social URL metrics
### Requirements
1. __ADD__: Store a record of a URL along with it's societal score
    - assume multiple instances of the same URL should be individually stored, rather than aggregating them into
    one record, as the information about insertion dates and rates may be deemed valuable in the future
1. __REMOVE__: Allow for deletion of a stored url
    - implication is that all urls matching that supplied will be deleted
1. __EXPORT__: Produce a report detailing statistics for each unique domain store (the *host* portion of the URL)
    - statistics consist of _domain_, _count_ and _total score_ 

## Assumptions/decisions

*  Use MongoDB as doc store, just the plain scala driver as the use case is simple
*  Link scores must be one or greater
*  Scores and entry counts will be within the domain of an `Int` type in Scala, 0 to 2,147,483,647 (no negative values)

## Application Design
The application consists of two main components:
### User Interface
A command-line dialog providing a command prompt and help menu, that accepts commands (see above), attempts to execute
them, and reporting success or errors.

It is implemented as a simple state-machine (`Machine`), driven by a command mapping (`CommandSet`), recursively 
executing user commands until terminated (QUIT).  Actions interact directly with the persistence component (below)
to store and query the stored links.  This is designed to be easily extendible, should a new command need to be added,
e.g., List all entries

## Domain Model
Taking a DDD approach to this assignment, the first steps was to sketch the domain based on the terminology used
as the events occurring in the scenarios described.  The solution arrived at consists of three prime entities:
1. __URL__
    - the link to be stored, any valid URL is accepted (based on `java.net.Url`)
1. __Score__
    - the social score value for the link, a positive integer from 1 to 2,147,483,647 
1. __Entry__
    - the url and its associated score
1. __Report__
    - a record detaling the domain, number of links and total of all social scores

As well as helping to tease-out the requirements and formulate the solution, this model defines the system boundary, 
guarding against invalid inputs, either accidental or malicious

## Persistence
As the current requirements consist of insert, delete and a group-by query, the service interface is implemented 
directly in the persistence class.  This implementation does, however, take pains to avoid leaking implementation 
details out to its clients

As and when requirements grow, it may make sense to refactored this into it's 'service' and 'storage' responsibilities.  
As such future requirements are not currently known, it would not make sense to 'gold plate' the solution, but instead 
wait until there is a business case to do so.  Likewise, the service is implemented directly without trying to appease
the cargo-cult gods by defining a service trait (interface) at this point, although that might make sense if a future 
requirement introduces the need for multiple storage mechanisms

MongoDB is used as the storage engine for this implementation, and given the requirements and the desire to make
the app easy to test and deploy, the embedded variant from 'de.flapdoodle' has been used.  This has the limitation
that URLs are not persisted between runs of the app, but given that this is a POC that is deemed acceptable.  
It also possible to run against an existing MongoDB instance, on a server or in a local docker container, by 
specifying the appropriate `mongodb.database.url` as an environment variable (see below)

Internally to the store service, the `Entry` domain object is transformed into a `MongoEntry`, extracting the host
part of the URL to use in aggregation queries, as well as adding a creation timestamp, should later data analysis
require it (not something that can be added later, so no accusations of gold plating on this decision, please).

The internal BSON document format is as below:
```json
{
  "id": ObjectId("5e2c6228b70e7966487ec147"),
  "url": "URL(http://www.rte.ie/news)",
  "domain": "www.rte.ie",
  "score": 10,
  "timestamp": ISODate("2020-01-25T15:43:36.891Z")
}
```

The store service runs in it's own, tunable, execution context, initially a fork-join pool with configurable parallelism

## Building & testing
### Prerequisites
*  Scala JDK 2.12.x
*  SBT build tool, 1.x
Optionally:
*  docker
*  mongodb, mongodb-clients

### Checkout & build
    % git clone https://github.com/sothach/linkscores.git
    % sbt compile

### Testing
As the application is self-contained, the unit tests include full integration test coverage

    % sbt clean coverage test coverageReport
    
### Running    
From the checked-out project home directory, run using an in-memory database:

    % sbt run
    
Or run against an external database, e.g., running locally as a docker container 
    
    % sbt run -Dmongodb.database.url=mongodb://0.0.0.0:27017
    
#### Output
```bash
enter command: HELP
QUIT:	quit program
HELP:	display this help
ADD:	add a URL with an associated social score
REMOVE:	remove a URL from the system
EXPORT:	export statistics about the URLs stored in the system
enter command: ADD https://www.rte.ie/news/politics/2018/1004/1001034-cso/ 20
	saved URL(https://www.rte.ie/news/politics/2018/1004/1001034-cso/) score: 20
enter command: ADD https://www.rte.ie/news/ulster/2018/1004/1000952-moanghan-mine/ 30
	saved URL(https://www.rte.ie/news/ulster/2018/1004/1000952-moanghan-mine/) score: 30
enter command: ADD http://www.bbc.com/news/world-europe-45746837 10
	saved URL(http://www.bbc.com/news/world-europe-45746837) score: 10
enter command: EXPORT
	domain;urls;social_score
	www.rte.ie;5;50
	www.bbc.com;1;10
enter command: QUIT
```    
    
### Running in a container
Build a docker image of the application:

    % sbt docker
    % docker run --rm --interactive org.anized/linkscore:latest
    
### Run against a docker mongodb
    % mkdir ~/data
    % docker run -d -p 27017:27017 -v ~/data:/data/db mongo
    % docker run --rm --interactive -e -Dmongodb.database.url=mongodb://0.0.0.0:27017 org.anized/linkscore:latest

## Further evolution
*  i18n for dialogue
*  split service / persistence concerns
*  bulk loading of urls
*  rest api for store operations
*  scalability - big data sets
*  metrics
