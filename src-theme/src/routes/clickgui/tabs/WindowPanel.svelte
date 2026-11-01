<script lang="ts">
    import type {Snippet} from "svelte";
    import {fade} from "svelte/transition";
    import {quintOut} from "svelte/easing";

    let { title, icon, children } = $props<{
        title: string;
        icon?: string;
        children: Snippet;
    }>();
</script>

<div class="window" transition:fade|global={{duration: 200, easing: quintOut}}>
    <div class="title">
        {#if icon}
            <img
                    class="icon"
                    src="img/clickgui/icon-{icon}.svg"
                    alt="icon"
            />
        {/if}
        <span class="title-text">{title}</span>
    </div>
    <div class="content">
        {@render children()}
    </div>
</div>

<style lang="scss">

  .window {
    position: fixed;
    top: 70px;
    left: 50%;
    transform: translateX(-50%);
    width: min(820px, 92vw);
    --window-max-height: 70vh;
    max-height: var(--window-max-height, none);
    border-radius: 18px;
    overflow: hidden;
    background-image: var(--clickgui-window-background-image);
    background-color: var(--clickgui-window-background-color);
    border: 1px solid var(--clickgui-window-border-color);
    -webkit-backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    box-shadow: var(--clickgui-window-shadow);
    user-select: none;
  }

  .title {
    display: grid;
    grid-template-columns: max-content 1fr;
    align-items: center;
    column-gap: 12px;
    background-image: var(--clickgui-window-header-background-image);
    background-color: var(--clickgui-window-header-background-color);
    padding: 16px 22px;
    font-size: 15px;
    font-weight: 600;
    letter-spacing: 0.2px;
    color: var(--clickgui-text-color);
    border-bottom: 1px solid var(--clickgui-window-header-border-color);
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45);

    > img {
      width: 16px;
      height: 16px;
      filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.5));
    }
  }

  .content {
    padding: 12px 22px 18px;
    overflow: auto;
    max-height: calc(var(--window-max-height, 9999px) - 60px);

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 999px;
      background-color: var(--clickgui-panel-scrollbar-thumb-color);
    }
  }
</style>
