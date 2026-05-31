****# sg-tavern

sg tavern is a discord bot created for Discord server SG Tavern.

This is a friend making discord for people in Singapore.

---

## Feature

### General Commands
`/colour [csscode]` <br>
Change the user's discord name colour. <br>
`/allthreads` <br>
List all the public channel and threads. <br>

### Mod Commands
`/invite` <br>
DM the user the invite link for the discord server. Valid for 48 hours. <br>
Admin message when command used. <br>
<s>`/hammycheckintro` <br> </s>
~~Every 2 days or upon command, DM hammy with the members name who haven't do their introduction.~~

### Event postings
Daily post to promote upcoming events.

`/eventstatus` <br>
To display event info in post<br>
`/manageevent [eventname] [eventlocation] [eventdatetime]` <br>
To manage the stored info regarding this event in post <br>

### Roles
Assign a role to all newcomers. Remove the newcomer role after 3 months. <br>
Assign a role to all post creator in upcoming-events channel. <br>

### Admin messages
Whenever a post created in upcoming-events channel. <br>
Whenever someone joined the server. <br>
Whenever someone left the server. <br>

### Scheduler Runtime
0000 - Close expired polls<br>
0005 - Reset monthly invite counters (1st only)<br>
0010 - Remove 3+ month newbie role<br>
0015 - Mark past events as PAST/DELETED<br>
0600 - Daily event promo embed<br>
2100 (every 3 days) - Check intro done<br>
2300 - GPT extract NEW/EDITED events<br>



