easy-split-multi-deposit
========================
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-split-multi-deposit.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-split-multi-deposit)
[![Codacy](https://api.codacy.com/project/badge/Grade/bace6a1d52234ce2a70846d15610ca75)](https://www.codacy.com/app/richard.v.heest/easy-split-multi-deposit?utm_source=github.com&utm_medium=referral&utm_content=rvanheest/easy-split-multi-deposit&utm_campaign=badger)

Splits a Multi-Deposit into several deposit directories for subsequent ingest into the archive
Utility to process a Multi-Deposit prior to ingestion into the DANS EASY Archive

SYNOPSIS
--------

    easy-split-multi-deposit [{--staging-dir|-s} <dir>] [{--identifier-info | -i} <file>] [{--validate|-v}] <multi-deposit-dir> <output-deposits-dir> <datamanager>


DESCRIPTION
-----------
A command line tool to process a Multi-Deposit into several Dataset Deposit. A Multi-Deposit
is a deposit containing data files and metadata for several datasets. The tool splits the
Multi-Deposit into separate Dataset Deposit directories that can be ingested into the archive by 
[easy-ingest-flow].

The Multi-Deposit may also contain audio/visual data files that are to be sent to a Springfield TV
installation to be converted to a streamable surrogate.

`easy-split-multi-deposit` is controlled by the Multi-Deposit Instructions (MDI) file. This is a CSV file,
that must be located at `<multi-deposit-dir>/instructions.csv`. It should contain the metadata for the
datasets that are to be created and may also contain instructions about how to process individual files.
See the [Multi-Deposit Instructions] page for details.

If the MDI file is found and is correct the following actions are taken:

1. The MDI file is read and checked. If the contents is invalid the tool reports the errors and exits.
2. The necessary preconditions for the instructions to be carried out are checked. 
   If the preconditions are not met, the tool reports the problems and exits.
3. One or more deposit directories are created, one for each dataset
4. Depending on whether the MDI file contains any processing instructions for the audio/visual files 
   in the Multi-Deposit they may be sent to a Springfield Inbox directory for
   subsequent processing by the Springfield Streaming Media Platform, along with
   a Springfield Actions XML and---optionally---subtitles files. 
  
  
ARGUMENTS
---------
```
  Usage: 

      easy-split-multi-deposit [{--staging-dir|-s} <dir>] [{--validate|-v}] <multi-deposit-dir> <output-deposits-dir> <datamanager>

  Options:

    -i, --identifier-info  <arg>   CSV file in which information about the created deposits is reported
    -s, --staging-dir  <arg>       A directory in which the deposit directories are created, after which they
                                   will be moved to the 'output-deposit-dir'. If not specified, the value of
                                   'staging-dir' in 'application.properties' is used.
    -v, --validate                 Validates the input of a Multi-Deposit ingest
    -h, --help                     Show help message
        --version                  Show version of this program
  
   trailing arguments:
    multi-deposit-dir (required)    Directory containing the Submission Information Package to process. This
                                    must be a valid path to a directory containing a file named
                                    'instructions.csv' in RFC4180 format.
    output-deposit-dir (required)   A directory to which the deposit directories are moved after the staging has
                                    been completed successfully. The deposit directory layout is described in
                                    the easy-sword2 documentation
    datamanager (required)          The username (id) of the datamanger (archivist) performing this deposit
```


INSTALLATION AND CONFIGURATION
------------------------------
Currently this project is built as an RPM package for RHEL7/CentOS7 and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/easy-split-multi-deposit` and the configuration files to `/etc/opt/dans.knaw.nl/easy-split-multi-deposit`. 

To install the module on systems that do not support RPM, you can copy and unarchive the tarball to the target host.
You will have to take care of placing the files in the correct locations for your system yourself. For instructions
on building the tarball, see next section.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
* RPM
 
Steps:

        git clone https://github.com/DANS-KNAW/easy-split-multi-deposit
        cd easy-split-multi-deposit
        mvn install

If the `rpm` executable is found at `/usr/local/bin/rpm`, the build profile that includes the RPM 
packaging will be activated. If `rpm` is available, but at a different path, then activate it by using
Maven's `-P` switch: `mvn -Pprm install`.

Alternatively, to build the tarball execute:

    mvn clean install assembly:single
    
[Multi-Deposit Instructions]: multi-deposit-instructions.md
[easy-ingest-flow]: https://github.com/DANS-KNAW/easy-ingest-flow
