Werewolf Skin Source Files
==========================

This directory contains the original PNG skin files for werewolf types.
These files are for editing and reference only - they are NOT used directly by the plugin.

How to Update Skins:
===================

1. Edit the PNG files in this directory using a Minecraft skin editor:
   - Minecraft Skin Editor (online)
   - Skindex (https://www.minecraftskins.com/)
   - Any image editor that supports PNG

2. Upload the edited PNG to MineSkin.org:
   - Go to https://mineskin.org/
   - Click "Upload" and select your PNG file
   - Click "Generate URL"
   - Copy the "value" field from the response

3. Add the texture value to skins.yml:
   - Open plugins/Werewolf/skins.yml
   - Find the skin type (alpha, witherfang, etc.)
   - Paste the "value" into the value field
   - Optionally paste the "signature" if provided

4. Reload the plugin or restart the server

Skin File Requirements:
======================

- Format: PNG (Portable Network Graphics)
- Dimensions: 64x64 pixels (classic) or 64x128 pixels (modern)
- Model: Both slim (Alex) and wide (Steve) models supported
- File naming: Use the werewolf type name (e.g., alpha.png, witherfang.png)

Available Skin Types:
====================

- alpha.png - Alpha werewolf skin
- witherfang.png - Witherfang werewolf skin
- silvermane.png - Silvermane werewolf skin
- bloodmoon.png - Bloodmoon werewolf skin

Note: You can add custom werewolf types by:
1. Adding a new PNG file here
2. Adding a corresponding entry in skins.yml
3. Adding the type to config.yml under "skins:" section

