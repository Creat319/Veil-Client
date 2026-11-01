import {type Writable, writable} from "svelte/store";
import {listenAlways} from "../../integration/ws";

export interface TDescription {
    description: string;
    anchor: "left" | "right",
    x: number;
    y: number;
}

export const os: Writable<string | null> = writable<string | null>(null);

export const description: Writable<TDescription | null> = writable(null);

export const maxPanelZIndex: Writable<number> = writable(0);

export const highlightModuleName: Writable<string | null> = writable(null);

export const scaleFactor: Writable<number> = writable(2);

export const showGrid: Writable<boolean> = writable(false);

export const snappingEnabled: Writable<boolean> = writable(true);

export const gridSize: Writable<number> = writable(10);

export const darken = writable(true);

/**
 * Bumped when the client switches the ClickGUI between English and the translated labels, so
 * both the module list and already expanded settings fetch their labels again.
 */
export const localizationEpoch: Writable<number> = writable(0);

listenAlways("clickGuiLocalizationChange", () => localizationEpoch.update(epoch => epoch + 1));
