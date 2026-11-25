# HavocEvent

A Paper 1.21.1 plugin that runs a configurable countdown, title/subtitle announcement, announcement sound, and an expanding world border.

## Commands
- `/hv start` — starts the countdown and event sequence.
- `/hv reload` — reloads `config.yml`.

Permission: `havocevent.admin` (default: operators).

## Build locally
Requires Java 21 and Maven:

```bash
mvn clean package
```

The plugin JAR is generated at `target/HavocEvent.jar`.

## Build with GitHub Actions
1. Create a GitHub repository and upload/push the contents of this folder.
2. Open the repository's **Actions** tab.
3. Run **Build HavocEvent** (or push a commit to trigger it automatically).
4. Download the `HavocEvent` artifact from the completed workflow run.

Edit `src/main/resources/config.yml` before building to set your preferred title, sounds, world, border size, and timing. After installing, the live config is at `plugins/HavocEvent/config.yml`.
