package com.saivivaahn.pvpcoach.client.mixin;

import com.saivivaahn.pvpcoach.client.PvPCoachClient;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures every physical left-button press before vanilla consumes the attack key binding. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void pvpcoach$recordLeftClick(long window, int button, int action, int modifiers, CallbackInfo info) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            PvPCoachClient.registerLeftMouseClick();
        }
    }
}
