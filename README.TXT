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
      
