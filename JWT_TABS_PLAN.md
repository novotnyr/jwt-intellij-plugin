# Split Parsed JWT into Payload and Header Tabs

## Goal

Make the payload the primary view of a parsed JWT and move header parameters into a secondary tab.

## Proposed UI

- Keep the tool window toolbar and encoded JWT input above the parsed content.
- Add two top-aligned tabs: **Payload** first and selected by default, then **Header**. Creating or pasting a new valid JWT, and using **Reset**, selects Payload.
- **Payload** contains the current claims table and its add, remove, edit, and copy actions.
- **Header** contains a read-only, scrollable **Claim / Value** table. It displays every parameter in the encoded header, including custom parameters, in the order found in the header JSON. Strings display as text; arrays and objects display on one line. A context menu copies the full value when a cell is clipped. There are no header add, edit, or remove controls.
- Keep the signing key or secret controls shared below the tabs, visible on either tab. Keep **Validate** at the bottom of the tool window.
- Remove the current header rows above the payload and the redundant **Payload** titled border. Keep the tab content free of an enclosing frame.
- When there is no valid token, keep both tabs visible, clear their tables, and show a short empty-state message. Invalid input preserves the shared signing key or secret values.

## Header behavior and RFC guidance

RFC 7519 §5 describes the JOSE header as metadata about cryptographic operations and optional token properties. Its `typ` parameter is optional; when present, `JWT` is the recommended value. Its `cty` parameter is normally absent. The table shows only parameters supplied by the token. RFC 7515 defines `alg` as the JWS algorithm parameter and `kid` as an optional key identifier.

The Header tab reports decoded values; an `alg` value does not itself indicate that signature validation succeeded. Shared validation controls remain available while Header is selected. Claims such as `iss`, `sub`, and `aud` remain in Payload unless the encoded header itself contains fields with those names.

Payload claim edits must preserve the existing header parameter names and values while re-signing the token. The encoded header bytes and parameter order need not stay identical.

References: [RFC 7519 §5](https://www.rfc-editor.org/rfc/rfc7519.html#section-5), [RFC 7515 §4.1](https://www.rfc-editor.org/rfc/rfc7515.html#section-4.1), and the [IntelliJ tabs guidelines](https://plugins.jetbrains.com/docs/intellij/tabs.html).

## Implementation outline

1. Rework `JwtPanel` to use an IntelliJ `JBTabbedPane` for the two parsed views inside the existing tool window content. Move the header table into Header and retain the claims table and action bar in Payload. Place the existing signing controls and Validate button below the tabs as shared controls.
2. Read the encoded header JSON object in source order so Header can show every supplied parameter, including ones beyond the four currently extracted by `Jwt` (`alg`, `typ`, `cty`, and `kid`). Render complex values compactly and provide a Copy Value context action.
3. Pass the original header parameter names and values through the payload re-signing path. New token creation can continue to construct its initial header.
4. On malformed or cleared encoded input, clear parsed token state and token-dependent actions, show a short empty message in both tables, and retain the current signing key or secret input. Use the `JBTable` empty-text support if suitable.
5. Test tab selection, shared controls, missing optional parameters, custom and complex header values, source order, full-value copying, invalid input, and header preservation after payload edits. Inspect the layout at narrow tool window widths in the supported IDE versions.

## Acceptance criteria

- Opening the tool window shows Payload selected, with the current claim actions and validation workflow available.
- Switching tabs never changes the encoded token, payload claims, or shared signing input.
- Header displays all supplied parameters in encoded JSON order under **Claim / Value**, including custom and complex values, with full-value copying and no header editing controls.
- Payload edits preserve header parameter names and values in the re-signed token.
- Invalid input clears both parsed views and token-dependent actions while retaining shared signing input; both tabs show a short empty message.
- A new valid token or Reset returns to Payload.
- The layout uses short tab labels, puts the most-used view first, and avoids a visible frame around tab content.

## Scope

Nested JWT handling is outside this change.
