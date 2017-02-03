package uk.co.rsbatechnology.football.endpoint;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.netkernel.mod.hds.HDSFactory;
import org.netkernel.mod.hds.IHDSDocument;
import org.netkernel.mod.hds.IHDSMutator;

import uk.co.rsbatechnology.football.endpoint.HomeAwayResultsAccessor;
import uk.co.rsbatechnology.football.model.HomeAwayResultsValueHolder;

public class HomeResultsAccessorTests {

	private HomeAwayResultsAccessor accessor;
	private IHDSDocument matches;

	@Before
	public void setup() {
		IHDSMutator m = HDSFactory.newDocument();
		m.addNode("season", "20014-2015");
		m.pushNode("matches");
		createMatch(m, "D1", LocalDate.now(), "Foo", "Bar", 2, 1, "W");
		m.popNode();
		matches = m.toDocument(false);
		accessor = new HomeAwayResultsAccessor();
	}
	
	@Test
	public void testHome() {
		// SETUP
		
		// EXECUTE
		Map<String, HomeAwayResultsValueHolder> homeResults = accessor.parseHomeAwayTeamResults(matches, true /* isHome */);
		
		// VERIFY
		assertNotNull("homeAwayResults return is null", homeResults);
		HomeAwayResultsValueHolder teamResults = homeResults.get("Foo");
		assertNotNull(teamResults);
		assertEquals(teamResults.played, 1);
		assertEquals(teamResults.won, 1);
		assertEquals(teamResults.lost, 0);
		assertEquals(teamResults.goalsFor, 2);
		assertEquals(teamResults.goalsAgainst, 1);
		assertEquals(teamResults.points, 3);
		
	}

	@Test
	public void testAway() {
		// SETUP
		
		// EXECUTE
		Map<String, HomeAwayResultsValueHolder> awayResults = accessor.parseHomeAwayTeamResults(matches, false /* isHome */);
		
		// VERIFY
		assertNotNull("homeAwayResults return is null", awayResults);
		HomeAwayResultsValueHolder teamResults = awayResults.get("Bar");
		assertNotNull(teamResults);
		assertEquals(teamResults.played, 1);
		assertEquals(teamResults.won, 0);
		assertEquals(teamResults.lost, 1);
		assertEquals(teamResults.goalsFor, 1);
		assertEquals(teamResults.goalsAgainst, 2);
		assertEquals(teamResults.points, 0);
		
	}

	private void createMatch(IHDSMutator m, String div, LocalDate date, String homeTeam, String awayTeam, int homeGoals, int awayGoals, String result) {
		m.pushNode("match");
		m.addNode("div", div);
		m.addNode("date", date.format(DateTimeFormatter.ISO_DATE));
		m.addNode("homeTeam", homeTeam);
		m.addNode("awayTeam", awayTeam);
		m.addNode("fthg", String.valueOf(homeGoals)); // Full Time Home Goals
		m.addNode("ftag", String.valueOf(awayGoals)); // Full Time Away Goals
		m.addNode("ftr", result);  // Full Time Result (H = home win, D = draw, A = away win)
		m.popNode();
	}
}
