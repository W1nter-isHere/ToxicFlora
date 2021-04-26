package wintersteve25.toxicflora.common.block.machines.infuser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankPropertiesWrapper;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.SoundKeyframeEvent;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;
import wintersteve25.toxicflora.ToxicFlora;
import wintersteve25.toxicflora.client.proxy.SoundProxy;
import wintersteve25.toxicflora.common.crafting.RecipeInfuser;

import javax.annotation.Nullable;

public class TileInfuser extends TileEntity implements ITickable, IFluidHandler, IFluidTank, IAnimatable {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            TileInfuser.this.markDirty();
        }
    };
    private static boolean isCrafting = false;
    public static int totalTicks = 0;
    private static int remainingTicks = 0;
    private static FluidStack inputFluidContent = null;
    private static FluidStack outputFluidContent = null;
    private static final int capacity = Fluid.BUCKET_VOLUME*4;
    public static final FluidTank inputTank = new FluidTank(capacity);
    public static final FluidTank outputTank = new FluidTank(capacity);
    private static IFluidTankProperties[] tankProperties;
    private static boolean canFill = true;
    private static boolean canDrain = true;

    private BlockInfuser blockInfuser;
    private final AnimationFactory factory = new AnimationFactory(this);
    private String controllerName = "infuser_controller";

    @Override
    public void update() {
        if (!world.isRemote) {
            if (isCrafting) {
                ItemStack itemStack = itemHandler.getStackInSlot(0);
                RecipeInfuser recipe = RecipeInfuser.getRecipe(inputTank.getFluid(), itemStack);
                if (recipe != null) {
                    if (remainingTicks > 0) {
                        remainingTicks--;
                        markDirty();
                        ToxicFlora.getLogger().info(remainingTicks);
                        if (itemStack.isEmpty() || inputTank.getFluid() == null) {
                            remainingTicks = 0;
                            isCrafting = false;
                            markDirty();
                        }
                        if (remainingTicks <= 0) {
                            ToxicFlora.getLogger().info("Infuser Craft completed");
                            outputFluidContent = RecipeInfuser.getFluidOutput(inputTank, itemStack).copy();
                            outputTank.fill(outputFluidContent, true);
                            inputFluidContent = null;
                            inputTank.drain(recipe.getFluidInput(), true);
                            itemHandler.extractItem(0, 1, false);
                            isCrafting = false;
                            remainingTicks = 0;
                            markDirty();

                        }
                    } else {
                        if (outputTank.getFluid() == null) {
                            totalTicks = recipe.getProcessTime();
                            remainingTicks = totalTicks;
                            markDirty();
                        } else if (outputTank.getFluid().containsFluid(recipe.getFluidOutput())) {
                            if (outputTank.getFluidAmount() - recipe.getFluidOutput().amount >= 0) {
                                totalTicks = recipe.getProcessTime();
                                remainingTicks = totalTicks;
                                markDirty();
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean addItem(@Nullable EntityPlayer player, ItemStack heldItem, @Nullable EnumHand hand) {
        if (!itemHandler.getStackInSlot(0).isEmpty()) {
            return false;
        }
        if (isCrafting == true) {
            return false;
        }
        if (itemHandler.getStackInSlot(0).isEmpty()) {
            ItemStack itemAdd = heldItem.copy();
            itemHandler.insertItem(0, itemAdd, false);
            if(player == null || !player.capabilities.isCreativeMode) {
                heldItem.shrink(heldItem.getCount());

            }
            markDirty();
        }
        return true;
    }

    public void tryStartCraft() {
        if (!world.isRemote) {
            this.isCrafting = true;
            markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("items", itemHandler.serializeNBT());
        inputTank.writeToNBT(compound);
        outputTank.writeToNBT(compound);

        compound.setInteger("progress", remainingTicks);
        compound.setBoolean("isCrafting", isCrafting);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if(compound.hasKey("items")) {
            itemHandler.deserializeNBT(compound.getCompoundTag("items"));
        }
        inputTank.readFromNBT(compound);
        inputFluidContent = FluidStack.loadFluidStackFromNBT(compound);

        outputTank.readFromNBT(compound);
        outputFluidContent = FluidStack.loadFluidStackFromNBT(compound);

        remainingTicks = compound.getInteger("progress");
        isCrafting = compound.getBoolean("isCrafting");
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (facing == EnumFacing.DOWN) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(outputTank);
            } else if (facing == EnumFacing.UP) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(inputTank);
            } else {
                if (outputTank.getFluid() != null) {
                    return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(outputTank);
                }
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(inputTank);
            }
        }
        return super.getCapability(capability, facing);
    }

    public boolean hasItem() {
        ItemStack itemStack = new ItemStack(itemHandler.serializeNBT());
        if (itemStack == null) { return false; } else { return true; }
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        if (this.tankProperties == null)
        {
            this.tankProperties = new IFluidTankProperties[] {
                    new FluidTankPropertiesWrapper(inputTank),
                    new FluidTankPropertiesWrapper(outputTank)
            };
        }
        return this.tankProperties;
    }

    @Nullable
    @Override
    public FluidStack getFluid() {
        return inputFluidContent;
    }

    public FluidStack getOutputFluid() {
        return outputFluidContent;
    }

    @Override
    public int getFluidAmount() {
        if (inputFluidContent == null)
        {
            return 0;
        }
        return inputFluidContent.amount;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public FluidTankInfo getInfo() {
        return new FluidTankInfo(this);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (!canFillFluidType(resource))
        {
            return 0;
        }

        return fillInternal(resource, doFill);
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (!canDrainFluidType(getFluid())) {
            return null;
        }
        return drainInternal(resource, doDrain);
    }

    @Nullable
    public FluidStack drainInternal(FluidStack resource, boolean doDrain)
    {
        if (resource == null || !resource.isFluidEqual(getFluid())) {
            return null;
        }
        return drainInternal(resource.amount, doDrain);
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (!canDrainFluidType(inputFluidContent)) {
            return null;
        }
        return drainInternal(maxDrain, doDrain);
    }

    @Nullable
    public FluidStack drainInternal(int maxDrain, boolean doDrain)
    {
        TileInfuser tile = new TileInfuser();
        if (inputFluidContent == null || maxDrain <= 0) {
            return null;
        }
        int drained = maxDrain;
        if (inputFluidContent.amount < drained)
        {
            drained = inputFluidContent.amount;
        }

        FluidStack stack = new FluidStack(inputFluidContent, drained);
        if (doDrain)
        {
            inputFluidContent.amount -= drained;
            if (inputFluidContent.amount <= 0)
            {
                inputFluidContent = null;
            }

            onContentsChanged();

            if (tile != null)
            {
                FluidEvent.fireEvent(new FluidEvent.FluidDrainingEvent(inputFluidContent, tile.getWorld(), tile.getPos(), this, drained));
            }
        }
        return stack;
    }

    public boolean canDrainFluidType(@Nullable FluidStack fluid)
    {
        return fluid != null && canDrain();
    }

    public boolean canDrain() {
        return canDrain;
    }

    public boolean canFillFluidType(FluidStack inputFluidContent) {
        return canFill();
    }

    public boolean canFill() {
        return canFill;
    }

    public int fillInternal(FluidStack resource, boolean doFill) {
        TileInfuser tile = new TileInfuser();
        if (resource == null || resource.amount <= 0)
        {
            return 0;
        }

        if (!doFill)
        {
            if (inputFluidContent == null)
            {
                return Math.min(capacity, resource.amount);
            }

            if (!inputFluidContent.isFluidEqual(resource))
            {
                return 0;
            }

            return Math.min(capacity - inputFluidContent.amount, resource.amount);
        }

        if (inputFluidContent == null)
        {
            inputFluidContent = new FluidStack(resource, Math.min(capacity, resource.amount));

            onContentsChanged();

            if (tile != null)
            {
                FluidEvent.fireEvent(new FluidEvent.FluidFillingEvent(inputFluidContent, tile.getWorld(), tile.getPos(), this, inputFluidContent.amount));
            }
            return inputFluidContent.amount;
        }

        if (!inputFluidContent.isFluidEqual(resource))
        {
            return 0;
        }
        int filled = capacity - inputFluidContent.amount;

        if (resource.amount < filled)
        {
            inputFluidContent.amount += resource.amount;
            filled = resource.amount;
        }
        else
        {
            inputFluidContent.amount = capacity;
        }

        onContentsChanged();

        if (tile != null)
        {
            FluidEvent.fireEvent(new FluidEvent.FluidFillingEvent(inputFluidContent, tile.getWorld(), tile.getPos(), this, filled));
        }
        return filled;
    }

    public void onContentsChanged() {
        markDirty();
    }

    private <P extends TileEntity & IAnimatable> PlayState predicate(AnimationEvent<P> event) {
        AnimationController controller = event.getController();
        controller.transitionLengthTicks = 200;
        if (event.getAnimatable().getWorld().isRaining()) {
            controller.setAnimation(new AnimationBuilder().addAnimation("infuser.craft", false));
        }

        return PlayState.CONTINUE;
    }

    private <ENTITY extends IAnimatable> void soundListener(SoundKeyframeEvent<ENTITY> event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        player.playSound(SoundProxy.INFUSER_MUSIC, 1, 1);
    }

    @Override
    public void registerControllers(AnimationData data) {
        AnimationController controller = new AnimationController(this, controllerName, 200, this::predicate);
        controller.registerSoundListener(this::soundListener);
        data.addAnimationController(controller);
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}