package net.thiccaxe.gradient;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ColorSpaceInterpolator<ColorSpace> {

    @NotNull ColorSpace lerp(final double location, final @NotNull ColorSpace start, final @NotNull ColorSpace end);
}
