# Player2NPC Paper 26.2

A Paper 26.2 server-plugin project inspired by the Player2NPC companion concept.

## Target

- Minecraft / Paper: **26.2**
- Java: **25**
- Build system: **Gradle**

## Current plugin features

- Player-like Mannequin NPC appearance using the owner's profile/skin
- Companion creation/removal/listing
- Follow and stop-follow behavior
- Persistent companion storage
- Waypoints and memory entries
- Task modes: combat, mining, farming, crafting, smelting
- `/p2npc` command and tab completion

> This is an independent Paper implementation. It is not the original Fabric mod and does not include the original mod's proprietary runtime code.

## Build automatically on GitHub

1. Create a GitHub repository named `Player2NPC-Paper-26.2`.
2. Upload this project.
3. Open **Actions**.
4. Run **Build Player2NPC Paper 26.2** (or push to `main`).
5. Open the completed workflow run.
6. Download the **Player2NPC-Paper-26.2** artifact.
7. Put the generated `.jar` into your Paper server's `plugins/` folder.
8. Restart the server.

## Create a release JAR

After uploading the project, create a tag such as `v0.1.0` and push it. The release workflow will build the JAR and attach it to a GitHub Release.

## Local build

Requires Java 25 and Gradle 9.1+.

```bash
gradle build
```

The JAR is created in `build/libs/`.

## Commands

```text
/p2npc create <name>
/p2npc list
/p2npc follow <name>
/p2npc stop <name>
/p2npc remove <name>
/p2npc task <name> <combat|mining|farming|crafting|smelting>
/p2npc waypoint <name> <waypoint>
/p2npc memory <name> <text>
/p2npc reload
```

## Security

Never commit Telegram bot tokens, API keys, passwords, or server secrets to GitHub. Keep real server configuration outside the repository.
