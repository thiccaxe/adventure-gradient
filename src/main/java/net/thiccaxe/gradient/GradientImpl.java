package net.thiccaxe.gradient;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GradientImpl<ColorSpace, Interpolator extends ColorSpaceInterpolator<ColorSpace>> implements Gradient<ColorSpace, Interpolator> {
    private final @NotNull List<GradientStop<ColorSpace>> stops;

    GradientImpl(@NotNull List<GradientStop<ColorSpace>> stops) {
        this.stops = stops;
    }

    @Override
    public @NotNull GradientStop<ColorSpace> start() {
        return this.stops.get(0);
    }

    @Override
    public @NotNull GradientStop<ColorSpace> end() {
        return this.stops.get(this.stops.size() - 1);
    }

    @Override
    public @NotNull List<GradientStop<ColorSpace>> stops() {
        return this.stops;
    }

    @Override
    public @NotNull GradientColorGenerator<ColorSpace, Interpolator> generator(int steps, @NotNull Interpolator interpolator) {
        return new GradientColorGeneratorImpl<>(steps, interpolator, this.stops);
    }


    @Override
    public String toString() {
        return "GradientImpl{" +
                "stops=" + stops +
                '}';
    }
}

