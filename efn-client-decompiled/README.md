# efn-client-1.2.0.jar — decompiled (⚠ MALWARE, read-only)

**Verdict: confirmed malicious.** The jar is a Minecraft Fabric "hacked client" (`efn.lol`) that
carries a hidden, encrypted second stage. At game start it steals the player's Minecraft session
(access token, UUID, username), sends it to an attacker-controlled server, then pulls down and
launches a Python-based **info-stealer**. The cheat features are the bait.

> Static analysis only. Nothing from the jar was executed and no C2 server was contacted.
> Do **not** run this project. `runClient`/`runServer` are hard-disabled in `build.gradle`.

## Layout

| Path | What |
|---|---|
| `src/main/java` | Main jar, remapped intermediary → Yarn (1.21.11+build.6), decompiled with Vineflower 1.11.1 |
| `src/main/resources` | `fabric.mod.json`, mixin configs, assets, `assets/efn/data/runtime.dat` (**the encrypted payload**) |
| `libs/` | Jars that were nested in `META-INF/jars` (imgui, netty, fabric-api modules) |
| `stage2/src/main/java` | Raw decompile of the decrypted payload (`net.efn.loader.*`) |
| `stage2/deobfuscated-strings/` | Same classes with the obfuscated char-arithmetic strings constant-folded — **read these** |
| `analysis/decrypt_runtime.py` | Reproduces the `runtime.dat` → `p.jar` decryption (pure Python, no execution) |
| `analysis/fold_strings.py` | The string de-obfuscator (run repeatedly until it stops changing output) |

Open the folder as a Gradle project (Loom 1.14.10, Gradle 9.2 wrapper included). It indexes fine;
it will **not** compile cleanly: decompiler artifacts, plus optional `litematica`/`malilib`
(`suggests` in `fabric.mod.json`) are not on the classpath. One method failed to decompile:
`net.efn.client.util.seedscan.SeedWorldgen#emptyLevel` (stub with the error in a comment).

## Infection chain

1. **Dropper — `net.efn.agent.AgentInit`.** Registered as a `main` entrypoint in `fabric.mod.json`
   (and as the jar's `Main-Class`). On a background thread it reads `assets/efn/data/runtime.dat`,
   decrypts it (LCG keystream, seed `324508639`, xor key `5A 3C 71 0E 2D 44 19 67`), writes it to
   `%TEMP%\efn*\p.jar`, loads it in a `URLClassLoader`, and calls `net.efn.loader.LoaderInit`.
   All exceptions are swallowed.
2. **`LoaderInit` / `SessionAccess`.** Reflectively pulls the Minecraft `Session`
   (`getAccessToken`, `getUuid`, `getUsername`) via both mapped and intermediary names
   (`class_310`, `method_1548`, …), plus the game dir. Builds a base64 JSON context
   `{mcInfo:{username,uuid,accessToken}, prefireId, userId, tag, domain, gameDir, mcUsername, mcUuid, env:"Fabric"}`
   and launches a **detached** `javaw.exe -cp <mod jar> net.efn.loader.RuntimeBridge <context>`,
   stdin `NUL`, logging to `%LOCALAPPDATA%\Microsoft\Windows\NtProfileIndex\_spawn.log` (a name
   chosen to look like a Windows component). Windows-only.
3. **C2 lookup — `RuntimeBridge.getDomain()` (EtherHiding).** The C2 domain isn't hardcoded; it is
   read from the Polygon blockchain with `eth_call` to contract
   `0x9c0a507300fd902787bb193d80fca5ce6e1bff9a`, selector `0xce6d41de`, trying five public RPCs
   (`polygon.drpc.org`, `polygon-bor-rpc.publicnode.com`, `1rpc.io/matic`, `api.zan.top/polygon-mainnet`,
   `rpc-mainnet.matic.quiknode.pro`). Fallback if all fail: **`thisisafalsepositive.st`**.
   This makes the C2 un-takedownable by domain seizure — the attacker just updates the contract.
4. **`HttpTransport`.** Hand-rolled HTTP stack: resolves hosts via **DoH to `1.1.1.1`**
   (`cloudflare-dns.com`, bypasses local DNS/hosts blocking), spoofs a Chrome 131 UA, and installs a
   **`TrustAll` X509 trust manager** (no TLS verification).
5. **Registration.** `POST https://<c2>/shard/prefireMc` with the **Minecraft access token** as
   `sessionId` → returns a `prefireId`.
6. **Stage 3 download.** Fetches `https://<c2>/cdn/v2/9f4e7a2c1b8d.png` — an image with an embedded
   archive (`"PNG extraction failed"`, `Inflater`/`ZipInputStream`) that unpacks a portable
   `python.exe` + `AppHost\main.py` under `NtProfileIndex\`.
7. **Stealer.** Runs `python.exe AppHost\main.py --mcInfo … --prefireId … --env … --tag …`
   (log: `_stealer.log`), then uploads `latest.log` to `/shard/submitMinecraftLog`.

> What `main.py` actually steals is **not** established here: it is downloaded at runtime from the
> C2 and I did not fetch it. From the name/log strings ("stealer") and the Python runtime it's a
> typical credential/browser/Discord-token stealer, but that is inference, not observed.

The rest of the client (`net.efn.client.*`, ~1300 classes) looked like a normal cheat client:
module system, ImGui GUI, ESP, combat/movement, seed-scan, Spotify/TikTok overlays. Its network
calls are Microsoft/Xbox/Mojang auth, TikTok live status, lrclib lyrics, and a Discord invite. I found no
other loader (only `AgentInit` touches `runtime.dat`). I only checked the
hardcoded URLs there, not every class line by line — `MicrosoftLogin`/`Accounts`/`webhook` are worth a
read if you want to rule out token logging beyond the dropper.

## IOCs

```
efn-client-1.2.0.jar   sha256 581870564fa9e71c09a6824b39cc75ebbd81e3c3690f154b3ef6aa08a4bab40b
runtime.dat            sha256 6423460f35b3f63f024c5a24862620ec2619e25fdfdce4808e8981473e054441
decrypted p.jar        sha256 b9e5d53d59e44dfb81c2c10607b4faeb02090941cec90fbdfbcef9b109172692

Polygon contract       0x9c0a507300fd902787bb193d80fca5ce6e1bff9a  (selector 0xce6d41de)
Fallback C2 domain     thisisafalsepositive.st
C2 paths               /shard/prefireMc  /shard/submitMinecraftLog  /cdn/v2/9f4e7a2c1b8d.png
Host artifacts         %TEMP%\efn*\p.jar
                       %LOCALAPPDATA%\Microsoft\Windows\NtProfileIndex\  (_spawn.log, _stealer.log, python.exe, AppHost\main.py)
Process                javaw.exe -cp <efn-client jar> net.efn.loader.RuntimeBridge <base64 json>
```

## If it was ever run

Treat the machine and every account used on it as compromised: rotate Microsoft/Mojang,
Discord, browser-saved passwords, crypto wallets, and anything else on that PC; invalidate Minecraft
sessions by changing the Microsoft password; delete the `NtProfileIndex` folder and the `efn*` temp
dir; and check for other persistence before trusting it again.
