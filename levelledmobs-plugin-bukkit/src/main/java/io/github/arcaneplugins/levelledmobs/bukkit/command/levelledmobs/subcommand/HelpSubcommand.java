package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem.HomeChapter;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem.SubChapter;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;

public final class HelpSubcommand {

    public static CommandAPICommand createInstance() {
        HelpSystem.setChapterPageSender(Log::inf);
        //TODO remove ^^^^^. ignore for now - need to remove this feature from arcane framework

        final HelpSystem hs = new HelpSystem();
        final HomeChapter home = hs.getHomeChapter();
        home.addPages(
            """
            &8 • &9&nClick here&7 to view command help.
            &8 • &9&nClick here&7 to view documentation.
            &8 • &9&nClick here&7 to ask for support with LM.-------"""
        );

        { // Home -> Support
            final SubChapter home_support = new SubChapter(home, "Support");
            home_support.addPages(
                """
                &9&nRead the Wiki&7 before contacting support.
                
                &7Free user support is provided by volunteer staff at the following sites:
                
                &8 • &9&nJoin ArcanePlugins Discord Guild
                &8 • &9&nJoin ArcanePlugins Matrix Space
                
                &7Do not use other sites to request support from our staff."""
            );
        }

        { // Home -> Documentation
            final SubChapter home_documentation = new SubChapter(home, "Documentation");
            home_documentation.addPages(
                """
                &7Recommended documentation:
                
                &8 • &9&Read the Frequently Asked Questions
                &8 • &9&nRead the Wiki
                &8 • &9&nRead API Developer Javadocs"""
            );
        }

        { // Home -> /LM
            final SubChapter home_lm = new SubChapter(home, "/LM");
            home_lm.addPages(
                // p1
                """
                &7Available sub-commands:
                
                &8 • &b/LM
                &8 ⎣ &7Provides commands to manage LevelledMobs.
                
                &8 • &b/LM About &9&n[read more]
                &8 ⎣ &7View info about the installed version of the plugin.
                
                &8 • &b/LM Backup &9&n[read more]
                &8 ⎣ &7Backup your config files.
                
                &8 • &b/LM Confirm &9&n[read more]
                &8 ⎣ &7Confirms a potentially dangerous action from another command.""",

                // p2
                """
                &8 • &b/LM Egg &9&n[read more]
                &8 ⎣ &7View info about the installed version of the plugin.
                
                &8 • &b/LM Help &9&n[read more]
                &8 ⎣ &7Backup your config files.
                
                &8 • &b/LM Kill &9&n[read more]
                &8 ⎣ &7Confirms a potentially dangerous action from another command.
                
                &8 • &b/LM Reload &9&n[read more]
                &8 ⎣ &7Confirms a potentially dangerous action from another command.""",

                // p3
                """
                &8 • &b/LM Routine
                &8 ⎣ &7View info about the installed version of the plugin.
                
                &8 • &b/LM Spawner &9&n[read more]
                &8 ⎣ &7Backup your config files.
                
                &8 • &b/LM Summon &9&n[read more]
                &8 ⎣ &7Confirms a potentially dangerous action from another command."""
            );

            { // Home -> /LM -> About
                final SubChapter home_lm_about = new SubChapter(home_lm, "About");
                home_lm_about.addPages(
                    """
                    &8&l » &7Command: &b/LM About
                    
                    &f&nDescription
                    &8 • &7View info about the installed version of the plugin.
                    
                    &f&nExample Usage
                    &8 • &9&n/LM About"""
                );
            }

            { // Home -> /LM -> Summon
                final SubChapter home_lm_summon = new SubChapter(home_lm, "Summon");
                home_lm_summon.addPages(
                    """
                    &8&l » &7Command: &b/LM Summon
                    
                    &f&nDescription
                    &8 • &7Summons a levelled mob of chosen specifications, similar to Minecraft's `/summon` command.
                    
                    &f&nSubcommands
                    &8 • &b/LM Summon <EntityType> ?...? &9&n[read more]
                    &8 • &b/LM Summon <CustomEntityId> ?...? &9&n[read more]""" //todo update
                );

                { // Home -> /LM -> Summon -> EntityType
                    final SubChapter home_lm_summon_entitytype =
                        new SubChapter(home_lm_summon, "EntityType");
                    home_lm_summon_entitytype.addPages(
                        """
                        &8&l » &7Command: &b/LM Summon EntityType
                                       
                        &f&nDescription
                        &8 • &7Summons a generic levelled mob of the specified entity type with chosen specifications.
                        
                        &f&nSubcommands
                        &8 • &b/LM Summon <EntityType> &9&n[read more]
                        """ //todo update
                    );
                }
            }
        }

        return new CommandAPICommand("help")
            .withPermission("levelledmobs.command.levelledmobs.help")
            .withShortDescription("View a list of available LM commands and support links.")
            .withFullDescription("Sends the user a list of available LevelledMobs commands and " +
                "also a list of URLs providing documentation and support.")
            .executes((sender, args) -> {
                //TODO Implement.
                //TODO Add Translatable Messages.
            });
    }

}
