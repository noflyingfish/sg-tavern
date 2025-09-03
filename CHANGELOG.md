# Changelog - sg-tavern

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2025-09-04
### Added
- Java 21
- `/eventstatus` to expose event details tracked
- `/manageevent` to allow edit event details tracked

## [1.2.2] - 2025-08-30
### Fixes
- Deleted event post removed from promo message

## [1.2.1] - 2025-07-26
### Added
- Bug fix to `/colour` command
- Stop sending intro check message to admin
- Attempt to listen to message in #upcoming-events with datetime

## [1.2.0] - 2025-07-26
### Fixes
- Logging output to file with log4j2

## [1.1.7] - 2025-06-21
### Fixes
- Bug fix to `/colour` command
- Bug fix to tracking of event posts

## [1.1.6] - 2025-06-21
### Added
- Colour Role with the `/colour` command

## [1.1.5e] - 2025-06-21
### Added
- Deleting the previous day's upcoming event schedules upon new day's message

## [1.1.5d] - 2025-06-15
### Added
- Cutover the upcoming event schedules into #events-promo

## [1.1.5c] - 2025-06-15
### Added
- Scheduler to post upcoming event schedules (testing, posting to mod only channel)

## [1.1.5b] - 2025-06-13
### Added
- Scheduler to track and mark past event posts (if managed)

## [1.1.5a] - 2025-06-12
### Added
- Scheduler to track new/edited event posts in #upcoming-events

## [1.1.4] - 2025-06-11
### Added
- Auto 'Event Organisers' _Role_ to anyone posted in #upcoming-events
- Admin message to admin-bot channel on post creation in #upcoming-events

## [1.1.3] - 2025-06-07
### Added
- Admin message to admin-bot channel on user join/exit

## [1.1.2] - 2025-05-09
### Fixes
- Display name changes in the admin bot log
- Limited invite to once use
- Limited `/invite` to only Roles with _Create Invite_ Permission

## [1.1.1] - 2025-03-15
### Fixes
- EffectiveName to Name in new_joiner table

## [1.1.0] - 2025-03-15
### Added
- Feature - Auto add/remove newcomer role for new user
- Background - Added database connection

## [1.0.1] - 2025-03-15
### Added
- Feature - command to send user server invite link

## [1.0.1] - 2025-03-15
### Added
- Feature - command to trigger intro check

## [1.0.0] - 2025-03-14
### Added
- Initial release
- Feature - Intro check scheduler

