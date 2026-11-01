<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ChooseSetting, ModuleSetting,} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import Dropdown from "./common/Dropdown.svelte";
    import {displayName} from "../../../util/display_name";

    export let setting: ModuleSetting;

    const cSetting = setting as ChooseSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }
</script>

<div class="setting">
    <Dropdown
        on:change={handleChange}
        bind:value={cSetting.value}
        options={cSetting.choices}
        name={displayName(setting.name, setting.localizedName, $spaceSeperatedNames)}
    />
</div>

<style lang="scss">
    .setting {
        padding: 7px 0px;
    }
</style>
