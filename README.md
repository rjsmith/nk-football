# README

This project is a demo application for the [NetKernel](http://1060research.com/products/) Resource Orientated Computing platform.

It reads a csv file of English premier league football results from the 2014-2015 season, then provides resources for generating league tables and other derived data from that dataset.

## Installation

### Prerequisites
You must have a NetKernel system installed at some path in your local system **NetKernel RootPath**, and running.

Have the [gradle](https://gradle.org/install/) build tool installed.

### Steps

Follow these steps to get the two NetKernel modules installed:

1. Edit the gradle.build files in the two modules in this repository to point to the [NetKernel Root] path, eg:

    ```groovy
        TEST
            {
                edition = "EE"
                location = "/Users/richardsmith/Projects/NetKernel/NKEE6.1.1/"
            }
    ```

    NB: Change the "EE" to 'SE" if you have the Standard Edition NetKernel.

2. From the root of your working copy of this git repository:

    ````bash
    cd modules
    cd urn.uk.co.rsbatechnology.football.matches/
    gradle build
    gradle deployModuleTEST

    cd ..
    cd urn.test.uk.co.rsbatechnology.football.matches/
    gradle build
    gradle deployModuleTEST
    ````

3. Open a browser and navigate to the [Module Explorer](http://localhost:1060/tools/ae/view/allModules).  Scroll down and check that you you can see the `Football - Matches` and `Test - Football - Matches` modules.

## Running xUnit Tests

1. Open a browser and navigate to the [Football Module Xunit page](http://localhost:1060/test/view/html/test:urn:uk:co:rsbatechnology:football:matches).
2. Click the **execute** button.

## Dev Diary

Whilst I worked on this application, I kept a [Dev Diary](https://github.com/rjsmith/nk-football/blob/master/modules/urn.uk.co.rsbatechnology.football.matches/src/resources/doc/dev_diary.md) to record the steps I went through , gotchas and lesson learned.

If you have installed the modules into your local NetKernel instance, you can also read it as an [embedded module documentation book](http://localhost:1060/book/view/book:urn:uk:co:rsbatechnology:football:matches/).

## Football Match Results

https://github.com/jokecamp/FootballData/tree/master/football-data.co.uk

https://github.com/jokecamp/FootballData/tree/master/football-data.co.uk/england

https://github.com/jokecamp/FootballData/blob/master/football-data.co.uk/england/2014-2015/Premier.csv
