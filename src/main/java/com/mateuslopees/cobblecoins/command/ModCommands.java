package com.mateuslopees.cobblecoins.command;

import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.config.CobbleCoinsConfig;
import com.mateuslopees.cobblecoins.data.BankAccountManager;
import com.mateuslopees.cobblecoins.shop.ShopManager;
import com.mateuslopees.cobblecoins.trade.TradeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CobbleCoins.MOD_ID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /money - View your balance
        dispatcher.register(Commands.literal("money")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        long balance = BankAccountManager.getBalance(player.getUUID());
                        player.displayClientMessage(
                                Component.translatable("command.cobblecoins.balance", balance)
                                        .withStyle(ChatFormatting.GOLD),
                                false);
                    }
                    return 1;
                }));

        // /balance - Alias for /money
        dispatcher.register(Commands.literal("balance")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        long balance = BankAccountManager.getBalance(player.getUUID());
                        player.displayClientMessage(
                                Component.translatable("command.cobblecoins.balance", balance)
                                        .withStyle(ChatFormatting.GOLD),
                                false);
                    }
                    return 1;
                }));

        // /pay <player> <amount> - Pay another player
        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer sender) {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                        long amount = LongArgumentType.getLong(context, "amount");

                                        if (sender.getUUID().equals(target.getUUID())) {
                                            sender.displayClientMessage(
                                                    Component.translatable("command.cobblecoins.cant_pay_self")
                                                            .withStyle(ChatFormatting.RED),
                                                    false);
                                            return 0;
                                        }

                                        if (BankAccountManager.transfer(sender.getUUID(), target.getUUID(), amount)) {
                                            sender.displayClientMessage(
                                                    Component.translatable("command.cobblecoins.paid", amount, target.getName().getString())
                                                            .withStyle(ChatFormatting.GREEN),
                                                    false);
                                            target.displayClientMessage(
                                                    Component.translatable("command.cobblecoins.received", amount, sender.getName().getString())
                                                            .withStyle(ChatFormatting.GOLD),
                                                    false);
                                        } else {
                                            sender.displayClientMessage(
                                                    Component.translatable("command.cobblecoins.insufficient_funds")
                                                            .withStyle(ChatFormatting.RED),
                                                    false);
                                        }
                                    }
                                    return 1;
                                }))));

        // /cshop - Open the shop
        dispatcher.register(Commands.literal("cshop")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        ShopManager.openShop(player);
                    }
                    return 1;
                }));

        // /shop - Alias for /cshop
        dispatcher.register(Commands.literal("shop")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        ShopManager.openShop(player);
                    }
                    return 1;
                }));

        // /trade <player> - Request a trade with a player
        dispatcher.register(Commands.literal("trade")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer sender) {
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                TradeManager.requestTrade(sender, target);
                            }
                            return 1;
                        })));

        // /tradeaccept - Accept a pending trade request
        dispatcher.register(Commands.literal("tradeaccept")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        player.displayClientMessage(
                                Component.translatable("command.cobblecoins.use_click_to_accept")
                                        .withStyle(ChatFormatting.YELLOW),
                                false);
                    }
                    return 1;
                }));

        // /tradedecline - Decline a pending trade request
        dispatcher.register(Commands.literal("tradedecline")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        player.displayClientMessage(
                                Component.translatable("command.cobblecoins.use_click_to_decline")
                                        .withStyle(ChatFormatting.YELLOW),
                                false);
                    }
                    return 1;
                }));

        // /tradecancel - Cancel current trade
        dispatcher.register(Commands.literal("tradecancel")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        TradeManager.cancelTrade(player);
                    }
                    return 1;
                }));
        
        // /streak - View your current streaks
        dispatcher.register(Commands.literal("streak")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        int captureStreak = com.mateuslopees.cobblecoins.event.CobblemonEventHandler.getPlayerCaptureStreak(player.getUUID());
                        int defeatStreak = com.mateuslopees.cobblecoins.event.CobblemonEventHandler.getPlayerDefeatStreak(player.getUUID());
                        
                        double captureBonus = captureStreak * CobbleCoinsConfig.COMMON.streakBonusPerCapture.get() * 100;
                        double defeatBonus = defeatStreak * CobbleCoinsConfig.COMMON.streakBonusPerDefeat.get() * 100;
                        
                        player.displayClientMessage(Component.literal("§6§l⚡ Your Streaks:"), false);
                        player.displayClientMessage(Component.literal("§7  Capture Streak: §e" + captureStreak + 
                                " §7(+§a" + String.format("%.0f", captureBonus) + "%§7 bonus)"), false);
                        player.displayClientMessage(Component.literal("§7  Battle Streak: §e" + defeatStreak + 
                                " §7(+§a" + String.format("%.0f", defeatBonus) + "%§7 bonus)"), false);
                    }
                    return 1;
                }));
        
        // /myshop - Open your own shop (simple command)
        dispatcher.register(Commands.literal("myshop")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        var shop = com.mateuslopees.cobblecoins.data.PlayerShopManager.getShop(player.getUUID());
                        if (shop == null) {
                            // Auto-create shop if it doesn't exist
                            if (com.mateuslopees.cobblecoins.data.PlayerShopManager.createShop(player.getUUID(), player.getName().getString() + "'s Shop")) {
                                player.displayClientMessage(
                                        Component.literal("§aShop created! Opening your shop..."),
                                        false);
                            } else {
                                player.displayClientMessage(
                                        Component.literal("§cPlayer shops are disabled on this server!"),
                                        false);
                                return 0;
                            }
                        }
                        com.mateuslopees.cobblecoins.data.PlayerShopManager.openOwnShop(player);
                    }
                    return 1;
                }));
        
        // /playershop <player> - Visit another player's shop (simple command)
        dispatcher.register(Commands.literal("playershop")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                ServerPlayer shopOwner = EntityArgument.getPlayer(context, "player");
                                var shop = com.mateuslopees.cobblecoins.data.PlayerShopManager.getShop(shopOwner.getUUID());
                                if (shop != null) {
                                    player.displayClientMessage(
                                            Component.literal("§aVisiting §e" + shop.getName() + "§a..."),
                                            false);
                                    com.mateuslopees.cobblecoins.data.PlayerShopManager.openPlayerShop(player, shopOwner.getUUID());
                                } else {
                                    player.displayClientMessage(
                                            Component.literal("§cThis player doesn't have a shop!"),
                                            false);
                                }
                            }
                            return 1;
                        }))
                .then(Commands.literal("list")
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                var shops = com.mateuslopees.cobblecoins.data.PlayerShopManager.getAllShops();
                                if (shops.isEmpty()) {
                                    player.displayClientMessage(
                                            Component.literal("§7No player shops available."),
                                            false);
                                } else {
                                    player.displayClientMessage(
                                            Component.literal("§6§l⬢ Player Shops:"),
                                            false);
                                    shops.forEach((uuid, shop) -> {
                                        ServerPlayer owner = player.getServer().getPlayerList().getPlayer(uuid);
                                        String ownerName = owner != null ? owner.getName().getString() : "Offline";
                                        player.displayClientMessage(
                                                Component.literal("§7  - §e" + shop.getName() + " §7by §f" + ownerName + " §7(§e/playershop " + ownerName + "§7)"),
                                                false);
                                    });
                                }
                            }
                            return 1;
                        })));

        // Admin commands
        dispatcher.register(Commands.literal("cobblecoins")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            long amount = LongArgumentType.getLong(context, "amount");
                                            
                                            BankAccountManager.addBalance(target.getUUID(), amount);
                                            
                                            context.getSource().sendSuccess(() ->
                                                    Component.translatable("command.cobblecoins.admin.gave", amount, target.getName().getString())
                                                            .withStyle(ChatFormatting.GREEN),
                                                    true);
                                            
                                            target.displayClientMessage(
                                                    Component.translatable("command.cobblecoins.admin.received", amount)
                                                            .withStyle(ChatFormatting.GOLD),
                                                    false);
                                            return 1;
                                        }))))
                .then(Commands.literal("take")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            long amount = LongArgumentType.getLong(context, "amount");
                                            
                                            BankAccountManager.removeBalance(target.getUUID(), amount);
                                            
                                            context.getSource().sendSuccess(() ->
                                                    Component.translatable("command.cobblecoins.admin.took", amount, target.getName().getString())
                                                            .withStyle(ChatFormatting.GREEN),
                                                    true);
                                            return 1;
                                        }))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            long amount = LongArgumentType.getLong(context, "amount");
                                            
                                            BankAccountManager.setBalance(target.getUUID(), amount);
                                            
                                            context.getSource().sendSuccess(() ->
                                                    Component.translatable("command.cobblecoins.admin.set", target.getName().getString(), amount)
                                                            .withStyle(ChatFormatting.GREEN),
                                                    true);
                                            return 1;
                                        }))))
                .then(Commands.literal("check")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    long balance = BankAccountManager.getBalance(target.getUUID());
                                    
                                    context.getSource().sendSuccess(() ->
                                            Component.translatable("command.cobblecoins.admin.check", target.getName().getString(), balance)
                                                    .withStyle(ChatFormatting.GOLD),
                                            false);
                                    return 1;
                                })))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            ShopManager.init(context.getSource().getServer());
                            context.getSource().sendSuccess(() ->
                                    Component.translatable("command.cobblecoins.admin.reloaded")
                                            .withStyle(ChatFormatting.GREEN),
                                    true);
                            return 1;
                        })));

        CobbleCoins.LOGGER.info("CobbleCoins commands registered");
    }
}
