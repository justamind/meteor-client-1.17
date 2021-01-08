/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.player.Chat;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

// Created by Eureka

public class AutoAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("How far away the target can be to be affected.")
            .defaultValue(4)
            .min(0)
            .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
            .name("height")
            .description("How high to place the anvils.")
            .defaultValue(5)
            .min(0)
            .max(10)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> placeButton = sgGeneral.add(new BoolSetting.Builder()
            .name("place-button")
            .description("Auto places a button beneath the target.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> toggleOnBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-on-break")
            .description("Toggles off when the target's helmet slot is empty.")
            .defaultValue(false)
            .build()
    );

    public AutoAnvil() {
        super(Category.Combat, "auto-anvil", "Automatically places anvils above players to destroy helmets.");
    }

    private PlayerEntity target = null;

    @Override
    public void onDeactivate() {
        target = null;
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {

        if (target != null) {
            if (mc.player.distanceTo(target) > range.get() || !target.isAlive()) target = null;
            if (mc.player.currentScreenHandler instanceof AnvilScreenHandler) mc.player.closeScreen();
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !FriendManager.INSTANCE.attack(player) || !player.isAlive() || mc.player.distanceTo(player) > range.get()) continue;

            if (target == null) target = player;
            else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) target = player;
        }

        if (target == null) {
            for (FakePlayerEntity player : FakePlayer.players.keySet()) {
                if (!FriendManager.INSTANCE.attack(player) || !player.isAlive() || mc.player.distanceTo(player) > range.get()) continue;

                if (target == null) target = player;
                else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) target = player;
            }
        }

        if (isActive() && toggleOnBreak.get() && target != null && target.inventory.getArmorStack(3).isEmpty()) {
            Chat.info(this, "Target head slot is empty… Disabling.");
            toggle();
        }

        int anvilSlot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();

            if (item == Items.ANVIL || item == Items.CHIPPED_ANVIL || item == Items.DAMAGED_ANVIL) {
                anvilSlot = i;
                break;
            }
        }
        if (anvilSlot == -1) return;

        int buttonSlot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();

            if (item == Items.ACACIA_BUTTON || item == Items.OAK_BUTTON || item == Items.STONE_BUTTON || item == Items.SPRUCE_BUTTON || item == Items.BIRCH_BUTTON || item == Items.JUNGLE_BUTTON || item == Items.DARK_OAK_BUTTON || item == Items.CRIMSON_BUTTON || item == Items.WARPED_BUTTON || item == Items.POLISHED_BLACKSTONE_BUTTON) {
                buttonSlot = i;
                break;
            }
        }
        if (buttonSlot == -1) return;



        if (target != null) {
            int prevSlot = mc.player.getInventory().selectedSlot;

            if (placeButton.get()) {
                mc.player.getInventory().selectedSlot = buttonSlot;
                BlockPos targetPos = target.getBlockPos();
                if (mc.world.getBlockState(targetPos.add(0, 0, 0)).isAir()) {
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, target.getBlockPos(), true));
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }

            mc.player.getInventory().selectedSlot = anvilSlot;
            BlockPos targetPos = target.getBlockPos().up();

            PlayerUtils.placeBlock(targetPos.add(0, height.get(), 0), Hand.MAIN_HAND);

            mc.player.getInventory().selectedSlot = prevSlot;
        }
    });
}
