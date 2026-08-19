package com.saivivaahn.pvpcoach;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FightHistoryManager {

    public record FightRecord(
            int id,
            long timestampMs,
            long durationSeconds,
            float damageDealt,
            float damageTaken,
            int hits,
            int attacks,
            double accuracy,
            int botLevel,
            String feedback
    ) {}

    private static final List<FightRecord> history = new ArrayList<>();
    private static int nextId = 1;

    public static FightRecord recordFight(long durationSecs, float damageDealt, float damageTaken, int hits, int attacks, double accuracy, int botLevel) {
        String feedback = generateFeedback(damageDealt, damageTaken, accuracy, durationSecs);

        FightRecord record = new FightRecord(
                nextId++,
                System.currentTimeMillis(),
                durationSecs,
                damageDealt,
                damageTaken,
                hits,
                attacks,
                accuracy,
                botLevel,
                feedback
        );

        history.add(record);
        return record;
    }

    private static String generateFeedback(float dealt, float taken, double accuracy, long time) {
        if (accuracy < 40.0) {
            return "§cWork on crosshair placement! Accuracy was under 40%.";
        } else if (taken > dealt * 1.5) {
            return "§eHeavy damage taken! Focus on spacing and sprint-resets.";
        } else if (dealt > taken * 2.0 && accuracy > 70.0) {
            return "§aDominant fight! Excellent spacing and high combo accuracy.";
        } else if (time > 60) {
            return "§eLong fight! Be more aggressive with hit-and-run trades.";
        } else {
            return "§bSolid trading! Keep up movement speed while attacking.";
        }
    }

    public static List<FightRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public static FightRecord getRecord(int id) {
        return history.stream().filter(r -> r.id() == id).findFirst().orElse(null);
    }
}