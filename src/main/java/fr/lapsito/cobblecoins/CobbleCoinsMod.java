package fr.lapsito.cobblecoins;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CobbleCoinsMod.MODID)
public class CobbleCoinsMod {
    public static final String MODID = "cobblecoins";

    public CobbleCoinsMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        items.ModItems.register(bus);
    }
}
