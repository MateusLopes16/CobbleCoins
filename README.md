# 💰 CobbleCoins - Cobblemon Economy & Shops Addon

A lightweight yet powerful economy system designed for **Cobblemon** servers.  
This addon introduces **CobbleCoins**, currency items, a global shop, player shops, and multiple ways to earn and spend money while progressing through your Pokémon adventure.

[![GitHub](https://img.shields.io/badge/GitHub-MateusLopes16-blue?logo=github)](https://github.com/MateusLopes16/CobbleCoins)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)](https://www.minecraft.net/)
[![Cobblemon](https://img.shields.io/badge/Cobblemon-1.17.1-orange)](https://cobblemon.com/)

---

## 📋 Table of Contents
- [Features Overview](#-features-overview)
- [Installation](#-installation)
- [Earning CobbleCoins](#-earning-cobblecoins)
- [Shop System](#-shop-system)
- [Trading](#-trades)
- [Commands](#-commands)
- [Configuration](#-server-admin-configuration)
- [Support](#-support)
- [License](#-license)

---

## ⭐ Features Overview

This addon adds a complete economy system centered around **CobbleCoins**, earned through Pokémon captures, defeats, and Pokédex progression.

### For Players:
- 💵 Earn money naturally while playing  
- 🛒 Access a full shop interface  
- 💱 Buy and sell items to the server  
- 🏪 Create and manage their own personal shops  
- 🤝 Trade items and money with other players
- 📊 View balance with HUD display or commands

### For Server Admins:
- ⚙️ Fully customizable prices  
- 📁 Custom shop categories  
- 💎 Configurable rewards  
- 🪙 Multiple currency support  
- 🎨 Complete shop content control  

---

## 📦 Installation

1. **Requirements:**
   - Minecraft Server 1.21.1
   - Cobblemon 1.17.1
   - NeoForge (depending on your setup)

2. **Installation Steps:**
   - Download the latest release from the [Releases page](https://github.com/MateusLopes16/CobbleCoins/releases)
   - Place the `.jar` file in your server's `mods` folder
   - Restart your server
   - Configure the addon (see [Configuration](#-server-admin-configuration))

---

# 🪙 Earning CobbleCoins

## Capture & Pokédex Rewards
- **100** CobbleCoins for each new Pokédex entry  
- **10** CobbleCoins for each Pokémon capture  
- **+500** bonus for every **50** Pokédex entries  
- **+50** bonus for every **50** captures  
- **+1000** bonus for every **100** Pokédex entries  
- **+100** bonus for every **100** captures  

## Defeat Rewards
Each Pokémon defeat grants coins based on level:

$$
\text{Reward} = \text{Pokémon Level} \times 1.5 + \text{baseReward}
$$

> **Note:** On capture and defeat, money is directly deposited into the player's bank account. A physical CobbleCoin item (`cobblecoins:cobblecoin`) is also provided if needed, which can be stacked to 1000 and consumed on right-click.

---

# 🏪 Shop System

## `/cshop`
Opens the main shop interface, which contains:

### 🌍 Server Shop
A fixed buy/sell shop defined by the server.  
Admins can configure:
- Categories  
- Items  
- Prices  
- Currency types  

### 👤 Player Shops
Each player has their own shop where they can:
- List items for sale  
- Set custom prices  
- Sell to other players  
- Sell directly to the server’s sell shop  

Items listed for sale are removed from the player’s inventory and returned when removed from the shop.

---

# Trades

Players can also use `/trade <playerName>` and `/tradeaccept` or `/tradedecline` to trade to a specific player they can trade items from their inventory to items or money each player needs to approuve the transaction to make it happen

---

# 👀 Viewing Your Money

By default, a small HUD box appears in the **bottom-right corner** showing the player’s current CobbleCoins.
Box can be disabled in the config

Players can also:
- Use `/money`  
- Open the shop interface  

---

# 🛠 Server Admin Configuration

All server shop settings can be customized in the configuration files located at:

```
/config/<config_path>
```

## Customizable Settings:
- 📁 Shop categories  
- 🛍️ Items that can be bought or sold  
- 💰 Item prices  
- 🪙 Currency types used  
- 🎁 Capture and defeat rewards
- 📊 HUD display settings

---

## 🏪 Server Shop Configuration

### Example: Server Shop Config

```json
{
    "server_buy_shop": [
        {
            "category": "category_name",
            "content": [
                {
                    "item_1": "minecraft:stone_pickaxe",
                    "price": 1000,
                    "currency": "cobblecoins:cobblecoins"
                }
            ]
        }
    ],
    "server_sell_shop": [
        {
            "category": "category_name",
            "content": [
                {
                    "item_1": "cobblecoins:cobblecoins",
                    "price": 1000,
                    "currency": "minecraft:stone_pickaxe"
                }
            ]
        }
    ]
}
```

---

## 🎁 Capture Reward Configuration

Capture and defeat rewards can be customized to fit your server's economy:

```json
{
    "rewardoncatch": 10,
    "rewardonpokedexentry": 100,
    "rewardon50capture": 50,
    "rewardon100capture": 100,
    "rewardon50pokedexentries": 500,
    "rewardon100pokedexentries": 1000,
    "rewardonpokemondeathmultiplyer": 1.5,
    "baserewardonpokemondeath": 0,
    "ismoneyboxvisible": true
}
```

### Configuration Options:
- `rewardoncatch`: Coins earned per Pokémon capture
- `rewardonpokedexentry`: Bonus for new Pokédex entries
- `rewardon50capture` / `rewardon100capture`: Milestone bonuses
- `rewardon50pokedexentries` / `rewardon100pokedexentries`: Pokédex milestone bonuses
- `rewardonpokemondeathmultiplyer`: Multiplier for defeat rewards
- `baserewardonpokemondeath`: Base reward for defeating Pokémon
- `ismoneyboxvisible`: Toggle HUD display on/off

---

## 🆘 Support

If you encounter any issues or have suggestions:
- 🐛 [Report a bug](https://github.com/MateusLopes16/CobbleCoins/issues)
- 💡 [Request a feature](https://github.com/MateusLopes16/CobbleCoins/issues)
- 💬 Join our community discussions

---

## 📄 License

This project is open source. Please check the repository for license details.

---

## 🙏 Credits

Created and maintained by [MateusLopes16](https://github.com/MateusLopes16)

Built for the Cobblemon community 💜

---

**Enjoy your Cobblemon economy! Happy trading! 💰🎮**