# AutoMend / RepairCycler
Automatically switches to low durability armor (and tools in offhand, if wanted) when getting AFK XP.

- Will switch to low durability pieces on its own when turned on.
- Will wait for a piece to be fully repaired before cycling to the next one.
- Will not overload the server with packets, has packet timer.

## Settings:
- UseOffhand - Uses your offhand to hold more damaged equipment. Temporarily turns off AutoTotem.
- PrioritizeTools - Prioritize putting tools in your offhand instead of armor
- Announce - Lets you know with an UI popup when an item is repaired & replaced
- ClickDelay - To prevent getting kicked we have a delay between clicks
