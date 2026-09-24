package io.github.fiol_dev.konstant.reload

import io.github.fiol_dev.konstant.core.ConfigSource
import kotlinx.coroutines.flow.Flow

/**
 * A source whose values can change while the app runs, such as a remote config service.
 * [ReloadableConfig.watch] reloads the config each time [changes] emits.
 */
public interface ReloadableSource : ConfigSource {
    /**
     * Emits after the source's values changed. It should also emit once when collection starts
     * (a `StateFlow` does), so a change made before [ReloadableConfig.watch] subscribes is not
     * lost; an extra emission only costs a reload that finds nothing new.
     */
    public val changes: Flow<Unit>
}
