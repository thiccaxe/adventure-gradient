/*
 * This file is part of adventure, licensed under the MIT License.
 *
 * Copyright (c) 2017-2024 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.thiccaxe.gradient;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.kyori.adventure.internal.Internals;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.internal.parser.node.TagNode;
import net.kyori.adventure.text.minimessage.internal.parser.node.ValueNode;
import net.kyori.adventure.text.minimessage.internal.serializer.SerializableResolver;
import net.kyori.adventure.text.minimessage.internal.serializer.StyleClaim;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tree.Node;
import net.kyori.adventure.util.HSVLike;
import net.kyori.examination.Examinable;
import net.kyori.examination.ExaminableProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;


final class ColorTagResolver implements TagResolver, SerializableResolver.Single {
    private static final String COLOR_3 = "c";
    private static final String COLOR_2 = "colour";
    private static final String COLOR = "color";

    static final TagResolver INSTANCE = new ColorTagResolver();
    private static final StyleClaim<TextColor> STYLE = StyleClaim.claim(COLOR, Style::color, (color, emitter) -> {
        // TODO: custom aliases
        // TODO: compact vs expanded format? COLOR vs color:COLOR vs c:COLOR
        if (color instanceof NamedTextColor) {
            emitter.tag(NamedTextColor.NAMES.key((NamedTextColor) color));
        } else {
            emitter.tag(color.asHexString());
        }
    });

    private static final Map<String, TextColor> COLOR_ALIASES = new HashMap<>();

    static {
        COLOR_ALIASES.put("dark_grey", NamedTextColor.DARK_GRAY);
        COLOR_ALIASES.put("grey", NamedTextColor.GRAY);
    }

    private static boolean isColorOrAbbreviation(final String name) {
        return name.equals(COLOR) || name.equals(COLOR_2) || name.equals(COLOR_3);
    }

    ColorTagResolver() {
    }

    @Override
    public @Nullable Tag resolve(final @NotNull String name, final @NotNull ArgumentQueue args, final @NotNull Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }

        final String colorName;
        if (isColorOrAbbreviation(name)) {
            colorName = args.popOr("Expected to find a color parameter: <name>|#RRGGBB").lowerValue();
        } else {
            colorName = name;
        }

        final TextColor color = resolveColor(colorName, ctx);
        return Tag.styling(color);
    }

    static @NotNull TextColor resolveColor(final @NotNull String colorName, final @NotNull Context ctx) throws ParsingException {
        final TextColor color;
        if (COLOR_ALIASES.containsKey(colorName)) {
            color = COLOR_ALIASES.get(colorName);
        } else if (colorName.charAt(0) == TextColor.HEX_CHARACTER) {
            color = TextColor.fromHexString(colorName);
        } else {
            color = NamedTextColor.NAMES.value(colorName);
        }

        if (color == null) {
            throw ctx.newException(String.format("Unable to parse a color from '%s'. Please use named colours or hex (#RRGGBB) colors.", colorName));
        }
        return color;
    }

    @Override
    public boolean has(final @NotNull String name) {
        return isColorOrAbbreviation(name)
                || TextColor.fromHexString(name) != null
                || NamedTextColor.NAMES.value(name) != null
                || COLOR_ALIASES.containsKey(name);
    }

    @Override
    public @Nullable StyleClaim<?> claimStyle() {
        return STYLE;
    }
}

abstract class AbstractColorChangingTag implements Modifying, Examinable {

    private static final ComponentFlattener LENGTH_CALCULATOR = ComponentFlattener.builder()
            .mapper(TextComponent.class, TextComponent::content)
            .unknownMapper(x -> "_") // every unknown component gets a single colour
            .build();

    private boolean visited;
    private int size = 0;
    private int disableApplyingColorDepth = -1;

    protected final int size() {
        return this.size;
    }

    @Override
    public final void visit(final @NotNull Node current, final int depth) {
        if (this.visited) {
            throw new IllegalStateException("Color changing tag instances cannot be re-used, return a new one for each resolve");
        }

        if (current instanceof ValueNode) {
            final String value = ((ValueNode) current).value();
            this.size += value.codePointCount(0, value.length());
        } else if (current instanceof TagNode) {
            final TagNode tag = (TagNode) current;
            if (tag.tag() instanceof Inserting) {
                // ComponentTransformation.apply() returns the value of the component placeholder
                LENGTH_CALCULATOR.flatten(((Inserting) tag.tag()).value(), s -> this.size += s.codePointCount(0, s.length()));
            }
        }
    }

    @Override
    public final void postVisit() {
        // init
        this.visited = true;
        this.init();
    }

    @Override
    public final Component apply(final @NotNull Component current, final int depth) {
        if ((this.disableApplyingColorDepth != -1 && depth > this.disableApplyingColorDepth) || current.style().color() != null) {
            if (this.disableApplyingColorDepth == -1 || depth < this.disableApplyingColorDepth) {
                this.disableApplyingColorDepth = depth;
            }
            // This component has its own color applied, which overrides ours
            // We still want to keep track of where we are though if this is text
            if (current instanceof TextComponent) {
                final String content = ((TextComponent) current).content();
                final int len = content.codePointCount(0, content.length());
                for (int i = 0; i < len; i++) {
                    // increment our color index
                    this.advanceColor();
                }
            }
            return current.children(Collections.emptyList());
        }

        this.disableApplyingColorDepth = -1;
        if (current instanceof TextComponent && ((TextComponent) current).content().length() > 0) {
            final TextComponent textComponent = (TextComponent) current;
            final String content = textComponent.content();

            final TextComponent.Builder parent = Component.text();

            // apply
            final int[] holder = new int[1];
            for (final PrimitiveIterator.OfInt it = content.codePoints().iterator(); it.hasNext();) {
                holder[0] = it.nextInt();
                final Component comp = Component.text(new String(holder, 0, 1), current.style().color(this.color()));
                this.advanceColor();
                parent.append(comp);
            }

            return parent.build();
        } else if (!(current instanceof TextComponent)) {
            final Component ret = current.children(Collections.emptyList()).colorIfAbsent(this.color());
            this.advanceColor();
            return ret;
        }

        return Component.empty().mergeStyle(current);
    }

    // The lifecycle

    protected abstract void init();

    /**
     * Advance the active color.
     */
    protected abstract void advanceColor();

    /**
     * Get the current color, without side-effects.
     *
     * @return the current color
     * @since 4.10.0
     */
    protected abstract TextColor color();

    // misc

    @Override
    public abstract @NotNull Stream<? extends ExaminableProperty> examinableProperties();

    @Override
    public final @NotNull String toString() {
        return Internals.toString(this);
    }

    @Override
    public abstract boolean equals(final @Nullable Object other);

    @Override
    public abstract int hashCode();
}

