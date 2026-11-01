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
    private static final int RESPONSE_TIMEOUT_TICKS = 10; // ile czekamy na zmiane poziomu, zanim ponowimy
    private static final int SPAM_LIMIT = 160;            // nie zwiekszaj, bo serwer wyrzuci za spam

    private boolean enabled = false;
    private boolean waiting = false;
    private int waitTicks = 0;
    private int sentLevel = 0;
    private int spam = 0; // naśladuje licznik antyspamu serwera: +20 za komende, -1 na tick

    @Override
    public void onInitializeClient() {
        // /modbutelka wlacza i wylacza moda
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("modbutelka").executes(context -> {
                    enabled = !enabled;
                    waiting = false;
                    context.getSource().sendFeedback(Text.literal(
                            enabled ? "§aModButelka: WŁĄCZONY" : "§cModButelka: WYŁĄCZONY"));
                    return 1;
                })));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (spam > 0) {
            spam--;
        }
        ClientPlayerEntity player = client.player;
        if (!enabled || player == null || client.getNetworkHandler() == null) {
            waiting = false;
            return;
        }

        if (waiting) {
            waitTicks--;
            // czekamy, az serwer zmieni nam poziom (czyli przyjal komende), albo minie limit czasu
            if (player.experienceLevel == sentLevel && waitTicks > 0) {
                return;
            }
            waiting = false;
        }

        if (player.experienceLevel >= REQUIRED_LEVEL && spam <= SPAM_LIMIT) {
            // wysyla komende prosto do serwera, bez otwierania czatu
            client.getNetworkHandler().sendChatCommand("butelka");
            spam += 20;
            sentLevel = player.experienceLevel;
            waiting = true;
            waitTicks = RESPONSE_TIMEOUT_TICKS;
        }
    }
}
