import {convertToSpacedString} from "../theme/theme_config";

/**
 * Label rendered for a module or category.
 *
 * The client can send a translated label (`localizedName`) when "ClickGuiTranslation" is on;
 * it only affects display - `name` stays the identifier that is sent back to the client, so
 * binds, saved configs and the search index keep working in either language.
 */
export function displayName(
    name: string,
    localizedName: string | null | undefined,
    spaceSeperated: boolean,
): string {
    if (localizedName) {
        return localizedName;
    }

    return spaceSeperated ? convertToSpacedString(name) : name;
}
