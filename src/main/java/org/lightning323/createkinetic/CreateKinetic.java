package org.lightning323.createkinetic;


import org.lightning323.createkinetic.compat.computercraft.CCProxy;
import org.lightning323.createkinetic.events.ModCapabilityEvents;
import org.lightning323.createkinetic.events.ModSetupEvents;
import org.lightning323.createkinetic.network.PropulsionPackets;
import org.lightning323.createkinetic.particles.ParticleTypes;
import org.lightning323.createkinetic.assemblerstick.item.ModItems;
import com.simibubi.create.compat.Mods;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.lightning323.createkinetic.registries.*;

@Mod(CreateKinetic.ID)
public class CreateKinetic {
    public static final String ID = "createkinetic";

    public CreateKinetic(IEventBus modBus, ModContainer modContainer) {
        modBus.addListener(ModCapabilityEvents::registerCapabilities);
        modBus.addListener(ModSetupEvents::onCommonSetup);
        //Content
        ParticleTypes.register(modBus);
        PropulsionBlocks.register(modBus);
        PropulsionBlockEntities.register(modBus);
        PropulsionItems.register(modBus);
        ModItems.register(modBus);
        PropulsionSoundEvents.register(modBus);
        PropulsionFluids.register(modBus);
        PropulsionPartialModels.register();
        PropulsionCreativeTab.registerAeronauticsSections();
        modBus.addListener(PropulsionPackets::register);
        PropulsionDisplaySources.register();
        PropulsionSableBridge.init();

        //Compat
        Mods.COMPUTERCRAFT.executeIfInstalled(() -> CCProxy::register);

        //Config
        modContainer.registerConfig(ModConfig.Type.COMMON, PropulsionConfig.COMMON_SPEC, ID + "-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, PropulsionConfig.CLIENT_SPEC, ID + "-client.toml");
        PropulsionDefaultStress.init(PropulsionConfig.COMMON_SPEC);
    }

    public static ResourceLocation loc(String loc) {
        return ResourceLocation.fromNamespaceAndPath(ID, loc);
    }
}
