# The Punisher

### Commands (Format: command -> pemrission)
- /punisher -> punisher.help
- /ban (username/uuid/ip) (reason) -> punisher.ban
- /unban (username/uuid/ip) -> punisher.unban
- /checkban (username/uuid/ip) -> punisher.checkban
- /mute (username/uuid/ip) reason -> punisher.mute
- /unmute (username/uuid/ip) -> punisher.unmute
- /checkmute (username/uuid/ip) -> punisher.checkmute
- /alts (username/uuid/ip) -> punisher.alts
- /kick (username) (reason) -> punisher.kick
- /bypassban (username/uuid) -> punisher.bypassban
- /bypassmute (username/uuid) -> punisher.bypassmute
- /unbypassban (username/uuid) -> punisher.unbypassban
- /unbypassmute (username/uuid) -> punisher.unbypassmute
- /iphistory (username/uuid) -> punisher.iphistory
- /warn (username/uuid) (warning) -> punisher.warn
- /staffrollback (username/uuid) (bans/warns/mutes/all) (time) -> punisher.staffrollback
- /unwarn (username/uuid) -> punisher.unwarn
- /history (username/uuid) -> punisher.history

## Permissions (Format: permission -> effect)
- punisher.bypass.ban -> members with this permission can login even tho they are banned
- punisher.bypass.mute -> members with this permission can talk even tho they are muted
- punisher.notify -> get advanced information about punishments