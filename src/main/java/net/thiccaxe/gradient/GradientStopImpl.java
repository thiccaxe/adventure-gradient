package net.thiccaxe.gradient;

import org.jetbrains.annotations.NotNull;

public class GradientStopImpl<ColorSpace> implements GradientStop<ColorSpace> {
    private final double location;
    private final @NotNull ColorSpace color;

    GradientStopImpl(double location, @NotNull ColorSpace color) {
        if (location < 0d || location > 1d) {
            throw new IllegalArgumentException("GradientStop location (" + location + ") must be within the required range [0, 1].");
        }
        this.location = location;
        this.color = color;
    }

    @Override
    public @NotNull ColorSpace color() {
        return color;
    }

    @Override
    public double location() {
        return location;
    }

    @Override
    public String toString() {
        return "GradientStopImpl{" +
                "location=" + location +
                ", color=" + color +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GradientStopImpl<?> that = (GradientStopImpl<?>) o;

        if (Double.compare(that.location, location) != 0) return false;
        return color.equals(that.color);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(location);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + color.hashCode();
        return result;
    }
}
