CREATE TABLE IF NOT EXISTS new_joiner(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    userId VARCHAR(48),
    username VARCHAR(255),
    joinDateTime TIMESTAMP
);

CREATE TABLE IF NOT EXISTS upcoming_event(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    postName VARCHAR(255),
    postId VARCHAR(255),
    postUrl VARCHAR(255),
    postStatus VARCHAR(255),
    eventDetailMsgId VARCHAR(255),
    createdOn TIMESTAMP,
    updatedOn TIMESTAMP,
    processedEventName VARCHAR(255),
    processedEventLocation VARCHAR(255),
    processedEventDateTime TIMESTAMP
);

CREATE TABLE IF NOT EXISTS message_reference(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    messageId VARCHAR(255),
    messagePurpose VARCHAR(255),
    createdOn TIMESTAMP,
    updatedOn TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role_colour(
    userId VARCHAR(48) PRIMARY KEY,
    username VARCHAR(255),
    colourCode VARCHAR(8),
    roleId VARCHAR(48),
    randomFlag VARCHAR(1),
    updatedOn TIMESTAMP
);