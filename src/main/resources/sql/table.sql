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

CREATE TABLE IF NOT EXISTS members(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    userId VARCHAR(255),
    inviterId VARCHAR(255),
    inviteLinkCounter INTEGER DEFAULT 0,
    totalMembersInvited INTEGER DEFAULT 0,
    updatedOn TIMESTAMP,
    joinDatetime TIMESTAMP,
    leaveDatetime TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invite_link(
    inviteCode VARCHAR(255) PRIMARY KEY,
    creatorMemberId VARCHAR(255),
    createdOn TIMESTAMP
);
