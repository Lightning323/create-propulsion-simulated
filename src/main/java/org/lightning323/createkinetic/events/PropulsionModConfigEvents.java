package org.lightning323.createkinetic.events;

import org.lightning323.createkinetic.CreatePropulsion;
import org.lightning323.createkinetic.PropulsionConfig;
import org.lightning323.createkinetic.content.thruster.SolidThrusterFuelManager;
import org.lightning323.createkinetic.content.thruster.ThrusterFuelManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

@EventBusSubscriber(modid = CreatePropulsion.ID, bus = EventBusSubscriber.Bus.MOD)
public final class PropulsionModConfigEvents {

    private PropulsionModConfigEvents() {}

    @SubscribeEvent
    public static void onCommonConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != PropulsionConfig.COMMON_SPEC) {
            return;
        }
        ThrusterFuelManager.rebuildThrusterFuelsAfterCommonConfigReload();
        SolidThrusterFuelManager.rebuildAfterCommonConfigReload();
    }
}
