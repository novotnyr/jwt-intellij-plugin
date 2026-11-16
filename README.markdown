# JWT (JSON Web Token) Analyzer for IntelliJ IDEA

Inspect, verify, and edit JSON Web Tokens in a dedicated IntelliJ IDEA tool window.

[Get it on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/9831-jwt-json-web-token-analyzer)

## What you can do

### Inspect a token

Paste a JWT into the tool window. The **Payload** tab shows decoded claims, and the **Header** tab shows every supplied header parameter. You can browse claims without supplying a secret or key. The encoded token highlights its payload segment, and malformed tokens show an error beside the input.

### Verify signatures and time claims

Enter a shared secret for HS256 or HS384, or an RSA public key for RS256, then select **Validate**. The plugin checks the signature and the optional `exp` and `nbf` time limits. It also checks that `exp`, `nbf`, and `iat` contain numeric timestamps when present, and marks invalid claims in the payload table.

### Create and edit JWTs

Use **New JWT** to create an HS256, HS384, or RS256 token, with an optional `iat` claim. With a signing secret or RSA key pair available, use the payload table controls to add, edit, or remove claims. The plugin re-signs the token after each change and provides editors for strings, numbers, booleans, timestamps, and raw JSON.

### Work with timestamps

Use the tool window's gear menu to switch between the **Unix timestamp**, **ISO**, and **Relative** display options. The timestamp editor includes **Now**, **+15 min**, and **-15 min** controls and preserves fractional timestamp values while editing.

### Copy or open the payload

Copy the whole payload as formatted JSON, or open it in a JSON scratch file formatted with the IDE's code style. You can also right-click a claim to copy its value or a `name=value` pair.

### Configure RS256 keys

In **Settings/Preferences → Tools → JWT**, choose PEM files for default RS256 public and private keys. The plugin validates the files before saving and uses them to prefill the RS256 key fields. You can also paste keys directly, including text with escaped newlines (`\n`).

## Install

Open **Settings/Preferences → Plugins → Marketplace**, search for **JWT (JSON Web Token) Analyzer**, and select **Install**.

For a local ZIP, download it from [GitHub Releases](https://github.com/novotnyr/jwt-intellij-plugin/releases) and use **Install Plugin from Disk** in the Plugins settings.

## Get started

1. Open **View → Tool Windows → JWT** and paste a token into the text area.
2. Enter the matching secret or RS256 public key, then select **Validate** to check its signature and claims.
3. To edit and re-sign a token, provide its signing secret or RS256 key pair and use the controls above the payload table. Use **New JWT** to create a token from scratch.

## Screenshots

**HS256:** The tool window shows decoded claims and a shared secret field. The expired `exp` claim is highlighted in red.

<img src="screenshot.png" alt="HS256 token with decoded claims and shared secret field" width="515">

**RS256:** The public key field is used to verify the signature. The private key field lets you edit claims and sign the updated token.

<img src="screenshot-keypair.png" alt="RS256 token with separate public and private key fields" width="515">
