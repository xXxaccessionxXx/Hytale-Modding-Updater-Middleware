# Hytale Modding Updater Middleware

An automated, intelligent, and seamless middleware agent designed to maintain, migrate, and update Hytale mods on the fly. Built to ensure server stability across game updates through dynamic bytecode manipulation and JSON asset padding.

This project was created with the help of Antigravity under the mind of **xXxaccessionxXx**.

## Features

- **Dynamic Bytecode Transformation:** Intercepts Java `.class` files in-memory before they are loaded by the Hytale Server to gracefully modify out-of-date method signatures or API calls.
- **Smart JSON/Asset Migration:** Automatically reads the `mods` folder upon boot, dynamically padding textures or updating outdated JSON models to the current Hytale spec without touching original source files.
- **Auto-Updater:** The agent automatically checks the GitHub Releases API for new versions. If an update is found, it downloads the newest version dynamically and replaces itself after the server closes.
- **Zero-Friction Instantiation:** Injects itself automatically via `JAVA_TOOL_OPTIONS` upon system boot, ensuring the middleware operates silently without requiring the user to launch a secondary application.
- **Sleek In-Game UI Overlay:** An elegant Java Swing overlay visually displays how many mods/assets the middleware processed right as you load into the world.
- **Automated Publishing Script:** A robust Python publishing script automates diff analysis, Git commits, and GitHub Releases seamlessly.

## Getting Started

### Installation
1. Download the latest `middleware-agent.jar` from the GitHub Releases page.
2. Clone or download this repository.
3. Run `install_global_hook.bat` to register the middleware JVM agent with your Windows environment.

### First Boot
Once the global hook is active, simply launch the Hytale Server natively (e.g., through your regular launcher or `hytale-server.jar`).
The middleware will:
1. Wake up and attach itself.
2. Check for updates.
3. Show the UI overlay if modifications (such as texture padding) occurred.

## Building from Source (For Developers)

To build the middleware agent from source, ensure you have the JDK installed, then run the build script:

```bash
build_agent.bat
```

This automates the compilation, pulls in `ASM` dependencies, and packages the runnable `.jar`.

### Publishing a New Release

If you intend to update the repo, use the built in Smart Publisher:

```bash
python publish.py
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
