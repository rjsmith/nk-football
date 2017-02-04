CREATE TABLE matches (
    id        	INTEGER IDENTITY,
    season 		VARCHAR(100),
    div 		VARCHAR(100),
    date		DATE,
    homeTeam	VARCHAR(100),
    awayTeam	VARCHAR(100),
    fthg		INTEGER,
    ftag		INTEGER,
    ftr			VARCHAR(1)
);

