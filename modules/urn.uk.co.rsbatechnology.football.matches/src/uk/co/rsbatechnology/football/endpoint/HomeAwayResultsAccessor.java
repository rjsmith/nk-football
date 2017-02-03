package uk.co.rsbatechnology.football.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.netkernel.layer0.meta.impl.SubstringArgumentMetaImpl;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFRequestReadOnly;
import org.netkernel.layer0.nkf.INKFResponse;
import org.netkernel.layer0.nkf.NKFException;
import org.netkernel.mod.hds.HDSFactory;
import org.netkernel.mod.hds.IHDSDocument;
import org.netkernel.mod.hds.IHDSMutator;
import org.netkernel.mod.hds.IHDSReader;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;

import uk.co.rsbatechnology.football.model.HomeAwayResultsValueHolder;

/**
 * Computes summary of team results when playing at home or away from a given set of match results.
 * 
 * @author richardsmith
 *
 */
public class HomeAwayResultsAccessor extends StandardAccessorImpl {

	public HomeAwayResultsAccessor() {
		this.declareThreadSafe();

	       // Tell the system what representation is returned from a SOURCE request
        this.declareSourceRepresentation(IHDSDocument.class);
        
        // Arguments
        this.declareArgument(new SubstringArgumentMetaImpl("homeOrAway", null /* set from grammar */, "String"));

	}

	@Override
	public void onSource(INKFRequestContext context) throws Exception {
		
		// WHAT
		INKFRequestReadOnly thisRequest = context.getThisRequest();
		String homeOrAway = thisRequest.getArgumentValue("homeOrAway");
		if (!homeOrAway.matches("(home|away)")) {
			throw new NKFException("homeOrAway must be either 'home' or 'away'");
		}
		
		// Such code is unecessary, because the attempted resolution of arg:matches below will throw a MissingArgument NKFException
//		INKFRequestReadOnly thisRequest = context.getThisRequest();
//		if (!thisRequest.argumentExists("matches")) {
//			throw new NKFException("Missing argument: matches");
//		}
		
		// WITH
		// Get HDS2  representation of set of matches
		IHDSDocument matches = context.source("arg:matches", IHDSDocument.class);
		
		// HOW
		final Map<String, HomeAwayResultsValueHolder> homeResults = parseHomeAwayTeamResults(matches, homeOrAway == "home");
		
		// Build response
		IHDSMutator m = HDSFactory.newDocument();
		m.pushNode("teamResults");
		for (HomeAwayResultsValueHolder team : homeResults.values()) {
			m.pushNode("teamResult");
			m.addNode("@team", team.team); // HDS2 index node
			m.addNode("team", team.team);
			m.addNode("played",  team.played);
			m.addNode("won",  team.won);
			m.addNode("drawn",  team.drawn);
			m.addNode("lost", team.lost);
			m.addNode("goalsFor", team.goalsFor);
			m.addNode("goalsAgainst", team.goalsAgainst);
			m.addNode("points",  team.points);
			m.popNode();
		}
		
		m.popNode();
		m.declareKey("byTeam", "/teamResults/teamResult", "@team");
		// REPLY
		INKFResponse response = context.createResponseFrom(m.toDocument(false));
		
	}
	
	protected Map<String, HomeAwayResultsValueHolder> parseHomeAwayTeamResults(IHDSDocument matches, boolean isHome) {
		Map<String, HomeAwayResultsValueHolder> teamResults = new HashMap<String, HomeAwayResultsValueHolder>();
		IHDSReader reader = matches.getReader();
		
		// Loop over all match nodes
		for(IHDSReader match: reader.getNodes("/matches/match")) {
			// Extract data out of HDS document
			String homeTeam = (String) match.getFirstValue("homeTeam");
			String awayTeam = (String) match.getFirstValue("awayTeam");
			int homeGoals = Integer.parseInt((String) match.getFirstValue("fthg"));
			int awayGoals = Integer.parseInt((String) match.getFirstValue("ftag"));
			
			String team = (isHome) ? homeTeam : awayTeam;
			
			HomeAwayResultsValueHolder result = teamResults.get(team);
			if (result == null) {
				result = new HomeAwayResultsValueHolder(team);
				teamResults.put(team, result);
			}
			
			// Played
			result.played++;
			
			if (isHome) {
				// Won
				if (homeGoals > awayGoals) {
					result.won++;
					result.points = result.points + 3;
				}
				// Lost
				if (homeGoals < awayGoals) {result.lost++;}
				// Drawn
				if (homeGoals == awayGoals) {
					result.drawn++;
					result.points = result.points + 1;
				}
				// GoalsFor
				result.goalsFor = result.goalsFor + homeGoals;
				// GoalsAgainst
				result.goalsAgainst = result.goalsAgainst + awayGoals;							
			} else {
				// Won
				if (awayGoals > homeGoals) {
					result.won++;
					result.points = result.points + 3;
				}
				// Lost
				if (awayGoals < homeGoals) {result.lost++;}
				// Drawn
				if (homeGoals == awayGoals) {
					result.drawn++;
					result.points = result.points + 1;
				}
				// GoalsFor
				result.goalsFor = result.goalsFor + awayGoals;
				// GoalsAgainst
				result.goalsAgainst = result.goalsAgainst + homeGoals;			
				
			}
		}
		
		return teamResults;
	}
	
}