/**
 * A transformation that applies a colour gradient.
 *
 * @since 4.10.0
 */
final class HSVGradientTag extends AbstractColorChangingTag {
    static double lerpDouble(double a, double b, double t) {
        return a*(1d - t) + (b*t);
    }

    static double clampDouble(double d, double min, double max) {
        return Math.min(Math.max(d, min), max);
    }
    private static final String GRADIENT = "gradient";

    static final TagResolver RESOLVER = TagResolver.resolver("gr", HSVGradientTag::create);

    private int index = 0;

    private double multiplier = 1;

    private final Gradient<HSVLike, ColorSpaceInterpolator<HSVLike>> gradient;
    private GradientColorGenerator<HSVLike, ColorSpaceInterpolator<HSVLike>> generator;

    private final TextColor[] colors;
    private @Range(from = -1, to = 1) double phase;

    static Tag create(final ArgumentQueue args, final Context ctx) {
        double phase = 0;
        final List<TextColor> textColors;
        if (args.hasNext()) {
            textColors = new ArrayList<>();
            while (args.hasNext()) {
                final Tag.Argument arg = args.pop();
                // last argument? maybe this is the phase?
                if (!args.hasNext()) {
                    final OptionalDouble possiblePhase = arg.asDouble();
                    if (possiblePhase.isPresent()) {
                        phase = possiblePhase.getAsDouble();
                        if (phase < -1d || phase > 1d) {
                            throw ctx.newException(String.format("Gradient phase is out of range (%s). Must be in the range [-1.0, 1.0] (inclusive).", phase), args);
                        }
                        break;
                    }
                }

                final TextColor parsedColor = ColorTagResolver.resolveColor(arg.value(), ctx);
                textColors.add(parsedColor);
            }

            if (textColors.size() == 1) {
                throw ctx.newException("Invalid gradient, not enough colors. Gradients must have at least two colors.", args);
            }
        } else {
            textColors = Collections.emptyList();
        }

        return new HSVGradientTag(phase, textColors);
    }

    private HSVGradientTag(final double phase, final List<TextColor> colors) {
        if (colors.isEmpty()) {
            this.colors = new TextColor[]{TextColor.color(0xffffff), TextColor.color(0x000000)};
        } else {
            this.colors = colors.toArray(new TextColor[0]);
        }

        if (phase < 0) {
            this.phase = 1 + phase; // [-1, 0) -> [0, 1)
            Collections.reverse(Arrays.asList(this.colors));
        } else {
            this.phase = phase;
        }

        this.gradient = Gradient.gradient(IntStream.range(0, this.colors.length).mapToObj(
                i -> GradientStop.gradientStop((double) i / (this.colors.length - 1), this.colors[i].asHSV())
        ).toList());
    }

    @Override
    protected void init() {
        // Set a scaling factor for character indices, so that the colours in a gradient are evenly spread across the original text
        // make it so the max character index maps to the maximum colour
        this.multiplier = this.size() == 1 ? 0 : (double) (this.colors.length - 1) / (this.size() - 1);
        this.phase *= this.colors.length - 1;
        this.index = 0;
        this.generator = this.gradient.generator(this.size(), (loc, start, end) -> {
            if (start.equals(end)) {
                return start;
            }

            return HSVLike.hsvLike(
                    (float)(lerpDouble(start.h(), end.h(), loc) % 360d),
                    (float)clampDouble(lerpDouble(start.s(), end.s(), loc), 0d, 1d),
                    (float)clampDouble(lerpDouble(start.v(), end.v(), loc), 0d, 1d));
        });
    }

    @Override
    protected void advanceColor() {
        this.index++;
    }

    @Override
    protected TextColor color() {
//        System.out.println(this.index);
        // from [0, this.colors.length - 1], select the position in the gradient
        // we will wrap around in order to preserve an even cycle as would be seen with non-zero phases
        final double position = ((this.index * this.multiplier) + this.phase);
        return TextColor.color(this.generator.colorAt(position));
    }

    @Override
    public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
        return Stream.of(
                ExaminableProperty.of("phase", this.phase),
                ExaminableProperty.of("colors", this.colors)
        );
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        final HSVGradientTag that = (HSVGradientTag) other;
        return this.index == that.index
                && this.phase == that.phase
                && Arrays.equals(this.colors, that.colors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(this.index, this.phase);
        result = 31 * result + Arrays.hashCode(this.colors);
        return result;
    }
}