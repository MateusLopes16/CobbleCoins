package com.mateuslopees.cobblecoins.client.screen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.client.ClientBankData;
import com.mateuslopees.cobblecoins.network.NetworkHandler;
import com.mateuslopees.cobblecoins.network.packet.PlayerShopActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class PlayerShopScreen extends Screen {
    private static final Gson GSON = new Gson();
    
    // Layout constants
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 280;
    private static final int ITEM_SIZE = 28;
    private static final int SHOP_ITEMS_PER_ROW = 6;
    private static final int SHOP_ITEMS_PER_PAGE = 12;
    private static final int INV_ITEMS_PER_ROW = 9;
    
    // Colors
    private static final int COLOR_BG_DARK = 0xF0141414;
    private static final int COLOR_BG_PANEL = 0xF01E1E1E;
    private static final int COLOR_BG_HOVER = 0xFF2A2A2A;
    private static final int COLOR_BORDER = 0xFF3D3D3D;
    private static final int COLOR_BORDER_LIGHT = 0xFF4A4A4A;
    private static final int COLOR_ACCENT = 0xFFFFD700;
    private static final int COLOR_ACCENT_BUY = 0xFF4CAF50;
    private static final int COLOR_ACCENT_SELL = 0xFFFF9800;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFAAAAAA;
    private static final int COLOR_TEXT_ERROR = 0xFFFF5555;
    private static final int COLOR_SELECTED = 0xFF3D5AFE;
    private static final int COLOR_PLAYER_SHOP = 0xFF9C27B0;
    private static final int COLOR_ADD_ITEM = 0xFF2196F3;
    
    private final List<ShopListing> listings = new ArrayList<>();
    private final String shopOwnerName;
    private final UUID shopOwnerId;
    private final boolean isOwnShop;
    
    private int guiLeft;
    private int guiTop;
    private int currentPage = 0;
    private ShopListing selectedListing = null;
    private int quantity = 1;
    private int hoveredItemIndex = -1;
    private int hoveredInventorySlot = -1;
    
    // Add item mode for own shop
    private boolean addItemMode = false;
    private int selectedInventorySlot = -1;
    private int addQuantity = 1;
    private long addPrice = 100;
    private String priceInput = "100";
    private boolean editingPrice = false;

    public PlayerShopScreen(String shopData) {
        super(Component.translatable("screen.cobblecoins.player_shop"));
        
        JsonObject json = GSON.fromJson(shopData, JsonObject.class);
        this.shopOwnerName = json.has("ownerName") ? json.get("ownerName").getAsString() : "Unknown";
        this.shopOwnerId = json.has("ownerId") ? UUID.fromString(json.get("ownerId").getAsString()) : null;
        this.isOwnShop = json.has("isOwnShop") && json.get("isOwnShop").getAsBoolean();
        
        parseListings(json);
    }

    private void parseListings(JsonObject json) {
        try {
            if (json.has("listings")) {
                JsonArray listingsArray = json.getAsJsonArray("listings");
                for (int i = 0; i < listingsArray.size(); i++) {
                    JsonObject entry = listingsArray.get(i).getAsJsonObject();
                    ShopListing listing = ShopListing.fromJson(entry);
                    listings.add(listing);
                }
            }
        } catch (Exception e) {
            CobbleCoins.LOGGER.error("Failed to parse player shop data", e);
        }
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    protected void renderMenuBackground(GuiGraphics graphics) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x88000000);
        
        drawPanel(graphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, COLOR_BG_DARK, COLOR_PLAYER_SHOP);
        
        renderHeader(graphics, mouseX, mouseY);
        
        if (isOwnShop && addItemMode) {
            renderAddItemMode(graphics, mouseX, mouseY);
        } else {
            renderShopItemsGrid(graphics, mouseX, mouseY);
            renderDetailPanel(graphics, mouseX, mouseY);
        }
        
        renderBalanceBar(graphics);
        
        super.render(graphics, mouseX, mouseY, partialTick);
        
        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int headerY = guiTop + 5;
        
        String title = isOwnShop ? "MY SHOP" : shopOwnerName + "'s Shop";
        graphics.drawString(font, title, guiLeft + 10, headerY + 5, COLOR_PLAYER_SHOP, false);
        
        if (isOwnShop) {
            // Add Item / Back button
            int addBtnX = guiLeft + 100;
            int addBtnY = headerY + 2;
            int addBtnWidth = addItemMode ? 50 : 60;
            boolean addHovered = isMouseOver(mouseX, mouseY, addBtnX, addBtnY, addBtnWidth, 16);
            int addColor = addItemMode ? COLOR_ACCENT_SELL : COLOR_ADD_ITEM;
            drawPanel(graphics, addBtnX, addBtnY, addBtnWidth, 16, addHovered ? (addItemMode ? 0xFFFFAB40 : 0xFF42A5F5) : addColor, addColor);
            String btnText = addItemMode ? "BACK" : "+ ADD";
            int btnTextX = addBtnX + (addBtnWidth - font.width(btnText)) / 2;
            graphics.drawString(font, btnText, btnTextX, addBtnY + 4, 0xFF000000, false);
        }
        
        String listingCount = listings.size() + " items";
        int countWidth = font.width(listingCount);
        graphics.drawString(font, listingCount, guiLeft + GUI_WIDTH - countWidth - 30, headerY + 5, COLOR_TEXT_DIM, false);
        
        int closeX = guiLeft + GUI_WIDTH - 18;
        int closeY = headerY + 2;
        boolean closeHovered = isMouseOver(mouseX, mouseY, closeX, closeY, 14, 14);
        drawPanel(graphics, closeX, closeY, 14, 14, closeHovered ? 0xFFAA0000 : COLOR_BG_PANEL, COLOR_BORDER);
        graphics.drawString(font, "X", closeX + 4, closeY + 3, COLOR_TEXT, false);
    }

    private void renderAddItemMode(GuiGraphics graphics, int mouseX, int mouseY) {
        Inventory playerInv = Minecraft.getInstance().player.getInventory();
        
        // Inventory panel
        int invX = guiLeft + 10;
        int invY = guiTop + 30;
        int invWidth = GUI_WIDTH - 20;
        int invHeight = 120;
        
        drawPanel(graphics, invX, invY, invWidth, invHeight, COLOR_BG_PANEL, COLOR_BORDER);
        graphics.drawString(font, "Select item from inventory:", invX + 5, invY + 5, COLOR_TEXT_DIM, false);
        
        hoveredInventorySlot = -1;
        int slotSize = 24;
        int startY = invY + 18;
        
        // Main inventory (27 slots) + Hotbar (9 slots) = 36 slots
        for (int i = 0; i < 36; i++) {
            int col = i % INV_ITEMS_PER_ROW;
            int row = i / INV_ITEMS_PER_ROW;
            
            int slotX = invX + 8 + col * (slotSize + 2);
            int slotY = startY + row * (slotSize + 2);
            
            ItemStack stack = i < 9 ? playerInv.getItem(i) : playerInv.getItem(i);
            boolean hovered = isMouseOver(mouseX, mouseY, slotX, slotY, slotSize, slotSize);
            boolean selected = i == selectedInventorySlot;
            
            if (hovered && !stack.isEmpty()) hoveredInventorySlot = i;
            
            int slotColor = selected ? COLOR_SELECTED : (hovered && !stack.isEmpty() ? COLOR_BG_HOVER : 0xFF2D2D2D);
            int borderColor = selected ? COLOR_ACCENT : (hovered ? COLOR_BORDER_LIGHT : COLOR_BORDER);
            drawPanel(graphics, slotX, slotY, slotSize, slotSize, slotColor, borderColor);
            
            if (!stack.isEmpty()) {
                int itemRenderX = slotX + (slotSize - 16) / 2;
                int itemRenderY = slotY + (slotSize - 16) / 2;
                graphics.renderItem(stack, itemRenderX, itemRenderY);
                
                if (stack.getCount() > 1) {
                    String countStr = String.valueOf(stack.getCount());
                    int countX = slotX + slotSize - font.width(countStr) - 2;
                    int countY = slotY + slotSize - 10;
                    graphics.drawString(font, countStr, countX, countY, COLOR_TEXT, false);
                }
            }
        }
        
        // Add item form
        int formX = guiLeft + 10;
        int formY = guiTop + 160;
        int formWidth = GUI_WIDTH - 20;
        int formHeight = 70;
        
        drawPanel(graphics, formX, formY, formWidth, formHeight, COLOR_BG_PANEL, COLOR_BORDER);
        
        if (selectedInventorySlot >= 0) {
            ItemStack selectedStack = playerInv.getItem(selectedInventorySlot);
            if (!selectedStack.isEmpty()) {
                // Selected item info
                graphics.renderItem(selectedStack, formX + 8, formY + 8);
                graphics.drawString(font, selectedStack.getHoverName().getString(), formX + 30, formY + 8, COLOR_TEXT, false);
                graphics.drawString(font, "Available: " + selectedStack.getCount(), formX + 30, formY + 20, COLOR_TEXT_DIM, false);
                
                // Quantity selector
                int qtyX = formX + 20;
                int qtyY = formY + 38;
                graphics.drawString(font, "Qty:", qtyX, qtyY + 4, COLOR_TEXT_DIM, false);
                
                int minusBtnX = qtyX + 25;
                boolean minusHovered = isMouseOver(mouseX, mouseY, minusBtnX, qtyY, 18, 18);
                drawPanel(graphics, minusBtnX, qtyY, 18, 18, minusHovered ? COLOR_BG_HOVER : COLOR_BG_DARK, COLOR_BORDER);
                graphics.drawString(font, "-", minusBtnX + 6, qtyY + 5, addQuantity > 1 ? COLOR_TEXT : COLOR_TEXT_DIM, false);
                
                int qtyDisplayX = minusBtnX + 20;
                drawPanel(graphics, qtyDisplayX, qtyY, 36, 18, COLOR_BG_DARK, COLOR_BORDER);
                String qtyStr = String.valueOf(addQuantity);
                int qtyTextX = qtyDisplayX + (36 - font.width(qtyStr)) / 2;
                graphics.drawString(font, qtyStr, qtyTextX, qtyY + 5, COLOR_TEXT, false);
                
                int plusBtnX = qtyDisplayX + 38;
                boolean plusHovered = isMouseOver(mouseX, mouseY, plusBtnX, qtyY, 18, 18);
                drawPanel(graphics, plusBtnX, qtyY, 18, 18, plusHovered ? COLOR_BG_HOVER : COLOR_BG_DARK, COLOR_BORDER);
                graphics.drawString(font, "+", plusBtnX + 6, qtyY + 5, addQuantity < selectedStack.getCount() ? COLOR_TEXT : COLOR_TEXT_DIM, false);
                
                // Max button
                int maxBtnX = plusBtnX + 22;
                boolean maxHovered = isMouseOver(mouseX, mouseY, maxBtnX, qtyY, 30, 18);
                drawPanel(graphics, maxBtnX, qtyY, 30, 18, maxHovered ? COLOR_BG_HOVER : COLOR_BG_DARK, COLOR_BORDER);
                graphics.drawString(font, "MAX", maxBtnX + 4, qtyY + 5, COLOR_TEXT_DIM, false);
                
                // Price input
                int priceX = formX + 170;
                graphics.drawString(font, "Price per item:", priceX, qtyY + 4, COLOR_TEXT_DIM, false);
                
                int priceInputX = priceX + 80;
                int priceInputWidth = 60;
                boolean priceHovered = isMouseOver(mouseX, mouseY, priceInputX, qtyY, priceInputWidth, 18);
                drawPanel(graphics, priceInputX, qtyY, priceInputWidth, 18, editingPrice ? COLOR_SELECTED : (priceHovered ? COLOR_BG_HOVER : COLOR_BG_DARK), editingPrice ? COLOR_ACCENT : COLOR_BORDER);
                String displayPrice = editingPrice ? priceInput + "_" : priceInput;
                graphics.drawString(font, displayPrice, priceInputX + 4, qtyY + 5, COLOR_ACCENT, false);
                
                // Total preview
                long totalValue = addPrice * addQuantity;
                graphics.drawString(font, "Total: " + formatPrice(totalValue) + " CobbleCoin", formX + 30, formY + 56, COLOR_ACCENT, false);
                
                // Add button
                int addBtnX = formX + formWidth - 70;
                int addBtnY = formY + 35;
                boolean canAdd = addQuantity > 0 && addPrice > 0;
                boolean addBtnHovered = isMouseOver(mouseX, mouseY, addBtnX, addBtnY, 60, 28);
                int addBtnColor = canAdd ? (addBtnHovered ? 0xFF66BB6A : COLOR_ACCENT_BUY) : 0xFF555555;
                drawPanel(graphics, addBtnX, addBtnY, 60, 28, addBtnColor, addBtnColor);
                String addText = "ADD";
                int addTextX = addBtnX + (60 - font.width(addText)) / 2;
                graphics.drawString(font, addText, addTextX, addBtnY + 10, canAdd ? 0xFF000000 : COLOR_TEXT_DIM, false);
            }
        } else {
            graphics.drawString(font, "Click an item in your inventory to add it to your shop", formX + 10, formY + 30, COLOR_TEXT_DIM, false);
        }
    }

    private void renderShopItemsGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int gridX = guiLeft + 10;
        int gridY = guiTop + 30;
        int gridWidth = GUI_WIDTH - 20;
        int gridHeight = 100;
        
        drawPanel(graphics, gridX, gridY, gridWidth, gridHeight, COLOR_BG_PANEL, COLOR_BORDER);
        
        if (listings.isEmpty()) {
            String emptyText = isOwnShop ? "No items listed. Click + ADD to add items!" : "This shop has no items for sale.";
            int textWidth = font.width(emptyText);
            graphics.drawString(font, emptyText, gridX + (gridWidth - textWidth) / 2, gridY + 45, COLOR_TEXT_DIM, false);
            return;
        }
        
        int startIndex = currentPage * SHOP_ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + SHOP_ITEMS_PER_PAGE, listings.size());
        
        hoveredItemIndex = -1;
        
        for (int i = startIndex; i < endIndex; i++) {
            ShopListing listing = listings.get(i);
            int localIndex = i - startIndex;
            int col = localIndex % SHOP_ITEMS_PER_ROW;
            int row = localIndex / SHOP_ITEMS_PER_ROW;
            
            int itemX = gridX + 10 + col * (ITEM_SIZE + 10);
            int itemY = gridY + 10 + row * (ITEM_SIZE + 18);
            
            boolean hovered = isMouseOver(mouseX, mouseY, itemX, itemY, ITEM_SIZE, ITEM_SIZE);
            boolean selected = listing == selectedListing;
            
            if (hovered) hoveredItemIndex = i;
            
            int slotColor = selected ? COLOR_SELECTED : (hovered ? COLOR_BG_HOVER : 0xFF2D2D2D);
            int borderColor = selected ? COLOR_ACCENT : (hovered ? COLOR_BORDER_LIGHT : COLOR_BORDER);
            drawPanel(graphics, itemX, itemY, ITEM_SIZE, ITEM_SIZE, slotColor, borderColor);
            
            ItemStack stack = listing.getItemStack();
            int itemRenderX = itemX + (ITEM_SIZE - 16) / 2;
            int itemRenderY = itemY + (ITEM_SIZE - 16) / 2;
            graphics.renderItem(stack, itemRenderX, itemRenderY);
            
            if (listing.amount > 1) {
                String qtyStr = String.valueOf(listing.amount);
                graphics.fill(itemX, itemY, itemX + font.width(qtyStr) + 2, itemY + 10, 0xCC000000);
                graphics.drawString(font, qtyStr, itemX + 1, itemY + 1, COLOR_TEXT, false);
            }
            
            String priceStr = formatCompactPrice(listing.price);
            int priceWidth = font.width(priceStr);
            graphics.fill(itemX + ITEM_SIZE - priceWidth - 2, itemY + ITEM_SIZE - 10, itemX + ITEM_SIZE, itemY + ITEM_SIZE, 0xCC000000);
            graphics.drawString(font, priceStr, itemX + ITEM_SIZE - priceWidth - 1, itemY + ITEM_SIZE - 9, COLOR_ACCENT, false);
        }
        
        int totalPages = (int) Math.ceil(listings.size() / (double) SHOP_ITEMS_PER_PAGE);
        if (totalPages > 1) {
            String pageStr = (currentPage + 1) + "/" + totalPages;
            int navY = gridY + gridHeight - 12;
            
            boolean canPrev = currentPage > 0;
            graphics.drawString(font, canPrev ? "<" : "", gridX + 5, navY, canPrev ? COLOR_ACCENT : COLOR_TEXT_DIM, false);
            
            int pageWidth = font.width(pageStr);
            graphics.drawString(font, pageStr, gridX + (gridWidth - pageWidth) / 2, navY, COLOR_TEXT_DIM, false);
            
            boolean canNext = currentPage < totalPages - 1;
            graphics.drawString(font, canNext ? ">" : "", gridX + gridWidth - 15, navY, canNext ? COLOR_ACCENT : COLOR_TEXT_DIM, false);
        }
    }

    private void renderDetailPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelX = guiLeft + 10;
        int panelY = guiTop + 140;
        int panelWidth = GUI_WIDTH - 20;
        int panelHeight = 90;
        
        drawPanel(graphics, panelX, panelY, panelWidth, panelHeight, COLOR_BG_PANEL, COLOR_BORDER);
        
        if (selectedListing == null) {
            String selectText = "Select an item to " + (isOwnShop ? "manage" : "buy");
            int textWidth = font.width(selectText);
            graphics.drawString(font, selectText, panelX + (panelWidth - textWidth) / 2, panelY + 40, COLOR_TEXT_DIM, false);
            return;
        }
        
        ItemStack stack = selectedListing.getItemStack();
        graphics.renderItem(stack, panelX + 8, panelY + 8);
        graphics.drawString(font, selectedListing.getDisplayName(), panelX + 30, panelY + 8, COLOR_TEXT, false);
        graphics.drawString(font, "Price: " + formatPrice(selectedListing.price) + " per item", panelX + 30, panelY + 22, COLOR_ACCENT, false);
        graphics.drawString(font, "Available: " + selectedListing.amount, panelX + 30, panelY + 36, COLOR_TEXT_DIM, false);
        
        if (!isOwnShop) {
            int qtyX = panelX + 20;
            int qtyY = panelY + 55;
            
            graphics.drawString(font, "Qty:", qtyX, qtyY + 4, COLOR_TEXT_DIM, false);
            
            int minusBtnX = qtyX + 25;
            boolean minusHovered = isMouseOver(mouseX, mouseY, minusBtnX, qtyY, 18, 18);
            drawPanel(graphics, minusBtnX, qtyY, 18, 18, minusHovered ? COLOR_BG_HOVER : COLOR_BG_DARK, COLOR_BORDER);
            graphics.drawString(font, "-", minusBtnX + 6, qtyY + 5, quantity > 1 ? COLOR_TEXT : COLOR_TEXT_DIM, false);
            
            int qtyDisplayX = minusBtnX + 20;
            drawPanel(graphics, qtyDisplayX, qtyY, 36, 18, COLOR_BG_DARK, COLOR_BORDER);
            String qtyStr = String.valueOf(quantity);
            int qtyTextX = qtyDisplayX + (36 - font.width(qtyStr)) / 2;
            graphics.drawString(font, qtyStr, qtyTextX, qtyY + 5, COLOR_TEXT, false);
            
            int plusBtnX = qtyDisplayX + 38;
            boolean plusHovered = isMouseOver(mouseX, mouseY, plusBtnX, qtyY, 18, 18);
            drawPanel(graphics, plusBtnX, qtyY, 18, 18, plusHovered ? COLOR_BG_HOVER : COLOR_BG_DARK, COLOR_BORDER);
            graphics.drawString(font, "+", plusBtnX + 6, qtyY + 5, quantity < selectedListing.amount ? COLOR_TEXT : COLOR_TEXT_DIM, false);
            
            long total = selectedListing.price * quantity;
            boolean canAfford = ClientBankData.getBalance() >= total;
            boolean canBuy = canAfford && quantity <= selectedListing.amount;
            
            graphics.drawString(font, "Total: " + formatPrice(total), qtyX + 130, qtyY + 4, canAfford ? COLOR_ACCENT : COLOR_TEXT_ERROR, false);
            
            int buyX = panelX + panelWidth - 70;
            int buyY = panelY + 50;
            boolean buyHovered = isMouseOver(mouseX, mouseY, buyX, buyY, 60, 30);
            int buyColor = canBuy ? (buyHovered ? 0xFF66BB6A : COLOR_ACCENT_BUY) : 0xFF555555;
            
            drawPanel(graphics, buyX, buyY, 60, 30, buyColor, buyColor);
            String buyText = "BUY";
            int buyTextX = buyX + (60 - font.width(buyText)) / 2;
            graphics.drawString(font, buyText, buyTextX, buyY + 11, canBuy ? 0xFF000000 : COLOR_TEXT_DIM, false);
            
            if (!canAfford) {
                graphics.drawString(font, "Insufficient funds!", panelX + 200, panelY + 70, COLOR_TEXT_ERROR, false);
            }
        } else {
            int removeX = panelX + panelWidth - 70;
            int removeY = panelY + 50;
            boolean removeHovered = isMouseOver(mouseX, mouseY, removeX, removeY, 65, 25);
            int removeColor = removeHovered ? 0xFFCC0000 : 0xFFAA0000;
            
            drawPanel(graphics, removeX, removeY, 65, 25, removeColor, removeColor);
            String removeText = "REMOVE";
            int removeTextX = removeX + (65 - font.width(removeText)) / 2;
            graphics.drawString(font, removeText, removeTextX, removeY + 8, COLOR_TEXT, false);
        }
    }

    private void renderBalanceBar(GuiGraphics graphics) {
        int barX = guiLeft;
        int barY = guiTop + GUI_HEIGHT + 2;
        int barWidth = GUI_WIDTH;
        int barHeight = 18;
        
        drawPanel(graphics, barX, barY, barWidth, barHeight, COLOR_BG_DARK, COLOR_BORDER);
        
        String balanceText = "Balance: " + ClientBankData.getFormattedBalance() + " CobbleCoin";
        graphics.drawString(font, balanceText, barX + 10, barY + 5, COLOR_TEXT, false);
        
        if (selectedListing != null && !isOwnShop) {
            long total = selectedListing.price * quantity;
            String cartText = "Total: " + formatPrice(total) + " (" + quantity + "x " + selectedListing.getDisplayName() + ")";
            int cartTextWidth = font.width(cartText);
            graphics.drawString(font, cartText, barX + barWidth - cartTextWidth - 10, barY + 5, COLOR_ACCENT, false);
        }
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (addItemMode && hoveredInventorySlot >= 0) {
            Inventory playerInv = Minecraft.getInstance().player.getInventory();
            ItemStack stack = playerInv.getItem(hoveredInventorySlot);
            if (!stack.isEmpty()) {
                graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        } else if (!addItemMode && hoveredItemIndex >= 0 && hoveredItemIndex < listings.size()) {
            ShopListing listing = listings.get(hoveredItemIndex);
            
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(listing.getDisplayName()));
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Price: " + formatPrice(listing.price) + " each").withStyle(style -> style.withColor(COLOR_ACCENT)));
            tooltip.add(Component.literal("Stock: " + listing.amount).withStyle(style -> style.withColor(COLOR_TEXT_DIM)));
            tooltip.add(Component.literal("Click to select").withStyle(style -> style.withColor(0xFF888888)));
            
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        
        // Close button
        int closeX = guiLeft + GUI_WIDTH - 18;
        int closeY = guiTop + 5 + 2;
        if (isMouseOver(mx, my, closeX, closeY, 14, 14)) {
            this.onClose();
            return true;
        }
        
        // Add/Back button for own shop
        if (isOwnShop) {
            int addBtnX = guiLeft + 100;
            int addBtnY = guiTop + 5 + 2;
            int addBtnWidth = addItemMode ? 50 : 60;
            if (isMouseOver(mx, my, addBtnX, addBtnY, addBtnWidth, 16)) {
                addItemMode = !addItemMode;
                selectedInventorySlot = -1;
                addQuantity = 1;
                addPrice = 100;
                priceInput = "100";
                editingPrice = false;
                return true;
            }
        }
        
        if (addItemMode) {
            return handleAddItemModeClick(mx, my);
        } else {
            return handleShopModeClick(mx, my);
        }
    }

    private boolean handleAddItemModeClick(int mx, int my) {
        Inventory playerInv = Minecraft.getInstance().player.getInventory();
        
        // Inventory slot clicks
        int invX = guiLeft + 10;
        int invY = guiTop + 30;
        int slotSize = 24;
        int startY = invY + 18;
        
        for (int i = 0; i < 36; i++) {
            int col = i % INV_ITEMS_PER_ROW;
            int row = i / INV_ITEMS_PER_ROW;
            int slotX = invX + 8 + col * (slotSize + 2);
            int slotY = startY + row * (slotSize + 2);
            
            if (isMouseOver(mx, my, slotX, slotY, slotSize, slotSize)) {
                ItemStack stack = playerInv.getItem(i);
                if (!stack.isEmpty()) {
                    selectedInventorySlot = i;
                    addQuantity = 1;
                    editingPrice = false;
                }
                return true;
            }
        }
        
        // Form controls
        if (selectedInventorySlot >= 0) {
            ItemStack selectedStack = playerInv.getItem(selectedInventorySlot);
            if (!selectedStack.isEmpty()) {
                int formX = guiLeft + 10;
                int formY = guiTop + 160;
                int formWidth = GUI_WIDTH - 20;
                
                int qtyX = formX + 20;
                int qtyY = formY + 38;
                
                // Minus quantity
                int minusBtnX = qtyX + 25;
                if (isMouseOver(mx, my, minusBtnX, qtyY, 18, 18)) {
                    if (addQuantity > 1) addQuantity--;
                    return true;
                }
                
                // Plus quantity
                int plusBtnX = minusBtnX + 20 + 36 + 2;
                if (isMouseOver(mx, my, plusBtnX, qtyY, 18, 18)) {
                    if (addQuantity < selectedStack.getCount()) addQuantity++;
                    return true;
                }
                
                // Max button
                int maxBtnX = plusBtnX + 22;
                if (isMouseOver(mx, my, maxBtnX, qtyY, 30, 18)) {
                    addQuantity = selectedStack.getCount();
                    return true;
                }
                
                // Price input click
                int priceX = formX + 170;
                int priceInputX = priceX + 80;
                if (isMouseOver(mx, my, priceInputX, qtyY, 60, 18)) {
                    editingPrice = true;
                    return true;
                } else {
                    editingPrice = false;
                }
                
                // Add button
                int addBtnX = formX + formWidth - 70;
                int addBtnY = formY + 35;
                if (isMouseOver(mx, my, addBtnX, addBtnY, 60, 28)) {
                    executeAddItem();
                    return true;
                }
            }
        }
        
        editingPrice = false;
        return false;
    }

    private boolean handleShopModeClick(int mx, int my) {
        // Item clicks
        if (hoveredItemIndex >= 0 && hoveredItemIndex < listings.size()) {
            selectedListing = listings.get(hoveredItemIndex);
            quantity = 1;
            return true;
        }
        
        // Detail panel interactions
        if (selectedListing != null) {
            int panelX = guiLeft + 10;
            int panelY = guiTop + 140;
            int panelWidth = GUI_WIDTH - 20;
            
            if (!isOwnShop) {
                int qtyX = panelX + 20;
                int qtyY = panelY + 55;
                
                int minusBtnX = qtyX + 25;
                if (isMouseOver(mx, my, minusBtnX, qtyY, 18, 18)) {
                    if (quantity > 1) quantity--;
                    return true;
                }
                
                int plusBtnX = minusBtnX + 20 + 36 + 2;
                if (isMouseOver(mx, my, plusBtnX, qtyY, 18, 18)) {
                    quantity = Math.min(quantity + 1, selectedListing.amount);
                    return true;
                }
                
                int buyX = panelX + panelWidth - 70;
                int buyY = panelY + 50;
                if (isMouseOver(mx, my, buyX, buyY, 60, 30)) {
                    executePurchase();
                    return true;
                }
            } else {
                int removeX = panelX + panelWidth - 70;
                int removeY = panelY + 50;
                if (isMouseOver(mx, my, removeX, removeY, 65, 25)) {
                    executeRemove();
                    return true;
                }
            }
        }
        
        // Page navigation
        int gridX = guiLeft + 10;
        int gridY = guiTop + 30;
        int gridWidth = GUI_WIDTH - 20;
        int gridHeight = 100;
        
        int totalPages = (int) Math.ceil(listings.size() / (double) SHOP_ITEMS_PER_PAGE);
        if (totalPages > 1) {
            int navY = gridY + gridHeight - 12;
            
            if (isMouseOver(mx, my, gridX + 5, navY - 2, 15, 12) && currentPage > 0) {
                currentPage--;
                return true;
            }
            
            if (isMouseOver(mx, my, gridX + gridWidth - 15, navY - 2, 15, 12) && currentPage < totalPages - 1) {
                currentPage++;
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingPrice) {
            if (keyCode == 259) { // Backspace
                if (!priceInput.isEmpty()) {
                    priceInput = priceInput.substring(0, priceInput.length() - 1);
                    updatePriceFromInput();
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter
                editingPrice = false;
                return true;
            } else if (keyCode == 256) { // Escape
                editingPrice = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingPrice) {
            if (Character.isDigit(chr) && priceInput.length() < 10) {
                priceInput += chr;
                updatePriceFromInput();
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    private void updatePriceFromInput() {
        try {
            addPrice = priceInput.isEmpty() ? 0 : Long.parseLong(priceInput);
        } catch (NumberFormatException e) {
            addPrice = 0;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!addItemMode) {
            int gridX = guiLeft + 10;
            int gridY = guiTop + 30;
            int gridWidth = GUI_WIDTH - 20;
            int gridHeight = 100;
            
            if (isMouseOver((int) mouseX, (int) mouseY, gridX, gridY, gridWidth, gridHeight)) {
                int totalPages = (int) Math.ceil(listings.size() / (double) SHOP_ITEMS_PER_PAGE);
                if (scrollY > 0 && currentPage > 0) {
                    currentPage--;
                } else if (scrollY < 0 && currentPage < totalPages - 1) {
                    currentPage++;
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void executeAddItem() {
        if (selectedInventorySlot < 0 || addQuantity <= 0 || addPrice <= 0) return;
        
        Inventory playerInv = Minecraft.getInstance().player.getInventory();
        ItemStack stack = playerInv.getItem(selectedInventorySlot);
        if (stack.isEmpty() || addQuantity > stack.getCount()) return;
        
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        
        NetworkHandler.sendToServer(new PlayerShopActionPacket(
                PlayerShopActionPacket.Action.LIST_ITEM,
                itemId,
                addQuantity,
                addPrice
        ));
        
        // Reset form
        selectedInventorySlot = -1;
        addQuantity = 1;
        addPrice = 100;
        priceInput = "100";
        editingPrice = false;
    }

    private void executePurchase() {
        if (selectedListing == null || isOwnShop) return;
        
        long total = selectedListing.price * quantity;
        
        if (ClientBankData.getBalance() >= total && quantity <= selectedListing.amount) {
            NetworkHandler.sendToServer(new PlayerShopActionPacket(
                    PlayerShopActionPacket.Action.BUY_FROM_PLAYER,
                    selectedListing.listingId,
                    quantity,
                    0
            ));
            
            ClientBankData.setBalance(ClientBankData.getBalance() - total);
            selectedListing.amount -= quantity;
            if (selectedListing.amount <= 0) {
                listings.remove(selectedListing);
                selectedListing = null;
            }
            quantity = 1;
        }
    }

    private void executeRemove() {
        if (selectedListing == null || !isOwnShop) return;
        
        NetworkHandler.sendToServer(new PlayerShopActionPacket(
                PlayerShopActionPacket.Action.REMOVE_ITEM,
                selectedListing.itemId,
                0,
                0
        ));
        
        listings.remove(selectedListing);
        selectedListing = null;
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int bgColor, int borderColor) {
        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x, y, x + width, y + 1, borderColor);
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        graphics.fill(x, y, x + 1, y + height, borderColor);
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private String formatPrice(long price) {
        if (price >= 1_000_000) {
            return String.format("%.1fM", price / 1_000_000.0);
        } else if (price >= 1000) {
            return String.format("%.1fK", price / 1000.0);
        }
        return String.valueOf(price);
    }

    private String formatCompactPrice(long price) {
        if (price >= 1_000_000) {
            return String.format("%.0fM", price / 1_000_000.0);
        } else if (price >= 1000) {
            return String.format("%.0fK", price / 1000.0);
        }
        return String.valueOf(price);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class ShopListing {
        String listingId;
        String itemId;
        String displayName;
        int amount;
        long price;
        String sellerName;
        UUID sellerId;
        ItemStack cachedStack;

        static ShopListing fromJson(JsonObject json) {
            ShopListing listing = new ShopListing();
            listing.listingId = json.has("id") ? json.get("id").getAsString() : UUID.randomUUID().toString();
            listing.itemId = json.has("itemId") ? json.get("itemId").getAsString() : "";
            listing.amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
            listing.price = json.has("price") ? json.get("price").getAsLong() : 0;
            listing.sellerName = json.has("sellerName") ? json.get("sellerName").getAsString() : "Unknown";
            listing.sellerId = json.has("sellerId") ? UUID.fromString(json.get("sellerId").getAsString()) : null;
            
            try {
                ResourceLocation itemLoc = ResourceLocation.parse(listing.itemId);
                Item item = BuiltInRegistries.ITEM.get(itemLoc);
                listing.displayName = item.getDescription().getString();
                listing.cachedStack = new ItemStack(item);
            } catch (Exception e) {
                listing.displayName = listing.itemId;
                listing.cachedStack = new ItemStack(Items.BARRIER);
            }
            
            return listing;
        }

        String getDisplayName() {
            return displayName != null ? displayName : itemId;
        }

        ItemStack getItemStack() {
            return cachedStack != null ? cachedStack : new ItemStack(Items.BARRIER);
        }
    }
}
