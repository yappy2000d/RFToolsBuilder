package mcjty.rftoolsbuilder.modules.builder.client;

import com.mojang.blaze3d.platform.GlStateManager;
import mcjty.lib.base.StyleConfig;
import mcjty.lib.client.RenderHelper;
import mcjty.lib.gui.IKeyReceiver;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.layout.HorizontalAlignment;
import mcjty.lib.gui.layout.HorizontalLayout;
import mcjty.lib.gui.layout.PositionalLayout;
import mcjty.lib.gui.layout.VerticalLayout;
import mcjty.lib.gui.widgets.Label;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.gui.widgets.*;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.rftoolsbuilder.modules.builder.BuilderConfiguration;
import mcjty.rftoolsbuilder.modules.builder.items.ShapeCardItem;
import mcjty.rftoolsbuilder.modules.builder.items.ShapeCardType;
import mcjty.rftoolsbuilder.modules.builder.network.PacketUpdateNBTItemInventoryShape;
import mcjty.rftoolsbuilder.modules.builder.network.PacketUpdateNBTShapeCard;
import mcjty.rftoolsbuilder.modules.scanner.ScannerConfiguration;
import mcjty.rftoolsbuilder.setup.RFToolsBuilderMessages;
import mcjty.rftoolsbuilder.shapes.IShapeParentGui;
import mcjty.rftoolsbuilder.shapes.Shape;
import mcjty.rftoolsbuilder.shapes.ShapeID;
import mcjty.rftoolsbuilder.shapes.ShapeRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

public class GuiShapeCard extends Screen implements IShapeParentGui, IKeyReceiver {

    /** The X size of the window in pixels. */
    protected int xSize = 360;
    /** The Y size of the window in pixels. */
    protected int ySize = 160;

    private int guiLeft;
    private int guiTop;

    private boolean isQuarryCard;

    private ChoiceLabel shapeLabel;
    private ChoiceLabel solidLabel;
    private TextField dimX;
    private TextField dimY;
    private TextField dimZ;
    private TextField offsetX;
    private TextField offsetY;
    private TextField offsetZ;
    private Window window;
    private Label blocksLabel;

    private Panel voidPanel;
    private ToggleButton stone;
    private ToggleButton cobble;
    private ToggleButton dirt;
    private ToggleButton gravel;
    private ToggleButton sand;
    private ToggleButton netherrack;
    private ToggleButton endstone;
    private ToggleButton oredict;

    public final boolean fromTE;

    // For GuiComposer, GuiBuilder, etc.: the current card to edit
    public static BlockPos fromTEPos = null;
    public static int fromTEStackSlot = 0;
    public static Screen returnGui = null;

    private ShapeID shapeID = null;
    private ShapeRenderer shapeRenderer = null;


    public GuiShapeCard(boolean fromTE) {
        super(new StringTextComponent("Shapecard"));
        this.fromTE = fromTE;
    }

    private ShapeRenderer getShapeRenderer() {
        if (shapeID == null) {
            shapeID = getShapeID();
        } else if (!shapeID.equals(getShapeID())) {
            shapeID = getShapeID();
            shapeRenderer = null;
        }
        if (shapeRenderer == null) {
            shapeRenderer = new ShapeRenderer(shapeID);
            shapeRenderer.initView(getPreviewLeft(), guiTop);
        }
        return shapeRenderer;
    }

