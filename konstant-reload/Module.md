# Module konstant-reload

Configs that change while the app runs. `ReloadableConfig` exposes the current config as a
`StateFlow`, reloads it on demand, on a timer or when a `ReloadableSource` changes, and keeps the
last good config when a reload fails. `RemoteSource` is a source your app fills from any remote
config service (Firebase Remote Config, your own backend) by calling `update`.

# Package io.github.fiol_dev.konstant.reload

Reloadable configs and the remote source.
