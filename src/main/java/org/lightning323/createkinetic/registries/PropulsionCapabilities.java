package org.lightning323.createkinetic.registries;

import org.lightning323.createkinetic.content.heat.IHeatConsumer;
import org.lightning323.createkinetic.content.heat.IHeatSource;
import org.lightning323.createkinetic.CreateKinetic;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

public class PropulsionCapabilities {
    public static final BlockCapability<IHeatSource, Direction> HEAT_SOURCE =
        BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(CreateKinetic.ID, "heat_source"), IHeatSource.class);

    public static final BlockCapability<IHeatConsumer, Direction> HEAT_CONSUMER =
        BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(CreateKinetic.ID, "heat_consumer"), IHeatConsumer.class);
}

