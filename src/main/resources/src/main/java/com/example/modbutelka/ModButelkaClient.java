package com.example.modbutelka;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class ModButelkaClient implements ClientModInitializer {
    private static final int REQUIRED_LEVEL = 31;
    private static final int COOLDOWN_TICKS = 20; // 20 tickow = 1 sekunda przerwy miedzy komendami

    private boolean enabled = false;
    private int cooldown = 0;

    @Override
    public void onInitializeClient() {
        // /modbutelka wlacza i wylacza moda
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("modbutelka").executes(context -> {
                    enabled = !enabled;
                    cooldown = 0;
                    context.getSource().sendFeedback(Text.literal(
                            enabled ? "§aModButelka: WŁĄCZONY" : "§cModButelka: WYŁĄCZONY"));
                    return 1;
                })));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (cooldown > 0) {
            cooldown--;
        }
        ClientPlayerEntity player = client.player;
        if (!enabled || player == null || client.getNetworkHandler() == null || cooldown > 0) {
            return;
        }
        if (player.experienceLevel >= REQUIRED_LEVEL) {
            // wysyla komende prosto do serwera, bez otwierania czatu
            client.getNetworkHandler().sendChatCommand("butelka");
            cooldown = COOLDOWN_TICKS;
        }
    }
}
