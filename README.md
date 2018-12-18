[![Clojars Project](https://img.shields.io/clojars/v/metabase/jar-compression.svg)](https://clojars.org/metabase/jar-compression)

Clojure library for programatically Compressing & Decompressing JAR files

```clj
(compress! source-jar & {:as options})

(decompress! source-jar & {:as options})
```

You can also use this as a Leiningen plugin! See https://github.com/metabase/lein-compress-jar

Why would you want to do this? Maybe you're bundling a bunch of JARs inside your uberjar and then extracting them and adding them to the classpath at runtime. Might as well shrink those JARs as small as we can.

In my experience, this ends up shrinking JAR sizes around ~80%. Not too bad.

### Compressing JAR Files

#### Typical Usage

```clj
(compress! "/Users/cam/metabase/resources/modules/sparksql.metabase-driver.jar"
  :blacklist "/Users/cam/metabase/uberjar-blacklist.txt"
  :pack200 {:classes-to-skip ["org/apache/hadoop/hive/shims/Hadoop23Shims.class"]}
  :compression :gz)
```

#### Options

###### Input

The first arg to `compress!` is the source JAR. It can be either a
String filename, `java.nio.files.Path`, or an `InputStream`.

###### :blacklist

You can optionally include a list of files to exclude from the
destination JAR by specifiying `:blacklist`. The blacklist can be
either a collection of filename strings, or a single string filename
containing a list of filenames separated by newlines; a typical way to
generate this file would be something like:

```bash
jar -tf target/uberjar/metabase.jar > uberjar-blacklist.txt
```

###### :pack200

Whether to use Pack 200 to pack the JAR. Defaults to `true`. You may
instead pass a map of pack 200 options. Currently, the only supported
option is `classes-to-skip`; like `:blacklist`, this may be either a
collection or the name of a file containing a list of classes to skip.

(Yes, Pack 200 is deprecated, but until we target only the version of
Java where it is removed and above we will be able to continue to use
it, which should be many years in the future. For example, if we
compile against Java 8, there's no reason we can't include the Pack
200 classes as part of this library to support usage on Java 13 or
whatever version removes those classes; only when we start compiling
against Java 13 will it be time to retire things entirely.)

###### :strip-directories

Whether to strip directories from the resulting JAR. Defaults to
`true`. You should only set this to `false` if you need to get a URI
to one of the directories in the JAR, e.g. via `class.getResource()` or the like;
this isn't true of any Metabase drivers, which is why it is `false` by
default.

In contrast, note that the Metabase uberjar itself needs to keep
directory entries because we iterate over entries under the `modules`
directory (via `io/resource`).

###### :strip-source

Whether to strip Clojure and Java source files from the compressed JAR. Default: `true`

###### :compression

Compression method to use. Defaults to `:xz`. Allowed options are
`:xz`, `:gz` (GZIP), or `:none`. Note that JARs are already
compressed, so using `:compression` without pack 200 is unlikely to
reduce the JAR size by more that a few percentage points.

###### :out

Name of the `String` destination file, or `Path`, or `OutputStream`,
to write the packed and compressed bytes to. Defaults to something
like `<input-filename>.pack.xz`, with extensions based on the values
of the `:pack200` and `:compression` options. Note that if the input
isn't a String filename or Path, we can't come up with a default
output filename, and you'll have to specify `:out`.


### Decompressing JAR Files

#### Typical Usage

```clj
(decompress! "/Users/cam/metabase/resources/modules/sparksql.metabase-driver.jar.pack.xz")
```

#### Options

###### Input

Name of the (String) filename to decompress, or a `Path` or `InputStream`.

###### :pack200

Whether this should be pack200-unpacked. Defaults to `true` if the
input filename contains `.pack` as the last or second-to-last
extension. Note that if input is not a filename (i.e., if input is an
`InputStream`), we cannot determine the default value of this, and so you
must specify this option.

###### :compression

Compression algorithm to use to decompress input. By default, inferred
from the input filename; if input is an `InputStream`, you must
specify this option manually.

It is probably possibly to look at the first few bytes and infer the
algorithm that way instead; PRs welcome!

###### :out

String or `Path` Filename to write decompressed/unpacked results
bytes, or an `OutputStream`. Defaults to the input filename without
`.pack` and without `.xz`/`.gz`; if input is not a filename, you must
specify this option manually.


### Extensibility

You can add support for additional compression algorithms by adding
implementations for `metabase.jar-compression.algorithms` methods
`compressed-input-stream` and `compressed-output-stream`.

You can add support for more input and output types besides `String`
and `Path` filenames and `InputStream`/`OutputStream` by adding
implementations for `metabase.jar-compression.common` methods
`->input-stream` and `->output-stream`.


### License

Copyright © 2018 Metabase, Inc.

Distributed under the Eclipse Public License, same as Clojure.
