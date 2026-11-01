<script lang="ts">
    import {onMount} from "svelte";
    import type {Module as TModule} from "../../integration/types";
    import {listen} from "../../integration/ws";
    import Module from "./Module.svelte";
    import type {KeyboardKeyEvent, ModuleToggleEvent} from "../../integration/events";
    import {fade} from "svelte/transition";
    import {quintOut} from "svelte/easing";
    import {
        gridSize,
        highlightModuleName,
        maxPanelZIndex,
        scaleFactor,
        showGrid,
        snappingEnabled
    } from "./clickgui_store";
    import {setItem} from "../../integration/persistent_storage";

    export let category: string;
    export let modules: TModule[];
    export let panelIndex: number;
    /** Translated category label; falls back to [category], which stays the identifier. */
    export let title: string | undefined = undefined;
    export let icon: string | undefined = undefined;

    let panelElement: HTMLElement;
    let modulesElement: HTMLElement;
    let expandButtonElement: HTMLElement;

    let moving = false;
    let offsetX = 0;
    let offsetY = 0;

    let scrollPositionSaveTimeout: number | undefined;

    const panelConfig = loadPanelConfig();

    let ignoreGrid = false;

    interface PanelConfig {
        top: number;
        left: number;
        expanded: boolean;
        scrollTop: number;
        zIndex: number;
    }

    function showFallbackIcon(event: Event) {
        (event.currentTarget as HTMLImageElement).src = "img/clickgui/icon-client.svg";
    }

    function clamp(number: number, min: number, max: number) {
        return Math.max(min, Math.min(number, max));
    }

    function loadPanelConfig(): PanelConfig {
        const localStorageItem = localStorage.getItem(
            `clickgui.panel.${category}`,
        );

        if (!localStorageItem) {
            return {
                top: panelIndex * 50 + 20,
                left: 20,
                expanded: false,
                scrollTop: 0,
                zIndex: 0
            };
        } else {
            const config: PanelConfig = JSON.parse(localStorageItem);

            // Migration
            if (!config.zIndex) {
                config.zIndex = 0;
            }

            if (config.zIndex > $maxPanelZIndex) {
                $maxPanelZIndex = config.zIndex;
            }

            return config;
        }
    }

    async function savePanelConfig() {
        await setItem(
            `clickgui.panel.${category}`,
            JSON.stringify(panelConfig),
        );
    }

    function fixPosition() {
        panelConfig.left = clamp(panelConfig.left, 0, document.documentElement.clientWidth * (2 / $scaleFactor) - panelElement.offsetWidth);
        panelConfig.top = clamp(panelConfig.top, 0, document.documentElement.clientHeight * (2 / $scaleFactor) - panelElement.offsetHeight);
    }

    function onMouseDown(e: MouseEvent) {
        if (e.button !== 0 && e.button !== 1) return;

        moving = true;
        offsetX = e.clientX * (2 / $scaleFactor) - panelConfig.left;
        offsetY = e.clientY * (2 / $scaleFactor) - panelConfig.top;
        panelConfig.zIndex = ++$maxPanelZIndex;

        $showGrid = $snappingEnabled && !expandButtonElement.contains(e.target as HTMLElement);
    }

    function onMouseMove(e: MouseEvent) {
        if (moving) {
            const newLeft = (e.clientX * (2 / $scaleFactor) - offsetX);
            const newTop = (e.clientY * (2 / $scaleFactor) - offsetY);

            panelConfig.left = snapToGrid(newLeft);
            panelConfig.top = snapToGrid(newTop);

            fixPosition();
        }
    }

    function onMouseUp() {
        if (moving) {
            savePanelConfig();
        }
        moving = false;
        $showGrid = false;
    }

    function toggleExpanded() {
        panelConfig.expanded = !panelConfig.expanded;

        savePanelConfig();
    }

    /**
     * The panel only reaches its new height once the `max-height` transition of the module
     * list has finished, so the position can only be clamped here. Doing it right after the
     * toggle would measure the height the panel had *before* it was expanded or collapsed.
     */
    function handleModulesTransitionEnd(e: TransitionEvent) {
        if (e.target !== modulesElement || e.propertyName !== "max-height") {
            return;
        }

        const {left, top} = panelConfig;

        fixPosition();

        if (panelConfig.left !== left || panelConfig.top !== top) {
            savePanelConfig();
        }
    }

    function handleModulesScroll() {
        panelConfig.scrollTop = modulesElement.scrollTop;

        if (scrollPositionSaveTimeout !== undefined) {
            clearTimeout(scrollPositionSaveTimeout);
        }
        scrollPositionSaveTimeout = setTimeout(() => {
            savePanelConfig();
        }, 500)
    }

    highlightModuleName.subscribe((name) => {
        const highlightModule = modules.find(
            (m) => m.name === name,
        );
        if (highlightModule) {
            panelConfig.zIndex = ++$maxPanelZIndex;
            panelConfig.expanded = true;
            savePanelConfig();
        }
    });

    listen("moduleToggle", (e: ModuleToggleEvent) => {
        const moduleName = e.moduleName;
        const moduleEnabled = e.enabled;

        const mod = modules.find((m) => m.name === moduleName);
        if (!mod) return;

        mod.enabled = moduleEnabled;
        modules = modules;
    });

    onMount(() => {
        if (!modulesElement) {
            return;
        }

        modulesElement.scrollTo({
            top: panelConfig.scrollTop,
            behavior: "smooth"
        });
    });

    listen("keyboardKey", (e: KeyboardKeyEvent) => {
        if (e.key === "key.keyboard.left.shift") {
            ignoreGrid = e.action === 1;
        }
    });

    function snapToGrid(value: number): number {
        if (ignoreGrid || !$snappingEnabled) return value;

        return Math.round(value / $gridSize) * $gridSize;
    }
