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

### Added commands

| Commands                     | permission                                  | Description                                                  |
| ---------------------------- | ------------------------------------------- | ------------------------------------------------------------ |
| [`/home`](#home)             | `com.wardlordruby.homeplugin.command.home`  | Teleports player to their default home                       |
| `/home <name>`               | `com.wardlordruby.homeplugin.command.home`  | Teleports player to their specified home                     |
| `/home set <name>`           | `com.wardlordruby.homeplugin.command.home`  | Sets the players home to their current position              |
| `/home default <name>`       | `com.wardlordruby.homeplugin.command.home`  | Sets the players specified home as default                   |
| `/home remove <name>`        | `com.wardlordruby.homeplugin.command.home`  | Removes the players specified home                           |
| `/home list`                 | `com.wardlordruby.homeplugin.command.home`  | Lists the players set homes, use --verbose for detailed list |
| [`/back`](#back)             | `com.wardlordruby.homeplugin.command.back`  | Teleports the player to the last entry in `TeleportHistory`  |

## Home

Players with permission to use `/home` can set a number of homes equal to `baseHomeCount` by default. 

To allow players to have more homes, grant them a rank permission in the format:
`com.wardlordruby.homeplugin.config.homerank.<number>`

The `<number>` corresponds to the position in the `homeCountByRank` list. For example:
- `homerank.1` grants the number of homes specified in the first value of `homeCountByRank`
- `homerank.2` grants the number specified in the second value
- And so on...

You can add additional ranks by extending the `homeCountByRank` list with more values.

## Back

Players can use `/back` after death if the `backOnDeath` setting in `config.json` is set to `true` (which is the default).

To override this on a per-player or per-group basis first set `backOnDeath` to `false` then, grant the permission:
`com.wardlordruby.homeplugin.config.backOnDeath`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
