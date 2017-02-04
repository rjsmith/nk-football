package uk.co.rsbatechnology.football.endpoint;

import static org.junit.Assert.*;

import org.junit.Test;
import org.netkernel.mod.hds.HDSFactory;
import org.netkernel.mod.hds.IHDSDocument;
import org.netkernel.mod.hds.IHDSMutator;
import org.netkernel.mod.hds.IHDSReader;

public class MatchResultsRDBMSAccessorTests {

    @Test
    public void testResultSetToMatchResultsHDS() {
        
        // SETUP
        // Create a simulated RDBMS result set.
        // See: http://localhost:1060/book/view/book:mod-db:book/doc:mod-db:query
        IHDSMutator m = HDSFactory.newDocument();
        m.pushNode("resultset");
        m.pushNode("row").addNode("ID", 1).addNode("SEASON", "2014-2015").addNode("DIV", "E0")
                .addNode("DATE", "2014-08-16").addNode("HOMETEAM", "Arsenal").addNode("AWAYTEAM", "Crystal Palace")
                .addNode("FTHG", 2).addNode("FTAG", 1).addNode("FTR", "W").popNode();
        m.popNode();
        IHDSDocument rs = m.toDocument(false);
        
        // EXECUTE
        MatchResultsRDBMSAccessor accessor = new MatchResultsRDBMSAccessor();
        IHDSDocument matches = accessor.parseResultSet(rs, "2014-2015");
        
        // VERIFY
        assertEquals(matches.getReader().getFirstValue("/season"), "2014-2015");
        IHDSReader r = matches.getReader().getFirstNodeOrNull("/matches");
        assertNotNull(r);
        IHDSReader match = r.getFirstNode("match");
        assertNotNull(match);
        assertEquals(match.getFirstValue("homeTeam"), "Arsenal");
        assertEquals(match.getFirstValue("date"), "2014-08-16");
    }

}
