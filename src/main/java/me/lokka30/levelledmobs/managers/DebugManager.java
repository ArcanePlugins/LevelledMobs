package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.misc.DebugType;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class DebugManager {
    public DebugManager(){
        this.filterDebugTypes = new HashSet<>();
        this.filterEntityTypes = new HashSet<>();
        this.filterRuleNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.filterPlayerNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    public boolean isEnabled;
    public final Set<DebugType> filterDebugTypes;
    public final Set<EntityType> filterEntityTypes;
    public final Set<String> filterRuleNames;
    public final Set<String> filterPlayerNames;
    public EvaluationTypes evaluationType = EvaluationTypes.FAILURE;
    public OutputTypes outputType = OutputTypes.CONSOLE;
    public Integer maxPlayerDistance = 50;
    public Integer minYLevel;
    public Integer maxYLevel;
    public Long disableAfter;
    public String disableAfterStr;

    public String getDebugStatus(){
        final StringBuilder sb = new StringBuilder("Debug status: ");
        if (isEnabled)
            sb.append("Enabled");
        else
            sb.append("Disabled");

        if (!hasFiltering()) return sb.toString();
        sb.append(", filter options:");

        if (!filterDebugTypes.isEmpty()){
            sb.append("\nDebug types: ");
            sb.append(filterDebugTypes);
        }

        if (!filterEntityTypes.isEmpty()){
            sb.append("\nEntity types: ");
            sb.append(filterEntityTypes);
        }

        if (!filterRuleNames.isEmpty()){
            sb.append("\nRule names: ");
            sb.append(filterRuleNames);
        }

        if (!filterPlayerNames.isEmpty()){
            sb.append("\nPlayer names: ");
            sb.append(filterPlayerNames);
        }

        if (evaluationType != EvaluationTypes.BOTH){
            sb.append("\nEvaluation type: ");
            sb.append(evaluationType);
        }

        if (maxPlayerDistance != null){
            sb.append("\nMax player distance: ");
            sb.append(maxPlayerDistance);
        }

        if (minYLevel != null){
            sb.append("\nMin y level: ");
            sb.append(minYLevel);
        }

        if (maxYLevel != null){
            if (minYLevel != null)
                sb.append(", ");
            else
                sb.append("\n");
            sb.append("Max y level: ");
            sb.append(maxYLevel);
        }

        if (outputType != OutputTypes.CONSOLE){
            sb.append("\nOutput to: ");
            sb.append(outputType);
        }

        if (disableAfter != null){
            sb.append("\nDisable after: ");
            sb.append(disableAfterStr);
        }

        return sb.toString();
    }

    private boolean hasFiltering(){
        return (
                !filterDebugTypes.isEmpty() ||
                !filterEntityTypes.isEmpty() ||
                !filterRuleNames.isEmpty() ||
                !filterPlayerNames.isEmpty() ||
                evaluationType != EvaluationTypes.BOTH ||
                outputType != OutputTypes.CONSOLE ||
                maxPlayerDistance != null ||
                minYLevel != null ||
                maxYLevel != null ||
                disableAfter != null
        );
    }

    public enum EvaluationTypes{
        FAILURE, PASS, BOTH
    }

    public enum OutputTypes{
        CONSOLE, CHAT, BOTH
    }
}
