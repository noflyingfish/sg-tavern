CREATE TABLE IF NOT EXISTS new_joiner(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    userId VARCHAR(48),
    username VARCHAR(255),
    joinDateTime TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ge_minigame(
    userId VARCHAR(48),
    username VARCHAR(255),
    sengkang DOUBLE,
    tampines DOUBLE,
    punggol DOUBLE,
    createdON TIMESTAMP
);