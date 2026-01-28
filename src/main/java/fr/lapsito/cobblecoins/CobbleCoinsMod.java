package fr.lapsito.cobblecoins;

import net.neoforged.eventbus.api.listener.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CobbleCoinsMod.MODID)
public class CobbleCoinsMod {
    public static final String MODID = "cobblecoins";

    public CobbleCoinsMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        fr.lapsito.cobblecoins.items.ModItems.register(bus);
    }
}
