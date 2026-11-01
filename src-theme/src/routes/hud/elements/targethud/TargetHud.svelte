<script lang="ts">
    import {onMount, tick} from "svelte";
    import {fly} from "svelte/transition";
    import {listen} from "../../../../integration/ws.js";
    import type {PlayerData} from "../../../../integration/types";
    import {getPlayerData, itemTextureUrl} from "../../../../integration/rest";
    import {REST_BASE} from "../../../../integration/host";
    import type {TargetChangeEvent} from "../../../../integration/events";
    import type {HudTargetHudSettings} from "../components";

    export let settings: { [name: string]: any };

    let cSettings: HudTargetHudSettings;

    $: cSettings = settings as HudTargetHudSettings;
    $: style = cSettings.style ?? "Round";
    $: readout = cSettings.readout ?? "Number";
    $: glow = String(cSettings.glow ?? "Off").toLowerCase();
    $: shadow = String(cSettings.shadow ?? "Off").toLowerCase();

    let target: PlayerData | null = null;
    let visible = false;
    let hurt = false;

    let localPosition: [number, number, number] | null = null;

    let hideTimeout: number;
    let hurtTimeout: number;

    $: health = target?.actualHealth ?? 0;
    $: maxHealth = target?.maxHealth ?? 20;
    $: absorption = Math.max(0, target?.absorption ?? 0);
    $: totalHealth = health + absorption;
    $: totalMaxHealth = maxHealth + absorption;
    $: healthPercent = totalMaxHealth > 0 ? Math.min(100, Math.max(0, totalHealth / totalMaxHealth * 100)) : 0;
    $: healthLow = healthPercent <= 30;

    $: distance = target !== null && localPosition !== null
        ? Math.hypot(
            target.position.x - localPosition[0],
            target.position.y - localPosition[1],
            target.position.z - localPosition[2]
        )
        : null;

    $: subText = [
        (cSettings.showDistance ?? true) && distance !== null ? `${distance.toFixed(1)}m` : null,
        `${Math.ceil(health)}/${Math.ceil(maxHealth)}`,
    ].filter(part => part !== null).join(" · ");

    $: skinUrl = target !== null ? `${REST_BASE}/api/v1/client/resource/skin?uuid=${target.uuid}` : "";
    $: hearts = buildHearts(totalHealth, totalMaxHealth);

    function buildHearts(health: number, max: number): number[] {
        const count = Math.min(10, Math.max(1, Math.ceil(max / 2)));
        return Array.from({length: count}, (_, index) => Math.min(1, Math.max(0, (health - index * 2) / 2)));
    }

    /** Replays the damage bounce: a short pop when the target loses health. */
    function triggerHurt() {
        hurt = false;
        clearTimeout(hurtTimeout);
        void tick().then(() => {
            hurt = true;
            hurtTimeout = window.setTimeout(() => hurt = false, 1500);
        });
    }

    function startHideTimeout() {
        clearTimeout(hideTimeout);
        hideTimeout = window.setTimeout(() => {
            visible = false;
        }, 1000);
    }

    listen("targetChange", (data: TargetChangeEvent) => {
        const previous = target;
        target = data.target;
        visible = true;

        if (previous !== null && data.target !== null && previous.username === data.target.username &&
            data.target.actualHealth < previous.actualHealth - 0.01) {
            triggerHurt();
        }

        startHideTimeout();
    });

    onMount(async () => {
        const player = await getPlayerData().catch(() => null);
        if (player !== null) {
            localPosition = [player.position.x, player.position.y, player.position.z];
        }
    });

    listen("clientPlayerData", (data: { playerData: PlayerData }) => {
        localPosition = [data.playerData.position.x, data.playerData.position.y, data.playerData.position.z];
    });

    startHideTimeout();
</script>

