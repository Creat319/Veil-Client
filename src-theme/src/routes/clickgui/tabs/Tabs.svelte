<script lang="ts">
    import type {Component} from "svelte";
    import {fly} from "svelte/transition";

    type Tab = {
        title: string;
        content: Component;
    };

    let {tabs, activeTab = $bindable(0)} = $props<{
        tabs: Tab[];
        activeTab?: number;
    }>();

    const Active = $derived(tabs[activeTab]?.content);
</script>

<div class="tabs" transition:fly={{duration: 200, y: -20}}>
    <div class="available-tabs">
        {#each tabs as tab, index (tab.title)}
            <button
                    class="tab-button"
                    class:active={index === activeTab}
                    onclick={() => (activeTab = index)}
                    type="button"
            >
                {tab.title}
            </button>
        {/each}
    </div>

    <div class="content">
        {#if Active}
            {@render Active()}
        {/if}
    </div>
</div>

<style lang="scss">

  .available-tabs {
    position: fixed;
    top: 15px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 4px;
    padding: 5px;
    border-radius: 999px;
    background-image: var(--clickgui-tabs-background-image);
    background-color: var(--clickgui-tabs-background-color);
    border: 1px solid var(--clickgui-tabs-border-color);
    -webkit-backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    box-shadow: var(--clickgui-tabs-shadow);
    z-index: 9999999999;
  }

  .tab-button {
    background: transparent;
    color: var(--clickgui-text-dimmed-color);
    padding: 7px 16px;
    font-size: 13.5px;
    font-weight: 600;
    letter-spacing: 0.2px;
    border-radius: 999px;
    cursor: pointer;
    transition: ease background-color 0.2s, ease color 0.2s, ease box-shadow 0.2s;
    border: solid 1px transparent;

    &:hover {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.active {
      color: var(--clickgui-text-color);
      background-image: var(--clickgui-tab-active-background-image);
      border: 1px solid var(--clickgui-tab-active-border-color);
      box-shadow: inset 0 1px 0 var(--clickgui-glass-rim-color),
      0 0 16px var(--clickgui-tab-active-glow-color);
    }
  }
</style>
