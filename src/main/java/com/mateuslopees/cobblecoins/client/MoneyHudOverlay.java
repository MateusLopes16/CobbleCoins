package com.mateuslopees.cobblecoins.client;

import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.config.CobbleCoinsConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MoneyHudOverlay implements LayeredDraw.Layer {
    private static final ResourceLocation COIN_ICON = ResourceLocation.fromNamespaceAndPath(CobbleCoins.MOD_ID, "textures/item/cobblecoin.png");
    
    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!CobbleCoinsConfig.CLIENT.showMoneyHud.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Calculate position based on anchor
        int x, y;
        CobbleCoinsConfig.HudAnchor anchor = CobbleCoinsConfig.CLIENT.hudAnchor.get();
        int offsetX = CobbleCoinsConfig.CLIENT.hudPositionX.get();
        int offsetY = CobbleCoinsConfig.CLIENT.hudPositionY.get();

        switch (anchor) {
            case TOP_LEFT -> {
                x = 10 + offsetX;
                y = 10 + offsetY;
            }
            case TOP_RIGHT -> {
                x = screenWidth - 80 + offsetX;
                y = 10 + offsetY;
            }
            case BOTTOM_LEFT -> {
                x = 10 + offsetX;
                y = screenHeight - 25 + offsetY;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - 80 + offsetX;
                y = screenHeight - 25 + offsetY;
            }
            default -> {
                x = screenWidth - 80 + offsetX;
                y = screenHeight - 25 + offsetY;
            }
        }

        // Draw background box
        int boxWidth = 70;
        int boxHeight = 18;
        graphics.fill(x - 2, y - 2, x + boxWidth + 2, y + boxHeight, 0x80000000);

        // Draw coin icon
        graphics.blit(COIN_ICON, x, y, 0, 0, 16, 16, 16, 16);

        // Draw balance text
        String balanceText = ClientBankData.getFormattedBalance();
        graphics.drawString(mc.font, Component.literal(balanceText), x + 20, y + 4, 0xFFD700, true);
    }
}
