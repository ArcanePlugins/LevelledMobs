package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException;
import dev.jorel.commandapi.arguments.CustomArgument.MessageBuilder;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem.Chapter;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem.HomeChapter;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem.SubChapter;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class HelpSubcommand {

    private static final HelpSystem hs = new HelpSystem();

    public static CommandAPICommand createInstance() {
        final HomeChapter home = hs.getHomeChapter();
        home.addPages(
            """
            &8 • &9&nClick here&7 to view command help.
            &8 • &9&nClick here&7 to view documentation.
            &8 • &9&nClick here&7 to ask for support with LM.-------"""
        );

        { // Breadcrumb: Home -> Support
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

        { // Breadcrumb: Home -> Documentation
            final SubChapter home_documentation = new SubChapter(home, "Documentation");
            home_documentation.addPages(
                """
                &7Recommended documentation:
                
                &8 • &9&Read the Frequently Asked Questions
                &8 • &9&nRead the Wiki
                &8 • &9&nRead API Developer Javadocs"""
            );
        }

        { // Breadcrumb: Home -> /LM
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

            { // Breadcrumb: Home -> /LM -> About
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

            { // Breadcrumb: Home -> /LM -> Summon
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

                { // Breadcrumb: Home -> /LM -> Summon -> EntityType
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
            .withArguments(chapterArgument("chapter"))
            .executes((sender, args) -> {
                //TODO Add Translatable Messages for the header and footers
                //TODO Add Translatable Messages for the chapter pages.
                final ChapterAndPageNum arg = (ChapterAndPageNum) args[0];

                final Chapter c = arg.chapter();
                final int pageNum = arg.pageNum();

                sender.sendMessage(Message.formatMd(new String[]{c.getFormattedPage(pageNum)}));
            });
    }

    public static Argument<ChapterAndPageNum> chapterArgument(final String nodeName) {

        return new CustomArgument<>(new GreedyStringArgument(nodeName), info -> {
            final String[] breadcrumbs = info.input().split(" ");
            Chapter chapter = hs.getHomeChapter();
            int pageNum = 1;

            for(final String breadcrumb : breadcrumbs) {
                final Optional<SubChapter> optSubChapter = chapter.getSubChapters().stream()
                    .filter(subChapter -> subChapter.getId().equalsIgnoreCase(breadcrumb))
                    .findFirst();

                if(optSubChapter.isPresent()) {
                    chapter = optSubChapter.get();
                    continue;
                }

                // if it's not a valid chapter then it should be a valid page number
                try {
                    pageNum = Integer.parseInt(breadcrumb);
                    break;
                } catch(final NumberFormatException ignored) {
                    throw new CustomArgumentException(
                        new MessageBuilder("Invalid sub-chapter or page: ").appendArgInput()
                    );
                }
            }

            if(pageNum < 1 || pageNum > chapter.getPages().size()) {
                throw new CustomArgumentException(
                    new MessageBuilder("Page does not exist: ").appendArgInput()
                );
            }

            return new ChapterAndPageNum(chapter, pageNum);
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> {
            final String[] breadcrumbs = info.currentInput().split(" ");
            Chapter chapter = hs.getHomeChapter();
            boolean endsWithPageNum = false;

            for(final String breadcrumb : breadcrumbs) {
                final Optional<SubChapter> optSubChapter = chapter.getSubChapters().stream()
                    .filter(subChapter -> subChapter.getId().equalsIgnoreCase(breadcrumb))
                    .findFirst();

                if(optSubChapter.isPresent()) {
                    chapter = optSubChapter.get();
                    continue;
                }

                // if it ends with a page number, that's all good
                try {
                    Integer.parseInt(breadcrumb);
                    endsWithPageNum = true;
                    break;
                } catch(final NumberFormatException ignored) {
                    // they've entered something invalid -
                    return new String[]{};
                }
            }

            final List<String> suggestions = new LinkedList<>();

            // add sub-chapters to suggestions
            chapter.getSubChapters().forEach(subChapter -> suggestions.add(subChapter.getId()));

            // add page numbers to suggestions
            if(!endsWithPageNum) {
                for(int pageNum = 1; pageNum <= chapter.getPages().size(); pageNum++) {
                    suggestions.add(Integer.toString(pageNum));
                }
            }

            return suggestions.toArray(new String[0]);
        }));
    }

    public record ChapterAndPageNum(
        @Nonnull Chapter chapter,
        int pageNum
    ) {}

}
