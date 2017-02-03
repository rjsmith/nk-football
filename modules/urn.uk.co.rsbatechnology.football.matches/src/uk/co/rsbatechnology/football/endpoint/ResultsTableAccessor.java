package uk.co.rsbatechnology.football.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.NKFException;
import org.netkernel.mod.hds.HDSFactory;
import org.netkernel.mod.hds.IHDSDocument;
import org.netkernel.mod.hds.IHDSMutator;
import org.netkernel.mod.hds.IHDSReader;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;

public class ResultsTableAccessor extends StandardAccessorImpl {

    public ResultsTableAccessor() {
        this.declareThreadSafe();
    }

    @Override
    public void onSource(INKFRequestContext context) throws Exception {
        
        // WHAT
        
        // WITH
        // Get Home and Away team results
        INKFRequest homeResultsRequest = context.createRequest("active:HomeResults");
        homeResultsRequest.addArgument("matches", "arg:matches");
        IHDSDocument homeResults = (IHDSDocument) context.issueRequest(homeResultsRequest);

        INKFRequest awayResultsRequest = context.createRequest("active:AwayResults");
        awayResultsRequest.addArgument("matches", "arg:matches");
        IHDSDocument awayResults = (IHDSDocument) context.issueRequest(awayResultsRequest);

        // HOW
        Map<String, ResultsTableValueHolder> results = generateResultsTable(homeResults, awayResults);
        
        IHDSMutator m = HDSFactory.newDocument();
        m.pushNode("teamResults");
        for (ResultsTableValueHolder team : results.values()) {
            m.pushNode("teamResult");
            m.addNode("team", team.team);
            m.addNode("played",  team.played);
            m.addNode("won",  team.won);
            m.addNode("drawn",  team.drawn);
            m.addNode("lost", team.lost);
            m.addNode("goalsFor", team.goalsFor);
            m.addNode("goalsAgainst", team.goalsAgainst);
            m.addNode("goalsDifference", team.goalsDifference);
            m.addNode("points",  team.points);
            m.popNode();
        }
        
        m.popNode();

        // REPLY
        context.createResponseFrom(m.toDocument(false));
        
    }
    
    protected Map<String, ResultsTableValueHolder> generateResultsTable(IHDSDocument homeResults, IHDSDocument awayResults) throws NKFException {
        Map<String, ResultsTableValueHolder> tableMap = new HashMap<String, ResultsTableValueHolder>();
        IHDSReader hr = homeResults.getReader();
        IHDSReader ar = awayResults.getReader();
        
        // Loop over one of the results sets
        for (IHDSReader homeResult : hr.getNodes("/teamResults/teamResult")) {
            String team = (String) homeResult.getFirstValue("team");
            
            // Get away results for same team, using HDS2 lookup by key
            IHDSReader awayResult = ar.getFirstNode("key('byTeam', '" + team + "')");
            
//            INKFRequest awayTeamResultRequest = context.createRequest("active:fragmentHDS");
//            awayTeamResultRequest.addArgumentByRequest("operand", awayResultsRequest);
//            awayTeamResultRequest.addArgumentByValue("xpath", "/teamResults/teamResult[team='" + team + "']");
//            IHDSReader awayResult = ((IHDSDocument) context.issueRequest(awayTeamResultRequest)).getReader().getFirstNode("/teamResult");
            
            ResultsTableValueHolder teamResults = new ResultsTableValueHolder(team);
            teamResults.played = (int) homeResult.getFirstValue("played") + (int)awayResult.getFirstValue("played");
            teamResults.won = (int) homeResult.getFirstValue("won") + (int)awayResult.getFirstValue("won");
            teamResults.drawn = (int) homeResult.getFirstValue("drawn") + (int)awayResult.getFirstValue("drawn");
            teamResults.lost = (int) homeResult.getFirstValue("lost") + (int)awayResult.getFirstValue("lost");
            teamResults.goalsFor = (int) homeResult.getFirstValue("goalsFor") + (int)awayResult.getFirstValue("goalsFor");
            teamResults.goalsAgainst = (int) homeResult.getFirstValue("goalsAgainst") + (int)awayResult.getFirstValue("goalsAgainst");
            teamResults.goalsDifference = teamResults.goalsFor - teamResults.goalsAgainst;
            teamResults.points = (int) homeResult.getFirstValue("points") + (int)awayResult.getFirstValue("points");

            tableMap.put(team, teamResults);
            
        }

        return tableMap;
    }
    
    /**
     * Value holder class for one team;s results table entry
     * @author richardsmith
     *
     */
    public class ResultsTableValueHolder {
        
        public String team;
        public int played;
        public int won;
        public int drawn;
        public int lost;
        public int goalsFor;
        public int goalsAgainst;
        public int goalsDifference;
        public int points;
        
        public ResultsTableValueHolder(String team) {
            this.team = team;
        }
    }
    
    
}
