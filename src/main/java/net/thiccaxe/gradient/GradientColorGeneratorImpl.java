package net.thiccaxe.gradient;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class GradientColorGeneratorImpl<ColorSpace, Interpolator extends ColorSpaceInterpolator<ColorSpace>> implements GradientColorGenerator<ColorSpace, Interpolator> {
    private final int steps;
    private final @NotNull Interpolator interpolator;
    private final @NotNull List<GradientStop<ColorSpace>> stops;

    GradientColorGeneratorImpl(final int steps, final @NotNull Interpolator interpolator, @NotNull List<GradientStop<ColorSpace>> stops) {
        if (steps < 0) {
            throw new IllegalArgumentException("steps (" + steps + ") must not be negative");
        }
        this.steps = steps;
        this.interpolator = interpolator;
        this.stops = stops;
    }

    @Override
    public @NotNull ColorSpace colorAt(final double location) {
        double boundedLocation = Math.min(Math.max(location, 0d), 1d);
        GradientStop<ColorSpace> start = stops.get(0);
        GradientStop<ColorSpace> end = stops.get(stops.size() - 1);
        for (final var stop : this.stops) {
//            System.out.println(stop);
            if (stop.location() < location) { // start bound
                if (start == null || start.location() <= stop.location()) {
                    start = stop;
                }
            } else if (stop.location() > location) { // end bound
                if (end == null || end.location() >= stop.location()) {
                    end = stop;
                }
            }
        }
//        System.out.println(start);
//        System.out.println(end);
        final double transformedLocation = (boundedLocation - start.location()) / (end.location() - start.location());
        return interpolator.lerp(transformedLocation, start.color(), end.color());
    }


    @NotNull
    @Override
    public Iterator<ColorSpace> iterator() {
        // special behavior
        if (steps == 0) {
            return Collections.emptyIterator();
        } else if (steps == 1) {
            return List.of(this.stops.get(0).color()).iterator();
        }
        // todo - better perfofmance
        final double scaleFactor = 1d / (steps - 1);
        return IntStream.range(0, steps).mapToDouble(pos -> pos * scaleFactor).mapToObj(this::colorAt).iterator();
    }
}
