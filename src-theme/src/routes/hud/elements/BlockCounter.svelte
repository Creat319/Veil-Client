<script lang="ts">
    import {listen} from "../../../integration/ws";
    import {fade} from "svelte/transition";
    import {itemTextureUrl} from "../../../integration/rest";
    import type {HudBlockCounterSettings} from "../components";

    export let settings: { [name: string]: any };

    let cSettings: HudBlockCounterSettings;

    $: cSettings = settings as HudBlockCounterSettings;
    $: glow = String(cSettings.glow ?? "Off").toLowerCase();
    $: shadow = String(cSettings.shadow ?? "Off").toLowerCase();
    $: iconPosition = cSettings.iconPosition ?? "Left";
    $: fullProgress = Number(cSettings.fullProgress ?? "64") || 64;
    $: percent = count === undefined ? 0 : Math.min(100, Math.max(0, count / fullProgress * 100));
    $: low = (cSettings.alert ?? true) && count !== undefined && count <= 32;

    let nextBlock: string | undefined = undefined;
    let count: number | undefined = undefined;

    listen("blockCountChange", (data) => {
        nextBlock = data.nextBlock;
        count = data.count;
    });
</script>

{#if count !== undefined}
    <div
            class="counter {iconPosition.toLowerCase()}"
            class:low
            data-glow={glow}
            data-shadow={shadow}
            transition:fade={{duration: 200}}
    >
        {#if nextBlock && iconPosition !== "None"}
            <img class="icon" src={itemTextureUrl(nextBlock)} alt={nextBlock}/>
        {/if}

        <div class="bar">
            <div class="fill" style="width: {percent}%"></div>
        </div>

        <div class="readout">{count} blocks</div>
    </div>
{/if}

<style lang="scss">
  .counter {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 10px;
    box-sizing: border-box;
    width: 200px;
    height: 40px;
    padding: 0 14px;
    border-radius: 20px;
    background-color: var(--blockcounter-background-color);
    color: var(--blockcounter-text-color);
    font-family: "Inter", sans-serif;
    box-shadow: inset 0 0 0 1px var(--blockcounter-rim-color);

    &[data-shadow="weak"] {
      box-shadow: inset 0 0 0 1px var(--blockcounter-rim-color), 0 1px 3px rgba(0, 0, 0, 0.45);
    }

    &[data-shadow="medium"] {
      box-shadow: inset 0 0 0 1px var(--blockcounter-rim-color), 0 2px 6px rgba(0, 0, 0, 0.55);
    }

    &[data-shadow="strong"] {
      box-shadow: inset 0 0 0 1px var(--blockcounter-rim-color), 0 3px 10px rgba(0, 0, 0, 0.7);
    }

    &[data-glow="weak"] .fill {
      box-shadow: 0 0 4px var(--blockcounter-fill-glow-color);
    }

    &[data-glow="medium"] .fill {
      box-shadow: 0 0 7px var(--blockcounter-fill-glow-color);
    }

    &[data-glow="strong"] .fill {
      box-shadow: 0 0 12px var(--blockcounter-fill-glow-color);
    }

    /* icon placement */
    &.right {
      flex-direction: row-reverse;
      justify-content: flex-end;
    }

    &.top,
    &.bottom {
      flex-direction: column;
      justify-content: center;
      gap: 4px;
      height: auto;
      padding: 8px 14px;

      .bar {
        width: 100%;
      }
    }

    &.bottom {
      flex-direction: column-reverse;
    }

    &.none .readout {
      margin-left: auto;
    }
  }

  .icon {
    width: 22px;
    height: 22px;
    flex: 0 0 auto;
    image-rendering: pixelated;
  }

  .bar {
    position: relative;
    flex: 1 1 auto;
    height: 6px;
    border-radius: 3px;
    background-color: var(--blockcounter-track-color);
    overflow: hidden;
  }

  .fill {
    height: 100%;
    border-radius: inherit;
    background-color: var(--blockcounter-fill-color);
    /* springy, matching the reference HUD's spring easing */
    transition: width 250ms cubic-bezier(0.34, 1.36, 0.64, 1);
  }

  .readout {
    flex: 0 0 auto;
    color: var(--blockcounter-text-color);
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
  }

  .counter.low {
    .fill {
      background-color: var(--blockcounter-alert-color);
    }

    .readout {
      color: var(--blockcounter-alert-color);
    }
  }
</style>
