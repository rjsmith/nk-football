#MARKDOWN
Development Diary
=================

## 1. Setup

 1. Created module from Standard Module
 2. Imported into Eclipse (New Java project -> use folder as source)
 3. Created modules.d file and created symbolic link in NK6.1EE `etc/modules.d` folder

## 2. CSV Space


 1. Copied Premier.csv into `/resources/matches/2014-2015.csv`
 2. Created a `<rootspace>` with public="false" (want to hide csv file from rest of system)
 3. Created a fileset endpoint `<regex>res:/resources/matches/.*csv</regex>`
 4. Used the Request Trace tool to check that the resource res:/resources/matches/Premier.csv can resolve

## 3. Results Endpoint

The idea is to provide an accessor endpoint that can return the set of all match results in a given football season.

 1. Created an `<accessor>` endpoint active:MatchResults with a MatchResultsAccessor class.
 2. Created the MatchResultsAccessor.java file, overriding the onSource() method
 3. Body of onSource method gets the 'season' (string) argument from the context request, then sources the corresponding csv file representation as a IReadableBinaryStreamRepresentation object.
 4. Set up a dummy response to return the size of the returned stream object.
 5. Used the Request Trace tool to check the request resolution and the endpoint returns a representation (e.g. "active:MatchResults+season@2014-2015")

## 4. HomeAwayResults Endpoint

Goal: Create a service endpoint that derives some information from the underlying set of match results.

In module.xml, created another service accessor, active:HomeResults, class: uk.co.rsbatechnology.football.HomeResultsAccessor.java

Created the HomeResultsAccessor class, constructor declaring endpoint is threadsafe (probably could be added later in the development)

Sketched out onSource method, with "WHAT", "WITH", "HOW" and "REPLY" commented sections.
 
In 'WITH', source the matches:
 
        IHDSDocument matches = context.source("arg:matches", IHDSDocument.class);
        
Test using RequestTool:
 
        <request>
	        <identifier>active:HomeResults</identifier>
            <argument name="matches">active:MatchResults+season@2014-2015+csvFile@res:/resources/matches/2014-2015.csv</argument>
        </request>
        
Created a protected method `parseHomeTeamResults` to calculate home team results from set of matches.  Created a HomeResultsAccessorTests jUnit class to test the `parseHomeTeamResults` method.
 
Once the test was passing, the "// DO" section of the endpoint body was easy: call the `parseHomeTeamResults` method, then build a simple HDS structure to hold the results:
 
	  teamResults: 
	    teamResult: 
	      team: Stoke
	      played: 19
	      won: 10
	      drawn: 3
	      lost: 6
	      goalsFor: 32
	      goalsAgainst: 22
	      points: 33
	    teamResult: 
	      team: Liverpool
	      played: 19
	      won: 10
	      drawn: 5
	      lost: 4
	      goalsFor: 30
	      goalsAgainst: 20
	      points: 35
	      
Refactored HomeResultsValueHolder to a public HomeAwayResultsValueHolder class.
  
Refactored HomeResultsAccessor into HomeAwayResultsAccessor to handle both home and away team performance calculations. Changed request identifier to:

        <active>
				<identifier>active:HomeAwayResults</identifier>
				<argument name="matches" desc="Set of matches e.g. as returned from active:MatchResults"/>
				<argument name="homeOrAway" desc="Compute results when team is playing at home or away"/>
		</active>
      
Added a Mapper endpoint to provide `active:HomeResults` and `active:AwayResults` service accessors, which call through to `active:HomeAwayResults` endpoint, which is now contained inside the Mapper's private space:
  
        <endpoint>
            <name>Home Team Results</name>
            <description>Aggregates results for each team when playing at home</description>
            <grammar>
                <active>
                    <identifier>active:HomeResults</identifier>
                    <argument name="matches" desc="Set of matches e.g. as returned from active:MatchResults"/>
                </active>
            </grammar>
            <request>
                <identifier>active:HomeAwayResults</identifier>
                <argument name="matches">arg:matches</argument>
                <argument name="homeOrAway">home</argument>
            </request>
        </endpoint>
  
# 5. ResultsTableEndpoint

Started building a ResultsTableAccessor physical endpoint.

I used the `active:HomeResults` and `active:AwayResults` resources as arguments to active:ResultsTable. The generateResultsTable internal method does the work, iterating over the homeResults nodes, then finding the corresponding node in the awayResults HDS doc using `active:fragmentHDS` endpoint in the mod:hds  across the response from `active:AwayResults` request:
     
    // Get away results for same team, using active:fragmentHDS
    INKFRequest awayTeamResultRequest = context.createRequest("active:fragmentHDS");
    awayTeamResultRequest.addArgumentByRequest("operand", awayResultsRequest);
    awayTeamResultRequest.addArgumentByValue("xpath", "/teamResults/teamResult[team='" + team + "']");
    IHDSReader awayResult = ((IHDSDocument) context.issueRequest(awayTeamResultRequest)).getReader().getFirstNode("/teamResult");
     
