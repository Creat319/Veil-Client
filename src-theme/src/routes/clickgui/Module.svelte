<script lang="ts">
    import {onMount} from "svelte";
    import {
        getModuleSettings,
        setModuleSettings,
        setModuleEnabled,
    } from "../../integration/rest";
    import type {ConfigurableSetting} from "../../integration/types";
    import GenericSetting from "./setting/common/GenericSetting.svelte";
    import {slide} from "svelte/transition";
    import {quintOut} from "svelte/easing";
    import {description as descriptionStore, highlightModuleName} from "./clickgui_store";
    import {localizationEpoch} from "./clickgui_store";
    import {setItem} from "../../integration/persistent_storage";
    import {convertToSpacedString, spaceSeperatedNames} from "../../theme/theme_config";
    import {displayName} from "../../util/display_name";
    import {scaleFactor} from "./clickgui_store";

    export let name: string;
    export let localizedName: string | null = null;
    export let enabled: boolean;
    export let description: string;
    export let aliases: string[];

    let moduleNameElement: HTMLElement;
    let configurable: ConfigurableSetting;
    const path = `clickgui.${name}`;
    let expanded = false;
    let hasSettings = false;

    onMount(async () => {
        await fetchModuleSettings();

        setTimeout(() => {
            expanded = localStorage.getItem(path) === "true"
        }, 500);
    });

    // Setting labels are translated by the client, so an already expanded settings block has to
    // be fetched again when the ClickGUI language changes.
    $: if ($localizationEpoch > 0) {
        void fetchModuleSettings();
    }

    highlightModuleName.subscribe((m) => {        if (name !== m) {
            return;
        }

        setTimeout(() => {
            if (!moduleNameElement) {
                return;
            }
            moduleNameElement.scrollIntoView({
                behavior: "smooth",
                block: "center",
            });
        }, 1000);
    });

    async function fetchModuleSettings() {
        configurable = await getModuleSettings(name);
        hasSettings = configurable.value.filter(v => v.name !== "Bind" && v.name !== "Hidden").length > 0;
    }

    async function updateModuleSettings() {
        await setModuleSettings(name, configurable);
        await fetchModuleSettings();
    }

    async function toggleModule() {
        await setModuleEnabled(name, !enabled);
    }

    function setDescription() {
        if (!moduleNameElement) return;

        const boundingRect = moduleNameElement.getBoundingClientRect();
        const y = (boundingRect.top + (moduleNameElement.clientHeight / 2)) * (2 / $scaleFactor);

        let moduleDescription = description;
        if (aliases.length > 0) {
            moduleDescription += ` (aka ${aliases.map(name => $spaceSeperatedNames ? convertToSpacedString(name) : name).join(", ")})`;
        }

        // If element is less than 300px from the right, display description on the left
        if (window.innerWidth - boundingRect.right > 300) {
            const x = boundingRect.right * (2 / $scaleFactor);
            descriptionStore.set({
                x,
                y,
                anchor: "right",
                description: moduleDescription
            });
        } else {
            const x = boundingRect.left * (2 / $scaleFactor);

            descriptionStore.set({
                x,
                y,
                anchor: "left",
                description: moduleDescription
            });
        }
    }

    async function toggleExpanded(e: MouseEvent) {
        e.stopPropagation();

        expanded = !expanded;
        await setItem(path, expanded.toString());
    }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
        class="module"
        class:expanded
        in:slide={{ duration: 500, easing: quintOut }}
        out:slide={{ duration: 500, easing: quintOut }}
>
    <!-- svelte-ignore a11y-click-events-have-key-events -->
    <div
            class="name"
            on:contextmenu|preventDefault={toggleExpanded}
            on:click={toggleModule}
            on:mouseenter={setDescription}
            on:mouseleave={() => descriptionStore.set(null)}
            bind:this={moduleNameElement}
            class:enabled
            class:highlight={name === $highlightModuleName}
    >
        {displayName(name, localizedName, $spaceSeperatedNames)}

        {#if hasSettings}
            <button
                    class="expand-arrow"
                    aria-label="Expand settings"
                    aria-expanded={expanded}
                    on:click={toggleExpanded}
            >
                <span class="expand-arrow-icon"></span>
            </button>
        {/if}
    </div>

    {#if expanded && configurable}
        <div class="settings">
            {#each configurable.value as setting (setting.name)}
                <GenericSetting {path} bind:setting on:change={updateModuleSettings}/>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  .module {
    position: relative;
  }

  .name {
    position: relative;
    margin: 1px 6px;
    padding: 9px 34px 9px 16px;
    border-radius: 10px;
    cursor: pointer;

    color: var(--clickgui-text-dimmed-color);
    text-align: left;
    font-size: 12px;
    font-weight: 500;
    line-height: 1.2;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    background-color: transparent;
    background-image: var(--clickgui-module-background-image);
    transition: ease color 0.2s, ease background-color 0.2s, ease box-shadow 0.2s;

    /* Status indicator, only lit while the module is enabled. */
    &::before {
      content: "";
      position: absolute;
      left: 6px;
      top: 50%;
      width: 3px;
      height: 12px;
      margin-top: -6px;
      border-radius: 999px;
      background-color: transparent;
      transition: ease background-color 0.22s, ease box-shadow 0.22s, ease height 0.22s, ease margin-top 0.22s;
    }

    &.highlight {
      box-shadow: inset 0 0 0 2px var(--clickgui-module-highlight-color),
      0 0 14px var(--clickgui-module-highlight-glow-color);
    }

    &:hover {
      background-image: var(--clickgui-module-hover-background-image);
      color: var(--clickgui-text-color);
    }

    &.enabled {
      color: var(--clickgui-module-enabled-color);
      background-image: var(--clickgui-module-enabled-background-image);

      &::before {
        background-color: var(--clickgui-module-indicator-color);
        box-shadow: 0 0 10px var(--clickgui-module-indicator-glow-color);
        height: 16px;
        margin-top: -8px;
      }

      &:hover {
        background-image: var(--clickgui-module-active-background-image);
        color: var(--clickgui-text-color);
      }
    }
  }

  .settings {
    margin: 2px 6px 8px;
    padding: 4px 11px 7px;
    border-radius: 12px;
    background-image: var(--clickgui-module-settings-background-image);
    box-shadow: inset 0 0 0 1px var(--clickgui-module-settings-border-color),
    inset 0 1px 10px var(--clickgui-module-settings-inner-shadow-color);
  }

  .expand-arrow {
    all: unset;
    position: absolute;
    top: 2px;
    right: 5px;
    width: 26px;
    height: calc(100% - 4px);
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    cursor: pointer;
    transition: ease background-color 0.2s;

    &:hover {
      background-color: var(--clickgui-module-hover-background-color);
    }
  }

  .expand-arrow-icon {
    width: 11px;
    height: 11px;
    display: block;
    background-image: url("/img/clickgui/icon-settings-expand.svg");
    background-position: center;
    background-repeat: no-repeat;
    opacity: 0.5;
    transform: rotate(-90deg);
    filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.45));
    transition: ease opacity 0.2s, ease transform 0.4s;
  }

  .module.expanded .expand-arrow-icon {
    transform: rotate(0);
    opacity: 1;
  }
</style>
