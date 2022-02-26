#############################################################
Welcome to the LevelledMobs configuration directory!
#############################################################

Here you can change how LevelledMobs functions. :)
Please read the rest of this file if you're new to
using LevelledMobs 4.




#############################################################
What does each file do? Where can I learn more about LM?
#############################################################

All documentation is provided on our Wiki - please see:
<https://github.com/lokka30/LevelledMobs/wiki>




#############################################################
How do I make my changes to these files take effect?
#############################################################

When you make any changes to the configuration files,
please run the `/lm reload` command or re-start your server,
so that your edits will be in effect.

Only new levelled mobs will be affected by any changes. You
can allow all loaded mobs to re-spawn by killing existing
levelled mobs (by running `/lm kill all *`), or alternatively,
you can 're-level' all *loaded* mobs
(by running `/lm rules force_all`). To reiterate, this only
functions for *loaded* mobs.

(Do not use your server's `/reload` command, it is known
to cause issues with many plugins, even the server itself!)