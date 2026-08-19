package com.saivivaahn.pvpcoach;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;

/** Keeps a small in-memory conversation context for each player. */
public final class CoachConversation {
    private static final int MAX_MESSAGES = 12;
    private static final Map<UUID, ArrayDeque<String>> history = new HashMap<>();

    private CoachConversation() { }

    public static Component ask(UUID playerId, String question) {
        ArrayDeque<String> messages = history.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        messages.addLast(question);
        while (messages.size() > MAX_MESSAGES) messages.removeFirst();
        String q = question.toLowerCase(Locale.ROOT);
        if (q.contains("aim") || q.contains("accuracy")) return PvPCoachAdvice.howTo("aim");
        if (q.contains("combo") || q.contains("sprint")) return PvPCoachAdvice.howTo("combo");
        if (q.contains("shield") || q.contains("axe")) return PvPCoachAdvice.howTo("shield");
        if (q.contains("heal") || q.contains("food") || q.contains("totem")) return PvPCoachAdvice.howTo("healing");
        if (q.contains("move") || q.contains("wasd") || q.contains("strafe")) return PvPCoachAdvice.howTo("movement");
        if (q.contains("jump") || q.contains("crit")) return PvPCoachAdvice.howTo("crit");
        if (q.contains("difficulty") || q.contains("level") || q.contains("bot")) return Component.literal("\u00A7bPvP Coach \u00A77> \u00A7fUse levels 1-5. Level 1 is forgiving; level 5 tracks, strafes, jump-crits, and punishes shields much more consistently.");
        return Component.literal("\u00A7bPvP Coach \u00A77> \u00A7fI remembered your question. I can coach aim, combos, movement, shields, healing, crits, bot difficulty, and gear. Try a more specific PvP question.");
    }

    public static void clear(UUID playerId) { history.remove(playerId); }
}
