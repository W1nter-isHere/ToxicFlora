package wintersteve25.toxicflora.client.model.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import wintersteve25.toxicflora.ToxicFlora;
import wintersteve25.toxicflora.common.block.machines.infuser.TileInfuser;
import wintersteve25.toxicflora.common.helper.MathHelper;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class InfuserTESR extends TileEntitySpecialRenderer<TileInfuser> {
    public static final float TANK_THICKNESS = 0.1f;

    @Override
    public void render(TileInfuser tileEntity, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableBlend();
        GlStateManager.translate((float) x, (float) y, (float) z);

        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        renderFluid(tileEntity);

        GlStateManager.popMatrix();

        renderItem(tileEntity, x, y, z);
    }

    private void renderItem(TileInfuser tile, double x, double y, double z) {
        Float random = MathHelper.getRangedRandom(0, 1);
        if (tile == null) {
            return;
        }
        if(tile.getItemHandler().getStackInSlot(0).isEmpty()) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableBlend();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.translate(0.5F, 0.25F, 0.5F);
        GlStateManager.translate(0F, 0F, 0.0F);
        GlStateManager.rotate(105F, 0.4F, 1F, 0F);
        Minecraft.getMinecraft().getRenderItem().renderItem(tile.getItemHandler().getStackInSlot(0), ItemCameraTransforms.TransformType.GROUND);
        GlStateManager.popMatrix();
    }

    private void renderFluid(TileInfuser tile) {
        if (tile == null) {
            return;
        }

        FluidStack fluidStack = tile.getInputTank().getFluid();
        if (fluidStack == null) {
            return;
        }

        Fluid fluid = fluidStack.getFluid();

        if (fluid == null) {
            return;
        }

        float scale = (0.625F / tile.getInputTank().getCapacity()) * tile.getInputTank().getFluidAmount();;

        if (scale > 0.0f) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder renderer = tessellator.getBuffer();
            ResourceLocation still = fluid.getStill();
            TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(still.toString());

            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();

            GlStateManager.color(1, 1, 1, .5f);
            renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

            float u1 = sprite.getMinU();
            float v1 = sprite.getMinV();
            float u2 = sprite.getMaxU();
            float v2 = sprite.getMaxV();

            // Top
            renderer.pos(TANK_THICKNESS,  scale + TANK_THICKNESS, TANK_THICKNESS).tex(u1, v1).color(255, 255, 255, 128).endVertex();
            renderer.pos(TANK_THICKNESS, scale + TANK_THICKNESS, 1-TANK_THICKNESS).tex(u1, v2).color(255, 255, 255, 128).endVertex();
            renderer.pos(1-TANK_THICKNESS, scale + TANK_THICKNESS, 1-TANK_THICKNESS).tex(u2, v2).color(255, 255, 255, 128).endVertex();
            renderer.pos(1-TANK_THICKNESS, scale + TANK_THICKNESS, TANK_THICKNESS).tex(u2, v1).color(255, 255, 255, 128).endVertex();

            tessellator.draw();

            net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
        }
    }
}
