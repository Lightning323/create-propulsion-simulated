package org.lightning323.createkinetic.utility;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class AttachmentUtils {
    private AttachmentUtils() {}

    @Nullable
    public static <T> T get(Level level, BlockPos pos, Class<T> attachmentClass, Supplier<T> factory) {
        return null;
    }

}
