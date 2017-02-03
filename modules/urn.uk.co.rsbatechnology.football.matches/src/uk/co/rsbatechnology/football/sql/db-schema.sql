CREATE TABLE ticker (
    id        	INTEGER IDENTITY,
    symbol 		VARCHAR(100)
);

CREATE TABLE instantprice (
    id        	INTEGER IDENTITY,
    tickerid 	INTEGER,
    price 		FLOAT,
    tickersize  FLOAT
);

CREATE TABLE closingprice (
    id        	INTEGER IDENTITY,
    day			INTEGER,
    tickerid 	INTEGER,
    price 		FLOAT
);

CREATE TABLE quantity (
    id        	INTEGER IDENTITY,
    day			INTEGER,
    tickerid 	INTEGER,
    shares 		INTEGER
);

CREATE TABLE indexlist (
    id        	INTEGER IDENTITY,
    tickerid 	INTEGER,
    position 	INTEGER
);

CREATE TABLE indexhistory (
    id      INTEGER IDENTITY,
    day 	INTEGER,
    value 	FLOAT
);

CREATE TABLE marcaphistory (
    id      INTEGER IDENTITY,
    day 	INTEGER,
    value 	FLOAT
);