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
    confirmedOn TIMESTAMP,
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

CREATE TABLE IF NOT EXISTS poll(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channelId VARCHAR(255),
    messageId VARCHAR(255),
    creatorUserId VARCHAR(255),
    question LONGTEXT,
    maxSelection INTEGER,
    durationOption VARCHAR(32),
    option1 LONGTEXT,
    option2 LONGTEXT,
    option3 LONGTEXT,
    option4 LONGTEXT,
    option5 LONGTEXT,
    status VARCHAR(32),
    createdOn TIMESTAMP,
    publishedOn TIMESTAMP,
    closesOn TIMESTAMP,
    closedOn TIMESTAMP
);

CREATE TABLE IF NOT EXISTS poll_vote(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pollId BIGINT,
    voterUserId VARCHAR(255),
    optionNumber INTEGER,
    createdOn TIMESTAMP,
    CONSTRAINT uk_poll_vote_selection UNIQUE (pollId, voterUserId, optionNumber)
);

ALTER TABLE upcoming_event ADD COLUMN signUpMsgId VARCHAR(255) NULL;
ALTER TABLE upcoming_event ADD COLUMN maxCap INTEGER NULL;

CREATE TABLE IF NOT EXISTS event_attendance(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    postId VARCHAR(255) NOT NULL,
    userId VARCHAR(255) NOT NULL,
    displayName VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    createdOn DATETIME
);

ALTER TABLE event_attendance ADD COLUMN isMain BIT NOT NULL DEFAULT 0;