Refactored to use the [HDS2 index feature](doc:mod:hds:guide).  First, changed the HomeAwayResultsAccessor to create its HDS with an embedded index node on the team value:
  
    m.pushNode("teamResult");
	m.addNode("@team", team.team); // HDS2 index node
	m.addNode("team", team.team);
    ...
    m.declareKey("byTeam", "/teamResults/teamResult", "@team");
    
Then, replacing the `active:fragmentHDS` subrequests above in the `generateResultsTable` method to use the `key('aKey', 'value')` XPATH expression to find the `teamResult` node in the `awayResults` IHDSDocument object:
 
    IHDSReader awayResult = ar.getFirstNode("key('byTeam', '" + team + "')");
 
Using the Visualizer, this was faster than using the individual subrequests (13ms vs 15 ms, across a awayResults HDS structure with 20 teamResults nodes).

Tested this with the Request Trace:

	<request>
		<identifier>active:ResultsTable</identifier>
		<argument name="matches">active:MatchResults+season@2014-2015+csvFile@res:/resources/matches/2014-2015.csv</argument>
	</request>
 
## 6. XUNIT Tests Triggered from Eclipse

I recalled I had previously experimented in using RobotFramework to externally trigger the execution of NetKernel XUnit tests, and assert the results returned.  This worked by using an (undocumented?) known url that allows externalsystems to trigger execution of a module's xUnit tests via the backend fulcrum.

For example, executing the following from the host command line:

    $ curl http://localhost:1060/test/exec/xml/test:urn:uk:co:rsbatechnology:football:matches
    
Returns this http response:

	<?xml version="1.0" encoding="UTF-8"?>
	<testlist>
		<results>
			<space>urn:uk:co:rsbatechnology:football:matches:test</space>
			<version>1.0.0</version>
			<uri>res:/resources/test/testlist.xml</uri>
			<testTotal>1</testTotal>
			<testRun>1</testRun>
			<testSuccess>1</testSuccess>
			<testFailException>0</testFailException>
			<testFailAssert>0</testFailAssert>
			<testExecutionTime>0</testExecutionTime>
			<testTotalTime>4</testTotalTime>
			<testDate>Sat, 21 Jan 2017 15:49:31 GMT</testDate>
		</results>
		<module>
			<uri>urn:uk:co:rsbatechnology:football:matches</uri>
		</module>
		<!-- Add your tests here -->
		<test testStatus="success">
			<request requestTime="0">
				<identifier>res:/THIS-WILL-FAIL-TO-RESOLVE-CHANGE-ME</identifier>
			</request>
			<assert>
			<exception/>
			</assert>
		</test>
	</testlist>

  
So I experimented with setting up a jUnit test that uses the Apache httpclient library to trigger my local NKEE instance to run the xUnit tests for this module, and then assert the returned results.

## 7. XUnit test for active:HomeResults

To test this resource, we need to provide a set of matches in the correct-format HDS document, and then assert the computed results.

Used a literal representation of an example response from a active:MatchResults resource.  But got an exception because it did not represent the expected HDS2 node value datatype (LocalDate).
According to tab@1060research.com, it would work if I specified a base64 - encoded serialised LocalDate object, but that is not terribly readable.
I got round this issue by changing the node value types all to strings. Instead, I could have written some groovy code to create the example HDS representation, possibly even embedding this in the test file (as a request to active:groovy, returning the HDS structure) 

## 8. Refactor into separate modules

Refactored the XUnit tests into a separate urn:test:uk:co:rsbatechnology:football:matches module.  I also moved the example csv file out of the main module and into the test module. 

## 9. Added RDBMS space

Created a space to encapsulate a database containing football match results, as an example of using NetKernel's RDBMS module.

I triggered the active:initDatabase endpoint by issuing the request into the test rootspace (after importing the new rdbms space into the test module rootspace), in order that the `matches` argument could be resolved to the HDS set of matches obtained from CSV file.

	<request>
		<identifier>active:initDatabase</identifier>
		<argument name="matches">active:MatchResults+season@2014-2015+csvFile@res:/resources/matches/2014-2015.csv</argument>
	</request>

This request effectively transfers football match state from CSV -> HDS -> RDBMS.  Resolution into the test space superstack ensures that all of the endpoints can be resolved.

