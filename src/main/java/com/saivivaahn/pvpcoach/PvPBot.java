package com.saivivaahn.pvpcoach;

import java.util.EnumSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ChestMenu;

/** A configurable practice opponent. It is intentionally not a real player or cheat client. */
public final class PvPBot {
    public enum Mode { AGGRESSIVE, PASSIVE }

    private static CoachZombie bot;
    /** The live, player-accessible contents shown by a normal right-click. */
    private static SimpleContainer inventory = new SimpleContainer(9);
    private static boolean weakBot;
    private static int botLevel = 1;
    private static Mode mode = Mode.AGGRESSIVE;

    private PvPBot() { }

    public static void spawn(ServerPlayer player) {
        spawn(player, false);
    }

    /** Same loadout as the regular bot, but one heart for quick aim and combo practice. */
    public static void spawnWeak(ServerPlayer player) {
        spawn(player, true);
    }

    private static void spawn(ServerPlayer player, boolean weak) {
        remove();
        weakBot = weak;
        bot = new CoachZombie(player.level());
        bot.setPos(player.getX() + 2.0D, player.getY(), player.getZ() + 2.0D);
        bot.setPersistenceRequired();
        equipDefault();
        if (weak) bot.setHealth(1.0F);
        updateName();
        player.level().addFreshEntity(bot);
        applyMode(player);
    }

    public static boolean exists() { return bot != null && bot.isAlive(); }
    public static void remove() { if (bot != null) { bot.discard(); bot = null; } weakBot = false; inventory = new SimpleContainer(9); }
    public static void heal() { if (exists()) bot.setHealth(bot.getMaxHealth()); }
    public static int getBotLevel() { return botLevel; }
    public static Mode getMode() { return mode; }

    public static void setBotLevel(int level) {
        botLevel = Math.clamp(level, 1, 5);
        if (exists()) updateName();
    }

    public static void setMode(Mode newMode, ServerPlayer player) {
        mode = newMode;
        if (exists()) {
            updateName();
            applyMode(player);
        }
    }

    public static void toggleMode(ServerPlayer player) {
        setMode(mode == Mode.AGGRESSIVE ? Mode.PASSIVE : Mode.AGGRESSIVE, player);
        player.sendSystemMessage(Component.literal("§ePvP Bot mode: §f" + mode));
    }

    public static boolean setItem(String slot, String itemName) {
        if (!exists()) return false;
        EquipmentSlot equipmentSlot = switch (slot.toLowerCase()) {
            case "mainhand", "hand" -> EquipmentSlot.MAINHAND;
            case "offhand" -> EquipmentSlot.OFFHAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> null;
        };
        ItemStack stack = switch (itemName.toLowerCase()) {
            case "empty", "clear" -> ItemStack.EMPTY;
            case "wooden_sword" -> new ItemStack(Items.WOODEN_SWORD);
            case "iron_sword" -> new ItemStack(Items.IRON_SWORD);
            case "diamond_sword" -> new ItemStack(Items.DIAMOND_SWORD);
            case "netherite_sword" -> new ItemStack(Items.NETHERITE_SWORD);
            case "iron_axe" -> new ItemStack(Items.IRON_AXE);
            case "diamond_axe" -> new ItemStack(Items.DIAMOND_AXE);
            case "netherite_axe" -> new ItemStack(Items.NETHERITE_AXE);
            case "shield" -> new ItemStack(Items.SHIELD);
            case "totem" -> new ItemStack(Items.TOTEM_OF_UNDYING);
            case "iron_helmet" -> new ItemStack(Items.IRON_HELMET);
            case "diamond_helmet" -> new ItemStack(Items.DIAMOND_HELMET);
            case "netherite_helmet" -> new ItemStack(Items.NETHERITE_HELMET);
            case "iron_chestplate" -> new ItemStack(Items.IRON_CHESTPLATE);
            case "diamond_chestplate" -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            case "netherite_chestplate" -> new ItemStack(Items.NETHERITE_CHESTPLATE);
            case "iron_leggings" -> new ItemStack(Items.IRON_LEGGINGS);
            case "diamond_leggings" -> new ItemStack(Items.DIAMOND_LEGGINGS);
            case "netherite_leggings" -> new ItemStack(Items.NETHERITE_LEGGINGS);
            case "iron_boots" -> new ItemStack(Items.IRON_BOOTS);
            case "diamond_boots" -> new ItemStack(Items.DIAMOND_BOOTS);
            case "netherite_boots" -> new ItemStack(Items.NETHERITE_BOOTS);
            default -> null;
        };
        if (equipmentSlot == null || stack == null) return false;
        setEquipment(equipmentSlot, stack);
        return true;
    }

    /**
     * Normal right-click opens the bot's loot/loadout. Sneak-right-click equips the held item,
     * so players can inspect and take the bot's gear without accidentally giving it an item.
     */
    public static boolean interact(ServerPlayer player, InteractionHand hand, Entity clicked) {
        if (!exists() || clicked != bot) return false;
        if (!player.isShiftKeyDown()) {
            player.openMenu(new SimpleMenuProvider((containerId, playerInventory, ignored) ->
                    new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, containerId, playerInventory, inventory, 1) {
                        @Override
                        public void removed(net.minecraft.world.entity.player.Player closingPlayer) {
                            super.removed(closingPlayer);
                            syncEquipmentFromInventory();
                        }
                    }, Component.literal("PvP Bot Inventory")));
            return true;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return false;
        setEquipment(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, held.copy());
        player.setItemInHand(hand, ItemStack.EMPTY);
        player.sendSystemMessage(Component.literal("\u00A7aPvP Bot equipped your " + held.getHoverName().getString() + ". Sneak-right-click to equip; right-click to open its inventory."));
        return true;
    }