    private ShapeID getShapeID() {
        ItemStack stackToEdit = getStackToEdit();
        return new ShapeID(DimensionType.OVERWORLD, null, ShapeCardItem.getScanId(stackToEdit), false, ShapeCardItem.isSolid(stackToEdit));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private ItemStack getStackToEdit() {
        if (fromTE) {
            TileEntity te = minecraft.world.getTileEntity(fromTEPos);
            if (te instanceof IInventory) {
                return ((IInventory) te).getStackInSlot(fromTEStackSlot);
            } else {
                return ItemStack.EMPTY;
            }
        } else {
            return minecraft.player.getHeldItem(Hand.MAIN_HAND);
        }
    }

    @Override
    public int getPreviewLeft() {
        return guiLeft + 104;
    }

    @Override
    public int getPreviewTop() {
        return guiTop - 5 + (isQuarryCard ? 0 : 10);
    }

    @Override
    public void init() {
        super.init();

        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        ItemStack heldItem = getStackToEdit();
        if (heldItem.isEmpty()) {
            // Cannot happen!
            return;
        }

        ShapeCardType type = ShapeCardItem.getType(heldItem);
        isQuarryCard = type.isQuarry();
        if (isQuarryCard) {
            ySize = 160 + 28;
        }

        getShapeRenderer().initView(getPreviewLeft(), guiTop);

        shapeLabel = new ChoiceLabel(minecraft, this).setDesiredWidth(100).setDesiredHeight(16).addChoices(
                Shape.SHAPE_BOX.getDescription(),
                Shape.SHAPE_TOPDOME.getDescription(),
                Shape.SHAPE_BOTTOMDOME.getDescription(),
                Shape.SHAPE_SPHERE.getDescription(),
                Shape.SHAPE_CYLINDER.getDescription(),
                Shape.SHAPE_CAPPEDCYLINDER.getDescription(),
                Shape.SHAPE_PRISM.getDescription(),
                Shape.SHAPE_TORUS.getDescription(),
                Shape.SHAPE_CONE.getDescription(),
                Shape.SHAPE_HEART.getDescription(),
                Shape.SHAPE_COMPOSITION.getDescription(),
                Shape.SHAPE_SCAN.getDescription()
        ).addChoiceEvent((parent, newChoice) -> updateSettings());

        solidLabel = new ChoiceLabel(minecraft, this).setDesiredWidth(50).setDesiredHeight(16).addChoices(
                "Hollow",
                "Solid"
        ).addChoiceEvent((parent, newChoice) -> updateSettings());

        Panel shapePanel = new Panel(minecraft, this).setLayout(new HorizontalLayout()).addChild(shapeLabel).addChild(solidLabel);

        Shape shape = ShapeCardItem.getShape(heldItem);
        shapeLabel.setChoice(shape.getDescription());
        boolean solid = ShapeCardItem.isSolid(heldItem);
        solidLabel.setChoice(solid ? "Solid" : "Hollow");

        blocksLabel = new Label(minecraft, this).setText("# ").setHorizontalAlignment(HorizontalAlignment.ALIGN_LEFT);
        blocksLabel.setDesiredWidth(100).setDesiredHeight(16);

        Panel modePanel = new Panel(minecraft, this).setLayout(new VerticalLayout()).setDesiredWidth(170).addChild(shapePanel).addChild(blocksLabel);

        BlockPos dim = ShapeCardItem.getDimension(heldItem);
        BlockPos offset = ShapeCardItem.getOffset(heldItem);

        dimX = new TextField(minecraft, this).addTextEvent((parent, newText) -> {
            if (isTorus()) {
                dimZ.setText(newText);
            }
            updateSettings();
        }).setText(String.valueOf(dim.getX()));
        dimY = new TextField(minecraft, this).addTextEvent((parent, newText) -> updateSettings()).setText(String.valueOf(dim.getY()));
        dimZ = new TextField(minecraft, this).addTextEvent((parent, newText) -> updateSettings()).setText(String.valueOf(dim.getZ()));
        Panel dimPanel = new Panel(minecraft, this).setLayout(new HorizontalLayout().setHorizontalMargin(0)).addChild(new Label(minecraft, this).setText("Dim:").setHorizontalAlignment(HorizontalAlignment.ALIGN_RIGHT).setDesiredWidth(40)).setDesiredHeight(18).addChild(dimX).addChild(dimY).addChild(dimZ);
        offsetX = new TextField(minecraft, this).addTextEvent((parent, newText) -> updateSettings()).setText(String.valueOf(offset.getX()));
        offsetY = new TextField(minecraft, this).addTextEvent((parent, newText) -> updateSettings()).setText(String.valueOf(offset.getY()));
        offsetZ = new TextField(minecraft, this).addTextEvent((parent, newText) -> updateSettings()).setText(String.valueOf(offset.getZ()));
        Panel offsetPanel = new Panel(minecraft, this).setLayout(new HorizontalLayout().setHorizontalMargin(0)).addChild(new Label(minecraft, this).setText("Offset:").setHorizontalAlignment(HorizontalAlignment.ALIGN_RIGHT).setDesiredWidth(40)).setDesiredHeight(18).addChild(offsetX).addChild(offsetY).addChild(offsetZ);

        Panel settingsPanel = new Panel(minecraft, this).setLayout(new VerticalLayout().setSpacing(1).setVerticalMargin(1).setHorizontalMargin(0))
                .addChild(dimPanel).addChild(offsetPanel);

        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize) / 2;

        Panel modeSettingsPanel = new Panel(minecraft, this).setLayout(new VerticalLayout().setHorizontalMargin(0)).addChild(modePanel).addChild(settingsPanel);
        modeSettingsPanel.setLayoutHint(0, 0, 180, 160);
        Panel toplevel;
        if (isQuarryCard) {
            setupVoidPanel(heldItem);
            toplevel = new Panel(minecraft, this).setLayout(new PositionalLayout()).setFilledRectThickness(2).addChild(modeSettingsPanel).addChild(voidPanel);

        } else {
            toplevel = new Panel(minecraft, this).setLayout(new PositionalLayout()).setFilledRectThickness(2).addChild(modeSettingsPanel);
        }

        toplevel.setBounds(new Rectangle(k, l, xSize, ySize));

        window = new Window(this, toplevel);
    }

