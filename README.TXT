This is a WORK-IN-PROGRESS to make a proper open-source project out of the
collection of the various support code that I've written over the years.

Here's a brief overview of what's inside on a per-package basis


- com.github.solf.extra2.beans
FluentIntrospector that supports examining Java beans with fluent setters
(those that return 'this' as opposed to void).


- com.github.solf.extra2.cache.wbrb
Highly flexible yet easy to use write behind (write back) cache that also supports 
background synchronization with the storage.

See WriteBehindResyncInBackgroundCache section below for more details.


- com.github.solf.extra2.codegenerate.stepbuilder
'step builder' generator -- generates builder classes for complex constructors
that require step-by-step specification of each constructor argument (i.e.
it is not possible to 'miss' an argument, as it won't compile).

See _Extra_GenerateStepBuilders and WBRBStatusBuilder for examples


- com.github.solf.extra2.collection
Misc collections extensions, in particular ability to obtain value from set and 
entry from map.

ItemSampler provides a way to 'sample' (aggregate fixed-size list) items over 
an unknown-length source list -- it tries to space sample items more or less
equidistantly.

WACollections provides convenient toIterable(..) methods for various often-used
sources, such as ResultSet (so it is possible to for (an item : source) iterate)
and some other functions.


- com.github.solf.extra2.concurrent
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
immediately); the latch may re-closed & re-opened as desired.

RunnableWithException and Interruptable* interfaces provide useful signatures 
for writing concurrency-related code.


- com.github.solf.extra2.config
Utilities for loading configuration values from plain property files & 
sectioned configuration files (ini files).

Also provides support for 'merging' multiple configuration sources together &
adding custom value overrides on top of existing configurations.

Often used together with 'options' package below.


- com.github.solf.extra2.options
Support for the 'easy' way to read option values from configuration files.

See ExampleDbOptions for a trivial example or WBRBConfig for a much more
advanced one (WBRBConfig also 'hides' underlying 'get value' methods from the
public API).


- com.github.solf.extra2.console
Support for easily executing external commands on Windows & Unix and interacting
with them -- such as reading their output and sending input to them as required.


- com.github.solf.extra2.email
Support for accessing IMAP E-mails.


- com.github.solf.extra2.exception
Some exceptions that seem very useful, e.g. IORuntimeException and
AssertionException (for the cases when Error is undesirable).


- com.github.solf.extra2.file
Utilities to simplify reading & editing of text files.


- com.github.solf.extra2.io
Input/Output streams that support compression as well as ByteArrayInputStream
that can read directly from ByteArrayOutputStream.


- com.github.solf.extra2.jdbc
Simplified way to work with jdbc/SQL.


- com.github.solf.extra2.kryo
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


- com.github.solf.extra2.lambda
Utility stuff that comes in handy when actively working with lambdas, e.g.
various object wrappers (so that the lambda-code can modify something outside its
own body), TriFunction, LambdaUtil with various predicates useful for e.g. streams
filtering, and various *Lazy implementations that can be used to lazily initialize
something in different circumstances.


- com.github.solf.extra2.log
Some SLF4J helpers.


- com.github.solf.extra2.log4j
LOG4J extensions, sound-producing appender, filters that can work on exception
text, etc.


- com.github.solf.extra2.nullable
NullableOptional which is a version of Optional that can hold null value and
exception information; NonNullOptional which is the same except value must
be non-null.


- com.github.solf.extra2.objectgraph
ObjectGraphUtil that is useful for walking object graphs -- can be used e.g. after
deserialization in order to adjust post-serialization state on all objects in the
graph.


- com.github.solf.extra2.options
Described above together with com.github.solf.extra2.config


- com.github.solf.extra2.pool
SimpleObjectPool -- simple objects pool with configurable min and max values with
optional automatic objects expiration if they are not used.


- com.github.solf.extra2.stacktrace
Support for producing condensed (one-line) stack traces & obtaining information
about current Java file name / line number.


- com.github.solf.extra2.storage
A sort-of file system implementation that supports tree structure of 'items'
where each item can have both 'contents' (i.e. bytes comprising the item) AND
any number of named metadata entries (where each entry value may be a single 
line, a list of lines, or a free-form String).

Was used, for example, in tooling that processed and stored huge amount of data --
and so for individual data pieces an additional metadata was stored to make
some statistical information easy and fast to access.


- com.github.solf.extra2.testutil
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


- com.github.solf.extra2.thread
A couple of useful primitives for handling thread-related issues.

ExitableThread is an abstract thread that can be externally commanded to exit.

InterruptHandlingExitableThread is an extension of ExitableThread that also
provides for a handler to handle unexpected InterruptedException


- com.github.solf.extra2.util
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

TODO


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
      
