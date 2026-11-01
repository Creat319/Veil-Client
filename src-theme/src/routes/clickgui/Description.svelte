<script lang="ts">
    import {fly} from "svelte/transition";
    import {description, type TDescription} from "./clickgui_store";

    let data: TDescription | null = null;

    description.subscribe((v) => {
        data = v;
    });

    let element: HTMLElement | null = null;
    let left = 0;
    let anchor: "right" | "left" = "right";

    $: {
        if (data?.x !== undefined && element !== null) {
            anchor = data.anchor;
            if (data.anchor === "left") {
                left = data.x - element.clientWidth - 20;
            } else {
                left = data.x + 20;
            }
        }
    }
</script>

{#key data}
    {#if data !== null}
        <div transition:fly|global={{duration: 200, x: anchor === "right" ? -15 : 15}} class="description-wrapper"
             style="top: {data.y}px; left: {left}px;" bind:this={element}>
            <div class="description" class:right={anchor === "left"}>
                <div class="text">{data.description}</div>
            </div>
        </div>
    {/if}
{/key}

<style lang="scss">

  .description-wrapper {
    position: fixed;
    z-index: 999999999999;
    transform: translateY(-50%);
  }

  .description {
    position: relative;
    border-radius: 10px;
    background-image: var(--clickgui-description-background-image);
    background-color: var(--clickgui-description-background-color);
    -webkit-backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    border: 1px solid var(--clickgui-description-border-color);
    box-shadow: var(--clickgui-description-shadow);

    &::before {
      content: "";
      display: block;
      position: absolute;
      width: 0;
      height: 0;
      border-top: 8px solid transparent;
      border-bottom: 8px solid transparent;
      border-right: 8px solid var(--clickgui-description-arrow-color);
      left: -8px;
      top: 50%;
      transform: translateY(-50%);
    }

    &.right {
      &::before {
        transform: translateY(-50%) rotate(180deg);
        left: unset;
        right: -8px;
      }
    }
  }

  .text {
    font-size: 12px;
    line-height: 1.45;
    padding: 10px 13px;
    color: var(--clickgui-text-color);
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45);
  }
</style>