package wintersteve25.toxicflora.common.crafting;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.oredict.OreDictionary;
import wintersteve25.toxicflora.common.config.InfuserConfig;

import java.util.ArrayList;
import java.util.List;

public class RecipeInfuser extends RecipeBaseSimple{
    private FluidStack output;
    private FluidStack input;
    private Object itemInput;
    private int processTime;
    private int minVitality;

    public static final List<RecipeInfuser> recipesInfuser = new ArrayList<RecipeInfuser>();

    public RecipeInfuser(Object itemInput, FluidStack input, FluidStack output, int processTime, int minVitality) {
        this.input = input;
        this.output = output;
        this.processTime = processTime;
        this.minVitality = minVitality;

        if (itemInput == null) {
            throw new IllegalArgumentException("Item input can not be null!");
        }
        if (itemInput instanceof ItemStack) {
            this.itemInput = ((ItemStack) itemInput).copy();
        } else if (itemInput instanceof String) {
            this.itemInput = OreDictionary.getOres((String) itemInput);
        } else {
            throw new IllegalArgumentException("Invalid item input, must be an ore dictionary or itemstack");
        }
    }

    public RecipeInfuser(Object itemInput, FluidStack input, FluidStack output, int processTime) {
        this.input = input;
        this.output = output;
        this.processTime = processTime;
        this.minVitality = InfuserConfig.defaultMinVitality;

        if (itemInput == null) {
            throw new IllegalArgumentException("Item input can not be null!");
        }
        if (itemInput instanceof ItemStack) {
            this.itemInput = ((ItemStack) itemInput).copy();
        } else if (itemInput instanceof String) {
            this.itemInput = OreDictionary.getOres((String) itemInput);
        } else {
            throw new IllegalArgumentException("Invalid item input, must be an ore dictionary or itemstack");
        }
    }

    public RecipeInfuser(Object itemInput, FluidStack input, FluidStack output) {
        this.input = input;
        this.output = output;
        this.processTime = InfuserConfig.defaultProcessSpeed;
        this.minVitality = InfuserConfig.defaultMinVitality;

        if (itemInput == null) {
            throw new IllegalArgumentException("Item input can not be null!");
        }
        if (itemInput instanceof ItemStack) {
            this.itemInput = ((ItemStack) itemInput).copy();
        } else if (itemInput instanceof String) {
            this.itemInput = OreDictionary.getOres((String) itemInput);
        } else {
            throw new IllegalArgumentException("Invalid item input, must be an ore dictionary or itemstack");
        }
    }

    public boolean isRecipeMatch(FluidStack input, Object itemInput) {
        if (input == null || !input.isFluidEqual(getFluidInput())) {
            return false;
        }
        if (input.amount < getFluidInput().amount) {
            return false;
        }
        if (itemInput instanceof ItemStack) {
            ItemStack itemStack = ((ItemStack) itemInput);
            return areStacksTheSame((ItemStack) (getItemInput()), itemStack, false);
        } else if (itemInput instanceof String) {
            List<ItemStack> list = OreDictionary.getOres((String) itemInput);
            for (ItemStack stack : list) {
                return areStacksTheSame((ItemStack) (getItemInput()), stack, false);
            }
        }
        return false;
    }

    public static RecipeInfuser addRecipe(Object inputItem, FluidStack inputFluid, FluidStack outputFluid, int processTime, int minVitality) {
        RecipeInfuser recipe = new RecipeInfuser(inputItem, inputFluid, outputFluid, processTime, minVitality);
        recipesInfuser.add(recipe);
        return recipe;
    }

    public static RecipeInfuser addRecipe(Object inputItem, FluidStack inputFluid, FluidStack outputFluid, int processTime) {
        RecipeInfuser recipe = new RecipeInfuser(inputItem, inputFluid, outputFluid, processTime);
        recipesInfuser.add(new RecipeInfuser(inputItem, inputFluid, outputFluid, processTime));
        return recipe;
    }

    public static RecipeInfuser addRecipe(Object inputItem, FluidStack inputFluid, FluidStack outputFluid) {
        RecipeInfuser recipe = new RecipeInfuser(inputItem, inputFluid, outputFluid);
        recipesInfuser.add(new RecipeInfuser(inputItem, inputFluid, outputFluid));
        return recipe;
    }

    public static void removeRecipe(FluidStack input, Object item) {
        RecipeInfuser recipeToRemove = getRecipe(input, item);
        recipesInfuser.remove(recipeToRemove);
    }

    public static FluidStack getFluidOutput(IFluidTank tank, Object item) {
        RecipeInfuser recipeInfuser = getRecipe(tank.getFluid(), item);
        FluidStack fluidStack = recipeInfuser != null ? recipeInfuser.getFluidOutput() : null;
        return fluidStack;
    }

    public static RecipeInfuser getRecipe(FluidStack input, Object item) {
        for (RecipeInfuser recipes : recipesInfuser) {
            if(recipes.isRecipeMatch(input, item)) {
                return recipes;
            }
        }
        return null;
    }

    @Override
    public Object getItemInput() {
        return itemInput;
    }

    @Override
    public FluidStack getFluidOutput() {
        return output;
    }

    @Override
    public FluidStack getFluidInput() {
        return input;
    }

    @Override
    public int getProcessTime() {
        return processTime;
    }

    @Override
    public int getMinVitality() {
        return minVitality;
    }
}
