package net.swill.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SwillClient implements ClientModInitializer {
    public static KeyBinding menuKey;
    public static boolean aimbotEnabled = true;
    public static boolean wallhackEnabled = true;
    public static boolean triggerbotEnabled = true;

    @Override
    public void onInitializeClient() {
        System.out.println("[SWILL] Client initialized");
        
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.swill.menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "SWILL Client"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            if (aimbotEnabled) {
                Entity target = findNearestMob(client);
                if (target != null) aimAtEntity(client, target);
            }
            
            if (triggerbotEnabled && client.crosshairTarget instanceof EntityHitResult) {
                EntityHitResult hit = (EntityHitResult) client.crosshairTarget;
                Entity target = hit.getEntity();
                if ((target instanceof HostileEntity || target instanceof PassiveEntity) && target.isAlive()) {
                    if (client.player.distanceTo(target) <= 4.0) {
                        client.interactionManager.attackEntity(client.player, target);
                        try { Thread.sleep(150); } catch(Exception e) {}
                    }
                }
            }
        });
        
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!wallhackEnabled || MinecraftClient.getInstance().player == null) return;
            for (Entity entity : MinecraftClient.getInstance().world.getEntities()) {
                if (entity == MinecraftClient.getInstance().player) continue;
                if (entity instanceof HostileEntity || entity instanceof PassiveEntity) {
                    drawESP(context, entity);
                }
            }
        });
    }
    
    private Entity findNearestMob(MinecraftClient client) {
        Entity nearest = null;
        double nearestDist = 20.0;
        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (!(entity instanceof HostileEntity || entity instanceof PassiveEntity)) continue;
            if (!entity.isAlive()) continue;
            double dist = client.player.distanceTo(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }
        return nearest;
    }
    
    private void aimAtEntity(MinecraftClient client, Entity target) {
        Vec3d targetPos = target.getBoundingBox().getCenter();
        Vec3d playerPos = client.player.getEyePos();
        Vec3d delta = targetPos.subtract(playerPos);
        double yaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
        double pitch = -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        client.player.setYaw((float) (client.player.getYaw() + (yaw - client.player.getYaw()) / 5.0));
        client.player.setPitch((float) (client.player.getPitch() + (pitch - client.player.getPitch()) / 5.0));
    }
    
    private void drawESP(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context, Entity entity) {
        var matrices = context.matrixStack();
        matrices.push();
        Box box = entity.getBoundingBox();
        double x = entity.getX() - context.camera().getPos().x;
        double y = entity.getY() - context.camera().getPos().y;
        double z = entity.getZ() - context.camera().getPos().z;
        matrices.translate(x, y, z);
        float r = entity instanceof HostileEntity ? 1.0f : 0.2f;
        float g = entity instanceof HostileEntity ? 0.2f : 1.0f;
        float b = 0.2f;
        var tessellator = net.minecraft.client.render.Tessellator.getInstance();
        var buffer = tessellator.getBuffer();
        buffer.begin(net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINES, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        var matrix = matrices.peek().getPositionMatrix();
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, 0.8f).next();
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, 0.8f).next();
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, 0.8f).next();
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, 0.8f).next();
        tessellator.draw();
        matrices.pop();
    }
}
