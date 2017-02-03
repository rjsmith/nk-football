package uk.co.rsbatechnology.football.model;

public class HomeAwayResultsValueHolder {
	public String team;
	public int played;
	public int won;
	public int drawn;
	public int lost;
	public int goalsFor;
	public int goalsAgainst;
	public int points;
	
	public HomeAwayResultsValueHolder(String team) {
		this.team = team;
	}
}
