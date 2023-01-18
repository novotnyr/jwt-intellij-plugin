# JWT (JSON Web Token) Analyzer for IntelliJ IDEA

An IntelliJ IDEA plugin for JSON Web Tokens (JWT)

## Supported features

*   Visualize JWT contents in a tabular form
*   Verify signature (HS384, HS256 and RS256 support)
*   Verify validity of timestamp-based claims
*   Visualize timestamp-based claims in unix / ISO / human relative format
*   Edit claim values: dedicated editor for string, timestamp, boolean, numeric and raw JSON values

## Installing

1. Download the [latest release](https://github.com/novotnyr/jwt-intellij-plugin/releases) as a ZIP file.
2. From *IntelliJ* settings, drill down to *Plugins* and install via **Install Plug-in From disk** button.

## Screenshots
![Screenshot](screenshot.png)

## Usage

After installation, toggle the JWT tool window via a button in the right section of the IDE window.

![JWT Tool Window](screenshot-intellij-right-panel.png)

Alternatively, you can display the JWT tool window via menu *View → Tool Windows → JWT*.