    private static void equipDefault() {
        inventory = new SimpleContainer(9);
        setItemDirect(EquipmentSlot.MAINHAND, Items.NETHERITE_SWORD);
        setItemDirect(EquipmentSlot.OFFHAND, Items.SHIELD);
        setItemDirect(EquipmentSlot.HEAD, Items.NETHERITE_HELMET);
        setItemDirect(EquipmentSlot.CHEST, Items.NETHERITE_CHESTPLATE);
        setItemDirect(EquipmentSlot.LEGS, Items.NETHERITE_LEGGINGS);
        setItemDirect(EquipmentSlot.FEET, Items.NETHERITE_BOOTS);
    }

    private static void setItemDirect(EquipmentSlot slot, net.minecraft.world.item.Item item) { setEquipment(slot, new ItemStack(item)); }
    private static void setEquipment(EquipmentSlot slot, ItemStack stack) {
        bot.setItemSlot(slot, stack);
        bot.setDropChance(slot, 1.0F);
        inventory.setItem(inventorySlot(slot), stack.copy());
    }
    private static int inventorySlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> 0;
            case OFFHAND -> 1;
            case HEAD -> 2;
            case CHEST -> 3;
            case LEGS -> 4;
            case FEET -> 5;
            default -> throw new IllegalArgumentException("Unsupported bot equipment slot: " + slot);
        };
    }
    private static void syncEquipmentFromInventory() {
        if (!exists()) return;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            bot.setItemSlot(slot, inventory.getItem(inventorySlot(slot)).copy());
            bot.setDropChance(slot, 1.0F);
        }
    }
    private static void updateName() {
        String name = weakBot ? "§cOne-Shot Bot §7[§c1 Hit§7]" : "§bPvP Bot §7[" + mode.name().charAt(0) + mode.name().substring(1).toLowerCase() + " §eLvl " + botLevel + "§7]";
        bot.setCustomName(Component.literal(name));
        bot.setCustomNameVisible(true);
    }
    private static void applyMode(ServerPlayer player) { bot.setTarget(mode == Mode.AGGRESSIVE ? player : null); }

    private static final class CoachZombie extends Zombie {
        CoachZombie(ServerLevel level) { super(EntityType.ZOMBIE, level); }

        @Override
        protected void registerGoals() {
            goalSelector.removeAllGoals(goal -> true);
            targetSelector.removeAllGoals(goal -> true);
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(1, new HumanCombatGoal(this));
        }
    }

    private static final class HumanCombatGoal extends Goal {
        private final CoachZombie fighter;
        private int attackTicks;
        private int strafeTicks;
        private int retreatTicks;
        private boolean strafeRight;

        HumanCombatGoal(CoachZombie fighter) { this.fighter = fighter; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return mode == Mode.AGGRESSIVE && fighter.getTarget() instanceof ServerPlayer player && player.isAlive(); }

        @Override public void tick() {
            ServerPlayer target = (ServerPlayer) fighter.getTarget();
            double distance = fighter.distanceToSqr(target);
            fighter.getLookControl().setLookAt(target, 35.0F + botLevel * 8.0F, 30.0F);
            attackTicks++;
            if (++strafeTicks >= Math.max(6, 28 - botLevel * 4)) { strafeRight = fighter.getRandom().nextBoolean(); strafeTicks = 0; }
            boolean needsSpace = fighter.getHealth() < fighter.getMaxHealth() * (0.18F + botLevel * 0.04F);
            if (needsSpace) retreatTicks = 14 + botLevel * 3;
            if (retreatTicks > 0) retreatTicks--;
            float forward = distance > 10.0D ? 0.9F : distance < 4.0D ? -0.20F : 0.35F;
            if (retreatTicks > 0) forward = -0.65F;
            fighter.setSprinting(botLevel >= 2 && distance > 3.5D);
            float strafe = 0.18F + botLevel * 0.055F;
            fighter.getMoveControl().strafe(forward, strafeRight ? strafe : -strafe);
            // Preserve the weapon the player configured. Higher levels use more frequent movement mix-ups.
            if (botLevel >= 4 && distance <= 8.0D && fighter.onGround() && fighter.getRandom().nextFloat() < 0.025F * botLevel) fighter.jumpFromGround();
            int interval = Math.max(7, 19 - botLevel * 2);
            if (retreatTicks == 0 && distance <= 9.0D && attackTicks >= interval) {
                // Releasing sprint just before a hit imitates a player sprint-reset for knockback.
                fighter.setSprinting(false);
                fighter.swing(InteractionHand.MAIN_HAND);
                fighter.doHurtTarget((ServerLevel) fighter.level(), target);
                attackTicks = 0;
            }
        }
    }
}