    private void setupVoidPanel(ItemStack heldItem) {
        voidPanel = new Panel(minecraft, this).setLayout(new HorizontalLayout())
                .setDesiredHeight(26)
                .setFilledRectThickness(-2)
                .setFilledBackground(StyleConfig.colorListBackground);
        voidPanel.setLayoutHint(5, 155, 350, 26);
        Label label = new Label(minecraft, this).setText("Void:");
        stone = new ToggleButton(minecraft, this).setDesiredWidth(20).setDesiredHeight(20).setTooltips("Void stone").addButtonEvent(widget -> updateVoidSettings());
        cobble = new ToggleButton(minecraft, this).setDesiredWidth(20).setDesiredHeight(20).setTooltips("Void cobble").addButtonEvent(widget -> updateVoidSettings());
        dirt = new ToggleButton(minecraft, this).setDesiredWidth(20).setDesiredHeight(20).setTooltips("Void dirt").addButtonEvent(widget -> updateVoidSettings());
        gravel = new ToggleButton(minecraft, this).setDesiredWidth(20).setDesiredHeight(20).setTooltips("Void gravel").addButtonEvent(widget -> updateVoidSettings());
        sand = new ToggleButton(minecraft, this).setDesiredWidth(20).setDesiredHeight(20).setTooltips("Void sand").addButtonEvent(widget -> updateVoidSettings());
        netherrack = new ToggleButton(minecraft, this).setDesiredWidth(20).setDesiredHeight(20).setTooltips("Void netherrack").addButtonEvent(widget -> updateVoidSettings());
        endstone = new ToggleButton(minecraft, this).setDesiredWidth(20).setDesiredHeight(20).setTooltips("Void end stone").addButtonEvent(widget -> updateVoidSettings());
        oredict = new ToggleButton(minecraft, this).setDesiredWidth(60).setDesiredHeight(15).setTooltips("Enable ore dictionary matching")
                .setText("Oredict")
                .setCheckMarker(true)
                .addButtonEvent(widget -> updateVoidSettings());

        stone.setPressed(ShapeCardItem.isVoiding(heldItem, "stone"));
        cobble.setPressed(ShapeCardItem.isVoiding(heldItem, "cobble"));
        dirt.setPressed(ShapeCardItem.isVoiding(heldItem, "dirt"));
        gravel.setPressed(ShapeCardItem.isVoiding(heldItem, "gravel"));
        sand.setPressed(ShapeCardItem.isVoiding(heldItem, "sand"));
        netherrack.setPressed(ShapeCardItem.isVoiding(heldItem, "netherrack"));
        endstone.setPressed(ShapeCardItem.isVoiding(heldItem, "endstone"));
        oredict.setPressed(ShapeCardItem.isOreDictionary(heldItem));

        voidPanel.addChildren(label, stone, cobble, dirt, gravel, sand, netherrack, endstone, oredict);
    }

    private boolean isTorus() {
        Shape shape = getCurrentShape();
        return Shape.SHAPE_TORUS.equals(shape);
    }

    private Shape getCurrentShape() {
        return Shape.getShape(shapeLabel.getCurrentChoice());
    }

