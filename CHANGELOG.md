# Changelog - sg-tavern

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.6.5] - 2026-09-03
- [Event Sign Up] - Allow all member sign ups to be remove with chat messages

## [2.6.4] - 2026-08-27
- [Event Sign Up] - Setting first trigger of sign up to be always user himself
- [Event Sign Up] - Auto and manual closing of the form buttons. 

## [2.6.3] - 2026-06-08
- `/pingattending` command added
- bug fix on editing event title not refreshing event detail listener

## [2.6.2] - 2026-05-30
- `/pingreacts` command added
- minor fixes to the display order of attending/kiv/waitlist in signup

## [2.6.1] - 2026-05-30
- Trigger for event reset via post title change with confirmation

## [2.6.0] - 2026-05-28
- change for event post to be triggered basis from a button instead of scheduler
- pilot deployment for new event management flow
- scheduler timing change

## [2.5.1] - 2026-05-21
- beta change for event post to be triggered basis from a button instead of scheduler
- pilot list selection for new event management flow

## [2.5.0] - 2026-05-19
- `/pollanonymous` deployed

## [2.4.1] - 2026-05-14
- Huge behind the scene changes for invite link controls and tracking

## [2.4.0] - 2026-04-27
- new `/invite` command for members. Limit to 3 creation monthly.
- new `/invitemany` command for mods
- log4j2 history from daily to monthly

## [2.3.2] - 2026-03-15
- Message to mod channel on User's Role (event organiser/ newcomer) change

## [2.3.1] - 2026-03-10
- Improved GPT model and debug

## [2.3.0] - 2026-03-08
- New command `/everyone` for pinging 
- bug fix for `/extractevent`

## [2.2.2] - 2026-02-26
- Bug fix on sendGpt component
- deferred reply for `/extractevent`

## [2.2.1] - 2026-02-25
- Bug fix on `/eventstatus` and sendGpt component
- Rollback on event scheduler's gpt component

## [2.2.0] - 2026-02-01
- Major version change for JDA to 6.3.0 from 5.6.1
- Bug fix on `/eventstatus` and sendGpt component
- Clean up logging for newbie scheduler

## [2.1.4] - 2026-01-20
### Added
- Bug fix on deleted post in event scheduler

## [2.1.3] - 2026-01-03
### Added
- Update GPT prompt to date

## [2.1.2] - 2025-12-29
### Added
- Bug fix on event scheduler
- Fix on event date detection

## [2.1.1] - 2025-12-16
### Added
- Clean up the event scheduler for easier management

## [2.1.0] - 2025-11-22
### Added
- Check for regex check for `1.` for a namelist
- `/extractevent` send EventDetailPost to gpt to extract and update event

## [2.0.0] - 2025-11-21
### Added
- Attempt to use GPT to extract event info
- `/pastevent` to drop post from event detail message tracking

## [1.6.0] - 2025-11-09
### Added
- Attempted message tracking on event details
- `/resetevent` to reset tracked message on event 
- Managed edited event post on event basis instead of scheduler
- bug fixed date regex

## [1.5.0] - 2025-11-08
### Added
- Manage new event post creation on event basis, instead of scheduler
- A reminder message after event post title change

## [1.4.0] - 2025-09-23
### Added
- `/allthreads` to show all public channel and threads
- Description change to the daily tavern event post

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

