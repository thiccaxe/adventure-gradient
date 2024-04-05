package net.thiccaxe.gradient;

import org.jetbrains.annotations.NotNull;


/**
 * Just for nicety
 *
 */
public interface GradientColorGenerator<ColorSpace, Interpolator extends ColorSpaceInterpolator<ColorSpace>> extends Iterable<ColorSpace> {
    @NotNull ColorSpace colorAt(double location);
}
