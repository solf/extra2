
If you are here in order to evaluate my code, you can take a look at
https://github.com/solf/extra2/blob/main/src/main/java/io/github/solf/extra2/retry/RetryAndRateLimitService.java
for a specific example of a rather complex asynchronous code that is currently 
used in production.

See https://github.com/solf/extra2/blob/main/src/main/example/io/github/solf/extra2/retry/example/ExampleAsyncHttpSubmitterService.java
for an example of a RetryAndRateLimitService implementation;
see https://github.com/solf/extra2/blob/main/src/main/example/io/github/solf/extra2/retry/example/ExampleAsyncHttpSubmitterServiceUsage.java
for an example of using the service.

There's additional information about this service in the io.github.solf.extra2.retry 
section below.

-------------------
-------------------
-------------------

Collection of various Java support code to make Java programming more 'fun'.

Project is intended for use with Maven:
		<dependency>
			<groupId>io.github.solf</groupId>
			<artifactId>extra2</artifactId>
			<version>${VERSION}</version>
		</dependency>

(replace version reference with any available versions from here:
https://repo.maven.apache.org/maven2/io/github/solf/extra2/ )

NOTE: there's an 'extended' version of this project that contains some
additional stuff that is EPL-licensed, see here:
https://github.com/solf/extra2-epl
(extra2-epl also automatically includes nullanno dependency)

Because all the code was originally in the single project, some of the
descriptions below may refer to code that was actually moved to extra2-epl

-------------------

This project is developed using Eclipse's null-analysis using included 
annotations in javax.annotation package and external annotations as available
here: https://github.com/solf/ext-eclipse-annotations

Uses null-annotations available here: https://github.com/solf/nullanno
(but those are an 'optional' dependency and thus not included in dependencies
by default)

-------------------

Here's a brief overview of what's inside on a per-package basis


- io.github.solf.extra2.beans
FluentIntrospector that supports examining Java beans with fluent setters
(those that return 'this' as opposed to void).


- io.github.solf.extra2.cache.wbrb
Highly flexible yet easy to use write behind (write back) cache that also supports 
background synchronization with the storage.

See WriteBehindResyncInBackgroundCache section below for more details.


- io.github.solf.extra2.codegenerate.stepbuilder
'step builder' generator -- generates builder classes for complex constructors
that require step-by-step specification of each constructor argument (i.e.
it is not possible to 'miss' an argument, as it won't compile).

See _Extra_GenerateStepBuilders and WBRBStatusBuilder for examples

To use this generator it is necessary to specify addtional dependency (possibly 
in a custom Maven profile in order to do not pollute the main build):
		<dependency>
		    <groupId>com.github.javaparser</groupId>
		    <artifactId>javaparser-symbol-solver-core</artifactId>
		    <version>3.15.14</version>
			<exclusions>
				<exclusion>
					<groupId>com.google.code.findbugs</groupId>
					<artifactId>jsr305</artifactId>
				</exclusion>
			</exclusions>
		</dependency>


- io.github.solf.extra2.collection
A major rework/improvement on Java (Hash)Map & (Hash)Set that provides such
things as read-only views of these collection plus fixes/clarifies a lot of 
type-checking/semantics issues plus adds functionality such as being able 
to retrieve elements from sets, keys and entries from map, and being able
to replace elements & keys.

The functionality is separated in parts that can be easily 'wrapped onto'
pre-existing set and map instances (BMap/ReadOnlyMap; BSet/ReadOnlySet) and
extended functionality that cannot be done in a simple wrapper 
(EMap/EReadOnlyMap/RMap; ESet/RSet).

Also older misc collections extensions, in particular ability to obtain value 
from set and entry from map via reflection (HashMapExt, HashSetExt, WACollections).

ItemSampler provides a way to 'sample' (aggregate fixed-size list) items over 
an unknown-length source list -- it tries to space sample items more or less
equidistantly.

WACollections provides convenient toIterable(..) methods for various often-used
sources, such as ResultSet (so it is possible to for (an item : source) iterate)
and some other functions.


- io.github.solf.extra2.concurrent
Helpful concurrency-related stuff.

WAFuture & WAExecutor (and related classes) provide support for Futures that
have reference to the original task (unlike standard Future where you have to
use some kind of external mapping if you need to attribute results to the tasks).

WAThreadPoolExecutor is a thread pool with min & max thread amounts -- it will
dynamically increase & decrease active threads count as required (within bounds);
it also has incoming queue and will not reject tasks even if all threads are
currently busy.

WAExecutors thread pools factory that requires (and makes it easy) to specify
whether threads are daemon and a common naming prefix for the pool threads.

Latch is a simple control latch -- when it is closed, threads can wait until it
is open via various 'await' methods; when it is open all waiting threads are
released & new threads will proceed immediately as well (await returns 
immediately); the latch may be re-closed & re-opened as desired.

RunnableWithException and Interruptable* interfaces provide useful signatures 
for writing concurrency-related code.


- io.github.solf.extra2.config
Utilities for loading configuration values from plain property files & 
sectioned configuration files (ini files).

Also provides support for 'merging' multiple configuration sources together &
adding custom value overrides on top of existing configurations.

Often used together with 'options' package below.


- io.github.solf.extra2.options
Support for the 'easy' way to read option values from configuration files.

See ExampleDbOptions for a trivial example or WBRBConfig for a much more
advanced one (WBRBConfig also 'hides' underlying 'get value' methods from the
public API).


- io.github.solf.extra2.console
Support for easily executing external commands on Windows & Unix and interacting
with them -- such as reading their output and sending input to them as required.


- io.github.solf.extra2.email
Support for accessing IMAP E-mails.


- io.github.solf.extra2.exception
Some exceptions that seem very useful, e.g. IORuntimeException and
AssertionException (for the cases when Error is undesirable).


- io.github.solf.extra2.file
Utilities to simplify reading & editing of text files.


- io.github.solf.extra2.io
Input/Output streams that support compression as well as ByteArrayInputStream
that can read directly from ByteArrayOutputStream.


- io.github.solf.extra2.jdbc
Simplified way to work with jdbc/SQL.


- io.github.solf.extra2.kryo
A simple way for a Java application to persist state between sessions via Kryo
(de)serialization. This is useful for many apps and doesn't require any database
support.

Implementation support data model changes in so far as underlying Kryo supports
those -- the implementation used here only supports fields addition; in-house
a modified version of Kryo (which is not publicly available) was used that did
support field removal.

The code should be updated to a newer version of Kryo that does support field
removal.

There's support for KryoUpgradeListener and KryoPostUpgradeListener that allows
data model to make internal changes after the old version data is loaded (this
uses objectgraph discussed later).


- io.github.solf.extra2.lambda
Utility stuff that comes in handy when actively working with lambdas, e.g.
various object wrappers (so that the lambda-code can modify something outside its
own body), TriFunction, LambdaUtil with various predicates useful for e.g. streams
filtering, and various *Lazy implementations that can be used to lazily initialize
something in different circumstances.


- io.github.solf.extra2.log
BaseLoggingUtility that it is intended to provide basis for implementing
application-specific logging with support for message throttling & monitoring.
See ExampleLoggingUtility.

Also some SLF4J helpers.


- io.github.solf.extra2.log4j
LOG4J extensions, sound-producing appender, filters that can work on exception
text, etc.


- io.github.solf.extra2.nullable
NullableOptional which is a version of Optional that can hold null value and
exception information; NonNullOptional which is the same except value must
be non-null.


- io.github.solf.extra2.objectgraph
ObjectGraphUtil that is useful for walking object graphs -- can be used e.g. after
deserialization in order to adjust post-serialization state on all objects in the
graph.


- io.github.solf.extra2.options
Described above together with io.github.solf.extra2.config


- io.github.solf.extra2.pool
SimpleObjectPool -- simple objects pool with configurable min and max values with
optional automatic objects expiration if they are not used.


- io.github.solf.extra2.retry
RetryAndRateLimitService provides support for executing [external] requests that
might need retrying (due to transient failures etc.) and may also require
rate limiting (restricting number of requests per time interval to avoid 
overloading the target system).

. All requests are initially put into queue.
. Requests from the queue are processed when a thread (used to process request
  asynchronously) and a token (obtained from rate limiting service in order to
  facilitate throttling) are both available.
. If request processing fails, then a decision is made (based on configuration
  by default, but it can be customized) -- request is either retried (after some
  configurable delay), is considered failed (too many failures) or is considered
  'timed out' (when requests are submitted, validity time must be specified --
  it may be pointless to retry time-sensitive request after a lot of time has
  passed).
. It is also possible for requests to 'time out' without making an actual
  processing attempt -- e.g. in cases when obtaining resources (thread & token) 
  takes too long.
. For every submitted request, a Future is provided that can be used to monitor
  request status (and eventually it will be possible to submit cancellation
  request).
. There's configurable limit on the number of pending requests (i.e. if there
  are so many that it will take too long to process them all, it is pointless
  to accept more).
. There's event listener that can be used to monitor various events and collect
  statistics etc.
. There are also various hooks that can be used to customize behavior.

See ExampleAsyncHttpSubmitterService and ExampleAsyncHttpSubmitterServiceUsage
for basic example on how to use the service.


- io.github.solf.extra2.stacktrace
Support for producing condensed (one-line) stack traces & obtaining information
about current Java file name / line number.


- io.github.solf.extra2.storage
A sort-of file system implementation that supports tree structure of 'items'
where each item can have both 'contents' (i.e. bytes comprising the item) AND
any number of named metadata entries (where each entry value may be a single 
line, a list of lines, or a free-form String).

Was used, for example, in tooling that processed and stored huge amount of data --
and so for individual data pieces an additional metadata was stored to make
some statistical information easy and fast to access.


- io.github.solf.extra2.testutil
AssertExtra contains various useful asserts, such as assertContains*, 
assertBetween*, assertGreater* etc; those throw exceptions rather than Errors
(such as AssertError) for greater compatibility (e.g. JUnit)

TestUtil contains various convenience methods such as executing code 
asynchronously, executing code with time limit, and accessing inaccessible
fields and method via reflection.

MockSocketService provides a way to test code using the sockets (for this to
work the code being tested must create sockets via some kind of factory that
can be intercepted). See TestExampleSocketsUsingService for an example of how
to use TestUtil and MockSocketService.


- io.github.solf.extra2.thread
A couple of useful primitives for handling thread-related issues.

ExitableThread is an abstract thread that can be externally commanded to exit.

InterruptHandlingExitableThread is an extension of ExitableThread that also
provides for a handler to handle unexpected InterruptedException


- io.github.solf.extra2.util
A collection of various utilities, some of which are described below.

ASCIIUtil - utilities for working with ASCII-style strings, in particular
ability to encode/decode strings to the safe format (filename-safe and similar),
that is still human-readable e.g.:
123 Cache Status: {running}
->
a123_Cache_Status___running__e_521_321__85___a

NullUtil - various typecasting methods particularly useful when mixing null-safe 
(null-annotated) code and null-unsafe code.

SplitUtil - merge and split together various strings, useful e.g. when need to
pass multiple 'things' as a single string.

TypeUtil - some typecasting/coercing methods that are sometimes necessary
particularly when null-analysis gets confused in complex cases.

WebUtil - utilities for easily reading web page contents.



WriteBehindResyncInBackgroundCache

NOTE: this code is 'complete' in the sense that it appears to work and is being
used; however there are still many things 'to do', not the least of which is
adding missing Javadocs & improving tests coverage.

This is a very flexible implementation of write-behind (aka write-back) cache 
(i.e. cache that writes updates to the underlying storage with some delay in 
order to be able to aggregate multiple updates) that also performs background 
resync (cache re-reads underlying storage in background and updates in-memory
cache state with any updates that might've been made by 3rd parties). The 
background resync does not interfere with cache access and updates by clients.

For this functionality to have practical value the following two conditions
are important:

1. Writes to the underlying storage should not (in most cases) overwrite the
entire state (ideally they should only overwrite the parts that have been
changed); if the entire state is overwritten, then the background resync will 
not likely to be useful as it will just re-read the same data as was written;
HOWEVER there's a partial workaround for this via WBRBReadBeforeWriteCache
which will be discussed later.

2. There needs to be a way to reconcile two distinct cache object states -- 
e.g. between in-cache-memory state and the state that was just read from the
underlying storage during the resync. This can be done in whatever manner is
more suitable for a particular case, however the default assumption is that the
updates to cache items are encapsulated into individual 'update events' and those
'update events' can be applied to any existing cache item state (such as to
the current in-memory cache item state and later on to the cache item state
read from the underlying storage during the resync in order to 'upgrade' it to
the most up-to-date state). These 'update events' are collected and used 
internally by WriteBehindResyncInBackgroundCache as needed; most implementations
do not need to care about how those are handled.

NOTE: all operations in the cache are performed using read/write locks -- read
operations may execute concurrently whereas write(update) operation is exclusive
(no other operations may take place while write(update) is executing), therefore
actual cache implementation code does not need to worry about concurrency 
issues.

See WBRBCache.png in this directory for an overview of the cache operation.

The internal cache functionality is quite complex, however using the cache is
not -- see ExampleSignalCache for an example of cache implementation (to write
a new cache implementation you simply extend WriteBehindResyncInBackgroundCache,
provide required type parameters, and implement required abstract methods);
see ExampleSignalCacheUsage for a basic example of how to use the cache.

The cache also comes with built-in logging (including automatic throttling) & 
monitoring support that is pretty simple to use -- use getStatus(..) method 
for monitoring and override  spiUnknownLockGetLogger(..) method to control 
logging.


WBRBReadBeforeWriteCache

As mentiond before, WriteBehindResyncInBackgroundCache is quite flexible and
WBRBReadBeforeWriteCache is a great example of taking advantage of this 
flexibility.

WBRBReadBeforeWriteCache modifies cache behavior so that all 'cache update'
events are stored in-memory until the actual underlying storage write -- and 
the 'write' operation instead does the following:
- reads current underlying storage data
- applies all the collected 'cache update' events to it
- writes resulting value to the underlying storage and updates cached state
with this new data (resync)

This means that if read-update-write operation is quick compared to how often
items are written to the storage (i.e. caching duration), then there's a high
likelyhood that multiple concurrently running caches will not overwrite each
others' data even if they are updating the 'same' thing.

The drawback is that all the 'cache update' events have to be stored in memory
until read-update-write operation occurs (i.e. for the 'caching duration' time).



ATTENTION: this project uses Lombok: https://projectlombok.org/
To use it in IDE you need to install Lombok for your IDE (developed using
version 1.18.10).

Contains various useful utilities that are intended to be used by other projects.

Look into specific package-info.java files for more information.


Fixing TestNG DTD errors in Eclipse
'Referenced file contains errors (http://testng.org/testng-1.0.dtd)...'

a) Change path to https
b) If after change there's a problem with PKIX certification path -- need to
update JRE to latest (which may include required certs) or install certs manually.

- Open https://testng.org/
- Check certification info, e.g. for 'Sectigo RSA Domain Validation Secure Server CA'
- Check serial number, e.g.: 7D:5B:51:26:B4:76:BA:11:DB:74:16:0B:BC:53:0D:A7
- Go to Sectigo certs page: https://secure.comodo.com/products/publiclyDisclosedSubCACerts
- Download required cert e.g. 7D5B5126B476BA11DB74160BBC530DA7
- keytool -import -trustcacerts -alias sectigo-domain-validation-cert -keystore cacerts -file SectigoRSADomainValidationSecureServerCA.crt
    default password is: changeit
      
