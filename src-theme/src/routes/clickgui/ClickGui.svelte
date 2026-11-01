<script lang="ts">
    import type {GroupedModules, Module} from "../../integration/types";
    import Panel from "./Panel.svelte";
    import Search from "./Search.svelte";
    import Description from "./Description.svelte";
    import {fade} from "svelte/transition";
    import {onMount} from "svelte";
    import {getCategories, getModules} from "../../integration/rest";
    import {groupByCategory} from "../../integration/util";
    import {gridSize, localizationEpoch, showGrid} from "./clickgui_store";
    import ScaledClickGuiContent from "./ScaledClickGuiContent.svelte";

    let categories = $state<GroupedModules>({});
    let modules = $state<Module[]>([]);
    let icons = $state<Record<string, string>>({});
    let categoryTitles = $state<Record<string, string>>({});

    /**
     * The client decides whether labels are translated (see the "ClickGuiTranslation" module),
     * so the list is fetched again whenever that changes.
     */
    async function loadModules() {
        modules = await getModules();
        categories = groupByCategory(modules);

        const availableCategories = await getCategories();

        icons = Object.fromEntries(
            availableCategories
                .filter(category => category.icon)
                .map(category => [category.name, category.icon!])
        );
        categoryTitles = Object.fromEntries(
            availableCategories.map(category => [category.name, category.localizedName ?? category.name])
        );
    }

    // Fires on mount and again whenever the client switches the labels between languages
    // (see the "ClickGuiTranslation" module).
    onMount(() => {
        return localizationEpoch.subscribe(() => {
            void loadModules();
        });
    });
</script>

<ScaledClickGuiContent>
    <div
            class="clickgui"
            class:grid={$showGrid}
            style="background-size: {$gridSize}px {$gridSize}px;"
            transition:fade|global={{duration: 200}}
    >
        <Description/>
        <Search modules={structuredClone($state.snapshot(modules))}/>

        {#each Object.entries(categories) as [category, modules], panelIndex (category)}
            <Panel {category} {modules} {panelIndex} title={categoryTitles[category]} icon={icons[category]}/>
        {/each}
    </div>
</ScaledClickGuiContent>

<style lang="scss">
  .clickgui {
    position: absolute;
    inset: 0;

    &.grid {
      background-image: linear-gradient(to right, var(--clickgui-grid-color) 1px, transparent 1px),
      linear-gradient(to bottom, var(--clickgui-grid-color) 1px, transparent 1px);
    }
  }
</style>
