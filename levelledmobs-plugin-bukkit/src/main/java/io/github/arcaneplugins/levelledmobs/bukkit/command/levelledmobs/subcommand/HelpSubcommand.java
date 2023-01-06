package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException;
import dev.jorel.commandapi.arguments.CustomArgument.MessageBuilder;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem.Chapter;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem.HomeChapter;
import io.github.arcaneplugins.arcaneframework.command.HelpSystem.SubChapter;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class HelpSubcommand {

    public static List<CommandAPICommand> createInstances() {
        HelpSystem.msgLeadingHelpCommand = "/lm help";
        HelpSystem.msgStructure = """
            
            &8┌ &b&lLM &fHelp Menu &8• &7Path: %breadcrumbs%
            
            &r%page-contents%&r
            
            &8└ &8&m-&8{%previous-page%&8}&7 Page %page-current%&7 of %page-max%&8 &8{%next-page%&8}&m-
            """;

        HelpSystem.msgHeadingPreviousPage = """
            [«««](color=blue format=bold run_command=%leading-help-command%%chapter-path% %previous-index%)""";

        HelpSystem.msgHeadingNextPage = """
            [»»»](color=blue format=bold run_command=%leading-help-command%%chapter-path% %next-index%)""";

        HelpSystem.msgHeadingPageMax = """
            [%max-index%](color=gray format=italic run_command=%leading-help-command%%chapter-path% %max-index%)""";

        HelpSystem.msgHeadingPageCurrent = """
            [%current-index%](color=gray format=italic run_command=%leading-help-command%%chapter-path% %current-index%)""";

        HelpSystem.msgBreadcrumb = """
            [%chapter-id%](color=white format=underlined run_command=%leading-help-command%%chapter-path%)""";

        HelpSystem.msgBreadcrumbSeparator = """
            &8 »\s""";

        final HomeChapter home = HelpSystem.getHomeChapter();

        home.getPages().clear();
        home.getSubChapters().clear();

        home.addPages(
            """
            &8 • [Click here](color=blue format=underlined run_command=/lm help /lm)&7 to view command help.
            &8 • [Click here](color=blue format=underlined run_command=/lm help documentation)&7 to view documentation.
            &8 • [Click here](color=blue format=underlined run_command=/lm help support)&7 to ask for support with LM."""
        );

        { // Breadcrumb: home -> support
            final SubChapter home_support = new SubChapter(home, "support");
            home_support.addPages(
                """
                [Check the Docs](color=blue format=underlined run_command=/lm help documentation)&7 before contacting support.
                
                &7Free user support is provided by volunteer staff at the following sites:
                
                &8 • [Join ArcanePlugins Discord Guild](color=blue format=underlined open_url=https://discord.gg/HqZwdcJ)
                &8 • [Join ArcanePlugins Matrix Space](color=blue format=underlined open_url=https://matrix.to/#/#arcaneplugins:matrix.org)
                
                &7Do not use other sites to request support from our staff."""
            );
        }

        { // Breadcrumb: home -> documentation
            final SubChapter home_documentation = new SubChapter(home, "documentation");
            home_documentation.addPages(
                """
                &7Recommended documentation:
                
                &8 • [Read the Frequently Asked Questions](color=blue format=underlined open_url=https://github.com/lokka30/LevelledMobs/wiki/Frequently-Asked-Questions)
                &8 • [Read the Wiki](color=blue format=underlined open_url=https://github.com/ArcanePlugins/LevelledMobs/wiki)
                &8 • [Read API Developer Javadocs](color=blue format=underlined open_url=https://lokka30.github.io/LevelledMobs/)"""
            );
        }

        { // Breadcrumb: home -> /lm
            final SubChapter home_lm = new SubChapter(home, "/lm");
            home_lm.addPages(
                // p1
                """
                &7Available sub-commands:
                
                &8 • [/lm](color=blue format=underlined run_command=/lm help /lm)
                &8 ⎣ &7Provides commands to manage LevelledMobs.
                
                &8 • [/lm about](color=blue format=underlined run_command=/lm help /lm about)
                &8 ⎣ &7View info about the installed version of the plugin.
                
                &8 • [/lm backup](color=blue format=underlined run_command=/lm help /lm backup)
                &8 ⎣ &7Backup your config files.
                
                &8 • [/lm confirm](color=blue format=underlined run_command=/lm help /lm confirm)
                &8 ⎣ &7Confirms a potentially dangerous action from another command.""",

                // p2
                """
                &8 • [/lm egg](color=blue format=underlined run_command=/lm help /lm egg)
                &8 ⎣ &7TBD
                
                &8 • [/lm help](color=blue format=underlined run_command=/lm help /lm help)
                &8 ⎣ &7TBD
                
                &8 • [/lm kill](color=blue format=underlined run_command=/lm help /lm kill)
                &8 ⎣ &7TBD
                
                &8 • [/lm reload](color=blue format=underlined run_command=/lm help /lm reload)
                &8 ⎣ &7TBD""",

                // p3
                """
                &8 • [/lm routine](color=blue format=underlined run_command=/lm help /lm routine)
                &8 ⎣ &7TBD
                
                &8 • [/lm spawner](color=blue format=underlined run_command=/lm help /lm spawner)
                &8 ⎣ &7TBD
                
                &8 • [/lm summon](color=blue format=underlined run_command=/lm help /lm summon)
                &8 ⎣ &7TBD"""
            );

            { // Breadcrumb: Home -> /LM -> About
                final SubChapter home_lm_about = new SubChapter(home_lm, "about");
                home_lm_about.addPages(
                    """
                    &8&l » &7Command: [/lm about](color=blue format=underlined run_command=/lm help /lm about)
                    
                    &f&nDescription
                    &8 • &7View info about the installed version of the plugin.
                    
                    &f&nExample Usage
                    &8 • [/lm about](color=blue format=underlined run_command=/lm about)"""
                );
            }

            { // Breadcrumb: Home -> /LM -> Summon
                final SubChapter home_lm_summon = new SubChapter(home_lm, "summon");
                home_lm_summon.addPages(
                    """
                    &8&l » &7Command: [/lm summon](color=blue format=underlined run_command=/lm help /lm summon)
                    
                    &f&nDescription
                    &8 • &7Summons a levelled mob of chosen specifications, similar to Minecraft's [/summon](color=blue format=underlined run_command=/help summon) command.
                    
                    &f&nUsage
                    &8 • [/lm summon <entityType> ?...?](color=blue format=underlined run_command=/lm help /lm summon entitytype)
                    &8 • [/lm summon <customEntityId> ?...?](color=blue format=underlined run_command=/lm help /lm summon customentityid)""" //todo update the usage list
                );

                { // Breadcrumb: Home -> /LM -> Summon -> EntityType
                    final SubChapter home_lm_summon_entitytype =
                        new SubChapter(home_lm_summon, "entityType");
                    home_lm_summon_entitytype.addPages(
                        """
                        &8&l » &7Command: [/lm summon <entityType> ...](color=blue format=underlined run_command=/lm help /lm summon entitytype)
                                       
                        &f&nDescription
                        &8 • &7Summons a generic levelled mob of the specified entity type with chosen specifications.
                        
                        &f&nUsage
                        &8 • [/lm summon <entityType> ?...?](color=blue format=underlined run_command=/lm help /lm summon entitytype)
                        
                        &f&nExample Usage
                        &8 • [/lm summon Zombie ?...?](color=blue format=underlined run_command=/lm summon zombie ?...?)
                        """ //todo update the usage and example usage
                    );
                }
            }
        }

        return List.of(
            new CommandAPICommand("help")
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
                }),
            new CommandAPICommand("help")
                .withPermission("levelledmobs.command.levelledmobs.help")
                .withShortDescription("View a list of available LM commands and support links.")
                .withFullDescription("Sends the user a list of available LevelledMobs commands and " +
                    "also a list of URLs providing documentation and support.")
                .executes((sender, args) -> {
                    sender.sendMessage(
                        Message.formatMd(new String[]{
                            home.getFormattedPage(1)
                        })
                    );
                })
        );
    }

    public static Argument<ChapterAndPageNum> chapterArgument(final String nodeName) {

        return new CustomArgument<>(new GreedyStringArgument(nodeName), info -> {
            final String[] breadcrumbs = info.input().split(" ");
            Chapter currentChapter = HelpSystem.getHomeChapter();
            int pageNum = 1;

            for(final String breadcrumb : breadcrumbs) {
                final Optional<SubChapter> optSubChapter = currentChapter.getSubChapters().stream()
                    .filter(subChapter -> subChapter.getId().equalsIgnoreCase(breadcrumb))
                    .findFirst();

                if(optSubChapter.isPresent()) {
                    currentChapter = optSubChapter.get();
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

            if(pageNum < 1 || pageNum > currentChapter.getPages().size()) {
                throw new CustomArgumentException(
                    "Page '%s' does not exist in chapter '%s'".formatted(pageNum, currentChapter.getId())
                );
            }

            return new ChapterAndPageNum(currentChapter, pageNum);
        });
        /*.replaceSuggestions(ArgumentSuggestions.strings(info -> {
            // It seems that suggestions can't be made for greedy string arguments aside from their
            // first word before the next space character.
            // E.g., only `/LM` can be suggested in `/LM Help /LM Summon`, but not `Summon` or any
            // other theoretical arguments thereafter.
            // This isn't written in the docs but it appears that way.
            // Server owners should be using the interactive help interface, typing the path is
            // not a feasible choice for them anyways, so it doesn't matter that suggestions don't
            // work for the help command.
            // Docs:
            // <https://commandapi.jorel.dev/8.7.0/customarguments.html#example---message-builder-for-invalid-objective-argument>

            //final String[] breadcrumbs = info.currentInput().stripTrailing().split(" ");

            //final String[] breadcrumbs = new String[info.previousArgs().length];
            //for(int i = 0; i < info.previousArgs().length; i++)
            //    breadcrumbs[i] = info.previousArgs()[i].toString();

            final String[] splitInput = info.currentInput().stripTrailing().split(" ");
            Log.inf("SplitInput = " + Arrays.toString(splitInput));

            final String[] breadcrumbs = new String[Math.max(0, splitInput.length - 2)];
            System.arraycopy(splitInput, 2, breadcrumbs, 0, breadcrumbs.length);
            Log.inf("Breadcrumbs=" + Arrays.toString(breadcrumbs));
            Log.inf("Breadcrumbs.length=" + breadcrumbs.length);

            Chapter chapter = hs.getHomeChapter();

            for(int i = 0; i < breadcrumbs.length; i++) {
                final String breadcrumb = breadcrumbs[i];
                Log.inf("breadcrumb[i=" + i + "] = " + breadcrumb);

                final Optional<SubChapter> optSubChapter = chapter.getSubChapters().stream()
                    .filter(subChapter -> subChapter.getId().equalsIgnoreCase(breadcrumb))
                    .findFirst();

                if(optSubChapter.isPresent()) {
                    chapter = optSubChapter.get();
                    Log.inf("CurrentChapter=" + chapter.getId());
                } else {
                    try {
                        Integer.parseInt(breadcrumb);
                        // if it ends with a page number, that's all good
                        Log.inf("Ends with page number: no suggestions");
                        return new String[]{};
                    } catch(final NumberFormatException ignored) {
                        // they've entered something invalid, keep showing suggestions
                        Log.inf("Breadcrumb-Invalid");
                    }
                }
            }

            final List<String> suggestions = new LinkedList<>();

            // add sub-chapters to suggestions
            chapter.getSubChapters().forEach(subChapter -> suggestions.add(subChapter.getId()));
            Log.inf("Suggestions1=" + suggestions);

            // add page numbers to suggestions
            for(int pageNum = 1; pageNum <= chapter.getPages().size(); pageNum++) {
                suggestions.add(Integer.toString(pageNum));
            }
            Log.inf("Suggestions2=" + suggestions);

            final String[] suggestionsArr = suggestions.toArray(new String[0]);
            Log.inf("Suggestions3=" + Arrays.toString(suggestionsArr));

            return suggestionsArr;
        }));*/
    }

    public record ChapterAndPageNum(
        @Nonnull Chapter chapter,
        int pageNum
    ) {}

}
