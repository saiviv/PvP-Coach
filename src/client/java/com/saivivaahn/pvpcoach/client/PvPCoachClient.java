package com.saivivaahn.pvpcoach.client;

import com.saivivaahn.pvpcoach.CoachSettings;
import com.saivivaahn.pvpcoach.BotSpawnPayload;
import com.saivivaahn.pvpcoach.BotModePayload;
import com.saivivaahn.pvpcoach.FightHistoryManager;
import com.saivivaahn.pvpcoach.PvPBot;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class PvPCoachClient implements ClientModInitializer {
    public static float damageDealt = 0.0F;
    public static float damageTaken = 0.0F;
    public static int hits = 0;
    public static int attacks = 0;
    private static int groundHits = 0;
    private static int jumpCount = 0;
    private static int trainingHits;
    private static int trainingAttempts;
    private static boolean jumpWasDown;
    private static final ArrayDeque<Long> recentAttacks = new ArrayDeque<>();
    private static String targetName = "None";
    private static int targetHits;
    private static int targetAttacks;
    private static int targetEntityId = Integer.MIN_VALUE;
    private static long lastTargetTime;
    private static int outlinedTargetId = Integer.MIN_VALUE;
    private static String rangeStatus = "Aim at a player or mob to highlight it";
    private static double targetDistance = -1.0D;
    private static long targetAcquiredAt;
    private static long lastReactionMillis;

    private static KeyMapping toggleHudKey;
    private static KeyMapping aimCoachKey;
    private static KeyMapping spawnBotKey;
    private static KeyMapping botModeKey;
    private static boolean weakBotSelection;
    private static boolean trainingEnabled;
    private static int trainingLevel = 1;
    private static boolean showingReport = false;
    private static long reportStartTime = 0L;
    private static long fightStartTime = 0L;
    private static final List<String> postMatchTips = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        fightStartTime = System.currentTimeMillis();
        ClientCoachCommands.register();

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvpcoach.toggle_hud",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.pvpcoach"
        ));
        aimCoachKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvpcoach.combat_training",
                GLFW.GLFW_KEY_R,
                "category.pvpcoach"
        ));
        spawnBotKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvpcoach.spawn_bot",
                GLFW.GLFW_KEY_B,
                "category.pvpcoach"
        ));
        botModeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvpcoach.toggle_bot_mode", GLFW.GLFW_KEY_P, "category.pvpcoach"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            long now = System.currentTimeMillis();
            while (!recentAttacks.isEmpty() && now - recentAttacks.peekFirst() > 1000L) {
                recentAttacks.removeFirst();
            }

            boolean jumpDown = client.options.keyJump.isDown();
            if (jumpDown && !jumpWasDown) jumpCount++;
            jumpWasDown = jumpDown;
            updateTargetOutline(client);

            while (toggleHudKey.consumeClick()) {
                CoachSettings.hudEnabled = !CoachSettings.hudEnabled;
            }
            while (aimCoachKey.consumeClick()) {
                if (!controlDown(client)) continue;
                if (client.options.keyShift.isDown()) {
                    trainingLevel = trainingLevel == 5 ? 1 : trainingLevel + 1;
                    if (client.player != null) client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§eCombat Training level " + trainingLevel + "/5"), true);
                } else {
                    trainingEnabled = !trainingEnabled;
                    if (client.player != null) client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§eCombat Training " + (trainingEnabled ? "ON" : "OFF") + " (Shift+R changes level)"), true);
                }
            }
            while (spawnBotKey.consumeClick()) {
                if (!client.options.keyShift.isDown()) {
                    if (client.player != null) client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§eHold Shift+B to spawn a bot."), true);
                    continue;
                }
                boolean weak = weakBotSelection;
                if (ClientPlayNetworking.canSend(BotSpawnPayload.TYPE)) {
                    ClientPlayNetworking.send(new BotSpawnPayload(weak));
                    weakBotSelection = !weakBotSelection;
                    if (client.player != null) {
                        String botType = weak ? "One-Shot Bot (1 hit)" : "Normal Bot";
                        client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§eSpawned: §f" + botType), true);
                    }
                } else if (client.player != null) {
                    client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cThe server needs PvP Coach installed to spawn practice bots."), true);
                }
            }
            while (botModeKey.consumeClick()) {
                if (!client.options.keyShift.isDown()) continue;
                if (ClientPlayNetworking.canSend(BotModePayload.TYPE)) {
                    ClientPlayNetworking.send(new BotModePayload());
                } else if (client.player != null) {
                    client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cThe server needs PvP Coach installed for bot controls."), true);
                }
            }

            if (client.options.keyAttack.consumeClick()) {
                attacks++;
                if (trainingEnabled) trainingAttempts++;
                if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) client.hitResult;
                    if (entityHit.getEntity() != null) {
                        updateTarget(entityHit.getEntity(), now);
                        if (targetAcquiredAt > 0L && entityHit.getEntity().getId() == outlinedTargetId) {
                            lastReactionMillis = now - targetAcquiredAt;
                            targetAcquiredAt = 0L;
                        }
                        targetAttacks++;
                        hits++;
                        if (trainingEnabled) {
                            trainingHits++;
                            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§aTraining hit  §f" + trainingHits + "/" + trainingAttempts), true);
                        }
                        targetHits++;
                        damageDealt += 7.0F;
                        // A grounded melee hit cannot be a vanilla critical hit.
                        if (client.player.onGround()) groundHits++;
                    }
                } else {
                    if (now - lastTargetTime < 2500L) {
                        // A swing shortly after engaging the same target is an estimated miss for this fight.
                        targetAttacks++;
                    }
                    if (trainingEnabled) client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cTraining miss  §f" + trainingHits + "/" + trainingAttempts), true);
                }
            }
        });

        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || !CoachSettings.hudEnabled) return;

            int x = CoachSettings.hudX;
            int y = CoachSettings.hudY;

            double accuracy = attacks > 0 ? ((double) hits / attacks) * 100.0 : 0.0;
            int cps = recentAttacks.size();
            int metricsY = y;

            // The top row is deliberately just performance and clicking: both are player metrics.
            if (CoachSettings.showFps) {
                graphics.drawString(client.font, "FPS: " + client.getFps(), x, metricsY, CoachSettings.textColor);
            }
            graphics.drawString(client.font, "CPS: " + cps, x + 82, metricsY, CoachSettings.textColor);
            metricsY += 16;

            int aimColor = trainingEnabled ? 0xFF55FF55 : 0xFFAAAAAA;
            graphics.fill(x, metricsY, x + 142, metricsY + 12, 0xB0101010);
            graphics.drawString(client.font, "TRAINING " + (trainingEnabled ? "ON" : "OFF") + "  LVL " + trainingLevel + "/5", x + 3, metricsY + 2, aimColor);
            metricsY += 16;

            // Keep movement keys directly under FPS/CPS instead of in an unrelated corner.
            if (CoachSettings.showKeystrokes) {
                drawKeystrokes(graphics, client, x, metricsY);
                metricsY += 62;
            }

            int line = 0;
            graphics.drawString(client.font, "Hits: " + hits + " / Attacks: " + attacks, x, metricsY + (line++ * 12), CoachSettings.textColor);
            graphics.drawString(client.font, "Dealt: " + String.format("%.1f", damageDealt), x, metricsY + (line++ * 12), 0x55FF55);
            graphics.drawString(client.font, "Taken: " + String.format("%.1f", damageTaken), x, metricsY + (line++ * 12), 0xFF5555);

            if (CoachSettings.showAccuracy) {
                graphics.drawString(client.font, "Accuracy: " + String.format("%.1f", accuracy) + "%", x, metricsY + (line++ * 12), 0xFFFF55);
            }
            graphics.drawString(client.font, "Jumps: " + jumpCount + " | Ground hits: " + groundHits, x, metricsY + (line++ * 12), 0xFFAA00);
            if (trainingEnabled) {
                graphics.drawString(client.font, trainingStatus(accuracy), x, metricsY + (line++ * 12), 0xFF55FF55);
            }
            graphics.drawString(client.font, coachingLine(accuracy), x, metricsY + (line++ * 12), 0x55FFFF);

            if (showingReport) {
                graphics.drawString(client.font, "=== POST-MATCH REPORT ===", x, metricsY + (line++ * 12), 0xFFAA00);
                for (String tip : postMatchTips) {
                    graphics.drawString(client.font, tip, x, metricsY + (line++ * 12), 0xFFFFFF);
                }

                if (System.currentTimeMillis() - reportStartTime > 8000L) {
                    showingReport = false;
                    resetStats();
                }
            }

            drawTargetOverlay(graphics, client);

        });
    }

    private static String movementKeys(Minecraft client) {
        StringBuilder keys = new StringBuilder();
        if (client.options.keyUp.isDown()) keys.append("W ");
        if (client.options.keyLeft.isDown()) keys.append("A ");
        if (client.options.keyDown.isDown()) keys.append("S ");
        if (client.options.keyRight.isDown()) keys.append("D ");
        if (client.options.keyJump.isDown()) keys.append("JUMP ");
        if (client.options.keySprint.isDown()) keys.append("SPRINT");
        return keys.isEmpty() ? "idle" : keys.toString().trim();
    }

    private static boolean controlDown(Minecraft client) {
        long window = client.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private static void updateTarget(Entity entity, long now) {
        if (entity.getId() != targetEntityId || now - lastTargetTime > 2500L) {
            targetEntityId = entity.getId();
            targetName = entity.getName().getString();
            targetHits = 0;
            targetAttacks = 0;
        }
        lastTargetTime = now;
    }

    /** Highlights the exact living player or mob under the player's cursor in bright red. */
    private static void updateTargetOutline(Minecraft client) {
        if (client.level == null || client.player == null) return;
        PlayerTeam team = client.level.getScoreboard().getPlayerTeam("pvpcoach_hit_range");
        if (team == null) {
            team = client.level.getScoreboard().addPlayerTeam("pvpcoach_hit_range");
            team.setColor(ChatFormatting.RED);
        }
        Entity target = client.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
        int newTargetId = Integer.MIN_VALUE;
        if (target instanceof LivingEntity living && living.isAlive() && target != client.player) {
            newTargetId = target.getId();
            living.setGlowingTag(true);
            client.level.getScoreboard().addPlayerToTeam(living.getScoreboardName(), team);
            double distance = client.player.distanceTo(living);
            targetDistance = distance;
            rangeStatus = "TARGET: " + living.getName().getString() + " (" + String.format("%.1f", distance) + " blocks)";
        } else {
            rangeStatus = "Aim at a player or mob to highlight it";
            targetDistance = -1.0D;
        }
        if (outlinedTargetId != Integer.MIN_VALUE && outlinedTargetId != newTargetId) {
            Entity old = client.level.getEntity(outlinedTargetId);
            if (old != null) old.setGlowingTag(false);
        }
        if (newTargetId != outlinedTargetId) targetAcquiredAt = newTargetId == Integer.MIN_VALUE ? 0L : System.currentTimeMillis();
        outlinedTargetId = newTargetId;
    }

    private static void drawTargetOverlay(net.minecraft.client.gui.GuiGraphics graphics, Minecraft client) {
        String text = targetDistance < 0.0D ? "TARGET: none" : rangeStatus;
        int color = targetDistance >= 0.0D && targetDistance <= 3.2D ? 0xFFFF3030 : 0xFFFF7070;
        int width = client.font.width(text) + 12;
        int right = client.getWindow().getGuiScaledWidth() - 8;
        int left = right - width;
        graphics.fill(left, 8, right, 23, 0xB0101010);
        graphics.fill(left, 8, right, 9, 0xFFFF2020);
        graphics.drawString(client.font, text, left + 6, 12, color, false);
    }

    /** Compact player-only key overlay with a high-contrast red pressed outline. */
    private static void drawKeystrokes(net.minecraft.client.gui.GuiGraphics graphics, Minecraft client, int x, int y) {
        drawKey(graphics, client, x + 20, y, "W", client.options.keyUp.isDown());
        drawKey(graphics, client, x, y + 20, "A", client.options.keyLeft.isDown());
        drawKey(graphics, client, x + 20, y + 20, "S", client.options.keyDown.isDown());
        drawKey(graphics, client, x + 40, y + 20, "D", client.options.keyRight.isDown());
        drawKey(graphics, client, x, y + 40, "JUMP", client.options.keyJump.isDown());
    }

    private static void drawKey(net.minecraft.client.gui.GuiGraphics graphics, Minecraft client, int x, int y, String text, boolean pressed) {
        int width = text.equals("JUMP") ? 60 : 18;
        int fill = pressed ? 0xB0350000 : 0x80303030;
        graphics.fill(x, y, x + width, y + 18, fill);
        int outline = pressed ? 0xFFFF2020 : 0xFF707070;
        graphics.fill(x, y, x + width, y + 1, outline);
        graphics.fill(x, y + 17, x + width, y + 18, outline);
        graphics.fill(x, y, x + 1, y + 18, outline);
        graphics.fill(x + width - 1, y, x + width, y + 18, outline);
        graphics.drawString(client.font, text, x + (width - client.font.width(text)) / 2, y + 5, 0xFFFFFFFF, false);
    }

    private static String coachingLine(double accuracy) {
        if (attacks >= 8 && accuracy < 45.0) return "Coach: slow down and keep your crosshair at chest height.";
        if (groundHits >= 5) return "Coach: you have ground hits; create space, then hit while falling for crit practice.";
        if (recentAttacks.size() >= 8) return "Coach: high CPS is not enough—wait for reach and reset sprint.";
        return "Coach: strafe unpredictably and reset sprint after a hit.";
    }

    private static String trainingStatus(double accuracy) {
        int targetAccuracy = 35 + trainingLevel * 10;
        int reactionGoal = 1100 - trainingLevel * 150;
        double drillAccuracy = trainingAttempts == 0 ? 0.0 : trainingHits * 100.0 / trainingAttempts;
        String accuracyMark = drillAccuracy >= targetAccuracy ? "✓" : "…";
        String reaction = lastReactionMillis == 0L
                ? "Reaction goal: " + reactionGoal + " ms"
                : "Reaction: " + lastReactionMillis + " / " + reactionGoal + " ms";
        return accuracyMark + " Drill: " + trainingHits + "/" + trainingAttempts + " (" + String.format("%.0f", drillAccuracy) + "% / " + targetAccuracy + "%) | " + reaction;
    }

    /** Called from the raw mouse callback so CPS is not lost when Minecraft consumes the attack keybind. */
    public static void registerLeftMouseClick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.screen == null) {
            recentAttacks.addLast(System.currentTimeMillis());
        }
    }

    public static void triggerPostMatchReport() {
        if (showingReport) return;
        showingReport = true;
        reportStartTime = System.currentTimeMillis();

        long duration = Math.max(1, (System.currentTimeMillis() - fightStartTime) / 1000L);
        double accuracy = attacks > 0 ? ((double) hits / attacks) * 100.0 : 0.0;

        postMatchTips.clear();

        if (accuracy < 45.0) {
            postMatchTips.add("§c- Aim Precision: Crosshair stayed off target.");
        } else {
            postMatchTips.add("§a- Aim Precision: High hit consistency!");
        }

        if (damageTaken > damageDealt) {
            postMatchTips.add("§e- Movement: Practice sprint resets & spacing.");
        } else {
            postMatchTips.add("§a- Spacing: Maintained reach advantage.");
        }

        FightHistoryManager.recordFight(duration, damageDealt, damageTaken, hits, attacks, accuracy, PvPBot.getBotLevel());
    }

    public static void resetStats() {
        damageDealt = 0.0F;
        damageTaken = 0.0F;
        hits = 0;
        attacks = 0;
        groundHits = 0;
        jumpCount = 0;
        trainingHits = 0;
        trainingAttempts = 0;
        jumpWasDown = false;
        recentAttacks.clear();
        targetName = "None";
        targetHits = 0;
        targetAttacks = 0;
        targetEntityId = Integer.MIN_VALUE;
        lastTargetTime = 0L;
        outlinedTargetId = Integer.MIN_VALUE;
        rangeStatus = "Aim at a player or mob to highlight it";
        targetDistance = -1.0D;
        targetAcquiredAt = 0L;
        lastReactionMillis = 0L;
        showingReport = false;
        postMatchTips.clear();
        fightStartTime = System.currentTimeMillis();
    }
}
