<script lang="ts">
    import {onMount, tick} from "svelte";
    import type {Module} from "../../../integration/types";
    import {getModules} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import {getTextWidth} from "../../../integration/text_measurement";
    import {flip} from "svelte/animate";
    import {cubicOut} from "svelte/easing";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import type {HudArrayListSettings} from "../components";

    export let settings: { [name: string]: any };

    let cSettings = settings as HudArrayListSettings;

    interface Row {
        name: string;
        text: string;
        tag: string | null;
        phase: number;
        corners: string;
    }

    let rows: Row[] = [];

    // Mirrors the row metrics of the layout below, so widths can be measured before rendering.
    const ROW_HEIGHT = 18;
    const PAD_X = 5;
    const LINE_WIDTH = 2;
    const RADIUS = 4;
    const FONT = "500 12.5px Inter";

    $: rightAligned = cSettings.itemAlignment !== "Left";
    $: style = cSettings.style ?? "Staircase";
    $: animation = cSettings.animation ?? "SlideFade";
    $: showLine = style !== "Text" && (cSettings.showLine || style === "Accent");
    $: showBackground = style !== "Text" && (cSettings.showBackground ?? true);
    $: pill = style === "Pill";

    async function updateEnabledModules() {
        const modules = await getModules();
        const settings = cSettings;

        const visible = modules.filter(module => module.enabled && !module.hidden);
        const entries = visible.map(module => {
            let text = $spaceSeperatedNames ? convertToSpacedString(module.name) : module.name;
            if (settings.lowercase) {
                text = text.toLowerCase();
            }

            let tag = module.tag;
            if (tag != null && settings.showTags) {
                tag = settings.lowercase ? tag.toLowerCase() : tag;
            } else {
                tag = null;
            }

            const label = tag === null ? text : `${text} ${tag}`;
            const textWidth = getTextWidth(label, FONT);
            const width = textWidth + PAD_X * 2 + (showLine ? LINE_WIDTH : 0);

            return {name: module.name, text, tag, width};
        });

        switch (settings.order) {
            case "Ascending":
                entries.sort((a, b) => a.width - b.width);
                break;
            case "Alphabetical":
                entries.sort((a, b) => a.text.localeCompare(b.text));
                break;
            default:
                entries.sort((a, b) => b.width - a.width);
        }

        // The row that sticks out further than its neighbour gets a rounded corner on the ragged
        // side, so the whole column reads as one connected staircase.
        rows = entries.map((entry, index, all) => {
            const next = all[index + 1];
            const ragged = next === undefined ? RADIUS : Math.min(RADIUS, Math.abs(entry.width - next.width));
            const first = index === 0;
            const last = index === all.length - 1;

            const tl = first ? RADIUS : 0;
            const tr = first ? RADIUS : 0;
            let bl = 0;
            let br = 0;

            if (rightAligned) {
                bl = ragged;
                if (last) {
                    bl = RADIUS;
                    br = RADIUS;
                }
            } else {
                br = ragged;
                if (last) {
                    bl = RADIUS;
                    br = RADIUS;
                }
            }

            return {
                name: entry.name,
                text: entry.text,
                tag: entry.tag,
                phase: index,
                corners: `${tl}px ${tr}px ${br}px ${bl}px`,
                width: entry.width,
            };
        });

        await tick();
    }

    /**
     * Row animation, following the module list this was ported from: the row grows out of the
     * edge (width and height), sliding in towards its final position.
     */
    function reveal(node: HTMLElement, params: { mode: string, right: boolean }) {
        const width = node.offsetWidth;
        const height = node.offsetHeight;
        const mode = params.mode;
        const slide = (params.right ? 1 : -1) * 18;

        return {
            duration: 240,
            easing: cubicOut,
            css: (t: number) => {
                const slideMode = mode === "Slide" || mode === "SlideFade";
                const w = mode === "Fade" ? width : Math.max(0.1, width * t);
                const h = mode === "Fade" ? height : Math.max(0.1, height * t);
                const x = slideMode ? slide * (1 - t) : 0;
                const scale = mode === "Scale" ? 0.62 + 0.38 * t : 1;
                const opacity = mode === "Slide" ? 1 : t;

                return `width: ${w}px; height: ${h}px; opacity: ${opacity}; ` +
                    `transform: translateX(${x}px) scale(${scale});`;
            }
        };
    }

    $: if (cSettings !== settings) {
        cSettings = settings as HudArrayListSettings;
        updateEnabledModules();
    }

    spaceSeperatedNames.subscribe(async () => {
        await updateEnabledModules();
    });

    onMount(async () => {
        await updateEnabledModules();
    });

    listen("moduleToggle", async () => {
        await updateEnabledModules();
    });

    listen("refreshArrayList", async () => {
        await updateEnabledModules();
    });
