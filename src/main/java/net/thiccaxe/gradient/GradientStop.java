package net.thiccaxe.gradient;

import org.jetbrains.annotations.NotNull;

interface GradientStop<ColorSpace> {
    @NotNull ColorSpace color();

    /**
     * Location must be between 0 and 1, inclusive.
     */
    double location();

    static <C>  GradientStop<C> start(@NotNull C color) {
        return new GradientStopImpl<>(0d, color);
    }

    static <C>  GradientStop<C> end(@NotNull C color) {
        return new GradientStopImpl<>(1d, color);
    }

    static <C>  GradientStop<C> gradientStop(double location, @NotNull C color) {
        return new GradientStopImpl<C>(location, color);
    }

}