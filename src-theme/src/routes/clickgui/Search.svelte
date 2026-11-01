<script lang="ts">
    import type {ConfigurableSetting, Module} from "../../integration/types";
    import {getModuleSettings, setModuleEnabled} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {ClickGuiValueChangeEvent, KeyboardKeyEvent, ModuleToggleEvent} from "../../integration/events";
    import {highlightModuleName} from "./clickgui_store";
    import {onMount} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../theme/theme_config";
    import {displayName} from "../../util/display_name";
    import {isClickGuiScreen} from "../../util/utils";

    export let modules: Module[];

    let resultElements: HTMLElement[] = [];
    let searchContainerElement: HTMLElement;
    let autoFocus: boolean = true
    let searchInputElement: HTMLElement;
    let query: string;
    let filteredModules: Module[] = [];
    let selectedIndex = 0;
    let hasFocus = false;

    type SearchableModule = {
        raw: Module;
        lowerName: string;
        lowerAliases: string[];
        lowerLocalized: string;
    };

    // Cache lowercase names/aliases to avoid repeated toLowerCase() per keystroke.
    let searchableModules: SearchableModule[] = [];
    $: searchableModules = modules.map(m => ({
        raw: m,
        lowerName: m.name.toLowerCase(),
        lowerAliases: m.aliases.map(a => a.toLowerCase()),
        // The original name stays searchable while a translated label is displayed.
        lowerLocalized: (m.localizedName ?? "").toLowerCase(),
    }));

    function reset() {
        filteredModules = [];
        query = "";
        $highlightModuleName = null;
    }

    function filterModules(resetIndex: boolean) {
        if (!query) {
            reset();
            return;
        }

        if (resetIndex) {
            selectedIndex = 0;
        }

        const pureQuery = query.toLowerCase().replaceAll(" ", "");

        filteredModules = searchableModules.filter(({ raw, lowerName, lowerAliases, lowerLocalized }) => {
            return lowerName.includes(pureQuery)
                || lowerAliases.some(a => a.includes(pureQuery))
                || lowerLocalized.includes(pureQuery);
        }).map(it => it.raw);
    }

    async function handleKeyDown(e: KeyboardKeyEvent) {
        if (!isClickGuiScreen(e.screen)) {
            return;
        }

        if (filteredModules.length === 0 || e.action === 0) {
            return;
        }

        switch (e.key) {
            case "key.keyboard.down":
                selectedIndex = (selectedIndex + 1) % filteredModules.length;
                break;
            case "key.keyboard.up":
                selectedIndex =
                    (selectedIndex - 1 + filteredModules.length) %
                    filteredModules.length;
                break;
            case "key.keyboard.enter":
                await toggleModule(
                    filteredModules[selectedIndex].name,
                    !filteredModules[selectedIndex].enabled,
                );
                break;
            case "key.keyboard.tab":
                const m = filteredModules[selectedIndex]?.name;
                if (m) {
                    $highlightModuleName = m;
                }
                break;
        }

        resultElements[selectedIndex]?.scrollIntoView({
            behavior: "smooth",
            block: "nearest",
        });
    }

    function handleBrowserKeyDown(e: KeyboardEvent) {
        if (e.key === "ArrowDown" || e.key === "ArrowUp" || e.key === "Tab") {
            e.preventDefault();
        }
    }

    async function toggleModule(name: string, enabled: boolean) {
        await setModuleEnabled(name, enabled);
    }

    function handleWindowClick(e: MouseEvent) {
        if (!searchContainerElement.contains(e.target as Node) && !hasFocus) {
            reset();
        }
    }

    function handleMouseOut() {
        hasFocus = false;
        reset();
    }

    function handleWindowKeyDown() {
        if (document.activeElement !== document.body) {
            return;
        }

        if (autoFocus) {
            searchInputElement.focus();
        }
    }

    function applyValues(configurable: ConfigurableSetting) {
        autoFocus = configurable.value.find(v => v.name === "SearchBarAutoFocus")?.value as boolean ?? true;
    }

    onMount(async () => {
        const clickGuiSettings = await getModuleSettings("ClickGUI");
        applyValues(clickGuiSettings);

        if (autoFocus) {
            searchInputElement.focus();
        }
    });

    listen("moduleToggle", (e: ModuleToggleEvent) => {
        const mod = modules.find((m) => m.name === e.moduleName);
        if (!mod) {
            return;
        }
        mod.enabled = e.enabled;

        // Refilter modules to update enabled state
        filterModules(false);
    });

    listen("keyboardKey", handleKeyDown);

    listen("clickGuiValueChange", (e: ClickGuiValueChangeEvent) => {
        applyValues(e.configurable);
    });