</script>

<svelte:window on:mouseup={onMouseUp} on:mousemove={onMouseMove}/>

<div
        class="panel"
        style="left: {panelConfig.left}px; top: {panelConfig.top}px; z-index: {panelConfig.zIndex};"
        bind:this={panelElement}
        transition:fade|global={{duration: 200, easing: quintOut}}
>
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div
            class="title"
            on:mousedown={onMouseDown}
            on:contextmenu|preventDefault={toggleExpanded}
    >
        <img
                class="icon"
                src={icon ?? `img/clickgui/icon-${category.toLowerCase()}.svg`}
                alt="icon"
                on:error={showFallbackIcon}
        />
        <span class="category">{title ?? category}</span>

        <!-- svelte-ignore a11y_consider_explicit_label -->
        <button class="expand-toggle" on:click={toggleExpanded} bind:this={expandButtonElement}>
            <div class="icon" class:expanded={panelConfig.expanded}></div>
        </button>
    </div>

    <div
            class="modules"
            class:expanded={panelConfig.expanded}
            on:scroll={handleModulesScroll}
            on:transitionend={handleModulesTransitionEnd}
            bind:this={modulesElement}
    >
        {#each modules as {name, localizedName, enabled, description, aliases} (name)}
            <Module {name} {localizedName} {enabled} {description} {aliases}/>
        {/each}
    </div>
</div>

<style lang="scss">

  .panel {
    border-radius: 16px;
    width: 262px;
    position: absolute;
    overflow: hidden;
    background-image: var(--clickgui-panel-background-image);
    background-color: var(--clickgui-panel-background-color);
    border: 1px solid var(--clickgui-panel-border-color);
    -webkit-backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    box-shadow: var(--clickgui-panel-shadow);
    will-change: transform;
    transition: none;
    user-select: none;
  }

  .title {
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    align-items: center;
    column-gap: 11px;
    background-image: var(--clickgui-panel-header-background-image);
    background-color: var(--clickgui-panel-header-background-color);
    border-bottom: 1px solid var(--clickgui-panel-header-border-color);
    padding: 11px 12px 11px 15px;
    cursor: grab;

    &:active {
      cursor: grabbing;
    }

    > img {
      width: 15px;
      height: 15px;
      opacity: 0.92;
      filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.5));
    }

    .category {
      font-size: 13px;
      letter-spacing: 0.2px;
      color: var(--clickgui-text-color);
      font-weight: 600;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45);
    }
  }

  .modules {
    transition: max-height 300ms ease;
    scroll-behavior: smooth;
    max-height: 0;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 4px 0 6px;
    background-color: var(--clickgui-panel-body-background-color);

    &.expanded {
      max-height: 545px;
    }

    &::-webkit-scrollbar {
      width: 4px;
      height: 4px;
    }

    &::-webkit-scrollbar-track {
      background-color: transparent;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 999px;
      background-color: var(--clickgui-panel-scrollbar-thumb-color);
      transition: background-color 0.2s ease;
    }

    &:hover::-webkit-scrollbar-thumb {
      background-color: var(--clickgui-panel-scrollbar-thumb-hover-color);
    }
  }

  .expand-toggle {
    background-color: transparent;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    width: 24px;
    height: 24px;
    display: grid;
    place-items: center;
    transition: background-color 0.2s ease;

    &:hover {
      background-color: var(--clickgui-tab-hover-background-color);
    }

    .icon {
      height: 12px;
      width: 12px;
      position: relative;

      &::before {
        content: "";
        position: absolute;
        background-color: var(--clickgui-panel-toggle-icon-color);
        transition: transform 0.4s ease-out, background-color 0.2s ease;
        top: 0;
        left: 50%;
        width: 2px;
        height: 100%;
        margin-left: -1px;
        border-radius: 2px;
      }

      &::after {
        content: "";
        position: absolute;
        background-color: var(--clickgui-panel-toggle-icon-color);
        transition: transform 0.4s ease-out, background-color 0.2s ease;
        top: 50%;
        left: 0;
        width: 100%;
        height: 2px;
        margin-top: -1px;
        border-radius: 2px;
      }

      &.expanded {
        &::before {
          transform: rotate(90deg);
        }

        &::after {
          transform: rotate(180deg);
        }
      }
    }

    &:hover .icon {
      &::before,
      &::after {
        background-color: var(--clickgui-panel-toggle-icon-hover-color);
      }
    }
  }
</style>
