package net.thiccaxe.gradient;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.util.HSVLike;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Main {

    static double lerpDouble(double a, double b, double t) {
        return a*(1d - t) + (b*t);
    }

    static double clampDouble(double d, double min, double max) {
        return Math.min(Math.max(d, min), max);
    }

    public static void main(String[] args) {
        System.out.println("Hello world!");

        final var comp = Component.text("test", Style.style().color(TextColor.fromCSSHexString("#25f9d2")).decorate(TextDecoration.BOLD, TextDecoration.ITALIC).build());
        System.out.println(ANSIComponentSerializer.ansi().serialize(comp));

        var grad1 = Gradient.gradient(
                (TextColor)NamedTextColor.RED,
                NamedTextColor.AQUA
        );
        System.out.println(grad1);
        /**
         * This will be a static final field somewhere (the text color lerp should be pulled out)?
         */
        System.out.println(StreamSupport.stream(grad1.generator(10, (loc, start, end) -> TextColor.lerp((float)loc, start, end)).spliterator(), false).toList());
        var compBuilder1 = Component.text();
        StreamSupport.stream(grad1.generator(10, (loc, start, end) -> TextColor.lerp((float)loc, start, end)).spliterator(), false)
                .forEachOrdered(col -> compBuilder1.append(Component.text("A", col)));
        System.out.println(ANSIComponentSerializer.ansi().serialize(compBuilder1.build()));

        var grad2 = Gradient.gradient(
                TextColor.fromCSSHexString("#06302a").asHSV(),
                TextColor.fromCSSHexString("#ed582a").asHSV()
        );
        System.out.println(grad2);
        /**
         * This will be a static final field somewhere?
         * Gradient.HSV_INTERPOLATOR ?
         */
        ColorSpaceInterpolator<HSVLike> hsvLerper = (loc, start, end) -> {
            if (start.equals(end)) {
                return start;
            }

            return HSVLike.hsvLike(
                    (float)(lerpDouble(start.h(), end.h(), loc) % 360d),
                    (float)clampDouble(lerpDouble(start.s(), end.s(), loc), 0d, 1d),
                    (float)clampDouble(lerpDouble(start.v(), end.v(), loc), 0d, 1d));
        };
        System.out.println(
                StreamSupport.stream(grad2.generator(10, hsvLerper).spliterator(), false).collect(Collectors.toList())
        );
        var compBuilder2 = Component.text();
        StreamSupport.stream(grad2.generator(10, hsvLerper).spliterator(), false)
                .forEachOrdered(col -> compBuilder2.append(Component.text("A", TextColor.color(col))));
        System.out.println(ANSIComponentSerializer.ansi().serialize(compBuilder2.build()));
    }
}