</script>

<svelte:window on:click={handleWindowClick} on:keydown={handleWindowKeyDown} on:contextmenu={handleWindowClick}/>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
        class="search"
        class:has-results={query}
        class:has-focus={hasFocus}
        bind:this={searchContainerElement}
        on:mouseenter={() => hasFocus = true}
        on:mouseleave={handleMouseOut}
>
    <input
            type="text"
            class="search-input"
            placeholder="Search"
            spellcheck="false"
            bind:value={query}
            bind:this={searchInputElement}
            on:input={() => filterModules(true)}
            on:keydown={handleBrowserKeyDown}
    />

    {#if query}
        <div class="results">
            {#if filteredModules.length > 0}
                {#each filteredModules as {name, localizedName, enabled, aliases}, index (name)}
                    <!-- svelte-ignore a11y-click-events-have-key-events -->
                    <!-- svelte-ignore a11y-no-static-element-interactions -->
                    <div
                            class="result"
                            class:enabled
                            on:click={() => toggleModule(name, !enabled)}
                            on:contextmenu|preventDefault={() => $highlightModuleName = name}
                            class:selected={selectedIndex === index}
                            bind:this={resultElements[index]}
                    >
                        <div class="module-name">
                            {displayName(name, localizedName, $spaceSeperatedNames)}
                        </div>
                        <div class="aliases">
                            {#if aliases.length > 0}
                                (aka {aliases.map(name => $spaceSeperatedNames ? convertToSpacedString(name) : name).join(", ")})
                            {/if}
                        </div>
                    </div>
                {/each}
            {:else}
                <div class="placeholder">No modules found</div>
            {/if}
        </div>
    {/if}
</div>

<style lang="scss">

  .search {
    position: fixed;
    left: 50%;
    top: 70px;
    transform: translateX(-50%);
    width: 600px;
    border-radius: 999px;
    overflow: hidden;
    background-image: var(--clickgui-search-background-image);
    background-color: var(--clickgui-search-background-color);
    -webkit-backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    backdrop-filter: blur(var(--clickgui-glass-blur-radius)) saturate(var(--clickgui-glass-saturation));
    border: 1px solid var(--clickgui-search-border-color);
    box-shadow: var(--clickgui-search-shadow);
    transition: ease border-radius 0.24s, ease border-color 0.2s, ease box-shadow 0.2s;

    &.has-results {
      border-radius: 18px;
    }

    &:focus-within,
    &.has-focus {
      z-index: 9999999999;
      border-color: var(--clickgui-search-focus-border-color);
      box-shadow: var(--clickgui-search-shadow),
      0 0 22px var(--clickgui-search-focus-glow-color);
    }
  }

  .results {
    border-top: solid 1px var(--clickgui-search-divider-color);
    padding: 4px 8px 8px;
    max-height: 250px;
    overflow: auto;

    .result {
      font-size: 15px;
      padding: 9px 12px;
      border-radius: 10px;
      transition: ease padding-left 0.2s, ease background-color 0.2s, ease color 0.2s;
      cursor: pointer;
      display: grid;
      grid-template-columns: max-content 1fr max-content;
      align-items: baseline;
      column-gap: 8px;

      .module-name {
        color: var(--clickgui-text-dimmed-color);
        transition: ease color 0.2s;
      }

      &.enabled {
        .module-name {
          color: var(--clickgui-search-enabled-color);
        }
      }

      .aliases {
        color: var(--clickgui-search-alias-color);
        margin-left: 0;
      }

      &.selected {
        padding-left: 20px;
      }

      &:hover {
        color: var(--clickgui-text-color);
        background-image: var(--clickgui-search-row-hover-background-image);

        &::after {
          content: "Right-click to locate";
          color: var(--clickgui-search-hint-color);
          font-size: 12px;
        }
      }
    }

    .placeholder {
      color: var(--clickgui-text-dimmed-color);
      font-size: 15px;
      padding: 10px 12px;
    }

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 999px;
      background-color: var(--clickgui-panel-scrollbar-thumb-color);
    }
  }

  .search-input {
    padding: 14px 24px;
    background-color: transparent;
    border: none;
    font-family: "Inter", sans-serif;
    font-size: 15px;
    color: var(--clickgui-text-color);
    width: 100%;

    &::placeholder {
      color: var(--clickgui-search-hint-color);
    }
  }
</style>
