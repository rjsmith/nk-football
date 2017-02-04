package uk.co.rsbatechnology.football.endpoint;

import java.time.LocalDate;
import java.util.List;

import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFRequestReadOnly;
import org.netkernel.mod.hds.HDSFactory;
import org.netkernel.mod.hds.IHDSDocument;
import org.netkernel.mod.hds.IHDSMutator;
import org.netkernel.mod.hds.IHDSReader;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;

/**
 * Retrieve season match results from RDBMS and return HDS representation
 * @author richardsmith
 *
 */
public class MatchResultsRDBMSAccessor extends StandardAccessorImpl {

    public MatchResultsRDBMSAccessor() {
        this.declareThreadSafe();
    }

    @Override
    public void onSource(INKFRequestContext context) throws Exception {

        // WHAT
        INKFRequestReadOnly request = context.getThisRequest();
        String season = (String) request.getArgumentValue("season");
        
        // WITH
        INKFRequest sqlRequest = context.createRequest("active:sqlQuery");
        sqlRequest.addArgumentByValue("operand", "SELECT * FROM matches WHERE season = '" + season + "'");
        sqlRequest.setRepresentationClass(IHDSDocument.class);
        IHDSDocument resultSet = (IHDSDocument) context.issueRequest(sqlRequest);
        
        // HOW
        IHDSDocument matches = parseResultSet(resultSet, season);
        
        // REPLY
        context.createResponseFrom(matches);
    
    }

    protected IHDSDocument parseResultSet(IHDSDocument rs, String season) {
        
        // Build a HDS tree to represent all matches
        IHDSMutator m = HDSFactory.newDocument();
        m.addNode("season", season);
        m.pushNode("matches");
        
        List<IHDSReader> rows = rs.getReader().getNodes("/resultset/row");
        
        for (IHDSReader row : rows) {
            
            m.pushNode("match");
            m.addNode("div", row.getFirstValue("DIV"));
            m.addNode("date", row.getFirstValue("DATE"));
            m.addNode("homeTeam", row.getFirstValue("HOMETEAM"));
            m.addNode("awayTeam", row.getFirstValue("AWAYTEAM"));
            m.addNode("fthg", row.getFirstValue("FTHG")); // Full Time Home Goals
            m.addNode("ftag", row.getFirstValue("FTAG")); // Full Time Away Goals
            m.addNode("ftr", row.getFirstValue("FTR"));  // Full Time Result (H = home win, D = draw, A = away win)
            m.popNode();
        }
        m.popNode(); // matches
        
        return m.toDocument(false);

    }



}
