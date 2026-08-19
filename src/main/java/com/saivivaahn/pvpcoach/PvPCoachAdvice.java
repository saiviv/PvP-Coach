package com.saivivaahn.pvpcoach;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.chat.Component;

/** Small, offline coaching responses for use from the PvP Coach commands. */
public final class PvPCoachAdvice {
    private static final List<String> GENERAL_TIPS = List.of(
            "Keep your crosshair at opponent chest height while you move; it makes follow-up hits much easier.",
            "Do not hold sprint through every hit. Reset sprint between hits to get stronger knockback.",
            "Strafe unpredictably. Change direction after a hit instead of moving in one long, easy-to-track line.",
            "Fight at a distance where your next hit is ready. Rushing into range with no attack cooldown gives away free hits.",
            "Use your hotbar deliberately: sword, axe, food, blocks, and healing should always be in familiar slots.",
            "After taking knockback, keep your aim on the opponent and use the moment to reset your sprint or eat safely.");

    private PvPCoachAdvice() { }

    public static Component randomTip() {
        String tip = GENERAL_TIPS.get(ThreadLocalRandom.current().nextInt(GENERAL_TIPS.size()));
        return coach("Tip: " + tip);
    }

    public static Component howTo(String topic) {
        return switch (topic.toLowerCase()) {
            case "aim" -> coach("Aim: keep your crosshair around chest height, make small mouse corrections, and practice tracking while strafing. Do not flick wildly after each hit.");
            case "combo", "combos" -> coach("Combos: land a hit, briefly release and re-press sprint, then keep the opponent at the edge of your reach. Strafe and track them instead of running straight in.");
            case "crit", "crits", "critical" -> coach("Critical hits: jump only when you can hit while falling. Random jumping is easy to punish, so use a crit after creating space or predicting their swing.");
            case "shield", "shields" -> coach("Shields: block an expected hit, then counterattack. If an opponent turtles behind a shield, switch to an axe, hit once, and return to your sword when it is disabled.");
            case "healing", "heal", "food" -> coach("Healing: make distance first. Use blocks, knockback, or a shield to create a safe second, then eat or use your healing item; do not heal in sword range.");
            case "movement", "strafe", "strafing" -> coach("Movement: alternate left and right strafes with short forward bursts. Avoid a fixed rhythm, because good players will aim where you are about to move.");
            case "practice", "training" -> coach("Practice: start the bot at level 1, focus on one skill for a few fights, then increase its level. Try aim first, then sprint resets, then shield and axe timing.");
            default -> coach("I can help with aim, combo, crit, shield, healing, movement, or practice. Try /pvpcoach howto aim.");
        };
    }

    public static Component help() {
        return coach("Ask for a tip with /pvpcoach tip, or use /pvpcoach howto <aim|combo|crit|shield|healing|movement|practice>.");
    }

    public static Component commands() {
        return Component.literal("§bPvP Coach commands and keys\n"
                + "§eKeys: §fRight Shift §7= HUD on/off\n"
                + "§fCtrl+R §7= Combat Training on/off\n"
                + "§fCtrl+Shift+R §7= Training level 1-5\n"
                + "§fB §7= no bot action; §fShift+B §7= normal, one-shot, normal...\n"
                + "§fShift+P §7= bot aggressive/passive\n"
                + "§fRight-click bot §7= inventory; sneak-right-click §7= give item\n"
                + "§eCommands: §f/pvpcoach help §7| §f/pvpcoach tip\n"
                + "§f/pvpcoach howto <topic> §7| §f/pvpcoach ask <question>\n"
                + "§f/pvpcoach forget §7| §f/pvpcoach bot <spawn|remove|heal>\n"
                + "§f/pvpcoach bot level <1-5> §7| §f/pvpcoach bot <aggressive|passive>\n"
                + "§f/pvpcoach bot item <slot> <item> §7| §f/pvpcoach hud position <x> <y>\n"
                + "§f/pvpcoach hud toggle <fps|accuracy>\n"
                + "§7Training is client-side; bot keys need PvP Coach on the server.");
    }

    private static Component coach(String message) {
        return Component.literal("\u00A7bPvP Coach \u00A77> \u00A7f" + message);
    }
}