</script>

<div
        class="arraylist"
        class:right={rightAligned}
        class:pill
        data-style={style}
        data-animation={animation}
        data-color={cSettings.textColor ?? "Theme"}
        data-glow={String(cSettings.glow ?? "Off").toLowerCase()}
        data-shadow={String(cSettings.shadow ?? "Off").toLowerCase()}
>
    {#each rows as row (row.name)}
        <div
                class="row"
                class:line={showLine}
                class:no-background={!showBackground}
                style="--row-w: {row.width}px; --row-h: {ROW_HEIGHT}px; --row-radius: {row.corners}; --phase: {row.phase};"
                animate:flip={{duration: 200}}
                transition:reveal={{mode: animation, right: rightAligned}}
        >
            <span class="label">{row.text}</span>
            {#if row.tag !== null}
                <span class="tag">{row.tag}</span>
            {/if}
        </div>
    {/each}
</div>

<style lang="scss">
  .arraylist {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    width: max-content;
    user-select: none;

    &.right {
      align-items: flex-end;
    }

    &.pill {
      gap: 2.5px;
    }

    &[data-shadow="weak"] {
      filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.45));
    }

    &[data-shadow="medium"] {
      filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.55));
    }

    &[data-shadow="strong"] {
      filter: drop-shadow(0 3px 10px rgba(0, 0, 0, 0.7));
    }
  }

  .row {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    box-sizing: border-box;
    width: var(--row-w, max-content);
    height: var(--row-h, 18px);
    padding: 0 5px;
    overflow: hidden;
    white-space: nowrap;
    border-radius: var(--row-radius, 0);
    background-color: var(--arraylist-background-color);

    &.no-background {
      background-color: transparent;
    }

    /* accent bar on the side facing away from the screen edge */
    &.line {
      padding-left: 7px;

      &::before {
        content: "";
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        width: 2px;
        background-color: var(--arraylist-accent-color);
      }
    }
  }

  .arraylist.right .row.line {
    padding-left: 5px;
    padding-right: 7px;

    &::before {
      left: auto;
      right: 0;
    }
  }

  .label {
    font-family: "Inter", sans-serif;
    font-size: 12.5px;
    font-weight: 500;
    letter-spacing: 0.1px;
    line-height: 1;
    color: var(--arraylist-text-color);
  }

  .tag {
    margin-left: 4px;
    font-family: "Inter", sans-serif;
    font-size: 12.5px;
    font-weight: 500;
    line-height: 1;
    color: var(--arraylist-tag-color);
  }

  /* text colour modes */
  .arraylist[data-color="Accent"] .label {
    color: var(--arraylist-accent-color);
  }

  .arraylist[data-color="White"] .label {
    color: #ffffff;
  }

  /* flowing gradient over the labels, each row offset a little so the column shimmers */
  .arraylist[data-color="Theme"] .label {
    background-image: linear-gradient(100deg,
        var(--arraylist-flow-start-color),
        var(--arraylist-flow-mid-color),
        var(--arraylist-flow-end-color),
        var(--arraylist-flow-mid-color),
        var(--arraylist-flow-start-color));
    background-size: 300% 100%;
    background-clip: text;
    -webkit-background-clip: text;
    color: transparent;
    animation: arraylist-flow 4s linear infinite;
    animation-delay: calc(var(--phase, 0) * -0.35s);
  }

  @keyframes arraylist-flow {
    from {
      background-position: 0% 50%;
    }
    to {
      background-position: -300% 50%;
    }
  }

  /* glow modes */
  .arraylist[data-glow="weak"] .label {
    filter: drop-shadow(0 0 3px var(--arraylist-glow-color));
  }

  .arraylist[data-glow="medium"] .label {
    filter: drop-shadow(0 0 6px var(--arraylist-glow-color));
  }

  .arraylist[data-glow="strong"] .label {
    filter: drop-shadow(0 0 10px var(--arraylist-glow-color));
  }

  .arraylist.pill .row {
    border-radius: 999px;
  }
</style>
