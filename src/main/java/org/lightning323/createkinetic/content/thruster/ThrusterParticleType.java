package org.lightning323.createkinetic.content.thruster;

import java.util.Locale;

import org.lightning323.createkinetic.particles.plasma.PlasmaParticleData;
import org.lightning323.createkinetic.particles.plume.PlumeParticleData;
import com.mojang.serialization.Codec;

import net.minecraft.core.particles.ParticleOptions;

public enum ThrusterParticleType {
    NONE,
    PLUME,
    PLASMA;

    public static final Codec<ThrusterParticleType> CODEC = Codec.STRING.xmap(
        ThrusterParticleType::fromString,
        ThrusterParticleType::serializedName
    );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ThrusterParticleType fromString(String value) {
        if (value == null) {
            return PLUME;
        }
        try {
            return ThrusterParticleType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return PLUME;
        }
    }

    public ParticleOptions createParticleOptions() {
        return switch (this) {
            case PLASMA -> new PlasmaParticleData();
            case NONE, PLUME -> new PlumeParticleData();
        };
    }

    public ParticleOptions createParticleOptions(FluidThrusterProperties properties) {
        return switch (this) {
            case PLASMA -> new PlasmaParticleData(properties.overrideTextures(), properties.overrideColor());
            case NONE, PLUME -> new PlumeParticleData(properties.overrideTextures(), properties.overrideColor());
        };
    }

    public ParticleOptions createParticleOptions(ItemThrusterProperties properties) {
        return switch (this) {
            case PLASMA -> new PlasmaParticleData(properties.overrideTextures(), properties.overrideColor());
            case NONE, PLUME -> new PlumeParticleData(properties.overrideTextures(), properties.overrideColor());
        };
    }
}