Whilst writing the MatchResultsRDBMSAccessor class, I used the NetKernel SQL Playpen to check the SQL statements.  I also wrote a jUnit test, MatchResultsRDBMSACcessorTests, to test the internal parsing of the returned ResultSet HDS structure into the Match Results HDS.

I added a simple XUnit test to assert that the endpoint returns valid HDS match representation.



Gotchas & Lessons Learnt
-----------------------
Stumbling blocks encountered whilst working on this module:

 1. This module documentation in /src/resources/doc/dev_diary.md would not render in the NK docs system, instead threw a SAXParseException.
 
    I used the Visualizer to try and trace where the rendering was going wrong, then got to the BookViewProcess.gy endpoint code. I finally figured out what was going on .. the rendered representation from active:docRendererInner was NOT escaping xml element markup in the text of this page e.g. `<rootspace>` (without the backticks). So when NK tried to transrept the XML markeup to DOM (for rendering), it failed because it found unterminated xml elements in the book xml (including the rendered doc content).  The solution was to ensure that all xml element markup in this page is surrounded by backticks or as code blocks.
    
        org.xml.sax.SAXParseException
        The element type "accessor" must be terminated by the matching end-tag "</accessor>".

      
 2. Structure module endpoint java files under `src/<package>/endpoint packages`, then create `src/<package>/doc` folder for endpoint - specific documentation, referenced in module.xml by `<doc>res:/<package>/doc/foo.md</doc>`.
 
 3. Use the NK Backend Module Explorer "Browse Module Files" menu option to quickly view all files (especially module.xml) from a module, e.g. Netkernel modules.
 
      <rootspace> </rootspace>

 4. Use the active:fragmentHDS in conjunction with the RequestTrace tool to experiment with xpath expressions (where the expression is supposed to return a single node).  I already had org.netkernel.mod.hds imported into my application space, so I could just inject a request into my application space, using other resources and test data I had available:
 
	 <request>
	  <identifier>active:fragmentHDS</identifier>
	        <argument name="xpath"><literal type="string">/teamResults/teamResult[team='Liverpool']</literal></argument>
	  <argument name="operand">
	        <request>
	          <identifier>active:HomeResults</identifier>
	            <argument name="matches">active:MatchResults+season@2014-2015</argument>
	        </request>
	</argument>
	</request>
	
 5. Standard module endpoint `<doc>` element is no longer supported, although it is still documented. The expected technique is:
 5.1  to add a doc (referenced in the module's etc/system/Docs.xml) for each endpoint you want to document, also specifying `<category>doc accessor</category>`
 5.2 adding {endpoint}MyEndpointUri{endpoint} macro to bring in the auto-generated endpoint documentation.
 
  6. XUnit Test space public endpoints
     All endpoints in a XPath unit test space MUST BE PUBLIC!  Even any `<import>`ed spaces required for the tests.  Private endpoints cannot be resolved by the external XUnit test manager. 
     
  7. Exporting contents of jars from a module using the `<system><classpath><export>` must specify the correct regex to include all classes and other files you want to expose to other modules. eg:
  7.1 "com\.foo\.bar\..*" for all java classes in the com.foo.bar package (and sub-packages).
  7.2 "com/foo/bar/..*" for files of any type under the file path com/foo/bar
  
  8. The `active:java` language runtime endpoint does not get access to Netkernel's superstack classloader (ie. access to classes via modules imported into the module containing the `active:java` endpoint). It only gets access to the local lib/ folder and classes in the module itself (unlike other language runtime endpoints like active:groovy).
   
  9. To remote debug a java endpoint in a running NetKernel instance:
  9.1 Start the NetKernel instance by executing `./netkernel.sh -debug` in the **[Install]/bin** folder. This will open a remote debugger port 8000.
  9.2 In Eclipse, switch to the Debug perspective, and create a new Debug run configuration "Remote Java Application". Enter the hostname e.g. `localhost` and the debug port e.g. `8000`
  9.3 Run the debug configuration.
  9.4 In your local dynamic module in Eclipse, set breakpoints on the endpoint(s) you want to examine
  9.5 Trigger NK requests as appropriate to hit the endpoints you want to debug.
  
  10. To shutdown a running Netkernel that was started with `$ ./netkernel.sh` from the command line, hit Ctrl+C,. This will gracefully shut the instance down. Using Ctrl+Z will kill the local command, but leave the NK process running (then have to use `ps -ax | grep NK` then `kill -9 373737`)

Useful Eclipse Plugins for NetKernel Development
------------------------------------------------

Gradle

Mylyn WikiText (for authoring module documentation markdown)

DBBeaver (SQL)

Groovy Editor (https://github.com/groovy/groovy-eclipse/wiki)
 
For more information on editing documentation see the [doc:sysadmin:guide:doc:editing|Editing Guide].