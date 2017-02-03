package uk.co.rsbatechnology.football.endpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.netkernel.layer0.meta.impl.SubstringArgumentMetaImpl;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFRequestReadOnly;
import org.netkernel.layer0.representation.IReadableBinaryStreamRepresentation;
import org.netkernel.mod.hds.HDSFactory;
import org.netkernel.mod.hds.IHDSDocument;
import org.netkernel.mod.hds.IHDSMutator;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;

/**
 * Accessor responsible for returning a HDS2 representation of a set (season) of football match results.
 *
 */
public class MatchResultsAccessor extends StandardAccessorImpl {

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter ISOFormatter = DateTimeFormatter.ISO_DATE;
	public MatchResultsAccessor()
	{	
	    this.declareThreadSafe();
	    
	    // Tell the system what representation is returned from a SOURCE request
	    this.declareSourceRepresentation(IHDSDocument.class);
	    	    
	}
	
	@Override
	public void onSource(INKFRequestContext context) throws Exception
	{	
		// WHAT HAVE WE PROMISED
		INKFRequestReadOnly thisRequest = context.getThisRequest();
		
		String season = thisRequest.getArgumentValue("season");
		
		// WITH WHAT ELSE DO WE NEED
		// Source the underlying csv file contents
		IReadableBinaryStreamRepresentation stream = context.source("arg:csvFile", IReadableBinaryStreamRepresentation.class);
		BufferedReader br = new BufferedReader(new InputStreamReader(stream.getInputStream(), StandardCharsets.UTF_8));

		// HOW TO ADD VALUE
		IHDSDocument matches = parseFootballCSVContents(br, season);
		
		// REPLY BY BUILDING A RESPONSE
		context.createResponseFrom(matches);
		
		// MODIFY THE RESPONSE
	}

	/**
	 * Internal method to parse a https://github.com/jokecamp/FootballData csv file
	 * 
	 * Keys to csv data described here:
	 * https://github.com/jokecamp/FootballData/tree/master/football-data.co.uk
	 * 
	 * @param br
	 * @return
	 * @throws IOException
	 */
	private IHDSDocument parseFootballCSVContents(BufferedReader br, String season) throws IOException {
		String line;
		
		// Skip header row
		br.readLine();
		
		// Build a HDS tree to represent all matches
		IHDSMutator m = HDSFactory.newDocument();
		m.addNode("season", season);
		m.pushNode("matches");
		
		while ((line = br.readLine()) != null) {
			String[] cols = line.split(",");
			m.pushNode("match");
			m.addNode("div", cols[0]);
			m.addNode("date", LocalDate.parse(cols[1], formatter).format(ISOFormatter));
			m.addNode("homeTeam", cols[2]);
			m.addNode("awayTeam", cols[3]);
			m.addNode("fthg", cols[4]); // Full Time Home Goals
			m.addNode("ftag", cols[5]); // Full Time Away Goals
			m.addNode("ftr", cols[6]);  // Full Time Result (H = home win, D = draw, A = away win)
			m.popNode();
		}
		m.popNode();
		
		return m.toDocument(false);
		
	}

}