{#if visible && target != null}
    <div
            class="targethud {style.toLowerCase()}"
            class:hurt
            data-glow={glow}
            data-shadow={shadow}
            transition:fly={{y: -8, duration: 180}}
    >
        <div class="avatar" style="--avatar-image: url('{skinUrl}');"></div>

        {#if style === "Rise"}
            <div class="name">
                <span class="prefix">Name:&#32;</span><span class="gradient">{target.username}</span>
            </div>
            <div class="capsule">
                <div class="ghost"></div>
                <div class="fill" style="width: {healthPercent}%"></div>
            </div>
            <div class="readout">{health.toFixed(1)}</div>
        {:else if style === "Moon"}
            <div class="name">{target.username}</div>
            <div class="readout accent">{Math.ceil(health)}</div>
            {#if cSettings.showArmor ?? true}
                <div class="armor">
                    {#each [3, 2, 1, 0] as slot}
                        {#if target.armorItems[slot]?.count > 0}
                            <div class="armor-slot">
                                <img src={itemTextureUrl(target.armorItems[slot].identifier)} alt="armor"/>
                                <div class="durability">
                                    <div
                                            class="durability-fill"
                                            style="width: {(1 - target.armorItems[slot].damage / Math.max(1, target.armorItems[slot].maxDamage)) * 100}%"
                                    ></div>
                                </div>
                            </div>
                        {:else}
                            <div class="armor-slot empty"></div>
                        {/if}
                    {/each}
                </div>
            {/if}
            <div class="bar">
                <div class="ghost"></div>
                <div class="fill" style="width: {healthPercent}%"></div>
            </div>
        {:else if style === "Simple"}
            <div class="name">{target.username}</div>
            {#if readout === "Hearts"}
                <div class="hearts">
                    {#each hearts as fill}
                        <span class="heart" style="--fill: {fill * 100}%">♥</span>
                    {/each}
                </div>
            {:else}
                <div class="sub">HP: {Math.ceil(health)}</div>
            {/if}
            <div class="bar">
                <div class="ghost"></div>
                <div class="fill" style="width: {healthPercent}%"></div>
            </div>
        {:else}
            <div class="name">{target.username}</div>
            <div class="sub">{subText}</div>
            <div class="bar">
                <div class="ghost"></div>
                <div class="fill" style="width: {healthPercent}%"></div>
            </div>
        {/if}
    </div>
{/if}

<style lang="scss">
  .targethud {
    position: relative;
    box-sizing: border-box;
    overflow: hidden;
    background-color: var(--targethud-background-color);
    color: var(--targethud-text-color);
    font-family: "Inter", sans-serif;
    border-radius: var(--targethud-radius, 7px);
    box-shadow: inset 0 0 0 1px var(--targethud-rim-color);

    &.hurt {
      animation: targethud-hurt 1.5s cubic-bezier(0.18, 1.4, 0.35, 1);
    }

    &[data-shadow="weak"] {
      box-shadow: inset 0 0 0 1px var(--targethud-rim-color), 0 1px 3px rgba(0, 0, 0, 0.45);
    }

    &[data-shadow="medium"] {
      box-shadow: inset 0 0 0 1px var(--targethud-rim-color), 0 2px 6px rgba(0, 0, 0, 0.55);
    }

    &[data-shadow="strong"] {
      box-shadow: inset 0 0 0 1px var(--targethud-rim-color), 0 3px 10px rgba(0, 0, 0, 0.7);
    }

    &[data-glow="weak"] .fill {
      box-shadow: 0 0 4px var(--targethud-fill-glow-color);
    }

    &[data-glow="medium"] .fill {
      box-shadow: 0 0 7px var(--targethud-fill-glow-color);
    }

    &[data-glow="strong"] .fill {
      box-shadow: 0 0 12px var(--targethud-fill-glow-color);
    }
  }

  @keyframes targethud-hurt {
    0% {
      transform: scale(0.72);
    }
    60% {
      transform: scale(1.04);
    }
    100% {
      transform: scale(1);
    }
  }

  /* rounded-square head cut out of the full skin texture */
  .avatar {
    position: absolute;
    left: var(--avatar-left, 6px);
    top: var(--avatar-top, 50%);
    width: var(--avatar-size, 40px);
    height: var(--avatar-size, 40px);
    translate: 0 -50%;
    border-radius: var(--avatar-radius, 7px);
    background-color: var(--targethud-avatar-background-color);
    background-image: var(--avatar-image);
    background-size: 800% auto;
    /* head front face sits at 12.5% / 12.5% of the skin sheet */
    background-position: 14.2857% 14.2857%;
    image-rendering: pixelated;
  }

  .name {
    position: absolute;
    color: var(--targethud-text-color);
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .sub {
    position: absolute;
    color: var(--targethud-text-dimmed-color);
    font-weight: 500;
    white-space: nowrap;
  }

  .prefix {
    color: var(--targethud-text-color);
  }

  .gradient {
    background-image: linear-gradient(90deg,
        var(--targethud-gradient-start-color),
        var(--targethud-gradient-end-color));
    background-clip: text;
    -webkit-background-clip: text;
    color: transparent;
  }

  .readout {
    position: absolute;
    font-weight: 600;
    white-space: nowrap;

    &.accent {
      color: var(--accent-color);
    }
  }

  .bar,
  .capsule {
    position: absolute;
    overflow: hidden;
    background-color: var(--targethud-track-color);

    .ghost,
    .fill {
      position: absolute;
      top: 0;
      bottom: 0;
      left: 0;
      border-radius: inherit;
    }

    .ghost {
      background-color: var(--targethud-ghost-color);
      transition: width 1500ms cubic-bezier(0.22, 1, 0.36, 1);
    }

    .fill {
      background-image: linear-gradient(90deg,
          var(--targethud-fill-color),
          var(--targethud-fill-end-color));
      transition: width 500ms cubic-bezier(0.16, 1, 0.3, 1);
    }
  }

  .hearts {
    position: absolute;
    display: flex;
    gap: 1.5px;
  }

  .heart {
    font-size: 9px;
    line-height: 1;
    background-image: linear-gradient(90deg,
        var(--targethud-heart-color) 0 var(--fill),
        var(--targethud-heart-empty-color) var(--fill) 100%);
    background-clip: text;
    -webkit-background-clip: text;
    color: transparent;
  }

  /* ---------------------------------------------------------------- Round */
  .targethud.round {
    --targethud-radius: 7px;
    --avatar-size: 40px;
    --avatar-top: 50%;
    width: 170px;
    height: 52px;

    .name {
      left: 55px;
      top: 12px;
      font-size: 13px;
    }

    .sub {
      left: 55px;
      top: 30px;
      font-size: 10.5px;
    }

    .bar {
      left: 55px;
      right: 9px;
      bottom: 9px;
      height: 5px;
      border-radius: 3px;
    }
  }

  /* ----------------------------------------------------------------- Rise */
  .targethud.rise {
    --targethud-radius: 12px;
    --avatar-size: 48px;
    --avatar-top: 50%;
    --avatar-left: 6px;
    --avatar-radius: 10px;
    width: 216px;
    height: 60px;

    .name {
      left: 62px;
      top: 11px;
      font-size: 14px;
    }

    .capsule {
      left: 62px;
      top: 34px;
      width: 112px;
      height: 10px;
      border-radius: 5px;
    }

    .readout {
      right: 8px;
      top: 30px;
      font-size: 12px;
    }
  }

  /* ----------------------------------------------------------------- Moon */
  .targethud.moon {
    --targethud-radius: 7px;
    --avatar-size: 36px;
    --avatar-top: 6px;
    --avatar-left: 6px;
    --avatar-radius: 7px;
    width: 170px;
    height: 62px;

    .avatar {
      translate: none;
    }

    .name {
      left: 50px;
      top: 12px;
      font-size: 13px;
    }

    .readout {
      right: 8px;
      top: 12px;
      font-size: 13px;
    }

    .armor {
      position: absolute;
      left: 50px;
      top: 30px;
      display: flex;
      gap: 4px;
    }

    .armor-slot {
      position: relative;
      width: 13px;
      height: 13px;
      border-radius: 3px;
      background-color: rgba(255, 255, 255, 0.08);

      img {
        width: 100%;
        height: 100%;
        image-rendering: pixelated;
      }

      &.empty {
        background-color: rgba(255, 255, 255, 0.06);
      }

      .durability {
        position: absolute;
        left: 0;
        right: 0;
        bottom: -3px;
        height: 2px;
        border-radius: 1px;
        background-color: var(--targethud-track-color);
        overflow: hidden;
      }

      .durability-fill {
        height: 100%;
        background-color: var(--targethud-fill-color);
      }
    }

    .bar {
      left: 6px;
      right: 6px;
      bottom: 8px;
      height: 5px;
      border-radius: 3px;
    }
  }

  /* --------------------------------------------------------------- Simple */
  .targethud.simple {
    --targethud-radius: 7px;
    --avatar-size: 38px;
    --avatar-top: 50%;
    --avatar-left: 6px;
    width: 160px;
    height: 50px;

    .name {
      left: 52px;
      top: 10px;
      font-size: 13px;
    }

    .sub,
    .hearts {
      left: 52px;
      top: 28px;
    }

    .sub {
      font-size: 10.5px;
    }

    .bar {
      left: 52px;
      right: 8px;
      bottom: 7px;
      height: 4px;
      border-radius: 2px;

      .ghost {
        background-color: var(--targethud-ghost-low-color);
      }

      .fill {
        background-image: none;
        background-color: var(--targethud-fill-plain-color);
      }
    }
  }
</style>
