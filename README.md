# HomePlugin

A simple hytale plugin for Hytale servers that adds `/home` and `/back`.

## Building

1. Place `HytaleServer.jar` in the `libs/` directory
2. Build with Gradle:

```bash
./gradlew build
```

The compiled plugin JAR will be located at `build/libs/HomePlugin.jar`.

## Installation

Copy the built JAR file to your Hytale server's `mods/` directory.

## Usage

This plugin is split into modules, each of which can be loaded independently. This can be configured from within 'config.json'.  

Added commands:

| Commands                     | permission                                | Description                                                 |
| ---------------------------- | ----------------------------------------- | ----------------------------------------------------------- |
| `/home`                      | com.wardlordruby.homeplugin.command.home  | Teleports player to their set home                          |
| `/home set`                  | com.wardlordruby.homeplugin.command.home  | Sets the players home to their current position             |
| [`/back`](#back)             | com.wardlordruby.homeplugin.command.back  | Teleports the player to the last entry in `TeleportHistory` |

## Back

The ability for players to use back on death can be configured globally via 'config.json' where it is defaulted to true. You can also grant
this functionality to groups or specific players by assigning them the permission 'com.wardlordruby.homeplugin.backOnDeath'.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
