package data.scripts.console.commands;

import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class fer_StressTest implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (context != CommandContext.COMBAT_SIMULATION) {
            Console.showMessage("Command only usable in simulation");
            return CommandResult.ERROR;
        }
        if (args.trim().isEmpty()) {
            Console.showMessage("Specify variant");
            return CommandResult.BAD_SYNTAX;
        }

        String[] ids = args.split(" ");
        if (ids.length != 3) {
            Console.showMessage("Syntax error");
            return CommandResult.BAD_SYNTAX;
        }

        String variant = ids[0];
        int rows = Integer.parseInt(ids[1]);
        int cols = Integer.parseInt(ids[2]);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Vector2f loc = new Vector2f(x * 200f, y * 200f);
                CombatUtils.spawnShipOrWingDirectly(variant, FleetMemberType.SHIP, FleetSide.PLAYER, 0.7f, loc, 90f).resetDefaultAI();
            }
        }

        return CommandResult.SUCCESS;
    }
}
