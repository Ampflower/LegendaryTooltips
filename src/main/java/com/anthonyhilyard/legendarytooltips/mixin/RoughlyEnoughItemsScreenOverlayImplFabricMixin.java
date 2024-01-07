package com.anthonyhilyard.legendarytooltips.mixin;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiConsumer;

import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.Loader;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.impl.client.gui.fabric.ScreenOverlayImplFabric;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Mixin(ScreenOverlayImplFabric.class)
public class RoughlyEnoughItemsScreenOverlayImplFabricMixin
{
	private static final BiConsumer<GuiGraphics, ItemStack> setTooltipStack;

	static
	{
		BiConsumer<GuiGraphics, ItemStack> $setTooltipStack = ($1, $2) -> {};
		try
		{
			// FIXME: Realistically this should go into Iceberg, or it should expose a stable API.
			Field tooltipStackField = GuiGraphics.class.getDeclaredField("icebergTooltipStack");
			tooltipStackField.setAccessible(true);

			$setTooltipStack = (graphics, stack) ->
			{
				try
				{
					tooltipStackField.set(graphics, stack);
				}
				catch (ReflectiveOperationException roe)
				{
					throw new AssertionError(roe);
				}
			};
		}
		catch (ReflectiveOperationException | SecurityException e)
		{
			Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
		}

		setTooltipStack = $setTooltipStack;
	}

	@Inject(method = "renderTooltipInner(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;Lme/shedaniel/rei/api/client/gui/widgets/Tooltip;II)V",
			at = @At(value = "HEAD"), require = 0)
	private void setHoverStack(Screen screen, GuiGraphics graphics, Tooltip tooltip, int mouseX, int mouseY, CallbackInfo info)
	{
		EntryStack<?> entryStack = tooltip.getContextStack();
		ItemStack itemStack = entryStack.getType() == VanillaEntryTypes.ITEM ? entryStack.castValue() : ItemStack.EMPTY;

		setTooltipStack.accept(graphics, itemStack);
	}

	@Inject(method = "renderTooltipInner(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;Lme/shedaniel/rei/api/client/gui/widgets/Tooltip;II)V",
			at = @At(value = "INVOKE", target = "Lme/shedaniel/rei/impl/client/gui/fabric/ScreenOverlayImplFabric;renderTooltipInner(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;II)V",
			shift = Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT, require = 0)
	private void formatTooltipComponents(Screen screen, GuiGraphics graphics, Tooltip tooltip, int mouseX, int mouseY, CallbackInfo info, List<ClientTooltipComponent> lines)
	{
		EntryStack<?> entryStack = tooltip.getContextStack();
		ItemStack itemStack = entryStack.getType() == VanillaEntryTypes.ITEM ? entryStack.castValue() : ItemStack.EMPTY;

		if (itemStack.isEmpty())
		{
			return;
		}

		List<Component> textElements = tooltip.entries().stream().map(e -> e.isText() ? e.getAsText() : null).filter(e -> e != null).toList();
		lines.clear();
		lines.addAll(Tooltips.gatherTooltipComponents(itemStack, textElements, itemStack.getTooltipImage(), mouseX, screen.width, screen.height, null, screen.font, -1));
	}
}
