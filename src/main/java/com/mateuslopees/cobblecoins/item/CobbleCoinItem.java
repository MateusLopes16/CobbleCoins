package com.mateuslopees.cobblecoins.item;

import com.mateuslopees.cobblecoins.data.BankAccountManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CobbleCoinItem extends Item {
    private final int value;

    public CobbleCoinItem(Properties properties) {
        this(properties, 1);
    }

    public CobbleCoinItem(Properties properties, int value) {
        super(properties);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int totalValue = stack.getCount() * value;
            
            // Deposit coins into bank account
            BankAccountManager.addBalance(serverPlayer.getUUID(), totalValue);
            
            // Play coin sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
            
            // Send message to player
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.deposited", totalValue)
                            .withStyle(ChatFormatting.GREEN),
                    true);
            
            // Consume all coins in the stack
            stack.shrink(stack.getCount());
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        int totalValue = stack.getCount() * value;
        
        if (value > 1) {
            tooltip.add(Component.translatable("tooltip.cobblecoins.value_per_coin", value)
                    .withStyle(ChatFormatting.GOLD));
        }
        
        tooltip.add(Component.translatable("tooltip.cobblecoins.total_value", totalValue)
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.cobblecoins.right_click_deposit")
                .withStyle(ChatFormatting.GRAY));
    }
}
