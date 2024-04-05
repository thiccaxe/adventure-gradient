package net.thiccaxe.gradient;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public interface Gradient<ColorSpace, Interpolator extends ColorSpaceInterpolator<ColorSpace>> {
    /**
     * start().location() == 0d -> true
     */
    @NotNull GradientStop<ColorSpace> start();

    /**
     * end().location() == 1d -> true
     */
    @NotNull GradientStop<ColorSpace> end();


    /**
     * stops().size() >= 2 -> true
     * First item equivalent to start()
     * Last item equivalent to stop()
     */
    @NotNull List<GradientStop<ColorSpace>> stops();

    @NotNull GradientColorGenerator<ColorSpace, Interpolator> generator(int steps, Interpolator interpolator);

    static <C, CI extends ColorSpaceInterpolator<C>> Gradient<C, CI> gradient(@NotNull C start, @NotNull C end) {
        return new GradientImpl<>(List.of(
                GradientStop.start(start),
                GradientStop.end(end)
        ));
    }

    static <C, CI extends ColorSpaceInterpolator<C>> Gradient<C, CI> gradient(@NotNull C start, @NotNull List<GradientStop<C>> stops, @NotNull C end) {
        // stupid impl
        return new GradientImpl<>(Stream.concat(Stream.concat(
                Stream.of(GradientStop.start(start)), stops.stream()
        ), Stream.of(GradientStop.end(end))).toList());
    }

    static <C, CI extends ColorSpaceInterpolator<C>> Gradient<C, CI> gradient(@NotNull List<GradientStop<C>> stops) {
        return new GradientImpl<>(List.copyOf(stops));
    }
}