    private boolean isSolid() {
        return "Solid".equals(solidLabel.getCurrentChoice());
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateSettings() {
        int dx = parseInt(dimX.getText());
        int dy = parseInt(dimY.getText());
        int dz = parseInt(dimZ.getText());
        int max = Math.max(ScannerConfiguration.maxScannerDimension.get(), BuilderConfiguration.maxBuilderDimension.get());
        if (dx < 0) {
            dx = 0;
        } else if (dx > max) {
            dx = max;
        }
        dimX.setText(Integer.toString(dx));
        if (dz < 0) {
             dz = 0;
        } else if (dz > max) {
            dz = max;
        }
        dimZ.setText(Integer.toString(dz));
        if (dy < 0) {
            dy = 0;
        } else if (dy > 256) {
            dy = 256;
        }
        dimY.setText(Integer.toString(dy));

        if (isTorus()) {
            dimZ.setText(dimX.getText());
        }
        if (fromTE) {
            ItemStack stack = getStackToEdit();
            if (!stack.isEmpty()) {
                CompoundNBT tag = stack.getTag();
                if (tag == null) {
                    tag = new CompoundNBT();
                }
                ShapeCardItem.setShape(stack, getCurrentShape(), isSolid());
                ShapeCardItem.setDimension(stack, dx, dy, dz);
                ShapeCardItem.setOffset(stack, parseInt(offsetX.getText()), parseInt(offsetY.getText()), parseInt(offsetZ.getText()));
                RFToolsBuilderMessages.INSTANCE.sendToServer(new PacketUpdateNBTItemInventoryShape(
                        fromTEPos, fromTEStackSlot, tag));
            }
        } else {
            RFToolsBuilderMessages.INSTANCE.sendToServer(new PacketUpdateNBTShapeCard(
                    TypedMap.builder()
                            .put(new Key<>("shape", Type.STRING), getCurrentShape().getDescription())
                            .put(new Key<>("solid", Type.BOOLEAN), isSolid())
                            .put(new Key<>("dimX", Type.INTEGER), dx)
                            .put(new Key<>("dimY", Type.INTEGER), dy)
                            .put(new Key<>("dimZ", Type.INTEGER), dz)
                            .put(new Key<>("offsetX", Type.INTEGER), parseInt(offsetX.getText()))
                            .put(new Key<>("offsetY", Type.INTEGER), parseInt(offsetY.getText()))
                            .put(new Key<>("offsetZ", Type.INTEGER), parseInt(offsetZ.getText()))
                            .build()));
        }
    }

    private void updateVoidSettings() {
        if (fromTE) {
            ItemStack stack = getStackToEdit();
            if (!stack.isEmpty()) {
                CompoundNBT tag = stack.getTag();
                if (tag == null) {
                    tag = new CompoundNBT();
                }
                tag.putBoolean("voidstone", stone.isPressed());
                tag.putBoolean("voidcobble", cobble.isPressed());
                tag.putBoolean("voiddirt", dirt.isPressed());
                tag.putBoolean("voidgravel", gravel.isPressed());
                tag.putBoolean("voidsand", sand.isPressed());
                tag.putBoolean("voidnetherrack", netherrack.isPressed());
                tag.putBoolean("voidendstone", endstone.isPressed());
                tag.putBoolean("oredict", oredict.isPressed());
                RFToolsBuilderMessages.INSTANCE.sendToServer(new PacketUpdateNBTItemInventoryShape(
                        fromTEPos, fromTEStackSlot, tag));
            }
        } else {
            RFToolsBuilderMessages.INSTANCE.sendToServer(new PacketUpdateNBTShapeCard(
                    TypedMap.builder()
                            .put(new Key<>("voidstone", Type.BOOLEAN), stone.isPressed())
                            .put(new Key<>("voidcobble", Type.BOOLEAN), cobble.isPressed())
                            .put(new Key<>("voiddirt", Type.BOOLEAN), dirt.isPressed())
                            .put(new Key<>("voidgravel", Type.BOOLEAN), gravel.isPressed())
                            .put(new Key<>("voidsand", Type.BOOLEAN), sand.isPressed())
                            .put(new Key<>("voidnetherrack", Type.BOOLEAN), netherrack.isPressed())
                            .put(new Key<>("voidendstone", Type.BOOLEAN), endstone.isPressed())
                            .put(new Key<>("oredict", Type.BOOLEAN), oredict.isPressed())
                            .build()));
        }
    }

    private boolean buttons[] = new boolean[10];    // @todo ugly hack to get mouse buttons?

    @Override
    public void mouseMoved(double xx, double yy) {
        window.handleMouseInput(0); // @todo 1.14 is this right? What button?

        MouseHelper mouse = getMinecraft().mouseHelper;
        int x = (int)mouse.getMouseX() * width / getMinecraft().mainWindow.getWidth();
        int y = (int)mouse.getMouseY() * height / getMinecraft().mainWindow.getHeight();
        x -= guiLeft;
        y -= guiTop;

        getShapeRenderer().handleShapeDragging(x, y, buttons);
    }


    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button < buttons.length) {
            buttons[button] = true;
        }
        boolean rc = super.mouseClicked(x, y, button);
        window.mouseClicked((int)x, (int)y, button);
        return rc;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button < buttons.length) {
            buttons[button] = false;
        }
        boolean rc = super.mouseReleased(mouseX, mouseY, button);
        window.mouseMovedOrUp((int)mouseX, (int)mouseY, button);
        return rc;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean rc = false;
        if (!window.keyTyped(keyCode, scanCode)) {
            rc = super.keyPressed(keyCode, scanCode, modifiers);
        }
        return rc;
    }

    @Override
    public Window getWindow() {
        return window;
    }

    @Override
    public void keyTypedFromEvent(int keyCode, int scanCode) {
        if (window != null) {
            if (window.keyTyped(keyCode, scanCode)) {
                super.keyPressed(keyCode, scanCode, 0); // @todo 1.14: modifiers?
            }
        }
    }

    @Override
    public void charTypedFromEvent(char codePoint) {
        if (window != null) {
            if (window.charTyped(codePoint)) {
                super.charTyped(codePoint, 0); // @todo 1.14: modifiers?
            }
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double wheel) {
        getShapeRenderer().handleMouseWheel(wheel);
        return super.mouseScrolled(x, y, wheel);
    }

    private static int updateCounter = 20;

    @Override
    public void render(int xSize_lo, int ySize_lo, float par3) {

        super.render(xSize_lo, ySize_lo, par3);

        dimZ.setEnabled(!isTorus());

        updateCounter--;
        if (updateCounter <= 0) {
            updateCounter = 10;
            int count = getShapeRenderer().getCount();
            if (count >= ShapeCardItem.MAXIMUM_COUNT) {
                blocksLabel.setText("#Blocks: ++" + count);
            } else {
                blocksLabel.setText("#Blocks: " + count);
            }
        }

        window.draw();

        if (isQuarryCard) {
            // @@@ Hacky code!
            int x = (int) (window.getToplevel().getBounds().getX() + voidPanel.getBounds().getX()) + 1;
            int y = (int) (window.getToplevel().getBounds().getY() + voidPanel.getBounds().getY() + stone.getBounds().getY()) + 1;

            renderVoidBlock(x, y, stone, Blocks.STONE);
            renderVoidBlock(x, y, cobble, Blocks.COBBLESTONE);
            renderVoidBlock(x, y, dirt, Blocks.DIRT);
            renderVoidBlock(x, y, gravel, Blocks.GRAVEL);
            renderVoidBlock(x, y, sand, Blocks.SAND);
            renderVoidBlock(x, y, netherrack, Blocks.NETHERRACK);
            renderVoidBlock(x, y, endstone, Blocks.END_STONE);
        }

        ItemStack stack = getStackToEdit();
        if (!stack.isEmpty()) {
            getShapeRenderer().renderShape(this, stack, guiLeft, guiTop, true, true, true, false);
        }

        List<String> tooltips = window.getTooltips();
        if (tooltips != null) {
            int guiLeft = (this.width - this.xSize) / 2;
            int guiTop = (this.height - this.ySize) / 2;
            MouseHelper mouse = getMinecraft().mouseHelper;
            int x = (int)mouse.getMouseX() * width / getMinecraft().mainWindow.getWidth();
            int y = height - (int)mouse.getMouseY() * height / getMinecraft().mainWindow.getHeight() - 1;
            renderTooltip(tooltips, x-guiLeft, y-guiTop, minecraft.fontRenderer);
        }
    }

    private void renderVoidBlock(int x, int y, ToggleButton button, Block block) {
        x += (int) button.getBounds().getX();
        RenderHelper.renderObject(Minecraft.getInstance(), x, y, new ItemStack(block), button.isPressed());
        if (button.isPressed()) {
            drawLine(x-1, y-1, x+18, y+18, 0xffff0000);
            drawLine(x+18, y-1, x-1, y+18, 0xffff0000);
        }
    }

    private static void drawLine(int x1, int y1, int x2, int y2, int color) {
        float f3 = (color >> 24 & 255) / 255.0F;
        float f = (color >> 16 & 255) / 255.0F;
        float f1 = (color >> 8 & 255) / 255.0F;
        float f2 = (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();
        GlStateManager.disableDepthTest();
        GL11.glLineWidth(2.0f);
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color4f(f, f1, f2, f3);
        buffer.pos(x1, y1, 0.0D).endVertex();
        buffer.pos(x2, y2, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture();
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
    }

    public static void open() {
        Minecraft.getInstance().displayGuiScreen(new GuiShapeCard(false));
    }

}
